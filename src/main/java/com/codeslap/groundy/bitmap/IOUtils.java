package com.codeslap.groundy.bitmap;


import android.content.Context;

import java.io.*;
import java.net.URL;

/**
 * IO useful methods
 * @author evelio
 * @version 1.0
 */
class IOUtils {
	/**
	 * Non instance constants class
	 */
	private IOUtils(){}
	/**
	 * Read it's name
	 */
	private static final int DEFAULT_BUFFER_SIZE = 4096; // 4 KiB
	/**
	 * Copies an input stream to an output stream
	 * @param input
	 * The source
	 * @param output
	 * The target
	 * @throws java.io.IOException
	 *             From
	 *             http://stackoverflow.com/questions/4064211/how-to-make-a-deep-copy-of-an-inputstream-in-java
	 */
	private static void copy(InputStream input, OutputStream output)
			throws IOException {
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		int n;
		while ((n = input.read(buffer)) > 0) {// > 0 due zero sized streams
			output.write(buffer, 0, n);
		}
	}

    /**
	 * Finds out the cache directory
	 * @param context
	 * Context to use
	 *
	 * @return
	 * A File where means a directory where cache files should be written
	 */
	public static File getCacheDirectory(Context context) {
		File cacheDir = context.getCacheDir();
		if(!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		return cacheDir;
	}

    /**
	 * Download a file at <code>fromUrl</code> to a file specified by <code>toFile</code>
	 * @param fromUrl
	 * An url pointing to a file to download
	 * @param toFile
	 * File to save to, if existent will be overwrite
	 * @throws java.io.IOException
	 * If fromUrl is invalid or there is any IO issue.
	 */
	public static void downloadFile(String fromUrl, File toFile) throws IOException {
		InputStream input = new URL(fromUrl).openStream();
        OutputStream output = new FileOutputStream(toFile);
        IOUtils.copy(input, output);
        output.close();
        input.close();
	}
}
