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
import java.util.Comparator;
import java.util.List;

/**
 * Minutiae extraction from a raw greyscale image ({@code FRFXLLCreateFeatureSetFromRaw} with the
 * {@code FeatureExtraction} pipeline of {@code FeatureExtraction.h}).
 * <p>
 * The image is resampled to 333 dpi (at most 256x400 pixels, centre-cropped), optionally enhanced, turned
 * into an orientation map, a footprint and a phase map, and minutiae are detected on the phase map.
 * Positions are returned at 500 ppi. The context overrides applied by NIST for NFIQ 2 are built in: the
 * user-feedback minimums (minutiae count and footprint area) are 0 and up to 255 minutiae are kept.
 * <p>
 * Stateless and thread-safe.
 */
public final class FeatureExtractor {
	/** Flag: skip the FFT enhancement ({@code FRFXLL_FEX_DISABLE_ENHANCEMENT}). */
	public static final int FEX_DISABLE_ENHANCEMENT = 0x1;
	/** Flag: run the FFT enhancement ({@code FRFXLL_FEX_ENABLE_ENHANCEMENT}), as NFIQ 2 does. */
	public static final int FEX_ENABLE_ENHANCEMENT = 0x2;

	/** Minimum number of minutiae for a successful extraction. */
	static final int MIN_MINUTIA = 6;
	/** Maximum working width at 333 dpi. */
	static final int MAX_WIDTH = 256;
	/** Maximum working height at 333 dpi. */
	static final int MAX_HEIGHT = 400;
	/** Working resolution in dpi. */
	static final int INT_RESOLUTION = 333;
	/** Internal coordinate scale ({@code 127 * 5}). */
	static final int IMAGE_SCALE = 635;
	/** Maximum number of minutiae kept. */
	static final int CAPACITY = 255;
	/** Minimum number of minutiae requested by user feedback (0 in the NFIQ 2 context). */
	static final int MIN_NUMBER_OF_MINUTIA = 0;
	/** Minimum footprint area requested by user feedback (0 in the NFIQ 2 context). */
	static final int MIN_FOOTPRINT_AREA = 0;
	/** Footprint bitmap width (cells of 8x8 pixels). */
	private static final int FOOTPRINT_WIDTH = 32;
	/** Footprint bitmap height (cells of 8x8 pixels). */
	private static final int FOOTPRINT_HEIGHT = 50;
	/** Resolution the minutiae are finally scaled to, in pixels per centimetre ({@code 197}). */
	private static final int OUT_PPCM = 197;
	/** Resolution assumed for internal coordinates, in pixels per centimetre ({@code 167}). */
	private static final int IN_PPCM = 167;

	/** Output order of the minutiae: highest confidence first, then top-down, left-right, by angle. */
	private static final Comparator<Minutia> BY_CONFIDENCE = Comparator.comparingInt((Minutia m) -> -m.conf)
			.thenComparingInt(m -> m.y).thenComparingInt(m -> m.x).thenComparingInt(m -> m.theta);

	/** Working image width. */
	private int width;
	/** Working image height. */
	private int height;
	/** Working image size. */
	private int size;
	/** Horizontal offset of the working image, in working pixels. */
	private int xOffs = 1;
	/** Vertical offset of the working image, in working pixels. */
	private int yOffs = 6;
	/** Resolution attributed to the working image. */
	private int imageResolution;
	/** Orientation map width. */
	private int oriWidth;
	/** Orientation map size. */
	private int oriSize;

	/** Instances only live for one extraction. */
	private FeatureExtractor() {
	}

