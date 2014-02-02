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

package com.telly.groundy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;

import android.os.ResultReceiver;
import java.io.Serializable;
import java.util.ArrayList;

public final class Groundy implements Parcelable {
  /**
   * Key used by the {@link com.telly.groundy.annotations.OnProgress} callback to specify
   * the progress of the task. Parameters annotated with this key must be int.
   */
  public static final String PROGRESS = "com.telly.groundy.key.PROGRESS";

  /**
   * If the task crashed, the {@link com.telly.groundy.annotations.OnFailure} callback will
   * be invoked and it's possible to receive the exception message by annotating a parameter with
   * this key, e.g.
   *
   * <pre>{@code
   *
   * @OnFailure public void onFail(@Param(Groundy.CRASH_MESSAGE) String crashMessage) {
   * // do something nice
   * }
   * }</pre>
   */
  public static final String CRASH_MESSAGE = "com.telly.groundy.key.ERROR";

  /**
   * Every callback can receive the task id by annotating a parameter with this key. The task
   * id is a long timestamp generated when the Groundy task is created.
   */
  public static final String TASK_ID = "com.telly.groundy.key.TASK_ID";

  /**
   * If the task was cancelled, the {@link com.telly.groundy.annotations.OnCancel} callback will
   * be invoked and it's possible to receive the cancel reason by annotating a parameter with
   * this key. It must be an integer.
   */
  public static final String CANCEL_REASON = "com.telly.groundy.key.CANCEL_REASON";

  /**
   * Every callback can receive the original arguments sent to the task by annotating a parameter
   * with this key.
   */
  public static final String ORIGINAL_PARAMS = "com.telly.groundy.key.ORIGINAL_ARGS";

  /** The class used to implement the executed task. */
  public static final String TASK_IMPLEMENTATION = "com.telly.groundy.key.TASK_IMPLEMENTATION";

  /** Progress value returned when it's not possible to determine the size of a file. **/
  public static final int NO_SIZE_AVAILABLE = Integer.MIN_VALUE;

  /**
   * If true, the stack trace of the call for each groundy task will be send to the service.
   * It allows to know which piece of code invoked this task.*
   */
  public static boolean devMode = false;

  static final String KEY_ARGUMENTS = "com.telly.groundy.key.ARGS";
  static final String STACK_TRACE = "com.telly.groundy.key.STACK_TRACE";
  static final String KEY_RECEIVER = "com.telly.groundy.key.RECEIVER";
  static final String KEY_TASK = "com.telly.groundy.key.TASK";
  static final String KEY_GROUP_ID = "com.telly.groundy.key.GROUP_ID";
  static final String KEY_CALLBACK_ANNOTATION = "com.telly.groundy.key.CALLBACK_ANNOTATION";
  static final String KEY_CALLBACK_NAME = "com.telly.groundy.key.CALLBACK_NAME";

  private final Class<? extends GroundyTask> mGroundyTask;
  private final long mId;
  private CallbacksReceiver mReceiver;
  private final Bundle mArgs = new Bundle();
  private int mGroupId;
  private boolean mAlreadyProcessed = false;
  private CallbacksManager mCallbacksManager;
  private Class<? extends GroundyService> mGroundyClass = GroundyService.class;
  private boolean mAllowNonUIThreadCallbacks = false;

  private Groundy(Class<? extends GroundyTask> groundyTask) {
    mGroundyTask = groundyTask;
    mId = System.nanoTime();
  }

  private Groundy(Class<? extends GroundyTask> groundyTask, long id) {
    mGroundyTask = groundyTask;
    mId = id;
  }

  /**
   * Creates a new Groundy instance ready to be queued or executed. You can configure it by adding
   * arguments ({@link #args(android.os.Bundle)}), setting a group id ({@link #group(int)}) or
   * providing a callback ({@link #callback(Object...)}).
   * <p/>
   * You must configure the value <b>before</b> queueing or executing it.
   *
   * @param groundyTask reference of the groundy value implementation
   * @return new Groundy instance (does not execute anything)
   */
  public static Groundy create(Class<? extends GroundyTask> groundyTask) {
    if (groundyTask == null) {
      throw new IllegalStateException("GroundyTask no provided");
    }
    return new Groundy(groundyTask);
  }

  /**
   * Set the arguments needed to run the task.
   *
   * @param arguments a bundle of arguments
   * @return itself
   */
  public Groundy args(Bundle arguments) {
    checkAlreadyProcessed();
    mArgs.putAll(arguments);
    return this;
  }

