package org.mosip.nist.nfiq2.test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2;
import org.mosip.nist.nfiq2.Nfiq2Exception;
import org.mosip.nist.nfiq2.QualityMeasures;
import org.mosip.nist.nfiq2.imagetools.IsoImageDecoder;
import org.mosip.nist.nfiq2.qualitymeasures.QualityModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample harness used by {@code run-local-JP2} / {@code run-local-WSQ}: scores one ISO/IEC 19794-4 record.
 * <p>
 * Arguments: {@code imgfile=<iso>} (relative to the working directory) and optionally {@code logs=1} to also
 * print the actionable feedback, the 69 native quality measures and the module timings.
 */
public final class Nfiq2Application {
	/** Logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(Nfiq2Application.class);
	/** Exit code for a missing or bad argument. */
	private static final int EXIT_USAGE = 2;
	/** Exit code for an NFIQ 2 failure. */
	private static final int EXIT_FAILURE = 1;

	/** Entry point only. */
	private Nfiq2Application() {
	}

	/**
	 * Scores the record given on the command line.
	 *
	 * @param args {@code imgfile=<iso>} and optionally {@code logs=0|1}
	 */
	public static void main(String[] args) {
		String imageFile = null;
		boolean details = false;
		for (String arg : args) {
			if (arg.startsWith("imgfile=")) {
				imageFile = arg.substring("imgfile=".length());
			} else if (arg.startsWith("logs=")) {
				details = "1".equals(arg.substring("logs=".length()));
			}
		}
		if (imageFile == null || imageFile.isEmpty()) {
			LOGGER.error("Usage: Nfiq2Application imgfile=<ISO 19794-4 record> [logs=0|1]");
			System.exit(EXIT_USAGE);
		}
		try {
			FingerprintImageData image = IsoImageDecoder.decode(Path.of(imageFile));
			List<QualityModule> modules = QualityMeasures.computeNativeQualityMeasureAlgorithms(image);
			int score = new Nfiq2().computeUnifiedQualityScore(modules);
			if (details) {
				print("ACTIONABLE FEEDBACK", QualityMeasures.getActionableQualityFeedback(modules));
				print("NATIVE QUALITY MEASURES", QualityMeasures.getNativeQualityMeasures(modules));
				print("MODULE SPEEDS (ms)", QualityMeasures.getNativeQualityMeasureAlgorithmSpeeds(modules));
			}
			LOGGER.info("NFIQ2={} (image {}x{}, {} ppi)", score, image.getWidth(), image.getHeight(), image.getPpi());
		} catch (Nfiq2Exception e) {
			LOGGER.error("NFIQ 2 failed [{} {}]: {}", e.getErrorCode(), e.getNfiq2ErrorCode(), e.getErrorText(), e);
			System.exit(EXIT_FAILURE);
		} catch (Exception e) {
			LOGGER.error("Cannot score {}", imageFile, e);
			System.exit(EXIT_FAILURE);
		}
	}

	/**
	 * Logs a titled block of values, one per line.
	 *
	 * @param title  block title
	 * @param values values by identifier
	 */
	private static void print(String title, Map<String, Double> values) {
		StringBuilder sb = new StringBuilder(title).append(System.lineSeparator());
		values.forEach((k, v) -> sb.append(String.format(" %-40s %s%n", k, v)));
		LOGGER.info("{}", sb);
	}
}
