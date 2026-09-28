package org.mosip.nist.nfiq1.util;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.Nist;

/**
 * Java versions of the C string-to-number routines ({@code strtol} / {@code strtoul}) and a byte-to-char
 * helper used when porting NIST C code that parses text and binary headers.
 * <p>
 * The parsers follow the classic BSD libc algorithm and work on NUL-terminated {@code char[]} buffers so the
 * C pointer arithmetic can be reproduced. Stateless and thread-safe.
 */
public final class StringUtil extends Nist {
	/**
	 * Parses a NUL-terminated character buffer as an integer in the given base, like C {@code strtoul()}.
	 * <p>
	 * Skips leading white space, accepts an optional {@code +}/{@code -} sign, and accepts a {@code 0x}/{@code 0X}
	 * prefix when {@code base} is 0 or 16. When {@code base} is 0 it is inferred: 16 for {@code 0x}, 8 for a
	 * leading {@code 0}, otherwise 10. Parsing stops at the first character that is not a valid digit for the
	 * base. On overflow the result saturates to {@code 0x80000000} (negative input) or {@code 0x7FFFFFFF}.
	 *
	 * @param cp     the NUL-terminated characters to parse
	 * @param endptr output (may be {@code null}): receives the unparsed remainder of the input, or the whole
	 *               input if no digits were consumed
	 * @param base   numeric base (2 to 36), or 0 to detect it from the prefix
	 * @return the parsed value (negated if a {@code -} sign was present)
	 */
	private static long strtoul(char[] cp, AtomicReference<String> endptr, int base) {
		String cpStr = new String(cp);
		long acc;
		int c;
		int cpIndex = 0;
		long cutoff;
		int neg = 0, any, cutlim;
		/*
		 * Skip white space and pick up leading +/- sign if any. If base is 0, allow 0x
		 * for hex and 0 for octal, else assume decimal; if base is already 16, allow
		 * 0x.
		 */
		do {
			c = cpStr.charAt(cpIndex++);
		} while (Character.isSpaceChar((char) c));

		if ((char) c == '-') {
			neg = 1;
			c = cpStr.charAt(cpIndex++);
		} else if (c == '+') {
			c = cpStr.charAt(cpIndex++);
		}

		if ((base == 0 || base == 16) && (char) c == '0'
				&& (cpStr.charAt(cpIndex) == 'x' || cpStr.charAt(cpIndex) == 'X')) {
			c = cpStr.charAt(cpIndex + 1);
			cpIndex += 2;
			base = 16;
		}
		if (base == 0) {
			base = c == '0' ? 8 : 10;
		}
		/*
		 * Compute the cutoff value between legal numbers and illegal numbers. That is
		 * the largest legal value, divided by the base. An input number that is greater
		 * than this value, if followed by a legal input character, is too big. One that
		 * is equal to this value may be valid or not; the limit between valid and
		 * invalid numbers is then based on the last digit. For instance, if the range
		 * for longs is [-2147483648..2147483647] and the input base is 10, cutoff will
		 * be set to 214748364 and cutlim to either 7 (neg==0) or 8 (neg==1), meaning
		 * that if we have accumulated a value > 214748364, or equal but the next digit
		 * is > 7 (or 8), the number is too big, and we will return a range error.
		 *
		 * Set any if any `digits' consumed; make it negative to indicate overflow.
		 */
		cutoff = neg == 1 ? -(long) 0x80000000 : 0x7FFFFFFF;
		cutlim = (int) (cutoff % base);
		cutoff /= base;
		for (acc = 0, any = 0;; c = cpStr.charAt(cpIndex++)) {
			if (isDigit((char) c))
				c -= '0';
			else if (Character.isAlphabetic(c))
				c -= Character.isUpperCase(c) ? 'A' - 10 : 'a' - 10;
			else
				break;

			if (c >= base)
				break;

			if (any < 0 || acc > cutoff || (acc == cutoff && c > cutlim))
				any = -1;
			else {
				any = 1;
				acc *= base;
				acc += c;
			}
		}

		if (any < 0) {
			acc = neg == 1 ? 0x80000000 : 0x7FFFFFFF;
		} else if (neg == 1)
			acc = -acc;

		if (endptr != null)
			endptr.set((any > 0 ? cpStr.substring(cpIndex - 1) : new String(cp)));

		return (acc);

	}

