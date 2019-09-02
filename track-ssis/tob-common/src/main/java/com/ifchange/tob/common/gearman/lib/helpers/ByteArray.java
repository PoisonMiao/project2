package com.ifchange.tob.common.gearman.lib.helpers;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Almost Immutable byte array
 * @author isaiah
 */
public final class ByteArray {
	private final byte[] array;

	public ByteArray(final byte[] array) {
		this.array = array;
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(array);
	}

	public final byte[] getBytes() {
		return array.clone();
	}

	public final byte get(int index) {
		return array[index];
	}

	public final String toString(Charset charset) {
		return new String(array, charset);
	}

	public final boolean isEmpty() {
		return this.array.length==0;
	}

	public final int length() {
		return this.array.length;
	}

	@Override
	public final String toString() {
		return new String(array);
	}

	@Override
	public final boolean equals(final Object obj) {
		if(obj instanceof ByteArray)
			return this.equals((ByteArray)obj);
		else if(obj instanceof byte[])
			return this.equals((byte[])obj);
		else
			return false;
	}

	public final boolean equals(final ByteArray array) {
		return this == array || this.equals(array.array);
	}

	public final boolean equals(final byte[] array) {
		return Arrays.equals(this.array, array);
	}
}
