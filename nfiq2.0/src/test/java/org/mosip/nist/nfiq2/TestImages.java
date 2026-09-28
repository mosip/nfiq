package org.mosip.nist.nfiq2;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.mosip.nist.nfiq2.imagetools.IsoImageDecoder;
import org.mosip.nist.nfiq2.qualitymeasures.QualityModule;

/**
 * Sample ISO/IEC 19794-4 records of {@code src/test/resources}, read in place, with their decoded images and
 * modules cached for the whole test run.
 */
public final class TestImages {
	/** Directory of the sample records. */
	public static final Path DIR = Path.of("src", "test", "resources");
	/** JPEG 2000 sample. */
	public static final Path JP2 = DIR.resolve("info_jp2.iso");
	/** WSQ sample. */
	public static final Path WSQ = DIR.resolve("info_wsq.iso");

	/** Decoded JPEG 2000 sample. */
	private static FingerprintImageData jp2;
	/** Decoded WSQ sample. */
	private static FingerprintImageData wsq;
	/** Modules of the JPEG 2000 sample. */
	private static List<QualityModule> jp2Modules;
	/** Modules of the WSQ sample. */
	private static List<QualityModule> wsqModules;

	/** Static helpers only. */
	private TestImages() {
	}

	/**
	 * Returns the decoded JPEG 2000 sample.
	 *
	 * @return image
	 * @throws IOException when the file cannot be read
	 */
	public static synchronized FingerprintImageData jp2() throws IOException {
		if (jp2 == null) {
			jp2 = IsoImageDecoder.decode(JP2);
		}
		return jp2;
	}

	/**
	 * Returns the decoded WSQ sample.
	 *
	 * @return image
	 * @throws IOException when the file cannot be read
	 */
	public static synchronized FingerprintImageData wsq() throws IOException {
		if (wsq == null) {
			wsq = IsoImageDecoder.decode(WSQ);
		}
		return wsq;
	}

	/**
	 * Returns the modules of the JPEG 2000 sample.
	 *
	 * @return modules
	 * @throws IOException when the file cannot be read
	 */
	public static synchronized List<QualityModule> jp2Modules() throws IOException {
		if (jp2Modules == null) {
			jp2Modules = QualityMeasures.computeNativeQualityMeasureAlgorithms(jp2());
		}
		return jp2Modules;
	}

	/**
	 * Returns the modules of the WSQ sample.
	 *
	 * @return modules
	 * @throws IOException when the file cannot be read
	 */
	public static synchronized List<QualityModule> wsqModules() throws IOException {
		if (wsqModules == null) {
			wsqModules = QualityMeasures.computeNativeQualityMeasureAlgorithms(wsq());
		}
		return wsqModules;
	}

	/**
	 * Returns the cropped JPEG 2000 sample, as seen by the modules.
	 *
	 * @return image
	 * @throws IOException when the file cannot be read
	 */
	public static FingerprintImageData jp2Cropped() throws IOException {
		return jp2().copyRemovingNearWhiteFrame();
	}
}