	/**
	 * Parses a NUL-terminated character buffer as a signed integer, like C {@code strtol()}.
	 * <p>
	 * A leading {@code -} is stripped and the value from {@link #strtoul(char[], AtomicReference, int)} is
	 * negated; otherwise parsing is delegated unchanged.
	 *
	 * @param cp   the NUL-terminated characters to parse
	 * @param ptr  output (may be {@code null}): receives the unparsed remainder of the input
	 * @param base numeric base (2 to 36), or 0 to detect it from the prefix
	 * @return the parsed signed value
	 */
	private static long strtol(char[] cp, AtomicReference<String> ptr, int base) {
		if (cp[0] == '-') {
			return -strtoul(subChars(cp, 1), ptr, base);
		}
		return strtoul(cp, ptr, base);
	}

	/**
	 * Tests whether a character is a hexadecimal digit ({@code 0-9}, {@code a-f}, {@code A-F}), like C
	 * {@code isxdigit()}.
	 *
	 * @param c the character to test
	 * @return {@code true} if {@code c} is a hexadecimal digit
	 */
	private static boolean isXDigit(char c) {
		return ('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
	}

	/**
	 * Tests whether a character is an ASCII decimal digit ({@code 0-9}), like C {@code isdigit()}.
	 *
	 * @param c the character to test
	 * @return {@code true} if {@code c} is between {@code '0'} and {@code '9'}
	 */
	private static boolean isDigit(char c) {
		return '0' <= c && c <= '9';
	}

	/**
	 * Returns a sub-range of a character array, emulating C pointer offsets.
	 * <p>
	 * With one index, returns the characters from {@code indexs[0]} to the end. With two or more, returns
	 * {@code indexs[1]} characters starting at {@code indexs[0]}. With none, returns {@code cp} itself.
	 *
	 * @param cp     the source array
	 * @param indexs the start offset, optionally followed by a length
	 * @return a copy of the requested range, or {@code cp} if no indexes are given
	 */
	private static char[] subChars(char[] cp, int... indexs) {
		if (indexs.length == 1) {
			return Arrays.copyOfRange(cp, indexs[0], cp.length);
		} else if (indexs.length > 1) {
			return Arrays.copyOfRange(cp, indexs[0], indexs[0] + indexs[1]);
		}
		return cp;
	}

	/**
	 * Parses a string as a signed integer in the given base, like C {@code strtol()}.
	 * <p>
	 * A NUL terminator is appended before delegating to the {@code char[]} implementation.
	 *
	 * @param str  the text to parse
	 * @param ptr  output (may be {@code null}): receives the unparsed remainder of the input
	 * @param base numeric base (2 to 36), or 0 to detect it from the prefix
	 * @return the parsed signed value
	 */
	public static long strtol(String str, AtomicReference<String> ptr, int base) {
		return strtol((str + "\0").toCharArray(), ptr, base);
	}

	/**
	 * Parses a string as an integer in the given base, like C {@code strtoul()}.
	 * <p>
	 * A NUL terminator is appended before delegating to the {@code char[]} implementation.
	 *
	 * @param str  the text to parse
	 * @param ptr  output (may be {@code null}): receives the unparsed remainder of the input
	 * @param base numeric base (2 to 36), or 0 to detect it from the prefix
	 * @return the parsed value
	 */
	public static long strtoul(String str, AtomicReference<String> ptr, int base) {
		return strtoul((str + "\0").toCharArray(), ptr, base);
	}

	/**
	 * Copies a byte range into a new char array, widening each byte to a {@code char}.
	 * <p>
	 * Bytes from {@code startIndex} (inclusive) to {@code endIndex} (exclusive) are copied. Nothing is copied
	 * (the result stays all zeros) if {@code array} is {@code null} or has fewer than {@code endIndex} bytes.
	 *
	 * @param array      the source bytes (may be {@code null})
	 * @param startIndex first byte index to copy (inclusive)
	 * @param endIndex   end byte index (exclusive)
	 * @param size       length of the returned array; must be at least {@code endIndex - startIndex}
	 * @return a new char array of length {@code size}
	 */
	public static char[] byteToCharArray(byte[] array, int startIndex, int endIndex, int size) {
		char[] ret = new char[size];
		if (array != null && array.length >= endIndex) {
			for (int i = 0; startIndex < endIndex; i++, startIndex++) {
				ret[i] = (char) array[startIndex];
			}
		}
		return ret;
	}
}