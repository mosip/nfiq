package org.mosip.nist.nfiq2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.mosip.nist.nfiq2.prediction.RandomForestModel;
import org.mosip.nist.nfiq2.qualitymeasures.CommonFunctions;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NFIQ 2 algorithm (NIST {@code NFIQ2::Algorithm}): computes the unified quality score (0..100) of a
 * 500 ppi fingerprint image from its native quality measures with a random forest.
 * <p>
 * Instances are immutable and thread-safe once built.
 */
public final class Nfiq2 {
	/** Classpath resource of the embedded model (gzip-compressed YAML). */
	public static final String EMBEDDED_MODEL_RESOURCE = "nist_plain_tir-ink.yaml.gz";
	/** Classpath resource of the embedded model description. */
	public static final String EMBEDDED_MODEL_INFO_RESOURCE = "nist_plain_tir-ink.txt";
	/** Prediction algorithm identifier. */
	public static final String RANDOM_FOREST = "NFIQ2_RandomForest";

	/** Logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(Nfiq2.class);
	/** Lowest score. */
	private static final float MIN_QUALITY = 0;
	/** Highest score. */
	private static final float MAX_QUALITY = 100;
	/** Lowest raw prediction. */
	private static final float MIN_TREES = 0;

	/** Input order of the random forest; any change gives wrong scores. */
	private static final List<String> RF_FEATURE_ORDER = buildFeatureOrder();

	/** Random forest. */
	private final RandomForestModel model;
	/** MD5 of the model YAML text. */
	private final String parameterHash;

	/**
	 * Loads the embedded NIST model ({@code nist_plain_tir-ink}, plain optical TIR and scanned ink).
	 *
	 * @throws Nfiq2Exception {@link ErrorCode#InvalidConfiguration} when the embedded model does not match its
	 *                        hash, {@link ErrorCode#UnknownError} when it cannot be read
	 */
	public Nfiq2() {
		try (InputStream info = resource(EMBEDDED_MODEL_INFO_RESOURCE);
				InputStream gz = new GZIPInputStream(resource(EMBEDDED_MODEL_RESOURCE))) {
			String expected = ModelInfo.read(info).hash();
			model = new RandomForestModel(gz);
			if (!expected.equals(model.getHash())) {
				throw new Nfiq2Exception(ErrorCode.InvalidConfiguration,
						"The trained network could not be initialized! Error: " + model.getHash());
			}
		} catch (IOException e) {
			throw new Nfiq2Exception(ErrorCode.UnknownError, "Cannot read the embedded random forest: " + e, e);
		}
		parameterHash = model.getHash();
		LOGGER.debug("NFIQ 2 embedded random forest loaded, hash {}", parameterHash);
	}

	/**
	 * Loads a model file (YAML, optionally gzip-compressed with a {@code .gz} name).
	 *
	 * @param fileName model file
	 * @param fileHash expected MD5 of the YAML text (lower-case hex)
	 * @throws Nfiq2Exception {@link ErrorCode#BadArguments} when the file cannot be read or the hash differs
	 */
	public Nfiq2(Path fileName, String fileHash) {
		RandomForestModel m;
		try (InputStream raw = Files.newInputStream(fileName);
				InputStream in = fileName.toString().endsWith(".gz") ? new GZIPInputStream(raw) : raw) {
			m = new RandomForestModel(in);
		} catch (IOException e) {
			throw new Nfiq2Exception(ErrorCode.BadArguments,
					"Could not initialize random forest parameters with external file. Most likely, the file does "
							+ "not exist. Check the path (" + fileName + ") and hash (" + fileHash
							+ ") (initial error: " + e.getMessage() + ").",
					e);
		} catch (Nfiq2Exception e) {
			throw badHash(fileName, fileHash, e);
		}
		if (!m.getHash().equals(fileHash)) {
			throw badHash(fileName, fileHash, new Nfiq2Exception(ErrorCode.InvalidConfiguration,
					"The trained network could not be initialized! Error: " + m.getHash()));
		}
		model = m;
		parameterHash = m.getHash();
	}

	/**
	 * Loads the model described by a model information file; {@code Path} is resolved against the directory
	 * of the information file.
	 *
	 * @param modelInfoFile model information file
	 * @return algorithm
	 * @throws Nfiq2Exception {@link ErrorCode#BadArguments} when the files cannot be read or the hash differs
	 */
	public static Nfiq2 fromModelInfo(Path modelInfoFile) {
		ModelInfo info;
		try (InputStream in = Files.newInputStream(modelInfoFile)) {
			info = ModelInfo.read(in);
		} catch (IOException e) {
			throw new Nfiq2Exception(ErrorCode.BadArguments, "Cannot read model information " + modelInfoFile, e);
		}
		Path dir = modelInfoFile.toAbsolutePath().getParent();
		return new Nfiq2(dir.resolve(info.path()), info.hash());
	}

	/**
	 * Computes the unified quality score of an image.
	 *
	 * @param rawImage image (500 ppi)
	 * @return score 0..100
	 * @throws Nfiq2Exception when the image is invalid or a quality measure cannot be computed
	 */
	public int computeUnifiedQualityScore(FingerprintImageData rawImage) {
		return computeUnifiedQualityScore(QualityMeasures.computeNativeQualityMeasureAlgorithms(rawImage));
	}

	/**
	 * Computes the unified quality score from computed modules.
	 *
	 * @param modules modules of {@link QualityMeasures#computeNativeQualityMeasureAlgorithms}
	 * @return score 0..100
	 * @throws Nfiq2Exception {@link ErrorCode#QualityMeasureCalculationError} when measures are missing
	 */
	public int computeUnifiedQualityScore(List<QualityModule> modules) {
		Map<String, Double> quality = QualityMeasures.getNativeQualityMeasures(modules);
		if (quality.isEmpty()) {
			throw new Nfiq2Exception(ErrorCode.QualityMeasureCalculationError, "No features have been computed");
		}
		return (int) getQualityPrediction(quality);
	}

