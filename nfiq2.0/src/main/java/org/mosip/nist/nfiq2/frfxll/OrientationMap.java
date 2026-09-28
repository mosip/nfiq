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

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * Orientation map and fingerprint footprint estimation ({@code orimap.h}).
 * <p>
 * The orientation map has one packed {@code complex<int8>} entry per {@value #ORI_SCALE}x{@value #ORI_SCALE}
 * image block; the footprint has one 0/1 byte per block.
 */
final class OrientationMap {
	/** Image pixels per orientation map cell, in each direction. */
	static final int ORI_SCALE = 4;
	/** Typical background value used to prime the delay lines. */
	private static final int FILLER = 255;

	/** Utility class; not instantiable. */
	private OrientationMap() {
	}

	/**
	 * Computes the orientation map and/or the footprint of an image at 333 dpi.
	 *
	 * @param width            image width, a multiple of {@value #ORI_SCALE}
	 * @param size             image size in pixels, height a multiple of {@value #ORI_SCALE}
	 * @param img              image pixels
	 * @param computeFootprint {@code true} to (re)compute {@code footprint}
	 * @param ori              orientation map output, or {@code null} to skip it
	 * @param footprint        footprint, output when {@code computeFootprint}, otherwise input
	 */
	static void orientationMapAndFootprint(int width, int size, byte[] img, boolean computeFootprint, int[] ori,
			byte[] footprint) {
		int oriWidth = width / ORI_SCALE;
		int oriSize = size / ORI_SCALE / ORI_SCALE;
		rawOrimap(width, size, img, computeFootprint, ori, footprint);
		if (computeFootprint) {
			boxfilt(oriWidth, oriSize, footprint, 3, t -> t > 3 ? 1 : 0);
			boxfilt(oriWidth, oriSize, footprint, 5, t -> t > 11 ? 1 : 0);
			boxfilt(oriWidth, oriSize, footprint, 5, t -> t > 11 ? 1 : 0);
			fillHoles(1, oriWidth, oriWidth, oriSize, footprint);
			fillHoles(oriWidth, oriSize, 1, oriWidth, footprint);
			boxfilt(oriWidth, oriSize, footprint, 5, t -> t > 14 ? 1 : 0);
			boxfilt(oriWidth, oriSize, footprint, 5, t -> t > 14 ? 1 : 0);
		}
		if (ori != null) {
			smoothOrimap(oriWidth, oriSize, ori, footprint, (re, im) -> IntMath.octSign(re, im, 128));
			smoothOrimap(oriWidth, oriSize, ori, footprint, OrientationMap::div2);
		}
	}

	/**
	 * Halves the angle of a complex number and returns the unit vector at that angle.
	 *
	 * @param re real part
	 * @param im imaginary part
	 * @return packed {@code (cos(a/2), sin(a/2))}
	 */
	static int div2(int re, int im) {
		int a = IntMath.atan2(re, im) / 2;
		return IntMath.pack(IntMath.cos(a), IntMath.sin(a));
	}

	/**
	 * Raw (unsmoothed) orientation estimate from second-order derivative filters.
	 *
	 * @param width            image width
	 * @param size             image size in pixels
	 * @param img              image pixels
	 * @param computeFootprint {@code true} to write the raw footprint
	 * @param ori              orientation output, or {@code null}
	 * @param footprint        footprint output
	 */
	static void rawOrimap(int width, int size, byte[] img, boolean computeFootprint, int[] ori, byte[] footprint) {
		int oriWidth = width / ORI_SCALE;
		int oriSize = size / (ORI_SCALE * ORI_SCALE);

		Delay x100 = new Delay(width + 1, FILLER * 2);
		Delay x102 = new Delay(width + 1, FILLER * 5);
		Delay x103 = new Delay(width - 1, FILLER * 10);
		int p0 = (width + 1) * 2 + 4;
		int p1 = p0 + width - 1;

		Delay x10c = new Delay(width, FILLER * 5);
		Delay x101 = new Delay(width, FILLER * 25);
		Delay x201 = new Delay(width);
		Delay x221 = new Delay(width);
		Delay x0 = new Delay(1);
		Delay x10 = new Delay(1, FILLER * 50);
		Delay x11 = new Delay(1);
		Delay x20 = new Delay(1);
		Delay x21 = new Delay(1);
		Delay x22 = new Delay(1);
		Delay domRe = new Delay(1);
		Delay domIm = new Delay(1);

		int[] magRe = new int[oriWidth];
		int[] magIm = new int[oriWidth];
		for (int y = 0; y < oriSize - oriWidth; y += oriWidth) {
			java.util.Arrays.fill(magRe, 0);
			java.util.Arrays.fill(magIm, 0);
			for (int i = ORI_SCALE; i > 0; --i) {
				for (int x = 0; x < oriWidth; ++x) {
					int zr = 0;
					int zi = 0;
					for (int j = ORI_SCALE; j > 0; ++p0, ++p1, --j) {
						int a0 = img[p0] & 0xFF;
						short cur = (short) (a0 + (img[p1] & 0xFF));
						short v1 = (short) (x100.apply(cur) + cur + x0.apply(a0));
						short v2 = (short) (x102.apply(v1) + v1);
						v1 = (short) (x103.apply(v2) + v2 + x10c.apply(v1));
						short v1x = (short) x101.apply(v1);
						short v10 = (short) (v1x + v1);
						v10 = (short) (x10.apply(v10) - v10);
						short v11 = (short) (v1x - v1);
						v11 = (short) (x11.apply(v11) + v11);

						short v10x = (short) x201.apply(v10);
						short v20 = (short) (v10x + v10);
						v20 = (short) (x20.apply(v20) - v20);
						short v21 = (short) (v10x - v10);
						v21 = (short) (x21.apply(v21) + v21);
						short v22 = (short) (x221.apply(v11) - v11);
						v22 = (short) (x22.apply(v22) + v22);

						int g20 = v20 + v22;
						int g21 = v20 - v22;
						int g22 = 2 * v21;
						zr += g20 * g21;
						zi += g20 * g22;
					}
					magRe[x] += zr;
					magIm[x] += zi;
				}
			}
			for (int x = 0; x < oriWidth; ++x) {
				int o = IntMath.octSign(domRe.apply(magRe[x]), domIm.apply(magIm[x]), 50000);
				if (computeFootprint) {
					footprint[x + y] = (byte) (o != 0 ? 1 : 0);
				}
				if (ori != null) {
					ori[x + y] = o;
				}
			}
		}
		for (int x = oriSize - oriWidth; x < oriSize; ++x) {
			if (computeFootprint) {
				footprint[x] = 0;
			}
			if (ori != null) {
				ori[x] = 0;
			}
		}
	}

	/**
	 * Smooths the orientation map with two cascaded 5x5 box filters and applies {@code postproc}; cells
	 * outside the footprint are cleared.
	 *
	 * @param width     orientation map width
	 * @param size      orientation map size
	 * @param ori       orientation map, smoothed in place
	 * @param footprint footprint
	 * @param postproc  maps the smoothed {@code complex<int32>} to a packed {@code complex<int8>}
	 */
	static void smoothOrimap(int width, int size, int[] ori, byte[] footprint, IntBinaryOperator postproc) {
		final int n = 5;
		final int n1 = n - 1;
		short[] o1Re = new short[width];
		short[] o1Im = new short[width];
		short[] o2Re = new short[width];
		short[] o2Im = new short[width];
		Delay od1Re = new Delay(n * width);
		Delay od1Im = new Delay(n * width);
		Delay od2Re = new Delay(n * width);
		Delay od2Im = new Delay(n * width);
		for (int y = 0; y < size + n1 * width; y += width) {
			Delay od3Re = new Delay(n);
			Delay od3Im = new Delay(n);
			Delay od4Re = new Delay(n);
			Delay od4Im = new Delay(n);
			int o3Re = 0;
			int o3Im = 0;
			int o4Re = 0;
			int o4Im = 0;
			for (int x = 0; x < width + n1; ++x) {
				int o2tRe = 0;
				int o2tIm = 0;
				if (x < width) {
					int v = y < size ? ori[y + x] : 0;
					o1Re[x] = (short) (o1Re[x] + IntMath.re(v));
					o1Im[x] = (short) (o1Im[x] + IntMath.im(v));
					short dRe = (short) (o1Re[x] - od1Re.apply(o1Re[x]));
					short dIm = (short) (o1Im[x] - od1Im.apply(o1Im[x]));
					o2Re[x] = (short) (o2Re[x] + dRe);
					o2Im[x] = (short) (o2Im[x] + dIm);
					o2tRe = (short) (o2Re[x] - od2Re.apply(o2Re[x]));
					o2tIm = (short) (o2Im[x] - od2Im.apply(o2Im[x]));
				}
				if (y >= n1 * width) {
					o3Re += o2tRe;
					o3Im += o2tIm;
					o4Re += o3Re - od3Re.apply(o3Re);
					o4Im += o3Im - od3Im.apply(o3Im);
					if (x < n1) {
						od4Re.apply(o4Re);
						od4Im.apply(o4Im);
					} else {
						o2tRe = o4Re - od4Re.apply(o4Re);
						o2tIm = o4Im - od4Im.apply(o4Im);
						int pos = y + x - (width + 1) * n1;
						ori[pos] = footprint[pos] != 0 ? postproc.applyAsInt(o2tRe, o2tIm) : 0;
					}
				}
			}
		}
	}

	/**
	 * In-place box filter of an 8-bit map followed by a point function; sums wrap at 8 bits as in the
	 * original.
	 *
	 * @param width   map width
	 * @param size    map size
	 * @param inout   map, filtered in place
	 * @param boxsize box side, odd
	 * @param f       point function applied to each box sum
	 */
	static void boxfilt(int width, int size, byte[] inout, int boxsize, IntUnaryOperator f) {
		final int n = boxsize;
		final int n2 = boxsize / 2;
		int[] s1 = new int[width];
		Delay d1 = new Delay(n * width);
		for (int y = 0; y < size + n2 * width; y += width) {
			Delay d2 = new Delay(n);
			int s2 = 0;
			for (int x = 0; x < width + n2; ++x) {
				int t = 0;
				if (x < width) {
					int v = y < size ? inout[y + x] & 0xFF : 0;
					s1[x] = (s1[x] + v) & 0xFF;
					s1[x] = (s1[x] - d1.apply(v)) & 0xFF;
					t = s1[x];
				}
				if (y >= n2 * width) {
					s2 = (s2 + t) & 0xFF;
					if (x < n2) {
						d2.apply(t);
					} else {
						s2 = (s2 - d2.apply(t)) & 0xFF;
						t = f.applyAsInt(s2);
						inout[y - (width + 1) * n2 + x] = (byte) t;
					}
				}
			}
		}
	}

	/**
	 * Fills the gaps between the first and last set cells of every line.
	 *
	 * @param strideX   step between cells of a line
	 * @param sizeX     extent of a line
	 * @param strideY   step between lines
	 * @param sizeY     extent of the lines
	 * @param footprint map, modified in place
	 */
	static void fillHoles(int strideX, int sizeX, int strideY, int sizeY, byte[] footprint) {
		for (int y = 0; y < sizeY; y += strideY) {
			int x1;
			for (x1 = 0; x1 < sizeX; x1 += strideX) {
				if (footprint[y + x1] != 0) {
					break;
				}
			}
			int x2;
			for (x2 = sizeX - strideX; x2 > x1; x2 -= strideX) {
				if (footprint[y + x2] != 0) {
					break;
				}
			}
			for (int x = x1 + strideX; x < x2; x += strideX) {
				footprint[y + x] = 1;
			}
		}
	}
}
