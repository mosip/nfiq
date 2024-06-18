package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicIntegerArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IIsEmpty;

public class IsEmpty extends MindTct implements IIsEmpty {
	private static IsEmpty instance;

	private IsEmpty() {
		super();
	}

	public static synchronized IsEmpty getInstance() {
		if (instance == null) {
			instance = new IsEmpty();
		}
		return instance;
	}

	/***********************************************************************
	 ************************************************************************
	 * #cat: isImageEmpty - Routine determines if statistics passed indicate #cat:
	 * an empty image. Input: qualityMap - quality map computed by NIST's Mindtct
	 * mapWidth - width of map mapHeight - height of map Return Code: True - image
	 * determined empty False - image determined NOT empty
	 ************************************************************************/
	public int isImageEmpty(AtomicIntegerArray qualityMap, final int mapWidth, final int mapHeight) {
		/* This routine is designed to be expanded as more statistical */
		/* tests are developed. */

		if (isQualityMapEmpty(qualityMap, mapWidth, mapHeight) == ILfs.TRUE) {
			return (ILfs.TRUE);
		} else {
			return (ILfs.FALSE);
		}
	}

	/***********************************************************************
	 ************************************************************************
	 * #cat: isQualityMapEmpty - Routine determines if quality map is all set to
	 * zero Input: qualityMap - quality map computed by NIST's Mindtct mapWidth -
	 * width of map mapHeight - height of map Return Code: True - quality map is
	 * empty False - quality map is NOT empty
	 ************************************************************************/
	public int isQualityMapEmpty(AtomicIntegerArray qualityMap, final int mapWidth, final int mapHeight) {
		int i;
		int mapLen;
		int qptrIndex;
		qptrIndex = 0;
		mapLen = mapWidth * mapHeight;
		for (i = 0; i < mapLen; i++) {
			if (qualityMap.get(qptrIndex++) != ILfs.FALSE) {
				return (ILfs.FALSE);
			}
		}
		return (ILfs.TRUE);
	}
}