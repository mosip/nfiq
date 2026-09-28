package org.mosip.nist.nfiq2.frfxll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.TestImages;

/**
 * Tests of {@link FeatureExtractor} and the FingerJetFX records.
 */
class FeatureExtractorTest {

	@Test
	void rejectsInvalidImages() {
		assertInvalid(new byte[400 * 400], 400, 400, 200);
		assertInvalid(new byte[400 * 400], 400, 400, 1100);
		assertInvalid(new byte[2100 * 400], 2100, 400, 500);
		assertInvalid(new byte[100 * 400], 100, 400, 500);
		assertInvalid(new byte[400 * 100], 400, 100, 500);
		assertInvalid(new byte[10], 400, 400, 500);
		assertInvalid(new byte[800 * 800], 800, 800, 1010);
	}

	@Test
	void blankImageHasTooFewMinutiae() {
		byte[] white = new byte[300 * 400];
		Arrays.fill(white, (byte) 255);
		FrfxllException e = assertThrows(FrfxllException.class,
				() -> FeatureExtractor.createFeatureSetFromRaw(white, 300, 400, 500,
						FeatureExtractor.FEX_ENABLE_ENHANCEMENT));
		assertEquals(FrfxllException.ERR_FB_TOO_SMALL_AREA, e.getCode());
	}

	@Test
	void extractsWithAndWithoutEnhancement() throws IOException, FrfxllException {
		FingerprintImageData img = TestImages.jp2Cropped();
		List<FrfxllMinutia> enhanced = FeatureExtractor.createFeatureSetFromRaw(img.getData(), img.getWidth(),
				img.getHeight(), 500, FeatureExtractor.FEX_ENABLE_ENHANCEMENT);
		List<FrfxllMinutia> plain = FeatureExtractor.createFeatureSetFromRaw(img.getData(), img.getWidth(),
				img.getHeight(), 500, FeatureExtractor.FEX_DISABLE_ENHANCEMENT);
		assertEquals(99, enhanced.size());
		assertFalse(plain.isEmpty());
		for (FrfxllMinutia m : enhanced) {
			assertTrue(m.quality() >= 0 && m.quality() <= 100);
		}
	}

	@Test
	void resamplesOtherResolutions() throws IOException {
		FingerprintImageData img = TestImages.wsq().copyRemovingNearWhiteFrame();
		for (int dpi : new int[] { 333, 400, 700 }) {
			int w = img.getWidth() & ~3;
			byte[] data = new byte[w * img.getHeight()];
			for (int y = 0; y < img.getHeight(); y++) {
				System.arraycopy(img.getData(), y * img.getWidth(), data, y * w, w);
			}
			try {
				assertFalse(FeatureExtractor
						.createFeatureSetFromRaw(data, w, img.getHeight(), dpi, FeatureExtractor.FEX_ENABLE_ENHANCEMENT)
						.isEmpty());
			} catch (FrfxllException e) {
				assertTrue(e.getCode() == FrfxllException.ERR_INVALID_IMAGE
						|| e.getCode() == FrfxllException.ERR_FB_TOO_SMALL_AREA, e.getMessage());
			}
		}
	}

	@Test
	void minutiaOrdering() {
		Minutia a = new Minutia((short) 5, (short) 5, 10, 50, Minutia.TYPE_RIDGE_ENDING);
		assertTrue(a.greater(new Minutia((short) 5, (short) 5, 10, 40, Minutia.TYPE_BIFURCATION)));
		assertFalse(a.greater(new Minutia((short) 5, (short) 5, 10, 60, Minutia.TYPE_OTHER)));
		assertTrue(a.greater(new Minutia((short) 5, (short) 6, 10, 50, Minutia.TYPE_OTHER)));
		assertFalse(a.greater(new Minutia((short) 5, (short) 4, 10, 50, Minutia.TYPE_OTHER)));
		assertTrue(a.greater(new Minutia((short) 6, (short) 5, 10, 50, Minutia.TYPE_OTHER)));
		assertFalse(a.greater(new Minutia((short) 4, (short) 5, 10, 50, Minutia.TYPE_OTHER)));
		assertTrue(a.greater(new Minutia((short) 5, (short) 5, 11, 50, Minutia.TYPE_OTHER)));
		assertFalse(a.greater(new Minutia((short) 5, (short) 5, 9, 50, Minutia.TYPE_OTHER)));
		assertTrue(a.greater(new Minutia((short) 5, (short) 5, 10, 50, Minutia.TYPE_OTHER)));
	}

	@Test
	void exceptionCode() {
		FrfxllException e = new FrfxllException(FrfxllException.ERR_INTERNAL, "internal");
		assertEquals(FrfxllException.ERR_INTERNAL, e.getCode());
		assertEquals("internal", e.getErrorText());
		assertEquals("FRFXLL-8007054F", e.getErrorCode());
	}

	/**
	 * Asserts that extraction fails with {@link FrfxllException#ERR_INVALID_IMAGE}.
	 *
	 * @param data   pixels
	 * @param width  width
	 * @param height height
	 * @param dpi    resolution
	 */
	private static void assertInvalid(byte[] data, int width, int height, int dpi) {
		FrfxllException e = assertThrows(FrfxllException.class, () -> FeatureExtractor.createFeatureSetFromRaw(data,
				width, height, dpi, FeatureExtractor.FEX_ENABLE_ENHANCEMENT));
		assertEquals(FrfxllException.ERR_INVALID_IMAGE, e.getCode());
	}
}
