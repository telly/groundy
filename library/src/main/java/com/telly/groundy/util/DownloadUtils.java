/**
 * Copyright Telly, Inc. and other Groundy contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.telly.groundy.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.telly.groundy.Groundy;
import com.telly.groundy.GroundyTask;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

public final class DownloadUtils {
  private static boolean alreadyCheckedInternetPermission = false;
  /**
   * Amount of maximum allowed redirects number by:
   * http://www.google.com/support/forum/p/Webmasters/thread?tid=3760b68fb305088a&hl=en.
   */
  private static final int MAX_REDIRECTS = 5;

  /** Non instance constants class. */
  private DownloadUtils() {
  }

  /** Read it's name. */
  private static final int DEFAULT_BUFFER_SIZE = 4096; // 4 KiB

  /**
   * Download a file at <code>fromUrl</code> to a file specified by <code>toFile</code>.
   *
   * @param fromUrl An url pointing to a file to download
   * @param toFile File to save to, if existent will be overwrite
   * @param listener Callback that notifies the download progress
   * @throws java.io.IOException If fromUrl is invalid or there is any IO issue.
   */
  public static void downloadFile(Context context, String fromUrl, File toFile,
      DownloadProgressListener listener) throws IOException {
    downloadFile(context, fromUrl, toFile, listener, null);
  }

  /**
   * Download a file at <code>fromUrl</code> to a file specified by <code>toFile</code>.
   *
   * @param fromUrl An url pointing to a file to download
   * @param toFile File to save to, if existent will be overwrite
   * @param listener Callback that notifies the download progress
   * @param cancelListener Used to interrupt the download if necessary
   * @throws java.io.IOException If fromUrl is invalid or there is any IO issue.
   */
  public static void downloadFile(Context context, String fromUrl, File toFile,
      DownloadProgressListener listener, DownloadCancelListener cancelListener) throws IOException {
    downloadFileHandleRedirect(context, fromUrl, toFile, 0, listener, cancelListener);
  }

  public interface DownloadCancelListener {
    boolean shouldCancelDownload();
  }

  public interface DownloadProgressListener {
    /**
     * @param url      the url being downloaded
     * @param progress percentage of the download or {@link Groundy#NO_SIZE_AVAILABLE}
     *                 if file size is not available.
     */
    void onProgress(String url, int progress);
  }

  /**
   * Returns a progress listener that will post progress to the specified groundyTask.
   *
   * @param groundyTask the groundyTask to post progress to. Cannot be null.
   * @return a progress listener
   */
  public static DownloadProgressListener getDownloadListenerForTask(final GroundyTask groundyTask) {
    return new DownloadProgressListener() {
      @Override
      public void onProgress(String url, int progress) {
        groundyTask.updateProgress(progress);
      }
    };
  }

  /**
   * Internal version of {@link #downloadFile(Context, String, java.io.File,
   * DownloadUtils.DownloadProgressListener)}.
   *
   *
   * @param fromUrl the url to download from
   * @param toFile the file to download to
   * @param redirect true if it should accept redirects
   * @param listener used to report result back
   * @param cancelListener used to interrupt the download if necessary
   * @throws java.io.IOException
   */
  private static void downloadFileHandleRedirect(Context context, String fromUrl, File toFile,
      int redirect, DownloadProgressListener listener, DownloadCancelListener cancelListener) throws IOException {
    if (context == null) {
      throw new RuntimeException("Context shall not be null");
    }
    if (!alreadyCheckedInternetPermission) {
      checkForInternetPermissions(context);
    }

    if (redirect > MAX_REDIRECTS) {
      throw new IOException("Too many redirects for " + fromUrl);
    }

    if (cancelListener != null && cancelListener.shouldCancelDownload()) {
      return;
    }

    URL url = new URL(fromUrl);
    URLConnection urlConnection = url.openConnection();
    urlConnection.setRequestProperty("Accept-Encoding", "gzip");
    urlConnection.connect();
    String redirectTarget = urlConnection.getHeaderField("Location");
    if (redirectTarget != null) {
      downloadFileHandleRedirect(context, redirectTarget, toFile, redirect + 1, listener,
          cancelListener);
      return;
    }

    if (cancelListener != null && cancelListener.shouldCancelDownload()) {
      return;
    }

    InputStream input = urlConnection.getInputStream();
    if ("gzip".equals(urlConnection.getContentEncoding())) {
      input = new GZIPInputStream(input);
    }

    if (cancelListener != null && cancelListener.shouldCancelDownload()) {
      input.close();
      return;
    }

    OutputStream output = new FileOutputStream(toFile);
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    long total = 0;
    int count;
    int fileLength = urlConnection.getContentLength();

    if (fileLength == -1 && listener != null) {
      listener.onProgress(fromUrl, Groundy.NO_SIZE_AVAILABLE);
    }

    while ((count = input.read(buffer)) > 0) {// > 0 due zero sized streams
      if (cancelListener != null && cancelListener.shouldCancelDownload()) {
        output.close();
        input.close();
        return;
      }
      total += count;
      output.write(buffer, 0, count);
      if (listener != null && fileLength > 0) {
        listener.onProgress(fromUrl, (int) (total * 100 / fileLength));
      }
    }
    output.close();
    input.close();
  }

  private static void checkForInternetPermissions(Context context) {
    try {
      PackageManager pm = context.getPackageManager();
      PackageInfo packageInfo =
          pm.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
      String[] requestedPermissions = packageInfo.requestedPermissions;
      if (requestedPermissions == null) {
        throw new RuntimeException("You must add android.permission.INTERNET to your app");
      }
      boolean found = false;
      for (String requestedPermission : requestedPermissions) {
        if ("android.permission.INTERNET".equals(requestedPermission)) {
          found = true;
        }
      }
      if (!found) {
        throw new RuntimeException("You must add android.permission.INTERNET to your app");
      } else {
        alreadyCheckedInternetPermission = true;
      }
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
  }
}
