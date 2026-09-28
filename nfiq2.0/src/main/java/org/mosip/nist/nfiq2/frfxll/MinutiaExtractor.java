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

import java.util.ArrayList;
import java.util.List;

/**
 * Minutia candidate detection and confirmation on the phase map ({@code extract_minutia.h}).
 * <p>
 * Candidates are local maxima of a structure-tensor singularity measure; each one is confirmed with the
 * {@link BifFilter} along the local ridge direction and its angle is refined by following the ridge.
 */
final class MinutiaExtractor {
	/** Structure tensor threshold ({@code 0.1 * 13280}). */
	static final int TB = 1328;
	/** Size of the direction smoothing window. */
	private static final int ORIFILT_SIZE = 13;
	/** Row offset of the processing start in the phase map. */
	private static final int YOFFS = 3;
	/** Phase map value treated as invalid by the gradient step. */
	private static final int INVALID = 255;
	/** Column and row delay of {@link Conv2d3}. */
	private static final int CONV_OFFS = 1;
	/** Column and row delay of {@link Max2d5Fast}. */
	private static final int MAX_OFFS = 4;
	/** Neighbour offsets of the eight directions. */
	private static final int[] OFFS = { 0, 1, 1, 1, 0, -1, -1, -1 };

	/** Utility class; not instantiable. */
	private MinutiaExtractor() {
	}

	/** 3x3 binomial smoothing of a pixel stream ({@code conv2d3<stride, 2, 1, 5>}). */
	static final class Conv2d3 {
		/** One row delay. */
		private final Delay dv1;
		/** Second row delay. */
		private final Delay dv2;
		/** One column delay. */
		private final Delay dh1 = new Delay(1);
		/** Second column delay. */
		private final Delay dh2 = new Delay(1);

		/**
		 * Creates a smoother for rows of the given width.
		 *
		 * @param width row width
		 */
		Conv2d3(int width) {
			dv1 = new Delay(width);
			dv2 = new Delay(width);
		}

		/**
		 * Consumes one sample.
		 *
		 * @param v0 sample
		 * @return smoothed value delayed by one row and one column
		 */
		int apply(int v0) {
			int v1 = dv1.apply(v0);
			int v2 = dv2.apply(v1);
			int h0 = v1 * 2 + (v2 + v0);
			int h1 = dh1.apply(h0);
			int h2 = dh2.apply(h1);
			int o = h1 * 2 + (h2 + h0);
			return (o + (1 << 4)) >> 5;
		}
	}

	/** Streaming 5x5 local maximum detector ({@code max2d5fast}). */
	static final class Max2d5Fast {
		/** Last seven rows of samples. */
		private final int[][] buffer;
		/** Maxima flags per buffered row, indexed by column plus the delay. */
		private final boolean[][] mxset;

		/**
		 * Creates a detector for rows of the given width.
		 *
		 * @param width row width
		 */
		Max2d5Fast(int width) {
			buffer = new int[7][width];
			for (int[] row : buffer) {
				java.util.Arrays.fill(row, -1);
			}
			mxset = new boolean[7][width + MAX_OFFS];
		}

		/**
		 * Consumes one sample.
		 *
		 * @param v sample, negative outside the footprint
		 * @param x column
		 * @param y row
		 * @return {@code true} when the sample 4 rows and 4 columns back is a local maximum
		 */
		boolean apply(int v, int x, int y) {
			int yy = y % 7;
			buffer[yy][x] = v;
			if (x == 0 && y > 0) {
				java.util.Arrays.fill(mxset[(y - 1) % 7], false);
			}
			if ((y >= 6 && y % 3 == 0) && (x >= 6 && x % 3 == 0)) {
				findMaxInBlock(y - 4, x - 4);
			}
			if (mxset[yy][x]) {
				mxset[yy][x] = false;
				return true;
			}
			return false;
		}

