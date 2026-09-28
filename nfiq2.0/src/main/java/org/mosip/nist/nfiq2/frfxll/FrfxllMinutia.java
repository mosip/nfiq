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
 * Extracted minutia in the ISO/IEC 19794-2 basic layout ({@code FRFXLL_Basic_19794_2_Minutia}).
 *
 * @param x       column in pixels at 500 ppi (unsigned 16-bit)
 * @param y       row in pixels at 500 ppi (unsigned 16-bit)
 * @param angle   direction, 256 steps per turn, counter-clockwise from the positive x axis
 * @param type    0 other, 1 ridge ending, 2 bifurcation
 * @param quality quality 0..100
 */
public record FrfxllMinutia(int x, int y, int angle, int type, int quality) {
}
