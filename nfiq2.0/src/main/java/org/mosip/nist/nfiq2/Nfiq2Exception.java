package org.mosip.nist.nfiq2;

import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * Failure raised by NFIQ 2 (NIST {@code NFIQ2::Exception}). Unchecked, like its C++ counterpart, and a MOSIP
 * {@link BaseUncheckedException}: {@link #getErrorCode()} returns the MOSIP code of the category (for example
 * {@code MOS-NFIQ2-002}) and {@link #getErrorText()} the description.
 */
public class Nfiq2Exception extends BaseUncheckedException {
	/** Serialization version. */
	private static final long serialVersionUID = 1L;

	/** Error category. */
	private final ErrorCode nfiq2ErrorCode;

	/**
	 * Creates an exception.
	 *
	 * @param errorCode category
	 * @param message   description
	 */
	public Nfiq2Exception(ErrorCode errorCode, String message) {
		super(errorCode.getErrorCode(), message);
		this.nfiq2ErrorCode = errorCode;
	}

	/**
	 * Creates an exception with a cause.
	 *
	 * @param errorCode category
	 * @param message   description
	 * @param cause     underlying failure
	 */
	public Nfiq2Exception(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode.getErrorCode(), message, cause);
		this.nfiq2ErrorCode = errorCode;
	}

	/**
	 * Returns the NIST error category ({@code NFIQ2::Exception::getErrorCode}).
	 *
	 * @return category
	 */
	public ErrorCode getNfiq2ErrorCode() {
		return nfiq2ErrorCode;
	}
}
