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

/**
 * Java port of NIST MINDTCT {@code minutia.c}: detection, bookkeeping and ordering of minutiae
 * within the LFS (Loops, Forks and Splits) minutiae detection pipeline used by NFIQ 1.0.
 *
 * <p>This helper is responsible for:
 * <ul>
 * <li>allocating and growing the {@link Minutiae} list ({@code alloc_minutiae}, {@code realloc_minutiae});</li>
 * <li>scanning the binarized fingerprint image horizontally and vertically for the ridge-ending and
 * bifurcation pixel patterns defined in {@link Globals#getFeaturePatterns()} ({@code scan4minutiae*});</li>
 * <li>turning each detected pattern into a {@link Minutia} with a location, direction, type,
 * appearing/disappearing flag and reliability ({@code process_*_scan_minutia*},
 * {@code adjust_high_curvature_minutia*}, {@code get_low_curvature_direction});</li>
 * <li>adding candidates to the list while rejecting duplicates ({@code update_minutiae*});</li>
 * <li>sorting, de-duplicating, removing and joining minutiae ({@code sort_minutiae_*},
 * {@code rm_dup_minutiae}, {@code remove_minutia}, {@code join_minutia});</li>
 * <li>debug dumps of minutiae lists to text files ({@code dump_minutiae*}).</li>
 * </ul>
 *
 * <p>The false-minutiae removal stage that follows detection is implemented in {@link RemoveMinutia}.
 *
 * <p>The class is a stateless singleton obtained through {@link #getInstance()} (initialization-on-demand
 * holder idiom). It holds no mutable state of its own, so the instance can be shared between threads as long
 * as callers do not share the {@link Minutiae} lists or image buffers they pass in.
 */
