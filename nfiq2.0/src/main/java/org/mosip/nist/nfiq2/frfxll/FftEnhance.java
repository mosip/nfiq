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
 * Block-wise fixed-point FFT image enhancement ({@code fft_enhance.h} and {@code block_fft.h}).
 * <p>
 * The inverted image is cut into overlapping {@value #BLOCK_DIM}x{@value #BLOCK_DIM} blocks spaced
 * {@value #SPACING} pixels apart. Each block is transformed, its spectrum is band-passed and amplified in
 * proportion to its own magnitude, transformed back, normalised, weighted by a triangular window and
 * accumulated into the output.
 */
final class FftEnhance {
	/** log2 of the block side. */
	private static final int BLOCK_BITS = 5;
	/** Block side in pixels. */
	static final int BLOCK_DIM = 1 << BLOCK_BITS;
	/** Distance between block origins in pixels. */
	static final int SPACING = 17;
	/** Number of fraction bits of the sine table. */
	private static final int SIN_BITS = 5;
	/** Sine table for 32 steps per turn (first half), scaled by 4096. */
	private static final int[] SIN_TABLE = { 0, 799, 1567, 2276, 2896, 3406, 3784, 4017, 4096, 4017, 3784, 3406,
			2896, 2276, 1567, 799 };
	/** Window normalisation {@code size + 1 - spacing}. */
	private static final int ENV_NORM = BLOCK_DIM + 1 - SPACING;

	/** Utility class; not instantiable. */
	private FftEnhance() {
	}

	/**
	 * Enhances an image in place.
	 *
	 * @param img   image pixels, replaced by the enhanced image
	 * @param width image width
	 * @param size  image size in pixels
	 */
	static void enhance(byte[] img, int width, int size) {
		byte[] in = img.clone();
		java.util.Arrays.fill(img, 0, size, (byte) 0);
		int bw = BLOCK_DIM * width;
		int[] block = new int[BLOCK_DIM * BLOCK_DIM];
		int yspacing = width * SPACING;
		for (int yw = yspacing - bw; yw < size; yw += yspacing) {
			for (int x = SPACING - BLOCK_DIM; x < width; x += SPACING) {
				int k = 0;
				for (int y = yw; y < yw + bw; y += width) {
					for (int xx = x; xx < x + BLOCK_DIM; ++xx) {
						block[k++] = inside(xx, y, width, size) ? (~in[xx + y]) & 0xFF : 0;
					}
				}
				enhanceBlock(block);
				k = 0;
				for (int y = yw; y < yw + bw; y += width) {
					for (int xx = x; xx < x + BLOCK_DIM; ++xx) {
						if (inside(xx, y, width, size)) {
							img[xx + y] = (byte) (img[xx + y] + block[k]);
						}
						k++;
					}
				}
			}
		}
		for (int i = 0; i < size; i++) {
			img[i] = (byte) ~img[i];
		}
	}

	/**
	 * Tells whether a pixel lies inside the image.
	 *
	 * @param x     column
	 * @param yw    row offset ({@code row * width})
	 * @param width image width
	 * @param size  image size
	 * @return {@code true} when inside
	 */
	private static boolean inside(int x, int yw, int width, int size) {
		return 0 <= x && x < width && 0 <= yw && yw < size;
	}

	/**
	 * Fixed-point sine for 32 steps per turn.
	 *
	 * @param a angle
	 * @return sine scaled by 4096
	 */
	static int sin(int a) {
		return ((a & SIN_TABLE.length) != 0 ? -1 : 1) * SIN_TABLE[a & (SIN_TABLE.length - 1)];
	}

	/**
	 * Fixed-point cosine for 32 steps per turn.
	 *
	 * @param a angle
	 * @return cosine scaled by 4096
	 */
	static int cos(int a) {
		return sin(a + 8);
	}

	/**
	 * Reverses the {@code bits} bits of {@code x} above {@code shift}.
	 *
	 * @param x     value
	 * @param bits  number of bits to reverse
	 * @param shift number of low bits left untouched (they are 0 for the callers)
	 * @return reversed value
	 */
	static int bitReverse(int x, int bits, int shift) {
		int v = x >> shift;
		int r = 0;
		for (int i = 0; i < bits; i++) {
			r = (r << 1) | ((v >> i) & 1);
		}
		return r << shift;
	}

	/**
	 * Bit-reversal permutation of interleaved complex data.
	 *
	 * @param data       data
	 * @param off        start offset
	 * @param sizeBits   log2 of the data length (in ints)
	 * @param strideBits log2 of the element stride (in ints)
	 */
	private static void shuffle(int[] data, int off, int sizeBits, int strideBits) {
		int size = 1 << sizeBits;
		int stride = 1 << strideBits;
		for (int i = stride; i < size - stride; i += stride) {
			int j = bitReverse(i, sizeBits - strideBits, strideBits);
			if (i > j) {
				int t = data[off + i];
				data[off + i] = data[off + j];
				data[off + j] = t;
				t = data[off + i + 1];
				data[off + i + 1] = data[off + j + 1];
				data[off + j + 1] = t;
			}
		}
	}

	/**
	 * Radix-2 fixed-point complex FFT on interleaved data.
	 *
	 * @param data       data
	 * @param off        start offset
	 * @param inverse    {@code true} for the inverse transform (no scaling)
	 * @param sizeBits   log2 of the data length (in ints)
	 * @param strideBits log2 of the element stride (in ints)
	 */
	static void fft(int[] data, int off, boolean inverse, int sizeBits, int strideBits) {
		int n = 1 << sizeBits;
		int stride = 1 << strideBits;
		shuffle(data, off, sizeBits, strideBits);
		int dt = (inverse ? 1 : -1) * (1 << SIN_BITS);
		for (int ll = strideBits; ll < sizeBits; ll++) {
			int mmax = 1 << ll;
			int istep = mmax << 1;
			dt >>= 1;
			for (int m = 0, t = 0; m < mmax; m += stride, t += dt) {
				int wr = cos(t);
				int wi = sin(t);
				for (int i = m; i < n; i += istep) {
					int pi = off + i;
					int pj = pi + mmax;
					int tr = IntMath.reduce(wr * data[pj] - wi * data[pj + 1], 12);
					int ti = IntMath.reduce(wr * data[pj + 1] + wi * data[pj], 12);
					data[pj] = data[pi] - tr;
					data[pj + 1] = data[pi + 1] - ti;
					data[pi] += tr;
					data[pi + 1] += ti;
				}
			}
		}
	}

	/**
	 * One row of the 2-D transform: complex FFT of half length plus the real-signal split step.
	 *
	 * @param data    data
	 * @param off     row offset
	 * @param inverse {@code true} for the inverse transform
	 */
	private static void fft1(int[] data, int off, boolean inverse) {
		final int sizeBits = BLOCK_BITS;
		final int strideBits = 1;
		if (!inverse) {
			fft(data, off, false, sizeBits, strideBits);
		}
		int stride = 1 << strideBits;
		int size = 1 << sizeBits;
		int dt = (inverse ? 1 : -1) * (1 << (SIN_BITS - (sizeBits - strideBits + 1)));
		int t = dt + (inverse ? (1 << (SIN_BITS - 1)) : 0);
		int bits = 12 + (inverse ? 0 : 1);
		for (int i = stride; i <= size / 2; i += stride, t += dt) {
			int wr = cos(t);
			int wi = sin(t);
			int p1 = off + i;
			int p2 = off + size - i;
			int h1r = (data[p2] + data[p1]) << 12;
			int h1i = (data[p1 + 1] - data[p2 + 1]) << 12;
			int zr = data[p1 + 1] + data[p2 + 1];
			int zi = data[p2] - data[p1];
			int h2r = wr * zr - wi * zi;
			int h2i = wr * zi + wi * zr;
			int f1r = IntMath.reduce(h1r + h2r, bits);
			int f1i = IntMath.reduce(h1i + h2i, bits);
			int f2r = IntMath.reduce(h1r - h2r, bits);
			int f2i = IntMath.reduce(-(h1i - h2i), bits);
			data[p1] = f1r;
			data[p1 + 1] = f1i;
			data[p2] = f2r;
			data[p2 + 1] = f2i;
		}
		int tr = data[off];
		int ti = data[off + 1];
		data[off] = tr + ti;
		data[off + 1] = inverse ? (tr - ti) : 0;
		if (inverse) {
			fft(data, off, true, sizeBits, strideBits);
		}
	}

	/**
	 * 2-D real FFT of a block (rows first on the forward transform, columns first on the inverse).
	 *
	 * @param data    block data
	 * @param inverse {@code true} for the inverse transform
	 */
	private static void fft2(int[] data, boolean inverse) {
		int size1 = BLOCK_DIM;
		int size2 = size1 << BLOCK_BITS;
		if (!inverse) {
			for (int y = 0; y < size2; y += size1) {
				fft1(data, y, false);
			}
		}
		for (int x = 0; x < size1; x += 2) {
			fft(data, x, inverse, BLOCK_BITS * 2, BLOCK_BITS);
		}
		if (inverse) {
			for (int y = 0; y < size2; y += size1) {
				fft1(data, y, true);
			}
		}
	}

	/**
	 * Spectrum shaping: removes very low and high frequencies and boosts strong components.
	 *
	 * @param data block spectrum, modified in place
	 */
	private static void enhanceArray(int[] data) {
		int size = BLOCK_DIM;
		int halfsize = BLOCK_DIM / 2;
		int d = 0;
		for (int y = 0; y < size; y++) {
			int fy = y - ((y < halfsize) ? 0 : size);
			for (int x = 0; x < halfsize; x++, d += 2) {
				int r2 = x * x + fy * fy;
				if (r2 <= 6 || r2 >= 169) {
					data[d] = 0;
					data[d + 1] = 0;
					continue;
				}
				int vr = data[d];
				int vi = data[d + 1];
				int a = IntMath.reduce(IntMath.octAbs(vr, vi), 5);
				int v1r = IntMath.reduce(vr * a, 7);
				int v1i = IntMath.reduce(vi * a, 7);
				data[d] = IntMath.reduce(vr + v1r, 3);
				data[d + 1] = IntMath.reduce(vi + v1i, 3);
			}
		}
	}

	/**
	 * Triangular window weight.
	 *
	 * @param i position in the block
	 * @return weight in [1, 16]
	 */
	private static int envelope(int i) {
		int floor = Math.min(ENV_NORM, SPACING);
		return Math.min(Math.min(i + 1, BLOCK_DIM - i), floor);
	}

	/**
	 * Rescales a block to [0, 251] and applies the window.
	 *
	 * @param data block, modified in place
	 */
	private static void normalize(int[] data) {
		int mn = Integer.MAX_VALUE;
		int mx = Integer.MIN_VALUE;
		for (int v : data) {
			mn = Math.min(mn, v);
			mx = Math.max(mx, v);
		}
		int range = mx - mn;
		int div = range;
		final int trsh = 16;
		if (range < trsh) {
			div = trsh;
			mn -= (trsh - range) / 2;
		}
		div *= ENV_NORM * ENV_NORM;
		int d = 0;
		for (int y = 0; y < BLOCK_DIM; ++y) {
			int ye = envelope(y);
			for (int x = 0; x < BLOCK_DIM; ++x, ++d) {
				data[d] = IntMath.divide((data[d] - mn) * 251 * envelope(x) * ye, div);
			}
		}
	}

	/**
	 * Enhances one block.
	 *
	 * @param data block pixels (inverted), replaced by the weighted enhanced block
	 */
	static void enhanceBlock(int[] data) {
		fft2(data, false);
		enhanceArray(data);
		fft2(data, true);
		for (int i = 0; i < data.length; i++) {
			data[i] = IntMath.reduce(data[i], BLOCK_BITS * 2);
		}
		normalize(data);
	}
}