	/**
	 * Computes the unified quality score from native quality measures.
	 *
	 * @param features measures by identifier
	 * @return score 0..100
	 * @throws Nfiq2Exception {@link ErrorCode#QualityMeasureCalculationError} when a measure is missing
	 */
	public int computeUnifiedQualityScore(Map<String, Double> features) {
		return (int) getQualityPrediction(features);
	}

	/**
	 * {@code RandomForestML::evaluate}: raw prediction scaled to 0..100 and rounded.
	 *
	 * @param features measures by identifier
	 * @return score
	 * @throws Nfiq2Exception {@link ErrorCode#QualityMeasureCalculationError} when a measure is missing or
	 *                        the score is out of range
	 */
	double getQualityPrediction(Map<String, Double> features) {
		float[] sample = new float[RF_FEATURE_ORDER.size()];
		for (int i = 0; i < sample.length; i++) {
			Double v = features.get(RF_FEATURE_ORDER.get(i));
			if (v == null) {
				throw new Nfiq2Exception(ErrorCode.QualityMeasureCalculationError,
						"Missing quality measure " + RF_FEATURE_ORDER.get(i));
			}
			sample[i] = v.floatValue();
		}
		float raw = model.predictRaw(sample);
		float maxTrees = model.getTreeCount();
		float scaled = ((raw - MIN_TREES) / (maxTrees - MIN_TREES)) * (MAX_QUALITY - MIN_QUALITY) + MIN_QUALITY;
		double quality = Math.floor(scaled + 0.5);
		if (quality > MAX_QUALITY || quality < MIN_QUALITY) {
			throw new Nfiq2Exception(ErrorCode.QualityMeasureCalculationError, "Computed quality out of range ("
					+ quality + " not in [" + MIN_QUALITY + ", " + MAX_QUALITY + "])");
		}
		return quality;
	}

	/**
	 * Returns the MD5 of the loaded model.
	 *
	 * @return lower-case hex digest
	 */
	public String getParameterHash() {
		return parameterHash;
	}

	/**
	 * Returns the random forest input order.
	 *
	 * @return native quality measure identifiers
	 */
	public static List<String> getRandomForestFeatureOrder() {
		return RF_FEATURE_ORDER;
	}

	/**
	 * Maps native quality measures to ISO/IEC 29794-1 quality block values.
	 *
	 * @param nativeQualityMeasureValues measures by identifier
	 * @return values 0..100 (255 when undefined) by identifier
	 */
	public static Map<String, Integer> getQualityBlockValues(Map<String, Double> nativeQualityMeasureValues) {
		Map<String, Integer> ret = new LinkedHashMap<>();
		nativeQualityMeasureValues.forEach((k, v) -> ret.put(k, getQualityBlockValue(k, v)));
		return ret;
	}

	/**
	 * Maps one native quality measure to its ISO/IEC 29794-1 quality block value.
	 *
	 * @param featureIdentifier   identifier
	 * @param nativeQualityMeasure value
	 * @return value 0..100 (255 when undefined)
	 */
	public static int getQualityBlockValue(String featureIdentifier, double nativeQualityMeasure) {
		return CommonFunctions.getQualityBlockValue(featureIdentifier, nativeQualityMeasure);
	}

	/**
	 * Opens a resource of this package.
	 *
	 * @param name resource name
	 * @return stream
	 * @throws IOException when the resource is missing
	 */
	private static InputStream resource(String name) throws IOException {
		InputStream in = Nfiq2.class.getResourceAsStream(name);
		if (in == null) {
			throw new IOException("Resource not found: " + name);
		}
		return in;
	}

	/**
	 * Wraps a model error as NIST does for external files.
	 *
	 * @param fileName model file
	 * @param fileHash expected hash
	 * @param cause    error
	 * @return exception
	 */
	private static Nfiq2Exception badHash(Path fileName, String fileHash, Nfiq2Exception cause) {
		return new Nfiq2Exception(ErrorCode.BadArguments,
				"Could not initialize random forest parameters with external file. Most likely, the hash is not "
						+ "correct. Check the path (" + fileName + ") and hash (" + fileHash + ") (initial error: "
						+ cause.getMessage() + ").",
				cause);
	}

	/**
	 * {@code rfFeatureOrder} of {@code RandomForestML::evaluate}.
	 *
	 * @return identifiers
	 */
	private static List<String> buildFeatureOrder() {
		List<String> order = new ArrayList<>(69);
		order.addAll(FDA.getNativeQualityMeasureIDs());
		order.add(FingerJetFX.COUNT_COM);
		order.add(FingerJetFX.COUNT);
		order.add(FJFXMinutiaeQuality.PERCENT_IMAGE_MEAN_50);
		order.add(FJFXMinutiaeQuality.PERCENT_ORIENTATION_CERTAINTY_80);
		order.add(ImgProcROI.MEAN);
		order.addAll(LCS.getNativeQualityMeasureIDs());
		order.add(Mu.MEAN_OF_BLOCK_MEANS);
		order.add(Mu.IMAGE_MEAN);
		order.addAll(OCLHistogram.getNativeQualityMeasureIDs());
		order.addAll(OF.getNativeQualityMeasureIDs());
		order.add(QualityMap.COHERENCE_MEAN);
		order.add(QualityMap.COHERENCE_SUM);
		order.addAll(RVUPHistogram.getNativeQualityMeasureIDs());
		return Collections.unmodifiableList(order);
	}
}
