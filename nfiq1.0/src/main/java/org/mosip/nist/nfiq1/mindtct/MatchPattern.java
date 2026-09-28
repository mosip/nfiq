package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IMatchPattern;

/**
 * Pixel-pair pattern matching used by MINDTCT minutia detection.
 * <p>
 * Port of NIST LFS {@code matchpat.c}. While scanning adjacent rows (or
 * columns) of the binarized image, MINDTCT compares consecutive pixel pairs
 * against the first, second and third pairs of each entry in
 * {@link Globals#getFeaturePatterns()} to detect candidate ridge endings and
 * bifurcations. Also provides helpers that skip runs of repeated pixel pairs.
 * <p>
 * Implemented as a lazily created singleton; {@link #getInstance()} is
 * synchronized and the class keeps no mutable state.
 */
public class MatchPattern extends MindTct implements IMatchPattern {
	/** Lazily created singleton instance, see {@link #getInstance()}. */
	private static MatchPattern instance;

	/**
	 * Private constructor; use {@link #getInstance()} to obtain the singleton.
	 */
	private MatchPattern() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code MatchPattern} instance
	 */
	public static synchronized MatchPattern getInstance() {
		if (instance == null) {
			synchronized (MatchPattern.class) {
				if (instance == null) {
					instance = new MatchPattern();
				}
			}
		}
		return instance;
	}

	/**
	 * Returns the shared {@link Globals} tables (feature patterns).
	 *
	 * @return the {@code Globals} singleton
	 */
	public Globals getGlobals() {
		return Globals.getInstance();
	}

	/**
	 * Determines which of the feature patterns have their first pixel pair match
	 * the specified pixel pair.
	 * <p>
	 * NIST: {@code match_1st_pair()}. Tests all {@link ILfs#NFEATURES} entries of
	 * {@code feature_patterns[]}.
	 *
	 * @param firstPixelValue  first pixel value of pair
	 * @param secondPixelValue second pixel value of pair
	 * @param oPossible        output: list of matching feature pattern indices
	 *                         (must hold at least {@link ILfs#NFEATURES} entries)
	 * @param oPossibleMatch   output: reset to 0, then set to the number of
	 *                         matches
	 * @return the number of matches (same as {@code oPossibleMatch})
	 */
	public int matchFirstPair(int firstPixelValue, int secondPixelValue, AtomicIntegerArray oPossible,
			AtomicInteger oPossibleMatch) {
		/* Set possibilities to 0 */
		oPossibleMatch.set(0);

		/* Foreach set of feature pairs ... */
		for (int i = 0; i < ILfs.NFEATURES; i++) {
			/* If current scan pair matches first pair for feature ... */
			if ((firstPixelValue == getGlobals().getFeaturePatterns()[i].getFirst()[0])
					&& (secondPixelValue == getGlobals().getFeaturePatterns()[i].getFirst()[1])) {
				/* Store feature as a oPossible match. */
				oPossible.set(oPossibleMatch.get(), i);
				/* Bump number of stored possibilities. */
				oPossibleMatch.set(oPossibleMatch.get() + 1);
			}
		}

		/* Return number of stored possibilities. */
		return (oPossibleMatch.get());
	}

	/**
	 * Determines which of the passed feature patterns have their second pixel
	 * pair match the specified pixel pair.
	 * <p>
	 * NIST: {@code match_2nd_pair()}. If both pixel values are equal the pair
	 * cannot be a second feature pair, and zero matches are returned.
	 *
	 * @param firstPixelValue  first pixel value of pair
	 * @param secondPixelValue second pixel value of pair
	 * @param oPossible        input/output: on input the list of
	 *                         potentially-matching feature pattern indices; on
	 *                         output compacted to the list of matching indices
	 * @param oPossibleMatch   input/output: on input the number of potential
	 *                         matches; on output the number of matches
	 * @return the number of matches (same as {@code oPossibleMatch})
	 */
	public int matchSecondPair(int firstPixelValue, int secondPixelValue, AtomicIntegerArray oPossible,
			AtomicInteger oPossibleMatch) {
		int currentPossibleMatches;

		/* Store input possibilities. */
		currentPossibleMatches = oPossibleMatch.get();
		/* Reset output possibilities to 0. */
		oPossibleMatch.set(0);

		/* If current scan pair values are the same ... */
		if (firstPixelValue == secondPixelValue) {
			/* Simply return because pair can't be a second feature pair. */
			return (oPossibleMatch.get());
		}

		/* Foreach oPossible match based on first pair ... */
		for (int i = 0; i < currentPossibleMatches; i++) {
			/* If current scan pair matches second pair for feature ... */
			if ((firstPixelValue == getGlobals().getFeaturePatterns()[oPossible.get(i)].getSecond()[0])
					&& (secondPixelValue == getGlobals().getFeaturePatterns()[oPossible.get(i)].getSecond()[1])) {
				/* Store feature as a oPossible match. */
				oPossible.set(oPossibleMatch.get(), oPossible.get(i));
				/* Bump number of stored possibilities. */
				oPossibleMatch.set(oPossibleMatch.get() + 1);
			}
		}

		/* Return number of stored possibilities. */
		return (oPossibleMatch.get());
	}

	/**
	 * Determines which of the passed feature patterns have their third pixel pair
	 * match the specified pixel pair.
	 * <p>
	 * NIST: {@code match_3rd_pair()}.
	 *
	 * @param firstPixelValue  first pixel value of pair
	 * @param secondPixelValue second pixel value of pair
	 * @param oPossible        input/output: on input the list of
	 *                         potentially-matching feature pattern indices; on
	 *                         output compacted to the list of matching indices
	 * @param oPossibleMatch   input/output: on input the number of potential
	 *                         matches; on output the number of matches
	 * @return the number of matches (same as {@code oPossibleMatch})
	 */
	public int matchThirdPair(int firstPixelValue, int secondPixelValue, AtomicIntegerArray oPossible,
			AtomicInteger oPossibleMatch) {
		int currentPossibleMatches;

		/* Store input possibilities. */
		currentPossibleMatches = oPossibleMatch.get();
		/* Reset output possibilities to 0. */
		oPossibleMatch.set(0);

		/* Foreach oPossible match based on first and second pairs ... */
		for (int i = 0; i < currentPossibleMatches; i++) {
			/* If current scan pair matches third pair for feature ... */
			if ((firstPixelValue == getGlobals().getFeaturePatterns()[oPossible.get(i)].getThird()[0])
					&& (secondPixelValue == getGlobals().getFeaturePatterns()[oPossible.get(i)].getThird()[1])) {
				/* Store feature as a oPossible match. */
				oPossible.set(oPossibleMatch.get(), oPossible.get(i));
				/* Bump number of stored possibilities. */
				oPossibleMatch.set(oPossibleMatch.get() + 1);
			}
		}

		/* Return number of stored possibilities. */
		return (oPossibleMatch.get());
	}

	/**
	 * Takes the location of two pixels in adjacent pixel rows within an image
	 * region and skips rightward until either the pixel pair no longer repeats
	 * itself or the image region is exhausted.
	 * <p>
	 * NIST: {@code skip_repeated_horizontal_pair()}. Always advances at least one
	 * pixel.
	 *
	 * @param currentXPixelIndex      input/output: current x-coord of starting
	 *                                pixel pair; on return the x-coord where the
	 *                                rightward skip terminated
	 * @param currentRightXPixelIndex right edge (exclusive) of the image region
	 * @param binarizedImageData      binarized image pixel data (row-major)
	 * @param currentTopPixel         input/output: index of current top pixel in
	 *                                pair; on return the top pixel where the skip
	 *                                terminated
	 * @param currentBottomPixel      input/output: index of current bottom pixel
	 *                                in pair; on return the bottom pixel where the
	 *                                skip terminated
	 * @param imageWidth              width (in pixels) of image (unused)
	 * @param imageHeight             height (in pixels) of image (unused)
	 */
	public void skipRepeatedHorizontalPair(AtomicInteger currentXPixelIndex, final int currentRightXPixelIndex,
			int[] binarizedImageData, AtomicInteger currentTopPixel, AtomicInteger currentBottomPixel,
			final int imageWidth, final int imageHeight) {
		int oldTopPixel;
		int oldBottomPixel;

		/* Store starting pixel pair. */
		oldTopPixel = binarizedImageData[currentTopPixel.get()];
		oldBottomPixel = binarizedImageData[currentBottomPixel.get()];

		/* Bump horizontally to next pixel pair. */
		currentXPixelIndex.set(currentXPixelIndex.get() + 1);
		currentTopPixel.set(currentTopPixel.get() + 1);
		currentBottomPixel.set(currentBottomPixel.get() + 1);

		/* While not at right of scan region... */
		while (currentXPixelIndex.get() < currentRightXPixelIndex) {
			/* If one or the other pixels in the new pair are different */
			/* from the starting pixel pair... */
			if ((binarizedImageData[currentTopPixel.get()] != oldTopPixel)
					|| (binarizedImageData[currentBottomPixel.get()] != oldBottomPixel)) {
				/* Done skipping repreated pixel pairs. */
				return;
			}
			/* Otherwise, bump horizontally to next pixel pair. */
			currentXPixelIndex.set(currentXPixelIndex.get() + 1);
			currentTopPixel.set(currentTopPixel.get() + 1);
			currentBottomPixel.set(currentBottomPixel.get() + 1);
		}
	}

	/**
	 * Takes the location of two pixels in adjacent pixel columns within an image
	 * region and skips downward until either the pixel pair no longer repeats
	 * itself or the image region is exhausted.
	 * <p>
	 * NIST: {@code skip_repeated_vertical_pair()}. Always advances at least one
	 * row.
	 *
	 * @param currentYPixelIndex       input/output: current y-coord of starting
	 *                                 pixel pair; on return the y-coord where the
	 *                                 downward skip terminated
	 * @param currentBottomYPixelIndex bottom (exclusive) of the image region
	 * @param binarizedImageData       binarized image pixel data (row-major)
	 * @param currentLeftPixel         input/output: index of current left pixel in
	 *                                 pair; on return the left pixel where the
	 *                                 skip terminated
	 * @param currentRightPixel        input/output: index of current right pixel
	 *                                 in pair; on return the right pixel where the
	 *                                 skip terminated
	 * @param imageWidth               width (in pixels) of image (row stride)
	 * @param imageHeight              height (in pixels) of image (unused)
	 */
	public void skipRepeatedVerticalPair(AtomicInteger currentYPixelIndex, final int currentBottomYPixelIndex,
			int[] binarizedImageData, AtomicInteger currentLeftPixel, AtomicInteger currentRightPixel,
			final int imageWidth, final int imageHeight) {
		int oldLeftPixelIndex;
		int oldRightPixelIndex;

		/* Store starting pixel pair. */
		oldLeftPixelIndex = binarizedImageData[currentLeftPixel.get()];
		oldRightPixelIndex = binarizedImageData[currentRightPixel.get()];

		/* Bump vertically to next pixel pair. */
		currentYPixelIndex.set(currentYPixelIndex.get() + 1);
		currentLeftPixel.set(currentLeftPixel.get() + imageWidth);
		currentRightPixel.set(currentRightPixel.get() + imageWidth);

		/* While not at bottom of scan region... */
		while (currentYPixelIndex.get() < currentBottomYPixelIndex) {
			/* If one or the other pixels in the new pair are different */
			/* from the starting pixel pair... */
			if ((binarizedImageData[currentLeftPixel.get()] != oldLeftPixelIndex)
					|| (binarizedImageData[currentRightPixel.get()] != oldRightPixelIndex)) {
				/* Done skipping repreated pixel pairs. */
				return;
			}
			/* Otherwise, bump vertically to next pixel pair. */
			currentYPixelIndex.set(currentYPixelIndex.get() + 1);
			currentLeftPixel.set(currentLeftPixel.get() + imageWidth);
			currentRightPixel.set(currentRightPixel.get() + imageWidth);
		}
	}
}