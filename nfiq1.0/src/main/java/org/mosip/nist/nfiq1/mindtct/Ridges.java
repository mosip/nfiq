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

public class Ridges extends MindTct implements IRidges {
	private static final Logger logger = LoggerFactory.getLogger(Ridges.class);
	private static Ridges instance;

	private Ridges() {
		super();
	}

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

	public Defs getDefs() {
		return Defs.getInstance();
	}

	public ImageUtil getImageUtil() {
		return ImageUtil.getInstance();
	}

	public Globals getGlobals() {
		return Globals.getInstance();
	}

	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	public Free getFree() {
		return Free.getInstance();
	}

	public Init getInit() {
		return Init.getInstance();
	}

	public Binarization getBinarization() {
		return Binarization.getInstance();
	}

	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}

	public Sort getSort() {
		return Sort.getInstance();
	}

	public Detect getDetect() {
		return Detect.getInstance();
	}

	public RemoveMinutia getRemoveMinutia() {
		return RemoveMinutia.getInstance();
	}

	public Line getLine() {
		return Line.getInstance();
	}

	public Contour getContour() {
		return Contour.getInstance();
	}

	public Maps getMap() {
		return Maps.getInstance();
	}

	public Loop getLoop() {
		return Loop.getInstance();
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: countMinutiaeRidges - Takes a list of oMinutiae, and for each one,
	 * #cat: determines its closest neighbors and counts the number #cat: of
	 * interveining ridges between the minutia point and #cat: each of its
	 * neighbors. Input: oMinutiae - list of oMinutiae binarizedImageData - binary
	 * image data (0==while & 1==black) imageWidth - width (in pixels) of image
	 * imageHeight - height (in pixels) of image lfsParams - parameters and
	 * thresholds for controlling LFS Output: oMinutiae - list of oMinutiae
	 * augmented with neighbors and ridge counts Return Code: Zero - successful
	 * completion Negative - system error
	 **************************************************************************/
	public int countMinutiaeRidges(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
			final int imageHeight, final LfsParams lfsParams) {
		int ret;
		int minutiaIndex;

		if (isShowLogs())
			logger.info("\nFINDING NBRS AND COUNTING RIDGES:\n");

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

	/*************************************************************************
	 **************************************************************************
	 * #cat: countMinutiaRidges - Takes a minutia, and determines its closest #cat:
	 * neighbors and counts the number of interveining ridges #cat: between the
	 * minutia point and each of its neighbors. Input: oMinutiae - input minutia
	 * binarizedImageData - binary image data (0==while & 1==black) imageWidth -
	 * width (in pixels) of image imageHeight - height (in pixels) of image
	 * lfsParams - parameters and thresholds for controlling LFS Output: oMinutiae -
	 * minutia augmented with neighbors and ridge counts Return Code: Zero -
	 * successful completion Negative - system error
	 **************************************************************************/
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
			logger.info(MessageFormat.format("NBRS FOUND: %d, %d = %d\n", oMinutiae.get().getList().get(first).getX(),
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: findNeighbors - Takes a primary minutia and a list of all oMinutiae
	 * #cat: and locates a specified maximum number of closest neighbors #cat: to
	 * the primary point. Neighbors are searched, starting #cat: in the same pixel
	 * column, below, the primary point and then #cat: along consecutive and
	 * complete pixel columns in the image #cat: to the right of the primary point.
	 * Input: maxNbrs - maximum number of closest neighbors to be returned
	 * firstMinutiaIndex - index of the primary minutia point oMinutiae - list of
	 * oMinutiae Output: oNbrList - points to list of detected closest neighbors
	 * oNoOfNbrs - points to number of neighbors returned Return Code: Zero -
	 * successful completion Negative - system error
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: updateNbrDists - Takes the current list of neighbors along with a #cat:
	 * primary minutia and a potential new neighbor, and #cat: determines if the new
	 * neighbor is sufficiently close #cat: to be added to the list of nearest
	 * neighbors. If added, #cat: it is placed in the list in its proper order based
	 * on #cat: squared distance to the primary point. Input: oNbrList - current
	 * list of nearest neighbor minutia indices oNbrSqrDists - corresponding squared
	 * euclidean distance of each neighbor to the primary minutia point oNoOfNbrs -
	 * number of neighbors currently in the list maxNbrs - maximum number of closest
	 * neighbors to be returned firstMinutiaIndex - index of the primary minutia
	 * point secondMinutiaIndex - index of the secondary (new neighbor) point
	 * oMinutiae - list of oMinutiae Output: nbrList - updated list of nearest
	 * neighbor indices nbrSqrDists - updated list of nearest neighbor distances
	 * noOfNbrs - number of neighbors in the update lists Return Code: Zero -
	 * successful completion Negative - system error
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: insertNeighbor - Takes a minutia index and its squared distance to a
	 * #cat: primary minutia point, and inserts them in the specified #cat: position
	 * of their respective lists, shifting previously #cat: stored values down and
	 * off the lists as necessary. Input: nbrListPos - postions where values are to
	 * be inserted in lists nbrIndex - index of minutia being inserted nbrDist2 -
	 * squared distance of minutia to its primary point oNbrList - current list of
	 * nearest neighbor minutia indices oNbrSqrDists - corresponding squared
	 * euclidean distance of each neighbor to the primary minutia point oNoOfNbrs -
	 * number of neighbors currently in the list maxNbrs - maximum number of closest
	 * neighbors to be returned Output: nbrList - updated list of nearest neighbor
	 * indices nbrSqrDists - updated list of nearest neighbor distances noOfNbrs -
	 * number of neighbors in the update lists Return Code: Zero - successful
	 * completion Negative - system error
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: sortNeighbors - Takes a list of primary minutia and its neighboring
	 * #cat: minutia indices and sorts the neighbors based on their #cat: position
	 * relative to the primary minutia point. Neighbors #cat: are sorted starting
	 * vertical to the primary point and #cat: proceeding clockwise. Input: oNbrList
	 * - list of neighboring minutia indices noOfNbrs - number of neighbors in the
	 * list firstMinutiaIndex - the index of the primary minutia point oMinutiae -
	 * list of oMinutiae Output: oNbrList - neighboring minutia indices in sorted
	 * order Return Code: Zero - successful completion Negative - system error
	 **************************************************************************/
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
			theta = getLfsUtil().angleToLine(oMinutiae.get().getList().get(minutiaIndex).getY(),
					oMinutiae.get().getList().get(minutiaIndex).getX(),
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: ridgeCount - Takes a pair of oMinutiae, and counts the number of #cat:
	 * ridges crossed along the linear trajectory connecting #cat: the 2 points in
	 * the image. Input: firstMinutiaIndex - index of primary minutia
	 * secondMinutiaIndex - index of secondary (neighbor) minutia oMinutiae - list
	 * of oMinutiae binarizedImageData - binary image data (0==while & 1==black)
	 * imageWidth - width (in pixels) of image imageHeight - height (in pixels) of
	 * image lfsParams - parameters and thresholds for controlling LFS Return Code:
	 * Zero or Positive - number of ridges counted Negative - system error
	 **************************************************************************/
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
			logger.info(MessageFormat.format("RIDGE COUNT: {0},{1} to {2},{3} ", firstMinutia.getX(), firstMinutia.getY(),
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
					logger.info("\n");

				/* Return number of ridges counted to this point. */
				return (ridgeCount);
			}
			/* Otherwise, we found a new ridge start transition, so store */
			/* its location (the location of the 1 in 0-to-1 transition). */
			ridgeStart = i.get();

			if (isShowLogs())
				logger.info(": RS {0},{1} ", xlist[i.get()], ylist[i.get()]);

			/* If 1-to-0 transition not found ... */
			if (findTransition(i, 1, 0, xlist, ylist, num.get(), binarizedImageData, imageWidth,
					imageHeight) == ILfs.FALSE) {
				/* Then we are done looking for ridges. */
				getFree().free(xlist);
				getFree().free(ylist);

				if (isShowLogs())
					logger.info("\n");

				/* Return number of ridges counted to this point. */
				return (ridgeCount);
			}
			/* Otherwise, we found a new ridge end transition, so store */
			/* its location (the location of the 0 in 1-to-0 transition). */
			ridgeEnd = i.get();

			if (isShowLogs())
				logger.info("; RE {0},{1} ", xlist[i.get()], ylist[i.get()]);

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
				logger.info("; V{0}", ret);

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
			logger.info(" ");

		/* Return the number of ridges counted. */
		return (ridgeCount);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: findTransition - Takes a pixel trajectory and a starting index, and
	 * #cat: searches forward along the trajectory until the specified #cat:
	 * adjacent pixel pair is found, returning the index where #cat: the pair was
	 * found (the index of the second pixel). Input: startPixel - pointer to
	 * starting pixel index into trajectory firstPixel - first pixel value in
	 * transition pair secondPixel - second pixel value in transition pair xlist -
	 * x-pixel coords of line trajectory ylist - y-pixel coords of line trajectory
	 * num - number of coords in line trajectory binarizedImageData - binary image
	 * data (0==while & 1==black) imageWidth - width (in pixels) of image
	 * imageHeight - height (in pixels) of image Output: iptr - points to location
	 * where 2nd pixel in pair is found Return Code: TRUE - pixel pair transition
	 * found FALSE - pixel pair transition not found
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: validateRidgeCrossing - Takes a pair of points, a ridge start #cat:
	 * transition and a ridge end transition, and walks the #cat: ridge contour from
	 * thre ridge end points a specified #cat: number of steps, looking for the
	 * ridge start point. #cat: If found, then transitions determined not to be a
	 * valid #cat: ridge crossing. Input: ridgeStart - index into line trajectory of
	 * ridge start transition ridgeEnd - index into line trajectory of ridge end
	 * transition xlist - x-pixel coords of line trajectory ylist - y-pixel coords
	 * of line trajectory num - number of coords in line trajectory
	 * binarizedImageData - binary image data (0==while & 1==black) imageWidth -
	 * width (in pixels) of image imageHeight - height (in pixels) of image
	 * maxRidgeSteps - number of steps taken in search in both scan directions
	 * Return Code: TRUE - ridge crossing VALID FALSE - ridge corssing INVALID
	 * Negative - system error
	 **************************************************************************/
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