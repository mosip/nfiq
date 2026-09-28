package org.mosip.nist.nfiq1.imagetools;

import java.util.concurrent.atomic.AtomicInteger;

import org.mosip.nist.nfiq1.util.StringUtil;

/**
 * Image-type codes and format sniffing for fingerprint image data.
 * <p>
 * Port of the image-type constants and {@code image_type()} / {@code is_*()} detection routines from NIST's
 * imgtools ({@code imgtype.c}). The numeric codes match NIST's values; this port only actually detects WSQ and
 * JPEG 2000 content.
 * <p>
 * Singleton using the initialization-on-demand holder idiom; it has no mutable state and is thread-safe.
 */
public class ImageType extends ImageTools {
	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()}.
	 */
	private ImageType() {
		super();
	}

	/**
	 * Lazy initialization holder for the {@link ImageType} singleton.
	 */
	private static class Holder {
		/** The singleton instance, created when {@code Holder} is first loaded. */
		private static final ImageType INSTANCE = new ImageType();
	}

	/**
	 * Returns the shared {@code ImageType} singleton.
	 *
	 * @return the singleton instance (never {@code null})
	 */
	public static synchronized ImageType getInstance() {
		return Holder.INSTANCE;
	}

	/** Image type could not be determined (NIST {@code UNKNOWN_IMG}). */
	public static final int UNKNOWN_IMG = -1;
	/** Raw, headerless pixel data (NIST {@code RAW_IMG}). */
	public static final int RAW_IMG = 0;
	/** WSQ (Wavelet Scalar Quantization) compressed image (NIST {@code WSQ_IMG}). */
	public static final int WSQ_IMG = 1;
	/** Lossless JPEG compressed image (NIST {@code JPEGL_IMG}). */
	public static final int JPEGL_IMG = 2;
	/** Baseline (lossy) JPEG compressed image (NIST {@code JPEGB_IMG}). */
	public static final int JPEGB_IMG = 3;
	/** NIST IHead format image (NIST {@code IHEAD_IMG}). */
	public static final int IHEAD_IMG = 4;
	/** ANSI/NIST-ITL transaction file (NIST {@code ANSI_NIST_IMG}). */
	public static final int ANSI_NIST_IMG = 5;
	/** JPEG 2000 compressed image (NIST {@code JP2_IMG}). */
	public static final int JP2_IMG = 6;
	/** PNG compressed image (NIST {@code PNG_IMG}). */
	public static final int PNG_IMG = 7;

	/**
	 * Determines the type of an in-memory image.
	 * <p>
	 * Port of NIST {@code image_type()}. The original also recognised IHEAD, JPEGL, JPEGB and ANSI_NIST data;
	 * this implementation only tests for WSQ ({@link #isWSQ(byte[], int)}) and then JPEG 2000
	 * ({@link #isJP2000(byte[], int)}).
	 *
	 * @param imageType   output: set to {@link #WSQ_IMG}, {@link #JP2_IMG} or {@link #UNKNOWN_IMG}
	 * @param imageData   the encoded image bytes
	 * @param imageLength length of {@code imageData} in bytes
	 * @return 0 if the type was recognised; -1 if it is unknown
	 */
	public int getImageType(AtomicInteger imageType, byte[] imageData, final int imageLength) {
		int ret = -1;

		if (isWSQ(imageData, imageLength) > 0) {
			imageType.set(WSQ_IMG);
			return (0);
		}

		if (isJP2000(imageData, imageLength) > 0) {
			imageType.set(JP2_IMG);
			return (0);
		}

		/* Otherwise, image type is UNKNOWN ... */
		imageType.set(UNKNOWN_IMG);
		return ret;
	}

	/**
	 * Tests whether a buffer holds a WSQ-compressed image.
	 * <p>
	 * The data counts as WSQ when it starts with the SOI marker {@code 0xFF 0xA0} and ends with the EOI marker
	 * {@code 0xFF 0xA1}.
	 *
	 * @param imageData   the encoded image bytes (at least 2 bytes)
	 * @param imageLength length of the data in bytes (not used; the array length is used instead)
	 * @return 1 if the data is WSQ, otherwise 0
	 */
	@SuppressWarnings("java:S1172")
	public int isWSQ(byte[] imageData, final int imageLength) {
		int ret = 0;
		// If the first two bytes are 0xFF and 0xA0 and the last two bytes are 0xFF and
		// 0xA1
		// then it is a WSQ file
		if ((imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xA0
				&& imageData[imageData.length - 2] == (byte) 0xFF && imageData[imageData.length - 1] == (byte) 0xA1)) {
			ret = 1;
		}

		return (ret);
	}

	/**
	 * Tests whether a buffer holds a JPEG 2000 (JP2) image.
	 * <p>
	 * Checks whether bytes 4 to 7 contain the JP2 signature box type {@code "jP  "}.
	 *
	 * @param imageData   the encoded image bytes (at least 8 bytes)
	 * @param imageLength length of the data in bytes (not used)
	 * @return 1 if the data is JPEG 2000, otherwise 0
	 */
	@SuppressWarnings("java:S1172")
	public int isJP2000(byte[] imageData, final int imageLength) {
		int ret = 0;
		int nptrIndex = 4;
		char[] buf = StringUtil.byteToCharArray(imageData, nptrIndex, nptrIndex + 4, 4);

		if (new String(buf).contains("jP  ")) {
			ret = 1;
		}

		return (ret);
	}
}