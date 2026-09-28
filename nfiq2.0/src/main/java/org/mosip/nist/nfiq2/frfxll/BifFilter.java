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
 * Bifurcation / ridge-ending confirmation filter ({@code biffilt.h}, 17-tap variant).
 * <p>
 * A {@value #PATCH_WIDTH}x{@value #PATCH_WIDTH} patch of the phase map is sampled along the candidate's
 * direction, filtered with two banks of separable filters at five scales, and the strongest symmetric or
 * anti-symmetric response decides whether the candidate is a minutia, its type and whether its direction
 * must be flipped.
 */
final class BifFilter {
	/** Fraction bits of the sampling coordinates. */
	private static final int BITS = 7;
	/** Filter length. */
	private static final int N = 17;
	/** Response size. */
	private static final int M = 3;
	/** Number of scales. */
	private static final int SCALES = 5;
	/** Offset of the second response bank. */
	private static final int RESP2_OFFS = M * M * SCALES;
	/** Patch side. */
	static final int PATCH_WIDTH = N + M - 1;
	/** Minimum confidence of a confirmed minutia. */
	static final int THRESHOLD = 80;
	/** Maximum ratio (scaled by 256) between the mirrored and the best response. */
	static final int RATIO = 102;
	/** Confidence above which the minutia type is reported. */
	static final int TYPE_THRESHOLD = 105;
	/** Confidence weights of the 45 responses. */
	private static final int[] CONF_WEIGHT = { 16, 16, 16, 16, 16, 16, 16, 16, 16, 15, 15, 15, 15, 15, 15, 15, 15,
			15, 14, 14, 14, 14, 14, 14, 14, 14, 14, 13, 13, 13, 13, 13, 13, 13, 13, 13, 12, 12, 12, 12, 12, 12, 12, 12,
			12 };

	/** Phase map (with the orientation map appended). */
	private final Mem img;
	/** Phase map width. */
	private final int width;
	/** Phase map height. */
	private final int height;
	/** Sampled patch. */
	private final byte[] patch = new byte[PATCH_WIDTH * PATCH_WIDTH];
	/** Filter responses: first bank, then second bank. */
	private final int[] resp = new int[2 * RESP2_OFFS];

	/** Confidence of the last evaluation. */
	int confidence;
	/** {@code true} for a ridge ending, {@code false} for a bifurcation. */
	boolean type;
	/** {@code true} when the direction must be rotated by 180 degrees; kept from the last confirmed call. */
	boolean rotate180;

	/**
	 * Creates a filter over a phase map.
	 *
	 * @param width  phase map width
	 * @param size   phase map size
	 * @param img    phase map memory
	 */
	BifFilter(int width, int size, Mem img) {
		this.width = width;
		this.height = size / width;
		this.img = img;
	}

	/**
	 * Bilinear sample of the phase map at fixed-point coordinates.
	 *
	 * @param x column times 128
	 * @param y row times 128
	 * @return sample, or the filler outside the map
	 */
	private int sample(long x, long y) {
		if (x < 0 || y < 0) {
			return Freeman.PHASEMAP_FILLER;
		}
		long x0 = x >> BITS;
		long y0 = y >> BITS;
		if (y0 > height - 1 || x0 > width - 1) {
			return Freeman.PHASEMAP_FILLER;
		}
		int scale = 1 << BITS;
		int wx1 = (int) (x & (scale - 1));
		int wy1 = (int) (y & (scale - 1));
		int wx0 = scale - wx1;
		int wy0 = scale - wy1;
		int p0 = (int) (x0 + y0 * width);
		int v = img.get(p0) * wx0 * wy0 + img.get(p0 + 1) * wx1 * wy0 + img.get(p0 + width) * wx0 * wy1
				+ img.get(p0 + width + 1) * wx1 * wy1;
		return IntMath.reduce(v, BITS * 2) & 0xFF;
	}

	/**
	 * Samples the patch around {@code (x, y)} rotated to direction {@code (c, s)}.
	 *
	 * @param x column
	 * @param y row
	 * @param c direction cosine (times 127)
	 * @param s direction sine (times 127)
	 */
	private void rotate(long x, long y, int c, int s) {
		x <<= BITS;
		y <<= BITS;
		x -= (long) (PATCH_WIDTH / 2) * (c - s);
		y -= (long) (PATCH_WIDTH / 2) * (s + c);
		for (int p = 0; p < patch.length; p += PATCH_WIDTH, x -= s, y += c) {
			long x0 = x;
			long y0 = y;
			for (int p0 = p; p0 < p + PATCH_WIDTH; ++p0, x0 += c, y0 += s) {
				patch[p0] = (byte) sample(x0, y0);
			}
		}
	}

	/**
	 * Runs one filter bank over the patch centre.
	 *
	 * @param cv  vertical filter
	 * @param ch  the five horizontal filters (fresh instances)
	 * @param out output offset in {@link #resp}
	 */
	private void convolutions(Conv cv, Conv[] ch, int out) {
		Mem pm = new Mem(patch, patch.length, null, 0, 0);
		int n2 = N / 2;
		int start = PATCH_WIDTH * n2;
		int end = start + PATCH_WIDTH * M;
		for (int p = start; p < end; p += PATCH_WIDTH) {
			int pi = p;
			int endline = p + N - 1;
			for (; pi < endline; pi++) {
				int bv = cv.vert(pm, PATCH_WIDTH, pi);
				for (Conv h : ch) {
					h.add(bv);
				}
			}
			endline += M;
			for (; pi < endline; pi++) {
				int bv = cv.vert(pm, PATCH_WIDTH, pi);
				for (Conv h : ch) {
					resp[out++] = h.apply(bv);
				}
			}
		}
	}

	/**
	 * Normalises a combined response.
	 *
	 * @param val response
	 * @return {@code round(val / 2^17)}
	 */
	private static int norm(int val) {
		return IntMath.reduce(val, 17);
	}

	/**
	 * Combines the two banks and decides whether the candidate is a minutia.
	 *
	 * @return {@code true} when confirmed
	 */
	private boolean butterfly() {
		int conf = 0;
		int mirr = 0;
		int pmx = 0;
		for (int r1 = 0, count = 0; r1 < RESP2_OFFS; r1++, count++) {
			int r2 = r1 + RESP2_OFFS;
			int sum = resp[r1] + resp[r2];
			int diff = resp[r1] - resp[r2];
			resp[r1] = sum;
			resp[r2] = diff;
			int asum = IntMath.reduce(Math.abs(sum) * CONF_WEIGHT[count], 4);
			int adif = IntMath.reduce(Math.abs(diff) * CONF_WEIGHT[count], 4);
			if (asum > conf) {
				conf = asum;
				mirr = adif;
				pmx = r1;
			}
			if (adif > conf) {
				conf = adif;
				mirr = asum;
				pmx = r2;
			}
		}
		conf = norm(conf);
		confidence = conf;
		mirr = norm(mirr);
		if (conf < THRESHOLD) {
			return false;
		}
		if (conf * RATIO < 256 * mirr) {
			return false;
		}
		rotate180 = pmx >= RESP2_OFFS;
		type = resp[pmx] > 0;
		return true;
	}

	/**
	 * Evaluates a minutia candidate.
	 *
	 * @param x column
	 * @param y row
	 * @param c direction cosine (times 127)
	 * @param s direction sine (times 127)
	 * @return {@code true} when the candidate is confirmed
	 */
	boolean apply(int x, int y, int c, int s) {
		confidence = 0;
		type = false;
		rotate(x, y, c, s);
		convolutions(new Conv(false, 50, 48, 41, 32, 23, 14, 8, 4, 2),
				new Conv[] { new Conv(true, 0, 55, -53, -61, 74, 3, -28, 7, 3),
						new Conv(true, 0, 42, 0, -83, 3, 63, -1, -25, 1),
						new Conv(true, 0, 29, 25, -50, -58, 22, 51, 5, -24),
						new Conv(true, -2, 16, 28, -14, -60, -41, 18, 41, 13),
						new Conv(true, 0, 13, 28, 13, -27, -50, -29, 14, 38) },
				0);
		convolutions(new Conv(false, 0, 48, 82, 96, 90, 72, 50, 30, 16),
				new Conv[] { new Conv(false, 52, -14, -32, 22, 6, -9, 1, 1, 0),
						new Conv(false, 44, 0, -36, 1, 19, 0, -7, 0, 2),
						new Conv(false, 37, 9, -28, -18, 12, 14, -1, -6, -1),
						new Conv(false, 31, 13, -17, -24, -5, 13, 11, 0, -5),
						new Conv(false, 26, 15, -7, -21, -16, 0, 11, 10, 3) },
				RESP2_OFFS);
		return butterfly();
	}
}
