package org.mosip.nist.nfiq1.common;

public class ImageIO {	
	public static final int IHDR_SIZE = 288; // len of hdr record (always even bytes)
	public static final int SHORT_CHARS = 8; // # of ASCII chars to represent a short
	public static final int BUFSIZE = 80; // default buffer size
	public static final int DATELEN = 26; // character length of date string

	public static final int UNCOMP = 0;
	public static final int CCITT_G3 = 1;
	public static final int CCITT_G4 = 2;
	public static final int RL = 5;
	public static final int JPEG_SD = 6;
	public static final int WSQ_SD14 = 7;
	public static final char MSBF = '0';
	public static final char LSBF = '1';
	public static final char HILOW = '0';
	public static final char LOWHI = '1';
	public static final char UNSIGNED = '0';
	public static final char SIGNED = '1';
	public static final char ROW_MAJ = '0';
	public static final char COL_MAJ = '1';
	public static final char TOP2BOT = '0';
	public static final char BOT2TOP = '1';
	public static final char LEFT2RIGHT = '0';
	public static final char RIGHT2LEFT = '1';
	public static final double BYTE_SIZE = 8.0;
	
	public static final int NO_INTRLV = 0;
	public static final int MAX_CMPNTS = 4;
	public static final int FREE_IMAGE = 1;
	public static final int NO_FREE_IMAGE = 0;
}