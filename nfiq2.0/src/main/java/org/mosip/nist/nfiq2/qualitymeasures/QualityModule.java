package org.mosip.nist.nfiq2.qualitymeasures;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mosip.nist.nfiq2.ErrorCode;
import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;

/**
 * One NFIQ 2 quality module (NIST {@code NFIQ2::QualityMeasures::Algorithm}): computes its native quality
 * measures when constructed and exposes them by identifier.
 */
public abstract class QualityModule {
	/** Native quality measures, in computation order. */
	private final Map<String, Double> features = new LinkedHashMap<>();
	/** Computation time in milliseconds. */
	private double speed;

	/**
	 * Returns the algorithm identifier (for example {@code "FrequencyDomainAnalysis"}).
	 *
	 * @return identifier
	 */
	public abstract String getName();

	/**
	 * Returns the native quality measures computed by this module.
	 *
	 * @return read-only map from measure identifier to value
	 */
	public Map<String, Double> getFeatures() {
		return Collections.unmodifiableMap(features);
	}

	/**
	 * Returns how long the computation took.
	 *
	 * @return milliseconds
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * Stores one native quality measure.
	 *
	 * @param id    identifier
	 * @param value value
	 */
	protected void put(String id, double value) {
		features.put(id, value);
	}

	/**
	 * Stores all measures of a map.
	 *
	 * @param values measures
	 */
	protected void putAll(Map<String, Double> values) {
		features.putAll(values);
	}

	/**
	 * Records the computation time since {@code startNanos}.
	 *
	 * @param startNanos {@link System#nanoTime()} at the start
	 */
	protected void setSpeedSince(long startNanos) {
		speed = (System.nanoTime() - startNanos) / 1_000_000.0;
	}

	/**
	 * Rejects images that are not 500 ppi.
	 *
	 * @param image image
	 * @throws Nfiq2Exception ({@link ErrorCode#QualityMeasureCalculationError}) otherwise
	 */
	static void require500Ppi(FingerprintImageData image) {
		if (image.getPpi() != FingerprintImageData.RESOLUTION_500_PPI) {
			throw new Nfiq2Exception(ErrorCode.QualityMeasureCalculationError,
					"Only 500 dpi fingerprint images are supported!");
		}
	}
}
