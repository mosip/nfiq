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
 * Ridge phase map from steerable (Freeman-Adelson) filters ({@code freeman.h}).
 * <p>
 * Second and third derivative-of-Gaussian responses are steered along the local orientation; the phase of
 * the resulting quadrature pair, quantised to 16 levels, forms the phase map. Positions outside the
 * footprint hold {@link #PHASEMAP_FILLER}. The output is shifted by 4 rows and 4 columns relative to the
 * input, as in the original.
 */
final class Freeman {
	/** Phase map value marking positions outside the footprint. */
	static final int PHASEMAP_FILLER = 127;
	/** Filter half size. */
	private static final int FLT_SIZE2 = 4;

	/** Utility class; not instantiable. */
	private Freeman() {
	}

	/**
	 * {@code sincosnorm}: divides by 128 rounding half up.
	 *
	 * @param x value
	 * @return rounded {@code x / 128}
	 */
	private static int sincosnorm(int x) {
		return (x + 64) >> 7;
	}

	/**
	 * Computes the phase map.
	 *
	 * @param width image width
	 * @param size  image size in pixels
	 * @param in    enhanced image followed by the orientation map
	 * @param ori   smoothed orientation map
	 * @return phase map of {@code size} bytes
	 */
	static byte[] phasemap(int width, int size, Mem in, int[] ori) {
		final int oriScale = OrientationMap.ORI_SCALE;
		int oriWidth = width / oriScale;
		byte[] out = new byte[size];
		int p = (width + 1) * FLT_SIZE2;
		int p2 = 0;

		Conv x20 = new Conv(false, -112, -7, 48, 14, 1);
		Conv x22 = new Conv(false, 122, 78, 20, 2, 0);
		Conv x33 = new Conv(false, 122, 78, 20, 2, 0);
		Conv x21 = new Conv(false, 0, 71, 37, 6, 0);
		Conv x30 = new Conv(false, 0, -92, -12, 8, 1);
		Conv x32 = new Conv(false, 0, 52, 27, 4, 0);
		Conv x31 = new Conv(false, -90, -23, 21, 7, 1);

		for (int y = oriWidth; y < size / (oriScale * oriScale) - oriWidth; y += oriWidth) {
			for (int i = oriScale; i > 0; --i) {
				for (int x = 0; x < oriWidth; ++x) {
					int o = ori[y + x];
					int a10 = IntMath.re(o);
					int a11 = IntMath.im(o);

					int a20 = sincosnorm(a10 * a10);
					int a21 = sincosnorm(2 * a10 * a11);
					int a22 = sincosnorm(a11 * a11);

					int a30 = sincosnorm(a10 * a20);
					int a31 = sincosnorm(3 * a20 * a11);
					int a32 = sincosnorm(3 * a22 * a10);
					int a33 = sincosnorm(a11 * a22);

					for (int j = FLT_SIZE2; j > 0; ++p, ++p2, --j) {
						int v20 = x20.apply(x22.vert(in, width, p));
						int v21 = x21.apply(x21.vert(in, width, p));
						int v22 = x22.apply(x20.vert(in, width, p));

						int v30 = x30.apply(x33.vert(in, width, p));
						int v31 = x31.apply(x32.vert(in, width, p));
						int v32 = x32.apply(x31.vert(in, width, p));
						int v33 = x33.apply(x30.vert(in, width, p));

						int xx2 = a20 * v20 + a21 * v21 + a22 * v22;
						int xx3 = a30 * v30 + a31 * v31 + a32 * v32 + a33 * v33;
						xx2 = (xx2 + 1024) >> 11;
						xx3 = (xx3 + 1024) >> 11;
						out[p2] = (byte) (xx2 != 0 ? ((127 - IntMath.re(IntMath.octSign(xx2, xx3, 0))) & 0xF0)
								: PHASEMAP_FILLER);
					}
				}
			}
		}
		for (int i = width * FLT_SIZE2 * 2; i > 0; --i) {
			out[p2++] = (byte) PHASEMAP_FILLER;
		}
		return out;
	}
}
