package org.mosip.nist.nfiq2;

/**
 * An 8-bit greyscale fingerprint image (NIST {@code NFIQ2::FingerprintImageData}).
 * <p>
 * Pixels are row-major, one byte per pixel, 0 = black and 255 = white. NFIQ 2 only accepts 500 ppi
 * images; the library does not resample.
 * <p>
 * Immutable apart from the pixel array, which is shared, not copied.
 */
public final class FingerprintImageData {
	/** The only resolution NFIQ 2 supports. */
	public static final int RESOLUTION_500_PPI = 500;

	/** Mean grey value above which an edge row or column is treated as white frame. */
	private static final double MU_THRESHOLD = 250;
	/** Largest width FingerJetFX accepts after trimming. */
	private static final int FINGERJET_MAX_WIDTH = 800;
	/** Largest height FingerJetFX accepts after trimming. */
	private static final int FINGERJET_MAX_HEIGHT = 1000;

	/** Pixels. */
	private final byte[] data;
	/** Width in pixels. */
	private final int width;
	/** Height in pixels. */
	private final int height;
	/** ISO/IEC 19794-4 finger position code. */
	private final int fingerCode;
	/** Resolution in pixels per inch. */
	private final int ppi;

	/**
	 * Wraps a pixel buffer.
	 *
	 * @param data       pixels, row-major; at least {@code width * height} bytes
	 * @param width      width in pixels
	 * @param height     height in pixels
	 * @param fingerCode ISO/IEC 19794-4 finger position (0 = unknown)
	 * @param ppi        resolution in pixels per inch
	 * @throws Nfiq2Exception ({@link ErrorCode#BadArguments}) when the buffer is too small or a size is not
	 *                        positive
	 */
	public FingerprintImageData(byte[] data, int width, int height, int fingerCode, int ppi) {
		if (data == null || width <= 0 || height <= 0 || data.length < (long) width * height) {
			throw new Nfiq2Exception(ErrorCode.BadArguments,
					"Image buffer does not hold " + width + "x" + height + " pixels");
		}
		this.data = data;
		this.width = width;
		this.height = height;
		this.fingerCode = fingerCode;
		this.ppi = ppi;
	}

	/**
	 * Returns the pixel buffer (not a copy).
	 *
	 * @return pixels
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Returns the width.
	 *
	 * @return width in pixels
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Returns the height.
	 *
	 * @return height in pixels
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Returns the finger position code.
	 *
	 * @return ISO/IEC 19794-4 finger position
	 */
	public int getFingerCode() {
		return fingerCode;
	}

	/**
	 * Returns the resolution.
	 *
	 * @return pixels per inch
	 */
	public int getPpi() {
		return ppi;
	}

	/**
	 * Crops the near-white frame around the fingerprint: edge rows and columns whose mean grey value is
	 * above 250 are removed from each side.
	 *
	 * @return cropped copy
	 * @throws Nfiq2Exception ({@link ErrorCode#InvalidImageSize}) when the image is blank, the crop is
	 *                        degenerate, or the result is wider than 800 or taller than 1000 pixels
	 */
	public FingerprintImageData copyRemovingNearWhiteFrame() {
		int top = 0;
		while (top < height && rowMean(top) > MU_THRESHOLD) {
			top++;
		}
		if (top >= height) {
			throw new Nfiq2Exception(ErrorCode.InvalidImageSize, "All image rows appear to be blank");
		}
		int bottom = height - 1;
		while (bottom >= top && rowMean(bottom) > MU_THRESHOLD) {
			bottom--;
		}
		bottom = Math.max(bottom, 0);

		int left = 0;
		while (left < width && columnMean(left) > MU_THRESHOLD) {
			left++;
		}
		if (left >= width) {
			throw new Nfiq2Exception(ErrorCode.InvalidImageSize, "All image columns appear to be blank");
		}
		int right = width - 1;
		while (right >= left && columnMean(right) > MU_THRESHOLD) {
			right--;
		}
		right = Math.max(right, 0);

		if (right <= left || bottom <= top) {
			throw new Nfiq2Exception(ErrorCode.InvalidImageSize, "Asked to inclusively crop from (" + left + ','
					+ top + ") to (" + right + ',' + bottom + ')');
		}
		int w = right - left + 1;
		int h = bottom - top + 1;
		if (w > FINGERJET_MAX_WIDTH) {
			throw new Nfiq2Exception(ErrorCode.InvalidImageSize, "Width is too large after trimming whitespace. WxH: "
					+ w + "x" + h + ", but maximum width is " + FINGERJET_MAX_WIDTH);
		}
		if (h > FINGERJET_MAX_HEIGHT) {
			throw new Nfiq2Exception(ErrorCode.InvalidImageSize, "Height is too large after trimming whitespace. WxH: "
					+ w + "x" + h + ", but maximum height is " + FINGERJET_MAX_HEIGHT);
		}
		byte[] cropped = new byte[w * h];
		for (int y = 0; y < h; y++) {
			System.arraycopy(data, (top + y) * width + left, cropped, y * w, w);
		}
		return new FingerprintImageData(cropped, w, h, fingerCode, ppi);
	}

	/**
	 * Mean grey value of a row.
	 *
	 * @param row row index
	 * @return mean
	 */
	private double rowMean(int row) {
		long mu = 0;
		int off = row * width;
		for (int x = 0; x < width; x++) {
			mu += data[off + x] & 0xFF;
		}
		return (double) mu / width;
	}

	/**
	 * Mean grey value of a column.
	 *
	 * @param col column index
	 * @return mean
	 */
	private double columnMean(int col) {
		long mu = 0;
		for (int y = 0; y < height; y++) {
			mu += data[y * width + col] & 0xFF;
		}
		return (double) mu / height;
	}
}
