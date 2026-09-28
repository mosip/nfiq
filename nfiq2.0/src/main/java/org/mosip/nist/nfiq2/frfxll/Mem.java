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
 * Read-only view of the extractor's working buffer as unsigned bytes.
 * <p>
 * The original code keeps the image, the orientation map and the footprint in one contiguous buffer, and a
 * few filters read a little past the end of the image into the orientation map. This class reproduces that
 * layout (image bytes followed by the packed orientation map) and returns a fixed filler value for
 * positions outside the buffer instead of reading unrelated memory.
 */
final class Mem {
	/** Buffer content. */
	private final byte[] data;
	/** Value returned for positions outside {@link #data}. */
	private final int filler;

	/**
	 * Creates a view over {@code image} followed by the orientation map.
	 *
	 * @param image   image bytes
	 * @param size    number of image bytes to use
	 * @param ori     packed orientation map ({@code complex<int8>} per entry), may be {@code null}
	 * @param oriSize number of orientation entries to append
	 * @param filler  value returned outside the buffer
	 */
	Mem(byte[] image, int size, int[] ori, int oriSize, int filler) {
		int extra = ori == null ? 0 : 2 * oriSize;
		data = new byte[size + extra];
		System.arraycopy(image, 0, data, 0, size);
		for (int i = 0; i < extra / 2; i++) {
			data[size + 2 * i] = (byte) IntMath.re(ori[i]);
			data[size + 2 * i + 1] = (byte) IntMath.im(ori[i]);
		}
		this.filler = filler;
	}

	/**
	 * Returns the unsigned byte at a linear position.
	 *
	 * @param idx position, may be out of range
	 * @return value in [0, 255], or the filler outside the buffer
	 */
	int get(int idx) {
		return idx >= 0 && idx < data.length ? data[idx] & 0xFF : filler;
	}
}