  /**
   * Allows this value to receive callback messages on non UI threads.
   *
   * @return itself
   */
  public Groundy allowNonUiCallbacks() {
    checkAlreadyProcessed();
    mAllowNonUIThreadCallbacks = true;
    return this;
  }

  /**
   * @param callbacks callbacks to register for this value
   * @return itself
   */
  public Groundy callback(Object... callbacks) {
    if (callbacks == null || callbacks.length == 0) {
      throw new IllegalArgumentException("You must pass at least one callback handler");
    }
    if (mReceiver != null) {
      throw new IllegalStateException("callback method can only be called once");
    }
    checkAlreadyProcessed();
    if (!mAllowNonUIThreadCallbacks && Looper.myLooper() != Looper.getMainLooper()) {
      throw new IllegalStateException(
          "callbacks can only be set on the UI thread. If you are sure you can handle callbacks "
              + "from a non UI thread, call Groundy#allowNonUiCallbacks() method first");
    }
    mReceiver = new CallbacksReceiver(mGroundyTask, callbacks);
    return this;
  }

  /**
   * This allows you to set an identification groupId to the value which can be later used to
   * cancel it. Group ids can be shared by several groundy tasks even if their implementation is
   * different.
   * If cancelling tasks using a groupId, all tasks created with this groupId will be cancelled
   * and/or removed from the queue.
   *
   * @param groupId groupId for this value
   * @return itself
   */
  public Groundy group(int groupId) {
    if (groupId <= 0) {
      throw new IllegalStateException("Group id must be greater than zero");
    }
    checkAlreadyProcessed();
    mGroupId = groupId;
    return this;
  }

  /**
   * This allows you to use a different GroundyService implementation.
   *
   * @param groundyClass a different Groundy service implementation
   * @return itself
   */
  public Groundy service(Class<? extends GroundyService> groundyClass) {
    if (groundyClass == GroundyService.class) {
      throw new IllegalStateException(
          "This method is meant to set a different GroundyService implementation. "
              + "You cannot use GroundyService.class, http://i.imgur.com/IR23PAe.png");
    }
    checkAlreadyProcessed();
    mGroundyClass = groundyClass;
    return this;
  }

  /**
   * Sets a callback manager for this value. It allows you to easily attach/detach your callbacks
   * on
   * configuration change. This is important if you are not handling the configuration changes by
   * your self, since it will prevent leaks or wrong results when callbacks are invoked.
   *
   * @param callbacksManager a callback manager instance
   * @return itself
   */
  public Groundy callbackManager(CallbacksManager callbacksManager) {
    checkAlreadyProcessed();
    mCallbacksManager = callbacksManager;
    return this;
  }

  /**
   * Queues a value to the Groundy Service. This value won't be executed until the previous queued
   * tasks are done. If you need your value to execute right away use the {@link
   * Groundy#executeUsing(Context)} method.
   *
   * @param context used to start the Groundy service
   * @return a {@link TaskHandler} that can be used to clear the task callbacks list or cancel it
   */
  public TaskHandler queueUsing(Context context) {
    boolean async = false;
    return internalQueueOrExecute(context, async);
  }

  /**
   * Execute a value right away.
   *
   * @param context used to start the Groundy service
   * @return a {@link TaskHandler} that can be used to clear the task callbacks list or cancel it
   */
  public TaskHandler executeUsing(Context context) {
    boolean async = true;
    return internalQueueOrExecute(context, async);
  }

  /**
   * Creates an intent that can be used to execute this task by invoking context.startService().
   *
   * @param context used to start the groundy service
   * @param async true if the task must be executed right away, false if it must be queued
   * @return a unique number assigned to this value
   */
  public Intent asIntent(Context context, boolean async) {
    markAsProcessed();
    return internalGetServiceIntent(context, async);
  }

  private TaskHandler internalQueueOrExecute(Context context, boolean async) {
    markAsProcessed();
    TaskHandler taskProxy = new TaskHandlerImpl(this);
    if (mCallbacksManager != null) {
      mCallbacksManager.register(taskProxy);
    }

    context.startService(internalGetServiceIntent(context, async));
    return taskProxy;
  }

  long getId() {
    return mId;
  }

  Class<? extends GroundyService> getGroundyServiceClass() {
    return mGroundyClass;
  }

  Class<? extends GroundyTask> getGroundyTaskClass() {
    return mGroundyTask;
  }

  CallbacksReceiver getReceiver() {
    return mReceiver;
  }

  private void checkAlreadyProcessed() {
    if (mAlreadyProcessed) {
      throw new IllegalStateException("This method can only be called before queueUsing(), "
          + "executeUsing() or asIntent() methods");
    }
  }

