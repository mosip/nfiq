package org.mosip.nist.nfiq2;

/**
 * Broad error categories, identical to NIST {@code NFIQ2::ErrorCode} (same names, same order), each with the
 * MOSIP error code and message carried by {@link Nfiq2Exception}.
 */
public enum ErrorCode {
	/** Unexpected failure. */
	UnknownError("MOS-NFIQ2-000", "Unknown error"),
	/** Memory allocation failed. */
	NotEnoughMemory("MOS-NFIQ2-001", "Not enough memory"),
	/** Invalid argument. */
	BadArguments("MOS-NFIQ2-002", "Bad arguments"),
	/** A quality module could not compute its measures. */
	QualityMeasureCalculationError("MOS-NFIQ2-003", "Quality measure calculation error"),
	/** Output could not be written. */
	CannotWriteToFile("MOS-NFIQ2-004", "Cannot write to file"),
	/** Input could not be read. */
	CannotReadFromFile("MOS-NFIQ2-005", "Cannot read from file"),
	/** Requested data has not been computed. */
	NoDataAvailable("MOS-NFIQ2-006", "No data available"),
	/** Base64 decoding failed. */
	CannotDecodeBase64("MOS-NFIQ2-007", "Cannot decode base64"),
	/** Model file or hash is invalid. */
	InvalidConfiguration("MOS-NFIQ2-008", "Invalid configuration"),
	/** Random forest prediction failed or the model is not loaded. */
	MachineLearningError("MOS-NFIQ2-009", "Machine learning error"),
	/** FingerJetFX context could not be created. */
	FJFX_CannotCreateContext("MOS-NFIQ2-010", "FingerJetFX cannot create context"),
	/** FingerJetFX could not extract a feature set. */
	FJFX_CannotCreateFeatureSet("MOS-NFIQ2-011", "FingerJetFX cannot create feature set"),
	/** FingerJetFX returned no feature set. */
	FJFX_NoFeatureSetCreated("MOS-NFIQ2-012", "FingerJetFX created no feature set"),
	/** Unified quality score out of range. */
	InvalidUnifiedQualityScore("MOS-NFIQ2-013", "Invalid unified quality score"),
	/** Image is blank or too large after trimming the white frame. */
	InvalidImageSize("MOS-NFIQ2-014", "Invalid image size");

	/** MOSIP error code. */
	private final String errorCode;
	/** Default error message. */
	private final String errorMessage;

	/**
	 * Creates a category.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage default error message
	 */
	ErrorCode(String errorCode, String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	/**
	 * Returns the MOSIP error code.
	 *
	 * @return code, for example {@code MOS-NFIQ2-002}
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Returns the default error message.
	 *
	 * @return message
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Returns the category of a MOSIP error code.
	 *
	 * @param errorCode MOSIP error code
	 * @return matching category, {@link #UnknownError} when none matches
	 */
	public static ErrorCode fromErrorCode(String errorCode) {
		for (ErrorCode code : values()) {
			if (code.errorCode.equalsIgnoreCase(errorCode)) {
				return code;
			}
		}
		return UnknownError;
	}
}