public class MinutiaHelper extends MindTct implements IMinutia {
	/**
	 * SLF4J logger used to report errors (e.g. out-of-range indices, bad pixel configurations) and debug-dump
	 * status for this class.
	 */
	private static final Logger logger = LoggerFactory.getLogger(MinutiaHelper.class);

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()} instead.
	 */
	private MinutiaHelper() {
		super();
	}

	/**
	 * Initialization-on-demand holder for the {@link MinutiaHelper} singleton. The JVM class-loading guarantees
	 * make creation of {@link #INSTANCE} lazy and thread-safe without explicit locking.
	 */
	private static class Holder {
		/**
		 * The single shared {@link MinutiaHelper} instance, created when {@link Holder} is first loaded.
		 */
		private static final MinutiaHelper INSTANCE = new MinutiaHelper();
	}

	/**
	 * Returns the shared {@link MinutiaHelper} singleton.
	 *
	 * @return the lazily created, process-wide {@link MinutiaHelper} instance
	 */
	public static synchronized MinutiaHelper getInstance() {
		return Holder.INSTANCE;
	}

	/**
	 * Returns the {@link MatchPattern} singleton used to match scanned pixel-pair sequences against the
	 * minutia feature patterns.
	 *
	 * @return the shared {@link MatchPattern} instance
	 */
	public MatchPattern getMatchPattern() {
		return MatchPattern.getInstance();
	}

	/**
	 * Returns the {@link Globals} singleton that holds the MINDTCT global tables (feature patterns, etc.).
	 *
	 * @return the shared {@link Globals} instance
	 */
	public Globals getGlobals() {
		return Globals.getInstance();
	}

	/**
	 * Returns the {@link Contour} singleton used to trace and search feature contours.
	 *
	 * @return the shared {@link Contour} instance
	 */
	public Contour getContour() {
		return Contour.getInstance();
	}

	/**
	 * Returns the {@link Line} singleton used to compute the pixel points along a line segment.
	 *
	 * @return the shared {@link Line} instance
	 */
	public Line getLine() {
		return Line.getInstance();
	}

	/**
	 * Returns the {@link Free} singleton, the Java stand-in for the C memory deallocation helpers.
	 *
	 * @return the shared {@link Free} instance
	 */
	public Free getFree() {
		return Free.getInstance();
	}

	/**
	 * Returns the {@link Sort} singleton used to compute sorted index orders.
	 *
	 * @return the shared {@link Sort} instance
	 */
	public Sort getSort() {
		return Sort.getInstance();
	}

	/**
	 * Returns the {@link Loop} singleton used to detect and process loops found while tracing contours.
	 *
	 * @return the shared {@link Loop} instance
	 */
	public Loop getLoop() {
		return Loop.getInstance();
	}

	/**
	 * Returns the {@link LfsUtil} singleton providing LFS geometry utilities (distances, line directions, ...).
	 *
	 * @return the shared {@link LfsUtil} instance
	 */
	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	/**
	 * Allocates and initializes a minutiae list sized for the specified maximum number of minutiae.
	 *
	 * <p>NIST origin: {@code alloc_minutiae()} in {@code minutia.c}. A new backing {@link ArrayList} with initial
	 * capacity {@code maxMinutiae} is assigned to the referenced {@link Minutiae}, its allocated length is set to
	 * {@code maxMinutiae} and its count of stored minutiae is reset to zero.
	 *
	 * @param oMinutiae   holder of an already constructed {@link Minutiae} object; on return its list, allocated
	 *                    length and count are initialized
	 * @param maxMinutiae number of minutiae slots to allocate in the list
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
	public int allocMinutiae(AtomicReference<Minutiae> oMinutiae, final int maxMinutiae) {
		List<Minutia> list = new ArrayList<>(maxMinutiae);

		oMinutiae.get().setList(list);
		oMinutiae.get().setAlloc(maxMinutiae);
		oMinutiae.get().setNum(0);

		return (ILfs.FALSE);
	}

	/**
	 * Extends the allocated length of a previously allocated minutiae list by the specified increment.
	 *
	 * <p>NIST origin: {@code realloc_minutiae()} in {@code minutia.c}. The allocated length recorded in the
	 * {@link Minutiae} object is increased by {@code incrMinutiae} and the capacity of the backing list is
	 * ensured accordingly; existing entries are preserved.
	 *
	 * @param oMinutiae    holder of a previously allocated minutiae list; extended in place
	 * @param incrMinutiae number of additional minutiae slots to allocate
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
	public int reallocMinutiae(AtomicReference<Minutiae> oMinutiae, final int incrMinutiae) {
		oMinutiae.get().setAlloc(oMinutiae.get().getAlloc() + incrMinutiae);
		((ArrayList<?>) oMinutiae.get().getList()).ensureCapacity(oMinutiae.get().getList().size() + incrMinutiae);

		return (ILfs.FALSE);
	}

	/**
	 * Scans a binary image, guided by its block Direction, Low Flow and High Curvature maps, for minutia points.
	 *
	 * <p>NIST origin: {@code detect_minutiae_V2()} in {@code minutia.c}. The block-level maps held by {@code map}
	 * are first "pixelized" (each block value copied to every pixel it covers) via
	 * {@link Maps#pixelizeMap}. The whole image is then scanned horizontally
	 * ({@link #scanForMinutiaeHorizontallyV2}) and vertically ({@link #scanForMinutiaeVerticallyV2}).
	 * Minutia points detected in blocks with INVALID direction are ignored, and those detected in LOW FLOW
	 * blocks are assigned a lower reliability.
	 *
	 * @param oMinutiae          holder of the minutiae list; detected minutiae are appended to it
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param mappedImageWidth   width of the binary image in pixels (the dimensions the block maps are pixelized
	 *                           to, despite the parameter name)
	 * @param mappedImageHeight  height of the binary image in pixels (see {@code mappedImageWidth})
	 * @param map                maps object holding the block Direction Map, Low Flow Map and High Curvature Map
	 *                           together with their width and height in blocks
	 * @param lfsParams          parameters and thresholds for controlling LFS; {@code blockOffsetSize} gives the
	 *                           block size in pixels used when pixelizing the maps
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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

	/**
	 * Adds a detected minutia to the list unless it is judged to already be present.
	 *
	 * <p>NIST origin: {@code update_minutiae()} in {@code minutia.c}. If the list is full it is first grown by
	 * {@link ILfs#MAX_MINUTIAE} via {@link #reallocMinutiae}. The new minutia is considered a duplicate of an
	 * existing one when both lie within {@code maxMinutiaDelta} pixels in x and y, have the same type, their
	 * directions differ by at most 45 degrees (a quarter of the semicircle directions), and either they share
	 * the exact same pixel or the new point is found within {@code maxMinutiaDelta} steps along the existing
	 * minutia's contour (searched clockwise, then counter-clockwise). Otherwise the minutia is appended.
	 *
	 * @param oMinutiae          holder of the minutiae list; the minutia is appended to it when accepted
	 * @param minutia            the newly detected minutia candidate
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) if the minutia was added to the list; {@link ILfs#IGNORE} if it is
	 *         ignored because it is already in the list; a negative value on system error
	 */
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

	/**
	 * Adds a detected minutia to the list unless it already exists there, preferring the more "compatible" of
	 * two near-duplicate minutiae.
	 *
	 * <p>NIST origin: {@code update_minutiae_V2()} in {@code minutia.c}. Works like {@link #updateMinutiae}
	 * (same proximity, type, direction and shared-contour tests, but walking the list in reverse order).
	 * When a near-duplicate sharing the same contour is found and the new minutia lies in a block with VALID
	 * direction, the scan direction compatible with that block direction ({@link #chooseScanDirection}) is
	 * compared with {@code scanDir}: if they match, the existing minutia is removed and the new one kept;
	 * otherwise the new minutia is ignored. With INVALID block direction the new minutia is ignored.
	 *
	 * @param oMinutiae          holder of the minutiae list; may have entries removed and the new minutia
	 *                           appended
	 * @param minutia            the newly detected minutia candidate
	 * @param scanDir            orientation of the scan that detected the minutia
	 *                           ({@link ILfs#SCAN_HORIZONTAL} or {@link ILfs#SCAN_VERTICAL})
	 * @param directionMapValue  directional ridge flow of the block containing the minutia (negative if INVALID)
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) if the minutia was added to the list; {@link ILfs#IGNORE} if it is
	 *         ignored (already in the list); a negative value on system error
	 */
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

	/**
	 * Sorts a minutiae list top-to-bottom and then left-to-right.
	 *
	 * <p>NIST origin: {@code sort_minutiae_y_x()} in {@code minutia.c}. Each minutia is ranked by its 1-D pixel
	 * offset {@code y * imageWidth + x}, the ranks are sorted in increasing order and the list is rebuilt in
	 * that order.
	 *
	 * @param oMinutiae   holder of the minutiae list; reordered in place
	 * @param imageWidth  width of the image, in pixels
	 * @param imageHeight height of the image, in pixels (unused; kept for parity with the C API)
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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

	/**
	 * Sorts a minutiae list left-to-right and then top-to-bottom.
	 *
	 * <p>NIST origin: {@code sort_minutiae_x_y()} in {@code minutia.c}. Each minutia is ranked by
	 * {@code x * imageWidth + y}, the ranks are sorted in increasing order and the list is rebuilt in that order.
	 *
	 * @param oMinutiae   holder of the minutiae list; reordered in place
	 * @param imageWidth  width of the image, in pixels
	 * @param imageHeight height of the image, in pixels (unused; kept for parity with the C API)
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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

	/**
	 * Removes redundant minutiae that share exactly the same pixel coordinates.
	 *
	 * <p>NIST origin: {@code rm_dup_minutiae()} in {@code minutia.c}. The list must already be sorted in some
	 * adjacent order (e.g. by {@link #sortMinutiaeTopToBottomAndThenLeftToRight}). Walking backwards, whenever
	 * two consecutive minutiae have identical coordinates the earlier one is removed, even if other attributes
	 * differ.
	 *
	 * @param oMinutiae holder of the sorted minutiae list; duplicates are removed in place
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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

	/**
	 * Writes a formatted text report of the contents of a minutiae list to a file (debugging aid).
	 *
	 * <p>NIST origin: {@code dump_minutiae()} in {@code minutia.c}. For each minutia the index, coordinates,
	 * direction, reliability, type (RIG/BIF), appearing flag (APP/DIS), feature id and neighbor information are
	 * written. I/O errors are logged and not propagated.
	 *
	 * @param file      destination file (overwritten)
	 * @param oMinutiae holder of the minutiae list to report
	 */
	public void dumpMinutiae(File file, AtomicReference<Minutiae> oMinutiae) {
		try (FileWriter myWriter = new FileWriter(file.getAbsoluteFile())) {
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
							oMinutiae.get().getList().get(i).getRidgeCounts().get(j)));
				}

				myWriter.write("");
			}

			logger.debug("dumpMinutiae::Successfully wrote to the file.");
		} catch (IOException e) {
			logger.error("An error occurred.", e);
		}
	}

	/**
	 * Writes the number of minutiae followed by the coordinate point of each minutia to a file (debugging aid).
	 *
	 * <p>NIST origin: {@code dump_minutiae_pts()} in {@code minutia.c}. I/O errors are logged and not propagated.
	 *
	 * @param file      destination file (overwritten)
	 * @param oMinutiae holder of the minutiae list to write
	 */
	public void dumpMinutiaePoints(File file, final AtomicReference<Minutiae> oMinutiae) {
		try (FileWriter myWriter = new FileWriter(file.getAbsoluteFile())) {
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

			logger.debug("dumpMinutiaePoints::Successfully wrote to the file.");
		} catch (IOException e) {
			logger.error("An error occurred.", e);
		}
	}

	/**
	 * Writes the coordinate points of the minutiae having a specific reliability to a file (debugging aid).
	 *
	 * <p>NIST origin: {@code dump_reliable_minutiae_pts()} in {@code minutia.c}. The count of qualifying minutiae
	 * is written first, followed by one coordinate pair per qualifying minutia. I/O errors are logged and not
	 * propagated.
	 *
	 * @param file        destination file (overwritten)
	 * @param oMinutiae   holder of the minutiae list to examine
	 * @param reliability reliability value a minutia must exactly match to be written
	 */
	public void dumpReliableMinutiaePoints(File file, AtomicReference<Minutiae> oMinutiae, final double reliability) {
		try (FileWriter myWriter = new FileWriter(file.getAbsoluteFile())) {
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

			logger.debug("Successfully wrote to the file.");
		} catch (IOException e) {
			logger.error("An error occurred.", e);
		}
	}

	/**
	 * Creates and initializes a {@link Minutia} from the attributes of a detected minutia point.
	 *
	 * <p>NIST origin: {@code create_minutia()} in {@code minutia.c}. Neighbor and ridge-count lists are left
	 * unset ({@code null}) and the neighbor count is zero.
	 *
	 * @param xLoc        x-pixel coordinate of the minutia (interior to the feature)
	 * @param yLoc        y-pixel coordinate of the minutia (interior to the feature)
	 * @param xEdge       x-pixel coordinate of the corresponding edge pixel (exterior to the feature)
	 * @param yEdge       y-pixel coordinate of the corresponding edge pixel (exterior to the feature)
	 * @param iDir        integer direction of the minutia on the full circle
	 * @param reliability floating-point measure of the minutia's reliability
	 * @param type        type of the minutia ({@link ILfs#RIDGE_ENDING} or {@link ILfs#BIFURCATION})
	 * @param appearing   whether the minutia is appearing ({@link ILfs#APPEARING}) or disappearing
	 * @param featureId   index of the minutia's matching entry in the feature patterns table
	 * @return the newly allocated and initialized minutia
	 */
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

	/**
	 * Removes the minutia at the specified position from a minutiae list.
	 *
	 * <p>NIST origin: {@code remove_minutia()} in {@code minutia.c}. Subsequent entries are slid up one position,
	 * the last slot is dropped and the minutiae count is decremented.
	 *
	 * @param index     position of the minutia to remove; expected to be in {@code [0, num)}
	 * @param oMinutiae holder of the minutiae list; modified in place
	 * @return {@link ILfs#FALSE} (zero) on successful completion; {@link ILfs#ERROR_CODE_380} (negative) if
	 *         the index is reported out of range
	 */
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

	/**
	 * Connects the features of two minutia points by drawing a line between them in the binary image.
	 *
	 * <p>NIST origin: {@code join_minutia()} in {@code minutia.c}. The line is drawn in the minutia color
	 * (black to join two ridge-endings, white to join two bifurcations) with the specified radial width,
	 * widened vertically when {@code |dx| >= |dy|} and horizontally otherwise. If {@code with_boundary} is
	 * non-zero, a one-pixel border of the opposite color is drawn on each side of the line. The end points
	 * themselves are not rewritten.
	 *
	 * @param minutia1           first minutia point to be joined
	 * @param minutia2           second minutia point to be joined
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major; edited in
	 *                           place with the features joined
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param with_boundary      non-zero to also draw boundary pixels of the opposite color
	 * @param line_radius        line-width radius of the join line, in pixels
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error (e.g. from
	 *         line point generation)
	 */
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

	/**
	 * Classifies a minutia as ridge-ending or bifurcation from the pixel color of the detected feature.
	 *
	 * <p>NIST origin: {@code minutia_type()} in {@code minutia.c}. A black feature pixel denotes a ridge-ending;
	 * a white feature pixel denotes a valley-ending, i.e. a bifurcation.
	 *
	 * @param featurePixel pixel color of the feature's interior ({@code 0} = white, {@code 1} = black)
	 * @return {@link ILfs#RIDGE_ENDING} if the minutia is a ridge-ending; {@link ILfs#BIFURCATION} if it is a
	 *         bifurcation (valley-ending)
	 */
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

	/**
	 * Determines whether a minutia is appearing or disappearing from the position of its edge pixel.
	 *
	 * <p>NIST origin: {@code is_minutia_appearing()} in {@code minutia.c}. A "feature" refers to either a ridge-
	 * or valley-ending. The edge pixel is always N, S, E or W of the feature pixel: an edge preceding the feature
	 * (smaller x, or smaller y) means appearing; an edge following it means disappearing.
	 *
	 * @param xLoc  x-pixel coordinate of the feature (interior to the feature)
	 * @param yLoc  y-pixel coordinate of the feature (interior to the feature)
	 * @param xEdge x-pixel coordinate of the corresponding edge pixel (exterior to the feature)
	 * @param yEdge y-pixel coordinate of the corresponding edge pixel (exterior to the feature)
	 * @return {@link ILfs#APPEARING} (TRUE == 1) if the minutia is appearing; {@link ILfs#DISAPPEARING}
	 *         (FALSE == 0) if disappearing; {@link ILfs#ERROR_CODE_240} (negative) for a bad pixel configuration
	 */
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

	/**
	 * Determines the orientation (horizontal or vertical) in which a block is to be scanned for minutiae.
	 *
	 * <p>NIST origin: {@code choose_scan_direction()} in {@code minutia.c}. The scan is performed orthogonally to
	 * the block's ridge flow: relatively vertical flow (direction within 45 degrees of vertical) is scanned
	 * horizontally, relatively horizontal flow is scanned vertically.
	 *
	 * @param nInputBlockImageMapValue the block's IMAP (Direction Map) direction
	 * @param nDirs                    number of possible IMAP directions within a semicircle
	 * @return {@link ILfs#SCAN_HORIZONTAL} for horizontal orientation; {@link ILfs#SCAN_VERTICAL} for vertical
	 *         orientation
	 */
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

	/**
	 * Scans one block of binary image data for potential minutia points (version 1 block-based detection).
	 *
	 * <p>NIST origin: {@code scan4minutiae()} in {@code minutia.c}. The block is scanned first in the primary
	 * orientation {@code scanDir} and then partially rescanned in the orthogonal orientation according to its
	 * neighbors' IMAP/NMAP values ({@link #rescanForMinutiaeVertically} or {@link #rescanForMinutiaeHorizontally}).
	 *
	 * @param oMinutiae           holder of the minutiae list; detected minutiae are added to it
	 * @param binarizedImageData  binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth          width of the image, in pixels
	 * @param imageHeight         height of the image, in pixels
	 * @param oInputBlockImageMap IMAP: matrix of block ridge flow directions
	 * @param oNMap               NMAP: IMAP augmented with HIGH-CURVATURE blocks and blocks with no neighboring
	 *                            valid directions
	 * @param blockX              x-block coordinate of the block to be scanned
	 * @param blockY              y-block coordinate of the block to be scanned
	 * @param mapWidth            width (in blocks) of the IMAP and NMAP matrices
	 * @param mapHeight           height (in blocks) of the IMAP and NMAP matrices
	 * @param scanX               x-pixel coordinate of the origin of the region to be scanned
	 * @param scanY               y-pixel coordinate of the origin of the region to be scanned
	 * @param scanWidth           width (in pixels) of the region to be scanned
	 * @param scanHeight          height (in pixels) of the region to be scanned
	 * @param scanDir             primary scan orientation ({@link ILfs#SCAN_HORIZONTAL} or
	 *                            {@link ILfs#SCAN_VERTICAL})
	 * @param lfsParams           parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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

	/**
	 * Scans a region of binary image data horizontally for potential minutia points.
	 *
	 * <p>NIST origin: {@code scan4minutiae_horizontally()} in {@code minutia.c}. Pairs of adjacent rows are
	 * walked and the vertical pixel-pair sequences matched against the feature patterns; each match is handed
	 * to {@link #processHorizontalScanMinutia}. Minutiae detected this way are by nature vertically oriented
	 * (orthogonal to the scan). The scanned region is enlarged by 2 pixel columns left and right and 1 row below
	 * to reduce misses at region boundaries, but some minutiae straddling boundaries may still be missed.
	 *
	 * @param oMinutiae                holder of the minutiae list; detected minutiae are added to it
	 * @param binarizedImageData       binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth               width of the image, in pixels
	 * @param imageHeight              height of the image, in pixels
	 * @param nInputBlockImageMapValue IMAP value associated with this image region
	 * @param nNMapValue               NMAP value associated with this image region
	 * @param scanX                    x-pixel coordinate of the origin of the region to be scanned
	 * @param scanY                    y-pixel coordinate of the origin of the region to be scanned
	 * @param scanWidth                width (in pixels) of the region to be scanned
	 * @param scanHeight               height (in pixels) of the region to be scanned
	 * @param lfsParams                parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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
					p2ptrIndex.set(p2ptrIndex.get() + 1);
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

	/**
	 * Scans the entire binary image horizontally for potential minutia points.
	 *
	 * <p>NIST origin: {@code scan4minutiae_horizontally_V2()} in {@code minutia.c}. Pairs of adjacent rows are
	 * walked over the whole image and pixel-pair sequences matched against the feature patterns; each match is
	 * handed to {@link #processHorizontalScanMinutiaV2}. Minutiae detected this way are by nature vertically
	 * oriented (orthogonal to the scan).
	 *
	 * @param oMinutiae          holder of the minutiae list; detected minutiae are added to it
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param oDirectionMap      pixelized Direction Map (one value per pixel)
	 * @param oLowFlowMap        pixelized Low Ridge Flow Map
	 * @param oHighCurveMap      pixelized High Curvature Map
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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

	/**
	 * Scans a region of binary image data vertically for potential minutia points.
	 *
	 * <p>NIST origin: {@code scan4minutiae_vertically()} in {@code minutia.c}. Pairs of adjacent columns are
	 * walked and the horizontal pixel-pair sequences matched against the feature patterns; each match is handed
	 * to {@link #processVerticalScanMinutia}. Minutiae detected this way are by nature horizontally oriented
	 * (orthogonal to the scan). The scanned region is enlarged by 1 pixel column to the right and 2 rows above
	 * and below to reduce misses at region boundaries, but some minutiae straddling boundaries may still be
	 * missed.
	 *
	 * @param minutiae                 holder of the minutiae list; detected minutiae are added to it
	 * @param binarizedImageData       binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth               width of the image, in pixels
	 * @param imageHeight              height of the image, in pixels
	 * @param nInputBlockImageMapValue IMAP value associated with this image region
	 * @param nNMapValue               NMAP value associated with this image region
	 * @param scanX                    x-pixel coordinate of the origin of the region to be scanned
	 * @param scanY                    y-pixel coordinate of the origin of the region to be scanned
	 * @param scanWidth                width (in pixels) of the region to be scanned
	 * @param scanHeight               height (in pixels) of the region to be scanned
	 * @param lfsParams                parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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
			cx.set(cx.get() + 1);
		} // While not out of scan columns.

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Rescans portions of a block horizontally for potential minutiae, based on its neighbors' IMAP/NMAP values.
	 *
	 * <p>NIST origin: {@code rescan4minutiae_horizontally()} in {@code minutia.c}. A HIGH-CURVATURE block is
	 * rescanned entirely; otherwise each of the NORTH, EAST, SOUTH and WEST neighbors is considered in turn via
	 * {@link #rescanPartialHorizontally}.
	 *
	 * @param oMinutiae           holder of the minutiae list; detected minutiae are added to it
	 * @param binarizedImageData  binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth          width of the image, in pixels
	 * @param imageHeight         height of the image, in pixels
	 * @param oInputBlockImageMap IMAP: matrix of block ridge flow directions
	 * @param oNMap               NMAP: IMAP augmented with HIGH-CURVATURE blocks and blocks with no neighboring
	 *                            valid directions
	 * @param blockX              x-block coordinate of the block to be rescanned
	 * @param blockY              y-block coordinate of the block to be rescanned
	 * @param mapWidth            width (in blocks) of the IMAP and NMAP matrices
	 * @param mapHeight           height (in blocks) of the IMAP and NMAP matrices
	 * @param scanX               x-pixel coordinate of the origin of the region to be rescanned
	 * @param scanY               y-pixel coordinate of the origin of the region to be rescanned
	 * @param scanWidth           width (in pixels) of the region to be rescanned
	 * @param scanHeight          height (in pixels) of the region to be rescanned
	 * @param lfsParams           parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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

	/**
	 * Scans the entire binary image vertically for potential minutia points.
	 *
	 * <p>NIST origin: {@code scan4minutiae_vertically_V2()} in {@code minutia.c}. Pairs of adjacent columns are
	 * walked over the whole image and pixel-pair sequences matched against the feature patterns; each match is
	 * handed to {@link #processVerticalScanMinutiaV2}. Minutiae detected this way are by nature horizontally
	 * oriented (orthogonal to the scan).
	 *
	 * @param oMinutiae          holder of the minutiae list; detected minutiae are added to it
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param oDirectionMap      pixelized Direction Map (one value per pixel)
	 * @param oLowFlowMap        pixelized Low Ridge Flow Map
	 * @param oHighCurveMap      pixelized High Curvature Map
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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

	/**
	 * Rescans portions of a block vertically for potential minutiae, based on its neighbors' IMAP/NMAP values.
	 *
	 * <p>NIST origin: {@code rescan4minutiae_vertically()} in {@code minutia.c}. A HIGH-CURVATURE block is
	 * rescanned entirely; otherwise each of the NORTH, EAST, SOUTH and WEST neighbors is considered in turn via
	 * {@link #rescanPartialVertically}.
	 *
	 * @param minutiae            holder of the minutiae list; detected minutiae are added to it
	 * @param binarizedImageData  binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth          width of the image, in pixels
	 * @param imageHeight         height of the image, in pixels
	 * @param oInputBlockImageMap IMAP: matrix of block ridge flow directions
	 * @param oNMap               NMAP: IMAP augmented with HIGH-CURVATURE blocks and blocks with no neighboring
	 *                            valid directions
	 * @param blockX              x-block coordinate of the block to be rescanned
	 * @param blockY              y-block coordinate of the block to be rescanned
	 * @param mapWidth            width (in blocks) of the IMAP and NMAP matrices
	 * @param mapHeight           height (in blocks) of the IMAP and NMAP matrices
	 * @param scanX               x-pixel coordinate of the origin of the region to be rescanned
	 * @param scanY               y-pixel coordinate of the origin of the region to be rescanned
	 * @param scanWidth           width (in pixels) of the region to be rescanned
	 * @param scanHeight          height (in pixels) of the region to be rescanned
	 * @param lfsParams           parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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

	/**
	 * Rescans part of a block horizontally according to the IMAP/NMAP values of one neighboring block.
	 *
	 * <p>NIST origin: {@code rescan_partial_horizontally()} in {@code minutia.c}. If the neighbor exists and has
	 * a VALID direction whose compatible scan direction ({@link #chooseScanDirection}) is horizontal, the half of
	 * the block adjacent to that neighbor (computed by {@link #adjustHorizontalRescan}) is rescanned with
	 * {@link #scanForMinutiaeHorizontally}. Missing neighbors are silently skipped.
	 *
	 * @param nbrDir              which neighbor to consider: {@link ILfs#NORTH}, {@link ILfs#SOUTH},
	 *                            {@link ILfs#EAST} or {@link ILfs#WEST}
	 * @param oMinutiae           holder of the minutiae list; detected minutiae are added to it
	 * @param binarizedImageData  binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth          width of the image, in pixels
	 * @param imageHeight         height of the image, in pixels
	 * @param oInputBlockImageMap IMAP: matrix of block ridge flow directions
	 * @param oNMap               NMAP: IMAP augmented with HIGH-CURVATURE blocks and blocks with no neighboring
	 *                            valid directions
	 * @param blockX              x-block coordinate of the block to be rescanned
	 * @param blockY              y-block coordinate of the block to be rescanned
	 * @param mapWidth            width (in blocks) of the IMAP and NMAP matrices
	 * @param mapHeight           height (in blocks) of the IMAP and NMAP matrices
	 * @param scanX               x-pixel coordinate of the origin of the image region
	 * @param scanY               y-pixel coordinate of the origin of the image region
	 * @param scanWidth           width (in pixels) of the image region
	 * @param scanHeight          height (in pixels) of the image region
	 * @param lfsParams           parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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

	/**
	 * Rescans part of a block vertically according to the IMAP/NMAP values of one neighboring block.
	 *
	 * <p>NIST origin: {@code rescan_partial_vertically()} in {@code minutia.c}. If the neighbor exists and has a
	 * VALID direction whose compatible scan direction ({@link #chooseScanDirection}) is vertical, the half of the
	 * block adjacent to that neighbor (computed by {@link #adjustVerticalRescan}) is rescanned with
	 * {@link #scanForMinutiaeVertically}. Missing neighbors are silently skipped.
	 *
	 * @param nbrDir              which neighbor to consider: {@link ILfs#NORTH}, {@link ILfs#SOUTH},
	 *                            {@link ILfs#EAST} or {@link ILfs#WEST}
	 * @param oMinutiae           holder of the minutiae list; detected minutiae are added to it
	 * @param binarizedImageData  binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth          width of the image, in pixels
	 * @param imageHeight         height of the image, in pixels
	 * @param oInputBlockImageMap IMAP: matrix of block ridge flow directions
	 * @param oNMap               NMAP: IMAP augmented with HIGH-CURVATURE blocks and blocks with no neighboring
	 *                            valid directions
	 * @param blockX              x-block coordinate of the block to be rescanned
	 * @param blockY              y-block coordinate of the block to be rescanned
	 * @param mapWidth            width (in blocks) of the IMAP and NMAP matrices
	 * @param mapHeight           height (in blocks) of the IMAP and NMAP matrices
	 * @param scanX               x-pixel coordinate of the origin of the image region
	 * @param scanY               y-pixel coordinate of the origin of the image region
	 * @param scanWidth           width (in pixels) of the image region
	 * @param scanHeight          height (in pixels) of the image region
	 * @param lfsParams           parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
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

	/**
	 * Determines the block index (if one exists) of a specified neighbor of a block.
	 *
	 * <p>NIST origin: {@code get_nbr_block_index()} in {@code minutia.c}.
	 *
	 * @param oBlockIndex receives the neighbor's 1-D block index ({@code ny * mapWidth + nx}) when found
	 * @param nbrDir      which neighbor: {@link ILfs#NORTH}, {@link ILfs#SOUTH}, {@link ILfs#EAST} or
	 *                    {@link ILfs#WEST}
	 * @param blockX      x-block coordinate of the block whose neighbor is sought
	 * @param blockY      y-block coordinate of the block whose neighbor is sought
	 * @param mapWidth    width (in blocks) of the IMAP and NMAP matrices
	 * @param mapHeight   height (in blocks) of the IMAP and NMAP matrices
	 * @return {@link ILfs#FOUND} if the neighbor exists and its index was returned; {@link ILfs#NOT_FOUND} if the
	 *         neighbor lies outside the map; {@link ILfs#ERROR_CODE_200} (negative) for an illegal neighbor
	 *         direction
	 */
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

	/**
	 * Determines the portion of an image block to be rescanned horizontally for a specified neighbor.
	 *
	 * <p>NIST origin: {@code adjust_horizontal_rescan()} in {@code minutia.c}. The rescan region is the part of
	 * the block nearest the given neighbor, computed from half and quarter of {@code blocksize}.
	 *
	 * @param nbrDir       which neighbor: {@link ILfs#NORTH}, {@link ILfs#SOUTH}, {@link ILfs#EAST} or
	 *                     {@link ILfs#WEST}
	 * @param rescanX      receives the x-pixel coordinate of the origin of the region to be rescanned
	 * @param rescanY      receives the y-pixel coordinate of the origin of the region to be rescanned
	 * @param rescanWidth  receives the width (in pixels) of the region to be rescanned
	 * @param rescanHeight receives the height (in pixels) of the region to be rescanned
	 * @param scanX        x-pixel coordinate of the origin of the image region
	 * @param scanY        y-pixel coordinate of the origin of the image region
	 * @param scanWidth    width (in pixels) of the image region
	 * @param scanHeight   height (in pixels) of the image region
	 * @param blocksize    dimension of image blocks, in pixels
	 * @return {@link ILfs#FALSE} (zero) on successful completion; {@link ILfs#ERROR_CODE_210} (negative) for an
	 *         illegal neighbor direction
	 */
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

	/**
	 * Determines the portion of an image block to be rescanned vertically for a specified neighbor.
	 *
	 * <p>NIST origin: {@code adjust_vertical_rescan()} in {@code minutia.c}. The rescan region is the part of the
	 * block nearest the given neighbor, computed from half and quarter of {@code blocksize}.
	 *
	 * @param nbrDir       which neighbor: {@link ILfs#NORTH}, {@link ILfs#SOUTH}, {@link ILfs#EAST} or
	 *                     {@link ILfs#WEST}
	 * @param rescanX      receives the x-pixel coordinate of the origin of the region to be rescanned
	 * @param rescanY      receives the y-pixel coordinate of the origin of the region to be rescanned
	 * @param rescanWidth  receives the width (in pixels) of the region to be rescanned
	 * @param rescanHeight receives the height (in pixels) of the region to be rescanned
	 * @param scanX        x-pixel coordinate of the origin of the image region
	 * @param scanY        y-pixel coordinate of the origin of the image region
	 * @param scanWidth    width (in pixels) of the image region
	 * @param scanHeight   height (in pixels) of the image region
	 * @param blocksize    dimension of image blocks, in pixels
	 * @return {@link ILfs#FALSE} (zero) on successful completion; {@link ILfs#ERROR_CODE_220} (negative) for an
	 *         illegal neighbor direction
	 */
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

	/**
	 * Converts a feature found by the horizontal scan into a minutia and adds it to the list if new.
	 *
	 * <p>NIST origin: {@code process_horizontal_scan_minutia()} in {@code minutia.c}. The x location is set half
	 * way between the start of the second pattern pair and the third pair; the y location and edge pixel are
	 * taken from the two scan rows depending on whether the feature is appearing or disappearing. In
	 * HIGH-CURVATURE blocks location and direction are refined by {@link #adjustHighCurvatureMinutia}; otherwise
	 * the direction is derived from the block direction by {@link #getLowCurvatureDirection}. Minutiae detected
	 * here are vertical in orientation (orthogonal to the scan) and get {@link ILfs#DEFAULT_RELIABILITY}.
	 *
	 * @param oMinutiae                holder of the minutiae list; the minutia is added to it when new
	 * @param cx                       x-pixel coordinate where the 3rd pattern pair of the minutia was detected
	 * @param cy                       y-pixel coordinate of the first of the two scan rows
	 * @param x2                       x-pixel coordinate where the 2nd pattern pair of the minutia was detected
	 * @param featureId                type of minutia (index into the feature patterns table)
	 * @param binarizedImageData       binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth               width of the image, in pixels
	 * @param imageHeight              height of the image, in pixels
	 * @param nInputBlockImageMapValue IMAP value associated with this image region
	 * @param nNMapValue               NMAP value associated with this image region
	 * @param lfsParams                parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; {@link ILfs#IGNORE} if the minutia is to be
	 *         ignored; a negative value on system error
	 */
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

	/**
	 * Converts a feature found by the full-image horizontal scan into a minutia and adds it to the list if new.
	 *
	 * <p>NIST origin: {@code process_horizontal_scan_minutia_V2()} in {@code minutia.c}. Location is derived as
	 * in {@link #processHorizontalScanMinutia}. Points in blocks with INVALID direction are ignored. In HIGH
	 * CURVATURE blocks location and direction are refined by {@link #adjustHighCurvatureMinutiaV2}; otherwise the
	 * direction comes from {@link #getLowCurvatureDirection}. Reliability is {@link ILfs#MEDIUM_RELIABILITY} in
	 * LOW RIDGE FLOW blocks and {@link ILfs#HIGH_RELIABILITY} elsewhere. The minutia is then offered to
	 * {@link #updateMinutiaeV2} with {@link ILfs#SCAN_HORIZONTAL}.
	 *
	 * @param oMinutiae          holder of the minutiae list; the minutia is added to it when new
	 * @param cx                 x-pixel coordinate where the 3rd pattern pair of the minutia was detected
	 * @param cy                 y-pixel coordinate of the first of the two scan rows
	 * @param x2                 x-pixel coordinate where the 2nd pattern pair of the minutia was detected
	 * @param featureId          type of minutia (index into the feature patterns table)
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param oDirectionMap      pixelized Direction Map
	 * @param oLowFlowMap        pixelized Low Ridge Flow Map
	 * @param oHighCurveMap      pixelized High Curvature Map
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; {@link ILfs#IGNORE} if the minutia is to be
	 *         ignored; a negative value on system error
	 */
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

	/**
	 * Converts a feature found by the vertical scan into a minutia and adds it to the list if new.
	 *
	 * <p>NIST origin: {@code process_vertical_scan_minutia()} in {@code minutia.c}. The y location is set half
	 * way between the start of the second pattern pair and the third pair; the x location and edge pixel are
	 * taken from the two scan columns depending on whether the feature is appearing or disappearing. In
	 * HIGH-CURVATURE blocks location and direction are refined by {@link #adjustHighCurvatureMinutia}; otherwise
	 * the direction is derived by {@link #getLowCurvatureDirection}. Minutiae detected here are horizontal in
	 * orientation (orthogonal to the scan) and get {@link ILfs#DEFAULT_RELIABILITY}.
	 *
	 * @param oMinutiae                holder of the minutiae list; the minutia is added to it when new
	 * @param cx                       x-pixel coordinate of the first of the two scan columns
	 * @param cy                       y-pixel coordinate where the 3rd pattern pair of the minutia was detected
	 * @param y2                       y-pixel coordinate where the 2nd pattern pair of the minutia was detected
	 * @param featureId                type of minutia (index into the feature patterns table)
	 * @param binarizedImageData       binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth               width of the image, in pixels
	 * @param imageHeight              height of the image, in pixels
	 * @param nInputBlockImageMapValue IMAP value associated with this image region
	 * @param nNMapValue               NMAP value associated with this image region
	 * @param lfsParams                parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; {@link ILfs#IGNORE} if the minutia is to be
	 *         ignored; a negative value on system error
	 */
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
		yEdge.set(yLoc.get());

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

	/**
	 * Converts a feature found by the full-image vertical scan into a minutia and adds it to the list if new.
	 *
	 * <p>NIST origin: {@code process_vertical_scan_minutia_V2()} in {@code minutia.c}. Location is derived as in
	 * {@link #processVerticalScanMinutia}. Points in blocks with INVALID direction are ignored. In HIGH CURVATURE
	 * blocks location and direction are refined by {@link #adjustHighCurvatureMinutiaV2}; otherwise the direction
	 * comes from {@link #getLowCurvatureDirection}. Reliability is {@link ILfs#MEDIUM_RELIABILITY} in LOW RIDGE
	 * FLOW blocks and {@link ILfs#HIGH_RELIABILITY} elsewhere. The minutia is then offered to
	 * {@link #updateMinutiaeV2} with {@link ILfs#SCAN_VERTICAL}.
	 *
	 * @param oMinutiae          holder of the minutiae list; the minutia is added to it when new
	 * @param cx                 x-pixel coordinate of the first of the two scan columns
	 * @param cy                 y-pixel coordinate where the 3rd pattern pair of the minutia was detected
	 * @param y2                 y-pixel coordinate where the 2nd pattern pair of the minutia was detected
	 * @param featureId          type of minutia (index into the feature patterns table)
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param oDirectionMap      pixelized Direction Map
	 * @param oLowFlowMap        pixelized Low Ridge Flow Map
	 * @param oHighCurveMap      pixelized High Curvature Map
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; {@link ILfs#IGNORE} if the minutia is to be
	 *         ignored; a negative value on system error
	 */
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

	/**
	 * Adjusts the location and direction of a minutia detected in a high-curvature area.
	 *
	 * <p>NIST origin: {@code adjust_high_curvature_minutia()} in {@code minutia.c}. The feature's contour of
	 * {@code 2 * highCurveHalfContour + 1} points is traced. If the contour forms a loop, the loop is processed
	 * (its minutiae extracted or the loop filled, see {@link Loop#processLoop}) and the triggering minutia is
	 * ignored. Otherwise the point of highest curvature (minimum angle between contour walls,
	 * {@link Contour#minContourTheta}) becomes the new location, provided the angle is below
	 * {@code maxHighCurveTheta} and the interior midpoint has the feature's color. The direction is the line
	 * from that point to the interior midpoint.
	 *
	 * @param oIDir              receives the direction of the adjusted minutia
	 * @param oXLoc              receives the adjusted x-pixel coordinate of the feature
	 * @param oYLoc              receives the adjusted y-pixel coordinate of the feature
	 * @param oXEdge             receives the adjusted x-pixel coordinate of the corresponding edge pixel
	 * @param oYEdge             receives the adjusted y-pixel coordinate of the corresponding edge pixel
	 * @param xLoc               starting x-pixel coordinate of the feature (interior to the feature)
	 * @param yLoc               starting y-pixel coordinate of the feature (interior to the feature)
	 * @param xEdge              x-pixel coordinate of the corresponding edge pixel (exterior to the feature)
	 * @param yEdge              y-pixel coordinate of the corresponding edge pixel (exterior to the feature)
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major; loops may
	 *                           be filled in place
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param oMinutiae          holder of the minutiae list; minutiae found on a processed loop are added to it
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) if the minutia point was processed successfully; {@link ILfs#IGNORE}
	 *         if the minutia point is to be ignored; a negative value on system error
	 */
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

	/**
	 * Adjusts the location and direction of a minutia detected in a high-curvature area (version 2).
	 *
	 * <p>NIST origin: {@code adjust_high_curvature_minutia_V2()} in {@code minutia.c}. Same algorithm as
	 * {@link #adjustHighCurvatureMinutia}, except that loops are processed with the Low Ridge Flow Map so that
	 * minutiae extracted from loops receive an appropriate reliability.
	 *
	 * @param oIDir              receives the direction of the adjusted minutia
	 * @param oXLoc              receives the adjusted x-pixel coordinate of the feature
	 * @param oYLoc              receives the adjusted y-pixel coordinate of the feature
	 * @param oXEdge             receives the adjusted x-pixel coordinate of the corresponding edge pixel
	 * @param oYEdge             receives the adjusted y-pixel coordinate of the corresponding edge pixel
	 * @param xLoc               starting x-pixel coordinate of the feature (interior to the feature)
	 * @param yLoc               starting y-pixel coordinate of the feature (interior to the feature)
	 * @param xEdge              x-pixel coordinate of the corresponding edge pixel (exterior to the feature)
	 * @param yEdge              y-pixel coordinate of the corresponding edge pixel (exterior to the feature)
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major; loops may
	 *                           be filled in place
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param oLowFlowMap        pixelized Low Ridge Flow Map
	 * @param oMinutiae          holder of the minutiae list; minutiae found on a processed loop are added to it
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) if the minutia point was processed successfully; {@link ILfs#IGNORE}
	 *         if the minutia point is to be ignored; a negative value on system error
	 */
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

	/**
	 * Converts a bi-directional block direction (semicircle) into a uni-directional minutia direction (full
	 * circle).
	 *
	 * <p>NIST origin: {@code get_low_curvature_direction()} in {@code minutia.c}. The result depends on the scan
	 * orientation used to detect the feature and on whether the minutia is appearing or disappearing. The same
	 * logic holds for ridge-endings and bifurcations:
	 * <ul>
	 * <li>Quadrant I (direction {@code <= nDirs / 2}): {@code nDirs} is added (the minutia points opposite the
	 * ridge flow) for a horizontal-scan appearing minutia or a vertical-scan disappearing minutia.</li>
	 * <li>Quadrant II: {@code nDirs} is added for a disappearing minutia (horizontal or vertical scan).</li>
	 * </ul>
	 * Otherwise the block direction is returned unchanged.
	 *
	 * @param scanDir                  feature scan orientation ({@link ILfs#SCAN_HORIZONTAL} or
	 *                                 {@link ILfs#SCAN_VERTICAL})
	 * @param appearing                whether the minutia is appearing ({@link ILfs#APPEARING}) or disappearing
	 * @param nInputBlockImageMapValue IMAP block direction on the semicircle
	 * @param nDirs                    number of IMAP directions in a semicircle
	 * @return the minutia direction on the full circle, in the range {@code [0, 2 * nDirs)}
	 */
	@SuppressWarnings("java:S125")
	public int getLowCurvatureDirection(int scanDir, int appearing, int nInputBlockImageMapValue, int nDirs) {
		int iDir;

		/* Start direction out with IMAP value. */
		iDir = nInputBlockImageMapValue;

		boolean isQuadrantI = (nInputBlockImageMapValue <= (nDirs >> 1));

		boolean isHorizontal = scanDir == ILfs.SCAN_HORIZONTAL;
		boolean isAppearing = appearing == ILfs.APPEARING;

		// Determine if direction needs to be adjusted
		boolean addOffset = false;

		/* NOTE! */
		/* The logic in this routine should hold whether for ridge endings */
		/* or for bifurcations. The examples in the comments show ridge */
		/* ending conditions only. */

		if (isQuadrantI) {
			// CASE I
			if ((isHorizontal && isAppearing) || (!isHorizontal && !isAppearing)) {
				addOffset = true;
			}
		} else {
			// CASE II
			if ((isHorizontal && !isAppearing) || (!isHorizontal && !isAppearing)) {
				addOffset = true;
			}
		}

		/*
		 * CASE I : Ridge flow in Quadrant I; directions [0..8] if (isQuadrantI) { I.A:
		 * HORIZONTAL scan if (scanDir == ILfs.SCAN_HORIZONTAL) { I.A.1: Appearing
		 * Minutia if (appearing == ILfs.APPEARING) { Ex. 0 0 0 0 1 0 ? ? Ridge flow is
		 * up and to the right, whereas actual ridge is running down and to the left.
		 * Thus: HORIZONTAL : appearing : should be OPPOSITE the ridge flow direction.
		 * iDir += nDirs; } Otherwise: I.A.2: Disappearing Minutia Ex. ? ? 0 1 0 0 0 0
		 * Ridge flow is up and to the right, which should be SAME direction from which
		 * ridge is projecting. Thus: HORIZONTAL : disappearing : should be the same as
		 * ridge flow direction. } // End if HORIZONTAL scan Otherwise: I.B: VERTICAL
		 * scan else { I.B.1: Disappearing Minutia if (appearing != ILfs.APPEARING) {
		 * Ex. 0 0 ? 1 0 ? 0 0 Ridge flow is up and to the right, whereas actual ridge
		 * is projecting down and to the left. Thus: VERTICAL : disappearing : should be
		 * OPPOSITE the ridge flow direction. iDir += nDirs; } Otherwise: I.B.2:
		 * Appearing Minutia Ex. 0 0 ? 0 1 ? 0 0 Ridge flow is up and to the right,
		 * which should be SAME direction the ridge is running. Thus: VERTICAL :
		 * appearing : should be be the same as ridge flow direction. } // End else
		 * VERTICAL scan } // End if Quadrant I Otherwise: CASE II : Ridge flow in
		 * Quadrant II; directions [9..15] else { II.A: HORIZONTAL scan if (scanDir ==
		 * ILfs.SCAN_HORIZONTAL) { II.A.1: Disappearing Minutia if (appearing !=
		 * ILfs.APPEARING) { Ex. ? ? 0 1 0 0 0 0 Ridge flow is down and to the right,
		 * whereas actual ridge is running up and to the left. Thus: HORIZONTAL :
		 * disappearing : should be OPPOSITE the ridge flow direction. iDir += nDirs; }
		 * Otherwise: II.A.2: Appearing Minutia Ex. 0 0 0 0 1 0 ? ? Ridge flow is down
		 * and to the right, which should be same direction from which ridge is
		 * projecting. Thus: HORIZONTAL : appearing : should be the SAME as ridge flow
		 * direction. } // End if HORIZONTAL scan Otherwise: II.B: VERTICAL scan else {
		 * II.B.1: Disappearing Minutia if (appearing != ILfs.APPEARING) { Ex. ? 0 0 ? 1
		 * 0 0 0 Ridge flow is down and to the right, whereas actual ridge is running up
		 * and to the left. Thus: VERTICAL : disappearing : should be OPPOSITE the ridge
		 * flow direction. iDir += nDirs; } Otherwise: II.B.2: Appearing Minutia Ex. 0 0
		 * 0 1 ? 0 0 ? Ridge flow is down and to the right, which should be same
		 * direction the ridge is projecting. Thus: VERTICAL : appearing : should be be
		 * the SAME as ridge flow direction. } // End else VERTICAL scan } // End else
		 * Quadrant II
		 */
		if (addOffset) {
			iDir += nDirs;
		}

		/* Return resulting direction on range [0..31]. */
		return (iDir);
	}

	/**
	 * Releases a minutiae list and all minutiae it holds.
	 *
	 * <p>NIST origin: {@code free_minutiae()} in {@code minutia.c}. Each minutia is passed to
	 * {@link #freeMinutia}, the list reference is cleared and the holder is set to {@code null}.
	 *
	 * @param oMinutiae holder of the minutiae list to release; may be {@code null}. On return its referenced
	 *                  value is {@code null}.
	 */
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

	/**
	 * Releases a single minutia.
	 *
	 * <p>NIST origin: {@code free_minutia()} in {@code minutia.c}. A no-op in Java because memory is reclaimed
	 * by the garbage collector; retained for parity with the C API.
	 *
	 * @param minutia the minutia to release
	 */
	@SuppressWarnings({ "java:S1186" })
	public void freeMinutia(Minutia minutia) {
	}
}