package org.mosip.nist.nfiq2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mosip.nist.nfiq2.qualitymeasures.FDA;
import org.mosip.nist.nfiq2.qualitymeasures.FJFXMinutiaeQuality;
import org.mosip.nist.nfiq2.qualitymeasures.FingerJetFX;
import org.mosip.nist.nfiq2.qualitymeasures.ImgProcROI;
import org.mosip.nist.nfiq2.qualitymeasures.LCS;
import org.mosip.nist.nfiq2.qualitymeasures.Mu;
import org.mosip.nist.nfiq2.qualitymeasures.OCLHistogram;
import org.mosip.nist.nfiq2.qualitymeasures.OF;
import org.mosip.nist.nfiq2.qualitymeasures.QualityMap;
import org.mosip.nist.nfiq2.qualitymeasures.QualityModule;
import org.mosip.nist.nfiq2.qualitymeasures.RVUPHistogram;

/**
 * Computation of the native quality measures and actionable feedback (NIST {@code NFIQ2::QualityMeasures}).
 */
public final class QualityMeasures {
	/** Standard deviation of the grey values (image is uniform below the threshold). */
	public static final String UNIFORM_IMAGE = "UniformImage";
	/** Mean grey value (image is empty or has too little contrast above the threshold). */
	public static final String EMPTY_IMAGE_OR_CONTRAST_TOO_LOW = "EmptyImageOrContrastTooLow";
	/** Number of minutiae. */
	public static final String FINGERPRINT_IMAGE_WITH_MINUTIAE = "FingerprintImageWithMinutiae";
	/** Number of foreground (region of interest) pixels. */
	public static final String SUFFICIENT_FINGERPRINT_FOREGROUND = "SufficientFingerprintForeground";

	/** Threshold of {@link #UNIFORM_IMAGE}. */
	public static final double UNIFORM_IMAGE_THRESHOLD = 1.0;
	/** Threshold of {@link #EMPTY_IMAGE_OR_CONTRAST_TOO_LOW}. */
	public static final double EMPTY_IMAGE_OR_CONTRAST_TOO_LOW_THRESHOLD = 250.0;
	/** Threshold of {@link #FINGERPRINT_IMAGE_WITH_MINUTIAE}. */
	public static final double FINGERPRINT_IMAGE_WITH_MINUTIAE_THRESHOLD = 5.0;
	/** Threshold of {@link #SUFFICIENT_FINGERPRINT_FOREGROUND}. */
	public static final double SUFFICIENT_FINGERPRINT_FOREGROUND_THRESHOLD = 50000.0;

	/** Static helpers only. */
	private QualityMeasures() {
	}

	/**
	 * Crops the near-white frame and runs every quality module, in NIST order.
	 *
	 * @param rawImage image
	 * @return modules
	 * @throws Nfiq2Exception when the image is invalid or a module fails
	 */
	public static List<QualityModule> computeNativeQualityMeasureAlgorithms(FingerprintImageData rawImage) {
		FingerprintImageData cropped = rawImage.copyRemovingNearWhiteFrame();
		List<QualityModule> modules = new ArrayList<>(10);
		modules.add(new FDA(cropped));
		FingerJetFX fjfx = new FingerJetFX(cropped);
		modules.add(fjfx);
		modules.add(new FJFXMinutiaeQuality(cropped, fjfx.getMinutiaData()));
		ImgProcROI roi = new ImgProcROI(cropped);
		modules.add(roi);
		modules.add(new LCS(cropped));
		modules.add(new Mu(cropped));
		modules.add(new OCLHistogram(cropped));
		modules.add(new OF(cropped));
		modules.add(new QualityMap(cropped, roi.getImgProcResults()));
		modules.add(new RVUPHistogram(cropped));
		return Collections.unmodifiableList(modules);
	}

	/**
	 * Computes the native quality measures of an image.
	 *
	 * @param rawImage image
	 * @return measures by identifier
	 * @throws Nfiq2Exception when the image is invalid or a module fails
	 */
	public static Map<String, Double> computeNativeQualityMeasures(FingerprintImageData rawImage) {
		return getNativeQualityMeasures(computeNativeQualityMeasureAlgorithms(rawImage));
	}

	/**
	 * Collects the measures of computed modules.
	 *
	 * @param modules modules
	 * @return measures by identifier, in module order
	 */
	public static Map<String, Double> getNativeQualityMeasures(List<QualityModule> modules) {
		Map<String, Double> quality = new LinkedHashMap<>();
		for (QualityModule module : modules) {
			quality.putAll(module.getFeatures());
		}
		return quality;
	}

