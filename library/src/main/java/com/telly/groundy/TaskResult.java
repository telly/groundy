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

import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * To be returned by {@link com.telly.groundy.GroundyTask#doInBackground()}. It must be either
 * {@link Succeeded}, {@link Failed} or {@link Cancelled}
 */
public class TaskResult {
  private final Bundle mBundle = new Bundle();
  private final ResultType mType;

  TaskResult(ResultType type) {
    mType = type;
  }

  Bundle getResultData() {
    return mBundle;
  }

  ResultType getType() {
    return mType;
  }

  /**
   * Inserts a Boolean value into the mapping of this Bundle, replacing any existing value for the
   * given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a Boolean, or null
   */
  public TaskResult add(String key, boolean value) {
    mBundle.putBoolean(key, value);
    return this;
  }

  /**
   * Inserts a byte value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value a byte
   */
  public TaskResult add(String key, byte value) {
    mBundle.putByte(key, value);
    return this;
  }

  /**
   * Inserts a char value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value a char, or null
   */
  public TaskResult add(String key, char value) {
    mBundle.putChar(key, value);
    return this;
  }

  /**
   * Inserts a short value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value a short
   */
  public TaskResult add(String key, short value) {
    mBundle.putShort(key, value);
    return this;
  }

  /**
   * Inserts an int value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value an int, or null
   */
  public TaskResult add(String key, int value) {
    mBundle.putInt(key, value);
    return this;
  }

  /**
   * Inserts a long value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value a long
   */
  public TaskResult add(String key, long value) {
    mBundle.putLong(key, value);
    return this;
  }

  /**
   * Inserts a float value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value a float
   */
  public TaskResult add(String key, float value) {
    mBundle.putFloat(key, value);
    return this;
  }

  /**
   * Inserts a double value into the mapping of this Bundle, replacing any existing value for the
   * given key.
   *
   * @param key a String, or null
   * @param value a double
   */
  public TaskResult add(String key, double value) {
    mBundle.putDouble(key, value);
    return this;
  }

  /**
   * Inserts a String value into the mapping of this Bundle, replacing any existing value for the
   * given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a String, or null
   */
  public TaskResult add(String key, String value) {
    mBundle.putString(key, value);
    return this;
  }

  /**
   * Inserts a CharSequence value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a CharSequence, or null
   */
  public TaskResult add(String key, CharSequence value) {
    mBundle.putCharSequence(key, value);
    return this;
  }

  /**
   * Inserts a Parcelable value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a Parcelable object, or null
   */
  public TaskResult add(String key, Parcelable value) {
    mBundle.putParcelable(key, value);
    return this;
  }

  /**
   * Inserts an array of Parcelable values into the mapping of this Bundle, replacing any existing
   * value for the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value an array of Parcelable objects, or null
   */
  public TaskResult add(String key, Parcelable[] value) {
    mBundle.putParcelableArray(key, value);
    return this;
  }

  /**
   * Inserts a List of Parcelable values into the mapping of this Bundle, replacing any existing
   * value for the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value an ArrayList of Parcelable objects, or null
   */
  public TaskResult addParcelableArrayList(String key, ArrayList<? extends Parcelable> value) {
    mBundle.putParcelableArrayList(key, value);
    return this;
  }

  /**
   * Inserts a SparseArray of Parcelable values into the mapping of this Bundle, replacing any
   * existing value for the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a SparseArray of Parcelable objects, or null
   */
  public TaskResult add(String key, SparseArray<? extends Parcelable> value) {
    mBundle.putSparseParcelableArray(key, value);
    return this;
  }

  /**
   * Inserts an ArrayList<Integer> value into the mapping of this Bundle, replacing any existing
   * value for the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value an ArrayList<Integer> object, or null
   */
  public TaskResult addIntegerArrayList(String key, ArrayList<Integer> value) {
    mBundle.putIntegerArrayList(key, value);
    return this;
  }

  /**
   * Inserts an ArrayList<String> value into the mapping of this Bundle, replacing any existing
   * value for the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value an ArrayList<String> object, or null
   */
  public TaskResult addStringArrayList(String key, ArrayList<String> value) {
    mBundle.putStringArrayList(key, value);
    return this;
  }

  /**
   * Inserts an ArrayList<CharSequence> value into the mapping of this Bundle, replacing any
   * existing value for the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value an ArrayList<CharSequence> object, or null
   */
  public TaskResult add(String key, ArrayList<CharSequence> value) {
    mBundle.putCharSequenceArrayList(key, value);
    return this;
  }

  /**
   * Inserts a Serializable value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a Serializable object, or null
   */
  public TaskResult add(String key, Serializable value) {
    mBundle.putSerializable(key, value);
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
  public TaskResult add(String key, boolean[] value) {
    mBundle.putBooleanArray(key, value);
    return this;
  }

  /**
   * Inserts a byte array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a byte array object, or null
   */
  public TaskResult add(String key, byte[] value) {
    mBundle.putByteArray(key, value);
    return this;
  }

  /**
   * Inserts a short array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a short array object, or null
   */
  public TaskResult add(String key, short[] value) {
    mBundle.putShortArray(key, value);
    return this;
  }

  /**
   * Inserts a char array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a char array object, or null
   */
  public TaskResult add(String key, char[] value) {
    mBundle.putCharArray(key, value);
    return this;
  }

  /**
   * Inserts an int array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value an int array object, or null
   */
  public TaskResult add(String key, int[] value) {
    mBundle.putIntArray(key, value);
    return this;
  }

  /**
   * Inserts a long array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a long array object, or null
   */
  public TaskResult add(String key, long[] value) {
    mBundle.putLongArray(key, value);
    return this;
  }

  /**
   * Inserts a float array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a float array object, or null
   */
  public TaskResult add(String key, float[] value) {
    mBundle.putFloatArray(key, value);
    return this;
  }

  /**
   * Inserts a double array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a double array object, or null
   */
  public TaskResult add(String key, double[] value) {
    mBundle.putDoubleArray(key, value);
    return this;
  }

  /**
   * Inserts a String array value into the mapping of this Bundle, replacing any existing value for
   * the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a String array object, or null
   */
  public TaskResult add(String key, String[] value) {
    mBundle.putStringArray(key, value);
    return this;
  }

  /**
   * Inserts a CharSequence array value into the mapping of this Bundle, replacing any existing
   * value for the given key.  Either key or value may be null.
   *
   * @param key a String, or null
   * @param value a CharSequence array object, or null
   */
  public TaskResult add(String key, CharSequence[] value) {
    mBundle.putCharSequenceArray(key, value);
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
  public TaskResult add(String key, Bundle value) {
    mBundle.putBundle(key, value);
    return this;
  }

  /**
   * Inserts all mappings from the given Bundle into this Bundle.
   *
   * @param bundle the bundle to map
   * @return itself
   */
  public TaskResult addAll(Bundle bundle) {
    mBundle.putAll(bundle);
    return this;
  }
}
