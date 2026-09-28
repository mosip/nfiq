package org.mosip.nist.nfiq1;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Root base class of the NFIQ 1.0 Java port.
 * <p>
 * Holds process-wide state shared by every NIST-derived component (currently only the verbose-logging switch)
 * and small pixel-access helpers used by the MINDTCT and NFIQ code paths. Most algorithm classes in
 * {@code org.mosip.nist.nfiq1} ultimately extend this class.
 * <p>
 * Thread-safety: the logging flag is a mutable {@code static} field without synchronization, so it is shared
 * by all threads and instances; concurrent callers that toggle it may observe each other's setting.
 */
public class Nist extends Object {
	/** Global verbose-logging switch; {@code true} routes diagnostic output through SLF4J, {@code false} to stdout. */
	private static boolean bShowLogs = true;
	
	/**
	 * Returns whether verbose (logger-based) diagnostic output is enabled.
	 *
	 * @return {@code true} if detailed logging is enabled, {@code false} otherwise
	 */
	public static boolean isShowLogs() {
		return Nist.bShowLogs;
	}

	/**
	 * Enables or disables verbose (logger-based) diagnostic output for all NFIQ components.
	 * <p>
	 * Side effect: modifies a {@code static} flag that is shared JVM-wide.
	 *
	 * @param bShowLogs {@code true} to enable detailed logging, {@code false} to disable it
	 */
	public static void setShowLogs(boolean bShowLogs) {
		Nist.bShowLogs = bShowLogs;
	}

	/**
	 * Returns the pixel value at column {@code bx}, row {@code by} of a row-major image stored in an
	 * {@link AtomicIntegerArray}.
	 *
	 * @param data row-major image pixel data
	 * @param bx   x coordinate (column) of the pixel
	 * @param by   y coordinate (row) of the pixel
	 * @param iw   image width in pixels (row stride)
	 * @param ih   image height in pixels (not used for the index computation; kept for API symmetry)
	 * @return the pixel value at {@code (bx, by)}
	 */
	public static int getPixelValueFromAtomicArray(AtomicIntegerArray data, int bx, int by, int iw, int ih)
	{
		return data.get(0 + (by * iw) + bx);
	}

	/**
	 * Returns the pixel value at column {@code bx}, row {@code by} of a row-major image stored in a byte array.
	 * <p>
	 * Note: the raw (signed) {@code byte} value is returned, so intensities above 127 come back negative unless
	 * the caller masks with {@code 0xFF}.
	 *
	 * @param data row-major image pixel data
	 * @param bx   x coordinate (column) of the pixel
	 * @param by   y coordinate (row) of the pixel
	 * @param iw   image width in pixels (row stride)
	 * @param ih   image height in pixels (not used for the index computation; kept for API symmetry)
	 * @return the signed byte pixel value at {@code (bx, by)}, widened to {@code int}
	 */
	public static int getPixelValueFromByteArray(byte[] data, int bx, int by, int iw, int ih)
	{
		return data [0 + (by * iw) + bx];
	}
}
