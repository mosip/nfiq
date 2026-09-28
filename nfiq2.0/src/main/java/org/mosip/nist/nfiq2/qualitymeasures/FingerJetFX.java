package org.mosip.nist.nfiq2.qualitymeasures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mosip.nist.nfiq2.ErrorCode;
import org.mosip.nist.nfiq2.FingerprintImageData;
import org.mosip.nist.nfiq2.Nfiq2Exception;
import org.mosip.nist.nfiq2.frfxll.FeatureExtractor;
import org.mosip.nist.nfiq2.frfxll.FrfxllException;
import org.mosip.nist.nfiq2.frfxll.FrfxllMinutia;

/**
 * Minutiae count (NIST {@code FingerJetFX.cpp}): number of minutiae found by FingerJetFX OSE, and the number
 * inside a {@code 200 x 200} square centred on their centre of mass.
 */
public final class FingerJetFX extends QualityModule {
	/** Algorithm identifier. */
	public static final String NAME = "MinutiaeCount";
	/** Number of minutiae. */
	public static final String COUNT = "FingerJetFX_MinutiaeCount";
	/** Number of minutiae in the {@code 200 x 200} square around their centre of mass. */
	public static final String COUNT_COM = "FingerJetFX_MinCount_COMMinRect200x200";

	/** Minimum image width accepted by FingerJetFX. */
	static final int MIN_WIDTH = 196;
	/** Minimum image height accepted by FingerJetFX. */
	static final int MIN_HEIGHT = 196;
	/** Side of the centre-of-mass square. */
	private static final int COM_RECT_SIZE = 200;
	/** White. */
	private static final byte WHITE = (byte) 255;

	/** Minutiae inside the original image area. */
	private final List<FrfxllMinutia> minutiaData;

	/**
	 * Extracts the minutiae and computes the measures.
	 *
	 * @param image cropped image
	 * @throws Nfiq2Exception {@link ErrorCode#FJFX_CannotCreateFeatureSet} when FingerJetFX fails
	 */
	public FingerJetFX(FingerprintImageData image) {
		long start = System.nanoTime();
		int width = image.getWidth();
		int height = image.getHeight();
		boolean tooSmall = width < MIN_WIDTH || height < MIN_HEIGHT;
		byte[] data = image.getData();
		int fxWidth = width;
		int fxHeight = height;
		if (tooSmall) {
			fxWidth = Math.max(width, MIN_WIDTH);
			fxHeight = Math.max(height, MIN_HEIGHT);
			byte[] bigger = new byte[fxWidth * fxHeight];
			Arrays.fill(bigger, WHITE);
			for (int y = 0; y < height; y++) {
				System.arraycopy(data, y * width, bigger, y * fxWidth, width);
			}
			data = bigger;
		}

		List<FrfxllMinutia> all;
		try {
			all = FeatureExtractor.createFeatureSetFromRaw(data, fxWidth, fxHeight, image.getPpi(),
					FeatureExtractor.FEX_ENABLE_ENHANCEMENT);
		} catch (FrfxllException e) {
			throw new Nfiq2Exception(ErrorCode.FJFX_CannotCreateFeatureSet,
					"Could not create feature set from raw data: " + e.getMessage(), e);
		}

		List<FrfxllMinutia> kept = new ArrayList<>(all.size());
		for (FrfxllMinutia m : all) {
			if (!tooSmall || (m.x() < width && m.y() < height)) {
				kept.add(m);
			}
		}
		minutiaData = Collections.unmodifiableList(kept);

		if (all.isEmpty()) {
			put(COUNT_COM, 0.0);
			put(COUNT, 0.0);
		} else {
			put(COUNT_COM, (double) countInComRect(minutiaData, width, height));
			put(COUNT, (double) all.size());
		}
		setSpeedSince(start);
	}

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * Returns the minutiae found inside the image.
	 *
	 * @return minutiae
	 */
	public List<FrfxllMinutia> getMinutiaData() {
		return minutiaData;
	}

	/**
	 * Returns the identifiers of the measures, in NIST order.
	 *
	 * @return identifiers
	 */
	public static List<String> getNativeQualityMeasureIDs() {
		return List.of(COUNT_COM, COUNT);
	}

	/**
	 * {@code computeROI}: minutiae inside the {@code 200 x 200} square (inclusive, clipped to the image)
	 * centred on the integer centre of mass.
	 *
	 * @param minutiae minutiae
	 * @param width    image width
	 * @param height   image height
	 * @return count
	 * @throws Nfiq2Exception when there are no minutiae to average
	 */
	static int countInComRect(List<FrfxllMinutia> minutiae, int width, int height) {
		if (minutiae.isEmpty()) {
			throw new Nfiq2Exception(ErrorCode.QualityMeasureCalculationError,
					"Cannot compute the centre of minutiae mass: all minutiae lie in the padding");
		}
		long lx = 0;
		long ly = 0;
		for (FrfxllMinutia m : minutiae) {
			lx += m.x();
			ly += m.y();
		}
		int cx = (int) (lx / minutiae.size());
		int cy = (int) (ly / minutiae.size());
		int half = COM_RECT_SIZE / 2;
		int startX = Math.max(cx - half, 0);
		int startY = Math.max(cy - half, 0);
		int endX = Math.min(cx + half, width - 1);
		int endY = Math.min(cy + half, height - 1);
		int count = 0;
		for (FrfxllMinutia m : minutiae) {
			if (m.x() >= startX && m.x() <= endX && m.y() >= startY && m.y() <= endY) {
				count++;
			}
		}
		return count;
	}
}
