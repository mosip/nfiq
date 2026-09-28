package org.mosip.nist.nfiq1.mindtct;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IRidges;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.Minutia;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Neighbour finding and ridge counting between minutiae for MINDTCT.
 * <p>
 * Port of NIST LFS {@code ridges.c}. For each detected minutia, locates up to
 * {@link LfsParams#getMaxNbrs()} closest neighbours (searching the
 * column-sorted minutia list), sorts them clockwise starting from vertical, and
 * counts the number of intervening ridges along the straight line to each
 * neighbour, validating each candidate ridge crossing by contour tracing.
 * <p>
 * Implemented as a lazily created singleton; {@link #getInstance()} is
 * synchronized and the class keeps no mutable state of its own (results are
 * written into the passed {@link Minutiae}).
 */
public class Ridges extends MindTct implements IRidges {
	/** SLF4J logger for diagnostics (when logs are enabled) and errors. */
	private static final Logger logger = LoggerFactory.getLogger(Ridges.class);
	/** Lazily created singleton instance, see {@link #getInstance()}. */
	private static Ridges instance;

	/**
	 * Private constructor; use {@link #getInstance()} to obtain the singleton.
	 */
	private Ridges() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code Ridges} instance
	 */
	public static synchronized Ridges getInstance() {
		if (instance == null) {
			synchronized (Ridges.class) {
				if (instance == null) {
					instance = new Ridges();
				}
			}
		}
		return instance;
	}

	/**
	 * Returns the shared {@link Defs} helper (modulo and rounding utilities).
	 *
	 * @return the {@code Defs} singleton
	 */
	public Defs getDefs() {
		return Defs.getInstance();
	}

	/**
	 * Returns the shared {@link ImageUtil} helper.
	 *
	 * @return the {@code ImageUtil} singleton
	 */
	public ImageUtil getImageUtil() {
		return ImageUtil.getInstance();
	}

	/**
	 * Returns the shared {@link Globals} tables.
	 *
	 * @return the {@code Globals} singleton
	 */
	public Globals getGlobals() {
		return Globals.getInstance();
	}

	/**
	 * Returns the shared {@link LfsUtil} helper (distances, angles, sorted
	 * insertion).
	 *
	 * @return the {@code LfsUtil} singleton
	 */
	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	/**
	 * Returns the shared {@link Free} helper used to release buffers.
	 *
	 * @return the {@code Free} singleton
	 */
	public Free getFree() {
		return Free.getInstance();
	}

	/**
	 * Returns the shared {@link Init} helper.
	 *
	 * @return the {@code Init} singleton
	 */
	public Init getInit() {
		return Init.getInstance();
	}

	/**
	 * Returns the shared {@link Binarization} helper.
	 *
	 * @return the {@code Binarization} singleton
	 */
	public Binarization getBinarization() {
		return Binarization.getInstance();
	}

	/**
	 * Returns the shared {@link MinutiaHelper} (minutia sorting and duplicate
	 * removal).
	 *
	 * @return the {@code MinutiaHelper} singleton
	 */
	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}

	/**
	 * Returns the shared {@link Sort} helper.
	 *
	 * @return the {@code Sort} singleton
	 */
	public Sort getSort() {
		return Sort.getInstance();
	}

	/**
	 * Returns the shared {@link Detect} helper.
	 *
	 * @return the {@code Detect} singleton
	 */
	public Detect getDetect() {
		return Detect.getInstance();
	}

	/**
	 * Returns the shared {@link RemoveMinutia} helper.
	 *
	 * @return the {@code RemoveMinutia} singleton
	 */
	public RemoveMinutia getRemoveMinutia() {
		return RemoveMinutia.getInstance();
	}

	/**
	 * Returns the shared {@link Line} helper (line rasterization).
	 *
	 * @return the {@code Line} singleton
	 */
	public Line getLine() {
		return Line.getInstance();
	}

	/**
	 * Returns the shared {@link Contour} helper (contour tracing).
	 *
	 * @return the {@code Contour} singleton
	 */
	public Contour getContour() {
		return Contour.getInstance();
	}

	/**
	 * Returns the shared {@link Maps} helper.
	 *
	 * @return the {@code Maps} singleton
	 */
	public Maps getMap() {
		return Maps.getInstance();
	}

	/**
	 * Returns the shared {@link Loop} helper.
	 *
	 * @return the {@code Loop} singleton
	 */
	public Loop getLoop() {
		return Loop.getInstance();
	}

	/**
	 * Takes a list of minutiae and, for each one, determines its closest
	 * neighbours and counts the number of intervening ridges between the minutia
	 * point and each of its neighbours.
	 * <p>
	 * NIST: {@code count_minutiae_ridges()}. The minutiae are first sorted on x
	 * then y (column-oriented) and duplicates are removed, so the list referenced
	 * by {@code oMinutiae} is reordered and may shrink.
	 *
	 * @param oMinutiae          input/output: list of minutiae; on return sorted,
	 *                           de-duplicated and augmented with neighbours and
	 *                           ridge counts
	 * @param binarizedImageData binary image data (0 = white, 1 = black)
	 * @param imageWidth         width (in pixels) of image
	 * @param imageHeight        height (in pixels) of image
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return zero ({@link ILfs#FALSE}) on successful completion; negative on
	 *         system error
	 */
	public int countMinutiaeRidges(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
			final int imageHeight, final LfsParams lfsParams) {
		int ret;
		int minutiaIndex;

		if (isShowLogs())
			logger.debug("\nFINDING NBRS AND COUNTING RIDGES:\n");

		/* Sort minutia points on x then y (column-oriented). */
		if ((ret = getMinutiaHelper().sortMinutiaeLeftToRightAndThenTopToBottom(oMinutiae, imageWidth,
				imageHeight)) != ILfs.FALSE) {
			return (ret);
		}

		/* Remove any duplicate minutia points from the list. */
		if ((ret = getMinutiaHelper().removeRedundantMinutiae(oMinutiae)) != ILfs.FALSE) {
			return (ret);
		}

		/* Foreach remaining sorted minutia in list ... */
		for (minutiaIndex = 0; minutiaIndex < oMinutiae.get().getNum() - 1; minutiaIndex++) {
			/* Located neighbors and count number of ridges in between. */
			/* NOTE: neighbor and ridge count results are stored in */
			/* oMinutiae->list[i]. */
			if ((ret = countMinutiaRidges(minutiaIndex, oMinutiae, binarizedImageData, imageWidth, imageHeight,
					lfsParams)) != ILfs.FALSE) {
				return (ret);
			}
		}

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Takes a minutia and determines its closest neighbours and counts the
	 * number of intervening ridges between the minutia point and each of its
	 * neighbours.
	 * <p>
	 * NIST: {@code count_minutia_ridges()}. Neighbours are found with
	 * {@link #findNeighbors}, sorted with {@link #sortNeighbors} and each ridge
	 * count is computed with {@link #ridgeCount}. If no neighbours are found the
	 * minutia is left unchanged.
	 *
	 * @param first              index of the primary minutia in the list
	 * @param oMinutiae          input/output: list of minutiae; on return the
	 *                           primary minutia's neighbour list, ridge counts
	 *                           and neighbour count are set
	 * @param binarizedImageData binary image data (0 = white, 1 = black)
	 * @param imageWidth         width (in pixels) of image
	 * @param imageHeight        height (in pixels) of image
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return zero ({@link ILfs#FALSE}) on successful completion; negative on
	 *         system error
	 */
	public int countMinutiaRidges(final int first, AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
			final int imageWidth, final int imageHeight, final LfsParams lfsParams) {
		int i, ret;
		AtomicIntegerArray nbrList, nbrNRidges;
		AtomicInteger oNoOfNbrs = new AtomicInteger(0);

		/* Allocate list of neighbor oMinutiae indices. */
		nbrList = new AtomicIntegerArray(lfsParams.getMaxNbrs());

		/* Find up to the maximum number of qualifying neighbors. */
		if ((ret = findNeighbors(nbrList, oNoOfNbrs, lfsParams.getMaxNbrs(), first, oMinutiae)) < 0) {
			getFree().free(nbrList);
			return (ret);
		}

		if (isShowLogs())
			logger.debug(MessageFormat.format("NBRS FOUND: %d, %d = %d\n", oMinutiae.get().getList().get(first).getX(),
					oMinutiae.get().getList().get(first).getY(), oNoOfNbrs.get()));

		/* If no neighors found ... */
		if (oNoOfNbrs.get() == ILfs.FALSE) {
			/* Then no list returned and no ridges to count. */
			return (ILfs.FALSE);
		}

		/* Sort neighbors on delta dirs. */
		if ((ret = sortNeighbors(nbrList, oNoOfNbrs.get(), first, oMinutiae)) != ILfs.FALSE) {
			getFree().free(nbrList);
			return (ret);
		}

		/* Count ridges between first and neighbors. */
		/* List of ridge counts, one for each neighbor stored. */
		nbrNRidges = new AtomicIntegerArray(oNoOfNbrs.get());

		/* Foreach neighbor found and sorted in list ... */
		for (i = 0; i < oNoOfNbrs.get(); i++) {
			/* Count the ridges between the primary minutia and the neighbor. */
			ret = ridgeCount(first, nbrList.get(i), oMinutiae, binarizedImageData, imageWidth, imageHeight, lfsParams);
			/* If system error ... */
			if (ret < ILfs.FALSE) {
				/* Deallocate working memories. */
				getFree().free(nbrList);
				getFree().free(nbrNRidges);
				/* Return error code. */
				return (ret);
			}

			/* Otherwise, ridge count successful, so store ridge count to list. */
			nbrNRidges.set(i, ret);
		}

		/* Assign neighbor indices and ridge counts to primary minutia. */
		oMinutiae.get().getList().get(first).setNbrs(nbrList);
		oMinutiae.get().getList().get(first).setRidgeCounts(nbrNRidges);
		oMinutiae.get().getList().get(first).setNumNbrs(oNoOfNbrs.get());

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Takes a primary minutia and a list of all minutiae and locates a specified
	 * maximum number of closest neighbours to the primary point.
	 * <p>
	 * NIST: {@code find_neighbors()}. Neighbours are searched starting in the
	 * same pixel column, below the primary point, and then along consecutive and
	 * complete pixel columns in the image to the right of the primary point (the
	 * list must already be sorted on x then y). The search stops once the list is
	 * full and the x-distance alone exceeds the largest stored distance.
	 *
	 * @param oNbrList          output: list of detected closest neighbour
	 *                          indices (pre-allocated with at least
	 *                          {@code maxNbrs} entries)
	 * @param oNoOfNbrs         output: number of neighbours returned
	 * @param maxNbrs           maximum number of closest neighbours to be
	 *                          returned
	 * @param firstMinutiaIndex index of the primary minutia point
	 * @param oMinutiae         list of minutiae
	 * @return zero ({@link ILfs#FALSE}) on successful completion; negative on
	 *         system error
	 */
	public int findNeighbors(AtomicIntegerArray oNbrList, AtomicInteger oNoOfNbrs, final int maxNbrs,
			final int firstMinutiaIndex, AtomicReference<Minutiae> oMinutiae) {
		int ret, secondMinutiaIndex, lastNbr;
		Minutia firstMinutia, secondMinutia;
		AtomicInteger noOfNbrs = new AtomicInteger(0);
		AtomicReferenceArray<Double> nbrSqrDists;
		double xdist, xdist2;

		/* Allocate list of neighbor oMinutiae indices. */
		// do in calling class

		/* Allocate list of squared euclidean distances between neighbors */
		/* and current primary minutia point. */
		nbrSqrDists = new AtomicReferenceArray<Double>(maxNbrs);

		/* Initialize number of stored neighbors to 0. */
		noOfNbrs.set(0);
		/* Assign secondary to one passed current primary minutia. */
		secondMinutiaIndex = firstMinutiaIndex + 1;
		/* Compute location of maximum last stored neighbor. */
		lastNbr = maxNbrs - 1;

		/* While minutia (in sorted order) still remian for processing ... */
		/* NOTE: The minutia in the input list have been sorted on X and */
		/* then on Y. So, the neighbors are selected according to those */
		/* that lie below the primary minutia in the same pixel column and */
		/* then subsequently those that lie in complete pixel columns to */
		/* the right of the primary minutia. */
		while (secondMinutiaIndex < oMinutiae.get().getNum()) {
			/* Assign temporary minutia pointers. */
			firstMinutia = oMinutiae.get().getList().get(firstMinutiaIndex);
			secondMinutia = oMinutiae.get().getList().get(secondMinutiaIndex);

			/* Compute squared distance between oMinutiae along x-axis. */
			xdist = secondMinutia.getX() - firstMinutia.getX();
			xdist2 = xdist * xdist;

			/* If the neighbor lists are not full OR the x-distance to current */
			/* secondary is smaller than maximum neighbor distance stored ... */
			if ((noOfNbrs.get() < maxNbrs) || (xdist2 < nbrSqrDists.get(lastNbr))) {
				/* Append or insert the new neighbor into the neighbor lists. */
				if ((ret = updateNbrDists(oNbrList, nbrSqrDists, noOfNbrs, maxNbrs, firstMinutiaIndex,
						secondMinutiaIndex, oMinutiae)) < ILfs.FALSE) {
					getFree().free(nbrSqrDists);
					return (ret);
				}
			}
			/* Otherwise, if the neighbor lists is full AND the x-distance */
			/* to current secondary is larger than maximum neighbor distance */
			/* stored ... */
			else {
				/* So, stop searching for more neighbors. */
				break;
			}

			/* Bump to next secondary minutia. */
			secondMinutiaIndex++;
		}

		/* Deallocate working memory. */
		getFree().free(nbrSqrDists);

		/* If no neighbors found ... */
		if (noOfNbrs.get() == ILfs.FALSE) {
			/* Deallocate the neighbor list. */
			oNoOfNbrs.set(0);
		}
		/* Otherwise, assign neighbors to output pointer. */
		else {
			oNoOfNbrs.set(noOfNbrs.get());
		}

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Takes the current list of neighbours along with a primary minutia and a
	 * potential new neighbour, and determines whether the new neighbour is
	 * sufficiently close to be added to the list of nearest neighbours.
	 * <p>
	 * NIST: {@code update_nbr_dists()}. If added, the neighbour is placed in the
	 * list in its proper order based on squared distance to the primary point.
	 *
	 * @param oNbrList           input/output: current list of nearest neighbour
	 *                           minutia indices; updated on return
	 * @param oNbrSqrDists       input/output: corresponding squared Euclidean
	 *                           distance of each neighbour to the primary minutia
	 *                           point; updated on return
	 * @param oNoOfNbrs          input/output: number of neighbours currently in
	 *                           the lists; updated on return
	 * @param maxNbrs            maximum number of closest neighbours to be
	 *                           returned
	 * @param firstMinutiaIndex  index of the primary minutia point
	 * @param secondMinutiaIndex index of the secondary (new neighbour) point
	 * @param oMinutiae          list of minutiae
	 * @return zero ({@link ILfs#FALSE}) on successful completion (whether or not
	 *         the neighbour was inserted); negative on system error:
	 *         {@link ILfs#ERROR_CODE_470} illegal insertion position,
	 *         {@link ILfs#ERROR_CODE_471} insertion failed
	 */
	public int updateNbrDists(AtomicIntegerArray oNbrList, AtomicReferenceArray<Double> oNbrSqrDists,
			AtomicInteger oNoOfNbrs, final int maxNbrs, final int firstMinutiaIndex, final int secondMinutiaIndex,
			AtomicReference<Minutiae> oMinutiae) {
		double dist2;
		Minutia firstMinutia;
		Minutia secondMinutia;
		int pos;
		int lastNbr;
		int ret;

		/* Compute position of maximum last neighbor stored. */
		lastNbr = maxNbrs - 1;

		/* Assigne temporary minutia pointers. */
		firstMinutia = oMinutiae.get().getList().get(firstMinutiaIndex);
		secondMinutia = oMinutiae.get().getList().get(secondMinutiaIndex);

		/* Compute squared euclidean distance between minutia pair. */
		dist2 = getLfsUtil().squaredDistance(firstMinutia.getX(), firstMinutia.getY(), secondMinutia.getX(),
				secondMinutia.getY());

		/* If maximum number of neighbors not yet stored in lists OR */
		/* if the squared distance to current secondary is less */
		/* than the largest stored neighbor distance ... */
		if ((oNoOfNbrs.get() < maxNbrs) || (dist2 < oNbrSqrDists.get(lastNbr))) {

			/* Find insertion point in neighbor lists. */
			pos = getLfsUtil().findIncrementalPositionInDoubleArray(dist2, oNbrSqrDists, oNoOfNbrs.get());
			/* If the position returned is >= maximum list length (this should */
			/* never happen, but just in case) ... */
			if (pos >= maxNbrs) {
				logger.error("ERROR : updateNbrDists : illegal position for new neighbor\n");
				return (ILfs.ERROR_CODE_470);
			}
			/* Insert the new neighbor into the neighbor lists at the */
			/* specified location. */
			ret = insertNeighbor(pos, secondMinutiaIndex, dist2, oNbrList, oNbrSqrDists, oNoOfNbrs, maxNbrs);
			if (ret < ILfs.FALSE) {
				return (ILfs.ERROR_CODE_471);
			}

			/* Otherwise, neighbor inserted successfully, so return normally. */
			return (ILfs.FALSE);
		}
		/* Otherwise, the new neighbor is not sufficiently close to be */
		/* added or inserted into the neighbor lists, so ignore the neighbor */
		/* and return normally. */
		else {
			return (ILfs.FALSE);
		}
	}

	/**
	 * Takes a minutia index and its squared distance to a primary minutia point,
	 * and inserts them in the specified position of their respective lists,
	 * shifting previously stored values down and off the lists as necessary.
	 * <p>
	 * NIST: {@code insert_neighbor()}. If the lists are full, the last
	 * neighbour is dropped to make room.
	 *
	 * @param nbrListPos   position where values are to be inserted in the lists
	 *                     (zero-oriented)
	 * @param nbrIndex     index of minutia being inserted
	 * @param nbrDist2     squared distance of minutia to its primary point
	 * @param oNbrList     input/output: current list of nearest neighbour minutia
	 *                     indices; updated on return
	 * @param oNbrSqrDists input/output: corresponding squared Euclidean distance
	 *                     of each neighbour to the primary minutia point; updated
	 *                     on return
	 * @param oNoOfNbrs    input/output: number of neighbours currently in the
	 *                     lists; incremented if the lists were not full
	 * @param maxNbrs      maximum number of closest neighbours to be returned
	 * @return zero ({@link ILfs#FALSE}) on successful completion; negative on
	 *         system error: {@link ILfs#ERROR_CODE_480} insertion point exceeds
	 *         lists, {@link ILfs#ERROR_CODE_481} overflow in neighbour lists
	 */
	public int insertNeighbor(final int nbrListPos, final int nbrIndex, final double nbrDist2,
			AtomicIntegerArray oNbrList, AtomicReferenceArray<Double> oNbrSqrDists, AtomicInteger oNoOfNbrs,
			final int maxNbrs) {
		int currentNbrIndex;

		/* If the desired insertion position is beyond one passed the last */
		/* neighbor in the lists OR greater than equal to the maximum ... */
		/* NOTE: pos is zero-oriented while noOfNbrs and maxNbrs are 1-oriented. */
		if ((nbrListPos > oNoOfNbrs.get()) || (nbrListPos >= maxNbrs)) {
			logger.error("ERROR : insertNeighbor : insertion point exceeds lists\n");
			return (ILfs.ERROR_CODE_480);
		}

		/* If the neighbor lists are NOT full ... */
		if (oNoOfNbrs.get() < maxNbrs) {
			/* Then we have room to shift everything down to make room for new */
			/* neighbor and increase the number of neighbors stored by 1. */
			currentNbrIndex = oNoOfNbrs.get() - 1;
			oNoOfNbrs.set(oNoOfNbrs.get() + 1);
		}
		/* Otherwise, the neighbors lists are full ... */
		else if (oNoOfNbrs.get() == maxNbrs) {
			/* So, we must bump the last neighbor in the lists off to make */
			/* room for the new neighbor (ignore last neighbor in lists). */
			currentNbrIndex = oNoOfNbrs.get() - 2;
		}
		/* Otherwise, there is a list overflow error condition */
		/* (shouldn't ever happen, but just in case) ... */
		else {
			logger.error("ERROR : insertNeighbor : overflow in neighbor lists\n");
			return (ILfs.ERROR_CODE_481);
		}

		/* While we havn't reached the desired insertion point ... */
		while (currentNbrIndex >= nbrListPos) {
			/* Shift the current neighbor down the list 1 positon. */
			oNbrList.set(currentNbrIndex + 1, oNbrList.get(currentNbrIndex));
			oNbrSqrDists.set(currentNbrIndex + 1, oNbrSqrDists.get(currentNbrIndex));
			currentNbrIndex--;
		}

		/* We are now ready to put our new neighbor in the position where */
		/* we shifted everything down from to make room. */
		oNbrList.set(nbrListPos, nbrIndex);
		oNbrSqrDists.set(nbrListPos, nbrDist2);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Takes a primary minutia and its neighbouring minutia indices and sorts the
	 * neighbours based on their position relative to the primary minutia point.
	 * <p>
	 * NIST: {@code sort_neighbors()}. Neighbours are sorted starting vertical to
	 * the primary point and proceeding clockwise.
	 *
	 * @param oNbrList          input/output: list of neighbouring minutia
	 *                          indices; on return in sorted order
	 * @param noOfNbrs          number of neighbours in the list
	 * @param firstMinutiaIndex the index of the primary minutia point
	 * @param oMinutiae         list of minutiae
	 * @return zero ({@link ILfs#FALSE}) on successful completion (negative would
	 *         indicate a system error)
	 */
	public int sortNeighbors(AtomicIntegerArray oNbrList, final int noOfNbrs, final int firstMinutiaIndex,
			AtomicReference<Minutiae> oMinutiae) {
		AtomicReferenceArray<Double> joinThetas;
		double theta;
		double pi2 = ILfs.M_PI * 2.0;

		joinThetas = new AtomicReferenceArray<Double>(noOfNbrs);

		for (int minutiaIndex = 0; minutiaIndex < noOfNbrs; minutiaIndex++) {
			/* Compute angle to line connecting the 2 points. */
			/* Coordinates are swapped and order of points reversed to */
			/* account for 0 direction is vertical and positive direction */
			/* is clockwise. */
			int nbrIndex = oNbrList.get(minutiaIndex);
			theta = getLfsUtil().angleToLine(oMinutiae.get().getList().get(nbrIndex).getY(),
					oMinutiae.get().getList().get(nbrIndex).getX(),
					oMinutiae.get().getList().get(firstMinutiaIndex).getY(),
					oMinutiae.get().getList().get(firstMinutiaIndex).getX());

			/* Make sure the angle is positive. */
			theta += pi2;
			theta = getDefs().fMod(theta, pi2);
			joinThetas.set(minutiaIndex, theta);
		}

		/* Sort the neighbor indicies into rank order. */
		getSort().bubbleSortDoubleArrayIncremental2(joinThetas, oNbrList, noOfNbrs);

		/* Deallocate the list of angles. */
		getFree().free(joinThetas);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Takes a pair of minutiae and counts the number of ridges crossed along the
	 * linear trajectory connecting the two points in the image.
	 * <p>
	 * NIST: {@code ridge_count()}. After skipping to the first pixel of opposite
	 * value, repeatedly finds a 0-to-1 (ridge start) and a 1-to-0 (ridge end)
	 * transition and counts the pair as a ridge if
	 * {@link #validateRidgeCrossing} confirms it (using
	 * {@link LfsParams#getMaxRidgeSteps()}).
	 *
	 * @param firstMinutiaIndex  index of primary minutia
	 * @param secondMinutiaIndex index of secondary (neighbour) minutia
	 * @param oMinutiae          list of minutiae
	 * @param binarizedImageData binary image data (0 = white, 1 = black)
	 * @param imageWidth         width (in pixels) of image
	 * @param imageHeight        height (in pixels) of image
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return zero or positive: number of ridges counted; negative: system error
	 */
	public int ridgeCount(final int firstMinutiaIndex, final int secondMinutiaIndex,
			AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth, final int imageHeight,
			final LfsParams lfsParams) {
		Minutia firstMinutia;
		Minutia secondMinutia;
		AtomicInteger i = new AtomicInteger(0);
		int ret;
		int found;
		int[] xlist;
		int[] ylist;
		AtomicInteger num = new AtomicInteger(0);
		int ridgeCount;
		int ridgeStart;
		int ridgeEnd;
		int prevpix;
		int curpix;

		firstMinutia = oMinutiae.get().getList().get(firstMinutiaIndex);
		secondMinutia = oMinutiae.get().getList().get(secondMinutiaIndex);

		/* If the 2 mintuia have identical pixel coords ... */
		if ((firstMinutia.getX() == secondMinutia.getX()) && (firstMinutia.getY() == secondMinutia.getY())) {
			/* Then zero ridges between points. */
			return (ILfs.FALSE);
		}

		/* Compute linear trajectory of contiguous pixels between first */
		/* and second minutia points. */
		int aSize = Math.max(Math.abs(secondMinutia.getX() - firstMinutia.getX()) + 2,
				Math.abs(secondMinutia.getY() - firstMinutia.getY()) + 2);
		xlist = new int[aSize];
		ylist = new int[aSize];
		if ((ret = getLine().linePoints(xlist, ylist, num, firstMinutia.getX(), firstMinutia.getY(),
				secondMinutia.getX(), secondMinutia.getY())) != ILfs.FALSE) {
			return (ret);
		}

		/* It there are no points on the line trajectory, then no ridges */
		/* to count (this should not happen, but just in case) ... */
		if (num.get() == ILfs.FALSE) {
			getFree().free(xlist);
			getFree().free(ylist);
			return (ILfs.FALSE);
		}

		/* Find first pixel opposite type along linear trajectory from */
		/* first minutia. */
		prevpix = binarizedImageData[0 + (ylist[0] * imageWidth) + xlist[0]];
		i.set(1);
		found = ILfs.FALSE;
		while (i.get() < num.get()) {
			curpix = binarizedImageData[0 + (ylist[i.get()] * imageWidth) + xlist[i.get()]];
			if (curpix != prevpix) {
				found = ILfs.TRUE;
				break;
			}
			i.set(i.get() + 1);
		}

		/* If opposite pixel not found ... then no ridges to count */
		if (found == ILfs.FALSE) {
			getFree().free(xlist);
			getFree().free(ylist);
			return (ILfs.FALSE);
		}

		/* Ready to count ridges, so initialize counter to 0. */
		ridgeCount = 0;

		if (isShowLogs())
			logger.debug(MessageFormat.format("RIDGE COUNT: {0},{1} to {2},{3} ", firstMinutia.getX(), firstMinutia.getY(),
					secondMinutia.getX(), secondMinutia.getY()));

		/* While not at the end of the trajectory ... */
		while (i.get() < num.get()) {
			/* If 0-to-1 transition not found ... */
			if (findTransition(i, 0, 1, xlist, ylist, num.get(), binarizedImageData, imageWidth,
					imageHeight) == ILfs.FALSE) {
				/* Then we are done looking for ridges. */
				getFree().free(xlist);
				getFree().free(ylist);

				if (isShowLogs())
					logger.debug("\n");

				/* Return number of ridges counted to this point. */
				return (ridgeCount);
			}
			/* Otherwise, we found a new ridge start transition, so store */
			/* its location (the location of the 1 in 0-to-1 transition). */
			ridgeStart = i.get();

			if (isShowLogs())
				logger.debug(": RS {0},{1} ", xlist[i.get()], ylist[i.get()]);

			/* If 1-to-0 transition not found ... */
			if (findTransition(i, 1, 0, xlist, ylist, num.get(), binarizedImageData, imageWidth,
					imageHeight) == ILfs.FALSE) {
				/* Then we are done looking for ridges. */
				getFree().free(xlist);
				getFree().free(ylist);

				if (isShowLogs())
					logger.debug("\n");

				/* Return number of ridges counted to this point. */
				return (ridgeCount);
			}
			/* Otherwise, we found a new ridge end transition, so store */
			/* its location (the location of the 0 in 1-to-0 transition). */
			ridgeEnd = i.get();

			if (isShowLogs())
				logger.debug("; RE {0},{1} ", xlist[i.get()], ylist[i.get()]);

			/* Conduct the validation, tracing the contour of the ridge */
			/* from the ridge ending point a specified number of steps */
			/* scanning for neighbors clockwise and counter-clockwise. */
			/* If the ridge starting point is encounted during the trace */
			/* then we can assume we do not have a valid ridge crossing */
			/* and instead we are walking on and off the edge of the */
			/* side of a ridge. */
			ret = validateRidgeCrossing(ridgeStart, ridgeEnd, xlist, ylist, num.get(), binarizedImageData, imageWidth,
					imageHeight, lfsParams.getMaxRidgeSteps());
			/* If system error ... */
			if (ret < ILfs.FALSE) {
				getFree().free(xlist);
				getFree().free(ylist);
				/* Return the error code. */
				return (ret);
			}

			if (isShowLogs())
				logger.debug("; V{0}", ret);

			/* If validation result is TRUE ... */
			if (ret != ILfs.FALSE) {
				/* Then assume we have found a valid ridge crossing and bump */
				/* the ridge counter. */
				ridgeCount++;
			}

			/* Otherwise, ignore the current ridge start and end transitions */
			/* and go back and search for new ridge start. */
		}

		/* Deallocate working memories. */
		getFree().free(xlist);
		getFree().free(ylist);

		if (isShowLogs())
			logger.debug(" ");

		/* Return the number of ridges counted. */
		return (ridgeCount);
	}

	/**
	 * Takes a pixel trajectory and a starting index, and searches forward along
	 * the trajectory until the specified adjacent pixel pair is found.
	 * <p>
	 * NIST: {@code find_trans()}. On success the position is set to the index of
	 * the second pixel in the pair; otherwise it is set to {@code num} (the end
	 * of the trajectory).
	 *
	 * @param startPixel         input/output: starting pixel index into the
	 *                           trajectory; on return the location where the
	 *                           second pixel in the pair is found (or
	 *                           {@code num})
	 * @param firstPixel         first pixel value in transition pair
	 * @param secondPixel        second pixel value in transition pair
	 * @param xlist              x-pixel coords of line trajectory
	 * @param ylist              y-pixel coords of line trajectory
	 * @param num                number of coords in line trajectory
	 * @param binarizedImageData binary image data (0 = white, 1 = black)
	 * @param imageWidth         width (in pixels) of image
	 * @param imageHeight        height (in pixels) of image
	 * @return {@link ILfs#TRUE} if the pixel pair transition was found;
	 *         {@link ILfs#FALSE} if it was not found
	 */
	public int findTransition(AtomicInteger startPixel, final int firstPixel, final int secondPixel, final int[] xlist,
			final int[] ylist, final int num, int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int i, j;

		/* Set previous index to starting position. */
		i = startPixel.get();
		/* Bump previous index by 1 to get next index. */
		j = i + 1;

		/* While not one point from the end of the trajectory .. */
		while (i < num - 1) {
			/* If we have found the desired transition ... */
			if ((binarizedImageData[0 + (ylist[i] * imageWidth) + xlist[i]] == firstPixel)
					&& (binarizedImageData[0 + (ylist[j] * imageWidth) + xlist[j]] == secondPixel)) {
				/* Adjust the position pointer to the location of the */
				/* second pixel in the transition. */
				startPixel.set(j);

				/* Return TRUE. */
				return (ILfs.TRUE);
			}
			/* Otherwise, the desired transition was not found in current */
			/* pixel pair, so bump to the next pair along the trajector. */
			i++;
			j++;
		}

		/* If we get here, then we exhausted the trajector without finding */
		/* the desired transition, so set the position pointer to the end */
		/* of the trajector, and return FALSE. */
		startPixel.set(num);
		return (ILfs.FALSE);
	}

	/**
	 * Takes a ridge start transition and a ridge end transition along a line
	 * trajectory, and walks the ridge contour from the ridge end point a
	 * specified number of steps looking for the ridge start point.
	 * <p>
	 * NIST: {@code validate_ridge_crossing()}. The contour is traced both
	 * clockwise and counter-clockwise; if the ridge start is encountered (or a
	 * trace is ignored), the transitions are determined not to be a valid ridge
	 * crossing (we are instead walking on and off the side of a ridge).
	 *
	 * @param ridgeStart         index into line trajectory of ridge start
	 *                           transition
	 * @param ridgeEnd           index into line trajectory of ridge end
	 *                           transition
	 * @param xlist              x-pixel coords of line trajectory
	 * @param ylist              y-pixel coords of line trajectory
	 * @param num                number of coords in line trajectory
	 * @param binarizedImageData binary image data (0 = white, 1 = black)
	 * @param imageWidth         width (in pixels) of image
	 * @param imageHeight        height (in pixels) of image
	 * @param maxRidgeSteps      number of steps taken in search in both scan
	 *                           directions
	 * @return {@link ILfs#TRUE} if the ridge crossing is VALID;
	 *         {@link ILfs#FALSE} if the ridge crossing is INVALID; negative on
	 *         system error
	 */
	public int validateRidgeCrossing(final int ridgeStart, final int ridgeEnd, final int[] xlist, final int[] ylist,
			final int num, int[] binarizedImageData, final int imageWidth, final int imageHeight,
			final int maxRidgeSteps) {
		AtomicInteger ret = new AtomicInteger(0);
		AtomicInteger featureX = new AtomicInteger(0), featureY = new AtomicInteger(0), edgeX = new AtomicInteger(0),
				edgeY = new AtomicInteger(0);
		Contour contour = null;
		AtomicInteger noOfContour = new AtomicInteger(0);

		/* Assign edge pixel pair for contour trace. */
		featureX.set(xlist[ridgeEnd]);
		featureY.set(ylist[ridgeEnd]);
		edgeX.set(xlist[ridgeEnd - 1]);
		edgeY.set(ylist[ridgeEnd - 1]);

		/* Adjust pixel pair if they neighbor each other diagonally. */
		getContour().fixEdgePixelPair(featureX, featureY, edgeX, edgeY, binarizedImageData, imageWidth, imageHeight);

		/* Trace ridge contour, starting at the ridge end transition, and */
		/* taking a specified number of step scanning for edge neighbors */
		/* clockwise. As we trace the ridge, we want to detect if we */
		/* encounter the ridge start transition. NOTE: The ridge end */
		/* position is on the white (of a black to white transition) and */
		/* the ridge start is on the black (of a black to white trans), */
		/* so the edge trace needs to look for the what pixel (not the */
		/* black one) of the ridge start transition. */
		contour = getContour().traceContour(ret, noOfContour, maxRidgeSteps, xlist[ridgeStart - 1],
				ylist[ridgeStart - 1], featureX.get(), featureY.get(), edgeX.get(), edgeY.get(), ILfs.SCAN_CLOCKWISE,
				binarizedImageData, imageWidth, imageHeight);
		/* If a system error occurred ... */
		if (ret.get() < ILfs.FALSE) {
			/* Return error code. */
			return (ret.get());
		}

		/* Otherwise, if the trace was not IGNORED, then a contour was */
		/* was generated and returned. We aren't interested in the */
		/* actual contour, so deallocate it. */
		if (ret.get() != ILfs.IGNORE) {
			getContour().freeContour(contour);
		}

		/* If the trace was IGNORED, then we had some sort of initialization */
		/* problem, so treat this the same as if was actually located the */
		/* ridge start point (in which case LOOP_FOUND is returned). */
		/* So, If not IGNORED and ridge start not encounted in trace ... */
		if ((ret.get() != ILfs.IGNORE) && (ret.get() != ILfs.LOOP_FOUND)) {
			/* Now conduct contour trace scanning for edge neighbors counter- */
			/* clockwise. */
			contour = getContour().traceContour(ret, noOfContour, maxRidgeSteps, xlist[ridgeStart - 1],
					ylist[ridgeStart - 1], featureX.get(), featureY.get(), edgeX.get(), edgeY.get(),
					ILfs.SCAN_COUNTER_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
			/* If a system error occurred ... */
			if (ret.get() < ILfs.FALSE) {
				/* Return error code. */
				return (ret.get());
			}

			/* Otherwise, if the trace was not IGNORED, then a contour was */
			/* was generated and returned. We aren't interested in the */
			/* actual contour, so deallocate it. */
			if (ret.get() != ILfs.IGNORE) {
				getContour().freeContour(contour);
			}

			/* If trace not IGNORED and ridge start not encounted in 2nd trace ... */
			if ((ret.get() != ILfs.IGNORE) && (ret.get() != ILfs.LOOP_FOUND)) {
				/* If we get here, assume we have a ridge crossing. */
				return (ILfs.TRUE);
			}
			/* Otherwise, second trace returned IGNORE or ridge start found. */
		}
		/* Otherwise, first trace returned IGNORE or ridge start found. */

		/* If we get here, then we failed to validate a ridge crossing. */
		return (ILfs.FALSE);
	}
}