	/**
	 * Computes the actionable feedback of an image.
	 *
	 * @param rawImage image
	 * @return feedback by identifier
	 * @throws Nfiq2Exception when the image is invalid or a module fails
	 */
	public static Map<String, Double> computeActionableQualityFeedback(FingerprintImageData rawImage) {
		return getActionableQualityFeedback(computeNativeQualityMeasureAlgorithms(rawImage));
	}

	/**
	 * Derives the actionable feedback from computed modules. Values not reached (an empty or uniform image
	 * stops the evaluation at the contrast module) stay NaN.
	 *
	 * @param modules modules
	 * @return feedback by identifier
	 */
	public static Map<String, Double> getActionableQualityFeedback(List<QualityModule> modules) {
		Map<String, Double> actionable = new LinkedHashMap<>();
		for (String id : getActionableQualityFeedbackIDs()) {
			actionable.put(id, Double.NaN);
		}
		for (QualityModule module : modules) {
			if (module instanceof Mu mu) {
				double sigma = mu.getSigma();
				actionable.put(UNIFORM_IMAGE, sigma);
				boolean uniform = sigma < UNIFORM_IMAGE_THRESHOLD;
				boolean empty = false;
				Double mean = mu.getFeatures().get(Mu.IMAGE_MEAN);
				if (mean != null) {
					actionable.put(EMPTY_IMAGE_OR_CONTRAST_TOO_LOW, mean);
					empty = mean > EMPTY_IMAGE_OR_CONTRAST_TOO_LOW_THRESHOLD;
				}
				if (empty || uniform) {
					return actionable;
				}
			} else if (module instanceof FingerJetFX fjfx) {
				Double count = fjfx.getFeatures().get(FingerJetFX.COUNT);
				if (count != null) {
					actionable.put(FINGERPRINT_IMAGE_WITH_MINUTIAE, count);
				}
			} else if (module instanceof ImgProcROI roi) {
				actionable.put(SUFFICIENT_FINGERPRINT_FOREGROUND, (double) roi.getImgProcResults().noOfROIPixels());
			}
		}
		return actionable;
	}

	/**
	 * Returns the processing time of each module.
	 *
	 * @param modules modules
	 * @return milliseconds by algorithm identifier
	 */
	public static Map<String, Double> getNativeQualityMeasureAlgorithmSpeeds(List<QualityModule> modules) {
		Map<String, Double> speeds = new LinkedHashMap<>();
		for (QualityModule module : modules) {
			speeds.put(module.getName(), module.getSpeed());
		}
		return speeds;
	}

	/**
	 * Returns the actionable feedback identifiers.
	 *
	 * @return identifiers
	 */
	public static List<String> getActionableQualityFeedbackIDs() {
		return List.of(UNIFORM_IMAGE, EMPTY_IMAGE_OR_CONTRAST_TOO_LOW, FINGERPRINT_IMAGE_WITH_MINUTIAE,
				SUFFICIENT_FINGERPRINT_FOREGROUND);
	}

	/**
	 * Returns every native quality measure identifier, in module order.
	 *
	 * @return identifiers
	 */
	public static List<String> getNativeQualityMeasureIDs() {
		List<String> ids = new ArrayList<>();
		ids.addAll(FDA.getNativeQualityMeasureIDs());
		ids.addAll(FingerJetFX.getNativeQualityMeasureIDs());
		ids.addAll(FJFXMinutiaeQuality.getNativeQualityMeasureIDs());
		ids.addAll(ImgProcROI.getNativeQualityMeasureIDs());
		ids.addAll(LCS.getNativeQualityMeasureIDs());
		ids.addAll(Mu.getNativeQualityMeasureIDs());
		ids.addAll(OCLHistogram.getNativeQualityMeasureIDs());
		ids.addAll(OF.getNativeQualityMeasureIDs());
		ids.addAll(QualityMap.getNativeQualityMeasureIDs());
		ids.addAll(RVUPHistogram.getNativeQualityMeasureIDs());
		return Collections.unmodifiableList(ids);
	}

	/**
	 * Returns the module (algorithm) identifiers, in module order.
	 *
	 * @return identifiers
	 */
	public static List<String> getNativeQualityMeasureAlgorithmIDs() {
		return List.of(FDA.NAME, FingerJetFX.NAME, FJFXMinutiaeQuality.NAME, ImgProcROI.NAME, LCS.NAME, Mu.NAME,
				OCLHistogram.NAME, OF.NAME, QualityMap.NAME, RVUPHistogram.NAME);
	}
}
