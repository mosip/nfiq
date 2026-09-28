package org.mosip.nist.nfiq2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mosip.nist.nfiq2.qualitymeasures.QualityModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * End-to-end tests of {@link Nfiq2} on the sample ISO records.
 */
class Nfiq2Test {
	/** Logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(Nfiq2Test.class);
	/** Hash of the embedded model. */
	private static final String MODEL_HASH = "b4a1e7586b3be906f9770e4b77768038";

	/** Algorithm with the embedded model. */
	private static Nfiq2 nfiq2;

	@BeforeAll
	static void load() {
		nfiq2 = new Nfiq2();
	}

	@Test
	void embeddedModelHash() {
		assertEquals(MODEL_HASH, nfiq2.getParameterHash());
		assertEquals(69, Nfiq2.getRandomForestFeatureOrder().size());
	}

	@Test
	void scoresJp2() throws IOException {
		int score = nfiq2.computeUnifiedQualityScore(TestImages.jp2());
		LOGGER.info("NFIQ 2 score of info_jp2.iso: {}", score);
		assertTrue(score >= 0 && score <= 100);
		assertEquals(score, nfiq2.computeUnifiedQualityScore(TestImages.jp2Modules()));
		assertEquals(score,
				nfiq2.computeUnifiedQualityScore(QualityMeasures.getNativeQualityMeasures(TestImages.jp2Modules())));
	}

	@Test
	void scoresWsq() throws IOException {
		int score = nfiq2.computeUnifiedQualityScore(TestImages.wsq());
		LOGGER.info("NFIQ 2 score of info_wsq.iso: {}", score);
		assertTrue(score >= 0 && score <= 100);
	}

	@Test
	void missingFeature() throws IOException {
		Map<String, Double> features = new HashMap<>(
				QualityMeasures.getNativeQualityMeasures(TestImages.jp2Modules()));
		features.remove(Nfiq2.getRandomForestFeatureOrder().get(0));
		Nfiq2Exception e = assertThrows(Nfiq2Exception.class, () -> nfiq2.computeUnifiedQualityScore(features));
		assertEquals(ErrorCode.QualityMeasureCalculationError, e.getNfiq2ErrorCode());
	}

	@Test
	void noModules() {
		List<QualityModule> none = List.of();
		Nfiq2Exception e = assertThrows(Nfiq2Exception.class, () -> nfiq2.computeUnifiedQualityScore(none));
		assertEquals(ErrorCode.QualityMeasureCalculationError, e.getNfiq2ErrorCode());
	}

	@Test
	void qualityBlockValues() throws IOException {
		Map<String, Double> features = QualityMeasures.getNativeQualityMeasures(TestImages.jp2Modules());
		Map<String, Integer> blocks = Nfiq2.getQualityBlockValues(features);
		assertEquals(features.keySet(), blocks.keySet());
		for (Map.Entry<String, Integer> e : blocks.entrySet()) {
			assertTrue(e.getValue() >= 0 && e.getValue() <= 255, e.getKey());
		}
		assertEquals(255, Nfiq2.getQualityBlockValue("FDA_Bin10_0", 3));
	}

	@Test
	void externalModel(@TempDir Path dir) throws IOException {
		Path yaml = dir.resolve("model.yaml");
		try (InputStream in = new GZIPInputStream(Nfiq2.class.getResourceAsStream(Nfiq2.EMBEDDED_MODEL_RESOURCE));
				OutputStream out = Files.newOutputStream(yaml)) {
			in.transferTo(out);
		}
		Nfiq2 external = new Nfiq2(yaml, MODEL_HASH);
		assertEquals(MODEL_HASH, external.getParameterHash());
		assertEquals(nfiq2.computeUnifiedQualityScore(TestImages.jp2Modules()),
				external.computeUnifiedQualityScore(TestImages.jp2Modules()));

		Path info = dir.resolve("model.txt");
		Files.writeString(info, "Name = test\nPath = model.yaml\nHash = " + MODEL_HASH + "\n");
		assertEquals(MODEL_HASH, Nfiq2.fromModelInfo(info).getParameterHash());

		Nfiq2Exception wrongHash = assertThrows(Nfiq2Exception.class, () -> new Nfiq2(yaml, "00"));
		assertEquals(ErrorCode.BadArguments, wrongHash.getNfiq2ErrorCode());
		assertEquals(ErrorCode.InvalidConfiguration, ((Nfiq2Exception) wrongHash.getCause()).getNfiq2ErrorCode());

		Path missing = dir.resolve("missing.yaml");
		Nfiq2Exception noFile = assertThrows(Nfiq2Exception.class, () -> new Nfiq2(missing, MODEL_HASH));
		assertEquals(ErrorCode.BadArguments, noFile.getNfiq2ErrorCode());

		Path bad = dir.resolve("bad.yaml");
		Files.writeString(bad, "my_random_trees:\n   is_classifier: 0\n");
		Nfiq2Exception badModel = assertThrows(Nfiq2Exception.class, () -> new Nfiq2(bad, MODEL_HASH));
		assertEquals(ErrorCode.BadArguments, badModel.getNfiq2ErrorCode());

		Path noInfo = dir.resolve("none.txt");
		assertThrows(Nfiq2Exception.class, () -> Nfiq2.fromModelInfo(noInfo));
	}

	@Test
	void gzipExternalModel(@TempDir Path dir) throws IOException {
		Path gz = dir.resolve("model.yaml.gz");
		try (InputStream in = Nfiq2.class.getResourceAsStream(Nfiq2.EMBEDDED_MODEL_RESOURCE)) {
			assertNotNull(in);
			Files.copy(in, gz);
		}
		assertFalse(new Nfiq2(gz, MODEL_HASH).getParameterHash().isEmpty());
	}
}
