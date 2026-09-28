package org.mosip.nist.nfiq2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Tests of {@link FingerprintImageData}.
 */
class FingerprintImageDataTest {
	/** White. */
	private static final byte WHITE = (byte) 255;

	@Test
	void rejectsBadArguments() {
		byte[] data = new byte[4];
		assertBadArguments(() -> new FingerprintImageData(null, 2, 2, 0, 500));
		assertBadArguments(() -> new FingerprintImageData(data, 0, 2, 0, 500));
		assertBadArguments(() -> new FingerprintImageData(data, 2, 0, 0, 500));
		assertBadArguments(() -> new FingerprintImageData(data, 3, 2, 0, 500));
	}

	@Test
	void getters() {
		byte[] data = new byte[6];
		FingerprintImageData image = new FingerprintImageData(data, 3, 2, 7, 500);
		assertSame(data, image.getData());
		assertEquals(3, image.getWidth());
		assertEquals(2, image.getHeight());
		assertEquals(7, image.getFingerCode());
		assertEquals(500, image.getPpi());
	}

	@Test
	void cropsWhiteFrame() {
		int w = 70;
		int h = 60;
		byte[] data = white(w * h);
		for (int y = 10; y < 50; y++) {
			Arrays.fill(data, y * w + 5, y * w + 65, (byte) 0);
		}
		FingerprintImageData cropped = new FingerprintImageData(data, w, h, 3, 500).copyRemovingNearWhiteFrame();
		assertEquals(60, cropped.getWidth());
		assertEquals(40, cropped.getHeight());
		assertEquals(3, cropped.getFingerCode());
		assertEquals(0, cropped.getData()[0]);
	}

	@Test
	void rejectsBlankImages() {
		assertInvalidSize(new FingerprintImageData(white(100), 10, 10, 0, 500));

		int w = 100;
		byte[] oneGreyRow = white(w * w);
		Arrays.fill(oneGreyRow, 50 * w, 51 * w, (byte) 200);
		assertInvalidSize(new FingerprintImageData(oneGreyRow, w, w, 0, 500));

		byte[] oneBlackRow = white(w * w);
		Arrays.fill(oneBlackRow, 50 * w, 51 * w, (byte) 0);
		assertInvalidSize(new FingerprintImageData(oneBlackRow, w, w, 0, 500));
	}

	@Test
	void rejectsOversizedImages() {
		assertInvalidSize(new FingerprintImageData(new byte[900 * 10], 900, 10, 0, 500));
		assertInvalidSize(new FingerprintImageData(new byte[10 * 1100], 10, 1100, 0, 500));
	}

	/**
	 * Returns a white buffer.
	 *
	 * @param size number of pixels
	 * @return pixels
	 */
	private static byte[] white(int size) {
		byte[] data = new byte[size];
		Arrays.fill(data, WHITE);
		return data;
	}

	/**
	 * Asserts that constructing an image fails with {@link ErrorCode#BadArguments}.
	 *
	 * @param construction constructor call
	 */
	private static void assertBadArguments(org.junit.jupiter.api.function.Executable construction) {
		assertEquals(ErrorCode.BadArguments, assertThrows(Nfiq2Exception.class, construction).getNfiq2ErrorCode());
	}

	/**
	 * Asserts that cropping fails with {@link ErrorCode#InvalidImageSize}.
	 *
	 * @param image image
	 */
	private static void assertInvalidSize(FingerprintImageData image) {
		assertEquals(ErrorCode.InvalidImageSize,
				assertThrows(Nfiq2Exception.class, image::copyRemovingNearWhiteFrame).getNfiq2ErrorCode());
	}
}
