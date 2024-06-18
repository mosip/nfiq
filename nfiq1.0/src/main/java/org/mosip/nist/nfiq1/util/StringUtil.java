package org.mosip.nist.nfiq1.util;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.Nist;

public final class StringUtil extends Nist {
	/*
	 * Parses the C-string str, interpreting its content as an integral number of
	 * the specified base, which is returned as an value of type unsigned long int.
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

		cpIndex = 0;
		if ((char) c == '-') {
			neg = 1;
			c = cpStr.charAt(cpIndex++);
		} else if (c == '+') {
			c = cpStr.charAt(cpIndex++);
		}

		if ((base == 0 || base == 16) && (char) c == '0'
				&& (cpStr.charAt(cpIndex) == 'x' || cpStr.charAt(cpIndex) == 'X')) {
			c = cp[1];
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

	private static long strtol(char[] cp, AtomicReference<String> ptr, int base) {
		if (cp[0] == '-') {
			return -strtoul(subChars(cp, 1), ptr, base);
		}
		return strtoul(cp, ptr, base);
	}

	private static boolean isXDigit(char c) {
		return ('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
	}

	private static boolean isDigit(char c) {
		return '0' <= c && c <= '9';
	}

	private static char[] subChars(char[] cp, int... indexs) {
		if (indexs.length == 1) {
			return Arrays.copyOfRange(cp, indexs[0], cp.length);
		} else if (indexs.length > 1) {
			return Arrays.copyOfRange(cp, indexs[0], indexs[0] + indexs[1]);
		}
		return cp;
	}

	public static long strtol(String str, AtomicReference<String> ptr, int base) {
		return strtol((str + "\0").toCharArray(), ptr, base);
	}

	public static long strtoul(String str, AtomicReference<String> ptr, int base) {
		return strtoul((str + "\0").toCharArray(), ptr, base);
	}

	public static char[] byteToCharArray(byte[] array, int startIndex, int endIndex, int size) {
		char[] ret = new char[size];
		/* Verify if exist Character size bytes to get from index */
		if (array != null && array.length >= (startIndex + Character.BYTES)) {
			for (int i = 0; startIndex < endIndex; i++, startIndex++) {
				ret[i] = (char) array[startIndex];
			}
		}
		return ret;
	}
}