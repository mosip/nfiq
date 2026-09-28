/*
 * FingerJetFX OSE -- Fingerprint Feature Extractor, Open Source Edition
 *
 * Copyright (c) 2011 by DigitalPersona, Inc. All rights reserved.
 *
 * DigitalPersona, FingerJet, and FingerJetFX are registered trademarks or trademarks of DigitalPersona, Inc.
 * in the United States and other countries.
 *
 * FingerJetFX OSE is open source software that you may modify and/or redistribute under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version, provided that the conditions specified in the
 * COPYRIGHT.txt file provided with this software are met.
 *
 * This is a MODIFIED version of the Digital Persona FingerJetFX OSE fingerprint feature extractor:
 * a Java translation of libFRFXLL (as patched by NIST for NFIQ 2), maintained by MOSIP.
 *
 * For more information, please visit digitalpersona.com/fingerjetfx.
 */
package org.mosip.nist.nfiq2.frfxll;

/**
 * Integer helpers of the extractor ({@code intmath.h} and {@code complex.h}): 8-bit trigonometry, rounding
 * shifts and divisions, and the octagonal complex magnitude / sign used by the orientation estimators.
 * <p>
 * Complex numbers with 8-bit parts ({@code complex<int8>}) are packed into one {@code int}: the real part
 * in bits 0..7 and the imaginary part in bits 8..15. Use {@link #re(int)} and {@link #im(int)} to unpack.
 */
final class IntMath {
	/** Sine table for a full turn of 256 steps, first half only, scaled by 127. */
	private static final int[] SIN_TABLE = { 0, 3, 6, 9, 12, 15, 19, 22, 25, 28, 31, 34, 37, 40, 43, 46, 49, 51,
			54, 57, 60, 63, 65, 68, 71, 73, 76, 78, 81, 83, 85, 88, 90, 92, 94, 96, 98, 100, 102, 104, 106, 107, 109,
			111, 112, 113, 115, 116, 117, 118, 120, 121, 122, 122, 123, 124, 125, 125, 126, 126, 126, 127, 127, 127,
			127, 127, 127, 127, 126, 126, 126, 125, 125, 124, 123, 122, 122, 121, 120, 118, 117, 116, 115, 113, 112,
			111, 109, 107, 106, 104, 102, 100, 98, 96, 94, 92, 90, 88, 85, 83, 81, 78, 76, 73, 71, 68, 65, 63, 60, 57,
			54, 51, 49, 46, 43, 40, 37, 34, 31, 28, 25, 22, 19, 15, 12, 9, 6, 3 };

	/** Arc-tangent table for the first octant (97 entries, the last one is the 45 degree point). */
	private static final int[] ATAN_TABLE = { 0, 1, 1, 2, 2, 3, 3, 3, 4, 4, 5, 5, 6, 6, 6, 7, 7, 8, 8, 8, 9, 9, 10,
			10, 10, 11, 11, 12, 12, 12, 13, 13, 13, 14, 14, 15, 15, 15, 16, 16, 16, 17, 17, 18, 18, 18, 19, 19, 19,
			20, 20, 20, 21, 21, 21, 22, 22, 22, 22, 23, 23, 23, 24, 24, 24, 25, 25, 25, 25, 26, 26, 26, 27, 27, 27, 27,
			28, 28, 28, 28, 29, 29, 29, 29, 30, 30, 30, 30, 31, 31, 31, 31, 31, 32, 32, 32, 32 };

	/** Number of arc-tangent table steps per octant. */
	private static final int NUM_ATAN_ENTRIES = 96;

	/** Utility class; not instantiable. */
	private IntMath() {
	}

	/**
	 * Sine of an 8-bit angle (256 steps per turn), scaled by 127.
	 *
	 * @param a angle, only the low 8 bits are used
	 * @return sine in [-127, 127]
	 */
	static int sin(int a) {
		int u = a & 0xFF;
		return ((u & 0x80) != 0 ? -1 : 1) * SIN_TABLE[u & 0x7F];
	}

