package org.mosip.nist.nfiq2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mosip.nist.nfiq2.qualitymeasures.Mu;
import org.mosip.nist.nfiq2.qualitymeasures.QualityModule;

/**
 * Tests of {@link QualityMeasures}.
 */
class QualityMeasuresTest {

	@Test
	void identifiers() {
		assertEquals(69, QualityMeasures.getNativeQualityMeasureIDs().size());
		assertTrue(QualityMeasures.getNativeQualityMeasureIDs().containsAll(Nfiq2.getRandomForestFeatureOrder()));
		assertEquals(10, QualityMeasures.getNativeQualityMeasureAlgorithmIDs().size());
		assertEquals(4, QualityMeasures.getActionableQualityFeedbackIDs().size());
	}

	@Test
	void measuresOfImage() throws IOException {
		List<QualityModule> modules = TestImages.jp2Modules();
		Map<String, Double> measures = QualityMeasures.getNativeQualityMeasures(modules);
		assertEquals(QualityMeasures.getNativeQualityMeasureIDs().size(), measures.size());
		assertTrue(measures.keySet().containsAll(QualityMeasures.getNativeQualityMeasureIDs()));

		Map<String, Double> speeds = QualityMeasures.getNativeQualityMeasureAlgorithmSpeeds(modules);
		assertEquals(QualityMeasures.getNativeQualityMeasureAlgorithmIDs(), List.copyOf(speeds.keySet()));
		speeds.values().forEach(s -> assertTrue(s >= 0));
	}

	@Test
	void computesFromImage() throws IOException {
		FingerprintImageData wsq = TestImages.wsq();
		assertEquals(69, QualityMeasures.computeNativeQualityMeasures(wsq).size());
		Map<String, Double> feedback = QualityMeasures.computeActionableQualityFeedback(wsq);
		assertEquals(QualityMeasures.getActionableQualityFeedbackIDs(), List.copyOf(feedback.keySet()));
		feedback.values().forEach(v -> assertTrue(!v.isNaN()));
	}

	@Test
	void uniformImageStopsFeedback() {
		Map<String, Double> feedback = QualityMeasures.getActionableQualityFeedback(List.of(new Mu(filled(128))));
		assertEquals(0.0, feedback.get(QualityMeasures.UNIFORM_IMAGE));
		assertEquals(128.0, feedback.get(QualityMeasures.EMPTY_IMAGE_OR_CONTRAST_TOO_LOW));
		assertTrue(feedback.get(QualityMeasures.FINGERPRINT_IMAGE_WITH_MINUTIAE).isNaN());
		assertTrue(feedback.get(QualityMeasures.SUFFICIENT_FINGERPRINT_FOREGROUND).isNaN());
	}

	@Test
	void emptyImageStopsFeedback() throws IOException {
		byte[] data = new byte[100 * 100];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) (i % 2 == 0 ? 248 : 255);
		}
		Mu mu = new Mu(new FingerprintImageData(data, 100, 100, 0, 500));
		List<QualityModule> modules = List.of(mu, TestImages.jp2Modules().get(1));
		Map<String, Double> feedback = QualityMeasures.getActionableQualityFeedback(modules);
		assertEquals(251.5, feedback.get(QualityMeasures.EMPTY_IMAGE_OR_CONTRAST_TOO_LOW), 1e-9);
		assertTrue(feedback.get(QualityMeasures.FINGERPRINT_IMAGE_WITH_MINUTIAE).isNaN());
	}

	@Test
	void noModulesGiveNaNFeedback() {
		Map<String, Double> feedback = QualityMeasures.getActionableQualityFeedback(List.of());
		feedback.values().forEach(v -> assertTrue(v.isNaN()));
	}

	/**
	 * Returns a uniform 100x100 image.
	 *
	 * @param grey grey value
	 * @return image
	 */
	private static FingerprintImageData filled(int grey) {
		byte[] data = new byte[100 * 100];
		Arrays.fill(data, (byte) grey);
		return new FingerprintImageData(data, 100, 100, 0, 500);
	}
}
