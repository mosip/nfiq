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
 * Separable symmetric / anti-symmetric FIR filter of odd length {@code 2k+1} ({@code conv9} and
 * {@code conv17} of {@code conv2.h}).
 * <p>
 * The horizontal part is stateful: {@link #apply(int)} consumes one sample and returns the response centred
 * {@code k} samples back. The vertical part ({@link #vert(Mem, int, int)}) is evaluated directly on an
 * image column.
 */
final class Conv {
	/** Taps {@code t0..tk}; {@code t0} is the centre tap. */
	private final int[] taps;
	/** {@code true} to add mirrored samples, {@code false} to subtract them (odd filter). */
	private final boolean symmetric;
	/** Half length {@code k}. */
	private final int half;
	/** Circular history of the last {@code 2k} samples. */
	private final int[] buffer;
	/** Index of the oldest sample in {@link #buffer}. */
	private int pos;

	/**
	 * Creates a filter.
	 *
	 * @param forceSymmetric {@code true} for the explicit {@code symmetric} variant; otherwise the filter is
	 *                       symmetric when {@code t0 != 0} and anti-symmetric when {@code t0 == 0}
	 * @param taps           taps {@code t0..tk}
	 */
	Conv(boolean forceSymmetric, int... taps) {
		this.taps = taps;
		this.half = taps.length - 1;
		this.symmetric = forceSymmetric || taps[0] != 0;
		this.buffer = new int[2 * half];
	}

	/**
	 * Combines a mirrored pair of samples.
	 *
	 * @param x newer / lower sample
	 * @param y older / upper sample
	 * @return {@code x + y} or {@code x - y}
	 */
	private int addSub(int x, int y) {
		return symmetric ? x + y : x - y;
	}

	/**
	 * Returns the history sample {@code j} positions after the oldest one.
	 *
	 * @param j offset in {@code [0, 2k)}
	 * @return sample
	 */
	private int p(int j) {
		int i = pos + j;
		return buffer[i >= buffer.length ? i - buffer.length : i];
	}

	/**
	 * Appends a sample to the history without computing a response.
	 *
	 * @param v sample
	 */
	void add(int v) {
		buffer[pos] = v;
		if (++pos >= buffer.length) {
			pos = 0;
		}
	}

	/**
	 * Consumes a sample and returns the filter response centred {@code k} samples back.
	 *
	 * @param v new sample
	 * @return response (not normalised)
	 */
	int apply(int v) {
		int len = buffer.length;
		int out = p(half) * taps[0] + addSub(v, p(0)) * taps[half];
		for (int j = 1; j < half; j++) {
			out += addSub(p(len - j), p(j)) * taps[half - j];
		}
		add(v);
		return out;
	}

	/**
	 * Vertical response at {@code offset} of an image with the given row width.
	 *
	 * @param img    image memory
	 * @param width  row width
	 * @param offset linear index of the centre sample
	 * @return response (not normalised)
	 */
	int vert(Mem img, int width, int offset) {
		int out = img.get(offset) * taps[0];
		for (int i = 1; i <= half; i++) {
			out += addSub(img.get(offset + i * width), img.get(offset - i * width)) * taps[i];
		}
		return out;
	}
}
