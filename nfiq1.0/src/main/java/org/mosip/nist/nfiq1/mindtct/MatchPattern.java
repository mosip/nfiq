package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IMatchPattern;

public class MatchPattern extends MindTct implements IMatchPattern {
	private static MatchPattern instance;

	private MatchPattern() {
		super();
	}

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

	public Globals getGlobals() {
		return Globals.getInstance();
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: matchFirstPair - Determines which of the feature_patterns[] have their
	 * #cat: first pixel pair match the specified pixel pair. Input: firstPixelValue
	 * - first pixel value of pair secondPixelValue - second pixel value of pair
	 * Output: oPossible - list of matching feature_patterns[] indices
	 * oPossibleMatch - number of matches Return Code: oPossibleMatch - number of
	 * matches
	 *************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: matchSecondPair - Determines which of the passed feature_patterns[]
	 * have #cat: their second pixel pair match the specified pixel pair. Input:
	 * firstPixelValue - first pixel value of pair secondPixelValue - second pixel
	 * value of pair oPossible - list of potentially-matching feature_patterns[]
	 * indices oPossibleMatch - number of potential matches Output: oPossible - list
	 * of matching feature_patterns[] indices oPossibleMatch - number of matches
	 * Return Code: oPossibleMatch - number of matches
	 *************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: matchThirdPair - Determines which of the passed feature_patterns[] have
	 * #cat: their third pixel pair match the specified pixel pair. Input:
	 * firstPixelValue - first pixel value of pair secondPixelValue - second pixel
	 * value of pair oPossible - list of potentially-matching feature_patterns[]
	 * indices oPossibleMatch - number of potential matches Output: oPossible - list
	 * of matching feature_patterns[] indices oPossibleMatch - number of matches
	 * Return Code: oPossibleMatch - number of matches
	 *************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: skipRepeatedHorizontalPair - Takes the location of two pixel in #cat:
	 * adjacent pixel rows within an image region and skips #cat: rightward until
	 * the either the pixel pair no longer repeats #cat: itself or the image region
	 * is exhausted. Input: currentXPixelIndex - current x-coord of starting pixel
	 * pair currentRightXPixelIndex - right edge of the image region
	 * binarizedImageData - array of image currentTopPixel - pointer to current top
	 * pixel in pair currentBottomPixel - pointer to current bottom pixel in pair
	 * imageWidth - width (in pixels) of image imageHeight - height (in pixels) of
	 * image Output: currentXPixelIndex - x-coord of where rightward skip terminated
	 * currentTopPixel - points to top pixel where rightward skip terminated
	 * currentBottomPixel - points to bottom pixel where rightward skip terminated
	 *************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: skipRepeatedVerticalPair - Takes the location of two pixel in #cat:
	 * adjacent pixel columns within an image region and skips #cat: downward until
	 * the either the pixel pair no longer repeats #cat: itself or the image region
	 * is exhausted. Input: currentYPixelIndex - current y-coord of starting pixel
	 * pair currentBottomYPixelIndex - bottom of the image region binarizedImageData
	 * - array of image currentLeftPixel - pointer to current left pixel in pair
	 * currentRightPixel - pointer to current right pixel in pair imageWidth - width
	 * (in pixels) of image imageHeight - height (in pixels) of image Output:
	 * currentYPixelIndex - y-coord of where downward skip terminated
	 * currentLeftPixel - points to left pixel where downward skip terminated
	 * currentRightPixel - points to right pixel where donward skip terminated
	 *************************************************************************/
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