package org.mosip.nist.nfiq2.qualitymeasures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mosip.nist.nfiq2.ErrorCode;
import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;
import org.mosip.nist.nfiq2.TestImages;
import org.mosip.nist.nfiq2.frfxll.FrfxllMinutia;

/**
 * Tests of {@link FingerJetFX} and {@link FJFXMinutiaeQuality}.
 */
class FingerJetFXTest {

	@Test
	void blankImageFails() {
		byte[] white = new byte[300 * 300];
		Arrays.fill(white, (byte) 255);
		FingerprintImageData image = new FingerprintImageData(white, 300, 300, 0, 500);
		Nfiq2Exception e = assertThrows(Nfiq2Exception.class, () -> new FingerJetFX(image));
		assertEquals(ErrorCode.FJFX_CannotCreateFeatureSet, e.getNfiq2ErrorCode());
	}

	@Test
	void smallImageIsPadded() throws IOException {
		FingerprintImageData full = TestImages.jp2Cropped();
		int w = 180;
		int h = full.getHeight();
		int x0 = (full.getWidth() - w) / 2;
		byte[] data = new byte[w * h];
		for (int y = 0; y < h; y++) {
			System.arraycopy(full.getData(), y * full.getWidth() + x0, data, y * w, w);
		}
		FingerJetFX fjfx = new FingerJetFX(new FingerprintImageData(data, w, h, 0, 500));
		assertEquals(FingerJetFX.NAME, fjfx.getName());
		for (FrfxllMinutia m : fjfx.getMinutiaData()) {
			assertTrue(m.x() < w && m.y() < h);
		}
		assertTrue(fjfx.getFeatures().get(FingerJetFX.COUNT) >= fjfx.getMinutiaData().size());
	}

	@Test
	void comRectangle() {
		List<FrfxllMinutia> minutiae = List.of(new FrfxllMinutia(100, 100, 0, 1, 50),
				new FrfxllMinutia(110, 110, 0, 1, 50), new FrfxllMinutia(130, 130, 0, 1, 50),
				new FrfxllMinutia(450, 450, 0, 1, 50));
		assertEquals(3, FingerJetFX.countInComRect(minutiae, 500, 500));
		Nfiq2Exception e = assertThrows(Nfiq2Exception.class, () -> FingerJetFX.countInComRect(List.of(), 10, 10));
		assertEquals(ErrorCode.QualityMeasureCalculationError, e.getNfiq2ErrorCode());
	}

	@Test
	void minutiaQualityNeedsOneBlock() {
		byte[] img = new byte[20 * 20];
		FrfxllMinutia m = new FrfxllMinutia(5, 5, 0, 1, 50);
		Nfiq2Exception e = assertThrows(Nfiq2Exception.class, () -> FJFXMinutiaeQuality.oclQuality(img, 20, 20, m));
		assertEquals(ErrorCode.QualityMeasureCalculationError, e.getNfiq2ErrorCode());
	}

	@Test
	void minutiaQualityAtImageEdges() {
		byte[] img = new byte[64 * 64];
		for (int i = 0; i < img.length; i++) {
			img[i] = (byte) ((i % 64) / 4 % 2 == 0 ? 40 : 220);
		}
		FingerprintImageData image = new FingerprintImageData(img, 64, 64, 0, 500);
		List<FrfxllMinutia> minutiae = List.of(new FrfxllMinutia(0, 0, 0, 1, 50), new FrfxllMinutia(63, 63, 0, 1, 50));
		FJFXMinutiaeQuality quality = new FJFXMinutiaeQuality(image, minutiae);
		assertEquals(FJFXMinutiaeQuality.NAME, quality.getName());
		assertEquals(2, quality.getFeatures().size());
		assertEquals(0, FJFXMinutiaeQuality.oclQuality(new byte[64 * 64], 64, 64, minutiae.get(1)));
	}
}