	/**
	 * Extracts minutiae from a raw 8-bit greyscale image.
	 *
	 * @param data   pixels, row-major, at least {@code width * height} bytes
	 * @param width  image width
	 * @param height image height
	 * @param dpi    image resolution in dots per inch
	 * @param flags  {@link #FEX_ENABLE_ENHANCEMENT} or {@link #FEX_DISABLE_ENHANCEMENT}
	 * @return minutiae in output order (at most 255)
	 * @throws FrfxllException when the image is invalid or fewer than 6 minutiae are found
	 */
	public static List<FrfxllMinutia> createFeatureSetFromRaw(byte[] data, int width, int height, int dpi, int flags)
			throws FrfxllException {
		long w500 = width * 500L;
		long h500 = height * 500L;
		if (width > 2000 || height > 2000 || dpi < 300 || dpi > 1024 || w500 < 150L * dpi || w500 > 812L * dpi
				|| h500 < 150L * dpi || h500 > 1000L * dpi) {
			throw invalidImage("image size or resolution out of range");
		}
		return new FeatureExtractor().fromRawSample(data, width, height, dpi, flags);
	}

	/**
	 * Creates an invalid-image exception.
	 *
	 * @param message description
	 * @return exception
	 */
	private static FrfxllException invalidImage(String message) {
		return new FrfxllException(FrfxllException.ERR_INVALID_IMAGE, "FRFXLL_ERR_INVALID_IMAGE: " + message);
	}

	/**
	 * {@code Init} followed by {@code ExtractMinutiae}.
	 *
	 * @param data   pixels
	 * @param inW    image width
	 * @param inH    image height
	 * @param dpi    resolution
	 * @param flags  extraction flags
	 * @return minutiae
	 * @throws FrfxllException on failure
	 */
	private List<FrfxllMinutia> fromRawSample(byte[] data, int inW, int inH, int dpi, int flags)
			throws FrfxllException {
		if (data.length < (long) inW * inH) {
			throw invalidImage("buffer smaller than width * height");
		}
		if (dpi > 1008) {
			throw invalidImage("resolution above 1008 dpi");
		}
		long widthAt500 = inW * 500L / dpi;
		// Upstream computes the height check from the width as well; kept for identical behaviour.
		long heightAt500 = widthAt500;
		if (widthAt500 < 196 || heightAt500 < 196 || widthAt500 > 800 || heightAt500 > 1000) {
			throw invalidImage("image size at 500 dpi out of range");
		}
		width = inW;
		height = inH;
		imageResolution = dpi;
		byte[] img = resizeTo333(data);
		return from333DpiImg(img, flags);
	}

	/**
	 * Resamples the input to about 333 dpi, centre-cropping to 256x400 ({@code Resize_AnyTo333InPlaceOrBuffer}).
	 *
	 * @param in input pixels
	 * @return working image
	 * @throws FrfxllException when the image is too small
	 */
	private byte[] resizeTo333(byte[] in) throws FrfxllException {
		if (height < 32 || width < 32) {
			throw invalidImage("image smaller than 32x32");
		}
		int inWidth = width;
		int inHeight = height;
		boolean near500 = imageResolution <= 550 && imageResolution >= 450;
		if (near500) {
			width = inWidth / 6 * 4;
			height = inHeight / 6 * 4;
		} else {
			width = (inWidth * INT_RESOLUTION / imageResolution) & ~(OrientationMap.ORI_SCALE - 1);
			height = (inHeight * INT_RESOLUTION / imageResolution) & ~(OrientationMap.ORI_SCALE - 1);
		}
		int inOff = 0;
		if (height > MAX_HEIGHT) {
			int maxHeightIn = near500 ? MAX_HEIGHT * 3 / 2 : MAX_HEIGHT * imageResolution / INT_RESOLUTION;
			int diff = inHeight - maxHeightIn;
			inOff += (diff / 2) * inWidth;
			yOffs += (height - MAX_HEIGHT) / 2;
			height = MAX_HEIGHT;
		}
		if (width > MAX_WIDTH) {
			int maxWidthIn = near500 ? MAX_WIDTH * 3 / 2 : MAX_WIDTH * imageResolution / INT_RESOLUTION;
			int diff = inWidth - maxWidthIn;
			inOff += diff / 2;
			xOffs += (width - MAX_WIDTH) / 2;
			width = MAX_WIDTH;
		}
		size = width * height;
		if (width == 0 || size == 0) {
			throw invalidImage("empty working image");
		}
		oriWidth = (width - 1) / OrientationMap.ORI_SCALE + 1;
		oriSize = ((size / width - 1) / OrientationMap.ORI_SCALE + 1) * oriWidth;

		byte[] out = new byte[size];
		if (near500) {
			imresize23(out, width, size, in, inOff, inWidth);
		} else if (imageResolution <= INT_RESOLUTION + 33 && imageResolution >= INT_RESOLUTION - 33
				&& (inWidth % 4) == 0) {
			System.arraycopy(in, inOff, out, 0, Math.min(size, in.length - inOff));
			imageResolution = (imageResolution * 5 + 1) / 3;
		} else {
			int scale256 = imageResolution * 256 / INT_RESOLUTION;
			imresize(out, width, size, in, inOff, inWidth, inHeight, scale256);
			imageResolution = 500;
		}
		return out;
	}

