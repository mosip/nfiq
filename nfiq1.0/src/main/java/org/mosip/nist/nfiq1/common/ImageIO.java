package org.mosip.nist.nfiq1.common;

/**
 * Constants for NIST IHead image headers and generic image I/O (port of NIST's {@code ihead.h} /
 * {@code imgio.h} / {@code imgdecod.h} definitions).
 * <p>
 * Describes compression codes, byte/bit ordering and scan-direction flags stored in an IHead header, plus
 * interleave/component limits used when decoding images. Holds constants only.
 */
public class ImageIO {	
	/** Length in bytes of an IHead header record (always an even number of bytes). */
	public static final int IHDR_SIZE = 288; // len of hdr record (always even bytes)
	/** Number of ASCII characters used to represent a {@code short} in an IHead header. */
	public static final int SHORT_CHARS = 8; // # of ASCII chars to represent a short
	/** Default text buffer size in characters for IHead header fields. */
	public static final int BUFSIZE = 80; // default buffer size
	/** Character length of the date string in an IHead header. */
	public static final int DATELEN = 26; // character length of date string

	/** IHead compression code: uncompressed. */
	public static final int UNCOMP = 0;
	/** IHead compression code: CCITT Group 3 fax compression. */
	public static final int CCITT_G3 = 1;
	/** IHead compression code: CCITT Group 4 fax compression. */
	public static final int CCITT_G4 = 2;
	/** IHead compression code: run-length encoding. */
	public static final int RL = 5;
	/** IHead compression code: lossless JPEG (NIST {@code JPEG_SD}). */
	public static final int JPEG_SD = 6;
	/** IHead compression code: WSQ (NIST {@code WSQ_SD14}). */
	public static final int WSQ_SD14 = 7;
	/** Byte-order flag: most significant byte first. */
	public static final char MSBF = '0';
	/** Byte-order flag: least significant byte first. */
	public static final char LSBF = '1';
	/** Bit-order flag: high-to-low. */
	public static final char HILOW = '0';
	/** Bit-order flag: low-to-high. */
	public static final char LOWHI = '1';
	/** Pixel sign flag: unsigned pixel values. */
	public static final char UNSIGNED = '0';
	/** Pixel sign flag: signed pixel values. */
	public static final char SIGNED = '1';
	/** Pixel storage flag: row-major order. */
	public static final char ROW_MAJ = '0';
	/** Pixel storage flag: column-major order. */
	public static final char COL_MAJ = '1';
	/** Vertical scan direction flag: top to bottom. */
	public static final char TOP2BOT = '0';
	/** Vertical scan direction flag: bottom to top. */
	public static final char BOT2TOP = '1';
	/** Horizontal scan direction flag: left to right. */
	public static final char LEFT2RIGHT = '0';
	/** Horizontal scan direction flag: right to left. */
	public static final char RIGHT2LEFT = '1';
	/** Number of bits in a byte, as a {@code double} for size computations. */
	public static final double BYTE_SIZE = 8.0;
	
	/** Interleave flag: component planes are not interleaved. */
	public static final int NO_INTRLV = 0;
	/** Maximum number of color components supported in a decoded image. */
	public static final int MAX_CMPNTS = 4;
	/** Flag requesting that the image buffer be freed after use. */
	public static final int FREE_IMAGE = 1;
	/** Flag requesting that the image buffer not be freed after use. */
	public static final int NO_FREE_IMAGE = 0;
}