		/**
		 * Finds the maximum of a 3x3 block and flags it when it also dominates its 5x5 neighbourhood.
		 *
		 * @param i block top row
		 * @param j block left column
		 */
		private void findMaxInBlock(int i, int j) {
			int mi = i;
			int mj = j;
			int mValue = buffer[mi % 7][mj];
			for (int i2 = i; i2 <= i + 2; i2++) {
				for (int j2 = j; j2 <= j + 2; j2++) {
					if (buffer[i2 % 7][j2] < 0) {
						return;
					}
					if (buffer[i2 % 7][j2] > mValue) {
						mi = i2;
						mj = j2;
						mValue = buffer[mi % 7][mj];
					}
				}
			}
			if (mValue <= 0) {
				return;
			}
			for (int i2 = mi - 2; i2 <= mi + 2; i2++) {
				for (int j2 = mj - 2; j2 <= mj + 2; j2++) {
					if ((i2 < i || i2 > i + 2 || j2 < j || j2 > j + 2)
							&& (buffer[i2 % 7][j2] < 0 || buffer[i2 % 7][j2] > mValue)) {
						return;
					}
				}
			}
			mxset[(mi + MAX_OFFS) % 7][mj + MAX_OFFS] = true;
		}
	}

	/**
	 * Tells whether a position and its 1-pixel neighbourhood corners lie inside the footprint.
	 *
	 * @param xp       column (negative values are outside)
	 * @param yp       row (negative values are outside)
	 * @param width    phase map width
	 * @param size     phase map size
	 * @param phasemap phase map
	 * @return {@code true} when inside
	 */
	static boolean isInFootprint(int xp, int yp, int width, int size, Mem phasemap) {
		if (xp < 0 || yp < 0) {
			return false;
		}
		long x = xp + 1L;
		long y = yp + 1L;
		long offs = x + y * width;
		if (!(x >= 2 && y >= 2 && x < width && offs < size)) {
			return false;
		}
		int o = (int) offs;
		return phasemap.get(o - 2) != Freeman.PHASEMAP_FILLER && phasemap.get(o) != Freeman.PHASEMAP_FILLER
				&& phasemap.get(o - (width * 2 + 2)) != Freeman.PHASEMAP_FILLER
				&& phasemap.get(o - width * 2) != Freeman.PHASEMAP_FILLER;
	}

	/** A point with a direction and the phase map value under it ({@code a_point}). */
	private static final class APoint {
		/** Column (16-bit). */
		final short x;
		/** Row (16-bit). */
		final short y;
		/** Direction, 8 steps per turn (8-bit). */
		final int a;
		/** Phase map value. */
		final int v;

		/**
		 * Creates a sentinel with a value only.
		 *
		 * @param v value
		 */
		APoint(int v) {
			this.x = 0;
			this.y = 0;
			this.a = 0;
			this.v = v;
		}

		/**
		 * Creates a point and reads its phase map value.
		 *
		 * @param x     column
		 * @param y     row
		 * @param a     direction
		 * @param p     phase map
		 * @param width phase map width
		 */
		APoint(int x, int y, int a, Mem p, int width) {
			this.x = (short) x;
			this.y = (short) y;
			this.a = a & 0xFF;
			this.v = p.get(this.x + this.y * width);
		}

		/**
		 * Moves to the darkest or brightest of the three neighbours ahead.
		 *
		 * @param p        phase map
		 * @param width    phase map width
		 * @param mn       {@code true} to pick the minimum, {@code false} for the maximum
		 * @param relative {@code true} to turn the direction with the step
		 * @return the chosen neighbour
		 */
		APoint next(Mem p, int width, boolean mn, boolean relative) {
			APoint out = new APoint(mn ? 255 : 0);
			for (int i = -1; i <= 1; ++i) {
				APoint cur = new APoint(x + OFFS[(a + i) & 7], y + OFFS[(a + i - 2) & 7],
						relative ? (a + i) & 0xFF : a, p, width);
				if (mn ? cur.v <= out.v : cur.v >= out.v) {
					out = cur;
				}
			}
			return out;
		}
	}

