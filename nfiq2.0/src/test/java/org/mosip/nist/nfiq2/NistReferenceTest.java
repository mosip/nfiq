package org.mosip.nist.nfiq2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mosip.nist.nfiq2.qualitymeasures.QualityModule;

/**
 * Compares the Java port with the NIST NFIQ 2.3.0 Windows binary ({@code nfiq2 -F -v -a}) run on the decoded
 * sample images: scores, actionable feedback and a selection of native quality measures.
 */
class NistReferenceTest {
	/** Relative tolerance of the NIST CSV values (printed with 5 decimals). */
	private static final double TOLERANCE = 1e-4;

	/** Algorithm with the embedded model. */
	private static Nfiq2 nfiq2;

	@BeforeAll
	static void load() {
		nfiq2 = new Nfiq2();
	}

	@Test
	void jp2MatchesNist() throws IOException {
		List<QualityModule> modules = TestImages.jp2Modules();
		assertEquals(72, nfiq2.computeUnifiedQualityScore(modules));
		assertValues(QualityMeasures.getActionableQualityFeedback(modules),
				Map.of(QualityMeasures.UNIFORM_IMAGE, 71.12043, QualityMeasures.EMPTY_IMAGE_OR_CONTRAST_TOO_LOW,
						157.14307, QualityMeasures.FINGERPRINT_IMAGE_WITH_MINUTIAE, 99.0,
						QualityMeasures.SUFFICIENT_FINGERPRINT_FOREGROUND, 118033.0));
		assertValues(QualityMeasures.getNativeQualityMeasures(modules),
				Map.ofEntries(Map.entry("FDA_Bin10_0", 7.0), Map.entry("FDA_Bin10_Mean", 0.41055),
						Map.entry("FingerJetFX_MinutiaeCount", 99.0),
						Map.entry("FingerJetFX_MinCount_COMMinRect200x200", 39.0),
						Map.entry("FJFXPos_Mu_MinutiaeQuality_2", 0.34343),
						Map.entry("FJFXPos_OCL_MinutiaeQuality_80", 0.30303),
						Map.entry("ImgProcROIArea_Mean", 133.38348), Map.entry("LCS_Bin10_Mean", 0.77002),
						Map.entry("MMB", 158.51614), Map.entry("Mu", 157.14307), Map.entry("OCL_Bin10_0", 4.0),
						Map.entry("OCL_Bin10_Mean", 0.73355), Map.entry("OF_Bin10_Mean", 0.36314),
						Map.entry("OrientationMap_ROIFilter_CoherenceRel", 0.60222),
						Map.entry("OrientationMap_ROIFilter_CoherenceSum", 83.10683),
						Map.entry("RVUP_Bin10_Mean", 1.15753), Map.entry("RVUP_Bin10_StdDev", 0.80387)));
	}

	@Test
	void wsqMatchesNist() throws IOException {
		List<QualityModule> modules = TestImages.wsqModules();
		assertEquals(75, nfiq2.computeUnifiedQualityScore(modules));
		assertValues(QualityMeasures.getActionableQualityFeedback(modules),
				Map.of(QualityMeasures.UNIFORM_IMAGE, 52.38722, QualityMeasures.EMPTY_IMAGE_OR_CONTRAST_TOO_LOW,
						167.13942, QualityMeasures.FINGERPRINT_IMAGE_WITH_MINUTIAE, 141.0,
						QualityMeasures.SUFFICIENT_FINGERPRINT_FOREGROUND, 194758.0));
		assertValues(QualityMeasures.getNativeQualityMeasures(modules),
				Map.ofEntries(Map.entry("FDA_Bin10_0", 5.0), Map.entry("FDA_Bin10_Mean", 0.48754),
						Map.entry("FingerJetFX_MinutiaeCount", 141.0),
						Map.entry("FingerJetFX_MinCount_COMMinRect200x200", 33.0),
						Map.entry("FJFXPos_Mu_MinutiaeQuality_2", 0.27659),
						Map.entry("FJFXPos_OCL_MinutiaeQuality_80", 0.12766),
						Map.entry("ImgProcROIArea_Mean", 136.95294), Map.entry("LCS_Bin10_Mean", 0.77700),
						Map.entry("MMB", 167.55676), Map.entry("Mu", 167.13942), Map.entry("OCL_Bin10_0", 37.0),
						Map.entry("OCL_Bin10_Mean", 0.63747), Map.entry("OF_Bin10_Mean", 0.25860),
						Map.entry("OrientationMap_ROIFilter_CoherenceRel", 0.58655),
						Map.entry("OrientationMap_ROIFilter_CoherenceSum", 129.62747),
						Map.entry("RVUP_Bin10_Mean", 1.11252), Map.entry("RVUP_Bin10_StdDev", 0.56709)));
	}

	/**
	 * Asserts that every expected value is present and within {@link #TOLERANCE}.
	 *
	 * @param actual   computed values
	 * @param expected NIST values
	 */
	private static void assertValues(Map<String, Double> actual, Map<String, Double> expected) {
		for (Map.Entry<String, Double> e : expected.entrySet()) {
			Double value = actual.get(e.getKey());
			assertNotNull(value, e.getKey());
			assertEquals(e.getValue(), value, TOLERANCE * Math.max(1, Math.abs(e.getValue())), e.getKey());
		}
	}
}
