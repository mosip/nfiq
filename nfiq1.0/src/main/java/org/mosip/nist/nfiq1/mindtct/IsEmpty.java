package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicIntegerArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IIsEmpty;

/**
 * Empty-image detection used by NFIQ 1.0 before feature extraction results are
 * scored.
 * <p>
 * Port of NIST NFIQ's {@code isempty.c}. It inspects the MINDTCT quality map
 * and reports whether the image appears to contain no usable fingerprint (in
 * which case NFIQ assigns the worst quality score without running the MLP).
 * <p>
 * Implemented as a lazily created singleton; {@link #getInstance()} is
 * synchronized and the class keeps no mutable state, so it is safe to share
 * across threads.
 */
public class IsEmpty extends MindTct implements IIsEmpty {
	/** Lazily created singleton instance, see {@link #getInstance()}. */
	private static IsEmpty instance;

	/**
	 * Private constructor; use {@link #getInstance()} to obtain the singleton.
	 */
	private IsEmpty() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code IsEmpty} instance
	 */
	public static synchronized IsEmpty getInstance() {
		if (instance == null) {
			instance = new IsEmpty();
		}
		return instance;
	}

	/**
	 * Determines whether the statistics passed indicate an empty image.
	 * <p>
	 * NIST: {@code is_image_empty()}. This routine is designed to be expanded as
	 * more statistical tests are developed; currently it only delegates to
	 * {@link #isQualityMapEmpty(AtomicIntegerArray, int, int)}.
	 *
	 * @param qualityMap quality map computed by NIST's MINDTCT (one value per
	 *                   block, row-major, {@code mapWidth * mapHeight} entries)
	 * @param mapWidth   width of the map in blocks
	 * @param mapHeight  height of the map in blocks
	 * @return {@link ILfs#TRUE} if the image is determined empty,
	 *         {@link ILfs#FALSE} if the image is determined NOT empty
	 */
	public int isImageEmpty(AtomicIntegerArray qualityMap, final int mapWidth, final int mapHeight) {
		/* This routine is designed to be expanded as more statistical */
		/* tests are developed. */

		if (isQualityMapEmpty(qualityMap, mapWidth, mapHeight) == ILfs.TRUE) {
			return (ILfs.TRUE);
		} else {
			return (ILfs.FALSE);
		}
	}

	/**
	 * Determines whether the quality map is entirely set to zero.
	 * <p>
	 * NIST: {@code is_qmap_empty()}. Scans all {@code mapWidth * mapHeight}
	 * entries and stops at the first non-zero value.
	 *
	 * @param qualityMap quality map computed by NIST's MINDTCT (row-major)
	 * @param mapWidth   width of the map in blocks
	 * @param mapHeight  height of the map in blocks
	 * @return {@link ILfs#TRUE} if the quality map is empty (all zeros),
	 *         {@link ILfs#FALSE} if the quality map is NOT empty
	 */
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
