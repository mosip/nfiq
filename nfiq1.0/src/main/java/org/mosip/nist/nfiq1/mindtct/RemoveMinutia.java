package org.mosip.nist.nfiq1.mindtct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IRemoveMinutia;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.Minutia;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java port of NIST MINDTCT {@code remove.c}: detection and removal of false minutiae.
 *
 * <p>After {@link MinutiaHelper} has detected candidate minutiae in the binarized fingerprint image, this
 * class prunes the candidates that are artifacts of noise, scars, pores or binarization, before NFIQ 1.0
 * computes its quality features. The main entry point, {@link #removeFalseMinutiaV2}, applies the following
 * tests in order: islands and lakes, holes, minutiae pointing to an INVALID block, minutiae near INVALID
 * blocks, side minutiae (removed or adjusted), hooks, overlaps, malformations and pores. Several tests also
 * edit the binary image (e.g. filling detected islands/lakes).
 *
 * <p>The class is a lazily created singleton obtained through {@link #getInstance()}. It keeps no mutable
 * per-call state, so the shared instance may be used from several threads provided the minutiae lists,
 * images and maps passed in are not shared between concurrent calls.
 */
public class RemoveMinutia extends MindTct implements IRemoveMinutia {
	/**
	 * SLF4J logger used to report errors and, when detailed logging is enabled, which minutiae are removed or
	 * adjusted.
	 */
	private static final Logger logger = LoggerFactory.getLogger(RemoveMinutia.class);
	/**
	 * Lazily created singleton instance; initialized by {@link #getInstance()}.
	 */
	private static RemoveMinutia instance;

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()} instead.
	 */
	private RemoveMinutia() {
		super();
	}

	/**
	 * Returns the shared {@link RemoveMinutia} singleton, creating it on first use.
	 *
	 * <p>The method is {@code synchronized}, so lazy creation is thread-safe.
	 *
	 * @return the process-wide {@link RemoveMinutia} instance
	 */
	public static synchronized RemoveMinutia getInstance() {
		if (instance == null) {
			synchronized (RemoveMinutia.class) {
				if (instance == null) {
					instance = new RemoveMinutia();
				}
			}
		}
		return instance;
	}

	/**
	 * Returns the {@link Defs} singleton providing numeric helpers (rounding, precision truncation).
	 *
	 * @return the shared {@link Defs} instance
	 */
	public Defs getDefs() {
		return Defs.getInstance();
	}

	/**
	 * Returns the {@link Contour} singleton used to trace feature contours.
	 *
	 * @return the shared {@link Contour} instance
	 */
	public Contour getContour() {
		return Contour.getInstance();
	}

	/**
	 * Returns the {@link MinutiaHelper} singleton used to sort, remove and join minutiae.
	 *
	 * @return the shared {@link MinutiaHelper} instance
	 */
	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}

	/**
	 * Returns the {@link Maps} singleton that manages the block Direction, Low Flow and High Curvature maps.
	 *
	 * @return the shared {@link Maps} instance
	 */
	public Maps getMap() {
		return Maps.getInstance();
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
	 * Returns the {@link ImageUtil} singleton providing image searches such as free-path and directional search.
	 *
	 * @return the shared {@link ImageUtil} instance
	 */
	public ImageUtil getImageUtil() {
		return ImageUtil.getInstance();
	}

	/**
	 * Returns the {@link LfsUtil} singleton providing LFS geometry utilities (distances, direction differences,
	 * line directions, min/max analysis).
	 *
	 * @return the shared {@link LfsUtil} instance
	 */
	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	/**
	 * Returns the {@link Loop} singleton used to detect loops, hooks and islands/lakes and to fill loops.
	 *
	 * @return the shared {@link Loop} instance
	 */
	public Loop getLoop() {
		return Loop.getInstance();
	}

	/**
	 * Detects and removes false minutiae from a list of true and false minutiae by applying a series of tests.
	 *
	 * <p>NIST origin: {@code remove_false_minutia_V2()} in {@code remove.c}. Steps, in order:
	 * <ol>
	 * <li>sort minutiae top-to-bottom, then left-to-right;</li>
	 * <li>remove minutiae on islands and lakes ({@link #removeIslandsAndLakes});</li>
	 * <li>remove minutiae on holes ({@link #removeHoles});</li>
	 * <li>remove minutiae pointing to an INVALID block ({@link #removePointingInvblockV2});</li>
	 * <li>remove minutiae near INVALID blocks ({@link #removeNearInvblocksV2});</li>
	 * <li>remove or adjust side minutiae ({@link #removeOrAdjustSideMinutiaeV2});</li>
	 * <li>remove minutiae on hooks ({@link #removeHooks});</li>
	 * <li>remove minutiae on opposite sides of an overlap ({@link #removeOverlaps});</li>
	 * <li>remove malformed minutiae ({@link #removeMalformations});</li>
	 * <li>remove minutiae on pores ({@link #removePoresV2}).</li>
	 * </ol>
	 * Processing stops at the first step returning a non-zero code.
	 *
	 * @param oMinutiae          holder of the list of true and false minutiae; pruned in place
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major; may be
	 *                           edited (e.g. islands/lakes filled)
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param map                maps object holding the block Direction Map, Low Flow Map and High Curvature Map
	 * @param mappedImageWidth   width of the maps, in blocks
	 * @param mappedImageHeight  height of the maps, in blocks
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
	public int removeFalseMinutiaV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
			final int imageHeight, Maps map, final int mappedImageWidth, final int mappedImageHeight,
			final LfsParams lfsParams) {
		int ret;
		/* 1. Sort minutiae points top-to-bottom and left-to-right. */
		if ((ret = getMinutiaHelper().sortMinutiaeTopToBottomAndThenLeftToRight(oMinutiae, imageWidth,
				imageHeight)) != ILfs.FALSE) {
			return (ret);
		}

		/* 2. Remove minutiae on lakes (filled with white pixels) and */
		/* islands (filled with black pixels), both defined by a pair of */
		/* minutia points. */
		if ((ret = removeIslandsAndLakes(oMinutiae, binarizedImageData, imageWidth, imageHeight,
				lfsParams)) != ILfs.FALSE) {
			return (ret);
		}

		/* 3. Remove minutiae on holes in the binary image defined by a */
		/* single point. */
		if ((ret = removeHoles(oMinutiae, binarizedImageData, imageWidth, imageHeight, lfsParams)) != ILfs.FALSE) {
			return (ret);
		}

		/* 4. Remove minutiae that point sufficiently close to a block with */
		/* INVALID direction. */
		if ((ret = removePointingInvblockV2(oMinutiae, map.getDirectionMap(), mappedImageWidth, mappedImageHeight,
				lfsParams)) != ILfs.FALSE) {
			return (ret);
		}

		/* 5. Remove minutiae that are sufficiently close to a block with */
		/* INVALID direction. */
		if ((ret = removeNearInvblocksV2(oMinutiae, map.getDirectionMap(), mappedImageWidth, mappedImageHeight,
				lfsParams)) != ILfs.FALSE) {
			return (ret);
		}

		/* 6. Remove or adjust minutiae that reside on the side of a ridge */
		/* or valley. */
		if ((ret = removeOrAdjustSideMinutiaeV2(oMinutiae, binarizedImageData, imageWidth, imageHeight,
				map.getDirectionMap(), mappedImageWidth, mappedImageHeight, lfsParams)) != ILfs.FALSE) {
			return (ret);
		}

		/* 7. Remove minutiae that form a hook on the side of a ridge or valley. */
		if ((ret = removeHooks(oMinutiae, binarizedImageData, imageWidth, imageHeight, lfsParams)) != ILfs.FALSE) {
			return (ret);
		}

		/* 8. Remove minutiae that are on opposite sides of an overlap. */
		if ((ret = removeOverlaps(oMinutiae, binarizedImageData, imageWidth, imageHeight, lfsParams)) != ILfs.FALSE) {
			return (ret);
		}

		/* 9. Remove minutiae that are "irregularly" shaped. */
		if ((ret = removeMalformations(oMinutiae, binarizedImageData, imageWidth, imageHeight, map.getLowFlowMap(),
				mappedImageWidth, mappedImageHeight, lfsParams)) != ILfs.FALSE) {
			return (ret);
		}

		/* 10. Remove minutiae that form long, narrow, loops in the */
		/* "unreliable" regions in the binary image. */
		if ((ret = removePoresV2(oMinutiae, binarizedImageData, imageWidth, imageHeight, map.getDirectionMap(),
				map.getLowFlowMap(), map.getHighCurveMap(), mappedImageWidth, mappedImageHeight,
				lfsParams)) != ILfs.FALSE) {
			return (ret);
		}

		return (ILfs.FALSE);
	}

	/**
	 * Removes minutia points lying on small loops (holes) around valleys.
	 *
	 * <p>NIST origin: {@code remove_holes()} in {@code remove.c}. For each bifurcation, the feature contour is
	 * checked for forming a loop of at most {@code smallLoopLen} pixels ({@link Loop#onLoop}); if so the minutia
	 * is removed.
	 *
	 * @param oMinutiae          holder of the list of true and false minutiae; pruned in place
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
	public int removeHoles(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
			final int imageHeight, LfsParams lfsParams) {
		int minutiaIndex;
		int ret;
		Minutia minutia;

		if (isShowLogs())
			logger.debug("REMOVING HOLES:");

		minutiaIndex = 0;
		/* Foreach minutia remaining in list ... */
		while (minutiaIndex < oMinutiae.get().getNum()) {
			/* Assign a temporary pointer. */
			minutia = oMinutiae.get().getList().get(minutiaIndex);
			/* If current minutia is a bifurcation ... */
			if (minutia.getType() == ILfs.BIFURCATION) {
				/* Check to see if it is on a loop of specified length (ex. 15). */
				ret = getLoop().onLoop(minutia, lfsParams.getSmallLoopLen(), binarizedImageData, imageWidth,
						imageHeight);
				/* If minutia is on a loop ... or loop test IGNORED */
				if ((ret == ILfs.LOOP_FOUND) || (ret == ILfs.IGNORE)) {
					if (isShowLogs())
						logger.debug("{},{} RM", minutia.getX(), minutia.getY());

					/* Then remove the minutia from list. */
					if ((ret = getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae)) != ILfs.FALSE) {
						/* Return error code. */
						return (ret);
					}
					/* No need to advance because next minutia has "slid" */
					/* into position pointed to by 'i'. */
				}
				/* If the minutia is NOT on a loop... */
				else if (ret == ILfs.FALSE) {
					/* Simply advance to next minutia in the list. */
					minutiaIndex++;
				}
				/* Otherwise, an ERROR occurred while looking for loop. */
				else {
					/* Return error code. */
					return (ret);
				}
			}
			/* Otherwise, the current minutia is a ridge-ending... */
			else {
				/* Advance to next minutia in the list. */
				minutiaIndex++;
			}
		}

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Detects and removes false minutiae lying on a hook (white or black).
	 *
	 * <p>NIST origin: {@code remove_hooks()} in {@code remove.c}. The list is assumed sorted top-to-bottom. Pairs
	 * of minutiae of different type that are within {@code maxRmTestDist} pixels of each other and whose
	 * directions are nearly opposite (difference greater than {@code 3 * (numDirections / 4) - 1} direction units,
	 * about 124 degrees for 16 directions) are tested with {@link Loop#onHook}
	 * over at most {@code maxHookLen} contour steps; both minutiae of a detected hook are flagged and then removed.
	 *
	 * @param oMinutiae          holder of the list of true and false minutiae; pruned in place
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; {@link ILfs#ERROR_CODE_641} (negative) on an
	 *         invalid direction difference; another negative value on system error
	 */
	public int removeHooks(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, final int imageWidth,
			final int imageHeight, final LfsParams lfsParams) {
		List<Boolean> toRemoveIndexes;
		int minutiaIndex;
		int minutiaFIndex;
		int minutiaSIndex;
		int ret;
		int deltaY;
		int fullNDirs;
		int qtrNDirs;
		int deltaDir;
		int minDeltaDir;
		Minutia minutia1, minutia2;
		double dDistance;

		if (isShowLogs())
			logger.debug("REMOVING HOOKS:");

		/* Allocate list of minutia indices that upon completion of testing */
		/* should be removed from the minutiae lists. Note: That using */
		/* initializes the list to FALSE. */
		toRemoveIndexes = new ArrayList<Boolean>(Arrays.asList(new Boolean[oMinutiae.get().getNum()]));
		Collections.fill(toRemoveIndexes, Boolean.FALSE);

		/* Compute number directions in full circle. */
		fullNDirs = lfsParams.getNumDirections() << 1;
		/* Compute number of directions in 45=(180/4) degrees. */
		qtrNDirs = lfsParams.getNumDirections() >> 2;

		/* Minimum allowable deltadir to consider joining minutia. */
		/* (The closer the deltadir is to 180 degrees, the more likely the join. */
		/* When ndirs==16, then this value is 11=(3*4)-1 == 123.75 degrees. */
		/* I chose to parameterize this threshold based on a fixed fraction of */
		/* 'ndirs' rather than on passing in a parameter in degrees and doing */
		/* the conversion. I doubt the difference matters. */
		minDeltaDir = (3 * qtrNDirs) - 1;

		if (isShowLogs())
			logger.debug("num={}, full_ndirs={}, qtr_ndirs={}", oMinutiae.get().getNum(), fullNDirs, qtrNDirs);
		minutiaFIndex = 0;
		/* Foreach primary (first) minutia (except for last one in list) ... */
		while (minutiaFIndex < oMinutiae.get().getNum() - 1) {
			/* If current first minutia not previously set to be removed. */
			if (!toRemoveIndexes.get(minutiaFIndex)) {
				if (isShowLogs())
					logger.debug("");

				/* Set first minutia to temporary pointer. */
				minutia1 = oMinutiae.get().getList().get(minutiaFIndex);
				/* Foreach secondary (second) minutia to right of first minutia ... */
				minutiaSIndex = minutiaFIndex + 1;
				while (minutiaSIndex < oMinutiae.get().getNum()) {
					/* Set second minutia to temporary pointer. */
					minutia2 = oMinutiae.get().getList().get(minutiaSIndex);

					if (isShowLogs())
						logger.debug("1:{}({},{}){} 2:{}({},{}){} ", minutiaFIndex, minutia1.getX(), minutia1.getY(),
								minutia1.getType(), minutiaSIndex, minutia2.getX(), minutia2.getY(),
								minutia2.getType());

					/* The binary image is potentially being edited during each */
					/* iteration of the secondary minutia loop, therefore */
					/* minutia pixel values may be changed. We need to catch */
					/* these events by using the next 2 tests. */

					/* If the first minutia's pixel has been previously changed... */
					if (binarizedImageData[(minutia1.getY() * imageWidth) + minutia1.getX()] != minutia1.getType()) {
						if (isShowLogs())
							logger.debug("");
						/* Then break out of secondary loop and skip to next first. */
						break;
					}

					/* If the second minutia's pixel has been previously changed... */
					if (binarizedImageData[(minutia2.getY() * imageWidth) + minutia2.getX()] != minutia2.getType()) {
						/* Set to remove second minutia. */
						toRemoveIndexes.set(minutiaSIndex, true);
					}

					/* If the second minutia not previously set to be removed. */
					if (!toRemoveIndexes.get(minutiaSIndex)) {
						/* Compute delta y between 1st & 2nd minutiae and test. */
						deltaY = minutia2.getY() - minutia1.getY();
						/* If delta y small enough (ex. < 8 pixels) ... */
						if (deltaY <= lfsParams.getMaxRmTestDist()) {
							if (isShowLogs())
								logger.debug("1DY ");

							/* Compute Euclidean distance between 1st & 2nd mintuae. */
							dDistance = getLfsUtil().distance(minutia1.getX(), minutia1.getY(), minutia2.getX(),
									minutia2.getY());
							/* If distance is NOT too large (ex. < 8 pixels) ... */
							if (dDistance <= lfsParams.getMaxRmTestDist()) {
								if (isShowLogs())
									logger.debug("2DS ");

								/* Compute "inner" difference between directions on */
								/* a full circle and test. */
								if ((deltaDir = getLfsUtil().closestDirDistance(minutia1.getDirection(),
										minutia2.getDirection(), fullNDirs)) == ILfs.INVALID_DIR) {
									getFree().free(toRemoveIndexes);
									logger.error("ERROR : removeHooks : INVALID direction");
									return (ILfs.ERROR_CODE_641);
								}
								/* If the difference between dirs is large enough ... */
								/* (the more 1st & 2nd point away from each other the */
								/* more likely they should be joined) */
								if (deltaDir > minDeltaDir) {
									if (isShowLogs())
										logger.debug("3DD ");

									/* If 1st & 2nd minutiae are NOT same type ... */
									if (minutia1.getType() != minutia2.getType()) {
										/* Check to see if pair on a hook with contour */
										/* of specified length (ex. 15 pixels) ... */

										ret = getLoop().onHook(minutia1, minutia2, lfsParams.getMaxHookLen(),
												binarizedImageData, imageWidth, imageHeight);
										/* If hook detected between pair ... */
										if (ret == ILfs.HOOK_FOUND) {
											if (isShowLogs())
												logger.debug("4HK RM");

											/* Set to remove first minutia. */
											toRemoveIndexes.set(minutiaFIndex, true);
											/* Set to remove second minutia. */
											toRemoveIndexes.set(minutiaSIndex, true);
										}
										/* If hook test IGNORED ... */
										else if (ret == ILfs.IGNORE) {
											if (isShowLogs())
												logger.debug("RM");

											/* Set to remove first minutia. */
											toRemoveIndexes.set(minutiaFIndex, true);
											/* Skip to next 1st minutia by breaking out of */
											/* inner secondary loop. */
											break;
										}
										/* If system error occurred during hook test ... */
										else if (ret < ILfs.FALSE) {
											getFree().free(toRemoveIndexes);
											return (ret);
										}
										/* Otherwise, no hook found, so skip to next */
										/* second minutia. */
										else {
											if (isShowLogs())
												logger.debug("");
										}
									} else {
										if (isShowLogs())
											logger.debug("");
									}
									/* End different type test. */
								} // End deltadir test.
								else {
									if (isShowLogs())
										logger.debug("");
								}
							} // End distance test.
							else {
								if (isShowLogs())
									logger.debug("");
							}
						}
						/* Otherwise, current 2nd too far below 1st, so skip to next */
						/* 1st minutia. */
						else {
							if (isShowLogs())
								logger.debug("");

							/* Break out of inner secondary loop. */
							break;
						} // End delta-y test.
					} // End if !to_remove[s]
					else {
						if (isShowLogs())
							logger.debug("");
					}

					/* Bump to next second minutia in minutiae list. */
					minutiaSIndex++;
				} // End secondary minutiae loop.
			} // Otherwise, first minutia already flagged to be removed.

			/* Bump to next first minutia in minutiae list. */
			minutiaFIndex++;
		} // End primary minutiae loop.

		/* Now remove all minutiae in list that have been flagged for removal. */
		/* NOTE: Need to remove the minutia from their lists in reverse */
		/* order, otherwise, indices will be off. */
		for (minutiaIndex = oMinutiae.get().getNum() - 1; minutiaIndex >= 0; minutiaIndex--) {
			/* If the current minutia index is flagged for removal ... */
			if (toRemoveIndexes.get(minutiaIndex)) {
				/* Remove the minutia from the minutiae list. */
				if ((ret = this.getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae)) != ILfs.FALSE) {
					getFree().free(toRemoveIndexes);
					return (ret);
				}
			}
		}

		/* Deallocate flag list. */
		getFree().free(toRemoveIndexes);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Removes minutia points on hooks, islands, lakes and overlaps in a single pass, editing the binary image.
	 *
	 * <p>NIST origin: {@code remove_hooks_islands_lakes_overlaps()} in {@code remove.c} (version 1 pipeline; not
	 * called by {@link #removeFalseMinutiaV2}). For nearby pairs of minutiae with nearly opposite directions:
	 * pairs of different type on a hook are removed; pairs of the same type on an island or lake are removed and
	 * the small loop is filled in the image; pairs on opposite sides of an overlap connected by a free path are
	 * removed and their features joined in the image ({@link MinutiaHelper#joinMinutia}).
	 *
	 * @param oMinutiae          holder of the list of true and false minutiae; pruned in place
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major; edited in
	 *                           place with loops filled and overlaps removed
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; {@link ILfs#ERROR_CODE_301} (negative) on an
	 *         invalid direction difference; another negative value on system error
	 */
	public int removeHooksIslandsLakesOverlaps(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
			final int imageWidth, final int imageHeight, final LfsParams lfsParams) {
		List<Boolean> toRemoveIndexes;
		int minutiaIndex;
		int firstMinutiaIndex;
		int secondMinutiaIndex;
		int ret;
		int deltaY;
		int fullNDirs;
		int qtrNDirs;
		int deltaDir;
		int minDeltaDir;
		AtomicInteger oNoOfLoop = new AtomicInteger(0);
		Minutia firstMinutia;
		Minutia secondMinutia;
		double dDistance;

		if (isShowLogs())
			logger.debug("REMOVING HOOKS, ISLANDS, LAKES, AND OVERLAPS:");

		/* Allocate list of minutia indices that upon completion of testing */
		/* should be removed from the minutiae lists. Note: That using */
		/* initializes the list to FALSE. */
		toRemoveIndexes = new ArrayList<Boolean>(Arrays.asList(new Boolean[oMinutiae.get().getNum()]));
		Collections.fill(toRemoveIndexes, Boolean.FALSE);
		/* Compute number directions in full circle. */
		fullNDirs = lfsParams.getNumDirections() << 1;
		/* Compute number of directions in 45=(180/4) degrees. */
		qtrNDirs = lfsParams.getNumDirections() >> 2;

		/* Minimum allowable deltadir to consider joining minutia. */
		/* (The closer the deltadir is to 180 degrees, the more likely the join. */
		/* When ndirs==16, then this value is 11=(3*4)-1 == 123.75 degrees. */
		/* I chose to parameterize this threshold based on a fixed fraction of */
		/* 'ndirs' rather than on passing in a parameter in degrees and doing */
		/* the conversion. I doubt the difference matters. */
		minDeltaDir = (3 * qtrNDirs) - 1;

		firstMinutiaIndex = 0;
		/* Foreach primary (first) minutia (except for last one in list) ... */
		while (firstMinutiaIndex < oMinutiae.get().getNum() - 1) {
			/* If current first minutia not previously set to be removed. */
			if (!toRemoveIndexes.get(firstMinutiaIndex)) {
				if (isShowLogs())
					logger.debug("");

				/* Set first minutia to temporary pointer. */
				firstMinutia = oMinutiae.get().getList().get(firstMinutiaIndex);
				/* Foreach secondary (second) minutia to right of first minutia ... */
				secondMinutiaIndex = firstMinutiaIndex + 1;
				while (secondMinutiaIndex < oMinutiae.get().getNum()) {
					/* Set second minutia to temporary pointer. */
					secondMinutia = oMinutiae.get().getList().get(secondMinutiaIndex);

					if (isShowLogs())
						logger.debug("1:{}({},{}){} 2:{}({},{}){} ", firstMinutiaIndex, firstMinutia.getX(),
								firstMinutia.getY(), firstMinutia.getType(), secondMinutiaIndex, secondMinutia.getX(),
								secondMinutia.getY(), secondMinutia.getType());

					/* The binary image is potentially being edited during each */
					/* iteration of the secondary minutia loop, therefore */
					/* minutia pixel values may be changed. We need to catch */
					/* these events by using the next 2 tests. */

					/* If the first minutia's pixel has been previously changed... */
					if (binarizedImageData[(firstMinutia.getY() * imageWidth) + firstMinutia.getX()] != firstMinutia
							.getType()) {
						if (isShowLogs())
							logger.debug("");
						/* Then break out of secondary loop and skip to next first. */
						break;
					}

					/* If the second minutia's pixel has been previously changed... */
					if (binarizedImageData[(secondMinutia.getY() * imageWidth) + secondMinutia.getX()] != secondMinutia
							.getType()) {
						/* Set to remove second minutia. */
						toRemoveIndexes.set(secondMinutiaIndex, true);
					}

					/* If the second minutia not previously set to be removed. */
					if (!toRemoveIndexes.get(secondMinutiaIndex)) {
						/* Compute delta y between 1st & 2nd minutiae and test. */
						deltaY = secondMinutia.getY() - firstMinutia.getY();
						/* If delta y small enough (ex. < 8 pixels) ... */
						if (deltaY <= lfsParams.getMaxRmTestDist()) {
							if (isShowLogs())
								logger.debug("1DY ");

							/* Compute Euclidean distance between 1st & 2nd mintuae. */
							dDistance = this.getLfsUtil().distance(firstMinutia.getX(), firstMinutia.getY(),
									secondMinutia.getX(), secondMinutia.getY());
							/* If distance is NOT too large (ex. < 8 pixels) ... */
							if (dDistance <= lfsParams.getMaxRmTestDist()) {
								if (isShowLogs())
									logger.debug("2DS ");

								/* Compute "inner" difference between directions on */
								/* a full circle and test. */
								if ((deltaDir = this.getLfsUtil().closestDirDistance(firstMinutia.getDirection(),
										secondMinutia.getDirection(), fullNDirs)) == ILfs.INVALID_DIR) {
									getFree().free(toRemoveIndexes);
									logger.error("ERROR : removeHooksIslandsLakesOverlaps : INVALID direction");
									return (ILfs.ERROR_CODE_301);
								}
								/* If the difference between dirs is large enough ... */
								/* (the more 1st & 2nd point away from each other the */
								/* more likely they should be joined) */
								if (deltaDir > minDeltaDir) {
									if (isShowLogs())
										logger.debug("3DD ");

									/* If 1st & 2nd minutiae are NOT same type ... */
									if (firstMinutia.getType() != secondMinutia.getType()) {
										/* Check to see if pair on a hook with contour */
										/* of specified length (ex. 15 pixels) ... */
										ret = this.getLoop().onHook(firstMinutia, secondMinutia,
												lfsParams.getMaxHookLen(), binarizedImageData, imageWidth, imageHeight);
										/* If hook detected between pair ... */
										if (ret == ILfs.HOOK_FOUND) {
											if (isShowLogs())
												logger.debug("4HK RM");

											/* Set to remove first minutia. */
											toRemoveIndexes.set(firstMinutiaIndex, true);
											/* Set to remove second minutia. */
											toRemoveIndexes.set(secondMinutiaIndex, true);
										}
										/* If hook test IGNORED ... */
										else if (ret == ILfs.IGNORE) {
											if (isShowLogs())
												logger.debug("RM");

											/* Set to remove first minutia. */
											toRemoveIndexes.set(firstMinutiaIndex, true);
											/* Skip to next 1st minutia by breaking out of */
											/* inner secondary loop. */
											break;
										}
										/* If system error occurred during hook test ... */
										else if (ret < ILfs.FALSE) {
											getFree().free(toRemoveIndexes);
											return (ret);
										}
										/* Otherwise, no hook found, so skip to next */
										/* second minutia. */
										else {
											if (isShowLogs())
												logger.debug("");
										}
									}
									/* Otherwise, pair is the same type, so test to see */
									/* if both are on an island or lake. */
									else {
										/* Check to see if pair on a loop of specified */
										/* half length (ex. 15 pixels) ... */
										AtomicInteger returnCode = new AtomicInteger(0);
										Contour contour = getLoop().onIslandLake(returnCode, oNoOfLoop, firstMinutia,
												secondMinutia, lfsParams.getMaxHalfLoop(), binarizedImageData,
												imageWidth, imageHeight);
										ret = returnCode.get();
										/* If pair is on island/lake ... */
										if (ret == ILfs.LOOP_FOUND) {
											if (isShowLogs())
												logger.debug("4IL RM");

											/* Fill the loop. */
											if ((ret = getLoop().fillLoop(contour.getContourX(), contour.getContourY(),
													oNoOfLoop.get(), binarizedImageData, imageWidth,
													imageHeight)) != ILfs.FALSE) {
												getContour().freeContour(contour);
												getFree().free(toRemoveIndexes);
												return (ret);
											}
											/* Set to remove first minutia. */
											toRemoveIndexes.set(firstMinutiaIndex, true);
											/* Set to remove second minutia. */
											toRemoveIndexes.set(secondMinutiaIndex, true);
											/* Deallocate loop contour. */
											getContour().freeContour(contour);
										}
										/* If island/lake test IGNORED ... */
										else if (ret == ILfs.IGNORE) {
											if (isShowLogs())
												logger.debug("RM");

											/* Set to remove first minutia. */
											toRemoveIndexes.set(firstMinutiaIndex, true);
											/* Skip to next 1st minutia by breaking out of */
											/* inner secondary loop. */
											break;
										}
										/* If ERROR while looking for island/lake ... */
										else if (ret < ILfs.FALSE) {
											getFree().free(toRemoveIndexes);
											return (ret);
										}
										/* Otherwise, minutia pair not on island/lake, */
										/* but might be on an overlap. */
										else {
											/* If free path exists between pair ... */
											if (getImageUtil().freePath(firstMinutia.getX(), firstMinutia.getY(),
													secondMinutia.getX(), secondMinutia.getY(), binarizedImageData,
													imageWidth, imageHeight, lfsParams) != ILfs.FALSE) {
												if (isShowLogs())
													logger.debug("4OV RM");

												/* Then assume overlap, so ... */
												/* Join first and second minutiae in image. */
												if ((ret = this.getMinutiaHelper().joinMinutia(firstMinutia,
														secondMinutia, binarizedImageData, imageWidth, imageHeight,
														ILfs.NO_BOUNDARY, ILfs.JOIN_LINE_RADIUS)) != ILfs.FALSE) {
													getFree().free(toRemoveIndexes);
													return (ret);
												}
												/* Set to remove first minutia. */
												toRemoveIndexes.set(firstMinutiaIndex, true);
												/* Set to remove second minutia. */
												toRemoveIndexes.set(secondMinutiaIndex, true);
											}
											/* Otherwise, pair not on an overlap, so skip */
											/* to next second minutia. */
											else {
												if (isShowLogs())
													logger.debug("");
											}
										} // End overlap test.
									} // End same type tests (island/lake & overlap).
								} // End deltadir test.
								else {
									if (isShowLogs())
										logger.debug("");
								}
							} // End distance test.
							else {
								if (isShowLogs())
									logger.debug("");
							}
						}
						/* Otherwise, current 2nd too far below 1st, so skip to next */
						/* 1st minutia. */
						else {
							if (isShowLogs())
								logger.debug("");

							/* Break out of inner secondary loop. */
							break;
						} // End delta-y test.
					} // End if !to_remove[s]
					else {
						if (isShowLogs())
							logger.debug("");
					}
					/* Bump to next second minutia in minutiae list. */
					secondMinutiaIndex++;
				} // End secondary minutiae loop.
			} // Otherwise, first minutia already flagged to be removed.

			/* Bump to next first minutia in minutiae list. */
			firstMinutiaIndex++;
		} // End primary minutiae loop.

		/* Now remove all minutiae in list that have been flagged for removal. */
		/* NOTE: Need to remove the minutia from their lists in reverse */
		/* order, otherwise, indices will be off. */
		for (minutiaIndex = oMinutiae.get().getNum() - 1; minutiaIndex >= 0; minutiaIndex--) {
			/* If the current minutia index is flagged for removal ... */
			if (toRemoveIndexes.get(minutiaIndex)) {
				/* Remove the minutia from the minutiae list. */
				if ((ret = this.getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae)) != ILfs.FALSE) {
					toRemoveIndexes = null;
					return (ret);
				}
			}
		}

		/* Deallocate flag list. */
		getFree().free(toRemoveIndexes);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Detects and removes false minutiae lying on a common island (black) or lake (white).
	 *
	 * <p>NIST origin: {@code remove_islands_and_lakes()} in {@code remove.c}. The list is assumed sorted
	 * top-to-bottom. Pairs of minutiae of the same type within {@code maxRmTestDist} pixels whose directions are
	 * nearly opposite are tested with {@link Loop#onIslandLake} (loop half-length {@code maxHalfLoop}); when both
	 * lie on the same small loop, the loop is filled in the binary image and both minutiae are removed.
	 *
	 * @param oMinutiae          holder of the list of true and false minutiae; pruned in place
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major; detected
	 *                           islands/lakes are filled in place
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; {@link ILfs#ERROR_CODE_611} (negative) on an
	 *         invalid direction difference; another negative value on system error
	 */
	public int removeIslandsAndLakes(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, int imageWidth,
			int imageHeight, LfsParams lfsParams) {
		int[] toRemoveIndexes;
		int minutiaIndex;
		int firstMinutiaIndex;
		int secondMinutiaIndex = 0;
		AtomicInteger ret = new AtomicInteger(0);
		int deltaY;
		int fullNDirs;
		int qtrNDirs;
		int deltaDir;
		int minDeltaDir;
		Contour contour;
		AtomicInteger nloop = new AtomicInteger(0);
		AtomicReference<Minutia> oFirstMinutia = new AtomicReference<>();
		AtomicReference<Minutia> oSecondMinutia = new AtomicReference<>();
		double dist;
		int distThresh;
		int halfLoop;

		if (isShowLogs())
			logger.debug("REMOVING ISLANDS AND LAKES:");

		distThresh = lfsParams.getMaxRmTestDist();
		halfLoop = lfsParams.getMaxHalfLoop();

		toRemoveIndexes = new int[oMinutiae.get().getNum()];

		/* Compute number directions in full circle. */
		fullNDirs = lfsParams.getNumDirections() << 1;
		/* Compute number of directions in 45=(180/4) degrees. */
		qtrNDirs = lfsParams.getNumDirections() >> 2;

		/* Minimum allowable deltadir to consider joining minutia. */
		/* (The closer the deltadir is to 180 degrees, the more likely the join. */
		/* When ndirs==16, then this value is 11=(3*4)-1 == 123.75 degrees. */
		/* I chose to parameterize this threshold based on a fixed fraction of */
		/* 'ndirs' rather than on passing in a parameter in degrees and doing */
		/* the conversion. I doubt the difference matters. */
		minDeltaDir = (3 * qtrNDirs) - 1;

		/* Foreach primary (first) minutia (except for last one in list) ... */
		firstMinutiaIndex = 0;
		while (firstMinutiaIndex < oMinutiae.get().getNum() - 1) {

			if (isShowLogs())
				logger.debug("(f = {}, s = {})", firstMinutiaIndex, secondMinutiaIndex);

			/* If current first minutia not previously set to be removed. */
			if (toRemoveIndexes[firstMinutiaIndex] != ILfs.TRUE) {
				/* Set first minutia to temporary pointer. */
				oFirstMinutia.set(oMinutiae.get().getList().get(firstMinutiaIndex));

				/* Foreach secondary minutia to right of first minutia ... */
				secondMinutiaIndex = firstMinutiaIndex + 1;
				while (secondMinutiaIndex < oMinutiae.get().getNum()) {
					/* Set second minutia to temporary pointer. */
					oSecondMinutia.set(oMinutiae.get().getList().get(secondMinutiaIndex));

					/* If the secondary minutia is desired type ... */
					if (oSecondMinutia.get().getType() == oFirstMinutia.get().getType()) {
						if (isShowLogs())
							logger.debug("1:{}({},{}){} 2:{}({},{}){} ", firstMinutiaIndex, oFirstMinutia.get().getX(),
									oFirstMinutia.get().getY(), oFirstMinutia.get().getType(), secondMinutiaIndex,
									oSecondMinutia.get().getX(), oSecondMinutia.get().getY(),
									oSecondMinutia.get().getType());

						/* The binary image is potentially being edited during */
						/* each iteration of the secondary minutia loop, */
						/* therefore minutia pixel values may be changed. We */
						/* need to catch these events by using the next 2 tests. */

						/* If the first minutia's pixel has been previously */
						/* changed... */
						if (binarizedImageData[(oFirstMinutia.get().getY() * imageWidth)
								+ oFirstMinutia.get().getX()] != oFirstMinutia.get().getType()) {
							if (isShowLogs())
								logger.debug("");
							/* Then break out of secondary loop and skip to next */
							/* first. */
							break;
						}

						/* If the second minutia's pixel has been previously */
						/* changed... */
						if (binarizedImageData[(oSecondMinutia.get().getY() * imageWidth)
								+ oSecondMinutia.get().getX()] != oSecondMinutia.get().getType()) {
							/* Set to remove second minutia. */
							toRemoveIndexes[secondMinutiaIndex] = ILfs.TRUE;
						}

						/* If the second minutia not previously set to be removed. */
						if (toRemoveIndexes[secondMinutiaIndex] == ILfs.FALSE) {
							/* Compute delta y between 1st & 2nd minutiae and test. */
							deltaY = oSecondMinutia.get().getY() - oFirstMinutia.get().getY();
							/* If delta y small enough (ex. <16 pixels)... */
							if (deltaY <= distThresh) {
								if (isShowLogs())
									logger.debug("1DY ");

								/* Compute Euclidean distance between 1st & 2nd */
								/* mintuae. */
								dist = getLfsUtil().distance(oFirstMinutia.get().getX(), oFirstMinutia.get().getY(),
										oSecondMinutia.get().getX(), oSecondMinutia.get().getY());

								/* If distance is NOT too large (ex. <16 pixels)... */
								if (dist <= distThresh) {
									if (isShowLogs())
										logger.debug("2DS ");

									/* Compute "inner" difference between directions */
									/* on a full circle and test. */
									if ((deltaDir = getLfsUtil().closestDirDistance(oFirstMinutia.get().getDirection(),
											oSecondMinutia.get().getDirection(), fullNDirs)) == ILfs.INVALID_DIR) {
										toRemoveIndexes = null;
										logger.error("ERROR : removeIslandsAndLakes : INVALID direction");
										return (ILfs.ERROR_CODE_611);
									}

									/* If the difference between dirs is large */
									/* enough ... */
									/* (the more 1st & 2nd point away from each */
									/* other the more likely they should be joined) */
									if (deltaDir > minDeltaDir) {
										if (isShowLogs())
											logger.debug("3DD ");

										/* Pair is the same type, so test to see */
										/* if both are on an island or lake. */

										/* Check to see if pair on a loop of specified */
										/* half length (ex. 30 pixels) ... */
										contour = getLoop().onIslandLake(ret, nloop, oFirstMinutia.get(),
												oSecondMinutia.get(), halfLoop, binarizedImageData, imageWidth,
												imageHeight);

										/* If pair is on island/lake ... */
										if (ret.get() == ILfs.LOOP_FOUND) {
											if (isShowLogs())
												logger.debug("4IL RM");

											/* Fill the loop. */
											ret.set(getLoop().fillLoop(contour.getContourX(), contour.getContourY(),
													nloop.get(), binarizedImageData, imageWidth, imageHeight));
											if (ret.get() != ILfs.FALSE) {
												getContour().freeContour(contour);
												getFree().free(toRemoveIndexes);
												return (ret.get());
											}
											/* Set to remove first minutia. */
											toRemoveIndexes[firstMinutiaIndex] = ILfs.TRUE;
											/* Set to remove second minutia. */
											toRemoveIndexes[secondMinutiaIndex] = ILfs.TRUE;
											;
											/* Deallocate loop contour. */
											getContour().freeContour(contour);
										}
										/* If island/lake test IGNORED ... */
										else if (ret.get() == ILfs.IGNORE) {
											if (isShowLogs())
												logger.debug("RM");

											/* Set to remove first minutia. */
											toRemoveIndexes[firstMinutiaIndex] = ILfs.TRUE;
											/* Skip to next 1st minutia by breaking out */
											/* of inner secondary loop. */
											break;
										}
										/* If ERROR while looking for island/lake ... */
										else if (ret.get() < ILfs.FALSE) {
											getFree().free(toRemoveIndexes);
											return (ret.get());
										} else {
											if (isShowLogs())
												logger.debug("");
										}
									} // End deltadir test.
									else {
										if (isShowLogs())
											logger.debug("");
									}
								} // End distance test.
								else {
									if (isShowLogs())
										logger.debug("");
								}
							}
							/* Otherwise, current 2nd too far below 1st, so skip to */
							/* next 1st minutia. */
							else {
								if (isShowLogs())
									logger.debug("");
								/* Break out of inner secondary loop. */
								break;
							} // End delta-y test.
						} // End if !to_remove[s]
						else {
							if (isShowLogs())
								logger.debug("");
						}
					} // End if 2nd not desired type

					/* Bump to next second minutia in minutiae list. */
					secondMinutiaIndex++;
				} // End secondary minutiae loop.
			} // Otherwise, first minutia already flagged to be removed.
			/* Bump to next first minutia in minutiae list. */
			firstMinutiaIndex++;
		} /* End primary minutiae loop. */

		/* Now remove all minutiae in list that have been flagged for removal. */
		/* NOTE: Need to remove the minutia from their lists in reverse */
		/* order, otherwise, indices will be off. */
		for (minutiaIndex = oMinutiae.get().getNum() - 1; minutiaIndex >= 0; minutiaIndex--) {
			/* If the current minutia index is flagged for removal ... */
			if (toRemoveIndexes[minutiaIndex] == ILfs.TRUE) {
				/* Remove the minutia from the minutiae list. */
				ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
				if (ret.get() != ILfs.FALSE) {
					getFree().free(toRemoveIndexes);
					return (ret.get());
				}
			}
		}

		/* Deallocate flag list. */
		getFree().free(toRemoveIndexes);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Detects and removes "irregularly" shaped (malformed) minutia points.
	 *
	 * <p>NIST origin: {@code remove_malformations()} in {@code remove.c}. For each minutia, the contour is traced
	 * {@code malformationSteps2} steps in both directions; incomplete contours or loops cause removal.
	 * Irregularity is measured across the feature interior at two progressive contour points
	 * ({@code malformationSteps1} and {@code malformationSteps2}). In a LOW RIDGE FLOW block the minutia is
	 * removed if the distance at the second point exceeds {@code maxMalformationDist}. Otherwise, if a pixel of
	 * opposite color is found along the line between the second pair of points, the minutia is removed when the
	 * ratio of the two distances exceeds {@code minMalformationRatio}.
	 *
	 * @param oMinutiae          holder of the list of true and false minutiae; pruned in place
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param oLowFlowMap        block map of image blocks flagged as LOW RIDGE FLOW
	 * @param mappedImageWidth   width of the map, in blocks
	 * @param mappedImageHeight  height of the map, in blocks
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
	@SuppressWarnings("java:S1066")
	public int removeMalformations(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, int imageWidth,
			int imageHeight, AtomicIntegerArray oLowFlowMap, int mappedImageWidth, int mappedImageHeight,
			LfsParams lfsParams) {
		int minutiaIndex, j;
		AtomicInteger ret = new AtomicInteger(0);
		AtomicReference<Minutia> oMinutia = new AtomicReference<>();
		Contour contour = null;
		AtomicInteger oNoOfContour = new AtomicInteger(0);

		int ax1;
		int ay1;
		int bx1;
		int by1;
		int ax2;
		int ay2;
		int bx2;
		int by2;
		int[] xList;
		int[] yList;
		AtomicInteger num = new AtomicInteger(0);
		double aDist;
		double bDist;
		double ratio;
		int fmapval;
		int removed;
		int blockX;
		int blockY;

		if (isShowLogs())
			logger.debug("REMOVING MALFORMATIONS:");

		for (minutiaIndex = oMinutiae.get().getNum() - 1; minutiaIndex >= 0; minutiaIndex--) {
			oMinutia.set(oMinutiae.get().getList().get(minutiaIndex));

			contour = getContour().traceContour(ret, oNoOfContour, lfsParams.getMalformationSteps2(),
					oMinutia.get().getX(), oMinutia.get().getY(), oMinutia.get().getX(), oMinutia.get().getY(),
					oMinutia.get().getEx(), oMinutia.get().getEy(), ILfs.SCAN_COUNTER_CLOCKWISE, binarizedImageData,
					imageWidth, imageHeight);
			/* If system error occurred during trace ... */
			if (ret.get() < ILfs.FALSE) {
				/* Return error code. */
				return (ret.get());
			}

			/* If trace was not possible OR loop found OR */
			/* contour is incomplete ... */
			if ((ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.LOOP_FOUND)
					|| (oNoOfContour.get() < lfsParams.getMalformationSteps2())) {
				/* If contour allocated and returned ... */
				if ((ret.get() == ILfs.LOOP_FOUND) || (oNoOfContour.get() < lfsParams.getMalformationSteps2())) {
					/* Deallocate the contour. */
					getContour().freeContour(contour);
				}

				if (isShowLogs())
					logger.debug("{},{} RMA", oMinutia.get().getX(), oMinutia.get().getY());

				/* Then remove the minutia. */
				ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
				if (ret.get() != ILfs.FALSE) {
					/* If system error, return error code. */
					return (ret.get());
				}
			}
			/* Otherwise, traced contour is complete. */
			else {
				/* Store 'A1' contour point. */
				ax1 = contour.getContourX().get(lfsParams.getMalformationSteps1() - 1);
				ay1 = contour.getContourY().get(lfsParams.getMalformationSteps1() - 1);

				/* Store 'B1' contour point. */
				bx1 = contour.getContourX().get(lfsParams.getMalformationSteps2() - 1);
				by1 = contour.getContourY().get(lfsParams.getMalformationSteps2() - 1);

				/* Deallocate the contours. */
				getContour().freeContour(contour);

				contour = getContour().traceContour(ret, oNoOfContour, lfsParams.getMalformationSteps2(),
						oMinutia.get().getX(), oMinutia.get().getY(), oMinutia.get().getX(), oMinutia.get().getY(),
						oMinutia.get().getEx(), oMinutia.get().getEy(), ILfs.SCAN_CLOCKWISE, binarizedImageData,
						imageWidth, imageHeight);
				/* If system error occurred during trace ... */
				if (ret.get() < ILfs.FALSE) {
					/* Return error code. */
					return (ret.get());
				}

				/* If trace was not possible OR loop found OR */
				/* contour is incomplete ... */
				if ((ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.LOOP_FOUND)
						|| (oNoOfContour.get() < lfsParams.getMalformationSteps2())) {
					/* If contour allocated and returned ... */
					if ((ret.get() == ILfs.LOOP_FOUND) || (oNoOfContour.get() < lfsParams.getMalformationSteps2())) {
						/* Deallocate the contour. */
						getContour().freeContour(contour);
					}

					if (isShowLogs())
						logger.debug("{},{} RMB", oMinutia.get().getX(), oMinutia.get().getY());

					/* Then remove the minutia. */
					ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
					if (ret.get() != ILfs.FALSE) {
						/* If system error, return error code. */
						return (ret.get());
					}
				}
				/* Otherwise, traced contour is complete. */
				else {
					/* Store 'A2' contour point. */
					ax2 = contour.getContourX().get(lfsParams.getMalformationSteps1() - 1);
					ay2 = contour.getContourY().get(lfsParams.getMalformationSteps1() - 1);

					/* Store 'B2' contour point. */
					bx2 = contour.getContourX().get(lfsParams.getMalformationSteps2() - 1);
					by2 = contour.getContourY().get(lfsParams.getMalformationSteps2() - 1);

					/* Deallocate the contour. */
					getContour().freeContour(contour);

					/* Compute distances along A & B paths. */
					aDist = getLfsUtil().distance(ax1, ay1, ax2, ay2);
					bDist = getLfsUtil().distance(bx1, by1, bx2, by2);

					/* Compute block coords from minutia's pixel location. */
					blockX = oMinutia.get().getX() / lfsParams.getBlockOffsetSize();
					blockY = oMinutia.get().getY() / lfsParams.getBlockOffsetSize();

					removed = ILfs.FALSE;

					/* Check to see if distances are not zero. */
					if ((aDist == 0.0) || (bDist == 0.0)) {
						/* Remove the malformation minutia. */
						if (isShowLogs())
							logger.debug("{},{} RMMAL1", oMinutia.get().getX(), oMinutia.get().getY());

						ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
						if (ret.get() != ILfs.FALSE) {
							/* If system error, return error code. */
							return (ret.get());
						}
						removed = ILfs.TRUE;
					}

					if (removed == ILfs.FALSE) {
						/* Determine if minutia is in LOW RIDGE FLOW block. */
						fmapval = oLowFlowMap.get((blockY * mappedImageWidth) + blockX);
						if (fmapval == ILfs.TRUE) {
							/* If in LOW RIDGE LFOW, conduct a cursory distance test. */
							/* Need to test this out! */
							if (bDist > lfsParams.getMaxMalformationDist()) {
								/* Remove the malformation minutia. */
								if (isShowLogs())
									logger.debug("{},{} RMMAL2", oMinutia.get().getX(), oMinutia.get().getY());

								ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
								if (ret.get() != ILfs.FALSE) {
									/* If system error, return error code. */
									return (ret.get());
								}
								removed = ILfs.TRUE;
							}
						}
					}

					if (removed == ILfs.FALSE) {
						int asize = Math.max(Math.abs(bx2 - bx1) + 2, Math.abs(by2 - by1) + 2);
						xList = new int[asize];
						yList = new int[asize];
						/* Compute points on line between the points A & B. */
						ret.set(Line.getInstance().linePoints(xList, yList, num, bx1, by1, bx2, by2));
						if (ret.get() != ILfs.FALSE) {
							return (ret.get());
						}
						/* Foreach remaining point along line segment ... */
						for (j = 0; j < num.get(); j++) {
							/* If B path contains pixel opposite minutia type ... */
							if (binarizedImageData[(yList[j] * imageWidth) + xList[j]] != oMinutia.get().getType()) {
								if (aDist != 0.0) {
									/* Compute ratio of A & B path lengths. */
									ratio = bDist / aDist;
									/* Need to truncate precision so that answers are */
									/* consistent on different computer architectures. */
									ratio = getDefs().truncDoublePrecision(ratio, ILfs.TRUNC_SCALE);
									/* If the B path is sufficiently longer than A path ... */
									if (ratio > lfsParams.getMinMalformationRatio()) {
										/* Remove the malformation minutia. */
										/* Then remove the minutia. */
										if (isShowLogs())
											logger.debug("{},{} RMMAL3", oMinutia.get().getX(), oMinutia.get().getY());

										ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
										if (ret.get() != ILfs.FALSE) {
											/* If system error, return error code. */
											return (ret.get());
										}
										/* Break out of FOR loop. */
										break;
									}
								}
							}
						}
					}
				}
			}
		}

		return ILfs.FALSE;
	}

	/**
	 * Removes minutia points that are too close to a block with INVALID ridge flow or to the edge of the image.
	 *
	 * <p>NIST origin: {@code remove_near_invblock_V2()} in {@code remove.c}. A minutia lying within
	 * {@code invBlockMargin} pixels of its block boundary is checked against the neighboring block(s) in that
	 * direction (including diagonals). If such a neighbor lies outside the image, the minutia is removed; if the
	 * neighbor is INVALID, the minutia is removed when that neighbor has fewer than {@code rmValidNbrMin} valid
	 * neighbors of its own.
	 *
	 * @param oMinutiae         holder of the list of true and false minutiae; pruned in place
	 * @param directionMap      block map of directional ridge flow ({@link ILfs#INVALID_DIR} for invalid blocks)
	 * @param mappedImageWidth  width of the map, in blocks
	 * @param mappedImageHeight height of the map, in blocks
	 * @param lfsParams         parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; {@link ILfs#ERROR_CODE_620} (negative) if
	 *         {@code invBlockMargin} exceeds half the block size; another negative value on system error
	 */
	public int removeNearInvblocksV2(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray directionMap,
			final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams) {
		int minutiaIndex;
		int ret;
		int ni;
		int nbx;
		int nby;
		int nvalid;
		int ix;
		int iy;
		int sbi;
		int ebi;
		int bx;
		int by;
		int px;
		int py;
		boolean removed;
		Minutia minutia;
		int lowMargin;
		int highMargin;

		/* The next 2 lookup tables are indexed by 'ix' and 'iy'. */
		/* When a feature pixel lies within a 6-pixel margin of a */
		/* block, this routine examines neighboring blocks to */
		/* determine appropriate actions. */
		/* 'ix' may take on values: */
		/* 0 == x-pixel coord in leftmost margin */
		/* 1 == x-pixel coord in middle of block */
		/* 2 == x-pixel coord in rightmost margin */
		/* 'iy' may take on values: */
		/* 0 == y-pixel coord in topmost margin */
		/* 1 == y-pixel coord in middle of block */
		/* 2 == y-pixel coord in bottommost margin */
		/* Given (ix, iy): */
		/* 'startblk[ix][iy]' == starting neighbor index (sbi) */
		/* 'endblk[ix][iy]' == ending neighbor index (ebi) */
		/* so that neighbors begin to be analized from index */
		/* 'sbi' to 'ebi'. */
		/* Ex. (ix, iy) = (2, 0) */
		/* ix==2 ==> x-pixel coord in rightmost margin */
		/* iy==0 ==> y-pixel coord in topmost margin */
		/* X - marks the region in the current block */
		/* corresponding to (ix=2, iy=0). */
		/* sbi = 0 = startblk[2][0] */
		/* ebi = 2 = endblk[2][0] */
		/* so neighbors are analized on index range [0..2] */
		/* | */
		/* nbr block 0 | nbr block 1 */
		/* --------------------------+------------ */
		/* top margin | X | */
		/* _._._._._._._._._._._._._.| */
		/* | | */
		/* current block .r m| nbr block 2 */
		/* |i a| */
		/* .g g| */
		/* |h i| */
		/* .t n| */
		/* | | */

		/* LUT for starting neighbor index given (ix, iy). */
		byte startblock[] = { 6, 0, 0, 6, -1, 2, 4, 4, 2 };
		/* LUT for ending neighbor index given (ix, iy). */
		byte endblock[] = { 8, 0, 2, 6, -1, 2, 6, 4, 4 };

		/* Pixel coord offsets specifying the order in which neighboring */
		/* blocks are searched. The current block is in the middle of */
		/* 8 surrounding neighbors. The following illustrates the order */
		/* of neighbor indices. (Note that 9 overlaps 1.) */
		/* 8 */
		/* 7 0 1 */
		/* 6 C 2 */
		/* 5 4 3 */
		/*                                                               */
		/* 0 1 2 3 4 5 6 7 8 */
		byte blockdx[] = { 0, 1, 1, 1, 0, -1, -1, -1, 0 }; /* Delta-X */
		byte blockdy[] = { -1, -1, 0, 1, 1, 1, 0, -1, -1 }; /* Delta-Y */

		if (isShowLogs())
			logger.debug("REMOVING MINUTIA NEAR INVALID BLOCKS:");

		/* If the margin covers more than the entire block ... */
		if (lfsParams.getInvBlockMargin() > (lfsParams.getBlockOffsetSize() >> 1)) {
			/* Then treat this as an error. */
			logger.error("ERROR : removeNearInvblocksV2 : margin too large for blocksize");
			return (ILfs.ERROR_CODE_620);
		}

		/* Compute the low and high pixel margin boundaries (ex. 6 pixels wide) */
		/* in the block. */
		lowMargin = lfsParams.getInvBlockMargin();
		highMargin = lfsParams.getBlockOffsetSize() - lfsParams.getInvBlockMargin() - 1;

		minutiaIndex = 0;
		/* Foreach minutia remaining in the list ... */
		while (minutiaIndex < oMinutiae.get().getNum()) {
			/* Assign temporary minutia pointer. */
			minutia = oMinutiae.get().getList().get(minutiaIndex);

			/* Compute block coords from minutia's pixel location. */
			bx = minutia.getX() / lfsParams.getBlockOffsetSize();
			by = minutia.getY() / lfsParams.getBlockOffsetSize();

			/* Compute pixel offset into the image block corresponding to the */
			/* minutia's pixel location. */
			/* NOTE: The margins used here will not necessarily correspond to */
			/* the actual block boundaries used to compute the map values. */
			/* This will be true when the image width and/or height is not an */
			/* even multiple of 'blocksize' and we are processing minutia */
			/* located in the right-most column (or bottom-most row) of */
			/* blocks. I don't think this will pose a problem in practice. */
			px = minutia.getX() % lfsParams.getBlockOffsetSize();
			py = minutia.getY() % lfsParams.getBlockOffsetSize();

			/* Determine if x pixel offset into the block is in the margins. */
			/* If x pixel offset is in left margin ... */
			if (px < lowMargin) {
				ix = 0;
			}
			/* If x pixel offset is in right margin ... */
			else if (px > highMargin) {
				ix = 2;
			}
			/* Otherwise, x pixel offset is in middle of block. */
			else {
				ix = 1;
			}

			/* Determine if y pixel offset into the block is in the margins. */
			/* If y pixel offset is in top margin ... */
			if (py < lowMargin) {
				iy = 0;
			}
			/* If y pixel offset is in bottom margin ... */
			else if (py > highMargin) {
				iy = 2;
			}
			/* Otherwise, y pixel offset is in middle of block. */
			else {
				iy = 1;
			}

			/* Set remove flag to FALSE. */
			removed = false;

			/* If one of the minutia's pixel offsets is in a margin ... */
			if ((ix != 1) || (iy != 1)) {
				/* Compute the starting neighbor block index for processing. */
				sbi = startblock[(iy * 3) + ix];
				/* Compute the ending neighbor block index for processing. */
				ebi = endblock[(iy * 3) + ix];

				/* Foreach neighbor in the range to be processed ... */
				for (ni = sbi; ni <= ebi; ni++) {
					/* Compute the neighbor's block coords relative to */
					/* the block the current minutia is in. */
					nbx = bx + blockdx[ni];
					nby = by + blockdy[ni];

					/* If neighbor's block coords are outside of map boundaries... */
					if ((nbx < 0) || (nbx >= mappedImageWidth) || (nby < 0) || (nby >= mappedImageHeight)) {
						if (isShowLogs())
							logger.debug("{},{} RM1", minutia.getX(), minutia.getY());

						/* Then the minutia is in a margin adjacent to the edge of */
						/* the image. */
						/* NOTE: This is true when the image width and/or height */
						/* is an even multiple of blocksize. When the image is not */
						/* an even multiple, then some minutia may not be detected */
						/* as being in the margin of "the image" (not the block). */
						/* In practice, I don't think this will impact performance. */
						if ((ret = getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae)) != ILfs.FALSE) {
							/* If system error occurred while removing minutia, */
							/* then return error code. */
							return (ret);
						}
						/* Set remove flag to TURE. */
						removed = true;
						/* Break out of neighboring block loop. */
						break;
					}
					/* If the neighboring block has INVALID direction ... */
					else if (directionMap.get((nby * mappedImageWidth) + nbx) == ILfs.INVALID_DIR) {
						/* Count the number of valid blocks neighboring */
						/* the current neighbor. */
						nvalid = getMap().numValid8Nbrs(directionMap, nbx, nby, mappedImageWidth, mappedImageHeight);
						/* If the number of valid neighbors is < threshold */
						/* (ex. 7)... */
						if (nvalid < lfsParams.getRmValidNbrMin()) {
							if (isShowLogs())
								logger.debug("{},{} RM2", minutia.getX(), minutia.getY());

							/* Then remove the current minutia from the list. */
							if ((ret = getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae)) != ILfs.FALSE) {
								/* If system error occurred while removing minutia, */
								/* then return error code. */
								return (ret);
							}
							/* Set remove flag to TURE. */
							removed = true;
							/* Break out of neighboring block loop. */
							break;
						}
						/* Otherwise enough valid neighbors, so don't remove minutia */
						/* based on this neighboring block. */
					}
					/* Otherwise neighboring block has valid direction, */
					/* so don't remove minutia based on this neighboring block. */
				}
			} // Otherwise not in margin, so skip to next minutia in list.

			/* If current minutia not removed ... */
			if (!removed) {
				/* Advance to the next minutia in the list. */
				minutiaIndex++;
			}
			/* Otherwise the next minutia has slid into the spot where current */
			/* minutia was removed, so don't bump minutia index. */
		} // End minutia loop

		/* Return normally. */
		return ILfs.FALSE;
	}

	/**
	 * Removes minutia points that are relatively close, in the direction opposite the minutia, to a block with
	 * INVALID ridge flow.
	 *
	 * <p>NIST origin: {@code remove_pointing_invblock_V2()} in {@code remove.c}. Each minutia point is translated
	 * {@code transDirPixel} pixels opposite to its direction; if the translated point falls in an INVALID block,
	 * the minutia is removed.
	 *
	 * @param oMinutiae         holder of the list of true and false minutiae; pruned in place
	 * @param directionMap      block map of directional ridge flow ({@link ILfs#INVALID_DIR} for invalid blocks)
	 * @param mappedImageWidth  width of the map, in blocks
	 * @param mappedImageHeight height of the map, in blocks
	 * @param lfsParams         parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
	public int removePointingInvblockV2(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray directionMap,
			final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams) {
		int minutiaIndex, ret;
		int deltaX, deltaY, dMapValue;
		int nx, ny, bx, by;
		Minutia minutia;
		double piFactor, theta;
		double dx, dy;

		if (isShowLogs())
			logger.debug("REMOVING MINUTIA POINTING TO INVALID BLOCKS:");

		/* Compute factor for converting integer directions to radians. */
		piFactor = ILfs.M_PI / lfsParams.getNumDirections();

		minutiaIndex = 0;
		/* Foreach minutia remaining in list ... */
		while (minutiaIndex < oMinutiae.get().getNum()) {
			/* Set temporary minutia pointer. */
			minutia = oMinutiae.get().getList().get(minutiaIndex);
			/* Convert minutia's direction to radians. */
			theta = minutia.getDirection() * piFactor;
			/* Compute translation offsets (ex. 6 pixels). */
			dx = Math.sin(theta) * lfsParams.getTransDirPixel();
			dy = Math.cos(theta) * lfsParams.getTransDirPixel();
			/* Need to truncate precision so that answers are consistent */
			/* on different computer architectures when rounding doubles. */
			dx = getDefs().truncDoublePrecision(dx, ILfs.TRUNC_SCALE);
			dy = getDefs().truncDoublePrecision(dy, ILfs.TRUNC_SCALE);
			deltaX = getDefs().sRound(dx);
			deltaY = getDefs().sRound(dy);
			/* Translate the minutia's coords. */
			nx = minutia.getX() - deltaX;
			ny = minutia.getY() + deltaY;
			/* Convert pixel coords to NMAP block coords. */
			bx = (nx / lfsParams.getBlockOffsetSize());
			by = (ny / lfsParams.getBlockOffsetSize());
			/* The translation could move the point out of image boundaries, */
			/* and therefore the corresponding block coords can be out of */
			/* IMAP boundaries, so limit the block coords to within boundaries. */
			bx = Math.max(0, bx);
			bx = Math.min(mappedImageWidth - 1, bx);
			by = Math.max(0, by);
			by = Math.min(mappedImageHeight - 1, by);

			/* Get corresponding block's ridge flow direction. */
			dMapValue = getPixelValueFromAtomicArray(directionMap, bx, by, mappedImageWidth, mappedImageHeight);
			/* If the NMAP value of translated minutia point is INVALID ... */
			if (dMapValue == ILfs.INVALID_DIR) {
				if (isShowLogs())
					logger.debug("{},{} RM", minutia.getX(), minutia.getY());

				/* Remove the minutia from the minutiae list. */
				if ((ret = getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae)) != ILfs.FALSE) {
					return (ret);
				}
				/* No need to advance because next minutia has slid into slot. */
			} else {
				/* Advance to next minutia in list. */
				minutiaIndex++;
			}
		}

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Detects and removes false minutiae lying on opposite sides of an overlap.
	 *
	 * <p>NIST origin: {@code remove_overlaps()} in {@code remove.c}. The list is assumed sorted top-to-bottom.
	 * Pairs of minutiae of the same type within {@code maxOverlapDist} pixels, with nearly opposite directions,
	 * where the line joining them is compatible with their directions (or they are within
	 * {@code maxOverlapJoinDist}) and a free path exists between them, are both removed. Unlike
	 * {@link #removeHooksIslandsLakesOverlaps}, the binary image is NOT edited.
	 *
	 * @param oMinutiae          holder of the list of true and false minutiae; pruned in place
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; {@link ILfs#ERROR_CODE_651} (negative) on an
	 *         invalid direction difference; another negative value on system error
	 */
	public int removeOverlaps(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, int imageWidth,
			int imageHeight, LfsParams lfsParams) {
		int[] toRemoveIndexes;
		int minutiaIndex;
		int firstMinutiaIndex;
		int secondMinutiaIndex;
		AtomicInteger ret = new AtomicInteger(0);
		int deltaY;
		int fullNDirs;
		int qtrNDirs;
		int deltaDir;
		int minDeltaDir;
		AtomicReference<Minutia> oFirstMinutia = new AtomicReference<>();
		AtomicReference<Minutia> oSecondMinutia = new AtomicReference<>();
		double dDistance;
		int joinDir;
		int opp1Dir;
		int halfNDirs;

		if (isShowLogs())
			logger.debug("REMOVING OVERLAPS:");

		/* Allocate list of minutia indices that upon completion of testing */
		/* should be removed from the minutiae lists. Note: That using */
		/* "calloc" initializes the list to FALSE. */
		toRemoveIndexes = new int[oMinutiae.get().getNum()];

		/* Compute number directions in full circle. */
		fullNDirs = lfsParams.getNumDirections() << 1;
		/* Compute number of directions in 45=(180/4) degrees. */
		qtrNDirs = lfsParams.getNumDirections() >> 2;
		/* Compute number of directions in 90=(180/2) degrees. */
		halfNDirs = lfsParams.getNumDirections() >> 1;

		/* Minimum allowable deltadir to consider joining minutia. */
		/* (The closer the deltadir is to 180 degrees, the more likely the join. */
		/* When ndirs==16, then this value is 11=(3*4)-1 == 123.75 degrees. */
		/* I chose to parameterize this threshold based on a fixed fraction of */
		/* 'ndirs' rather than on passing in a parameter in degrees and doing */
		/* the conversion. I doubt the difference matters. */
		minDeltaDir = (3 * qtrNDirs) - 1;

		firstMinutiaIndex = 0;
		/* Foreach primary (first) minutia (except for last one in list) ... */
		while (firstMinutiaIndex < oMinutiae.get().getNum() - 1) {
			/* If current first minutia not previously set to be removed. */
			if (toRemoveIndexes[firstMinutiaIndex] != ILfs.TRUE) {
				if (isShowLogs())
					logger.debug("");

				/* Set first minutia to temporary pointer. */
				oFirstMinutia.set(oMinutiae.get().getList().get(firstMinutiaIndex));
				/* Foreach secondary (second) minutia to right of first minutia ... */
				secondMinutiaIndex = firstMinutiaIndex + 1;
				while (secondMinutiaIndex < oMinutiae.get().getNum()) {
					/* Set second minutia to temporary pointer. */
					oSecondMinutia.set(oMinutiae.get().getList().get(secondMinutiaIndex));

					if (isShowLogs())
						logger.debug("1:{}({},{}){} 2:{}({},{}){} ", firstMinutiaIndex, oFirstMinutia.get().getX(),
								oFirstMinutia.get().getY(), oFirstMinutia.get().getType(), secondMinutiaIndex,
								oSecondMinutia.get().getX(), oSecondMinutia.get().getY(),
								oSecondMinutia.get().getType());

					/* The binary image is potentially being edited during each */
					/* iteration of the secondary minutia loop, therefore */
					/* minutia pixel values may be changed. We need to catch */
					/* these events by using the next 2 tests. */

					/* If the first minutia's pixel has been previously changed... */
					if (binarizedImageData[(oFirstMinutia.get().getY() * imageWidth)
							+ oFirstMinutia.get().getX()] != oFirstMinutia.get().getType()) {
						if (isShowLogs())
							logger.debug("");
						/* Then break out of secondary loop and skip to next first. */
						break;
					}

					/* If the second minutia's pixel has been previously changed... */
					if (binarizedImageData[(oSecondMinutia.get().getY() * imageWidth)
							+ oSecondMinutia.get().getX()] != oSecondMinutia.get().getType()) {
						/* Set to remove second minutia. */
						toRemoveIndexes[secondMinutiaIndex] = ILfs.TRUE;
					}

					/* If the second minutia not previously set to be removed. */
					if (toRemoveIndexes[secondMinutiaIndex] != ILfs.TRUE) {
						/* Compute delta y between 1st & 2nd minutiae and test. */
						deltaY = oSecondMinutia.get().getY() - oFirstMinutia.get().getY();
						/* If delta y small enough (ex. < 8 pixels) ... */
						if (deltaY <= lfsParams.getMaxOverlapDist()) {
							if (isShowLogs())
								logger.debug("1DY ");

							/* Compute Euclidean distance between 1st & 2nd mintuae. */
							dDistance = getLfsUtil().distance(oFirstMinutia.get().getX(), oFirstMinutia.get().getY(),
									oSecondMinutia.get().getX(), oSecondMinutia.get().getY());
							/* If distance is NOT too large (ex. < 8 pixels) ... */
							if (dDistance <= lfsParams.getMaxOverlapDist()) {
								if (isShowLogs())
									logger.debug("2DS ");

								/* Compute "inner" difference between directions on */
								/* a full circle and test. */
								if ((deltaDir = getLfsUtil().closestDirDistance(oFirstMinutia.get().getDirection(),
										oSecondMinutia.get().getDirection(), fullNDirs)) == ILfs.INVALID_DIR) {
									toRemoveIndexes = null;
									logger.error("ERROR : removeOverlaps : INVALID direction");
									return (ILfs.ERROR_CODE_651);
								}
								/* If the difference between dirs is large enough ... */
								/* (the more 1st & 2nd point away from each other the */
								/* more likely they should be joined) */
								if (deltaDir > minDeltaDir) {
									if (isShowLogs())
										logger.debug("3DD ");

									/* If 1st & 2nd minutiae are same type ... */
									if (oFirstMinutia.get().getType() == oSecondMinutia.get().getType()) {
										/* Test to see if both are on opposite sides */
										/* of an overlap. */

										/* Compute direction of "joining" vector. */
										/* First, compute direction of line from first */
										/* to second minutia points. */
										joinDir = getLfsUtil().lineToDirection(oFirstMinutia.get().getX(),
												oFirstMinutia.get().getY(), oSecondMinutia.get().getX(),
												oSecondMinutia.get().getY(), lfsParams.getNumDirections());

										/* Comptue opposite direction of first minutia. */
										opp1Dir = (oFirstMinutia.get().getDirection() + lfsParams.getNumDirections())
												% fullNDirs;
										/* Take "inner" distance on full circle between */
										/* the first minutia's opposite direction and */
										/* the joining direction. */
										joinDir = Math.abs(opp1Dir - joinDir);
										joinDir = Math.min(joinDir, fullNDirs - joinDir);

										if (isShowLogs())
											logger.debug("joindir={} dist=%f ", joinDir, dDistance);

										/* If the joining angle is <= 90 degrees OR */
										/* the 2 points are sufficiently close AND */
										/* a free path exists between pair ... */
										if (((joinDir <= halfNDirs) || (dDistance <= lfsParams.getMaxOverlapJoinDist()))
												&& getImageUtil().freePath(oFirstMinutia.get().getX(),
														oFirstMinutia.get().getY(), oSecondMinutia.get().getX(),
														oSecondMinutia.get().getY(), binarizedImageData, imageWidth,
														imageHeight, lfsParams) == ILfs.TRUE) {
											if (isShowLogs())
												logger.debug("4OV RM");

											/* Then assume overlap, so ... */
											/* Set to remove first minutia. */
											toRemoveIndexes[firstMinutiaIndex] = ILfs.TRUE;
											/* Set to remove second minutia. */
											toRemoveIndexes[secondMinutiaIndex] = ILfs.TRUE;
										}
										/* Otherwise, pair not on an overlap, so skip */
										/* to next second minutia. */
										else {
											if (isShowLogs())
												logger.debug("");
										}
									} else {
										if (isShowLogs())
											logger.debug("");
									}
									/* End same type test. */
								} // End deltadir test.
								else {
									if (isShowLogs())
										logger.debug("");
								}
							} // End distance test.
							else {
								if (isShowLogs())
									logger.debug("");
							}
						}
						/* Otherwise, current 2nd too far below 1st, so skip to next */
						/* 1st minutia. */
						else {
							if (isShowLogs())
								logger.debug("");
							/* Break out of inner secondary loop. */
							break;
						} // End delta-y test.
					} // End if !to_remove[s]
					else {
						if (isShowLogs())
							logger.debug("");
					}

					/* Bump to next second minutia in minutiae list. */
					secondMinutiaIndex++;
				} // End secondary minutiae loop.
			} // Otherwise, first minutia already flagged to be removed.

			/* Bump to next first minutia in minutiae list. */
			firstMinutiaIndex++;
		} // End primary minutiae loop.

		/* Now remove all minutiae in list that have been flagged for removal. */
		/* NOTE: Need to remove the minutia from their lists in reverse */
		/* order, otherwise, indices will be off. */
		for (minutiaIndex = oMinutiae.get().getNum() - 1; minutiaIndex >= 0; minutiaIndex--) {
			/* If the current minutia index is flagged for removal ... */
			if (toRemoveIndexes[minutiaIndex] == ILfs.TRUE) {
				/* Remove the minutia from the minutiae list. */
				ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
				if (ret.get() != ILfs.FALSE) {
					getFree().free(toRemoveIndexes);
					return (ret.get());
				}
			}
		}

		/* Return normally. */
		return ILfs.FALSE;
	}

	/**
	 * Detects and removes minutia points located on pore-shaped valleys and/or ridges.
	 *
	 * <p>NIST origin: {@code remove_pores_V2()} in {@code remove.c}. Only minutiae in blocks with LOW RIDGE FLOW
	 * or HIGH CURVATURE and a VALID direction are tested; others are kept. From a point R located
	 * {@code poresTransR} pixels behind the minutia, the method searches perpendicularly (up to
	 * {@code poresPerpSteps} steps) for edge points P and Q on either side of the feature, and traces their
	 * contours forward ({@code poresStepsFwd}) and backward ({@code poresStepsBwd}). The minutia is removed if P
	 * or Q cannot be found, if a contour trace is incomplete or forms a loop, or if the shape is pore-like: with
	 * A, B the end points of the contours traced from P and C, D those traced from Q, the squared distance
	 * {@code |CD|^2} exceeds {@code poresMinDist2} and the ratio {@code |AB|^2 / |CD|^2} is at most
	 * {@code poresMaxRatio}.
	 *
	 * @param oMinutiae          holder of the list of true and false minutiae; pruned in place
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param oDirectionMap      block map of directional ridge flow
	 * @param oLowFlowMap        block map of blocks flagged as LOW RIDGE FLOW
	 * @param oHighCurveMap      block map of blocks flagged as HIGH CURVATURE
	 * @param mappedImageWidth   width of the maps, in blocks
	 * @param mappedImageHeight  height of the maps, in blocks
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
	public int removePoresV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, int imageWidth,
			int imageHeight, AtomicIntegerArray oDirectionMap, AtomicIntegerArray oLowFlowMap,
			AtomicIntegerArray oHighCurveMap, int mappedImageWidth, int mappedImageHeight, LfsParams lfsParams) {
		int minutiaIndex;
		AtomicInteger ret = new AtomicInteger(0);
		int removed;
		int blockX;
		int blockY;
		int rx;
		int ry;
		AtomicInteger px = new AtomicInteger(0);
		AtomicInteger py = new AtomicInteger(0);
		AtomicInteger pex = new AtomicInteger(0);
		AtomicInteger pey = new AtomicInteger(0);
		int bx;
		int by;
		int dx;
		int dy;
		AtomicInteger qx = new AtomicInteger(0);
		AtomicInteger qy = new AtomicInteger(0);
		AtomicInteger qex = new AtomicInteger(0);
		AtomicInteger qey = new AtomicInteger(0);
		int ax;
		int ay;
		int cx;
		int cy;
		AtomicReference<Minutia> minutia = new AtomicReference<>();
		double piFactor;
		double theta;
		double sinTheta;
		double cosTheta;
		double ab2;
		double cd2;
		double ratio;
		Contour contour = null;
		AtomicInteger ncontour = new AtomicInteger(0);
		double drx;
		double dry;

		/* This routine attempts to locate the following points on all */
		/* minutia within the feature list. */
		/* 1. Compute R 3 pixels opposite the feature direction from */
		/* feature point F. */
		/* 2. Find white pixel transitions P & Q within 12 steps from */
		/* from R perpendicular to the feature's direction. */
		/* 3. Find points B & D by walking white edge from P. */
		/* 4. Find points A & C by walking white edge from Q. */
		/* 5. Measure squared distances between A-B and C-D. */
		/* 6. Compute ratio of squared distances and compare against */
		/* threshold (2.25). If A-B sufficiently larger than C-D, */
		/* then assume NOT pore, otherwise flag the feature point F. */
		/* If along the way, finding any of these points fails, then */
		/* assume the feature is a pore and flag it. */
		/*                                                                  */
		/* A */
		/* _____._ */
		/* ----___ Q C */
		/* ------____ ---_.________.___ */
		/* ---_ */
		/* (valley) F.\ .R (ridge) */
		/* ____/ */
		/* ______---- ___-.--------.--- */
		/* ____--- P D */
		/* -----.- */
		/* B */
		/*                                                                  */
		/* AB^2/CD^2 <= 2.25 then flag feature */
		/*                                                                  */

		if (isShowLogs())
			logger.debug("REMOVING PORES:");

		/* Factor for converting integer directions into radians. */
		piFactor = Math.PI / lfsParams.getNumDirections();

		/* Initialize to the beginning of the minutia list. */
		minutiaIndex = 0;
		/* Foreach minutia remaining in the list ... */
		while (minutiaIndex < oMinutiae.get().getNum()) {
			/* Set temporary minutia pointer. */
			minutia.set(oMinutiae.get().getList().get(minutiaIndex));

			/* Initialize remove flag to FALSE. */
			removed = ILfs.FALSE;

			/* Compute block coords from minutia point. */
			blockX = minutia.get().getX() / lfsParams.getBlockOffsetSize();
			blockY = minutia.get().getY() / lfsParams.getBlockOffsetSize();

			/* If minutia in LOW RIDGE FLOW or HIGH CURVATURE block */
			/* with a valid direction ... */
			if ((oLowFlowMap.get((blockY * mappedImageWidth) + blockX) != ILfs.FALSE
					|| oHighCurveMap.get((blockY * mappedImageWidth) + blockX) != ILfs.FALSE)
					&& (oDirectionMap.get((blockY * mappedImageWidth) + blockX) >= ILfs.FALSE)) {
				/* Compute radian angle from minutia direction. */
				theta = minutia.get().getDirection() * piFactor;
				/* Compute sine and cosine factors of this angle. */
				sinTheta = Math.sin(theta);
				cosTheta = Math.cos(theta);
				/* Translate the minutia point (ex. 3 pixels) in opposite */
				/* direction minutia is pointing. Call this point 'R'. */
				drx = minutia.get().getX() - (sinTheta * lfsParams.getPoresTransR());
				dry = minutia.get().getY() + (cosTheta * lfsParams.getPoresTransR());

				/* Need to truncate precision so that answers are consistent */
				/* on different computer architectures when rounding doubles. */
				drx = getDefs().truncDoublePrecision(drx, ILfs.TRUNC_SCALE);
				dry = getDefs().truncDoublePrecision(dry, ILfs.TRUNC_SCALE);
				rx = getDefs().sRound(drx);
				ry = getDefs().sRound(dry);

				/* If 'R' is opposite color from minutia type ... */
				if (binarizedImageData[(ry * imageWidth) + rx] != minutia.get().getType()) {
					/* Search a specified number of steps (ex. 12) from 'R' in a */
					/* perpendicular direction from the minutia direction until */
					/* the first white pixel is found. If a white pixel is */
					/* found within the specified number of steps, then call */
					/* this point 'P' (storing the point's edge pixel as well). */
					if (getImageUtil().searchInDirection(px, py, pex, pey, minutia.get().getType(), rx, ry, -cosTheta,
							-sinTheta, lfsParams.getPoresPerpSteps(), binarizedImageData, imageWidth,
							imageHeight) != ILfs.FALSE) {
						/* Trace contour from P's edge pixel in counter-clockwise */
						/* scan and step along specified number of steps (ex. 10). */
						contour = getContour().traceContour(ret, ncontour, lfsParams.getPoresStepsFwd(), px.get(),
								py.get(), px.get(), py.get(), pex.get(), pey.get(), ILfs.SCAN_COUNTER_CLOCKWISE,
								binarizedImageData, imageWidth, imageHeight);
						/* If system error occurred during trace ... */
						if (ret.get() < ILfs.FALSE) {
							/* Return error code. */
							return (ret.get());
						}

						/* If trace was not possible OR loop found OR */
						/* contour is incomplete ... */
						if ((ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.LOOP_FOUND)
								|| (ncontour.get() < lfsParams.getPoresStepsFwd())) {
							/* If contour allocated and returned ... */
							if ((ret.get() == ILfs.LOOP_FOUND) || (ncontour.get() < lfsParams.getPoresStepsFwd())) {
								/* Deallocate the contour. */
								getContour().freeContour(contour);
							}

							if (isShowLogs())
								logger.debug("{},{} RMB", minutia.get().getX(), minutia.get().getY());

							/* Then remove the minutia. */
							ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
							if (ret.get() != ILfs.FALSE) {
								/* If system error, return error code. */
								return (ret.get());
							}
							/* Set remove flag to TRUE. */
							removed = ILfs.TRUE;
						}
						/* Otherwise, traced contour is complete. */
						else {
							/* Store last point in contour as point 'B'. */
							bx = contour.getContourX().get(ncontour.get() - 1);
							by = contour.getContourY().get(ncontour.get() - 1);

							/* Deallocate the contour. */
							getContour().freeContour(contour);

							/* Trace contour from P's edge pixel in clockwise scan */
							/* and step along specified number of steps (ex. 8). */
							contour = getContour().traceContour(ret, ncontour, lfsParams.getPoresStepsBwd(), px.get(),
									py.get(), px.get(), py.get(), pex.get(), pey.get(), ILfs.SCAN_CLOCKWISE,
									binarizedImageData, imageWidth, imageHeight);
							/* If system error occurred during trace ... */
							if (ret.get() < ILfs.FALSE) {
								/* Return error code. */
								return (ret.get());
							}

							/* If trace was not possible OR loop found OR */
							/* contour is incomplete ... */
							if ((ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.LOOP_FOUND)
									|| (ncontour.get() < lfsParams.getPoresStepsBwd())) {
								/* If contour allocated and returned ... */
								if ((ret.get() == ILfs.LOOP_FOUND) || (ncontour.get() < lfsParams.getPoresStepsBwd())) {
									/* Deallocate the contour. */
									getContour().freeContour(contour);
								}

								if (isShowLogs())
									logger.debug("{},{} RMD", minutia.get().getX(), minutia.get().getY());

								/* Then remove the minutia. */
								ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
								if (ret.get() != ILfs.FALSE) {
									/* If system error, return error code. */
									return (ret.get());
								}
								/* Set remove flag to TRUE. */
								removed = ILfs.TRUE;
							}
							/* Otherwise, traced contour is complete. */
							else {
								/* Store last point in contour as point 'D'. */
								dx = contour.getContourX().get(ncontour.get() - 1);
								dy = contour.getContourY().get(ncontour.get() - 1);

								/* Deallocate the contour. */
								getContour().freeContour(contour);
								/* Search a specified number of steps (ex. 12) from */
								/* 'R' in opposite direction of that used to find */
								/* 'P' until the first white pixel is found. If a */
								/* white pixel is found within the specified number */
								/* of steps, then call this point 'Q' (storing the */
								/* point's edge pixel as well). */
								if (getImageUtil().searchInDirection(qx, qy, qex, qey, minutia.get().getType(), rx, ry,
										cosTheta, sinTheta, lfsParams.getPoresPerpSteps(), binarizedImageData,
										imageWidth, imageHeight) != ILfs.FALSE) {
									/* Trace contour from Q's edge pixel in clockwise */
									/* scan and step along specified number of steps */
									/* (ex. 10). */
									contour = getContour().traceContour(ret, ncontour, lfsParams.getPoresStepsFwd(),
											qx.get(), qy.get(), qx.get(), qy.get(), qex.get(), qey.get(),
											ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
									/* If system error occurred during trace ... */
									if (ret.get() < ILfs.FALSE) {
										/* Return error code. */
										return (ret.get());
									}

									/* If trace was not possible OR loop found OR */
									/* contour is incomplete ... */
									if ((ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.LOOP_FOUND)
											|| (ncontour.get() < lfsParams.getPoresStepsFwd())) {
										/* If contour allocated and returned ... */
										if ((ret.get() == ILfs.LOOP_FOUND)
												|| (ncontour.get() < lfsParams.getPoresStepsFwd())) {
											/* Deallocate the contour. */
											getContour().freeContour(contour);
										}

										if (isShowLogs())
											logger.debug("{},{} RMA", minutia.get().getX(), minutia.get().getY());

										/* Then remove the minutia. */
										ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
										if (ret.get() != ILfs.FALSE) {
											/* If system error, return error code. */
											return (ret.get());
										}
										/* Set remove flag to TRUE. */
										removed = ILfs.TRUE;
									}
									/* Otherwise, traced contour is complete. */
									else {
										/* Store last point in contour as point 'A'. */
										ax = contour.getContourX().get(ncontour.get() - 1);
										ay = contour.getContourY().get(ncontour.get() - 1);

										/* Deallocate the contour. */
										getContour().freeContour(contour);

										/* Trace contour from Q's edge pixel in */
										/* counter-clockwise scan and step along a */
										/* specified number of steps (ex. 8). */
										contour = getContour().traceContour(ret, ncontour, lfsParams.getPoresStepsBwd(),
												qx.get(), qy.get(), qx.get(), qy.get(), qex.get(), qey.get(),
												ILfs.SCAN_COUNTER_CLOCKWISE, binarizedImageData, imageWidth,
												imageHeight);
										/* If system error occurred during scan ... */
										if (ret.get() < ILfs.FALSE) {
											/* Return error code. */
											return (ret.get());
										}

										/* If trace was not possible OR loop found OR */
										/* contour is incomplete ... */
										if ((ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.LOOP_FOUND)
												|| (ncontour.get() < lfsParams.getPoresStepsBwd())) {
											/* If contour allocated and returned ... */
											if ((ret.get() == ILfs.LOOP_FOUND)
													|| (ncontour.get() < lfsParams.getPoresStepsBwd())) {
												/* Deallocate the contour. */
												getContour().freeContour(contour);
											}

											if (isShowLogs())
												logger.debug("{},{} RMC", minutia.get().getX(), minutia.get().getY());

											/* Then remove the minutia. */
											ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
											if (ret.get() != ILfs.FALSE) {
												/* If system error, return error code. */
												return (ret.get());
											}
											/* Set remove flag to TRUE. */
											removed = ILfs.TRUE;
										}
										/* Otherwise, traced contour is complete. */
										else {
											/* Store last point in contour as 'C'. */
											cx = contour.getContourX().get(ncontour.get() - 1);
											cy = contour.getContourY().get(ncontour.get() - 1);

											/* Deallocate the contour. */
											getContour().freeContour(contour);

											/* Compute squared distance between points */
											/* 'A' and 'B'. */
											ab2 = getLfsUtil().squaredDistance(ax, ay, bx, by);
											/* Compute squared distance between points */
											/* 'C' and 'D'. */
											cd2 = getLfsUtil().squaredDistance(cx, cy, dx, dy);
											/* If CD distance is not near zero */
											/* (ex. 0.5) ... */
											if (cd2 > lfsParams.getPoresMinDist2()) {
												/* Compute ratio of squared distances. */
												ratio = ab2 / cd2;

												/* If ratio is small enough (ex. 2.25)... */
												if (ratio <= lfsParams.getPoresMaxRatio()) {
													if (isShowLogs()) {
														logger.debug("{},{}", minutia.get().getX(),
																minutia.get().getY());
														logger.debug(
																"R={},{} P={},{} B={},{} D={},{} Q={},{} A={},{} C={},{} ",
																rx, ry, px.get(), py.get(), bx, by, dx, dy, qx.get(),
																qy.get(), ax, ay, cx, cy);
														logger.debug("RMRATIO %f", ratio);
													}
													/* Then assume pore & remove minutia. */
													ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
													if (ret.get() != ILfs.FALSE) {
														/* If system error, return error code. */
														return (ret.get());
													}
													/* Set remove flag to TRUE. */
													removed = ILfs.TRUE;
												}
												/* Otherwise, ratio to big, so assume */
												/* legitimate minutia. */
											} // Else, cd2 too small.
										} // Done with C.
									} // Done with A.
								}
								/* Otherwise, Q not found ... */
								else {
									if (isShowLogs())
										logger.debug("{},{} RMQ", minutia.get().getX(), minutia.get().getY());

									/* Then remove the minutia. */
									ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
									if (ret.get() != ILfs.FALSE) {
										/* If system error, return error code. */
										return (ret.get());
									}
									/* Set remove flag to TRUE. */
									removed = ILfs.TRUE;
								} // Done with Q.
							} // Done with D.
						} // Done with B.
					}
					/* Otherwise, P not found ... */
					else {
						if (isShowLogs())
							logger.debug("{},{} RMP", minutia.get().getX(), minutia.get().getY());

						/* Then remove the minutia. */
						ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
						if (ret.get() != ILfs.FALSE) {
							/* If system error, return error code. */
							return (ret.get());
						}
						/* Set remove flag to TRUE. */
						removed = ILfs.TRUE;
					}
				} // Else, R is on pixel the same color as type, so do not
				/* remove minutia point and skip to next one. */
			} // Else block is unreliable or has INVALID direction.

			/* If current minutia not removed ... */
			if (removed == ILfs.FALSE) {
				/* Bump to next minutia in list. */
				minutiaIndex++;
			}
			/* Otherwise, next minutia has slid into slot of current removed one. */

		} // End While minutia remaining in list.

		/* Return normally. */
		return ILfs.FALSE;
	}

	/**
	 * Removes minutiae not on complete contours of the required length, and adjusts the others from a min/max
	 * analysis of their rotated contour.
	 *
	 * <p>NIST origin: {@code remove_or_adjust_side_minutiae_V2()} in {@code remove.c}. A contour of
	 * {@code 2 * sideHalfContour + 1} points centered on each minutia is extracted; if it is incomplete, forms a
	 * loop or must be ignored, the minutia is removed. Otherwise the contour y-coordinates are rotated by the
	 * negative of the minutia direction, so that a well-formed minutia forms an upward bowl, and their relative
	 * minima/maxima are located. With exactly one minimum, or a min-max-min pattern (the lower minimum chosen),
	 * the minutia is moved to that contour point, and removed if it now lies in an INVALID block; any other
	 * pattern causes removal. Note: if the contour has fewer than 3 points the method returns immediately.
	 *
	 * @param oMinutiae          holder of the list of true and false minutiae; pruned and adjusted in place
	 * @param binarizedImageData binary image data ({@code 0} = white, {@code 1} = black), row-major
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param directionMap       block map of directional ridge flow
	 * @param mappedImageWidth   width of the map, in blocks
	 * @param mappedImageHeight  height of the map, in blocks
	 * @param lfsParams          parameters and thresholds for controlling LFS
	 * @return {@link ILfs#FALSE} (zero) on successful completion; a negative value on system error
	 */
	public int removeOrAdjustSideMinutiaeV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData,
			final int imageWidth, final int imageHeight, AtomicIntegerArray directionMap, final int mappedImageWidth,
			final int mappedImageHeight, final LfsParams lfsParams) {
		int minutiaIndex;
		AtomicInteger ret = new AtomicInteger(0);
		Minutia minutia;
		double piFactor;
		double theta;
		double sinTheta;
		double cosTheta;
		Contour contour = null;
		AtomicInteger oNoOfContour = new AtomicInteger(0);
		AtomicIntegerArray oRotatedYOfMinutiaContour;
		int minLoc;
		AtomicIntegerArray minmaxValue;
		AtomicIntegerArray minmaxIndex;
		AtomicIntegerArray minmaxType;
		AtomicInteger minmaxAlloc = new AtomicInteger(0);
		AtomicInteger minmaxNum = new AtomicInteger(0);
		double dRotatedYValue;
		int blockX;
		int blockY;

		if (isShowLogs())
			logger.debug("ADJUSTING SIDE MINUTIA:");

		/* Allocate working memory for holding rotated y-coord of a */
		/* minutia's contour. */
		oRotatedYOfMinutiaContour = new AtomicIntegerArray((lfsParams.getSideHalfContour() << 1) + 1);
		/* Compute factor for converting integer directions to radians. */
		piFactor = ILfs.M_PI / lfsParams.getNumDirections();

		minutiaIndex = 0;
		/* Foreach minutia remaining in list ... */
		while (minutiaIndex < oMinutiae.get().getNum()) {
			/* Assign a temporary pointer. */
			minutia = oMinutiae.get().getList().get(minutiaIndex);

			/* Extract a contour centered on the minutia point (ex. 7 pixels */
			/* in both directions). */
			contour = getContour().getCenteredContour(ret, oNoOfContour, lfsParams.getSideHalfContour(), minutia.getX(),
					minutia.getY(), minutia.getEx(), minutia.getEy(), binarizedImageData, imageWidth, imageHeight);

			/* If system error occurred ... */
			if (ret.get() < 0) {
				/* Deallocate working memory. */
				getFree().free(oRotatedYOfMinutiaContour);
				/* Return error code. */
				return (ret.get());
			}

			/* If we didn't succeed in extracting a complete contour for any */
			/* other reason ... */
			if ((ret.get() == ILfs.LOOP_FOUND) || (ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.INCOMPLETE)) {
				if (isShowLogs())
					logger.debug("{},{} RM1", minutia.getX(), minutia.getY());

				/* Remove minutia from list. */
				ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
				if (ret.get() != ILfs.FALSE) {
					/* Deallocate working memory. */
					getFree().free(oRotatedYOfMinutiaContour);
					/* Return error code. */
					return (ret.get());
				}
				/* No need to advance because next minutia has "slid" */
				/* into position pointed to by 'i'. */
			}
			/* Otherwise, a complete contour was found and extracted ... */
			else {
				/* Rotate contour points by negative angle of feature's direction. */
				/* The contour of a well-formed minutia point will form a bowl */
				/* shape concaved in the direction of the minutia. By rotating */
				/* the contour points by the negative angle of feature's direction */
				/* the bowl will be transformed to be concaved upwards and minima */
				/* and maxima of the transformed y-coords can be analyzed to */
				/* determine if the minutia is "well-formed" or not. If well- */
				/* formed then the position of the minutia point is adjusted. If */
				/* not well-formed, then the minutia point is removed altogether. */

				/* Normal rotation of T degrees around the origin of */
				/* the point (x,y): */
				/* rx = x*cos(T) - y*sin(T) */
				/* ry = x*cos(T) + y*sin(T) */
				/* The rotation here is for -T degrees: */
				/* rx = x*cos(-T) - y*sin(-T) */
				/* ry = x*cos(-T) + y*sin(-T) */
				/* which can be written: */
				/* rx = x*cos(T) + y*sin(T) */
				/* ry = x*sin(T) - y*cos(T) */

				/* Convert minutia's direction to radians. */
				theta = minutia.getDirection() * piFactor;
				/* Compute sine and cosine values at theta for rotation. */
				sinTheta = Math.sin(theta);
				cosTheta = Math.cos(theta);

				for (int j = 0; j < oNoOfContour.get(); j++) {
					/* We only need to rotate the y-coord (don't worry */
					/* about rotating the x-coord or contour edge pixels). */
					dRotatedYValue = (contour.getContourX().get(j) * sinTheta)
							- (contour.getContourY().get(j) * cosTheta);
					/* Need to truncate precision so that answers are consistent */
					/* on different computer architectures when rounding doubles. */
					dRotatedYValue = getDefs().truncDoublePrecision(dRotatedYValue, ILfs.TRUNC_SCALE);
					oRotatedYOfMinutiaContour.set(j, getDefs().sRound(dRotatedYValue));
				}

				/* Locate relative minima and maxima in vector of rotated */
				/* y-coords of current minutia's contour. */

				/* Determine maximum length for allocation of buffers. */
				/* If there are fewer than 3 items ... */
				if (oNoOfContour.get() < 3) {
					/* Then no min/max is possible, so set allocated length */
					/* to 0 and return. */
					return (0);
				}

				/* Otherwise, set allocation length to number of items - 2 */
				/* (one for the first item in the list, and on for the last). */
				/* Every other intermediate point can potentially represent a */
				/* min or max. */
				minmaxAlloc.set(oNoOfContour.get() - 2);
				minmaxValue = new AtomicIntegerArray(minmaxAlloc.get());
				minmaxType = new AtomicIntegerArray(minmaxAlloc.get());
				minmaxIndex = new AtomicIntegerArray(minmaxAlloc.get());

				ret.set(getLfsUtil().minMaxs(minmaxValue, minmaxType, minmaxIndex, minmaxAlloc, minmaxNum,
						oRotatedYOfMinutiaContour, oNoOfContour.get()));
				if (ret.get() < ILfs.FALSE) {
					/* If system error, then deallocate working memories. */
					getFree().free(oRotatedYOfMinutiaContour);
					getContour().freeContour(contour);
					/* Return error code. */
					return (ret.get());
				}

				/* If one and only one minima was found in rotated y-coord */
				/* of contour ... */
				if ((minmaxNum.get() == 1) && (minmaxType.get(0) == -1)) {
					if (isShowLogs())
						logger.debug("{},{} ", minutia.getX(), minutia.getY());

					/* Reset loation of minutia point to contour point at minima. */
					minutia.setX(contour.getContourX().get(minmaxIndex.get(0)));
					minutia.setY(contour.getContourY().get(minmaxIndex.get(0)));
					minutia.setEx(contour.getContourEx().get(minmaxIndex.get(0)));
					minutia.setEy(contour.getContourEy().get(minmaxIndex.get(0)));

					/* Must check if adjusted minutia is now in INVALID block ... */
					blockX = minutia.getX() / lfsParams.getBlockOffsetSize();
					blockY = minutia.getY() / lfsParams.getBlockOffsetSize();
					if (getPixelValueFromAtomicArray(directionMap, blockX, blockY, mappedImageWidth,
							mappedImageHeight) == ILfs.INVALID_DIR) {
						/* Remove minutia from list. */
						ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
						if (ret.get() != ILfs.FALSE) {
							/* Deallocate working memory. */
							getFree().free(oRotatedYOfMinutiaContour);
							getContour().freeContour(contour);
							if (minmaxAlloc.get() > ILfs.FALSE) {
								getFree().free(minmaxValue);
								getFree().free(minmaxType);
								getFree().free(minmaxIndex);
							}
							/* Return error code. */
							return (ret.get());
						}
						/* No need to advance because next minutia has "slid" */
						/* into position pointed to by 'i'. */

						if (isShowLogs())
							logger.debug("RM2");
					} else {
						/* Advance to the next minutia in the list. */
						minutiaIndex++;
						if (isShowLogs())
							logger.debug("AD1 {},{}", minutia.getX(), minutia.getY());
					}
				}
				/* If exactly 3 min/max found and they are min-max-min ... */
				else if ((minmaxNum.get() == 3) && (minmaxType.get(0) == -1)) {
					/* Choose minima location with smallest rotated y-coord. */
					if (minmaxValue.get(0) < minmaxValue.get(2)) {
						minLoc = minmaxIndex.get(0);
					} else {
						minLoc = minmaxIndex.get(2);
					}

					if (isShowLogs())
						logger.debug("{},{} ", minutia.getX(), minutia.getY());

					/* Reset loation of minutia point to contour point at minima. */
					minutia.setX(contour.getContourX().get(minLoc));
					minutia.setY(contour.getContourY().get(minLoc));
					minutia.setEx(contour.getContourEx().get(minLoc));
					minutia.setEy(contour.getContourEy().get(minLoc));

					/* Must check if adjusted minutia is now in INVALID block ... */
					blockX = minutia.getX() / lfsParams.getBlockOffsetSize();
					blockY = minutia.getY() / lfsParams.getBlockOffsetSize();

					if (getPixelValueFromAtomicArray(directionMap, blockX, blockY, mappedImageWidth,
							mappedImageHeight) == ILfs.INVALID_DIR) {
						/* Remove minutia from list. */
						ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
						if (ret.get() != ILfs.FALSE) {
							/* Deallocate working memory. */
							getFree().free(oRotatedYOfMinutiaContour);
							getContour().freeContour(contour);
							if (minmaxAlloc.get() > ILfs.FALSE) {
								getFree().free(minmaxValue);
								getFree().free(minmaxType);
								getFree().free(minmaxIndex);
							}
							/* Return error code. */
							return (ret.get());
						}
						/* No need to advance because next minutia has "slid" */
						/* into position pointed to by 'i'. */

						if (isShowLogs())
							logger.debug("RM3");
					} else {
						/* Advance to the next minutia in the list. */
						minutiaIndex++;
						if (isShowLogs())
							logger.debug("AD2 {},{}", minutia.getX(), minutia.getY());
					}
				}
				/* Otherwise, ... */
				else {
					if (isShowLogs())
						logger.debug("{},{} RM4", minutia.getX(), minutia.getY());

					/* Remove minutia from list. */
					ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
					if (ret.get() != ILfs.FALSE) {
						/* If system error, then deallocate working memories. */
						getFree().free(oRotatedYOfMinutiaContour);
						getContour().freeContour(contour);
						if (minmaxAlloc.get() > ILfs.FALSE) {
							getFree().free(minmaxValue);
							getFree().free(minmaxType);
							getFree().free(minmaxIndex);
						}
						/* Return error code. */
						return (ret.get());

					}
					/* No need to advance because next minutia has "slid" */
					/* into position pointed to by 'i'. */
				}
				/* Deallocate contour and min/max buffers. */
				getContour().freeContour(contour);
				if (minmaxAlloc.get() > ILfs.FALSE) {
					getFree().free(minmaxValue);
					getFree().free(minmaxType);
					getFree().free(minmaxIndex);
				}
			} // End else contour extracted.
		} // End while not end of minutiae list.

		/* Return normally. */
		return ILfs.FALSE;
	}
}