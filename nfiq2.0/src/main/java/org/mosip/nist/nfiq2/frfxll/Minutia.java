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
 * Internal minutia record ({@code Minutia} of {@code matchData.h}): 16-bit position, 8-bit angle,
 * confidence and type.
 */
final class Minutia {
	/** Type: other / unknown. */
	static final int TYPE_OTHER = 0;
	/** Type: ridge ending. */
	static final int TYPE_RIDGE_ENDING = 1;
	/** Type: bifurcation. */
	static final int TYPE_BIFURCATION = 2;

	/** Column (16-bit signed). */
	short x;
	/** Row (16-bit signed). */
	short y;
	/** Direction, 256 steps per turn. */
	int theta;
	/** Confidence (8-bit). */
	final int conf;
	/** One of the {@code TYPE_*} constants. */
	final int type;

	/**
	 * Creates a minutia.
	 *
	 * @param x     column
	 * @param y     row
	 * @param theta direction
	 * @param conf  confidence
	 * @param type  type
	 */
	Minutia(short x, short y, int theta, int conf, int type) {
		this.x = x;
		this.y = y;
		this.theta = theta;
		this.conf = conf;
		this.type = type;
	}

	/**
	 * Ordering of {@code CompareMinutiaByConfidence}: higher confidence first, then top to bottom, left to
	 * right and by angle. Equal minutiae compare as "greater", as in the original.
	 *
	 * @param o other minutia
	 * @return {@code true} when this minutia ranks before {@code o}
	 */
	boolean greater(Minutia o) {
		if (conf != o.conf) {
			return conf > o.conf;
		}
		if (y != o.y) {
			return y < o.y;
		}
		if (x != o.x) {
			return x < o.x;
		}
		if (theta != o.theta) {
			return theta < o.theta;
		}
		return true;
	}
}