	/**
	 * Refines a minutia direction by following the ridge or valley that leaves it.
	 *
	 * @param a        initial direction (8-bit)
	 * @param xp       column
	 * @param yp       row
	 * @param p        phase map
	 * @param width    phase map width
	 * @param size     phase map size
	 * @param relative {@code true} to let the walk turn
	 * @return the refined direction, or -1 when the walk was too short
	 */
	static int adjustAngle(int a, int xp, int yp, Mem p, int width, int size, boolean relative) {
		APoint start = new APoint(xp, yp, a >> 5, p, width);
		APoint cur = start.next(p, width, false, relative);
		APoint p2 = start.next(p, width, true, relative);
		boolean mn = (255 - p2.v) > cur.v;
		if (mn) {
			cur = p2;
		}
		long d0 = 0;
		for (int i = 0; i < 20; ++i) {
			cur = cur.next(p, width, mn, relative);
			if (mn == (cur.v >= 128)) {
				break;
			}
			if (!isInFootprint(cur.x, cur.y, width, size, p)) {
				break;
			}
			int dx = (short) (cur.x - start.x);
			int dy = (short) (cur.y - start.y);
			long d = (dx * dx + dy * dy) & 0xFFFFFFFFL;
			if (d <= d0) {
				break;
			}
			d0 = d;
			if (d > 400) {
				break;
			}
			if (i == 0) {
				p2 = cur;
			}
		}
		if (d0 >= 14 * 14) {
			int bx = (short) (cur.x - p2.x);
			int by = (short) (cur.y - p2.y);
			return IntMath.atan2(-by, bx);
		}
		return -1;
	}

	/**
	 * Detects and confirms minutiae.
	 *
	 * @param phasemap phase map (with the orientation map appended)
	 * @param width    phase map width
	 * @param size     phase map size
	 * @return confirmed minutiae in detection order, positions in phase map coordinates
	 */
	static List<Minutia> extract(Mem phasemap, int width, int size) {
		List<Minutia> found = new ArrayList<>();
		int p = width * YOFFS;
		int end = size - width;
		int height = size / width;
		Conv2d3 cxx = new Conv2d3(width);
		Conv2d3 cxy = new Conv2d3(width);
		Conv2d3 cyy = new Conv2d3(width);
		Max2d5Fast max5 = new Max2d5Fast(width);
		Delay delayMc = new Delay(width * (ORIFILT_SIZE - MAX_OFFS) - MAX_OFFS);
		BifFilter bf = new BifFilter(width, size, phasemap);

		Delay delayOriYRe = new Delay(ORIFILT_SIZE * width / 2);
		Delay delayOriYIm = new Delay(ORIFILT_SIZE * width / 2);
		short[] oriS1Re = new short[width / 2];
		short[] oriS1Im = new short[width / 2];
		int[] direction = new int[width / 2];
		for (int y = 0; y < height + ORIFILT_SIZE; y++) {
			for (int x = 0; x < width; p++, x++) {
				boolean outside = p >= end;
				int gx = 0;
				int gy = 0;
				if (!outside) {
					outside = phasemap.get(p + 1) == INVALID || phasemap.get(p - 3) == INVALID
							|| phasemap.get(p + width) == INVALID || phasemap.get(p - 3 * width) == INVALID;
					gx = phasemap.get(p + 1) - phasemap.get(p - 1);
					gy = phasemap.get(p + width) - phasemap.get(p - width);
				}
				int gxx = cxx.apply(gx * gx);
				int gxy = cxy.apply(gx * gy);
				int gyy = cyy.apply(gy * gy);
				boolean e1b = gxx + gyy > 2 * TB;
				int b2 = (TB - gxx) * (TB - gyy) - gxy * gxy;
				boolean e = e1b && b2 > 0;
				if ((x & 1) == 0 && (y & 1) == 0) {
					int ori = IntMath.octSign(gxx - gyy, 2 * gxy, 0);
					int s = x / 2;
					oriS1Re[s] = (short) (oriS1Re[s] + IntMath.re(ori));
					oriS1Im[s] = (short) (oriS1Im[s] + IntMath.im(ori));
					oriS1Re[s] = (short) (oriS1Re[s] - delayOriYRe.apply(IntMath.re(ori)));
					oriS1Im[s] = (short) (oriS1Im[s] - delayOriYIm.apply(IntMath.im(ori)));
				}
				boolean mc = delayMc.apply(max5.apply(outside ? -1 : e ? b2 : 0, x, y) ? 1 : 0) != 0;
				int xp = x - CONV_OFFS;
				int yp = y + YOFFS - CONV_OFFS - ORIFILT_SIZE;
				if (mc && isInFootprint(xp, yp, width, size, phasemap)) {
					candidate(found, bf, direction[x / 2], xp, yp, phasemap, width, size);
				}
			}
			if ((y & 1) == 0) {
				smoothDirection(oriS1Re, oriS1Im, direction, width);
			}
		}
		return found;
	}

