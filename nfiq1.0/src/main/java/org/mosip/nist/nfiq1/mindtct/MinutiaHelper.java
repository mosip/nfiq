package org.mosip.nist.nfiq1.mindtct;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IMinutia;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.Minutia;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinutiaHelper extends MindTct implements IMinutia {
	private static final Logger logger = LoggerFactory.getLogger(MinutiaHelper.class);

	private static MinutiaHelper instance;

	private MinutiaHelper() {
		super();
	}

	public static synchronized MinutiaHelper getInstance() {
		if (instance == null) {
			synchronized (MinutiaHelper.class) {
				if (instance == null) {
					instance = new MinutiaHelper();
				}
			}
		}
		return instance;
	}

	public MatchPattern getMatchPattern() {
		return MatchPattern.getInstance();
	}

	public Globals getGlobals() {
		return Globals.getInstance();
	}

	public Contour getContour() {
		return Contour.getInstance();
	}

	public Line getLine() {
		return Line.getInstance();
	}

	public Free getFree() {
		return Free.getInstance();
	}

	public Sort getSort() {
		return Sort.getInstance();
	}

	public Loop getLoop() {
		return Loop.getInstance();
	}

	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: allocMinutiae - Allocates and initializes a minutia list based on the
	 * #cat: specified maximum number of minutiae to be detected. Input: maxMinutiae
	 * - number of minutia to be allocated in list Output: oMinutiae - points to the
	 * allocated minutiae list Return Code: Zero - successful completion Negative -
	 * system error
	 **************************************************************************/
	public int allocMinutiae(AtomicReference<Minutiae> oMinutiae, final int maxMinutiae) {
		List<Minutia> list = new ArrayList<>(maxMinutiae);

		oMinutiae.get().setList(list);
		oMinutiae.get().setAlloc(maxMinutiae);
		oMinutiae.get().setNum(0);

		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: reallocMinutiae - Reallocates a previously allocated minutia list #cat:
	 * extending its allocated length based on the specified #cat: increment. Input:
	 * oMinutiae - previously allocated list of minutiae points incrMinutiae -
	 * number of minutia to be allocated in list Output: oMinutiae - extended list
	 * of minutiae points Return Code: Zero - successful completion Negative -
	 * system error
	 **************************************************************************/
	public int reallocMinutiae(AtomicReference<Minutiae> oMinutiae, final int incrMinutiae) {
		oMinutiae.get().setAlloc(oMinutiae.get().getAlloc() + incrMinutiae);
		((ArrayList<?>) oMinutiae.get().getList()).ensureCapacity(oMinutiae.get().getList().size() + incrMinutiae);

		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: detectMinutiaeV2 - Takes a binary image and its associated #cat:
	 * Direction and Low Flow Maps and scans each image block #cat: with valid
	 * direction for minutia points. Minutia points #cat: detected in LOW FLOW
	 * blocks are set with lower reliability. Input: binarizedImageData - binary
	 * image data (0==while & 1==black) imageWidth - width (in pixels) of image
	 * imageHeight - height (in pixels) of image Maps - object contains all below
	 * //direction_map - map of image blocks containing directional ridge flow
	 * //low_flow_map - map of image blocks flagged as LOW RIDGE FLOW
	 * //high_curve_map - map of image blocks flagged as HIGH CURVATURE
	 * mappedImageWidth - width (in blocks) of the maps mappedImageHeight - height
	 * (in blocks) of the maps lfsParams - parameters and thresholds for controlling
	 * LFS Output: oMinutiae - points to a list of detected minutia structures
	 * Return Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int detectMinutiaeV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
			final int mappedImageWidth, final int mappedImageHeight, Maps map, LfsParams lfsParams) {
		AtomicInteger ret = new AtomicInteger(0);
		int mapSize = mappedImageWidth * mappedImageHeight;
		AtomicIntegerArray pDirectionMap = new AtomicIntegerArray(mapSize);
		AtomicIntegerArray oLowFlowMap = new AtomicIntegerArray(mapSize);
		AtomicIntegerArray pHighCurveMap = new AtomicIntegerArray(mapSize);

		/* Pixelize the maps by assigning block values to individual pixels. */
		ret.set(map.pixelizeMap(pDirectionMap, mappedImageWidth, mappedImageHeight, map.getDirectionMap(),
				map.getMappedImageWidth().get(), map.getMappedImageHeight().get(), lfsParams.getBlockOffsetSize()));
		if (ret.get() != ILfs.FALSE) {
			return ret.get();
		}

		ret.set(map.pixelizeMap(oLowFlowMap, mappedImageWidth, mappedImageHeight, map.getLowFlowMap(),
				map.getMappedImageWidth().get(), map.getMappedImageHeight().get(), lfsParams.getBlockOffsetSize()));
		if (ret.get() != ILfs.FALSE) {
			return ret.get();
		}

		ret.set(map.pixelizeMap(pHighCurveMap, mappedImageWidth, mappedImageHeight, map.getHighCurveMap(),
				map.getMappedImageWidth().get(), map.getMappedImageHeight().get(), lfsParams.getBlockOffsetSize()));
		if (ret.get() != ILfs.FALSE) {
			return ret.get();
		}

		ret.set(scanForMinutiaeHorizontallyV2(oMinutiae, binarizedImageData, mappedImageWidth, mappedImageHeight,
				pDirectionMap, oLowFlowMap, pHighCurveMap, lfsParams));
		if (ret.get() < ILfs.FALSE) {
			return ret.get();
		}

		ret.set(scanForMinutiaeVerticallyV2(oMinutiae, binarizedImageData, mappedImageWidth, mappedImageHeight,
				pDirectionMap, oLowFlowMap, pHighCurveMap, lfsParams));
		if (ret.get() < ILfs.FALSE) {
			return ret.get();
		}

		/* Deallocate working memories. */
		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: updateMinutiae - Takes a detected minutia point and (if it is not #cat:
	 * determined to already be in the minutiae list) adds it to #cat: the list.
	 * Input: oMinutiae - minutia structure for detected point binarizedImageData -
	 * binary image data (0==while & 1==black) imageWidth - width (in pixels) of
	 * image imageHeight - height (in pixels) of image lfsParams - parameters and
	 * thresholds for controlling LFS Output: oMinutiae - points to a list of
	 * detected minutia structures Return Code: Zero - minutia added to successfully
	 * added to minutiae list IGNORE - minutia is to be ignored (already in the
	 * minutiae list) Negative - system error
	 **************************************************************************/
	public int updateMinutiae(AtomicReference<Minutiae> oMinutiae, Minutia minutia, int[] binarizedImageData,
			final int imageWidth, final int imageHeight, final LfsParams lfsParams) {
		int minutiaIndex;
		int ret;
		int distanceY;
		int distanceX;
		int deltaDir;
		int qtrNDirs;
		int fullNDirs;

		/* Check to see if minutiae list is full ... if so, then extend */
		/* the length of the allocated list of minutia points. */
		if (oMinutiae.get().getNum() >= oMinutiae.get().getAlloc()) {
			if ((ret = reallocMinutiae(oMinutiae, ILfs.MAX_MINUTIAE)) != ILfs.FALSE) {
				return (ret);
			}
		}

		/* Otherwise, there is still room for more minutia. */

		/* Compute quarter of possible directions in a semi-circle */
		/* (ie. 45 degrees). */
		qtrNDirs = lfsParams.getNumDirections() >> 2;

		/* Compute number of directions in full circle. */
		fullNDirs = lfsParams.getNumDirections() << 1;

		/* Is the minutiae list empty? */
		if (oMinutiae.get().getNum() > 0) {
			/* Foreach minutia stored in the list... */
			for (minutiaIndex = 0; minutiaIndex < oMinutiae.get().getNum(); minutiaIndex++) {
				/* If x distance between new minutia and current list minutia */
				/* are sufficiently close... */
				distanceX = Math.abs(oMinutiae.get().getList().get(minutiaIndex).getX() - minutia.getX());
				if (distanceX < lfsParams.getMaxMinutiaDelta()) {
					/* If y distance between new minutia and current list minutia */
					/* are sufficiently close... */
					distanceY = Math.abs(oMinutiae.get().getList().get(minutiaIndex).getY() - minutia.getY());
					if (distanceY < lfsParams.getMaxMinutiaDelta()) {
						/* If new minutia and current list minutia are same type... */
						if (oMinutiae.get().getList().get(minutiaIndex).getType() == minutia.getType()) {
							/* Test to see if minutiae have similar directions. */
							/* Take minimum of computed inner and outer */
							/* direction differences. */
							deltaDir = Math.abs(oMinutiae.get().getList().get(minutiaIndex).getDirection()
									- minutia.getDirection());
							deltaDir = Math.min(deltaDir, fullNDirs - deltaDir);
							/* If directional difference is <= 45 degrees... */
							if (deltaDir <= qtrNDirs) {
								/* If new minutia and current list minutia share */
								/* the same point... */
								if ((distanceX == 0) && (distanceY == 0)) {
									/* Then the minutiae match, so don't add the new one */
									/* to the list. */
									return (ILfs.IGNORE);
								}
								/* Othewise, check if they share the same contour. */
								/* Start by searching "max_minutia_delta" steps */
								/* clockwise. */
								/* If new minutia point found on contour... */
								if (getContour().searchContour(minutia.getX(), minutia.getY(),
										lfsParams.getMaxMinutiaDelta(),
										oMinutiae.get().getList().get(minutiaIndex).getX(),
										oMinutiae.get().getList().get(minutiaIndex).getY(),
										oMinutiae.get().getList().get(minutiaIndex).getEx(),
										oMinutiae.get().getList().get(minutiaIndex).getEy(), ILfs.SCAN_CLOCKWISE,
										binarizedImageData, imageWidth, imageHeight) == ILfs.FOUND) {
									/* Consider the new minutia to be the same as the */
									/* current list minutia, so don't add the new one */
									/* to the list. */
									return (ILfs.IGNORE);
								}
								/* Now search "max_minutia_delta" steps counter- */
								/* clockwise along contour. */
								/* If new minutia point found on contour... */
								if (getContour().searchContour(minutia.getX(), minutia.getY(),
										lfsParams.getMaxMinutiaDelta(),
										oMinutiae.get().getList().get(minutiaIndex).getX(),
										oMinutiae.get().getList().get(minutiaIndex).getY(),
										oMinutiae.get().getList().get(minutiaIndex).getEx(),
										oMinutiae.get().getList().get(minutiaIndex).getEy(),
										ILfs.SCAN_COUNTER_CLOCKWISE, binarizedImageData, imageWidth,
										imageHeight) == ILfs.FOUND) {
									/* Consider the new minutia to be the same as the */
									/* current list minutia, so don't add the new one */
									/* to the list. */
									return (ILfs.IGNORE);
								}

								/* Otherwise, new minutia and current list minutia do */
								/* not share the same contour, so although they are */
								/* similar in type and location, treat them as 2 */
								/* different minutia. */
							} // Otherwise, directions are too different.
						} // Otherwise, minutiae are different type.
					} // Otherwise, minutiae too far apart in Y.
				} // Otherwise, minutiae too far apart in X.
			} // End FOR minutia in list.
		} // Otherwise, minutiae list is empty.

		/* Otherwise, assume new minutia is not in the list, so add it. */
		oMinutiae.get().getList().add(oMinutiae.get().getNum(), minutia);
		oMinutiae.get().setNum(oMinutiae.get().getNum() + 1);

		/* New minutia was successfully added to the list. */
		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: updateMinutiaeV2 - Takes a detected minutia point and (if it is not
	 * #cat: determined to already be in the minutiae list or the #cat: new point is
	 * determined to be "more compatible") adds #cat: it to the list. Input:
	 * oMinutiae - minutia structure for detected point scanDir - orientation of
	 * scan when minutia was detected directionMapValue - directional ridge flow of
	 * block minutia is in binarizedImageData - binary image data (0==while &
	 * 1==black) imageWidth - width (in pixels) of image imageHeight - height (in
	 * pixels) of image lfsParams - parameters and thresholds for controlling LFS
	 * Output: oMinutiae - points to a list of detected oMinutiae structures Return
	 * Code: Zero - minutia added to successfully added to oMinutiae list IGNORE -
	 * minutia is to be ignored (already in the oMinutiae list) Negative - system
	 * error
	 **************************************************************************/
	public int updateMinutiaeV2(AtomicReference<Minutiae> oMinutiae, Minutia minutia, final int scanDir,
			final int directionMapValue, int[] binarizedImageData, final int imageWidth, final int imageHeight,
			final LfsParams lfsParams) {
		int i;
		int ret;
		int dy;
		int dx;
		int deltaDir;
		int qtrNDirs;
		int fullNDirs;
		int mapScanDir;

		/* Check to see if minutiae list is full ... if so, then extend */
		/* the length of the allocated list of minutia points. */
		if (oMinutiae.get().getNum() >= oMinutiae.get().getAlloc()) {
			if ((ret = reallocMinutiae(oMinutiae, ILfs.MAX_MINUTIAE)) != ILfs.FALSE) {
				return (ret);
			}
		}

		/* Otherwise, there is still room for more minutia. */

		/* Compute quarter of possible directions in a semi-circle */
		/* (ie. 45 degrees). */
		qtrNDirs = lfsParams.getNumDirections() >> 2;

		/* Compute number of directions in full circle. */
		fullNDirs = lfsParams.getNumDirections() << 1;

		/* Is the minutiae list empty? */
		if (oMinutiae.get().getNum() > 0) {
			/* Foreach minutia stored in the list (in reverse order) ... */
			for (i = oMinutiae.get().getNum() - 1; i >= 0; i--) {
				/* If x distance between new minutia and current list minutia */
				/* are sufficiently close... */
				dx = Math.abs(oMinutiae.get().getList().get(i).getX() - minutia.getX());
				if (dx < lfsParams.getMaxMinutiaDelta()) {
					/* If y distance between new minutia and current list minutia */
					/* are sufficiently close... */
					dy = Math.abs(oMinutiae.get().getList().get(i).getY() - minutia.getY());
					if (dy < lfsParams.getMaxMinutiaDelta()) {
						/* If new minutia and current list minutia are same type... */
						if (oMinutiae.get().getList().get(i).getType() == minutia.getType()) {
							/* Test to see if minutiae have similar directions. */
							/* Take minimum of computed inner and outer */
							/* direction differences. */
							deltaDir = Math
									.abs(oMinutiae.get().getList().get(i).getDirection() - minutia.getDirection());
							deltaDir = Math.min(deltaDir, fullNDirs - deltaDir);
							/* If directional difference is <= 45 degrees... */
							if (deltaDir <= qtrNDirs) {
								/* If new minutia and current list minutia share */
								/* the same point... */
								if ((dx == 0) && (dy == 0)) {
									/* Then the minutiae match, so don't add the new one */
									/* to the list. */

									return (ILfs.IGNORE);
								}
								/* Othewise, check if they share the same contour. */
								/* Start by searching "max_minutia_delta" steps */
								/* clockwise. */
								/* If new minutia point found on contour... */
								if (getContour().searchContour(minutia.getX(), minutia.getY(),
										lfsParams.getMaxMinutiaDelta(), oMinutiae.get().getList().get(i).getX(),
										oMinutiae.get().getList().get(i).getY(),
										oMinutiae.get().getList().get(i).getEx(),
										oMinutiae.get().getList().get(i).getEy(), ILfs.SCAN_CLOCKWISE,
										binarizedImageData, imageWidth, imageHeight) == ILfs.FOUND
										|| getContour().searchContour(minutia.getX(), minutia.getY(),
												lfsParams.getMaxMinutiaDelta(), oMinutiae.get().getList().get(i).getX(),
												oMinutiae.get().getList().get(i).getY(),
												oMinutiae.get().getList().get(i).getEx(),
												oMinutiae.get().getList().get(i).getEy(), ILfs.SCAN_COUNTER_CLOCKWISE,
												binarizedImageData, imageWidth, imageHeight) == ILfs.FOUND) {
									/* If new minutia has VALID block direction ... */
									if (directionMapValue >= ILfs.FALSE) {
										/* Derive feature scan direction compatible */
										/* with VALID direction. */
										mapScanDir = chooseScanDirection(directionMapValue,
												lfsParams.getNumDirections());
										/* If map scan direction compatible with scan */
										/* direction in which new minutia was found ... */
										if (mapScanDir == scanDir) {
											/* Then choose the new minutia over the one */
											/* currently in the list. */
											if ((ret = removeMinutia(i, oMinutiae)) != ILfs.FALSE) {
												return (ret);
											}
											/* Continue on ... */
										} else {
											/* Othersize, scan directions not compatible... */
											/* so choose to keep the current minutia in */
											/* the list and ignore the new one. */

											return (ILfs.IGNORE);
										}
									} else {
										/* Otherwise, no reason to believe new minutia */
										/* is any better than the current one in the list, */
										/* so consider the new minutia to be the same as */
										/* the current list minutia, and don't add the new */
										/* one to the list. */
										return (ILfs.IGNORE);
									}
								}

								/* Otherwise, new minutia and current list minutia do */
								/* not share the same contour, so although they are */
								/* similar in type and location, treat them as 2 */
								/* different minutia. */
							} // Otherwise, directions are too different.
						} // Otherwise, minutiae are different type.
					} // Otherwise, minutiae too far apart in Y.
				} // Otherwise, minutiae too far apart in X.
			} // End FOR minutia in list.
		} // Otherwise, minutiae list is empty.

		/* Otherwise, assume new minutia is not in the list, or those that */
		/* were close neighbors were selectively removed, so add it. */

		oMinutiae.get().getList().add(oMinutiae.get().getNum(), minutia);
		oMinutiae.get().setNum(oMinutiae.get().getNum() + 1);

		/* New minutia was successfully added to the list. */
		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: sortMinutiaeTopToBottomAndThenLeftToRight - Takes a list of minutia
	 * points and sorts them #cat: top-to-bottom and then left-to-right. Input:
	 * oMinutiae - list of minutiae imageWidth - width (in pixels) of image
	 * imageHeight - height (in pixels) of image Output: oMinutiae - list of sorted
	 * minutiae Return Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int sortMinutiaeTopToBottomAndThenLeftToRight(AtomicReference<Minutiae> oMinutiae, final int imageWidth,
			final int imageHeight) {
		AtomicIntegerArray ranks, order;
		int i;
		int ret;
		List<Minutia> newlist;

		ranks = new AtomicIntegerArray(oMinutiae.get().getNum());
		order = new AtomicIntegerArray(oMinutiae.get().getNum());

		/* Compute 1-D image pixel offsets form 2-D minutia coordinate points. */
		for (i = 0; i < oMinutiae.get().getNum(); i++) {
			ranks.set(i,
					(oMinutiae.get().getList().get(i).getY() * imageWidth) + oMinutiae.get().getList().get(i).getX());
		}

		/* Get sorted order of minutiae. */
		if ((ret = getSort().sortIndicesIntArrayIncremental(order, ranks, oMinutiae.get().getNum())) != ILfs.FALSE) {
			getFree().free(ranks);
			return (ret);
		}

		/* Allocate new MINUTIA list to hold sorted minutiae. */
		newlist = new ArrayList<Minutia>(oMinutiae.get().getNum());

		/* Put minutia into sorted order in new list. */
		for (i = 0; i < oMinutiae.get().getNum(); i++) {
			newlist.add(oMinutiae.get().getList().get(order.get(i)));
		}
		/* Deallocate non-sorted list of minutia pointers. */
		oMinutiae.get().getList().clear();
		/* Assign new sorted list of minutia to minutiae list. */
		for (int index = 0; index < newlist.size(); index++) {
			oMinutiae.get().getList().add(index, newlist.get(index));
		}

		/* Free the working memories supporting the sort. */
		getFree().free(order);
		getFree().free(ranks);
		getFree().free(newlist);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: sortMinutiaeLeftToRightAndThenTopToBottom - Takes a list of minutia
	 * points and sorts them #cat: left-to-right and then top-to-bottom. Input:
	 * oMinutiae - list of oMinutiae imageWidth - width (in pixels) of image
	 * imageHeight - height (in pixels) of image Output: oMinutiae - list of sorted
	 * oMinutiae Return Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int sortMinutiaeLeftToRightAndThenTopToBottom(AtomicReference<Minutiae> oMinutiae, final int imageWidth,
			final int imageHeight) {
		AtomicIntegerArray ranks;
		AtomicIntegerArray order;
		int i;
		int ret;
		List<Minutia> newlist;

		ranks = new AtomicIntegerArray(oMinutiae.get().getNum());
		order = new AtomicIntegerArray(oMinutiae.get().getNum());

		/* Compute 1-D image pixel offsets form 2-D minutia coordinate points. */
		for (i = 0; i < oMinutiae.get().getNum(); i++) {
			ranks.set(i,
					(oMinutiae.get().getList().get(i).getX() * imageWidth) + oMinutiae.get().getList().get(i).getY());
		}

		/* Get sorted order of minutiae. */
		if ((ret = getSort().sortIndicesIntArrayIncremental(order, ranks, oMinutiae.get().getNum())) != ILfs.FALSE) {
			getFree().free(ranks);
			return (ret);
		}

		/* Allocate new MINUTIA list to hold sorted minutiae. */
		newlist = new ArrayList<>(oMinutiae.get().getNum());

		/* Put minutia into sorted order in new list. */
		for (i = 0; i < oMinutiae.get().getNum(); i++) {
			newlist.add(oMinutiae.get().getList().get(order.get(i)));
		}
		/* Deallocate non-sorted list of minutia pointers. */
		oMinutiae.get().getList().clear();
		/* Assign new sorted list of minutia to minutiae list. */
		for (int index = 0; index < newlist.size(); index++) {
			oMinutiae.get().getList().add(index, newlist.get(index));
		}

		/* Free the working memories supporting the sort. */
		getFree().free(order);
		getFree().free(ranks);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: removeRedundantMinutiaeremoveRedundantMinutiae - Takes a list of
	 * minutiae sorted in some adjacent order #cat: and detects and removes
	 * redundant minutia that have the #cat: same exact pixel coordinate locations
	 * (even if other #cat: attributes may differ). Input: oMinutiae - list of
	 * sorted minutiae Output: oMinutiae - list of sorted minutiae with duplicates
	 * removed Return Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int removeRedundantMinutiae(AtomicReference<Minutiae> oMinutiae) {
		int i;
		int ret;
		Minutia firstMinutia;
		Minutia secondMinutia;

		/* Work backward from the end of the list of minutiae. This way */
		/* we can selectively remove minutia from the list and not cause */
		/* problems with keeping track of current indices. */
		for (i = oMinutiae.get().getNum() - 1; i > 0; i--) {
			firstMinutia = oMinutiae.get().getList().get(i);
			secondMinutia = oMinutiae.get().getList().get(i - 1);
			/* If minutia pair has identical coordinates ... */
			if ((firstMinutia.getX() == secondMinutia.getX()) && (firstMinutia.getY() == secondMinutia.getY())) {
				/* Remove the 2nd minutia from the minutiae list. */
				if ((ret = removeMinutia(i - 1, oMinutiae)) != ILfs.FALSE) {
					return (ret);
				}
				/* The first minutia slides into the position of the 2nd. */
			}
		}

		/* Return successfully. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: dump_minutiae - Given a oMinutiae list, writes a formatted text report
	 * of #cat: the list's contents to the specified open file pointer. Input:
	 * oMinutiae - list of minutia structures Output: file - open file pointer
	 **************************************************************************/
	public void dumpMinutiae(File file, AtomicReference<Minutiae> oMinutiae) {
		try (FileWriter myWriter = new FileWriter(file.getAbsoluteFile())){
			myWriter.write(MessageFormat.format("{0} Minutiae Detected", oMinutiae.get().getNum()));
			int i, j;
			for (i = 0; i < oMinutiae.get().getNum(); i++) {
				/* Precision of reliablity added one decimal position */
				/* on 09-13-04 */
				myWriter.write(MessageFormat.format("{0} : {1}, {2} : {3} : {4} :", i,
						oMinutiae.get().getList().get(i).getX(), oMinutiae.get().getList().get(i).getY(),
						oMinutiae.get().getList().get(i).getDirection(),
						oMinutiae.get().getList().get(i).getReliability()));
				if (oMinutiae.get().getList().get(i).getType() == ILfs.RIDGE_ENDING) {
					myWriter.write("RIG : ");
				} else {
					myWriter.write("BIF : ");
				}

				if (oMinutiae.get().getList().get(i).getAppearing() == ILfs.APPEARING) {
					myWriter.write("APP : ");
				} else {
					myWriter.write("DIS : ");
				}

				myWriter.write(MessageFormat.format("{0} ", oMinutiae.get().getList().get(i).getFeatureId()));

				for (j = 0; j < oMinutiae.get().getList().get(i).getNumNbrs(); j++) {
					myWriter.write(MessageFormat.format(": {0},{1}; {2} ",
							oMinutiae.get().getList().get(oMinutiae.get().getList().get(i).getNbrs().get(j)).getX(),
							oMinutiae.get().getList().get(oMinutiae.get().getList().get(i).getNbrs().get(j)).getY(),
							oMinutiae.get().getList().get(oMinutiae.get().getList().get(i).getRidgeCounts().get(j))));
				}

				myWriter.write("");
			}

			logger.info("dumpMinutiae::Successfully wrote to the file.");
		} catch (IOException e) {
			logger.error("An error occurred.", e);
		}
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: dumpMinutiaePoints - Given a oMinutiae list, writes the coordinate
	 * point #cat: for each minutia in the list to the specified open #cat: file
	 * pointer. Input: oMinutiae - list of minutia structures Output: file - open
	 * file pointer
	 **************************************************************************/
	public void dumpMinutiaePoints(File file, final AtomicReference<Minutiae> oMinutiae) {
		try (FileWriter myWriter = new FileWriter(file.getAbsoluteFile())){
			/* First line in the output file contians the number of minutia */
			/* points to be written to the file. */
			myWriter.write(MessageFormat.format("{0}", oMinutiae.get().getNum()));

			int i;
			/* Foreach minutia in list... */
			for (i = 0; i < oMinutiae.get().getNum(); i++) {
				/* Write the minutia's coordinate point to the file pointer. */
				myWriter.write(MessageFormat.format("{0} {1}", oMinutiae.get().getList().get(i).getX(),
						oMinutiae.get().getList().get(i).getY()));
			}

			logger.info("dumpMinutiaePoints::Successfully wrote to the file.");
		} catch (IOException e) {
			logger.error("An error occurred.", e);
		}
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: dumpReliableMinutiaePoints - Given a oMinutiae list, writes the #cat:
	 * coordinate point for each oMinutiae in the list that has #cat: the specified
	 * reliability to the specified open #cat: file pointer. Input: oMinutiae - list
	 * of minutia structures reliability - desired reliability level for oMinutiae
	 * to be reported Output: file - open file pointer
	 **************************************************************************/
	public void dumpReliableMinutiaePoints(File file, AtomicReference<Minutiae> oMinutiae, final double reliability) {
		try (FileWriter myWriter = new FileWriter(file.getAbsoluteFile())){
			int i;
			int count;

			/* First count the number of qualifying minutiae so that the */
			/* MFS header may be written. */
			count = 0;
			/* Foreach minutia in list... */
			for (i = 0; i < oMinutiae.get().getNum(); i++) {
				if (oMinutiae.get().getList().get(i).getReliability() == reliability)
					count++;
			}

			/* First line in the output file contians the number of minutia */
			/* points to be written to the file. */
			myWriter.write(MessageFormat.format("{0}", count));

			/* Foreach minutia in list... */
			for (i = 0; i < oMinutiae.get().getNum(); i++) {
				if (oMinutiae.get().getList().get(i).getReliability() == reliability) {
					/* Write the minutia's coordinate point to the file pointer. */
					myWriter.write(MessageFormat.format("{0} {1}", oMinutiae.get().getList().get(i).getX(),
							oMinutiae.get().getList().get(i).getY()));
				}
			}

			logger.info("Successfully wrote to the file.");
		} catch (IOException e) {
			logger.error("An error occurred.", e);
		}
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: createMinutia - Takes attributes associated with a detected minutia
	 * #cat: point and allocates and initializes a minutia structure. Input: xLoc -
	 * x-pixel coord of minutia (interior to feature) yLoc - y-pixel coord of
	 * minutia (interior to feature) xEdge - x-pixel coord of corresponding edge
	 * pixel (exterior to feature) yEdge - y-pixel coord of corresponding edge pixel
	 * (exterior to feature) iDir - integer direction of the minutia reliability -
	 * floating point measure of minutia's reliability type - type of the minutia
	 * (ridge-ending or bifurcation) appearing - designates the minutia as appearing
	 * or disappearing featureId - index of minutia's matching feature_patterns[]
	 * Output: Minutia - ponter to an allocated and initialized minutia structure
	 *************************************************************************/
	public Minutia createMinutia(final int xLoc, final int yLoc, final int xEdge, final int yEdge, final int iDir,
			final double reliability, final int type, final int appearing, final int featureId) {
		Minutia minutia = new Minutia();
		/* Assign minutia structure attributes. */
		minutia.setX(xLoc);
		minutia.setY(yLoc);
		minutia.setEx(xEdge);
		minutia.setEy(yEdge);
		minutia.setDirection(iDir);
		minutia.setReliability(reliability);
		minutia.setType(type);
		minutia.setAppearing(appearing);
		minutia.setFeatureId(featureId);
		minutia.setNbrs(null);
		minutia.setRidgeCounts(null);
		minutia.setNumNbrs(0);

		/* Return normally. */
		return minutia;
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: removeMinutia - Removes the specified minutia point from the input
	 * #cat: list of minutiae. Input: index - position of minutia to be removed from
	 * list oMinutiae - input list of minutiae Output: oMinutiae - list with minutia
	 * removed Return Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int removeMinutia(final int index, AtomicReference<Minutiae> oMinutiae) {
		int fromIndex;
		int toIndex;

		/* Make sure the requested index is within range. */
		if ((index < 0) && (index >= oMinutiae.get().getNum())) {
			logger.error("ERROR : removeMinutia : index out of range");
			return (ILfs.ERROR_CODE_380);
		}

		/* Slide the remaining list of ominutiae up over top of the */
		/* position of the minutia being removed. */
		for (toIndex = index, fromIndex = index + 1; fromIndex < oMinutiae.get().getNum(); toIndex++, fromIndex++) {
			oMinutiae.get().getList().set(toIndex, oMinutiae.get().getList().get(fromIndex));
		}

		/* Deallocate the minutia structure to be removed the last one. */
		oMinutiae.get().getList().remove(oMinutiae.get().getList().size() - 1);

		/* Decrement the number of ominutiae remaining in the list. */
		oMinutiae.get().setNum(oMinutiae.get().getNum() - 1);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: joinMinutia - Takes 2 minutia points and connectes their features in
	 * #cat: the input binary image. A line is drawn in the image #cat: between the
	 * 2 minutia with a specified line-width radius #cat: and a conditional border
	 * of pixels opposite in color #cat: from the interior line. Input: minutia1 -
	 * first minutia point to be joined minutia2 - second minutia point to be joined
	 * binarizedImageData - binary image data (0==while & 1==black) imageWidth -
	 * width (in pixels) of image imageHeight - height (in pixels) of image
	 * with_boundary - signifies the inclusion of border pixels line_radius -
	 * line-width radius of join line Output: binarizedImageData - edited image with
	 * minutia features joined Return Code: Zero - successful completion Negative -
	 * system error
	 **************************************************************************/
	public int joinMinutia(Minutia minutia1, Minutia minutia2, int[] binarizedImageData, final int imageWidth,
			final int imageHeight, final int with_boundary, final int line_radius) {
		int dxGreaterThandy;
		int deltaX;
		int deltaY;
		int[] xList = null;
		int[] yList = null;
		AtomicInteger oNum = new AtomicInteger(0);
		int minutiaPixel = 0;
		int boundaryPixel;
		int i;
		int j;
		int ret;
		int x1;
		int y1;
		int x2;
		int y2;

		/* Compute X and Y deltas between minutia points. */
		deltaX = Math.abs(minutia1.getX() - minutia2.getX());
		deltaY = Math.abs(minutia1.getY() - minutia2.getY());

		/* Set flag based on |DX| >= |DY|. */
		/* If flag is true then add additional pixel width to the join line */
		/* by adding pixels neighboring top and bottom. */
		/* If flag is false then add additional pixel width to the join line */
		/* by adding pixels neighboring left and right. */
		if (deltaX >= deltaY) {
			dxGreaterThandy = 1;
		} else {
			dxGreaterThandy = 0;
		}

		/* Compute points along line segment between the two minutia points. */
		/* Compute maximum number of points needed to hold line segment. */
		// init x_list and y_list before calling
		int asize = Math.max(Math.abs(minutia2.getX() - minutia1.getX()) + 2,
				Math.abs(minutia2.getY() - minutia1.getY()) + 2);
		xList = new int[asize];
		yList = new int[asize];

		if ((ret = getLine().linePoints(xList, yList, oNum, minutia1.getX(), minutia1.getY(), minutia2.getX(),
				minutia2.getY())) != ILfs.FALSE) {
			/* If error with line routine, return error code. */
			return (ret);
		}

		/* Determine pixel color of minutia and boundary. */
		if (minutia1.getType() == ILfs.RIDGE_ENDING) {
			/* To connect 2 ridge-endings, draw black. */
			minutiaPixel = 1;
			boundaryPixel = 0;
		} else {
			/* To connect 2 bifurcations, draw white. */
			minutiaPixel = 0;
			boundaryPixel = 1;
		}

		/* Foreach point on line connecting the minutiae points ... */
		for (i = 1; i < oNum.get() - 1; i++) {
			/* Draw minutia pixel at current point on line. */
			binarizedImageData[0 + (yList[i] * imageWidth) + xList[i]] = minutiaPixel;

			/* Initialize starting corrdinates for adding width to the */
			/* join line to the current point on the line. */
			x1 = xList[i];
			y1 = yList[i];
			x2 = x1;
			y2 = y1;
			/* Foreach pixel of added radial width ... */
			for (j = 0; j < line_radius; j++) {
				/* If |DX|>=|DY|, we want to add width to line by writing */
				/* to pixels neighboring above and below. */
				/* x1 -= (0=(1-1)); y1 -= 1 ==> ABOVE */
				/* x2 += (0=(1-1)); y2 += 1 ==> BELOW */
				/* If |DX|<|DY|, we want to add width to line by writing */
				/* to pixels neighboring left and right. */
				/* x1 -= (1=(1-0)); y1 -= 0 ==> LEFT */
				/* x2 += (1=(1-0)); y2 += 0 ==> RIGHT */

				/* Advance 1st point along width dimension. */
				x1 -= (1 - dxGreaterThandy);
				y1 -= dxGreaterThandy;
				/* If pixel 1st point is within image boundaries ... */
				if ((x1 >= 0) && (x1 < imageWidth) && (y1 >= 0) && (y1 < imageHeight)) {
					/* Write the pixel ABOVE or LEFT. */
					binarizedImageData[0 + (y1 * imageWidth) + x1] = minutiaPixel;
				}

				/* Advance 2nd point along width dimension. */
				x2 += (1 - dxGreaterThandy);
				y2 += dxGreaterThandy;
				/* If pixel 2nd point is within image boundaries ... */
				if ((x2 >= 0) && (x2 < imageWidth) && (y2 >= 0) && (y2 < imageHeight)) {
					binarizedImageData[0 + (y2 * imageWidth) + x2] = minutiaPixel;
				}
			}

			/* If boundary flag is set ... draw the boundary pixels. */
			if (with_boundary != 0) {
				/* Advance 1st point along width dimension. */
				x1 -= (1 - dxGreaterThandy);
				y1 -= dxGreaterThandy;
				/* If pixel 1st point is within image boundaries ... */
				if ((x1 >= 0) && (x1 < imageWidth) && (y1 >= 0) && (y1 < imageHeight)) {
					/* Write the pixel ABOVE or LEFT of opposite color. */
					binarizedImageData[0 + (y1 * imageWidth) + x1] = boundaryPixel;
				}

				/* Advance 2nd point along width dimension. */
				x2 += (1 - dxGreaterThandy);
				y2 += dxGreaterThandy;
				/* If pixel 2nd point is within image boundaries ... */
				if ((x2 >= 0) && (x2 < imageWidth) && (y2 >= 0) && (y2 < imageHeight)) {
					/* Write the pixel BELOW or RIGHT of opposite color. */
					binarizedImageData[0 + (y2 * imageWidth) + x2] = boundaryPixel;
				}
			}
		}

		/* Deallocate points along connecting line. */
		getFree().free(xList);
		getFree().free(yList);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: getMinutiaType - Given the pixel color of the detected feature, returns
	 * #cat: whether the minutia is a ridge-ending (black pixel) or #cat:
	 * bifurcation (white pixel). Input: featurePixel - pixel color of the feature's
	 * interior Return Code: RIDGE_ENDING - minutia is a ridge-ending BIFURCATION -
	 * minutia is a bifurcation (valley-ending)
	 **************************************************************************/
	public int getMinutiaType(final int featurePixel) {
		int type;

		/* If feature pixel is white ... */
		if (featurePixel == 0) {
			/* Then the feature is a valley-ending, so BIFURCATION. */
			type = ILfs.BIFURCATION;
		}
		/* Otherwise, the feature pixel is black ... */
		else {
			/* So the feature is a RIDGE-ENDING. */
			type = ILfs.RIDGE_ENDING;
		}

		/* Return the type. */
		return (type);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: isMinutiaAppearing - Given the pixel location of a minutia feature
	 * #cat: and its corresponding adjacent edge pixel, returns whether #cat: the
	 * minutia is appearing or disappearing. Remeber, that #cat: "feature" refers to
	 * either a ridge or valley-ending. Input: xLoc - x-pixel coord of feature
	 * (interior to feature) yLoc - y-pixel coord of feature (interior to feature)
	 * xEdge - x-pixel coord of corresponding edge pixel (exterior to feature) yEdge
	 * - y-pixel coord of corresponding edge pixel (exterior to feature) Return
	 * Code: APPEARING - minutia is appearing (TRUE==1) DISAPPEARING - minutia is
	 * disappearing (FALSE==0) Negative - system error
	 **************************************************************************/
	public int isMinutiaAppearing(final int xLoc, final int yLoc, final int xEdge, final int yEdge) {
		/* Edge pixels will always be N,S,E,W of feature pixel. */

		/* 1. When scanning for feature's HORIZONTALLY... */
		/* If the edge is above the feature, then appearing. */
		if (xEdge < xLoc) {
			return (ILfs.APPEARING);
		}
		/* If the edge is below the feature, then disappearing. */
		if (xEdge > xLoc) {
			return (ILfs.DISAPPEARING);
		}

		/* 1. When scanning for feature's VERTICALLY... */
		/* If the edge is left of feature, then appearing. */
		if (yEdge < yLoc) {
			return (ILfs.APPEARING);
		}
		/* If the edge is right of feature, then disappearing. */
		if (yEdge > yLoc) {
			return (ILfs.DISAPPEARING);
		}

		/* Should never get here, but just in case. */
		logger.error("ERROR : isMinutiaAppearing : bad configuration of pixels");
		return (ILfs.ERROR_CODE_240);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: chooseScanDirection - Determines the orientation (horizontal or #cat:
	 * vertical) in which a block is to be scanned for minutiae. #cat: The
	 * orientation is based on the blocks corresponding IMAP #cat: direction. Input:
	 * nInputBlockImageMapValue - Block's IMAP direction nDirs - number of possible
	 * IMAP directions (within semicircle) Return Code: SCAN_HORIZONTAL - horizontal
	 * orientation SCAN_VERTICAL - vertical orientation
	 **************************************************************************/
	public int chooseScanDirection(final int nInputBlockImageMapValue, final int nDirs) {
		int qtrNDirs;

		/* Compute quarter of directions in semi-circle. */
		qtrNDirs = nDirs >> 2;

		/* If ridge flow in block is relatively vertical, then we want */
		/* to scan for minutia features in the opposite direction */
		/* (ie. HORIZONTALLY). */
		if ((nInputBlockImageMapValue <= qtrNDirs) || (nInputBlockImageMapValue > (qtrNDirs * 3))) {
			return (ILfs.SCAN_HORIZONTAL);
		}
		/* Otherwise, ridge flow is realtively horizontal, and we want */
		/* to scan for minutia features in the opposite direction */
		/* (ie. VERTICALLY). */
		else {
			return (ILfs.SCAN_VERTICAL);
		}
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: scanForMinutiae - Scans a block of binary image data detecting
	 * potential #cat: minutiae points. Input: binarizedImageData - binary image
	 * data (0==while & 1==black) imageWidth - width (in pixels) of image
	 * imageHeight - height (in pixels) of image oInputBlockImageMap - matrix of
	 * ridge flow directions oNMap - IMAP augmented with blocks of HIGH-CURVATURE
	 * and blocks which have no neighboring valid directions. blockX - x-block coord
	 * to be scanned blockY - y-block coord to be scanned mapWidth - width (in
	 * blocks) of IMAP and NMAP matrices. mapHeight - height (in blocks) of IMAP and
	 * NMAP matrices. scanX - x-pixel coord of origin of region to be scanned scanY
	 * - y-pixel coord of origin of region to be scanned scanWidth - width (in
	 * pixels) of region to be scanned scanHeight - height (in pixels) of region to
	 * be scanned scanDir - the scan orientation (horizontal or vertical) lfsParams
	 * - parameters and thresholds for controlling LFS Output: oMinutiae - points to
	 * a list of detected minutia structures Return Code: Zero - successful
	 * completion Negative - system error
	 **************************************************************************/
	public int scanForMinutiae(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
			final int imageHeight, AtomicIntegerArray oInputBlockImageMap, AtomicIntegerArray oNMap, final int blockX,
			final int blockY, final int mapWidth, final int mapHeight, final int scanX, final int scanY,
			final int scanWidth, final int scanHeight, final int scanDir, final LfsParams lfsParams) {
		int blockIndex;
		int ret;

		/* Compute block index from block coordinates. */
		blockIndex = (blockY * mapWidth) + blockX;

		/* Conduct primary scan for minutiae horizontally. */
		if (scanDir == ILfs.SCAN_HORIZONTAL) {
			if ((ret = scanForMinutiaeHorizontally(oMinutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap.get(blockIndex), oNMap.get(blockIndex), scanX, scanY, scanWidth, scanHeight,
					lfsParams)) != ILfs.FALSE) {
				/* Return code may be: */
				/* 1. ret<0 (implying system error) */
				return (ret);
			}

			/* Rescan block vertically. */
			if ((ret = rescanForMinutiaeVertically(oMinutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap, oNMap, blockX, blockY, mapWidth, mapHeight, scanX, scanY, scanWidth,
					scanHeight, lfsParams)) != ILfs.FALSE) {
				/* Return code may be: */
				/* 1. ret<0 (implying system error) */
				return (ret);
			}
		}
		/* Otherwise, conduct primary scan for minutiae vertically. */
		else {
			if ((ret = scanForMinutiaeVertically(oMinutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap.get(blockIndex), oNMap.get(blockIndex), scanX, scanY, scanWidth, scanHeight,
					lfsParams)) != ILfs.FALSE) {
				/* Return resulting code. */
				return (ret);
			}

			/* Rescan block horizontally. */
			if ((ret = rescanForMinutiaeHorizontally(oMinutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap, oNMap, blockX, blockY, mapWidth, mapHeight, scanX, scanY, scanWidth,
					scanHeight, lfsParams)) != ILfs.FALSE) {
				/* Return resulting code. */
				return (ret);
			}
		}

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: scan4minutiae_horizontally - Scans a specified region of binary image
	 * #cat: data horizontally, detecting potential minutiae points. #cat: Minutia
	 * detected via the horizontal scan process are #cat: by nature vertically
	 * oriented (orthogonal to the scan). #cat: The region actually scanned is
	 * slightly larger than that #cat: specified. This overlap attempts to minimize
	 * the number #cat: of minutiae missed at the region boundaries. #cat: HOWEVER,
	 * some minutiae will still be missed! Input: binarizedImageData - binary image
	 * data (0==while & 1==black) imageWidth - width (in pixels) of image
	 * imageHeight - height (in pixels) of image nInputBlockImageMapValue - IMAP
	 * value associated with this image region nNMapValue - NMAP value associated
	 * with this image region scanX - x-pixel coord of origin of region to be
	 * scanned scanY - y-pixel coord of origin of region to be scanned scanWidth -
	 * width (in pixels) of region to be scanned scanHeight - height (in pixels) of
	 * region to be scanned lfsParams - parameters and thresholds for controlling
	 * LFS Output: oMinutiae - points to a list of detected minutia structures
	 * Return Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int scanForMinutiaeHorizontally(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
			final int imageWidth, final int imageHeight, final int nInputBlockImageMapValue, final int nNMapValue,
			final int scanX, final int scanY, final int scanWidth, final int scanHeight, final LfsParams lfsParams) {
		int sx;
		int sy;
		int ex;
		int ey;
		AtomicInteger cx = new AtomicInteger(0);
		AtomicInteger cy = new AtomicInteger(0);
		int x2;
		AtomicInteger p1ptrIndex = new AtomicInteger(0);
		AtomicInteger p2ptrIndex = new AtomicInteger(0);
		AtomicIntegerArray possible = new AtomicIntegerArray(ILfs.NFEATURES);
		AtomicInteger nposs = new AtomicInteger(0);
		int ret;

		/* NOTE!!! Minutia that "straddle" region boundaries may be missed! */

		/* If possible, overlap left and right of current scan region */
		/* by 2 pixel columns to help catch some minutia that straddle the */
		/* the scan region boundaries. */
		sx = Math.max(0, scanX - 2);
		ex = Math.min(imageWidth, scanX + scanWidth + 2);

		/* If possible, overlap the scan region below by 1 pixel row. */
		sy = scanY;
		ey = Math.min(imageHeight, scanY + scanHeight + 1);

		/* For now, we will not adjust for IMAP edge, as the binary image */
		/* was properly padded at its edges so as not to cause anomallies. */

		/* Start at first row in region. */
		cy.set(sy);
		/* While second scan row not outside the bottom of the scan region... */
		while ((cy.get() + 1) < ey) {
			/* Start at beginning of new scan row in region. */
			cx.set(sx);
			/* While not at end of region's current scan row. */
			while (cx.get() < ex) {
				/* Get pixel pair from current x position in current and next */
				/* scan rows. */
				p1ptrIndex.set(0 + (cy.get() * imageWidth) + cx.get());
				p2ptrIndex.set(0 + ((cy.get() + 1) * imageWidth) + cx.get());
				/* If scan pixel pair matches first pixel pair of */
				/* 1 or more features... */
				if (getMatchPattern().matchFirstPair(binarizedImageData[p1ptrIndex.get()],
						binarizedImageData[p2ptrIndex.get()], possible, nposs) != ILfs.FALSE) {
					/* Bump forward to next scan pixel pair. */
					cx.set(cx.get() + 1);
					p1ptrIndex.set(p1ptrIndex.get() + 1);
					p2ptrIndex.set(p1ptrIndex.get() + 1);
					/* If not at end of region's current scan row... */
					if (cx.get() < ex) {
						/* If scan pixel pair matches second pixel pair of */
						/* 1 or more features... */
						if (getMatchPattern().matchSecondPair(binarizedImageData[p1ptrIndex.get()],
								binarizedImageData[p2ptrIndex.get()], possible, nposs) != ILfs.FALSE) {
							/* Store current x location. */
							x2 = cx.get();
							/* Skip repeated pixel pairs. */
							getMatchPattern().skipRepeatedHorizontalPair(cx, ex, binarizedImageData, p1ptrIndex,
									p2ptrIndex, imageWidth, imageHeight);
							/* If not at end of region's current scan row... */
							if (cx.get() < ex) {
								/* If scan pixel pair matches third pixel pair of */
								/* a single feature... */
								if (getMatchPattern().matchThirdPair(binarizedImageData[p1ptrIndex.get()],
										binarizedImageData[p2ptrIndex.get()], possible, nposs) != ILfs.FALSE) {
									/* Process detected minutia point. */
									if ((ret = processHorizontalScanMinutia(oMinutiae, cx.get(), cy.get(), x2,
											possible.get(0), binarizedImageData, imageWidth, imageHeight,
											nInputBlockImageMapValue, nNMapValue, lfsParams)) != ILfs.FALSE) {
										/* Return code may be: */
										/* 1. ret< 0 (implying system error) */
										/* 2. ret==IGNORE (ignore current feature) */
										if (ret < ILfs.FALSE) {
											return (ret);
										}
										/* Otherwise, IGNORE and continue. */
									}
								}

								/* Set up to resume scan. */
								/* Test to see if 3rd pair can slide into 2nd pair. */
								/* The values of the 2nd pair MUST be different. */
								/* If 3rd pair values are different ... */
								if (binarizedImageData[p1ptrIndex.get()] != binarizedImageData[p2ptrIndex.get()]) {
									/* Set next first pair to last of repeated */
									/* 2nd pairs, ie. back up one pair. */
									cx.set(cx.get() - 1);
								}

								/* Otherwise, 3rd pair can't be a 2nd pair, so */
								/* keep pointing to 3rd pair so that it is used */
								/* in the next first pair test. */
							} // Else, at end of current scan row.
						}

						/* Otherwise, 2nd pair failed, so keep pointing to it */
						/* so that it is used in the next first pair test. */
					} // Else, at end of current scan row.
				}
				/* Otherwise, 1st pair failed... */
				else {
					/* Bump forward to next pixel pair. */
					cx.set(cx.get() + 1);
				}
			} // While not at end of current scan row.
			/* Bump forward to next scan row. */
			cy.set(cy.get() + 1);
		} // While not out of scan rows.

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: scan4minutiae_horizontally_V2 - Scans an entire binary image #cat:
	 * horizontally, detecting potential minutiae points. #cat: Minutia detected via
	 * the horizontal scan process are #cat: by nature vertically oriented
	 * (orthogonal to the scan). Input: binarizedImageData - binary image data
	 * (0==while & 1==black) imageWidth - width (in pixels) of image imageHeight -
	 * height (in pixels) of image oDirectionMap - pixelized Direction Map
	 * oLowFlowMap - pixelized Low Ridge Flow Map oHighCurveMap - pixelized High
	 * Curvature Map lfsParams - parameters and thresholds for controlling LFS
	 * Output: minutiae - points to a list of detected minutia structures Return
	 * Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int scanForMinutiaeHorizontallyV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
			final int imageWidth, final int imageHeight, AtomicIntegerArray oDirectionMap,
			AtomicIntegerArray oLowFlowMap, AtomicIntegerArray oHighCurveMap, final LfsParams lfsParams) {
		int sx;
		int sy;
		int ex;
		int ey;
		AtomicInteger cx = new AtomicInteger(0);
		AtomicInteger cy = new AtomicInteger(0);
		int x2;
		AtomicInteger p1ptrIndex = new AtomicInteger(0);
		AtomicInteger p2ptrIndex = new AtomicInteger(0);
		AtomicIntegerArray oPossible = new AtomicIntegerArray(ILfs.NFEATURES);
		AtomicInteger oNoOfPoss = new AtomicInteger(0);
		int ret;

		/* Set scan region to entire image. */
		sx = 0;
		ex = imageWidth;
		sy = 0;
		ey = imageHeight;

		/* Start at first row in region. */
		cy.set(sy);
		/* While second scan row not outside the bottom of the scan region... */
		while ((cy.get() + 1) < ey) {
			/* Start at beginning of new scan row in region. */
			cx.set(sx);
			/* While not at end of region's current scan row. */
			while (cx.get() < ex) {
				/* Get pixel pair from current x position in current and next */
				/* scan rows. */
				p1ptrIndex.set(0 + (cy.get() * imageWidth) + cx.get());
				p2ptrIndex.set(0 + ((cy.get() + 1) * imageWidth) + cx.get());
				/* If scan pixel pair matches first pixel pair of */
				/* 1 or more features... */

				if (getMatchPattern().matchFirstPair(binarizedImageData[p1ptrIndex.get()],
						binarizedImageData[p2ptrIndex.get()], oPossible, oNoOfPoss) != ILfs.FALSE) {
					/* Bump forward to next scan pixel pair. */
					cx.set(cx.get() + 1);
					p1ptrIndex.set(p1ptrIndex.get() + 1);
					p2ptrIndex.set(p2ptrIndex.get() + 1);
					/* If not at end of region's current scan row... */
					if (cx.get() < ex) {
						/* If scan pixel pair matches second pixel pair of */
						/* 1 or more features... */
						if (getMatchPattern().matchSecondPair(binarizedImageData[p1ptrIndex.get()],
								binarizedImageData[p2ptrIndex.get()], oPossible, oNoOfPoss) != ILfs.FALSE) {
							/* Store current x location. */
							x2 = cx.get();
							/* Skip repeated pixel pairs. */
							getMatchPattern().skipRepeatedHorizontalPair(cx, ex, binarizedImageData, p1ptrIndex,
									p2ptrIndex, imageWidth, imageHeight);

							/* If not at end of region's current scan row... */
							if (cx.get() < ex) {
								/* If scan pixel pair matches third pixel pair of */
								/* a single feature... */
								if (getMatchPattern().matchThirdPair(binarizedImageData[p1ptrIndex.get()],
										binarizedImageData[p2ptrIndex.get()], oPossible, oNoOfPoss) != ILfs.FALSE) {
									/* Process detected minutia point. */
									if ((ret = processHorizontalScanMinutiaV2(oMinutiae, cx.get(), cy.get(), x2,
											oPossible.get(0), binarizedImageData, imageWidth, imageHeight,
											oDirectionMap, oLowFlowMap, oHighCurveMap, lfsParams)) != ILfs.FALSE) {
										/* Return code may be: */
										/* 1. ret< 0 (implying system error) */
										/* 2. ret==IGNORE (ignore current feature) */
										if (ret < ILfs.FALSE) {
											return (ret);
										}
										/* Otherwise, IGNORE and continue. */
									}
								}

								/* Set up to resume scan. */
								/* Test to see if 3rd pair can slide into 2nd pair. */
								/* The values of the 2nd pair MUST be different. */
								/* If 3rd pair values are different ... */
								if (binarizedImageData[p1ptrIndex.get()] != binarizedImageData[p2ptrIndex.get()]) {
									/* Set next first pair to last of repeated */
									/* 2nd pairs, ie. back up one pair. */
									cx.set(cx.get() - 1);
								}

								/* Otherwise, 3rd pair can't be a 2nd pair, so */
								/* keep pointing to 3rd pair so that it is used */
								/* in the next first pair test. */

							} // Else, at end of current scan row.
						}

						/* Otherwise, 2nd pair failed, so keep pointing to it */
						/* so that it is used in the next first pair test. */
					} // Else, at end of current scan row.
				}
				/* Otherwise, 1st pair failed... */
				else {
					/* Bump forward to next pixel pair. */
					cx.set(cx.get() + 1);
				}
			} // While not at end of current scan row.
			/* Bump forward to next scan row. */
			cy.set(cy.get() + 1);
		} // While not out of scan rows.

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: scan4minutiae_vertically - Scans a specified region of binary image
	 * data #cat: vertically, detecting potential minutiae points. #cat: Minutia
	 * detected via the vetical scan process are #cat: by nature horizontally
	 * oriented (orthogonal to the scan). #cat: The region actually scanned is
	 * slightly larger than that #cat: specified. This overlap attempts to minimize
	 * the number #cat: of minutiae missed at the region boundaries. #cat: HOWEVER,
	 * some minutiae will still be missed! Input: binarizedImageData - binary image
	 * data (0==while & 1==black) imageWidth - width (in pixels) of image
	 * imageHeight - height (in pixels) of image nInputBlockImageMapValue - IMAP
	 * value associated with this image region nNMapValue - NMAP value associated
	 * with this image region scanX - x-pixel coord of origin of region to be
	 * scanned scanY - y-pixel coord of origin of region to be scanned scanWidth -
	 * width (in pixels) of region to be scanned scanHeight - height (in pixels) of
	 * region to be scanned lfsParams - parameters and thresholds for controlling
	 * LFS Output: minutiae - points to a list of detected minutia structures Return
	 * Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int scanForMinutiaeVertically(AtomicReference<Minutiae> minutiae, int[] binarizedImageData,
			final int imageWidth, final int imageHeight, final int nInputBlockImageMapValue, final int nNMapValue,
			final int scanX, final int scanY, final int scanWidth, final int scanHeight, final LfsParams lfsParams) {
		int sx;
		int sy;
		int ex;
		int ey;
		AtomicInteger cx = new AtomicInteger(0);
		AtomicInteger cy = new AtomicInteger(0);
		int y2;
		AtomicInteger p1ptrIndex = new AtomicInteger(0);
		AtomicInteger p2ptrIndex = new AtomicInteger(0);
		AtomicIntegerArray possible = new AtomicIntegerArray(ILfs.NFEATURES);
		AtomicInteger nposs = new AtomicInteger(0);
		int ret;

		/* NOTE!!! Minutia that "straddle" region boundaries may be missed! */

		/* If possible, overlap scan region to the right by 1 pixel column. */
		sx = scanX;
		ex = Math.min(imageWidth, scanX + scanWidth + 1);

		/* If possible, overlap top and bottom of current scan region */
		/* by 2 pixel rows to help catch some minutia that straddle the */
		/* the scan region boundaries. */
		sy = Math.max(0, scanY - 2);
		ey = Math.min(imageHeight, scanY + scanHeight + 2);

		/* For now, we will not adjust for IMAP edge, as the binary image */
		/* was properly padded at its edges so as not to cause anomalies. */

		/* Start at first column in region. */
		cx.set(sx);
		/* While second scan column not outside the right of the region ... */
		while ((cx.get() + 1) < ex) {
			/* Start at beginning of new scan column in region. */
			cy.set(sy);
			/* While not at end of region's current scan column. */
			while (cy.get() < ey) {
				/* Get pixel pair from current y position in current and next */
				/* scan columns. */
				p1ptrIndex.set(0 + (cy.get() * imageWidth) + cx.get());
				p2ptrIndex.set(p1ptrIndex.get() + 1);

				/* If scan pixel pair matches first pixel pair of */
				/* 1 or more features... */
				if (getMatchPattern().matchFirstPair(binarizedImageData[p1ptrIndex.get()],
						binarizedImageData[p2ptrIndex.get()], possible, nposs) != ILfs.FALSE) {
					/* Bump forward to next scan pixel pair. */
					cy.set(cy.get() + 1);
					p1ptrIndex.set(p1ptrIndex.get() + imageWidth);
					p2ptrIndex.set(p2ptrIndex.get() + imageWidth);

					/* If not at end of region's current scan column... */
					if (cy.get() < ey) {
						/* If scan pixel pair matches second pixel pair of */
						/* 1 or more features... */
						if (getMatchPattern().matchSecondPair(binarizedImageData[p1ptrIndex.get()],
								binarizedImageData[p2ptrIndex.get()], possible, nposs) != ILfs.FALSE) {
							/* Store current y location. */
							y2 = cy.get();
							/* Skip repeated pixel pairs. */
							getMatchPattern().skipRepeatedVerticalPair(cy, ey, binarizedImageData, p1ptrIndex,
									p2ptrIndex, imageWidth, imageHeight);
							/* If not at end of region's current scan column... */
							if (cy.get() < ey) {
								/* If scan pixel pair matches third pixel pair of */
								/* a single feature... */
								if (getMatchPattern().matchThirdPair(binarizedImageData[p1ptrIndex.get()],
										binarizedImageData[p2ptrIndex.get()], possible, nposs) != ILfs.FALSE) {
									/* Process detected minutia point. */
									if ((ret = processVerticalScanMinutia(minutiae, cx.get(), cy.get(), y2,
											possible.get(0), binarizedImageData, imageWidth, imageHeight,
											nInputBlockImageMapValue, nNMapValue, lfsParams)) != ILfs.FALSE) {
										/* Return code may be: */
										/* 1. ret< 0 (implying system error) */
										/* 2. ret==IGNORE (ignore current feature) */
										if (ret < ILfs.FALSE) {
											return (ret);
										}
										/* Otherwise, IGNORE and continue. */
									}
								}

								/* Set up to resume scan. */
								/* Test to see if 3rd pair can slide into 2nd pair. */
								/* The values of the 2nd pair MUST be different. */
								/* If 3rd pair values are different ... */
								if (binarizedImageData[p1ptrIndex.get()] != binarizedImageData[p2ptrIndex.get()]) {
									/* Set next first pair to last of repeated */
									/* 2nd pairs, ie. back up one pair. */
									cy.set(cy.get() - 1);
								}
								/* Otherwise, 3rd pair can't be a 2nd pair, so */
								/* keep pointing to 3rd pair so that it is used */
								/* in the next first pair test. */
							} // Else, at end of current scan row.
						}
						/* Otherwise, 2nd pair failed, so keep pointing to it */
						/* so that it is used in the next first pair test. */
					} // Else, at end of current scan column.
				}
				/* Otherwise, 1st pair failed... */
				else {
					/* Bump forward to next pixel pair. */
					cy.set(cy.get() + 1);
				}
			} // While not at end of current scan column.
			/* Bump forward to next scan column. */
			cx.set(cx.get() - 1);
		} // While not out of scan columns.

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: rescanForMinutiaeHorizontally - Rescans portions of a block of binary
	 * #cat: image data horizontally for potential minutiae. The areas #cat:
	 * rescanned within the block are based on the current #cat: block's neighboring
	 * blocks' IMAP and NMAP values. Input: binarizedImageData - binary image data
	 * (0==while & 1==black) imageWidth - width (in pixels) of image imageHeight -
	 * height (in pixels) of image oInputBlockImageMap - matrix of ridge flow
	 * directions oNMap - IMAP augmented with blocks of HIGH-CURVATURE and blocks
	 * which have no neighboring valid directions. blockX - x-block coord to be
	 * rescanned blockY - y-block coord to be rescanned mapWidth - width (in blocks)
	 * of IMAP and NMAP matrices. mapHeight - height (in blocks) of IMAP and NMAP
	 * matrices. scanX - x-pixel coord of origin of region to be rescanned scanY -
	 * y-pixel coord of origin of region to be rescanned scanWidth - width (in
	 * pixels) of region to be rescanned scanHeight - height (in pixels) of region
	 * to be rescanned lfsParams - parameters and thresholds for controlling LFS
	 * Output: oMinutiae - points to a list of detected minutia structures Return
	 * Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int rescanForMinutiaeHorizontally(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
			final int imageWidth, final int imageHeight, AtomicIntegerArray oInputBlockImageMap,
			AtomicIntegerArray oNMap, final int blockX, final int blockY, final int mapWidth, final int mapHeight,
			final int scanX, final int scanY, final int scanWidth, final int scanHeight, final LfsParams lfsParams) {
		int blockIndex;
		int ret;

		/* Compute block index from block coordinates. */
		blockIndex = (blockY * mapWidth) + blockX;

		/* If high-curve block... */
		if (oNMap.get(blockIndex) == ILfs.HIGH_CURVATURE) {
			/* Rescan entire block in orthogonal direction. */
			if ((ret = scanForMinutiaeHorizontally(oMinutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap.get(blockIndex), oNMap.get(blockIndex), scanX, scanY, scanWidth, scanHeight,
					lfsParams)) != ILfs.FALSE) {
				/* Return code may be: */
				/* 1. ret<0 (implying system error) */
				return (ret);
			}
		}
		/* Otherwise, block is low-curvature. */
		else {
			/* 1. Rescan horizontally to the North. */
			if ((ret = rescanPartialHorizontally(ILfs.NORTH, oMinutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap, oNMap, blockX, blockY, mapWidth, mapHeight, scanX, scanY, scanWidth,
					scanHeight, lfsParams)) != ILfs.FALSE) {
				/* Return code may be: */
				/* 1. ret<0 (implying system error) */
				return (ret);
			}

			/* 2. Rescan horizontally to the East. */
			if ((ret = rescanPartialHorizontally(ILfs.EAST, oMinutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap, oNMap, blockX, blockY, mapWidth, mapHeight, scanX, scanY, scanWidth,
					scanHeight, lfsParams)) != ILfs.FALSE) {
				return (ret);
			}

			/* 3. Rescan horizontally to the South. */
			if ((ret = rescanPartialHorizontally(ILfs.SOUTH, oMinutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap, oNMap, blockX, blockY, mapWidth, mapHeight, scanX, scanY, scanWidth,
					scanHeight, lfsParams)) != ILfs.FALSE) {
				return (ret);
			}

			/* 4. Rescan horizontally to the West. */
			if ((ret = rescanPartialHorizontally(ILfs.WEST, oMinutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap, oNMap, blockX, blockY, mapWidth, mapHeight, scanX, scanY, scanWidth,
					scanHeight, lfsParams)) != ILfs.FALSE) {
				return (ret);
			}
		} // End low-curvature rescan.

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: scanForMinutiaeVerticallyV2 - Scans an entire binary image #cat:
	 * vertically, detecting potential minutiae points. #cat: Minutia detected via
	 * the vetical scan process are #cat: by nature horizontally oriented
	 * (orthogonal to the scan). Input: binarizedImageData - binary image data
	 * (0==while & 1==black) imageWidth - width (in pixels) of image imageHeight -
	 * height (in pixels) of image Maps -- contains all below information
	 * oDirectionMap - pixelized Direction Map oLowFlowMap - pixelized Low Ridge
	 * Flow Map oHighCurveMap - pixelized High Curvature Map lfsParams - parameters
	 * and thresholds for controlling LFS Output: oMinutiae - points to a list of
	 * detected minutia structures Return Code: Zero - successful completion
	 * Negative - system error
	 **************************************************************************/
	public int scanForMinutiaeVerticallyV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
			final int imageWidth, final int imageHeight, AtomicIntegerArray oDirectionMap,
			AtomicIntegerArray oLowFlowMap, AtomicIntegerArray oHighCurveMap, final LfsParams lfsParams) {
		int sx;
		int sy;
		int ex;
		int ey;
		AtomicInteger cx = new AtomicInteger(0);
		AtomicInteger cy = new AtomicInteger(0);
		AtomicInteger p1ptrIndex = new AtomicInteger(0);
		AtomicInteger p2ptrIndex = new AtomicInteger(0);
		AtomicIntegerArray pPossible = new AtomicIntegerArray(ILfs.NFEATURES);
		AtomicInteger oPoss = new AtomicInteger(0);
		int y2;
		int ret;

		/* Set scan region to entire image. */
		sx = 0;
		ex = imageWidth;
		sy = 0;
		ey = imageHeight;

		/* Start at first column in region. */
		cx.set(sx);
		/* While second scan column not outside the right of the region ... */
		while ((cx.get() + 1) < ex) {
			/* Start at beginning of new scan column in region. */
			cy.set(sy);
			/* While not at end of region's current scan column. */
			while (cy.get() < ey) {
				/* Get pixel pair from current y position in current and next */
				/* scan columns. */
				p1ptrIndex.set(0 + (cy.get() * imageWidth) + cx.get());
				p2ptrIndex.set(p1ptrIndex.get() + 1);

				/* If scan pixel pair matches first pixel pair of */
				/* 1 or more features... */
				if (getMatchPattern().matchFirstPair(binarizedImageData[p1ptrIndex.get()],
						binarizedImageData[p2ptrIndex.get()], pPossible, oPoss) != ILfs.FALSE) {
					/* Bump forward to next scan pixel pair. */
					cy.set(cy.get() + 1);
					p1ptrIndex.set(p1ptrIndex.get() + imageWidth);
					p2ptrIndex.set(p2ptrIndex.get() + imageWidth);
					/* If not at end of region's current scan column... */
					if (cy.get() < ey) {
						/* If scan pixel pair matches second pixel pair of */
						/* 1 or more features... */
						if (getMatchPattern().matchSecondPair(binarizedImageData[p1ptrIndex.get()],
								binarizedImageData[p2ptrIndex.get()], pPossible, oPoss) != ILfs.FALSE) {
							/* Store current y location. */
							y2 = cy.get();
							/* Skip repeated pixel pairs. */
							getMatchPattern().skipRepeatedVerticalPair(cy, ey, binarizedImageData, p1ptrIndex,
									p2ptrIndex, imageWidth, imageHeight);
							/* If not at end of region's current scan column... */
							if (cy.get() < ey) {
								/* If scan pixel pair matches third pixel pair of */
								/* a single feature... */
								if (getMatchPattern().matchThirdPair(binarizedImageData[p1ptrIndex.get()],
										binarizedImageData[p2ptrIndex.get()], pPossible, oPoss) != ILfs.FALSE) {
									/* Process detected minutia point. */
									if ((ret = processVerticalScanMinutiaV2(oMinutiae, cx.get(), cy.get(), y2,
											pPossible.get(0), binarizedImageData, imageWidth, imageHeight,
											oDirectionMap, oLowFlowMap, oHighCurveMap, lfsParams)) != ILfs.FALSE) {
										/* Return code may be: */
										/* 1. ret< 0 (implying system error) */
										/* 2. ret==IGNORE (ignore current feature) */
										if (ret < ILfs.FALSE) {
											return (ret);
										}
										/* Otherwise, IGNORE and continue. */
									}
								}

								/* Set up to resume scan. */
								/* Test to see if 3rd pair can slide into 2nd pair. */
								/* The values of the 2nd pair MUST be different. */
								/* If 3rd pair values are different ... */
								if (binarizedImageData[p1ptrIndex.get()] != binarizedImageData[p2ptrIndex.get()]) {
									/* Set next first pair to last of repeated */
									/* 2nd pairs, ie. back up one pair. */
									cy.set(cy.get() - 1);
								}
								/* Otherwise, 3rd pair can't be a 2nd pair, so */
								/* keep pointing to 3rd pair so that it is used */
								/* in the next first pair test. */
							} // Else, at end of current scan row.
						}
						/* Otherwise, 2nd pair failed, so keep pointing to it */
						/* so that it is used in the next first pair test. */
					} // Else, at end of current scan column.
				}
				/* Otherwise, 1st pair failed... */
				else {
					/* Bump forward to next pixel pair. */
					cy.set(cy.get() + 1);
				}
			} // While not at end of current scan column.
			/* Bump forward to next scan column. */
			cx.set(cx.get() + 1);
		} // While not out of scan columns.

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: rescanForMinutiaeVertically - Rescans portions of a block of binary
	 * #cat: image data vertically for potential minutiae. The areas #cat: rescanned
	 * within the block are based on the current #cat: block's neighboring blocks'
	 * IMAP and NMAP values. Input: binarizedImageData - binary image data (0==while
	 * & 1==black) imageWidth - width (in pixels) of image imageHeight - height (in
	 * pixels) of image oInputBlockImageMap - matrix of ridge flow directions oNMap
	 * - IMAP augmented with blocks of HIGH-CURVATURE and blocks which have no
	 * neighboring valid directions. blockX - x-block coord to be rescanned blockY -
	 * y-block coord to be rescanned mapWidth - width (in blocks) of IMAP and NMAP
	 * matrices. mapHeight - height (in blocks) of IMAP and NMAP matrices. scanX -
	 * x-pixel coord of origin of region to be rescanned scanY - y-pixel coord of
	 * origin of region to be rescanned scanWidth - width (in pixels) of region to
	 * be rescanned scanHeight - height (in pixels) of region to be rescanned
	 * lfsParams - parameters and thresholds for controlling LFS Output: minutiae -
	 * points to a list of detected minutia structures Return Code: Zero -
	 * successful completion Negative - system error
	 **************************************************************************/
	public int rescanForMinutiaeVertically(AtomicReference<Minutiae> minutiae, int[] binarizedImageData,
			final int imageWidth, final int imageHeight, AtomicIntegerArray oInputBlockImageMap,
			AtomicIntegerArray oNMap, final int blockX, final int blockY, final int mapWidth, final int mapHeight,
			final int scanX, final int scanY, final int scanWidth, final int scanHeight, final LfsParams lfsParams) {
		int blockIndex;
		int ret;

		/* Compute block index from block coordinates. */
		blockIndex = (blockY * mapWidth) + blockX;

		/* If high-curve block... */
		if (oNMap.get(blockIndex) == ILfs.HIGH_CURVATURE) {
			/* Rescan entire block in orthogonal direction. */
			if ((ret = scanForMinutiaeVertically(minutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap.get(blockIndex), oNMap.get(blockIndex), scanX, scanY, scanWidth, scanHeight,
					lfsParams)) != ILfs.FALSE) {
				/* Return code may be: */
				/* 1. ret<0 (implying system error) */
				return (ret);
			}
		}
		/* Otherwise, block is low-curvature. */
		else {
			/* 1. Rescan vertically to the North. */
			if ((ret = rescanPartialVertically(ILfs.NORTH, minutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap, oNMap, blockX, blockY, mapWidth, mapHeight, scanX, scanY, scanWidth,
					scanHeight, lfsParams)) != ILfs.FALSE) {
				/* Return code may be: */
				/* 1. ret<0 (implying system error) */
				return (ret);
			}

			/* 2. Rescan vertically to the East. */
			if ((ret = rescanPartialVertically(ILfs.EAST, minutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap, oNMap, blockX, blockY, mapWidth, mapHeight, scanX, scanY, scanWidth,
					scanHeight, lfsParams)) != ILfs.FALSE) {
				return (ret);
			}

			/* 3. Rescan vertically to the South. */
			if ((ret = rescanPartialVertically(ILfs.SOUTH, minutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap, oNMap, blockX, blockY, mapWidth, mapHeight, scanX, scanY, scanWidth,
					scanHeight, lfsParams)) != ILfs.FALSE) {
				return (ret);
			}

			/* 4. Rescan vertically to the West. */
			if ((ret = rescanPartialVertically(ILfs.WEST, minutiae, binarizedImageData, imageWidth, imageHeight,
					oInputBlockImageMap, oNMap, blockX, blockY, mapWidth, mapHeight, scanX, scanY, scanWidth,
					scanHeight, lfsParams)) != ILfs.FALSE) {
				return (ret);
			}
		} // End low-curvature rescan.

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: rescanPartialHorizontally - Rescans a portion of a block of binary
	 * #cat: image data horizontally based on the IMAP and NMAP values #cat: of a
	 * specified neighboring block. Input: nbrDir - specifies which block neighbor
	 * {NORTH, SOUTH, EAST, WEST} binarizedImageData - binary image data (0==while &
	 * 1==black) imageWidth - width (in pixels) of image imageHeight - height (in
	 * pixels) of image oInputBlockImageMap - matrix of ridge flow directions oNMap
	 * - IMAP augmented with blocks of HIGH-CURVATURE and blocks which have no
	 * neighboring valid directions. blockX - x-block coord to be rescanned blockY -
	 * y-block coord to be rescanned mapWidth - width (in blocks) of IMAP and NMAP
	 * matrices. mapHeight - height (in blocks) of IMAP and NMAP matrices. scanX -
	 * x-pixel coord of origin of image region scanY - y-pixel coord of origin of
	 * image region scanWidth - width (in pixels) of image region scanHeight -
	 * height (in pixels) of image region lfsParams - parameters and thresholds for
	 * controlling LFS Output: oMinutiae - points to a list of detected minutia
	 * structures Return Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int rescanPartialHorizontally(final int nbrDir, AtomicReference<Minutiae> oMinutiae,
			int[] binarizedImageData, final int imageWidth, final int imageHeight,
			AtomicIntegerArray oInputBlockImageMap, AtomicIntegerArray oNMap, final int blockX, final int blockY,
			final int mapWidth, final int mapHeight, final int scanX, final int scanY, final int scanWidth,
			final int scanHeight, final LfsParams lfsParams) {
		AtomicInteger oBlockIndex = new AtomicInteger(0);
		int blockIndex;
		int rescanDir;
		AtomicInteger rescanX = new AtomicInteger(0);
		AtomicInteger rescanY = new AtomicInteger(0);
		AtomicInteger rescanWidth = new AtomicInteger(0);
		AtomicInteger rescanHeight = new AtomicInteger(0);
		int ret;

		/* Neighbor will either be NORTH, SOUTH, EAST, OR WEST. */
		ret = getNbrBlockIndex(oBlockIndex, nbrDir, blockX, blockY, mapWidth, mapHeight);
		/* Will return: */
		/* 1. Neighbor index found == FOUND */
		/* 2. Neighbor not found == NOT_FOUND */
		/* 3. System error < 0 */

		/* If system error ... */
		if (ret < ILfs.FALSE) {
			/* Return the error code. */
			return (ret);
		}

		/* If neighbor not found ... */
		if (ret == ILfs.NOT_FOUND) {
			/* Nothing to do, so return normally. */
			return (ILfs.FALSE);
		}

		/* Otherwise, neighboring block found ... */

		/* If neighbor block is VALID... */
		if (oInputBlockImageMap.get(oBlockIndex.get()) != ILfs.INVALID_DIR) {
			/* Compute block index from current (not neighbor) block coordinates. */
			blockIndex = (blockY * mapWidth) + blockX;

			/* Select feature scan direction based on neighbor IMAP. */
			rescanDir = chooseScanDirection(oInputBlockImageMap.get(oBlockIndex.get()), lfsParams.getNumDirections());
			/* If new scan direction is HORIZONTAL... */
			if (rescanDir == ILfs.SCAN_HORIZONTAL) {
				/* Adjust scanX, scanY, scanWidth, scanHeight for rescan. */
				if ((ret = adjustHorizontalRescan(nbrDir, rescanX, rescanY, rescanWidth, rescanHeight, scanX, scanY,
						scanWidth, scanHeight, lfsParams.getBlockOffsetSize())) != ILfs.FALSE) {
					/* Return system error code. */
					return (ret);
				}
				/* Rescan specified region in block vertically. */
				/* Pass IMAP direction for the block, NOT its neighbor. */
				if ((ret = scanForMinutiaeHorizontally(oMinutiae, binarizedImageData, imageWidth, imageHeight,
						oInputBlockImageMap.get(blockIndex), oNMap.get(blockIndex), rescanX.get(), rescanY.get(),
						rescanWidth.get(), rescanHeight.get(), lfsParams)) != ILfs.FALSE) {
					/* Return code may be: */
					/* 1. ret<0 (implying system error) */
					return (ret);
				}
			} // Otherwise, block has already been scanned vertically.
		} // Otherwise, neighbor has INVALID IMAP, so ignore rescan.

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: rescanPartialVertically - Rescans a portion of a block of binary #cat:
	 * image data vertically based on the IMAP and NMAP values #cat: of a specified
	 * neighboring block. Input: nbrDir - specifies which block neighbor {NORTH,
	 * SOUTH, EAST, WEST} binarizedImageData - binary image data (0==while &
	 * 1==black) imageWidth - width (in pixels) of image imageHeight - height (in
	 * pixels) of image oInputBlockImageMap - matrix of ridge flow directions oNMap
	 * - IMAP augmented with blocks of HIGH-CURVATURE and blocks which have no
	 * neighboring valid directions. blockX - x-block coord to be rescanned blockY -
	 * y-block coord to be rescanned mapWidth - width (in blocks) of IMAP and NMAP
	 * matrices. mapHeight - height (in blocks) of IMAP and NMAP matrices. scanX -
	 * x-pixel coord of origin of image region scanY - y-pixel coord of origin of
	 * image region scanWidth - width (in pixels) of image region scanHeight -
	 * height (in pixels) of image region lfsParams - parameters and thresholds for
	 * controlling LFS Output: oMinutiae - points to a list of detected minutia
	 * structures Return Code: Zero - successful completion Negative - system error
	 **************************************************************************/
	public int rescanPartialVertically(final int nbrDir, AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
			final int imageWidth, final int imageHeight, AtomicIntegerArray oInputBlockImageMap,
			AtomicIntegerArray oNMap, final int blockX, final int blockY, final int mapWidth, final int mapHeight,
			final int scanX, final int scanY, final int scanWidth, final int scanHeight, final LfsParams lfsParams) {
		AtomicInteger oBlockIndex = new AtomicInteger(0);
		int blkIndex;
		int rescanDir;
		AtomicInteger rescanX = new AtomicInteger(0);
		AtomicInteger rescanY = new AtomicInteger(0);
		AtomicInteger rescanWidth = new AtomicInteger(0);
		AtomicInteger rescanHeight = new AtomicInteger(0);
		int ret;

		/* Neighbor will either be NORTH, SOUTH, EAST, OR WEST. */
		ret = getNbrBlockIndex(oBlockIndex, nbrDir, blockX, blockY, mapWidth, mapHeight);
		/* Will return: */
		/* 1. Neighbor index found == FOUND */
		/* 2. Neighbor not found == NOT_FOUND */
		/* 3. System error < 0 */

		/* If system error ... */
		if (ret < ILfs.FALSE) {
			/* Return the error code. */
			return (ret);
		}

		/* If neighbor not found ... */
		if (ret == ILfs.NOT_FOUND) {
			/* Nothing to do, so return normally. */
			return (ILfs.FALSE);
		}

		/* Otherwise, neighboring block found ... */
		/* If neighbor block is VALID... */
		if (oInputBlockImageMap.get(oBlockIndex.get()) != ILfs.INVALID_DIR) {
			/* Compute block index from current (not neighbor) block coordinates. */
			blkIndex = (blockY * mapWidth) + blockX;

			/* Select feature scan direction based on neighbor IMAP. */
			rescanDir = chooseScanDirection(oInputBlockImageMap.get(oBlockIndex.get()), lfsParams.getNumDirections());
			/* If new scan direction is VERTICAL... */
			if (rescanDir == ILfs.SCAN_VERTICAL) {
				/* Adjust scanX, scanY, scanWidth, scanHeight for rescan. */
				if ((ret = adjustVerticalRescan(nbrDir, rescanX, rescanY, rescanWidth, rescanHeight, scanX, scanY,
						scanWidth, scanHeight, lfsParams.getBlockOffsetSize())) != ILfs.FALSE) {
					/* Return system error code. */
					return (ret);
				}
				/* Rescan specified region in block vertically. */
				/* Pass IMAP direction for the block, NOT its neighbor. */
				if ((ret = scanForMinutiaeVertically(oMinutiae, binarizedImageData, imageWidth, imageHeight,
						oInputBlockImageMap.get(blkIndex), oNMap.get(blkIndex), rescanX.get(), rescanY.get(),
						rescanWidth.get(), rescanHeight.get(), lfsParams)) != ILfs.FALSE) {
					/* Return code may be: */
					/* 1. ret<0 (implying system error) */
					return (ret);
				}
			} // Otherwise, block has already been scanned horizontally.
		} // Otherwise, neighbor has INVALID IMAP, so ignore rescan.

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: getNbrBlockIndex - Determines the block index (if one exists) #cat: for
	 * a specified neighbor of a block in the image. Input: nbrDir - specifies which
	 * block neighbor {NORTH, SOUTH, EAST, WEST} blockX - x-block coord to find
	 * neighbor of blockY - y-block coord to find neighbor of mapWidth - width (in
	 * blocks) of IMAP and NMAP matrices. mapHeight - height (in blocks) of IMAP and
	 * NMAP matrices. Output: oBlockIndex - points to neighbor's block index Return
	 * Code: NOT_FOUND - neighbor index does not exist FOUND - neighbor index exists
	 * and returned Negative - system error
	 **************************************************************************/
	public int getNbrBlockIndex(AtomicInteger oBlockIndex, final int nbrDir, final int blockX, final int blockY,
			final int mapWidth, final int mapHeight) {
		int nx;
		int ny;
		int ni;

		switch (nbrDir) {
		case ILfs.NORTH:
			/* If neighbor doesn't exist above... */
			if ((ny = blockY - 1) < 0) {
				/* Done, so return normally. */
				return (ILfs.NOT_FOUND);
			}
			/* Get neighbor's block index. */
			ni = (ny * mapWidth) + blockX;
			break;
		case ILfs.EAST:
			/* If neighbor doesn't exist to the right... */
			if ((nx = blockX + 1) >= mapWidth) {
				/* Done, so return normally. */
				return (ILfs.NOT_FOUND);
			}
			/* Get neighbor's block index. */
			ni = (blockY * mapWidth) + nx;
			break;
		case ILfs.SOUTH:
			/* If neighbor doesn't exist below... */
			if ((ny = blockY + 1) >= mapHeight) {
				/* Return normally. */
				return (ILfs.NOT_FOUND);
			}
			/* Get neighbor's block index. */
			ni = (ny * mapWidth) + blockX;
			break;
		case ILfs.WEST:
			/* If neighbor doesn't exist to the left... */
			if ((nx = blockX - 1) < 0) {
				/* Return normally. */
				return (ILfs.NOT_FOUND);
			}
			/* Get neighbor's block index. */
			ni = (blockY * mapWidth) + nx;
			break;
		default:
			logger.error("ERROR : getNbrBlockIndex : illegal neighbor direction");
			return (ILfs.ERROR_CODE_200);
		}

		/* Assign output pointer. */
		oBlockIndex.set(ni);

		/* Return neighbor FOUND. */
		return (ILfs.FOUND);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: adjustHorizontalRescan - Determines the portion of an image block to
	 * #cat: be rescanned horizontally based on a specified neighbor. Input: nbrDir
	 * - specifies which block neighbor {NORTH, SOUTH, EAST, WEST} scanX - x-pixel
	 * coord of origin of image region scanY - y-pixel coord of origin of image
	 * region scanWidth - width (in pixels) of image region scanHeight - height (in
	 * pixels) of image region blocksize - dimension of image blocks (in pixels)
	 * Output: rescanX - x-pixel coord of origin of region to be rescanned rescanY -
	 * y-pixel coord of origin of region to be rescanned rescanWidth - width (in
	 * pixels) of region to be rescanned rescanHeight - height (in pixels) of region
	 * to be rescanned Return Code: Zero - successful completion Negative - system
	 * error
	 **************************************************************************/
	public int adjustHorizontalRescan(final int nbrDir, AtomicInteger rescanX, AtomicInteger rescanY,
			AtomicInteger rescanWidth, AtomicInteger rescanHeight, final int scanX, final int scanY,
			final int scanWidth, final int scanHeight, final int blocksize) {
		int halfBlocksize;
		int qtrBlocksize;

		/* Compute half of blocksize. */
		halfBlocksize = blocksize >> 1;
		/* Compute quarter of blocksize. */
		qtrBlocksize = blocksize >> 2;

		/* Neighbor will either be NORTH, SOUTH, EAST, OR WEST. */
		switch (nbrDir) {
		case ILfs.NORTH:
			/*
			 *************************
			 * RESCAN NORTH * AREA *
			 *************************
			 * | | | | | | | | | | | | -------------------------
			 */
			/* Rescan origin stays the same. */
			rescanX.set(scanX);
			rescanY.set(scanY);
			/* Rescan width stays the same. */
			rescanWidth.set(scanWidth);
			/* Rescan height is reduced to "qtrBlocksize" */
			/* if scanHeight is larger. */
			rescanHeight.set(Math.min(qtrBlocksize, scanHeight));
			break;
		case ILfs.EAST:
			/*
			 * ------------************* | * * | * * | * E R * | * A E * | * S S * | * T C *
			 * | * A * | * N * | * * | * * ------------*************
			 */
			/* Rescan x-orign is set to halfBlocksize from right edge of */
			/* block if scan width is larger. */
			rescanX.set(Math.max(scanX + scanWidth - halfBlocksize, scanX));
			/* Rescan y-origin stays the same. */
			rescanY.set(scanY);
			/* Rescan width is reduced to "halfBlocksize" */
			/* if scan width is larger. */
			rescanWidth.set(Math.min(halfBlocksize, scanWidth));
			/* Rescan height stays the same. */
			rescanHeight.set(scanHeight);
			break;
		case ILfs.SOUTH:
			/*
			 * ------------------------- | | | | | | | | | | | |
			 *************************
			 * RESCAN SOUTH * AREA *
			 *************************
			 */
			/* Rescan x-origin stays the same. */
			rescanX.set(scanX);
			/* Rescan y-orign is set to qtrBlocksize from bottom edge of */
			/* block if scan height is larger. */
			rescanY.set(Math.max(scanY + scanHeight - qtrBlocksize, scanY));
			/* Rescan width stays the same. */
			rescanWidth.set(scanWidth);
			/* Rescan height is reduced to "qtrBlocksize" */
			/* if scan height is larger. */
			rescanHeight.set(Math.min(qtrBlocksize, scanHeight));
			break;
		case ILfs.WEST:
			/*
			 ************* ------------ * | * | W R * | E E * | S S * | T C * | A * | N * | * | * |
			 ************* ------------
			 */
			/* Rescan origin stays the same. */
			rescanX.set(scanX);
			rescanY.set(scanY);
			/* Rescan width is reduced to "halfBlocksize" */
			/* if scan width is larger. */
			rescanWidth.set(Math.min(halfBlocksize, scanWidth));
			/* Rescan height stays the same. */
			rescanHeight.set(scanHeight);
			break;
		default:
			logger.error("ERROR : adjustHorizontalRescan : illegal neighbor direction");
			return (ILfs.ERROR_CODE_210);
		}

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: adjustVerticalRescan - Determines the portion of an image block to
	 * #cat: be rescanned vertically based on a specified neighbor. Input: nbrDir -
	 * specifies which block neighbor {NORTH, SOUTH, EAST, WEST} scanX - x-pixel
	 * coord of origin of image region scanY - y-pixel coord of origin of image
	 * region scanWidth - width (in pixels) of image region scanHeight - height (in
	 * pixels) of image region blocksize - dimension of image blocks (in pixels)
	 * Output: rescanX - x-pixel coord of origin of region to be rescanned rescanY -
	 * y-pixel coord of origin of region to be rescanned rescanWidth - width (in
	 * pixels) of region to be rescanned rescanHeight - height (in pixels) of region
	 * to be rescanned Return Code: Zero - successful completion Negative - system
	 * error
	 **************************************************************************/
	public int adjustVerticalRescan(final int nbrDir, AtomicInteger rescanX, AtomicInteger rescanY,
			AtomicInteger rescanWidth, AtomicInteger rescanHeight, final int scanX, final int scanY,
			final int scanWidth, final int scanHeight, final int blocksize) {
		int halfBlocksize;
		int qtrBlocksize;

		/* Compute half of blocksize. */
		halfBlocksize = blocksize >> 1;
		/* Compute quarter of blocksize. */
		qtrBlocksize = blocksize >> 2;

		/* Neighbor will either be NORTH, SOUTH, EAST, OR WEST. */
		switch (nbrDir) {
		case ILfs.NORTH:
			/*
			 *************************
			 * * RESCAN NORTH * AREA * *
			 *************************
			 * | | | | | | | | | | -------------------------
			 */
			/* Rescan origin stays the same. */
			rescanX.set(scanX);
			rescanY.set(scanY);
			/* Rescan width stays the same. */
			rescanWidth.set(scanWidth);
			/* Rescan height is reduced to "halfBlocksize" */
			/* if scanHeight is larger. */
			rescanHeight.set(Math.min(halfBlocksize, scanHeight));
			break;
		case ILfs.EAST:
			/*
			 * ------------------******* | * * | * * | * E R * | * A E * | * S S * | * T C *
			 * | * A * | * N * | * * | * * ------------------*******
			 */
			/* Rescan x-orign is set to qtrBlocksize from right edge of */
			/* block if scan width is larger. */
			rescanX.set(Math.max(scanX + scanWidth - qtrBlocksize, scanX));
			/* Rescan y-origin stays the same. */
			rescanY.set(scanY);
			/* Rescan width is reduced to "qtrBlocksize" */
			/* if scan width is larger. */
			rescanWidth.set(Math.min(qtrBlocksize, scanWidth));
			/* Rescan height stays the same. */
			rescanHeight.set(scanHeight);
			break;
		case ILfs.SOUTH:
			/*
			 * ------------------------- | | | | | | | | | |
			 *************************
			 * * RESCAN SOUTH * AREA * *
			 *************************
			 */
			/* Rescan x-origin stays the same. */
			rescanX.set(scanX);
			/* Rescan y-orign is set to halfBlocksize from bottom edge of */
			/* block if scan height is larger. */
			rescanY.set(Math.max(scanY + scanHeight - halfBlocksize, scanY));
			/* Rescan width stays the same. */
			rescanWidth.set(scanWidth);
			/* Rescan height is reduced to "halfBlocksize" */
			/* if scan height is larger. */
			rescanHeight.set(Math.min(halfBlocksize, scanHeight));
			break;
		case ILfs.WEST:
			/*
			 ******* ------------------ * | * | W R * | E E * | S S * | T C * | A * | N * | * | *
			 * | ------------------
			 */
			/* Rescan origin stays the same. */
			rescanX.set(scanX);
			rescanY.set(scanY);
			/* Rescan width is reduced to "qtrBlocksize" */
			/* if scan width is larger. */
			rescanWidth.set(Math.min(qtrBlocksize, scanWidth));
			/* Rescan height stays the same. */
			rescanHeight.set(scanHeight);
			break;
		default:
			logger.error("ERROR : adjustVerticalRescan : illegal neighbor direction");
			return (ILfs.ERROR_CODE_220);
		}

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: processHorizontalScanMinutia - Takes a minutia point that was #cat:
	 * detected via the horizontal scan process and #cat: adjusts its location (if
	 * necessary), determines its #cat: direction, and (if it is not already in the
	 * minutiae #cat: list) adds it to the list. These minutia are by nature #cat:
	 * vertical in orientation (orthogonal to the scan). Input: cx - x-pixel coord
	 * where 3rd pattern pair of mintuia was detected cy - y-pixel coord where 3rd
	 * pattern pair of mintuia was detected y2 - y-pixel coord where 2nd pattern
	 * pair of mintuia was detected featureId - type of minutia (ex. index into
	 * feature_patterns[] list) binarizedImageData - binary image data (0==while &
	 * 1==black) imageWidth - width (in pixels) of image imageHeight - height (in
	 * pixels) of image nInputBlockImageMapValue - IMAP value associated with this
	 * image region nNMapValue - NMAP value associated with this image region
	 * lfsParams - parameters and thresholds for controlling LFS Output: oMinutiae -
	 * points to a list of detected minutia structures Return Code: Zero -
	 * successful completion IGNORE - minutia is to be ignored Negative - system
	 * error
	 **************************************************************************/
	public int processHorizontalScanMinutia(AtomicReference<Minutiae> oMinutiae, final int cx, final int cy,
			final int x2, final int featureId, int[] binarizedImageData, final int imageWidth, final int imageHeight,
			final int nInputBlockImageMapValue, final int nNMapValue, final LfsParams lfsParams) {
		Minutia minutia;
		AtomicInteger xLoc = new AtomicInteger(0);
		AtomicInteger yLoc = new AtomicInteger(0);
		AtomicInteger xEdge = new AtomicInteger(0);
		AtomicInteger yEdge = new AtomicInteger(0);
		AtomicInteger iDir = new AtomicInteger(0);
		int ret;

		/* Set x location of minutia point to be half way between */
		/* first position of second feature pair and position of */
		/* third feature pair. */
		xLoc.set((cx + x2) >> 1);

		/* Set same x location to neighboring edge pixel. */
		xEdge.set(xLoc.get());

		/* Feature location should always point to either ending */
		/* of ridge or (for bifurcations) ending of valley. */
		/* So, if detected feature is APPEARING... */
		if (getGlobals().getFeaturePatterns()[featureId].getAppearing() >= ILfs.APPEARING) {
			/* Set y location to second scan row. */
			yLoc.set(cy + 1);
			/* Set y location of neighboring edge pixel to the first scan row. */
			yEdge.set(cy);
		}
		/* Otherwise, feature is DISAPPEARING... */
		else {
			/* Set y location to first scan row. */
			yLoc.set(cy);
			/* Set y location of neighboring edge pixel to the second scan row. */
			yEdge.set(cy + 1);
		}

		/* If current minutia is in a high-curvature block... */
		if (nNMapValue == ILfs.HIGH_CURVATURE) {
			/* Adjust location and direction locally. */
			if ((ret = adjustHighCurvatureMinutia(iDir, xLoc, yLoc, xEdge, yEdge, xLoc.get(), yLoc.get(), xEdge.get(),
					yEdge.get(), binarizedImageData, imageWidth, imageHeight, oMinutiae, lfsParams)) != ILfs.FALSE) {
				/* Could be a system error or IGNORE minutia. */
				return ret;
			}
			/* Otherwise, we have our high-curvature minutia attributes. */
		}
		/* Otherwise, minutia is in fairly low-curvature block... */
		else {
			/* Get minutia direction based on current IMAP value. */
			iDir.set(getLowCurvatureDirection(ILfs.SCAN_HORIZONTAL,
					getGlobals().getFeaturePatterns()[featureId].getAppearing(), nInputBlockImageMapValue,
					lfsParams.getNumDirections()));
		}

		/* Create a minutia object based on derived attributes. */
		minutia = createMinutia(xLoc.get(), yLoc.get(), xEdge.get(), yEdge.get(), iDir.get(), ILfs.DEFAULT_RELIABILITY,
				getGlobals().getFeaturePatterns()[featureId].getType(),
				getGlobals().getFeaturePatterns()[featureId].getAppearing(), featureId);

		/* Update the minutiae list with potential new minutia. */
		ret = updateMinutiae(oMinutiae, minutia, binarizedImageData, imageWidth, imageHeight, lfsParams);
		/* If minuitia IGNORED and not added to the minutia list ... */
		if (ret == ILfs.IGNORE) {
			/* Deallocate the minutia. */
			freeMinutia(minutia);
		}

		/* Otherwise, return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: processHorizontalScanMinutiaV2 - Takes a minutia point that was #cat:
	 * detected via the horizontal scan process and #cat: adjusts its location (if
	 * necessary), determines its #cat: direction, and (if it is not already in the
	 * minutiae #cat: list) adds it to the list. These minutia are by nature #cat:
	 * vertical in orientation (orthogonal to the scan). Input: cx - x-pixel coord
	 * where 3rd pattern pair of mintuia was detected cy - y-pixel coord where 3rd
	 * pattern pair of mintuia was detected y2 - y-pixel coord where 2nd pattern
	 * pair of mintuia was detected featureId - type of minutia (ex. index into
	 * feature_patterns[] list) binarizedImageData - binary image data (0==while &
	 * 1==black) imageWidth - width (in pixels) of image imageHeight - height (in
	 * pixels) of image Maps - map oDirectionMap - pixelized Direction Map
	 * oLowFlowMap - pixelized Low Ridge Flow Map oHighCurveMap - pixelized High
	 * Curvature Map lfsParams - parameters and thresholds for controlling LFS
	 * Output: minutiae - points to a list of detected minutia structures Return
	 * Code: Zero - successful completion IGNORE - minutia is to be ignored Negative
	 * - system error
	 **************************************************************************/
	public int processHorizontalScanMinutiaV2(AtomicReference<Minutiae> oMinutiae, final int cx, final int cy,
			final int x2, final int featureId, int[] binarizedImageData, final int imageWidth, final int imageHeight,
			AtomicIntegerArray oDirectionMap, AtomicIntegerArray oLowFlowMap, AtomicIntegerArray oHighCurveMap,
			final LfsParams lfsParams) {
		Minutia minutia = null;
		AtomicInteger xLoc = new AtomicInteger(0);
		AtomicInteger yLoc = new AtomicInteger(0);
		AtomicInteger xEdge = new AtomicInteger(0);
		AtomicInteger yEdge = new AtomicInteger(0);
		AtomicInteger iDir = new AtomicInteger(0);

		int ret;
		int directionMapValue;
		int lowFlowMapValue;
		int highCurveMapValue;
		double reliability;

		/* Set x location of minutia point to be half way between */
		/* first position of second feature pair and position of */
		/* third feature pair. */
		xLoc.set((cx + x2) >> 1);

		/* Set same x location to neighboring edge pixel. */
		xEdge.set(xLoc.get());

		/* Feature location should always point to either ending */
		/* of ridge or (for bifurcations) ending of valley. */
		/* So, if detected feature is APPEARING... */
		if (getGlobals().getFeaturePatterns()[featureId].getAppearing() >= ILfs.APPEARING) {
			/* Set y location to second scan row. */
			yLoc.set(cy + 1);
			/* Set y location of neighboring edge pixel to the first scan row. */
			yEdge.set(cy);
		}
		/* Otherwise, feature is DISAPPEARING... */
		else {
			/* Set y location to first scan row. */
			yLoc.set(cy);
			/* Set y location of neighboring edge pixel to the second scan row. */
			yEdge.set(cy + 1);
		}

		directionMapValue = oDirectionMap.get(0 + (yLoc.get() * imageWidth) + xLoc.get());
		lowFlowMapValue = oLowFlowMap.get(0 + (yLoc.get() * imageWidth) + xLoc.get());
		highCurveMapValue = oHighCurveMap.get(0 + +(yLoc.get() * imageWidth) + xLoc.get());

		/* If the minutia point is in a block with INVALID direction ... */
		if (directionMapValue == ILfs.INVALID_DIR) {
			/* Then, IGNORE the point. */
			return (ILfs.IGNORE);
		}

		/* If current minutia is in a HIGH CURVATURE block ... */
		if (highCurveMapValue == ILfs.TRUE) {
			/* Adjust location and direction locally. */
			ret = adjustHighCurvatureMinutiaV2(iDir, xLoc, yLoc, xEdge, yEdge, xLoc.get(), yLoc.get(), xEdge.get(),
					yEdge.get(), binarizedImageData, imageWidth, imageHeight, oLowFlowMap, oMinutiae, lfsParams);
			if (ret != ILfs.FALSE) {
				/* Could be a system error or IGNORE minutia. */
				return (ret);
			}
			/* Otherwise, we have our high-curvature minutia attributes. */
		}
		/* Otherwise, minutia is in fairly low-curvature block... */
		else {
			/* Get minutia direction based on current block's direction. */
			iDir.set(getLowCurvatureDirection(ILfs.SCAN_HORIZONTAL,
					getGlobals().getFeaturePatterns()[featureId].getAppearing(), directionMapValue,
					lfsParams.getNumDirections()));
		}

		/* If current minutia is in a LOW RIDGE FLOW block ... */
		if (lowFlowMapValue == ILfs.TRUE) {
			reliability = ILfs.MEDIUM_RELIABILITY;
		} else {
			/* Otherwise, minutia is in a block with reliable direction and */
			/* binarization. */
			reliability = ILfs.HIGH_RELIABILITY;
		}

		/* Create a minutia object based on derived attributes. */
		minutia = createMinutia(xLoc.get(), yLoc.get(), xEdge.get(), yEdge.get(), iDir.get(), reliability,
				getGlobals().getFeaturePatterns()[featureId].getType(),
				getGlobals().getFeaturePatterns()[featureId].getAppearing(), featureId);

		/* Update the minutiae list with potential new minutia. */
		ret = updateMinutiaeV2(oMinutiae, minutia, ILfs.SCAN_HORIZONTAL, directionMapValue, binarizedImageData,
				imageWidth, imageHeight, lfsParams);

		/* If minuitia IGNORED and not added to the minutia list ... */
		if (ret == ILfs.IGNORE) {
			/* Deallocate the minutia. */
			freeMinutia(minutia);
		}

		/* Otherwise, return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: processVerticalScanMinutia - Takes a minutia point that was #cat:
	 * detected in via the vertical scan process and #cat: adjusts its location (if
	 * necessary), determines its #cat: direction, and (if it is not already in the
	 * minutiae #cat: list) adds it to the list. These minutia are by nature #cat:
	 * horizontal in orientation (orthogonal to the scan). Input: cx - x-pixel coord
	 * where 3rd pattern pair of mintuia was detected cy - y-pixel coord where 3rd
	 * pattern pair of mintuia was detected x2 - x-pixel coord where 2nd pattern
	 * pair of mintuia was detected featureId - type of minutia (ex. index into
	 * feature_patterns[] list) binarizedImageData - binary image data (0==while &
	 * 1==black) imageWidth - width (in pixels) of image imageHeight - height (in
	 * pixels) of image nInputBlockImageMapValue - IMAP value associated with this
	 * image region nNMapValue - NMAP value associated with this image region
	 * lfsParams - parameters and thresholds for controlling LFS Output: oMinutiae -
	 * points to a list of detected minutia structures Return Code: Zero -
	 * successful completion IGNORE - minutia is to be ignored Negative - system
	 * error
	 **************************************************************************/
	public int processVerticalScanMinutia(AtomicReference<Minutiae> oMinutiae, final int cx, final int cy, final int y2,
			final int featureId, int[] binarizedImageData, final int imageWidth, final int imageHeight,
			final int nInputBlockImageMapValue, final int nNMapValue, final LfsParams lfsParams) {
		Minutia minutia;
		AtomicInteger xLoc = new AtomicInteger(0);
		AtomicInteger yLoc = new AtomicInteger(0);
		AtomicInteger xEdge = new AtomicInteger(0);
		AtomicInteger yEdge = new AtomicInteger(0);
		AtomicInteger iDir = new AtomicInteger(0);
		int ret;

		/* Feature location should always point to either ending */
		/* of ridge or (for bifurcations) ending of valley. */
		/* So, if detected feature is APPEARING... */
		if (getGlobals().getFeaturePatterns()[featureId].getAppearing() >= ILfs.APPEARING) {
			/* Set x location to second scan column. */
			xLoc.set(cx + 1);
			/* Set x location of neighboring edge pixel to the first scan column. */
			xEdge.set(cx);
		}
		/* Otherwise, feature is DISAPPEARING... */
		else {
			/* Set x location to first scan column. */
			xLoc.set(cx);
			/* Set x location of neighboring edge pixel to the second scan column. */
			xEdge.set(cx + 1);
		}

		/* Set y location of minutia point to be half way between */
		/* first position of second feature pair and position of */
		/* third feature pair. */
		yLoc.set((cy + y2) >> 1);
		/* Set same y location to neighboring edge pixel. */
		yEdge = yLoc;

		/* If current minutia is in a high-curvature block... */
		if (nNMapValue == ILfs.HIGH_CURVATURE) {
			/* Adjust location and direction locally. */
			if ((ret = adjustHighCurvatureMinutia(iDir, xLoc, yLoc, xEdge, yEdge, xLoc.get(), yLoc.get(), xEdge.get(),
					yEdge.get(), binarizedImageData, imageWidth, imageHeight, oMinutiae, lfsParams)) != ILfs.FALSE) {
				/* Could be a system error or IGNORE minutia. */
				return (ret);
			}
			/* Otherwise, we have our high-curvature minutia attributes. */
		}
		/* Otherwise, minutia is in fairly low-curvature block... */
		else {
			/* Get minutia direction based on current IMAP value. */
			iDir.set(getLowCurvatureDirection(ILfs.SCAN_VERTICAL,
					getGlobals().getFeaturePatterns()[featureId].getAppearing(), nInputBlockImageMapValue,
					lfsParams.getNumDirections()));
		}

		/* Create a minutia object based on derived attributes. */
		minutia = createMinutia(xLoc.get(), yLoc.get(), xEdge.get(), yEdge.get(), iDir.get(), ILfs.DEFAULT_RELIABILITY,
				getGlobals().getFeaturePatterns()[featureId].getType(),
				getGlobals().getFeaturePatterns()[featureId].getAppearing(), featureId);

		/* Update the minutiae list with potential new minutia. */
		ret = updateMinutiae(oMinutiae, minutia, binarizedImageData, imageWidth, imageHeight, lfsParams);
		/* If minuitia IGNORED and not added to the minutia list ... */
		if (ret == ILfs.IGNORE) {
			/* Deallocate the minutia. */
			freeMinutia(minutia);
		}

		/* Otherwise, return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: processVerticalScanMinutiaV2 - Takes a minutia point that was #cat:
	 * detected in via the vertical scan process and #cat: adjusts its location (if
	 * necessary), determines its #cat: direction, and (if it is not already in the
	 * minutiae #cat: list) adds it to the list. These minutia are by nature #cat:
	 * horizontal in orientation (orthogonal to the scan). Input: cx - x-pixel coord
	 * where 3rd pattern pair of mintuia was detected cy - y-pixel coord where 3rd
	 * pattern pair of mintuia was detected x2 - x-pixel coord where 2nd pattern
	 * pair of mintuia was detected featureId - type of minutia (ex. index into
	 * feature_patterns[] list) binarizedImageData - binary image data (0==while &
	 * 1==black) imageWidth - width (in pixels) of image imageHeight - height (in
	 * pixels) of image oDirectionMap - pixelized Direction Map oLowFlowMap -
	 * pixelized Low Ridge Flow Map oHighCurveMap - pixelized High Curvature Map
	 * lfsParams - parameters and thresholds for controlling LFS Output: oMinutiae -
	 * points to a list of detected minutia structures Return Code: Zero -
	 * successful completion IGNORE - minutia is to be ignored Negative - system
	 * error
	 **************************************************************************/
	public int processVerticalScanMinutiaV2(AtomicReference<Minutiae> oMinutiae, final int cx, final int cy,
			final int y2, final int featureId, int[] binarizedImageData, final int imageWidth, final int imageHeight,
			AtomicIntegerArray oDirectionMap, AtomicIntegerArray oLowFlowMap, AtomicIntegerArray oHighCurveMap,
			final LfsParams lfsParams) {
		Minutia minutia = null;
		AtomicInteger xLoc = new AtomicInteger(0);
		AtomicInteger yLoc = new AtomicInteger(0);
		AtomicInteger xEdge = new AtomicInteger(0);
		AtomicInteger yEdge = new AtomicInteger(0);
		AtomicInteger iDir = new AtomicInteger(0);

		int ret;
		int directionMapValue;
		int lowFlowMapValue;
		int highCurveMapValue;
		double reliability;

		/* Feature location should always point to either ending */
		/* of ridge or (for bifurcations) ending of valley. */
		/* So, if detected feature is APPEARING... */
		if (getGlobals().getFeaturePatterns()[featureId].getAppearing() >= ILfs.APPEARING) {
			/* Set x location to second scan column. */
			xLoc.set(cx + 1);
			/* Set x location of neighboring edge pixel to the first scan column. */
			xEdge.set(cx);
		}
		/* Otherwise, feature is DISAPPEARING... */
		else {
			/* Set x location to first scan column. */
			xLoc.set(cx);
			/* Set x location of neighboring edge pixel to the second scan column. */
			xEdge.set(cx + 1);
		}

		/* Set y location of minutia point to be half way between */
		/* first position of second feature pair and position of */
		/* third feature pair. */
		yLoc.set((cy + y2) >> 1);
		/* Set same y location to neighboring edge pixel. */
		yEdge.set(yLoc.get());

		directionMapValue = oDirectionMap.get(0 + (yLoc.get() * imageWidth) + xLoc.get());
		lowFlowMapValue = oLowFlowMap.get(0 + (yLoc.get() * imageWidth) + xLoc.get());
		highCurveMapValue = oHighCurveMap.get(0 + (yLoc.get() * imageWidth) + xLoc.get());

		/* If the minutia point is in a block with INVALID direction ... */
		if (directionMapValue == ILfs.INVALID_DIR) {
			/* Then, IGNORE the point. */
			return (ILfs.IGNORE);
		}

		/* If current minutia is in a HIGH CURVATURE block... */
		if (highCurveMapValue == ILfs.TRUE) {
			/* Adjust location and direction locally. */
			ret = adjustHighCurvatureMinutiaV2(iDir, xLoc, yLoc, xEdge, yEdge, xLoc.get(), yLoc.get(), xEdge.get(),
					yEdge.get(), binarizedImageData, imageWidth, imageHeight, oLowFlowMap, oMinutiae, lfsParams);
			if (ret != ILfs.FALSE) {
				/* Could be a system error or IGNORE minutia. */
				return (ret);
			}
			/* Otherwise, we have our high-curvature minutia attributes. */
		}
		/* Otherwise, minutia is in fairly low-curvature block... */
		else {
			/* Get minutia direction based on current block's direction. */
			iDir.set(getLowCurvatureDirection(ILfs.SCAN_VERTICAL,
					getGlobals().getFeaturePatterns()[featureId].getAppearing(), directionMapValue,
					lfsParams.getNumDirections()));
		}

		/* If current minutia is in a LOW RIDGE FLOW block ... */
		if (lowFlowMapValue == ILfs.TRUE) {
			reliability = ILfs.MEDIUM_RELIABILITY;
		} else {
			/* Otherwise, minutia is in a block with reliable direction and */
			/* binarization. */
			reliability = ILfs.HIGH_RELIABILITY;
		}

		/* Create a minutia object based on derived attributes. */
		minutia = createMinutia(xLoc.get(), yLoc.get(), xEdge.get(), yEdge.get(), iDir.get(), reliability,
				getGlobals().getFeaturePatterns()[featureId].getType(),
				getGlobals().getFeaturePatterns()[featureId].getAppearing(), featureId);

		/* Update the minutiae list with potential new minutia. */
		ret = updateMinutiaeV2(oMinutiae, minutia, ILfs.SCAN_VERTICAL, directionMapValue, binarizedImageData,
				imageWidth, imageHeight, lfsParams);
		/* If minuitia IGNORED and not added to the minutia list ... */
		if (ret == ILfs.IGNORE) {
			/* Deallocate the minutia. */
			freeMinutia(minutia);
		}

		/* Otherwise, return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: adjustHighCurvatureMinutia - Takes an initial minutia point detected
	 * #cat: in a high-curvature area and adjusts its location and #cat: direction.
	 * First, it walks and extracts the contour #cat: of the detected feature
	 * looking for and processing any loop #cat: discovered along the way. Once the
	 * contour is extracted, #cat: the point of highest-curvature is determined and
	 * used to #cat: adjust the location of the minutia point. The angle of #cat:
	 * the line perpendicular to the tangent on the high-curvature #cat: contour at
	 * the minutia point is used as the mintutia's #cat: direction. Input: xLoc -
	 * starting x-pixel coord of feature (interior to feature) yLoc - starting
	 * y-pixel coord of feature (interior to feature) xEdge - x-pixel coord of
	 * corresponding edge pixel (exterior to feature) yEdge - y-pixel coord of
	 * corresponding edge pixel (exterior to feature) binarizedImageData - binary
	 * image data (0==while & 1==black) imageWidth - width (in pixels) of image
	 * imageHeight - height (in pixels) of image lfsParams - parameters and
	 * thresholds for controlling LFS Output: oIDir - direction of adjusted minutia
	 * point oXLoc - adjusted x-pixel coord of feature oYLoc - adjusted y-pixel
	 * coord of feature oXEdge - adjusted x-pixel coord of corresponding edge pixel
	 * oYEdge - adjusted y-pixel coord of corresponding edge pixel oMinutiae -
	 * points to a list of detected minutia structures Return Code: Zero - minutia
	 * point processed successfully IGNORE - minutia point is to be ignored Negative
	 * - system error
	 **************************************************************************/
	public int adjustHighCurvatureMinutia(AtomicInteger oIDir, AtomicInteger oXLoc, AtomicInteger oYLoc,
			AtomicInteger oXEdge, AtomicInteger oYEdge, final int xLoc, final int yLoc, final int xEdge,
			final int yEdge, int[] binarizedImageData, final int imageWidth, final int imageHeight,
			AtomicReference<Minutiae> oMinutiae, final LfsParams lfsParams) {
		Contour contour = null;
		AtomicInteger ret = new AtomicInteger(0);
		AtomicInteger oNoOfContour = new AtomicInteger(0);
		AtomicInteger oMinIndex = new AtomicInteger(0);
		AtomicReference<Double> oMinTheta = new AtomicReference<>(0.0);
		int midX;
		int midY;
		int midPixel;
		int featurePixel;
		int iDir;
		int halfContour;
		int angleEdge;

		/* Set variable from parameter structure. */
		halfContour = lfsParams.getHighCurveHalfContour();

		/* Set edge length for computing contour's angle of curvature */
		/* to one quarter of desired pixel length of entire contour. */
		/* Ex. If halfContour==14, then contour length==29=(2X14)+1 */
		/* and angleEdge==7=(14/2). */
		angleEdge = halfContour >> 1;

		/* Get the pixel value of current feature. */
		featurePixel = binarizedImageData[0 + (yLoc * imageWidth) + xLoc];

		/* Extract feature's contour. */
		contour = getContour().getHighCurvatureContour(ret, oNoOfContour, halfContour, xLoc, yLoc, xEdge, yEdge,
				binarizedImageData, imageWidth, imageHeight);
		if (ret.get() != ILfs.FALSE) {
			/* Returns with: */
			/* 1. Successful or empty contour == 0 */
			/* If contour is empty, then contour lists are not allocated. */
			/* 2. Contour forms loop == LOOP_FOUND */
			/* 3. Sysetm error < 0 */

			/* If the contour forms a loop... */
			if (ret.get() == ILfs.LOOP_FOUND) {
				/* If the order of the contour is clockwise, then the loops's */
				/* contour pixels are outside the corresponding edge pixels. We */
				/* definitely do NOT want to fill based on the feature pixel in */
				/* this case, because it is OUTSIDE the loop. For now we will */
				/* ignore the loop and the minutia that triggered its tracing. */
				/* It is likely that other minutia on the loop will be */
				/* detected that create a contour on the "inside" of the loop. */
				/* There is another issue here that could be addressed ... */
				/* It seems that many/multiple minutia are often detected within */
				/* the same loop, which currently requires retracing the loop, */
				/* locating minutia on opposite ends of the major axis of the */
				/* loop, and then determining that the minutia have already been */
				/* entered into the minutiae list upon processing the very first */
				/* minutia detected in the loop. There is a lot of redundant */
				/* work being done here! */
				/* Is_loop_clockwise takes a default value to be returned if the */
				/* routine is unable to determine the direction of the contour. */
				/* In this case, we want to IGNORE the loop if we can't tell its */
				/* direction so that we do not inappropriately fill the loop, so */
				/* we are passing the default value TRUE. */
				ret.set(getLoop().isLoopClockwise(contour.getContourX(), contour.getContourY(), oNoOfContour.get(),
						ILfs.TRUE));
				if (ret.get() != ILfs.FALSE) {
					/* Deallocate contour lists. */
					getContour().freeContour(contour);
					/* If we had a system error... */
					if (ret.get() < ILfs.FALSE) {
						/* Return the error code. */
						return (ret.get());
					}
					/* Otherwise, loop is clockwise, so return IGNORE. */
					return (ILfs.IGNORE);
				}

				/* Otherwise, process the clockwise-ordered contour of the loop */
				/* as it may contain minutia. If no minutia found, then it is */
				/* filled in. */
				ret.set(getLoop().processLoop(oMinutiae, contour.getContourX(), contour.getContourY(),
						contour.getContourEx(), contour.getContourEy(), oNoOfContour.get(), binarizedImageData,
						imageWidth, imageHeight, lfsParams));
				/* Returns with: */
				/* 1. Successful processing of loop == 0 */
				/* 2. System error < 0 */

				/* Deallocate contour lists. */
				getContour().freeContour(contour);

				/* If loop processed successfully ... */
				if (ret.get() == ILfs.FALSE) {
					/* Then either a minutia pair was extracted or the loop was */
					/* filled. Either way we want to IGNORE the minutia that */
					/* started the whole loop processing in the beginning. */
					return (ILfs.IGNORE);
				}

				/* Otherwise, there was a system error. */
				/* Return the resulting code. */
				return (ret.get());
			}

			/* Otherwise not a loop, so get_high_curvature_contour incurred */
			/* a system error. Return the error code. */
			return (ret.get());
		}

		/* If contour is empty ... then contour lists were not allocated, so */
		/* simply return IGNORE. The contour comes back empty when there */
		/* were not a sufficient number of points found on the contour. */
		if (oNoOfContour.get() == ILfs.FALSE) {
			return (ILfs.IGNORE);
		}

		/* Otherwise, there are contour points to process. */

		/* Given the contour, determine the point of highest curvature */
		/* (ie. forming the minimum angle between contour walls). */
		ret.set(getContour().minContourTheta(oMinIndex, oMinTheta, angleEdge, contour.getContourX(),
				contour.getContourY(), oNoOfContour.get()));
		if (ret.get() != ILfs.FALSE) {
			/* Deallocate contour lists. */
			getContour().freeContour(contour);
			/* Returns IGNORE or system error. Either way */
			/* free the contour and return the code. */
			return (ret.get());
		}

		/* If the minimum theta found along the contour is too large... */
		if (oMinTheta.get() >= lfsParams.getMaxHighCurveTheta()) {
			/* Deallocate contour lists. */
			getContour().freeContour(contour);
			/* Reject the high-curvature minutia, and return IGNORE. */
			return (ILfs.IGNORE);
		}

		/* Test to see if interior of curvature is OK. Compute midpoint */
		/* between left and right points symmetrically distant (angleEdge */
		/* pixels) from the contour's point of minimum theta. */
		midX = (contour.getContourX().get(oMinIndex.get() - angleEdge)
				+ contour.getContourX().get(oMinIndex.get() + angleEdge)) >> 1;
		midY = (contour.getContourY().get(oMinIndex.get() - angleEdge)
				+ contour.getContourY().get(oMinIndex.get() + angleEdge)) >> 1;
		midPixel = binarizedImageData[0 + (midY * imageWidth) + midX];
		/* If the interior pixel value is not the same as the feature's... */
		if (midPixel != featurePixel) {
			/* Deallocate contour lists. */
			getContour().freeContour(contour);
			/* Reject the high-curvature minutia and return IGNORE. */
			return (ILfs.IGNORE);
		}

		/* Compute new direction based on line connecting adjusted feature */
		/* location and the midpoint in the feature's interior. */
		iDir = getLfsUtil().lineToDirection(contour.getContourX().get(oMinIndex.get()),
				contour.getContourY().get(oMinIndex.get()), midX, midY, lfsParams.getNumDirections());

		/* Set minutia location to minimum theta position on the contour. */
		oIDir.set(iDir);
		oXLoc.set(contour.getContourX().get(oMinIndex.get()));
		oYLoc.set(contour.getContourY().get(oMinIndex.get()));
		oXEdge.set(contour.getContourEx().get(oMinIndex.get()));
		oYEdge.set(contour.getContourEy().get(oMinIndex.get()));

		/* Deallocate contour buffers. */
		getContour().freeContour(contour);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: adjustHighCurvatureMinutiaV2 - Takes an initial minutia point #cat: in
	 * a high-curvature area and adjusts its location and #cat: direction. First, it
	 * walks and extracts the contour #cat: of the detected feature looking for and
	 * processing any loop #cat: discovered along the way. Once the contour is
	 * extracted, #cat: the point of highest-curvature is determined and used to
	 * #cat: adjust the location of the minutia point. The angle of #cat: the line
	 * perpendicular to the tangent on the high-curvature #cat: contour at the
	 * minutia point is used as the mintutia's #cat: direction. Input: xLoc -
	 * starting x-pixel coord of feature (interior to feature) yLoc - starting
	 * y-pixel coord of feature (interior to feature) xEdge - x-pixel coord of
	 * corresponding edge pixel (exterior to feature) yEdge - y-pixel coord of
	 * corresponding edge pixel (exterior to feature) binarizedImageData - binary
	 * image data (0==while & 1==black) imageWidth - width (in pixels) of image
	 * imageHeight - height (in pixels) of image oLowFlowMap - pixelized Low Ridge
	 * Flow Map lfsParams - parameters and thresholds for controlling LFS Output:
	 * oIDir - direction of adjusted minutia point oXLoc - adjusted x-pixel coord of
	 * feature oYLoc - adjusted y-pixel coord of feature oXEdge - adjusted x-pixel
	 * coord of corresponding edge pixel oYEdge - adjusted y-pixel coord of
	 * corresponding edge pixel oMinutiae - points to a list of detected minutia
	 * structures Return Code: Zero - minutia point processed successfully IGNORE -
	 * minutia point is to be ignored Negative - system error
	 **************************************************************************/
	public int adjustHighCurvatureMinutiaV2(AtomicInteger oIDir, AtomicInteger oXLoc, AtomicInteger oYLoc,
			AtomicInteger oXEdge, AtomicInteger oYEdge, final int xLoc, final int yLoc, final int xEdge,
			final int yEdge, int[] binarizedImageData, final int imageWidth, final int imageHeight,
			AtomicIntegerArray oLowFlowMap, AtomicReference<Minutiae> oMinutiae, final LfsParams lfsParams) {
		Contour contour = null;
		AtomicInteger ret = new AtomicInteger(0);
		AtomicInteger oNoOfContour = new AtomicInteger(0);
		AtomicInteger oMinIndex = new AtomicInteger(0);
		AtomicReference<Double> oMinTheta = new AtomicReference<>(0.0d);
		int featurePixel;
		int midX;
		int midY;
		int midPixel;
		int iDir;
		int halfContour;
		int angleEdge;

		/* Set variable from parameter structure. */
		halfContour = lfsParams.getHighCurveHalfContour();

		/* Set edge length for computing contour's angle of curvature */
		/* to one quarter of desired pixel length of entire contour. */
		/* Ex. If halfContour==14, then contour length==29=(2X14)+1 */
		/* and angleEdge==7=(14/2). */
		angleEdge = halfContour >> 1;

		/* Get the pixel value of current feature. */
		featurePixel = binarizedImageData[0 + (yLoc * imageWidth) + xLoc];

		/* Extract feature's contour. */
		contour = getContour().getHighCurvatureContour(ret, oNoOfContour, halfContour, xLoc, yLoc, xEdge, yEdge,
				binarizedImageData, imageWidth, imageHeight);
		if (ret.get() != ILfs.FALSE) {
			/* Returns with: */
			/* 1. Successful or empty contour == 0 */
			/* If contour is empty, then contour lists are not allocated. */
			/* 2. Contour forms loop == LOOP_FOUND */
			/* 3. Sysetm error < 0 */

			/* If the contour forms a loop... */
			if (ret.get() == ILfs.LOOP_FOUND) {
				/* If the order of the contour is clockwise, then the loops's */
				/* contour pixels are outside the corresponding edge pixels. We */
				/* definitely do NOT want to fill based on the feature pixel in */
				/* this case, because it is OUTSIDE the loop. For now we will */
				/* ignore the loop and the minutia that triggered its tracing. */
				/* It is likely that other minutia on the loop will be */
				/* detected that create a contour on the "inside" of the loop. */
				/* There is another issue here that could be addressed ... */
				/* It seems that many/multiple minutia are often detected within */
				/* the same loop, which currently requires retracing the loop, */
				/* locating minutia on opposite ends of the major axis of the */
				/* loop, and then determining that the minutia have already been */
				/* entered into the minutiae list upon processing the very first */
				/* minutia detected in the loop. There is a lot of redundant */
				/* work being done here! */
				/* Is_loop_clockwise takes a default value to be returned if the */
				/* routine is unable to determine the direction of the contour. */
				/* In this case, we want to IGNORE the loop if we can't tell its */
				/* direction so that we do not inappropriately fill the loop, so */
				/* we are passing the default value TRUE. */
				ret.set(getLoop().isLoopClockwise(contour.getContourX(), contour.getContourY(), oNoOfContour.get(),
						ILfs.TRUE));
				if (ret.get() != ILfs.FALSE)// true
				{
					/* Deallocate contour lists. */
					getContour().freeContour(contour);
					/* If we had a system error... */
					if (ret.get() < ILfs.FALSE)// error
					{
						/* Return the error code. */
						return (ret.get());
					}
					/* Otherwise, loop is clockwise, so return IGNORE. */
					return (ILfs.IGNORE);
				}

				/* Otherwise, process the clockwise-ordered contour of the loop */
				/* as it may contain minutia. If no minutia found, then it is */
				/* filled in. */
				ret.set(getLoop().processLoopV2(oMinutiae, contour.getContourX(), contour.getContourY(),
						contour.getContourEx(), contour.getContourEy(), oNoOfContour.get(), binarizedImageData,
						imageWidth, imageHeight, oLowFlowMap, lfsParams));
				/* Returns with: */
				/* 1. Successful processing of loop == 0 */
				/* 2. System error < 0 */

				/* Deallocate contour lists. */
				getContour().freeContour(contour);

				/* If loop processed successfully ... */
				if (ret.get() == ILfs.FALSE) {
					/* Then either a minutia pair was extracted or the loop was */
					/* filled. Either way we want to IGNORE the minutia that */
					/* started the whole loop processing in the beginning. */
					return (ILfs.IGNORE);
				}

				/* Otherwise, there was a system error. */
				/* Return the resulting code. */
				return (ret.get());
			}

			/* Otherwise not a loop, so get_high_curvature_contour incurred */
			/* a system error. Return the error code. */
			return (ret.get());
		}

		/* If contour is empty ... then contour lists were not allocated, so */
		/* simply return IGNORE. The contour comes back empty when there */
		/* were not a sufficient number of points found on the contour. */
		if (oNoOfContour.get() == ILfs.FALSE) {
			return (ILfs.IGNORE);
		}

		/* Otherwise, there are contour points to process. */

		/* Given the contour, determine the point of highest curvature */
		/* (ie. forming the minimum angle between contour walls). */
		ret.set(getContour().minContourTheta(oMinIndex, oMinTheta, angleEdge, contour.getContourX(),
				contour.getContourY(), oNoOfContour.get()));
		if (ret.get() != ILfs.FALSE) {
			/* Deallocate contour lists. */
			getContour().freeContour(contour);
			/* Returns IGNORE or system error. Either way */
			/* free the contour and return the code. */
			return (ret.get());
		}

		/* If the minimum theta found along the contour is too large... */
		if (oMinTheta.get() >= lfsParams.getMaxHighCurveTheta()) {
			/* Deallocate contour lists. */
			getContour().freeContour(contour);
			/* Reject the high-curvature minutia, and return IGNORE. */
			return (ILfs.IGNORE);
		}

		/* Test to see if interior of curvature is OK. Compute midpoint */
		/* between left and right points symmetrically distant (angleEdge */
		/* pixels) from the contour's point of minimum theta. */
		midX = (contour.getContourX().get(oMinIndex.get() - angleEdge)
				+ contour.getContourX().get(oMinIndex.get() + angleEdge)) >> 1;
		midY = (contour.getContourY().get(oMinIndex.get() - angleEdge)
				+ contour.getContourY().get(oMinIndex.get() + angleEdge)) >> 1;
		midPixel = binarizedImageData[0 + (midY * imageWidth) + midX];
		/* If the interior pixel value is not the same as the feature's... */
		if (midPixel != featurePixel) {
			/* Deallocate contour lists. */
			getContour().freeContour(contour);
			/* Reject the high-curvature minutia and return IGNORE. */
			return (ILfs.IGNORE);
		}

		/* Compute new direction based on line connecting adjusted feature */
		/* location and the midpoint in the feature's interior. */
		iDir = getLfsUtil().lineToDirection(contour.getContourX().get(oMinIndex.get()),
				contour.getContourY().get(oMinIndex.get()), midX, midY, lfsParams.getNumDirections());

		/* Set minutia location to minimum theta position on the contour. */
		oIDir.set(iDir);
		oXLoc.set(contour.getContourX().get(oMinIndex.get()));
		oYLoc.set(contour.getContourY().get(oMinIndex.get()));
		oXEdge.set(contour.getContourEx().get(oMinIndex.get()));
		oYEdge.set(contour.getContourEy().get(oMinIndex.get()));

		/* Deallocate contour buffers. */
		getContour().freeContour(contour);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: getLowCurvatureDirection - Converts a bi-direcitonal IMAP direction
	 * #cat: (based on a semi-circle) to a uni-directional value covering #cat: a
	 * full circle based on the scan orientation used to detect #cat: a minutia
	 * feature (horizontal or vertical) and whether the #cat: detected minutia is
	 * appearing or disappearing.
	 * 
	 * Input: scanDir - designates the feature scan orientation appearing -
	 * designates the minutia as appearing or disappearing nInputBlockImageMapValue
	 * - IMAP block direction nDirs - number of IMAP directions (in semicircle)
	 * Return Code: New direction - bi-directonal integer direction on full circle
	 *************************************************************************/
	public int getLowCurvatureDirection(int scanDir, int appearing, int nInputBlockImageMapValue, int nDirs) {
		int iDir;

		/* Start direction out with IMAP value. */
		iDir = nInputBlockImageMapValue;

		/* NOTE! */
		/* The logic in this routine should hold whether for ridge endings */
		/* or for bifurcations. The examples in the comments show ridge */
		/* ending conditions only. */

		/* CASE I : Ridge flow in Quadrant I; directions [0..8] */
		if (nInputBlockImageMapValue <= (nDirs >> 1)) {
			/* I.A: HORIZONTAL scan */
			if (scanDir == ILfs.SCAN_HORIZONTAL) {
				/* I.A.1: Appearing Minutia */
				if (appearing == ILfs.APPEARING) {
					/* Ex. 0 0 0 */
					/* 0 1 0 */
					/* ? ? */
					/* Ridge flow is up and to the right, whereas */
					/* actual ridge is running down and to the */
					/* left. */
					/* Thus: HORIZONTAL : appearing : should be */
					/* OPPOSITE the ridge flow direction. */
					iDir += nDirs;
				}
				/* Otherwise: */
				/* I.A.2: Disappearing Minutia */
				/* Ex. ? ? */
				/* 0 1 0 */
				/* 0 0 0 */
				/* Ridge flow is up and to the right, which */
				/* should be SAME direction from which ridge */
				/* is projecting. */
				/* Thus: HORIZONTAL : disappearing : should */
				/* be the same as ridge flow direction. */
			} // End if HORIZONTAL scan
			/* Otherwise: */
			/* I.B: VERTICAL scan */
			else {
				/* I.B.1: Disappearing Minutia */
				if (appearing != ILfs.APPEARING) {
					/* Ex. 0 0 */
					/* ? 1 0 */
					/* ? 0 0 */
					/* Ridge flow is up and to the right, whereas */
					/* actual ridge is projecting down and to the */
					/* left. */
					/* Thus: VERTICAL : disappearing : should be */
					/* OPPOSITE the ridge flow direction. */
					iDir += nDirs;
				}
				/* Otherwise: */
				/* I.B.2: Appearing Minutia */
				/* Ex. 0 0 ? */
				/* 0 1 ? */
				/* 0 0 */
				/* Ridge flow is up and to the right, which */
				/* should be SAME direction the ridge is */
				/* running. */
				/* Thus: VERTICAL : appearing : should be */
				/* be the same as ridge flow direction. */
			} // End else VERTICAL scan
		} // End if Quadrant I
		/* Otherwise: */
		/* CASE II : Ridge flow in Quadrant II; directions [9..15] */
		else {
			/* II.A: HORIZONTAL scan */
			if (scanDir == ILfs.SCAN_HORIZONTAL) {
				/* II.A.1: Disappearing Minutia */
				if (appearing != ILfs.APPEARING) {
					/* Ex. ? ? */
					/* 0 1 0 */
					/* 0 0 0 */
					/* Ridge flow is down and to the right, */
					/* whereas actual ridge is running up and to */
					/* the left. */
					/* Thus: HORIZONTAL : disappearing : should */
					/* be OPPOSITE the ridge flow direction. */
					iDir += nDirs;
				}
				/* Otherwise: */
				/* II.A.2: Appearing Minutia */
				/* Ex. 0 0 0 */
				/* 0 1 0 */
				/* ? ? */
				/* Ridge flow is down and to the right, which */
				/* should be same direction from which ridge */
				/* is projecting. */
				/* Thus: HORIZONTAL : appearing : should be */
				/* the SAME as ridge flow direction. */
			} // End if HORIZONTAL scan
			/* Otherwise: */
			/* II.B: VERTICAL scan */
			else {
				/* II.B.1: Disappearing Minutia */
				if (appearing != ILfs.APPEARING) {
					/* Ex. ? 0 0 */
					/* ? 1 0 */
					/* 0 0 */
					/* Ridge flow is down and to the right, */
					/* whereas actual ridge is running up and to */
					/* the left. */
					/* Thus: VERTICAL : disappearing : should be */
					/* OPPOSITE the ridge flow direction. */
					iDir += nDirs;
				}
				/* Otherwise: */
				/* II.B.2: Appearing Minutia */
				/* Ex. 0 0 */
				/* 0 1 ? */
				/* 0 0 ? */
				/* Ridge flow is down and to the right, which */
				/* should be same direction the ridge is */
				/* projecting. */
				/* Thus: VERTICAL : appearing : should be */
				/* be the SAME as ridge flow direction. */
			} // End else VERTICAL scan
		} // End else Quadrant II

		/* Return resulting direction on range [0..31]. */
		return (iDir);
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: free_Minutiae - Takes a minutiae list and deallocates all memory #cat:
	 * associated with it. Input: oMinutiae - pointer to allocated list of minutia
	 * structures
	 *************************************************************************/
	public void freeMinutiae(AtomicReference<Minutiae> oMinutiae) {
		int i;

		if (oMinutiae != null) {
			/* Deallocate ominutiae structures in the list. */
			for (i = 0; i < oMinutiae.get().getNum(); i++) {
				freeMinutia(oMinutiae.get().getList().get(i));
			}

			/* Deallocate list of minutia pointers. */
			oMinutiae.get().setList(null);
			/* Deallocate the list structure. */
			oMinutiae.set(null);
		}
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: freeMinutia - Takes a minutia pointer and deallocates all memory #cat:
	 * associated with it. Input: minutia - pointer to allocated minutia structure
	 *************************************************************************/
	@SuppressWarnings({ "java:S1186" })
	public void freeMinutia(Minutia minutia) {
	}
}