	/**
	 * Downscales by 2/3 with a 3x3 to 2x2 kernel ({@code imresize23}).
	 *
	 * @param out      output pixels
	 * @param outWidth output width
	 * @param outSize  output size
	 * @param in       input pixels
	 * @param inOff    offset of the first input pixel
	 * @param inWidth  input row width
	 */
	static void imresize23(byte[] out, int outWidth, int outSize, byte[] in, int inOff, int inWidth) {
		final int div = 256;
		final int mul = div / 9;
		for (int po = 0, pi = inOff; po < outSize; pi += inWidth * 3, po += outWidth * 2) {
			int o = po;
			int i = pi;
			for (int endline = po + outWidth; o < endline; i += 3, o += 2) {
				int i00 = in[i] & 0xFF;
				int i01 = in[i + 1] & 0xFF;
				int i02 = in[i + 2] & 0xFF;
				int i10 = in[i + inWidth] & 0xFF;
				int i11 = in[i + inWidth + 1] & 0xFF;
				int i12 = in[i + inWidth + 2] & 0xFF;
				int i20 = in[i + inWidth * 2] & 0xFF;
				int i21 = in[i + inWidth * 2 + 1] & 0xFF;
				int i22 = in[i + inWidth * 2 + 2] & 0xFF;
				int o00 = i00 * 4 + i01 * 2 + i10 * 2 + i11;
				int o01 = i02 * 4 + i01 * 2 + i12 * 2 + i11;
				int o10 = i20 * 4 + i21 * 2 + i10 * 2 + i11;
				int o11 = i22 * 4 + i21 * 2 + i12 * 2 + i11;
				out[o] = (byte) (o00 * mul / div);
				out[o + 1] = (byte) (o01 * mul / div);
				out[o + outWidth] = (byte) (o10 * mul / div);
				out[o + outWidth + 1] = (byte) (o11 * mul / div);
			}
		}
	}

	/**
	 * Bilinear resampling by {@code 256 / scale256} ({@code imresize}).
	 *
	 * @param out      output pixels
	 * @param oW       output width
	 * @param oSize    output size
	 * @param in       input pixels
	 * @param inOff    offset of the first input pixel
	 * @param iW       input width
	 * @param iH       input height
	 * @param scale256 input step per output pixel, times 256
	 */
	static void imresize(byte[] out, int oW, int oSize, byte[] in, int inOff, int iW, int iH, int scale256) {
		int piv = inOff;
		int dy = 0;
		long limit = (long) inOff + (long) iW * iH;
		for (int po = 0; po < oSize;) {
			int pi = piv;
			int dx = 0;
			for (int endline = po + oW; po < endline; po++) {
				if (pi + iW < limit) {
					out[po] = (byte) (((at(in, pi) * (256 - dx) + at(in, pi + 1) * dx) * (256 - dy)
							+ (at(in, pi + iW) * (256 - dx) + at(in, pi + iW + 1) * dx) * dy) >> 16);
				}
				dx += scale256;
				pi += dx >> 8;
				dx &= 0xFF;
			}
			dy += scale256;
			piv += (dy >> 8) * iW;
			dy &= 0xFF;
		}
	}

	/**
	 * Reads an input pixel, 0 outside the buffer.
	 *
	 * @param in  pixels
	 * @param idx position
	 * @return value in [0, 255]
	 */
	private static int at(byte[] in, int idx) {
		return idx >= 0 && idx < in.length ? in[idx] & 0xFF : 0;
	}