	/**
	 * Cosine of an 8-bit angle (256 steps per turn), scaled by 127.
	 *
	 * @param a angle, only the low 8 bits are used
	 * @return cosine in [-127, 127]
	 */
	static int cos(int a) {
		return sin((a & 0xFF) + 64);
	}

	/**
	 * Table-driven arc-tangent returning an 8-bit angle (256 steps per turn).
	 *
	 * @param c cosine (x) component
	 * @param s sine (y) component
	 * @return angle in [0, 255]
	 */
	static int atan2(int c, int s) {
		boolean sn = s < 0;
		if (sn) {
			s = -s;
		}
		boolean cn = c < 0;
		if (cn) {
			c = -c;
		}
		boolean cls = c < s;
		if (cls) {
			int t = c;
			c = s;
			s = t;
		}
		if (c == 0) {
			return 0;
		}
		int out = ATAN_TABLE[s * NUM_ATAN_ENTRIES / c];
		if (cls) {
			out = 0x40 - out;
		}
		if (cn) {
			out = 0x80 - out;
		}
		if (sn) {
			out = -out;
		}
		return out & 0xFF;
	}

	/**
	 * Divides by {@code 2^n} rounding half up: {@code (a + 2^(n-1)) >> n}.
	 *
	 * @param a value
	 * @param n number of bits, at least 1
	 * @return rounded quotient
	 */
	static int reduce(int a, int n) {
		return (a + (1 << (n - 1))) >> n;
	}

	/**
	 * Signed division rounding half away from zero.
	 *
	 * @param x dividend
	 * @param y divisor, not 0
	 * @return rounded quotient
	 */
	static int divide(int x, int y) {
		int t = ((x >= 0) != (y > 0)) ? -1 : 1;
		x = Math.abs(x);
		y = Math.abs(y);
		return (x + (y >> 1)) / y * t;
	}

	/**
	 * {@code x * y / z} for 16-bit signed {@code x}, computed in 32 bits with {@link #divide(int, int)} and
	 * truncated back to 16 bits.
	 *
	 * @param x 16-bit value
	 * @param y multiplier
	 * @param z divisor
	 * @return {@code (short) round(x * y / z)}
	 */
	static short muldiv16(short x, int y, int z) {
		return (short) divide(x * y, z);
	}

	/**
	 * Octagonal approximation of the magnitude of a complex number.
	 *
	 * @param re real part
	 * @param im imaginary part
	 * @return {@code max(|re|, |im|, round((|re| + |im|) * 181 / 256))}
	 */
	static int octAbs(int re, int im) {
		int x = Math.abs(re);
		int y = Math.abs(im);
		int n = Math.max(x, y);
		return Math.max(n, reduce((x + y) * 181, 8));
	}

	/**
	 * Scales a complex number to 8-bit parts (octagonal normalisation), or 0 when its magnitude is at most
	 * {@code 127 * threshold}.
	 *
	 * @param re        real part
	 * @param im        imaginary part
	 * @param threshold magnitude threshold divided by 127
	 * @return packed {@code complex<int8>}
	 */
	static int octSign(int re, int im, int threshold) {
		int x = Math.abs(re);
		int y = Math.abs(im);
		int n = Math.max(x, y) / 127;
		if (n <= threshold) {
			return 0;
		}
		n = Math.max(n, (x + y) / 180);
		return pack(re / n, im / n);
	}

	/**
	 * Packs two values truncated to 8 bits into a {@code complex<int8>}.
	 *
	 * @param re real part
	 * @param im imaginary part
	 * @return packed value
	 */
	static int pack(int re, int im) {
		return (re & 0xFF) | ((im & 0xFF) << 8);
	}

	/**
	 * Real part of a packed {@code complex<int8>}.
	 *
	 * @param c packed value
	 * @return signed 8-bit real part
	 */
	static int re(int c) {
		return (byte) c;
	}

	/**
	 * Imaginary part of a packed {@code complex<int8>}.
	 *
	 * @param c packed value
	 * @return signed 8-bit imaginary part
	 */
	static int im(int c) {
		return (byte) (c >> 8);
	}
}