  private void markAsProcessed() {
    if (mAlreadyProcessed) {
      throw new IllegalStateException("Task already queued or executed");
    }
    mAlreadyProcessed = true;
  }

  private Intent internalGetServiceIntent(Context context, boolean async) {
    Intent intent = new Intent(context, mGroundyClass);
    intent.setAction(async ? GroundyService.ACTION_EXECUTE : GroundyService.ACTION_QUEUE);
    intent.putExtra(KEY_ARGUMENTS, mArgs);

    if (devMode) {
      StackTraceElement[] stackTrace = new Throwable().getStackTrace();
      intent.putExtra(STACK_TRACE, stackTrace);
    }
    if (mReceiver != null) {
      intent.putExtra(KEY_RECEIVER, mReceiver);
    }
    intent.putExtra(KEY_TASK, mGroundyTask);
    intent.putExtra(TASK_ID, mId);
    intent.putExtra(KEY_GROUP_ID, mGroupId);
    return intent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Groundy)) {
      return false;
    }

    Groundy groundy = (Groundy) o;

    if (mId != groundy.mId) return false;
    if (!mGroundyTask.equals(groundy.mGroundyTask)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = mGroundyTask.hashCode();
    result = 31 * result + (int) (mId ^ (mId >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "Groundy{" +
        ", groundyTask=" + mGroundyTask +
        ", resultReceiver=" + mReceiver +
        ", extras=" + mArgs +
        ", groupId=" + mGroupId +
        '}';
  }

  @SuppressWarnings("UnusedDeclaration")
  public static final Creator<Groundy> CREATOR = new Creator<Groundy>() {
    @Override public Groundy createFromParcel(Parcel source) {
      Class groundyTask = (Class) source.readSerializable();
      long id = source.readLong();
      boolean hadReceiver = source.readByte() == 1;

      //noinspection unchecked
      Groundy groundy = new Groundy(groundyTask, id);
      if (hadReceiver) {
        ResultReceiver r = source.readParcelable(CallbacksReceiver.class.getClassLoader());
        if (r instanceof CallbacksReceiver) {
          groundy.mReceiver = (CallbacksReceiver) r;
        } else if (r != null) {
          //noinspection unchecked
          groundy.mReceiver = new CallbacksReceiver(groundyTask);
          Bundle bundle = new Bundle();
          bundle.putParcelable(CallbacksReceiver.RECEIVER_PARCEL, groundy.mReceiver);
          r.send(CallbacksReceiver.ATTACH_RECEIVER_PARCEL, bundle);
        }
      }
      groundy.mArgs.putAll(source.readBundle());
      groundy.mGroupId = source.readInt();
      groundy.mAlreadyProcessed = source.readByte() == 1;
      //noinspection unchecked
      groundy.mGroundyClass = (Class) source.readSerializable();
      groundy.mAllowNonUIThreadCallbacks = source.readByte() == 1;
      return groundy;
    }

    @Override public Groundy[] newArray(int size) {
      return new Groundy[size];
    }
  };

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeSerializable(mGroundyTask);
    dest.writeLong(mId);
    dest.writeByte((byte) (mReceiver == null ? 0 : 1));
    if (mReceiver != null) {
      dest.writeParcelable(mReceiver, flags);
    }
    dest.writeBundle(mArgs);
    dest.writeInt(mGroupId);
    dest.writeByte((byte) (mAlreadyProcessed ? 1 : 0));
    dest.writeSerializable(mGroundyClass);
    dest.writeByte((byte) (mAllowNonUIThreadCallbacks ? 1 : 0));
  }

  /**
   * Inserts a Boolean value into the mapping of this Bundle, replacing any existing value for the
   * given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a Boolean, or null
   */
  public Groundy arg(String key, boolean value) {
    mArgs.putBoolean(key, value);
    return this;
  }

  /**
   * Inserts a byte value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value a byte
   */
  public Groundy arg(String key, byte value) {
    mArgs.putByte(key, value);
    return this;
  }

  /**
   * Inserts a char value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value a char, or null
   */
  public Groundy arg(String key, char value) {
    mArgs.putChar(key, value);
    return this;
  }

  /**
   * Inserts a short value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value a short
   */
  public Groundy arg(String key, short value) {
    mArgs.putShort(key, value);
    return this;
  }

  /**
   * Inserts an int value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value an int, or null
   */
  public Groundy arg(String key, int value) {
    mArgs.putInt(key, value);
    return this;
  }

  /**
   * Inserts a long value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value a long
   */
  public Groundy arg(String key, long value) {
    mArgs.putLong(key, value);
    return this;
  }

  /**
   * Inserts a float value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value a float
   */
  public Groundy arg(String key, float value) {
    mArgs.putFloat(key, value);
    return this;
  }

  /**
   * Inserts a double value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value a double
   */
  public Groundy arg(String key, double value) {
    mArgs.putDouble(key, value);
    return this;
  }

  /**
   * Inserts a String value into the mapping of this Bundle, replacing any existing value for the
   * given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a String, or null
   */
  public Groundy arg(String key, String value) {
    mArgs.putString(key, value);
    return this;
  }

  /**
   * Inserts a CharSequence value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a CharSequence, or null
   */
  public Groundy arg(String key, CharSequence value) {
    mArgs.putCharSequence(key, value);
    return this;
  }

  /**
   * Inserts a Parcelable value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a Parcelable object, or null
   */
  public Groundy arg(String key, Parcelable value) {
    mArgs.putParcelable(key, value);
    return this;
  }

  /**
   * Inserts an array of Parcelable values into the mapping of this Bundle, replacing any existing
   * value for the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value an array of Parcelable objects, or null
   */
  public Groundy arg(String key, Parcelable[] value) {
    mArgs.putParcelableArray(key, value);
    return this;
  }

  /**
   * Inserts an ArrayList<Integer> value into the mapping of this Bundle, replacing any existing
   * value for the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value an ArrayList<Integer> object, or null
   */
  public Groundy addIntegerArrayList(String key, ArrayList<Integer> value) {
    mArgs.putIntegerArrayList(key, value);
    return this;
  }

  /**
   * Inserts an ArrayList<String> value into the mapping of this Bundle, replacing any existing
   * value for the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value an ArrayList<String> object, or null
   */
  public Groundy addStringArrayList(String key, ArrayList<String> value) {
    mArgs.putStringArrayList(key, value);
    return this;
  }

  /**
   * Inserts an ArrayList<CharSequence> value into the mapping of this Bundle, replacing any
   * existing value for the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value an ArrayList<CharSequence> object, or null
   */
  public Groundy arg(String key, ArrayList<CharSequence> value) {
    mArgs.putCharSequenceArrayList(key, value);
    return this;
  }

  /**
   * Inserts a Serializable value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a Serializable object, or null
   */
  public Groundy arg(String key, Serializable value) {
    mArgs.putSerializable(key, value);
    return this;
  }

  /**
   * Inserts a boolean array value into the mapping of this Bundle, replacing any existing value
   * for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a boolean array object, or null
   */
  public Groundy arg(String key, boolean[] value) {
    mArgs.putBooleanArray(key, value);
    return this;
  }

  /**
   * Inserts a byte array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a byte array object, or null
   */
  public Groundy arg(String key, byte[] value) {
    mArgs.putByteArray(key, value);
    return this;
  }

  /**
   * Inserts a short array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a short array object, or null
   */
  public Groundy arg(String key, short[] value) {
    mArgs.putShortArray(key, value);
    return this;
  }

  /**
   * Inserts a char array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a char array object, or null
   */
  public Groundy arg(String key, char[] value) {
    mArgs.putCharArray(key, value);
    return this;
  }

  /**
   * Inserts an int array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value an int array object, or null
   */
  public Groundy arg(String key, int[] value) {
    mArgs.putIntArray(key, value);
    return this;
  }

  /**
   * Inserts a long array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a long array object, or null
   */
  public Groundy arg(String key, long[] value) {
    mArgs.putLongArray(key, value);
    return this;
  }

  /**
   * Inserts a float array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a float array object, or null
   */
  public Groundy arg(String key, float[] value) {
    mArgs.putFloatArray(key, value);
    return this;
  }

  /**
   * Inserts a double array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a double array object, or null
   */
  public Groundy arg(String key, double[] value) {
    mArgs.putDoubleArray(key, value);
    return this;
  }

  /**
   * Inserts a String array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a String array object, or null
   */
  public Groundy arg(String key, String[] value) {
    mArgs.putStringArray(key, value);
    return this;
  }

  /**
   * Inserts a CharSequence array value into the mapping of this Bundle, replacing any existing
   * value for the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a CharSequence array object, or null
   */
  public Groundy arg(String key, CharSequence[] value) {
    mArgs.putCharSequenceArray(key, value);
    return this;
  }

  /**
   * Inserts a Bundle value into the mapping of this Bundle, replacing any existing value for the
   * given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a Bundle object, or null
   * @return itself
   */
  public Groundy arg(String key, Bundle value) {
    mArgs.putBundle(key, value);
    return this;
  }
}
