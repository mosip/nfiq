package org.mosip.nist.nfiq2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Tests of {@link ModelInfo}.
 */
class ModelInfoTest {

	@Test
	void readsAllKeysAndSkipsComments() throws IOException {
		ModelInfo info = ModelInfo.read(text("# comment = ignored\nName = n\nTrainer = t\nDescription = d\n"
				+ "Version = 1\nno separator\n=empty key\nPath = m.yaml\nHash = abc\n"));
		assertEquals(new ModelInfo("n", "t", "d", "1", "m.yaml", "abc"), info);
	}

	@Test
	void optionalKeysMayBeMissing() throws IOException {
		ModelInfo info = ModelInfo.read(text("Path = m.yaml\nHash = abc\n"));
		assertNull(info.name());
		assertEquals("m.yaml", info.path());
	}

	@Test
	void requiresPathAndHash() {
		assertInvalid("Hash = abc\n");
		assertInvalid("Path = m.yaml\nHash =\n");
	}

	/**
	 * Asserts that reading fails with {@link ErrorCode#InvalidConfiguration}.
	 *
	 * @param content model information text
	 */
	private static void assertInvalid(String content) {
		InputStream in = text(content);
		Nfiq2Exception e = assertThrows(Nfiq2Exception.class, () -> ModelInfo.read(in));
		assertEquals(ErrorCode.InvalidConfiguration, e.getNfiq2ErrorCode());
	}

	/**
	 * Wraps text in a stream.
	 *
	 * @param content text
	 * @return UTF-8 stream
	 */
	private static InputStream text(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}
}
