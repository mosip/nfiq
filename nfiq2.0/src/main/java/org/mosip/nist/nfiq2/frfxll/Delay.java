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

import java.util.Arrays;

/**
 * Fixed-length delay line ({@code delay.h}): each call stores a value and returns the one stored {@code dt}
 * calls earlier. Values are plain {@code int}s; callers apply the narrowing of the original element type.
 */
final class Delay {
	/** Circular storage of the last {@code dt} values. */
	private final int[] buffer;
	/** Index of the oldest value. */
	private int ptr;

	/**
	 * Creates a delay line filled with {@code init}.
	 *
	 * @param dt   delay in calls, at least 1
	 * @param init initial content
	 */
	Delay(int dt, int init) {
		buffer = new int[dt];
		Arrays.fill(buffer, init);
	}

	/**
	 * Creates a zero-filled delay line.
	 *
	 * @param dt delay in calls, at least 1
	 */
	Delay(int dt) {
		this(dt, 0);
	}

	/**
	 * Pushes a value and returns the one pushed {@code dt} calls ago.
	 *
	 * @param in new value
	 * @return delayed value
	 */
	int apply(int in) {
		int out = buffer[ptr];
		buffer[ptr] = in;
		if (++ptr >= buffer.length) {
			ptr = 0;
		}
		return out;
	}
}