	/**
	 * Confirms one candidate and records it.
	 *
	 * @param found     output list
	 * @param bf        confirmation filter
	 * @param direction local ridge direction (8-bit, half turn)
	 * @param xp        column
	 * @param yp        row
	 * @param phasemap  phase map
	 * @param width     phase map width
	 * @param size      phase map size
	 */
	private static void candidate(List<Minutia> found, BifFilter bf, int direction, int xp, int yp, Mem phasemap,
			int width, int size) {
		int confidence = 0;
		boolean confirmed = false;
		boolean type = false;
		int a = 0;
		for (int i = -2; i <= 2; i += 4) {
			int a0 = (direction + i) & 0xFF;
			if (bf.apply(xp, yp, IntMath.cos(a0), IntMath.sin(a0))) {
				confirmed = true;
				if (bf.confidence > confidence) {
					confidence = bf.confidence;
					a = a0;
					type = bf.type;
				}
			}
		}
		if (!confirmed) {
			return;
		}
		if (bf.rotate180) {
			a = (a + 128) & 0xFF;
		}
		int adjusted = adjustAngle(a, xp, yp, phasemap, width, size, false);
		if (adjusted < 0) {
			adjusted = adjustAngle(a, xp, yp, phasemap, width, size, true);
		}
		if (adjusted >= 0) {
			a = adjusted;
		}
		int kind;
		if (confidence > BifFilter.TYPE_THRESHOLD) {
			kind = type ? Minutia.TYPE_RIDGE_ENDING : Minutia.TYPE_BIFURCATION;
		} else {
			kind = Minutia.TYPE_OTHER;
		}
		found.add(new Minutia((short) xp, (short) yp, a, confidence & 0xFF, kind));
	}

	/**
	 * Smooths the orientation column sums horizontally and converts them to directions.
	 *
	 * @param oriS1Re   column sums, real parts
	 * @param oriS1Im   column sums, imaginary parts
	 * @param direction output directions
	 * @param width     phase map width
	 */
	private static void smoothDirection(short[] oriS1Re, short[] oriS1Im, int[] direction, int width) {
		int s2Re = 0;
		int s2Im = 0;
		int half = width / 2;
		for (int x = 0; x < half + ORIFILT_SIZE / 2; x++) {
			if (x < half) {
				s2Re += oriS1Re[x];
				s2Im += oriS1Im[x];
			}
			if (x >= ORIFILT_SIZE) {
				s2Re -= oriS1Re[x - ORIFILT_SIZE];
				s2Im -= oriS1Im[x - ORIFILT_SIZE];
			}
			if (x >= ORIFILT_SIZE / 2) {
				direction[x - ORIFILT_SIZE / 2] = IntMath.atan2(s2Re, s2Im) / 2;
			}
		}
	}
}