	/**
	 * Runs the extraction on the working image ({@code From333DpiImg}).
	 *
	 * @param img   working image
	 * @param flags extraction flags
	 * @return minutiae
	 * @throws FrfxllException when fewer than 6 minutiae are found
	 */
	private List<FrfxllMinutia> from333DpiImg(byte[] img, int flags) throws FrfxllException {
		int[] ori = new int[oriSize];
		byte[] footprint = new byte[oriSize];
		if ((flags & FEX_ENABLE_ENHANCEMENT) != 0) {
			OrientationMap.orientationMapAndFootprint(width, size, img, true, null, footprint);
			FftEnhance.enhance(img, width, size);
			OrientationMap.orientationMapAndFootprint(width, size, img, false, ori, footprint);
		} else {
			OrientationMap.orientationMapAndFootprint(width, size, img, true, ori, footprint);
		}
		byte[] phase = Freeman.phasemap(width, size, new Mem(img, size, ori, oriSize, 0), ori);
		List<Minutia> found = MinutiaExtractor.extract(new Mem(phase, size, ori, oriSize, Freeman.PHASEMAP_FILLER),
				width, size);

		Minutia threshold = new Minutia((short) 0, (short) 0, 0, 0, Minutia.TYPE_OTHER);
		List<Minutia> kept = new ArrayList<>();
		for (Minutia m : found) {
			if (m.greater(threshold)) {
				kept.add(m);
			}
		}
		kept.sort(BY_CONFIDENCE);
		if (kept.size() > CAPACITY) {
			kept = new ArrayList<>(kept.subList(0, CAPACITY));
		}

		short dx = (short) (xOffs * IMAGE_SCALE / imageResolution);
		short dy = (short) (yOffs * IMAGE_SCALE / imageResolution);
		List<FrfxllMinutia> out = new ArrayList<>(kept.size());
		for (Minutia m : kept) {
			m.theta = (64 - m.theta) & 0xFF;
			m.x = (short) (m.x * IMAGE_SCALE / imageResolution);
			m.y = (short) (m.y * IMAGE_SCALE / imageResolution);
			m.x = (short) (m.x + dx);
			m.y = (short) (m.y + dy);
			m.x = IntMath.muldiv16(m.x, OUT_PPCM, IN_PPCM);
			m.y = IntMath.muldiv16(m.y, OUT_PPCM, IN_PPCM);
			out.add(new FrfxllMinutia(m.x & 0xFFFF, m.y & 0xFFFF, m.theta, m.type,
					Math.min((m.conf + 1) / 2, 100)));
		}
		int area = footprintArea(footprint);

		if (out.size() < MIN_MINUTIA || out.size() < MIN_NUMBER_OF_MINUTIA || area < MIN_FOOTPRINT_AREA) {
			throw new FrfxllException(FrfxllException.ERR_FB_TOO_SMALL_AREA,
					"FRFXLL_ERR_FB_TOO_SMALL_AREA: Fingerprint area is too small. Most likely this is because"
							+ " the tip of the finger is presented.");
		}
		return out;
	}

	/**
	 * Footprint area in 500 ppi pixels, sampled on a 32x50 grid of 8x8 cells ({@code WriteFootprint}).
	 *
	 * @param footprint footprint map
	 * @return area
	 */
	private int footprintArea(byte[] footprint) {
		int area = 0;
		for (int y = 0; y < FOOTPRINT_HEIGHT; y++) {
			for (int x = 0; x < FOOTPRINT_WIDTH; x++) {
				int xi = IntMath.reduce(((x * 8) * imageResolution + imageResolution / 2) / IMAGE_SCALE, 2);
				int yi = IntMath.reduce(((y * 8) * imageResolution + imageResolution / 2) / IMAGE_SCALE, 2)
						* oriWidth;
				if (xi < oriWidth && yi < oriSize && footprint[xi + yi] != 0) {
					area++;
				}
			}
		}
		return area * 8 * 8;
	}
}
