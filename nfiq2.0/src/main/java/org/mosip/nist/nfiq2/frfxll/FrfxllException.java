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

import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * Failure of the feature extractor, carrying the original {@code FRFXLL_RESULT} code. A MOSIP
 * {@link BaseCheckedException} whose error code is {@code FRFXLL-} followed by the result in hexadecimal.
 */
public class FrfxllException extends BaseCheckedException {
	/** Serialization version. */
	private static final long serialVersionUID = 1L;

	/** Fingerprint area too small, or fewer than 6 minutiae ({@code FRFXLL_ERR_FB_TOO_SMALL_AREA}). */
	public static final int ERR_FB_TOO_SMALL_AREA = 0x80048004;
	/** Unknown internal error ({@code FRFXLL_ERR_INTERNAL}). */
	public static final int ERR_INTERNAL = 0x8007054F;
	/** Invalid image ({@code FRFXLL_ERR_INVALID_IMAGE}). */
	public static final int ERR_INVALID_IMAGE = 0x85BA0022;

	/** {@code FRFXLL_RESULT} code. */
	private final int code;

	/**
	 * Creates an exception.
	 *
	 * @param code    {@code FRFXLL_RESULT} code
	 * @param message description
	 */
	public FrfxllException(int code, String message) {
		super(String.format("FRFXLL-%08X", code), message);
		this.code = code;
	}

	/**
	 * Returns the {@code FRFXLL_RESULT} code.
	 *
	 * @return code
	 */
	public int getCode() {
		return code;
	}
}
