package org.mosip.nist.nfiq2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * Tests of {@link Nfiq2Exception} and {@link ErrorCode}.
 */
class Nfiq2ExceptionTest {

	@Test
	void carriesMosipCodeAndCategory() {
		Nfiq2Exception e = new Nfiq2Exception(ErrorCode.BadArguments, "no image");
		assertTrue(e instanceof BaseUncheckedException);
		assertEquals(ErrorCode.BadArguments, e.getNfiq2ErrorCode());
		assertEquals("MOS-NFIQ2-002", e.getErrorCode());
		assertEquals("no image", e.getErrorText());
		assertTrue(e.getMessage().contains("MOS-NFIQ2-002"));
	}

	@Test
	void keepsCause() {
		IOException cause = new IOException("disk");
		Nfiq2Exception e = new Nfiq2Exception(ErrorCode.CannotReadFromFile, "read failed", cause);
		assertSame(cause, e.getCause());
		assertEquals(ErrorCode.CannotReadFromFile.getErrorCode(), e.getErrorCode());
	}

	@Test
	void errorCodesAreUniqueAndResolvable() {
		Set<String> codes = new HashSet<>();
		for (ErrorCode code : ErrorCode.values()) {
			assertTrue(codes.add(code.getErrorCode()), code.name());
			assertTrue(code.getErrorCode().startsWith("MOS-NFIQ2-"));
			assertTrue(!code.getErrorMessage().isEmpty());
			assertEquals(code, ErrorCode.fromErrorCode(code.getErrorCode().toLowerCase()));
		}
		assertEquals(ErrorCode.UnknownError, ErrorCode.fromErrorCode("MOS-OTHER-001"));
	}
}
