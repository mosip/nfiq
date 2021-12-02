package io.mosip.nist.nfiq1;

import java.util.concurrent.atomic.AtomicIntegerArray;

public class Nist extends Object {
	private static boolean bShowLogs = true;
	
	public static boolean isShowLogs() {
		return Nist.bShowLogs;
	}

	public static void setShowLogs(boolean bShowLogs) {
		Nist.bShowLogs = bShowLogs;
	}

	public static int getPixelValueFromAtomicArray(AtomicIntegerArray data, int bx, int by, int iw, int ih)
	{
		return data.get(0 + (by * iw) + bx);
	}

	public static int getPixelValueFromByteArray(byte[] data, int bx, int by, int iw, int ih)
	{
		return data [0 + (by * iw) + bx];
	}
}
