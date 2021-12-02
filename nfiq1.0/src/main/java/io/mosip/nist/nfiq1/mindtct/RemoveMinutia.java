package io.mosip.nist.nfiq1.mindtct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.nist.nfiq1.Defs;
import io.mosip.nist.nfiq1.common.ILfs;
import io.mosip.nist.nfiq1.common.ILfs.IRemoveMinutia;
import io.mosip.nist.nfiq1.common.ILfs.LfsParams;
import io.mosip.nist.nfiq1.common.ILfs.Minutia;
import io.mosip.nist.nfiq1.common.ILfs.Minutiae;

public class RemoveMinutia extends MindTct implements IRemoveMinutia {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoveMinutia.class);	
	private static volatile RemoveMinutia instance;
    public static RemoveMinutia getInstance() {
        if (instance == null) {
            synchronized (RemoveMinutia.class) {
                if (instance == null) {
                    instance = new RemoveMinutia();
                }
            }
        }
        return instance;
    }    
    private RemoveMinutia()
    {
    	super();
    }
    
	public Defs getDefs() {
		return Defs.getInstance();
	}	
	public Contour getContour() {
		return Contour.getInstance();
	}
	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}
	public Maps getMap() {
		return Maps.getInstance();
	}
	public Free getFree() {
		return Free.getInstance();
	}
	public ImageUtil getImageUtil() {
		return ImageUtil.getInstance();
	}
	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}
	public Loop getLoop() {
		return Loop.getInstance();
	}
	
	/*************************************************************************
	**************************************************************************
	#cat: removeFalseMinutiaV2 - Takes a list of true and false minutiae and
	#cat:                attempts to detect and remove the false minutiae based
	#cat:                on a series of tests.
	   Input:
	      oMinutiae  - list of true and false minutiae
	      binarizedImageData     - binary image data (0==while & 1==black)
	      imageWidth        - width (in pixels) of image
	      imageHeight        - height (in pixels) of image
	      map				- contains below info
	      directionMap  - map of image blocks containing directional ridge flow
	      lowFlowMap   - map of image blocks flagged as LOW RIDGE FLOW
	      highCurveMap - map of image blocks flagged as HIGH CURVATURE
	      mappedImageWidth        - width in blocks of the maps
	      mappedImageHeight        - height in blocks of the maps
	      lfsParams  - parameters and thresholds for controlling LFS
	   Output:
	      minutiae  - list of pruned minutiae
	   Return Code:
	      Zero     - successful completion
	      Negative - system error
	**************************************************************************/
	public int removeFalseMinutiaV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, 
		final int imageWidth, final int imageHeight, Maps map, 
		final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams) {
		int ret;
		/* 1. Sort minutiae points top-to-bottom and left-to-right. */
		if ((ret = getMinutiaHelper().sortMinutiaeTopToBottomAndThenLeftToRight(oMinutiae, imageWidth, imageHeight)) 
			!= ILfs.FALSE)
		{
			return (ret);
		}

		/* 2. Remove minutiae on lakes (filled with white pixels) and        */
		/*    islands (filled with black pixels), both  defined by a pair of */
		/*    minutia points.                                                */
		if ((ret = removeIslandsAndLakes(oMinutiae, binarizedImageData, imageWidth, imageHeight, lfsParams)) 
			!= ILfs.FALSE)
		{
			return (ret);
		}

		/* 3. Remove minutiae on holes in the binary image defined by a */
		/*    single point.                                             */
		if ((ret = removeHoles(oMinutiae, binarizedImageData, imageWidth, imageHeight, lfsParams)) 
			!= ILfs.FALSE)
		{
			return (ret);
		}

		/* 4. Remove minutiae that point sufficiently close to a block with */
		/*    INVALID direction.                                            */
		if ((ret = removePointingInvblockV2(oMinutiae, map.getDirectionMap(), 
			mappedImageWidth, mappedImageHeight, lfsParams)) != ILfs.FALSE)
		{
			return (ret);
		}

		/* 5. Remove minutiae that are sufficiently close to a block with */
		/*    INVALID direction.                                          */
		if ((ret = removeNearInvblocksV2(oMinutiae, map.getDirectionMap(), 
			mappedImageWidth, mappedImageHeight, lfsParams)) != ILfs.FALSE)
		{
			return (ret);
		}

		/* 6. Remove or adjust minutiae that reside on the side of a ridge */
		/*    or valley.                                                   */
		if ((ret = removeOrAdjustSideMinutiaeV2(oMinutiae, binarizedImageData, imageWidth, imageHeight, 
			map.getDirectionMap(), mappedImageWidth, mappedImageHeight, lfsParams)) != ILfs.FALSE)
		{
			return (ret);
		}

		/* 7. Remove minutiae that form a hook on the side of a ridge or valley. */
		if ((ret = removeHooks(oMinutiae, binarizedImageData, imageWidth, imageHeight, lfsParams)) 
			!= ILfs.FALSE)
		{
			return (ret);
		}

		/* 8. Remove minutiae that are on opposite sides of an overlap. */
		if ((ret = removeOverlaps(oMinutiae, binarizedImageData, imageWidth, imageHeight, lfsParams)) 
			!= ILfs.FALSE)
		{
			return (ret);
		}

		/* 9. Remove minutiae that are "irregularly" shaped. */
		if ((ret = removeMalformations(oMinutiae, binarizedImageData, imageWidth, imageHeight, 
			map.getLowFlowMap(), mappedImageWidth, mappedImageHeight, lfsParams)) != ILfs.FALSE)
		{
			return (ret);
		}

		/* 10. Remove minutiae that form long, narrow, loops in the */
		/*     "unreliable" regions in the binary image.            */
		if ((ret = removePoresV2(oMinutiae, binarizedImageData, imageWidth, imageHeight, 
			map.getDirectionMap(), map.getLowFlowMap(), map.getHighCurveMap(), 
			mappedImageWidth, mappedImageHeight, lfsParams)) != ILfs.FALSE)
		{
			return (ret);
		}

		return (ILfs.FALSE);
	}

	/*************************************************************************
	**************************************************************************
	#cat: removeHoles - Removes minutia points on small loops around valleys.
	   Input:
	      oMinutiae  - list of true and false minutiae
	      binarizedImageData     - binary image data (0==while & 1==black)
	      imageWidth        - width (in pixels) of image
	      imageHeight        - height (in pixels) of image
	      lfsParams  - parameters and thresholds for controlling LFS
	   Output:
	      oMinutiae  - list of pruned minutiae
	   Return Code:
	      Zero     - successful completion
	      Negative - system error
	**************************************************************************/
	public int removeHoles(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, 
		final int imageWidth, final int imageHeight, LfsParams lfsParams) {
		int minutiaIndex;
		int ret;
		Minutia minutia;

		if (isShowLogs())
			LOGGER.info(String.format("\nREMOVING HOLES:\n"));

		minutiaIndex = 0;
		/* Foreach minutia remaining in list ... */
		while (minutiaIndex < oMinutiae.get().getNum())
		{
			/* Assign a temporary pointer. */
			minutia = oMinutiae.get().getList().get(minutiaIndex);
			/* If current minutia is a bifurcation ... */
			if (minutia.getType() == ILfs.BIFURCATION)
			{
				/* Check to see if it is on a loop of specified length (ex. 15). */
				ret = getLoop().onLoop(minutia, lfsParams.getSmallLoopLen(), 
					binarizedImageData, imageWidth, imageHeight);
				/* If minutia is on a loop ... or loop test IGNORED */
				if ((ret == ILfs.LOOP_FOUND) || (ret == ILfs.IGNORE))
				{
					if (isShowLogs())
						LOGGER.info(String.format("%d,%d RM\n", minutia.getX(), minutia.getY()));

					/* Then remove the minutia from list. */
					if ((ret = getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae)) != ILfs.FALSE)
					{
						/* Return error code. */
						return (ret);
					}
					/* No need to advance because next minutia has "slid" */
					/* into position pointed to by 'i'.                   */
				}
				/* If the minutia is NOT on a loop... */
				else if (ret == ILfs.FALSE)
				{
					/* Simply advance to next minutia in the list. */
					minutiaIndex++;
				}
				/* Otherwise, an ERROR occurred while looking for loop. */
				else
				{
					/* Return error code. */
					return (ret);
				}
			}
			/* Otherwise, the current minutia is a ridge-ending... */
			else
			{
				/* Advance to next minutia in the list. */
				minutiaIndex++;
			}
		}

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	**************************************************************************
	#cat: removeHooks - Takes a list of true and false minutiae and
	#cat:                attempts to detect and remove those false minutiae that
	#cat:                are on a hook (white or black).
	   Input:
	      oMinutiae  - list of true and false minutiae
	      binarizedImageData     - binary image data (0==while & 1==black)
	      imageWidth        - width (in pixels) of image
	      imageHeight        - height (in pixels) of image
	      lfsParams  - parameters and thresholds for controlling LFS
	   Output:
	      oMinutiae  - list of pruned minutiae
	   Return Code:
	      Zero     - successful completion
	      Negative - system error
	**************************************************************************/
	public int removeHooks(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, 
		final int imageWidth, final int imageHeight, final LfsParams lfsParams) {
		List<Boolean> toRemoveIndexes;
		int minutiaIndex, minutiaFIndex, minutiaSIndex, ret;
		int deltaY, fullNDirs, qtrNDirs, deltaDir, minDeltaDir;
		Minutia minutia1, minutia2;
		double dDistance;

		if (isShowLogs())
			LOGGER.info(String.format("\nREMOVING HOOKS:\n"));

		/* Allocate list of minutia indices that upon completion of testing */
		/* should be removed from the minutiae lists.  Note: That using      */
		/* initializes the list to FALSE.                          */
		toRemoveIndexes = new ArrayList<Boolean>(Arrays.asList(new Boolean[oMinutiae.get().getNum()]));
		Collections.fill(toRemoveIndexes, Boolean.FALSE);
		
		/* Compute number directions in full circle. */
		fullNDirs = lfsParams.getNumDirections() << 1;
		/* Compute number of directions in 45=(180/4) degrees. */
		qtrNDirs = lfsParams.getNumDirections() >> 2;

		/* Minimum allowable deltadir to consider joining minutia.               */
		/* (The closer the deltadir is to 180 degrees, the more likely the join. */
		/* When ndirs==16, then this value is 11=(3*4)-1 == 123.75 degrees.      */
		/* I chose to parameterize this threshold based on a fixed fraction of   */
		/* 'ndirs' rather than on passing in a parameter in degrees and doing    */
		/* the conversion.  I doubt the difference matters.                      */
		minDeltaDir = (3 * qtrNDirs) - 1;

		if (isShowLogs())
			LOGGER.info(String.format("num=%2d, full_ndirs=%2d, qtr_ndirs=%2d", oMinutiae.get().getNum(), fullNDirs, qtrNDirs));
		minutiaFIndex = 0;
		/* Foreach primary (first) minutia (except for last one in list) ... */
		while (minutiaFIndex < oMinutiae.get().getNum() - 1)
		{
			/* If current first minutia not previously set to be removed. */
			if (!toRemoveIndexes.get(minutiaFIndex))
			{
				if (isShowLogs())
					LOGGER.info(String.format("\n"));

				/* Set first minutia to temporary pointer. */
				minutia1 = oMinutiae.get().getList().get(minutiaFIndex);
				/* Foreach secondary (second) minutia to right of first minutia ... */
				minutiaSIndex = minutiaFIndex + 1;
				while (minutiaSIndex < oMinutiae.get().getNum())
				{
					/* Set second minutia to temporary pointer. */
					minutia2 = oMinutiae.get().getList().get(minutiaSIndex);

					if (isShowLogs())
						LOGGER.info(String.format("1:%d(%d,%d)%d 2:%d(%d,%d)%d ", minutiaFIndex, minutia1.getX(), minutia1.getY(), minutia1.getType(), minutiaSIndex, minutia2.getX(), minutia2.getY(), minutia2.getType()));

					/* The binary image is potentially being edited during each */
					/* iteration of the secondary minutia loop, therefore       */
					/* minutia pixel values may be changed.  We need to catch   */
					/* these events by using the next 2 tests.                  */

					/* If the first minutia's pixel has been previously changed... */
					//if (*(binarizedImageData.argValue + (minutia1.y * imageWidth) + minutia1.x) != minutia1.type)
					if (binarizedImageData[(minutia1.getY() * imageWidth) + minutia1.getX()] != minutia1.getType())
					{
						if (isShowLogs())
							LOGGER.info(String.format("\n"));
						/* Then break out of secondary loop and skip to next first. */
						break;
					}

					/* If the second minutia's pixel has been previously changed... */
					//if (*(binarizedImageData.argValue + (minutia2.y * imageWidth) + minutia2.x) != minutia2.type)
					if (binarizedImageData [(minutia2.getY() * imageWidth) + minutia2.getX()] != minutia2.getType())
					{
						/* Set to remove second minutia. */
						toRemoveIndexes.set(minutiaSIndex, true);
					}

					/* If the second minutia not previously set to be removed. */
					if (!toRemoveIndexes.get(minutiaSIndex))
					{
						/* Compute delta y between 1st & 2nd minutiae and test. */
						deltaY = minutia2.getY() - minutia1.getY();
						/* If delta y small enough (ex. < 8 pixels) ... */
						if (deltaY <= lfsParams.getMaxRmTestDist())
						{
							if (isShowLogs())
								LOGGER.info(String.format("1DY "));

							/* Compute Euclidean distance between 1st & 2nd mintuae. */
							dDistance = getLfsUtil().distance(minutia1.getX(), minutia1.getY(), 
								minutia2.getX(), minutia2.getY());
							/* If distance is NOT too large (ex. < 8 pixels) ... */
							if (dDistance <= lfsParams.getMaxRmTestDist())
							{
								if (isShowLogs())
									LOGGER.info(String.format("2DS "));

								/* Compute "inner" difference between directions on */
								/* a full circle and test.                          */
								if ((deltaDir = getLfsUtil().closestDirDistance(minutia1.getDirection(), 
									minutia2.getDirection(), fullNDirs)) == ILfs.INVALID_DIR)
								{
									getFree().free(toRemoveIndexes);
									LOGGER.info(String.format("ERROR : removeHooks : INVALID direction\n"));
									return (ILfs.ERROR_CODE_641);
								}
								/* If the difference between dirs is large enough ...  */
								/* (the more 1st & 2nd point away from each other the  */
								/* more likely they should be joined)                  */
								if (deltaDir > minDeltaDir)
								{
									if (isShowLogs())
										LOGGER.info(String.format("3DD "));

									/* If 1st & 2nd minutiae are NOT same type ... */
									if (minutia1.getType() != minutia2.getType())
									{
										/* Check to see if pair on a hook with contour */
										/* of specified length (ex. 15 pixels) ...     */

										ret = getLoop().onHook(minutia1, minutia2, lfsParams.getMaxHookLen(), 
											binarizedImageData, imageWidth, imageHeight);
										/* If hook detected between pair ... */
										if (ret == ILfs.HOOK_FOUND)
										{
											if (isShowLogs())
												LOGGER.info(String.format("4HK RM\n"));

											/* Set to remove first minutia. */
											toRemoveIndexes.set(minutiaFIndex, true);
											/* Set to remove second minutia. */
											toRemoveIndexes.set(minutiaSIndex, true);
										}
										/* If hook test IGNORED ... */
										else if (ret == ILfs.IGNORE)
										{
											if (isShowLogs())
												LOGGER.info(String.format("RM\n"));

											/* Set to remove first minutia. */
											toRemoveIndexes.set(minutiaFIndex, true);
											/* Skip to next 1st minutia by breaking out of */
											/* inner secondary loop.                       */
											break;
										}
										/* If system error occurred during hook test ... */
										else if (ret < ILfs.FALSE)
										{
											getFree().free(toRemoveIndexes);
											return (ret);
										}
										/* Otherwise, no hook found, so skip to next */
										/* second minutia.                           */
										else
										{
											if (isShowLogs())
												LOGGER.info(String.format("\n"));
										}
									}
									else
									{
										if (isShowLogs())
											LOGGER.info(String.format("\n"));
									}
									/* End different type test. */
								} // End deltadir test.
								else
								{
									if (isShowLogs())
										LOGGER.info(String.format("\n"));
								}
							} // End distance test.
							else
							{
								if (isShowLogs())
									LOGGER.info(String.format("\n"));
							}
						}
						/* Otherwise, current 2nd too far below 1st, so skip to next */
						/* 1st minutia.                                              */
						else
						{
							if (isShowLogs())
								LOGGER.info(String.format("\n"));

							/* Break out of inner secondary loop. */
							break;
						} // End delta-y test.
					} // End if !to_remove[s]
					else
					{
						if (isShowLogs())
							LOGGER.info(String.format("\n"));
					}

					/* Bump to next second minutia in minutiae list. */
					minutiaSIndex++;
				} // End secondary minutiae loop.
			} // Otherwise, first minutia already flagged to be removed.

			/* Bump to next first minutia in minutiae list. */
			minutiaFIndex++;
		} // End primary minutiae loop.

		/* Now remove all minutiae in list that have been flagged for removal. */
		/* NOTE: Need to remove the minutia from their lists in reverse       */
		/*       order, otherwise, indices will be off.                       */
		for (minutiaIndex = oMinutiae.get().getNum() - 1; minutiaIndex >= 0; minutiaIndex--)
		{
			/* If the current minutia index is flagged for removal ... */
			if (toRemoveIndexes.get(minutiaIndex))
			{
				/* Remove the minutia from the minutiae list. */
				if ((ret = this.getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae)) != ILfs.FALSE)
				{
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

	/*************************************************************************
	**************************************************************************
	#cat: removeHooksIslandsLakesOverlaps - Removes minutia points on hooks,
	#cat:                islands, lakes, and overlaps and fills in small small
	#cat:                loops in the binary image and joins minutia features in
	#cat:                the image on opposite sides of an overlap.  So, this
	#cat:                routine not only prunes minutia points but it edits the
	#cat:                binary input image as well.
	   Input:
	      oMinutiae  - list of true and false minutiae
	      binarizedImageData     - binary image data (0==while & 1==black)
	      imageWidth        - width (in pixels) of image
	      imageHeight        - height (in pixels) of image
	      lfsParams  - parameters and thresholds for controlling LFS
	   Output:
	      oMinutiae  - list of pruned minutiae
	      binarizedImageData     - edited binary image with loops filled and overlaps removed
	   Return Code:
	      Zero     - successful completion
	      Negative - system error
	**************************************************************************/
	public int removeHooksIslandsLakesOverlaps(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, 
		final int imageWidth, final int imageHeight, final LfsParams lfsParams) {
		List<Boolean> toRemoveIndexes;
		int minutiaIndex, firstMinutiaIndex, secondMinutiaIndex;
		int ret;
		int deltaY, fullNDirs, qtrNDirs, deltaDir, minDeltaDir;
		AtomicInteger oNoOfLoop = new AtomicInteger(0);
		Minutia firstMinutia, secondMinutia;
		double dDistance;

		if (isShowLogs())
			LOGGER.info(String.format("\nREMOVING HOOKS, ISLANDS, LAKES, AND OVERLAPS:\n"));

		/* Allocate list of minutia indices that upon completion of testing */
		/* should be removed from the minutiae lists.  Note: That using      */
		/* initializes the list to FALSE.                          */
		toRemoveIndexes = new ArrayList<Boolean>(Arrays.asList(new Boolean[oMinutiae.get().getNum()]));
		/* Compute number directions in full circle. */
		fullNDirs = lfsParams.getNumDirections() << 1;
		/* Compute number of directions in 45=(180/4) degrees. */
		qtrNDirs = lfsParams.getNumDirections() >> 2;

		/* Minimum allowable deltadir to consider joining minutia.               */
		/* (The closer the deltadir is to 180 degrees, the more likely the join. */
		/* When ndirs==16, then this value is 11=(3*4)-1 == 123.75 degrees.      */
		/* I chose to parameterize this threshold based on a fixed fraction of   */
		/* 'ndirs' rather than on passing in a parameter in degrees and doing    */
		/* the conversion.  I doubt the difference matters.                      */
		minDeltaDir = (3 * qtrNDirs) - 1;

		firstMinutiaIndex = 0;
		/* Foreach primary (first) minutia (except for last one in list) ... */
		while (firstMinutiaIndex < oMinutiae.get().getNum() - 1)
		{
			/* If current first minutia not previously set to be removed. */
			if (!toRemoveIndexes.get(firstMinutiaIndex))
			{
				if (isShowLogs())
					LOGGER.info(String.format("\n"));

				/* Set first minutia to temporary pointer. */
				firstMinutia = oMinutiae.get().getList().get(firstMinutiaIndex);
				/* Foreach secondary (second) minutia to right of first minutia ... */
				secondMinutiaIndex = firstMinutiaIndex + 1;
				while (secondMinutiaIndex < oMinutiae.get().getNum())
				{
					/* Set second minutia to temporary pointer. */
					secondMinutia = oMinutiae.get().getList().get(secondMinutiaIndex);

					if (isShowLogs())
						LOGGER.info(String.format("1:%d(%d,%d)%d 2:%d(%d,%d)%d ", firstMinutiaIndex, firstMinutia.getX(), firstMinutia.getY(), firstMinutia.getType(), secondMinutiaIndex, secondMinutia.getX(), secondMinutia.getY(), secondMinutia.getType()));

					/* The binary image is potentially being edited during each */
					/* iteration of the secondary minutia loop, therefore       */
					/* minutia pixel values may be changed.  We need to catch   */
					/* these events by using the next 2 tests.                  */

					/* If the first minutia's pixel has been previously changed... */
					if (binarizedImageData[(firstMinutia.getY() * imageWidth) + firstMinutia.getX()] != firstMinutia.getType())
					{
						if (isShowLogs())
							LOGGER.info(String.format("\n"));
						/* Then break out of secondary loop and skip to next first. */
						break;
					}

					/* If the second minutia's pixel has been previously changed... */
					if (binarizedImageData[(secondMinutia.getY() * imageWidth) + secondMinutia.getX()] != secondMinutia.getType())
					{
						/* Set to remove second minutia. */
						toRemoveIndexes.set(secondMinutiaIndex, true);
					}

					/* If the second minutia not previously set to be removed. */
					if (!toRemoveIndexes.get(secondMinutiaIndex))
					{
						/* Compute delta y between 1st & 2nd minutiae and test. */
						deltaY = secondMinutia.getY() - firstMinutia.getY();
						/* If delta y small enough (ex. < 8 pixels) ... */
						if (deltaY <= lfsParams.getMaxRmTestDist())
						{
							if (isShowLogs())
								LOGGER.info(String.format("1DY "));

							/* Compute Euclidean distance between 1st & 2nd mintuae. */
							dDistance = this.getLfsUtil().distance(firstMinutia.getX(), firstMinutia.getY(), 
								secondMinutia.getX(), secondMinutia.getY());
							/* If distance is NOT too large (ex. < 8 pixels) ... */
							if (dDistance <= lfsParams.getMaxRmTestDist())
							{
								if (isShowLogs())
									LOGGER.info(String.format("2DS "));

								/* Compute "inner" difference between directions on */
								/* a full circle and test.                          */
								if ((deltaDir = this.getLfsUtil().closestDirDistance(firstMinutia.getDirection(), 
									secondMinutia.getDirection(), fullNDirs)) == ILfs.INVALID_DIR)
								{
									getFree().free(toRemoveIndexes);
									LOGGER.error(String.format("ERROR : removeHooksIslandsLakesOverlaps : INVALID direction\n"));
									return (ILfs.ERROR_CODE_301);
								}
								/* If the difference between dirs is large enough ...  */
								/* (the more 1st & 2nd point away from each other the  */
								/* more likely they should be joined)                  */
								if (deltaDir > minDeltaDir)
								{
									if (isShowLogs())
										LOGGER.info(String.format("3DD "));

									/* If 1st & 2nd minutiae are NOT same type ... */
									if (firstMinutia.getType() != secondMinutia.getType())
									{
										/* Check to see if pair on a hook with contour */
										/* of specified length (ex. 15 pixels) ...     */
										ret = this.getLoop().onHook(firstMinutia, secondMinutia, 
											lfsParams.getMaxHookLen(), binarizedImageData, imageWidth, imageHeight);
										/* If hook detected between pair ... */
										if (ret == ILfs.HOOK_FOUND)
										{
											if (isShowLogs())
												LOGGER.info(String.format("4HK RM\n"));
											
											/* Set to remove first minutia. */
											toRemoveIndexes.set(firstMinutiaIndex, true);
											/* Set to remove second minutia. */
											toRemoveIndexes.set(secondMinutiaIndex, true);
										}
										/* If hook test IGNORED ... */
										else if (ret == ILfs.IGNORE)
										{
											if (isShowLogs())
												LOGGER.info(String.format("RM\n"));

											/* Set to remove first minutia. */
											toRemoveIndexes.set(firstMinutiaIndex, true);
											/* Skip to next 1st minutia by breaking out of */
											/* inner secondary loop.                       */
											break;
										}
										/* If system error occurred during hook test ... */
										else if (ret < ILfs.FALSE)
										{
											getFree().free(toRemoveIndexes);
											return (ret);
										}
										/* Otherwise, no hook found, so skip to next */
										/* second minutia.                           */
										else
										{
											if (isShowLogs())
												LOGGER.info(String.format("\n"));
										}
									}
									/* Otherwise, pair is the same type, so test to see */
									/* if both are on an island or lake.                */
									else
									{
										/* Check to see if pair on a loop of specified */
										/* half length (ex. 15 pixels) ...             */
										AtomicInteger returnCode = new AtomicInteger(0);
										Contour contour = getLoop().onIslandLake(returnCode, oNoOfLoop, 
											firstMinutia, secondMinutia, lfsParams.getMaxHalfLoop(), 
											binarizedImageData, imageWidth, imageHeight);
										ret = returnCode.get();
										/* If pair is on island/lake ... */
										if (ret == ILfs.LOOP_FOUND)
										{
											if (isShowLogs())
												LOGGER.info(String.format("4IL RM\n"));

											/* Fill the loop. */
											if ((ret = getLoop().fillLoop(contour.getContourX(), contour.getContourY(), 
												oNoOfLoop.get(), binarizedImageData, imageWidth, imageHeight)) != ILfs.FALSE)
											{
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
										else if (ret == ILfs.IGNORE)
										{
											if (isShowLogs())
												LOGGER.info(String.format("RM\n"));

											/* Set to remove first minutia. */
											toRemoveIndexes.set(firstMinutiaIndex, true);
											/* Skip to next 1st minutia by breaking out of */
											/* inner secondary loop.                       */
											break;
										}
										/* If ERROR while looking for island/lake ... */
										else if (ret < ILfs.FALSE)
										{
											getFree().free(toRemoveIndexes);
											return (ret);
										}
										/* Otherwise, minutia pair not on island/lake, */
										/* but might be on an overlap.                 */
										else
										{
											/* If free path exists between pair ... */
											if (getImageUtil().freePath(firstMinutia.getX(), firstMinutia.getY(), 
												secondMinutia.getX(), secondMinutia.getY(), 
												binarizedImageData, imageWidth, imageHeight, lfsParams) != ILfs.FALSE)
											{
												if (isShowLogs())
													LOGGER.info(String.format("4OV RM\n"));

												/* Then assume overlap, so ...             */
												/* Join first and second minutiae in image. */
												if ((ret = this.getMinutiaHelper().joinMinutia(firstMinutia, secondMinutia, 
													binarizedImageData, imageWidth, imageHeight, 
													ILfs.NO_BOUNDARY, ILfs.JOIN_LINE_RADIUS)) != ILfs.FALSE)
												{
													getFree().free(toRemoveIndexes);
													return (ret);
												}
												/* Set to remove first minutia. */
												toRemoveIndexes.set(firstMinutiaIndex, true);
												/* Set to remove second minutia. */
												toRemoveIndexes.set(secondMinutiaIndex, true);
											}
											/* Otherwise, pair not on an overlap, so skip */
											/* to next second minutia.                    */
											else
											{
												if (isShowLogs())
													LOGGER.info(String.format("\n"));
											}
										} // End overlap test.
									} // End same type tests (island/lake & overlap).
								} // End deltadir test.
								else
								{
									if (isShowLogs())
										LOGGER.info(String.format("\n"));
								}
							} // End distance test.
							else
							{
								if (isShowLogs())
									LOGGER.info(String.format("\n"));
							}
						}
						/* Otherwise, current 2nd too far below 1st, so skip to next */
						/* 1st minutia.                                              */
						else
						{
							if (isShowLogs())
								LOGGER.info(String.format("\n"));

							/* Break out of inner secondary loop. */
							break;
						} // End delta-y test.
					} // End if !to_remove[s]
					else
					{
						if (isShowLogs())
							LOGGER.info(String.format("\n"));
					}
					/* Bump to next second minutia in minutiae list. */
					secondMinutiaIndex++;
				} // End secondary minutiae loop.
			} // Otherwise, first minutia already flagged to be removed.

			/* Bump to next first minutia in minutiae list. */
			firstMinutiaIndex++;
		} // End primary minutiae loop.

		/* Now remove all minutiae in list that have been flagged for removal. */
		/* NOTE: Need to remove the minutia from their lists in reverse       */
		/*       order, otherwise, indices will be off.                       */
		for (minutiaIndex = oMinutiae.get().getNum() - 1; minutiaIndex >= 0; minutiaIndex--)
		{
			/* If the current minutia index is flagged for removal ... */
			if (toRemoveIndexes.get(minutiaIndex))
			{
				/* Remove the minutia from the minutiae list. */
				if ((ret = this.getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae)) != ILfs.FALSE)
				{
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

	/*************************************************************************
	**************************************************************************
	#cat: removeIslandsAndLakes - Takes a list of true and false minutiae and
	#cat:                attempts to detect and remove those false minutiae that
	#cat:                are either on a common island (filled with black pixels)
	#cat:                or a lake (filled with white pixels).
	#cat:                Note that this routine edits the binary image by filling
	#cat:                detected lakes or islands.
	   Input:
	      oMinutiae  - list of true and false minutiae
	      binarizedImageData     - binary image data (0==while & 1==black)
	      imageWidth        - width (in pixels) of image
	      imageHeight        - height (in pixels) of image
	      lfsParams  - parameters and thresholds for controlling LFS
	   Output:
	      oMinutiae  - list of pruned minutiae
	   Return Code:
	      Zero     - successful completion
	      Negative - system error
	**************************************************************************/
	public int removeIslandsAndLakes(AtomicReference<Minutiae> oMinutiae, 
		int[] binarizedImageData, int imageWidth, int imageHeight, LfsParams lfsParams) {
		int []toRemoveIndexes;
		int minutiaIndex, firstMinutiaIndex, secondMinutiaIndex = 0; 
		AtomicInteger ret = new AtomicInteger(0);
		int deltaY, fullNDirs, qtrNDirs, deltaDir, minDeltaDir;
		Contour contour;
		AtomicInteger nloop = new AtomicInteger(0);
		AtomicReference<Minutia> oFirstMinutia = new AtomicReference<Minutia>(), 
			oSecondMinutia = new AtomicReference<Minutia>();
		double dist;
		int distThresh;
		int halfLoop;

		if (isShowLogs())
			LOGGER.info("\nREMOVING ISLANDS AND LAKES:\n");

		distThresh = lfsParams.getMaxRmTestDist();
		halfLoop = lfsParams.getMaxHalfLoop();

		toRemoveIndexes = new int [oMinutiae.get().getNum()];

		/* Compute number directions in full circle. */
		fullNDirs = lfsParams.getNumDirections() << 1;
		/* Compute number of directions in 45=(180/4) degrees. */
		qtrNDirs = lfsParams.getNumDirections() >> 2;

		/* Minimum allowable deltadir to consider joining minutia.               */
		/* (The closer the deltadir is to 180 degrees, the more likely the join. */
		/* When ndirs==16, then this value is 11=(3*4)-1 == 123.75 degrees.      */
		/* I chose to parameterize this threshold based on a fixed fraction of   */
		/* 'ndirs' rather than on passing in a parameter in degrees and doing    */
		/* the conversion.  I doubt the difference matters.                      */
		minDeltaDir = (3 * qtrNDirs) - 1;

		/* Foreach primary (first) minutia (except for last one in list) ... */
		firstMinutiaIndex = 0;
		while(firstMinutiaIndex < oMinutiae.get().getNum()-1){

			if (isShowLogs())
				LOGGER.info(String.format("(f = %d, s = %d)\n", firstMinutiaIndex, secondMinutiaIndex));

			/* If current first minutia not previously set to be removed. */
			if (toRemoveIndexes[firstMinutiaIndex] != ILfs.TRUE)
			{
				/* Set first minutia to temporary pointer. */
				oFirstMinutia.set(oMinutiae.get().getList().get(firstMinutiaIndex));

				/* Foreach secondary minutia to right of first minutia ... */
				secondMinutiaIndex = firstMinutiaIndex + 1;
				while (secondMinutiaIndex < oMinutiae.get().getNum())
				{
					/* Set second minutia to temporary pointer. */
					oSecondMinutia.set(oMinutiae.get().getList().get(secondMinutiaIndex));

					/* If the secondary minutia is desired type ... */
					if (oSecondMinutia.get().getType() == oFirstMinutia.get().getType())
					{
						if (isShowLogs())
							LOGGER.info(String.format("1:%d(%d,%d)%d 2:%d(%d,%d)%d ", firstMinutiaIndex, oFirstMinutia.get().getX(), oFirstMinutia.get().getY(), oFirstMinutia.get().getType(), secondMinutiaIndex, oSecondMinutia.get().getX(), oSecondMinutia.get().getY(), oSecondMinutia.get().getType()));

						/* The binary image is potentially being edited during   */
						/* each iteration of the secondary minutia loop,         */
						/* therefore minutia pixel values may be changed.  We    */
						/* need to catch these events by using the next 2 tests. */

						/* If the first minutia's pixel has been previously */
						/* changed...                                       */
						if (binarizedImageData [(oFirstMinutia.get().getY() * imageWidth) + oFirstMinutia.get().getX()] != oFirstMinutia.get().getType())
						{
							if (isShowLogs())
								LOGGER.info("\n");
							/* Then break out of secondary loop and skip to next */
							/* first.                                            */
							break;
						}

						/* If the second minutia's pixel has been previously */
						/* changed...                                        */
						if (binarizedImageData [(oSecondMinutia.get().getY() * imageWidth) + oSecondMinutia.get().getX()] != oSecondMinutia.get().getType())
						{
							/* Set to remove second minutia. */
							toRemoveIndexes[secondMinutiaIndex] = ILfs.TRUE;
						}

						/* If the second minutia not previously set to be removed. */
						if (toRemoveIndexes[secondMinutiaIndex] == ILfs.FALSE)
						{
							/* Compute delta y between 1st & 2nd minutiae and test. */
							deltaY = oSecondMinutia.get().getY() - oFirstMinutia.get().getY();
							/* If delta y small enough (ex. <16 pixels)... */
							if (deltaY <= distThresh)
							{
								if (isShowLogs())
									LOGGER.info("1DY ");

								/* Compute Euclidean distance between 1st & 2nd */
								/* mintuae.                                     */
								dist = getLfsUtil().distance(oFirstMinutia.get().getX(), oFirstMinutia.get().getY(), oSecondMinutia.get().getX(), oSecondMinutia.get().getY());

								/* If distance is NOT too large (ex. <16 pixels)... */
								if (dist <= distThresh)
								{
									if (isShowLogs())
										LOGGER.info("2DS ");

									/* Compute "inner" difference between directions */
									/* on a full circle and test.                    */
									if ((deltaDir = 
											getLfsUtil().closestDirDistance(oFirstMinutia.get().getDirection(), 
											oSecondMinutia.get().getDirection(), fullNDirs)) 
										== ILfs.INVALID_DIR)
									{
										toRemoveIndexes = null;
										LOGGER.error("ERROR : removeIslandsAndLakes : INVALID direction\n");
										return (ILfs.ERROR_CODE_611);
									}
								
									/* If the difference between dirs is large      */
									/* enough ...                                   */
									/* (the more 1st & 2nd point away from each     */
									/* other the more likely they should be joined) */
									if (deltaDir > minDeltaDir)
									{
										if (isShowLogs())
											LOGGER.info("3DD ");

										/* Pair is the same type, so test to see */
										/* if both are on an island or lake.     */

										/* Check to see if pair on a loop of specified */
										/* half length (ex. 30 pixels) ...             */
										contour = getLoop().onIslandLake(ret, nloop, oFirstMinutia.get(), oSecondMinutia.get(),
											halfLoop, binarizedImageData, imageWidth, imageHeight);
										 
										/* If pair is on island/lake ... */
										if (ret.get() == ILfs.LOOP_FOUND)
										{
											if (isShowLogs())
												LOGGER.info("4IL RM\n");
											
											/* Fill the loop. */
											ret.set(getLoop().fillLoop(contour.getContourX(), contour.getContourY(), 
												nloop.get(), binarizedImageData, imageWidth, imageHeight));
											if (ret.get() != ILfs.FALSE)
											{
												getContour().freeContour(contour);
												getFree().free(toRemoveIndexes);
												return (ret.get());
											}
											/* Set to remove first minutia. */
											toRemoveIndexes[firstMinutiaIndex] = ILfs.TRUE;
											/* Set to remove second minutia. */
											toRemoveIndexes[secondMinutiaIndex] = ILfs.TRUE;;
											/* Deallocate loop contour. */
											getContour().freeContour(contour);
										}
										/* If island/lake test IGNORED ... */
										else if (ret.get() == ILfs.IGNORE)
										{
											if (isShowLogs())
												LOGGER.info("RM\n");

											/* Set to remove first minutia. */
											toRemoveIndexes[firstMinutiaIndex] = ILfs.TRUE;
											/* Skip to next 1st minutia by breaking out */
											/* of inner secondary loop.                 */
											break;
										}
										/* If ERROR while looking for island/lake ... */
										else if (ret.get() < ILfs.FALSE)
										{
											getFree().free(toRemoveIndexes);
											return (ret.get());
										}
										else
										{
											if (isShowLogs())
												LOGGER.info("\n");
										}
									} // End deltadir test.
									else
									{
										if (isShowLogs())
											LOGGER.info("\n");
									}
								} // End distance test.
								else
								{
									if (isShowLogs())
										LOGGER.info("\n");
								}
							}
							/* Otherwise, current 2nd too far below 1st, so skip to */
							/* next 1st minutia.                                    */
							else
							{
								if (isShowLogs())
									LOGGER.info("\n");
								/* Break out of inner secondary loop. */
								break;
							} // End delta-y test.
						} // End if !to_remove[s]
						else
						{
							if (isShowLogs())
								LOGGER.info("\n");
						}
					} // End if 2nd not desired type

					/* Bump to next second minutia in minutiae list. */
					secondMinutiaIndex++;
				} // End secondary minutiae loop.
			} // Otherwise, first minutia already flagged to be removed.
			/* Bump to next first minutia in minutiae list. */
			firstMinutiaIndex++;
		}/* End primary minutiae loop. */

		/* Now remove all minutiae in list that have been flagged for removal. */
		/* NOTE: Need to remove the minutia from their lists in reverse       */
		/*       order, otherwise, indices will be off.                       */
		for(minutiaIndex = oMinutiae.get().getNum()-1; minutiaIndex >= 0; minutiaIndex--){
			/* If the current minutia index is flagged for removal ... */
		    if(toRemoveIndexes[minutiaIndex] == ILfs.TRUE){
		    	/* Remove the minutia from the minutiae list. */
		    	ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
		        if(ret.get() != ILfs.FALSE){
		        	getFree().free(toRemoveIndexes);
		        	return(ret.get());
		        }
		    }
		}

		/* Deallocate flag list. */
		getFree().free(toRemoveIndexes);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	**************************************************************************
	#cat: removeMalformations - Attempts to detect and remove minutia points
	#cat:            that are "irregularly" shaped.  Irregularity is measured
	#cat:            by measuring across the interior of the feature at
	#cat:            two progressive points down the feature's contour.  The
	#cat:            test is triggered if a pixel of opposite color from the
	#cat:            feture's type is found.  The ratio of the distances across
	#cat:            the feature at the two points is computed and if the ratio
	#cat:            is too large then the minutia is determined to be malformed.
	#cat:            A cursory test is conducted prior to the general tests in
	#cat:            the event that the minutia lies in a block with LOW RIDGE
	#cat:            FLOW.  In this case, the distance across the feature at
	#cat:            the second progressive contour point is measured and if
	#cat:            too large, the point is determined to be malformed.
	   Input:
	      oMinutiae  - list of true and false minutiae
	      binarizedImageData     - binary image data (0==while & 1==black)
	      imageWidth        - width (in pixels) of image
	      imageHeight        - height (in pixels) of image
	      oLowFlowMap   - map of image blocks flagged as LOW RIDGE FLOW
	      mappedImageWidth        - width in blocks of the map
	      mappedImageHeight        - height in blocks of the map
	      lfsParams  - parameters and thresholds for controlling LFS
	   Output:
	      oMinutiae  - list of pruned minutiae
	   Return Code:
	      Zero     - successful completion
	      Negative - system error
	**************************************************************************/
	public int removeMalformations(AtomicReference<Minutiae> oMinutiae, 
		int[] binarizedImageData, int imageWidth, int imageHeight, AtomicIntegerArray oLowFlowMap, 
		int mappedImageWidth, int mappedImageHeight, LfsParams lfsParams) {
		int minutiaIndex, j;
		AtomicInteger ret = new AtomicInteger(0);
		AtomicReference<Minutia> oMinutia = new AtomicReference<Minutia>();
		Contour contour = null;
		AtomicInteger oNoOfContour = new AtomicInteger(0);
		
		int ax1, ay1;
		int bx1, by1;
		int ax2, ay2;
		int bx2, by2;
		int [] xList;
		int [] yList;
		AtomicInteger num = new AtomicInteger(0);
		double aDist, bDist, ratio;
		int fmapval;
		int removed;
		int blockX, blockY;

		if (isShowLogs())
			LOGGER.info(String.format("\nREMOVING MALFORMATIONS:\n"));

		for (minutiaIndex = oMinutiae.get().getNum()- 1; minutiaIndex >= 0; minutiaIndex--)
		{
			oMinutia.set(oMinutiae.get().getList().get(minutiaIndex));
			
			contour = getContour().traceContour(ret, oNoOfContour, lfsParams.getMalformationSteps2(), 
				oMinutia.get().getX(), oMinutia.get().getY(), oMinutia.get().getX(), 
				oMinutia.get().getY(), oMinutia.get().getEx(), oMinutia.get().getEy(), 
				ILfs.SCAN_COUNTER_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
			/* If system error occurred during trace ... */
			if (ret.get() < ILfs.FALSE)
			{
				/* Return error code. */
				return (ret.get());
			}

			/* If trace was not possible OR loop found OR */
			/* contour is incomplete ...                  */
			if ((ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.LOOP_FOUND) || 
				(oNoOfContour.get() < lfsParams.getMalformationSteps2()))
			{
				/* If contour allocated and returned ... */
				if ((ret.get() == ILfs.LOOP_FOUND) || 
					(oNoOfContour.get() < lfsParams.getMalformationSteps2()))
				{
					/* Deallocate the contour. */
					getContour().freeContour(contour);
				}

				if (isShowLogs())
					LOGGER.info(String.format("%d,%d RMA\n", oMinutia.get().getX(), oMinutia.get().getY()));

				/* Then remove the minutia. */
				ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
				if (ret.get() != ILfs.FALSE)
				{
					/* If system error, return error code. */
					return (ret.get());
				}
			}
			/* Otherwise, traced contour is complete. */
			else
			{
				/* Store 'A1' contour point. */
				ax1 = contour.getContourX().get(lfsParams.getMalformationSteps1() - 1);
				ay1 = contour.getContourY().get(lfsParams.getMalformationSteps1() - 1);

				/* Store 'B1' contour point. */
				bx1 = contour.getContourX().get(lfsParams.getMalformationSteps2() - 1);
				by1 = contour.getContourY().get(lfsParams.getMalformationSteps2() - 1);

				/* Deallocate the contours. */
				getContour().freeContour(contour);

				contour = getContour().traceContour(ret, oNoOfContour, lfsParams.getMalformationSteps2(), 
					oMinutia.get().getX(), oMinutia.get().getY(), oMinutia.get().getX(), 
					oMinutia.get().getY(), oMinutia.get().getEx(), oMinutia.get().getEy(), 
					ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
				/* If system error occurred during trace ... */
				if (ret.get() < ILfs.FALSE)
				{
					/* Return error code. */
					return (ret.get());
				}
				
				/* If trace was not possible OR loop found OR */
				/* contour is incomplete ...                  */
				if ((ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.LOOP_FOUND) || 
					(oNoOfContour.get() < lfsParams.getMalformationSteps2()))
				{
					/* If contour allocated and returned ... */
					if ((ret.get() == ILfs.LOOP_FOUND) || (oNoOfContour.get() < lfsParams.getMalformationSteps2()))
					{
						/* Deallocate the contour. */
						getContour().freeContour(contour);
					}

					if (isShowLogs())
						LOGGER.info(String.format("%d,%d RMB\n", oMinutia.get().getX(), oMinutia.get().getY()));

					/* Then remove the minutia. */
					ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
					if (ret.get() != ILfs.FALSE)
					{
						/* If system error, return error code. */
						return (ret.get());
					}
				}
				/* Otherwise, traced contour is complete. */
				else
				{
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
					blockX = oMinutia.get().getX() / lfsParams.getBlockSize();
					blockY = oMinutia.get().getY() / lfsParams.getBlockSize();

					removed = ILfs.FALSE;

					/* Check to see if distances are not zero. */
					if ((aDist == 0.0) || (bDist == 0.0))
					{
						/* Remove the malformation minutia. */
						if (isShowLogs())
							LOGGER.info(String.format("%d,%d RMMAL1\n", oMinutia.get().getX(), oMinutia.get().getY()));

						ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
						if (ret.get() != ILfs.FALSE)
						{
							/* If system error, return error code. */
							return (ret.get());
						}
						removed = ILfs.TRUE;
					}
					
					if (removed == ILfs.FALSE)
					{
						/* Determine if minutia is in LOW RIDGE FLOW block. */
						fmapval = oLowFlowMap.get((blockY * mappedImageWidth) + blockX);
						if (fmapval == ILfs.TRUE)
						{
							/* If in LOW RIDGE LFOW, conduct a cursory distance test. */
							/* Need to test this out!                                 */
							if (bDist > lfsParams.getMaxMalformationDist())
							{
								/* Remove the malformation minutia. */
								if (isShowLogs())
									LOGGER.info(String.format("%d,%d RMMAL2\n", oMinutia.get().getX(), oMinutia.get().getY()));

								ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
								if (ret.get() != ILfs.FALSE)
								{
									/* If system error, return error code. */
									return (ret.get());
								}
								removed = ILfs.TRUE;
							}
						}
					}

					if (removed == ILfs.FALSE)
					{
						int asize = Math.max(Math.abs(bx2 - bx1) + 2, Math.abs(by2 - by1) + 2);
						xList = new int[asize]; yList = new int[asize];
						/* Compute points on line between the points A & B. */
						ret.set(Line.getInstance().linePoints(xList, yList, num, bx1, by1, bx2, by2));
						if (ret.get() != ILfs.FALSE)
						{
							return (ret.get());
						}
						/* Foreach remaining point along line segment ... */
						for (j = 0; j < num.get(); j++)
						{
							/* If B path contains pixel opposite minutia type ... */
							if (binarizedImageData [(yList[j] * imageWidth) + xList[j]] != oMinutia.get().getType())
							{
								/* Compute ratio of A & B path lengths. */
								ratio = bDist / aDist;
								/* Need to truncate precision so that answers are  */
								/* consistent on different computer architectures. */
								ratio = getDefs().truncDoublePrecision(ratio, ILfs.TRUNC_SCALE);
								/* If the B path is sufficiently longer than A path ... */
								if (ratio > lfsParams.getMinMalformationRatio())
								{
									/* Remove the malformation minutia. */
									/* Then remove the minutia. */
									if (isShowLogs())
										LOGGER.info(String.format("%d,%d RMMAL3\n", oMinutia.get().getX(), oMinutia.get().getY()));

									ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
									if (ret.get() != ILfs.FALSE)
									{
										xList = null;
										yList = null;
										/* If system error, return error code. */
										return (ret.get());
									}
									/* Break out of FOR loop. */
									break;
								}
							}
						}

						xList = null;
						yList = null;
					}
				}
			}
		}

		return ILfs.FALSE;
	}

	/*************************************************************************
	**************************************************************************
	#cat: removeNearInvblocksV2 - Removes minutia points from the given list
	#cat:                that are sufficiently close to a block with invalid
	#cat:                ridge flow or to the edge of the image.
	   Input:
	      oMinutiae  - list of true and false minutiae
	      directionMap - map of image blocks containing direction ridge flow
	      mappedImageWidth        - width in blocks of the map
	      mappedImageHeight        - height in blocks of the map
	      lfsParams  - parameters and thresholds for controlling LFS
	   Output:
	      oMinutiae  - list of pruned minutiae
	   Return Code:
	      Zero     - successful completion
	      Negative - system error
	**************************************************************************/
	public int removeNearInvblocksV2(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray directionMap, 
		final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams) {
		int minutiaIndex, ret;
		int ni, nbx, nby, nvalid;
		int ix, iy, sbi, ebi;
		int bx, by, px, py;
		boolean removed;
		Minutia minutia;
		int lowMargin, highMargin;

		/* The next 2 lookup tables are indexed by 'ix' and 'iy'. */
		/* When a feature pixel lies within a 6-pixel margin of a */
		/* block, this routine examines neighboring blocks to     */
		/* determine appropriate actions.                         */
		/*    'ix' may take on values:                            */
		/*         0 == x-pixel coord in leftmost margin          */
		/*         1 == x-pixel coord in middle of block          */
		/*         2 == x-pixel coord in rightmost margin         */
		/*    'iy' may take on values:                            */
		/*         0 == y-pixel coord in topmost margin           */
		/*         1 == y-pixel coord in middle of block          */
		/*         2 == y-pixel coord in bottommost margin        */
		/* Given (ix, iy):                                        */
		/*    'startblk[ix][iy]' == starting neighbor index (sbi) */
		/*    'endblk[ix][iy]'   == ending neighbor index (ebi)   */
		/*    so that neighbors begin to be analized from index   */
		/*    'sbi' to 'ebi'.                                     */
		/* Ex. (ix, iy) = (2, 0)                                  */
		/*    ix==2 ==> x-pixel coord in rightmost margin         */
		/*    iy==0 ==> y-pixel coord in topmost margin           */
		/*    X - marks the region in the current block           */
		/*        corresponding to (ix=2, iy=0).                  */
		/*    sbi = 0 = startblk[2][0]                            */
		/*    ebi = 2 = endblk[2][0]                              */
		/*    so neighbors are analized on index range [0..2]     */
		/*                                |                       */
		/*                 nbr block 0    |  nbr block 1          */
		/*      --------------------------+------------           */
		/*           top margin      | X  |                       */
		/*      _._._._._._._._._._._._._.|                       */
		/*                           |    |                       */
		/*          current block    .r  m|  nbr block 2          */
		/*                           |i  a|                       */
		/*                           .g  g|                       */
		/*                           |h  i|                       */
		/*                           .t  n|                       */
		/*                           |    |                       */

		/* LUT for starting neighbor index given (ix, iy).        */
		byte startblock[] = { 6, 0, 0,
                      		6,-1, 2,
                            4, 4, 2 };
		/* LUT for ending neighbor index given (ix, iy).          */
		byte endblock[] =   { 8, 0, 2,
                         	6,-1, 2,
                            6, 4, 4 };

		/* Pixel coord offsets specifying the order in which neighboring */
		/* blocks are searched.  The current block is in the middle of   */
		/* 8 surrounding neighbors.  The following illustrates the order */
		/* of neighbor indices.  (Note that 9 overlaps 1.)               */
		/*                        8                                      */
		/*                      7 0 1                                    */
		/*                      6 C 2                                    */
		/*                      5 4 3                                    */
		/*                                                               */
		/*                       0  1  2  3  4  5  6  7  8                    */
		byte blockdx[] = {  0, 1, 1, 1, 0,-1,-1,-1, 0 };  /* Delta-X     */
		byte blockdy[] = { -1,-1, 0, 1, 1, 1, 0,-1,-1 };  /* Delta-Y     */

		if (isShowLogs())
			LOGGER.info(String.format("\nREMOVING MINUTIA NEAR INVALID BLOCKS:\n"));

		/* If the margin covers more than the entire block ... */
		if (lfsParams.getInvBlockMargin() > (lfsParams.getBlockSize() >> 1))
		{
			/* Then treat this as an error. */
			LOGGER.error(String.format("ERROR : removeNearInvblocksV2 : margin too large for blocksize\n"));
			return (ILfs.ERROR_CODE_620);
		}

		/* Compute the low and high pixel margin boundaries (ex. 6 pixels wide) */
		/* in the block.                                                        */
		lowMargin = lfsParams.getInvBlockMargin();
		highMargin = lfsParams.getBlockSize() - lfsParams.getInvBlockMargin() - 1;

		
		minutiaIndex = 0;
		/* Foreach minutia remaining in the list ... */
		while (minutiaIndex < oMinutiae.get().getNum())
		{
			/* Assign temporary minutia pointer. */
			minutia = oMinutiae.get().getList().get(minutiaIndex);

			/* Compute block coords from minutia's pixel location. */
			bx = minutia.getX() / lfsParams.getBlockSize();
			by = minutia.getY() / lfsParams.getBlockSize();

			/* Compute pixel offset into the image block corresponding to the */
			/* minutia's pixel location.                                      */
			/* NOTE: The margins used here will not necessarily correspond to */
			/* the actual block boundaries used to compute the map values.    */
			/* This will be true when the image width and/or height is not an */
			/* even multiple of 'blocksize' and we are processing minutia     */
			/* located in the right-most column (or bottom-most row) of       */
			/* blocks.  I don't think this will pose a problem in practice.   */
			px = minutia.getX() % lfsParams.getBlockSize();
			py = minutia.getY() % lfsParams.getBlockSize();

			/* Determine if x pixel offset into the block is in the margins. */
			/* If x pixel offset is in left margin ... */
			if (px < lowMargin)
			{
				ix = 0;
			}
			/* If x pixel offset is in right margin ... */
			else if (px > highMargin)
			{
				ix = 2;
			}
			/* Otherwise, x pixel offset is in middle of block. */
			else
			{
				ix = 1;
			}

			/* Determine if y pixel offset into the block is in the margins. */
			/* If y pixel offset is in top margin ... */
			if (py < lowMargin)
			{
				iy = 0;
			}
			/* If y pixel offset is in bottom margin ... */
			else if (py > highMargin)
			{
				iy = 2;
			}
			/* Otherwise, y pixel offset is in middle of block. */
			else
			{
				iy = 1;
			}

			/* Set remove flag to FALSE. */
			removed = false;

			/* If one of the minutia's pixel offsets is in a margin ... */
			if ((ix != 1) || (iy != 1))
			{
				/* Compute the starting neighbor block index for processing. */
				//sbi = *(startblk + (iy * 3) + ix);
				sbi = startblock [(iy * 3) + ix];
				/* Compute the ending neighbor block index for processing. */
				//ebi = *(endblk + (iy * 3) + ix);
				ebi = endblock [(iy * 3) + ix];

				/* Foreach neighbor in the range to be processed ... */
				for (ni = sbi; ni <= ebi; ni++)
				{
					/* Compute the neighbor's block coords relative to */
					/* the block the current minutia is in.            */
					nbx = bx + blockdx[ni];
					nby = by + blockdy[ni];

					/* If neighbor's block coords are outside of map boundaries... */
					if ((nbx < 0) || (nbx >= mappedImageWidth) || 
						(nby < 0) || (nby >= mappedImageHeight))
					{
						if (isShowLogs())
							LOGGER.info(String.format("%d,%d RM1\n", minutia.getX(), minutia.getY()));

						/* Then the minutia is in a margin adjacent to the edge of */
						/* the image.                                              */
						/* NOTE: This is true when the image width and/or height   */
						/* is an even multiple of blocksize.  When the image is not*/
						/* an even multiple, then some minutia may not be detected */
						/* as being in the margin of "the image" (not the block).  */
						/* In practice, I don't think this will impact performance.*/
						if ((ret = getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae)) 
							!= ILfs.FALSE)
						{
							/* If system error occurred while removing minutia, */
							/* then return error code.                          */
							return (ret);
						}
						/* Set remove flag to TURE. */
						removed = true;
						/* Break out of neighboring block loop. */
						break;
					}
					/* If the neighboring block has INVALID direction ... */
					//else if (*(directionMap + (nby * mappedImageWidth) + nbx) == ILfs.INVALID_DIR)
					else if (directionMap.get((nby * mappedImageWidth) + nbx) == ILfs.INVALID_DIR)
					{
						/* Count the number of valid blocks neighboring */
						/* the current neighbor.                        */
						nvalid = getMap().numValid8Nbrs(directionMap, nbx, nby, mappedImageWidth, mappedImageHeight);
						/* If the number of valid neighbors is < threshold */
						/* (ex. 7)...                                      */
						if (nvalid < lfsParams.getRmValidNbrMin())
						{
							if (isShowLogs())
								LOGGER.info(String.format("%d,%d RM2\n", minutia.getX(), minutia.getY()));

							/* Then remove the current minutia from the list. */
							if ((ret = getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae)) != ILfs.FALSE)
							{
								/* If system error occurred while removing minutia, */
								/* then return error code.                          */
								return (ret);
							}
							/* Set remove flag to TURE. */
							removed = true;
							/* Break out of neighboring block loop. */
							break;
						}
						/* Otherwise enough valid neighbors, so don't remove minutia */
						/* based on this neighboring block.                          */
					}
					/* Otherwise neighboring block has valid direction,         */
					/* so don't remove minutia based on this neighboring block. */
				}
			} // Otherwise not in margin, so skip to next minutia in list.

			/* If current minutia not removed ... */
			if (!removed)
			{
				/* Advance to the next minutia in the list. */
				minutiaIndex++;
			}
			/* Otherwise the next minutia has slid into the spot where current */
			/* minutia was removed, so don't bump minutia index.               */
		} // End minutia loop

		   
		/* Return normally. */
		return ILfs.FALSE;
	}

	/*************************************************************************
	**************************************************************************
	#cat: removePointingInvblockV2 - Removes minutia points that are relatively
	#cat:                close in the direction opposite the minutia to a
	#cat:                block with INVALID ridge flow.
	   Input:
	      oMinutiae  - list of true and false minutiae
	      directionMap - map of image blocks containing directional ridge flow
	      mappedImageWidth        - width in blocks of the map
	      mappedImageHeight        - height in blocks of the map
	      lfsParams  - parameters and thresholds for controlling LFS
	   Output:
	      oMinutiae  - list of pruned minutiae
	   Return Code:
	      Zero     - successful completion
	      Negative - system error
	**************************************************************************/
	public int removePointingInvblockV2(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray directionMap, 
		final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams) {
		int minutiaIndex, ret;
		int deltaX, deltaY, dMapValue;
		int nx, ny, bx, by;
		Minutia minutia;
		double piFactor, theta;
		double dx, dy;

		if (isShowLogs())
			LOGGER.info("\nREMOVING MINUTIA POINTING TO INVALID BLOCKS:\n");

		/* Compute factor for converting integer directions to radians. */
		piFactor = ILfs.M_PI / (double)lfsParams.getNumDirections();

		minutiaIndex = 0;
		/* Foreach minutia remaining in list ... */
		while (minutiaIndex < oMinutiae.get().getNum())
		{
			/* Set temporary minutia pointer. */
			minutia = oMinutiae.get().getList().get(minutiaIndex);
			/* Convert minutia's direction to radians. */
			theta = minutia.getDirection() * piFactor;
			/* Compute translation offsets (ex. 6 pixels). */
			dx = Math.sin(theta) * (double)lfsParams.getTransDirPixel();
			dy = Math.cos(theta) * (double)lfsParams.getTransDirPixel();
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
			bx = (int)(nx / lfsParams.getBlockSize());
			by = (int)(ny / lfsParams.getBlockSize());
			/* The translation could move the point out of image boundaries,    */
			/* and therefore the corresponding block coords can be out of       */
			/* IMAP boundaries, so limit the block coords to within boundaries. */
			bx = Math.max(0, bx);
			bx = Math.min(mappedImageWidth - 1, bx);
			by = Math.max(0, by);
			by = Math.min(mappedImageHeight - 1, by);

			/* Get corresponding block's ridge flow direction. */
			dMapValue = getPixelValueFromAtomicArray(directionMap, bx, by, mappedImageWidth, mappedImageHeight);
			/* If the NMAP value of translated minutia point is INVALID ... */
			if (dMapValue == ILfs.INVALID_DIR)
			{
				if (isShowLogs())
					LOGGER.info(String.format("%d,%d RM\n", minutia.getX(), minutia.getY()));

				/* Remove the minutia from the minutiae list. */
				if ((ret = getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae)) 
					!= ILfs.FALSE)
				{
					return (ret);
				}
				/* No need to advance because next minutia has slid into slot. */
			}
			else
			{
				/* Advance to next minutia in list. */
				minutiaIndex++;
			}
		}

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/*************************************************************************
	**************************************************************************
	#cat: removeOverlaps - Takes a list of true and false minutiae and
	#cat:                attempts to detect and remove those false minutiae that
	#cat:                are on opposite sides of an overlap.  Note that this
	#cat:                routine does NOT edit the binary image when overlaps
	#cat:                are removed.
	   Input:
	      oMinutiae  		- list of true and false minutiae
	      binarizedImageData- binary image data (0==while & 1==black)
	      imageWidth        - width (in pixels) of image
	      imageHeight       - height (in pixels) of image
	      lfsParams  		- parameters and thresholds for controlling LFS
	   Output:
	      oMinutiae  		- list of pruned minutiae
	   Return Code:
	      Zero     - successful completion
	      Negative - system error
	**************************************************************************/
	public int removeOverlaps(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, 
		int imageWidth, int imageHeight, LfsParams lfsParams) {
		int [] toRemoveIndexes;
		int minutiaIndex, firstMinutiaIndex, secondMinutiaIndex;
		AtomicInteger ret = new AtomicInteger(0);
		int deltaY, fullNDirs, qtrNDirs, deltaDir, minDeltaDir;
		AtomicReference<Minutia> oFirstMinutia = new AtomicReference<Minutia>(), 
			oSecondMinutia = new AtomicReference<Minutia>();
		double dDistance;
		int joinDir, opp1Dir, halfNDirs;

		if (isShowLogs())
			LOGGER.info(String.format("\nREMOVING OVERLAPS:\n"));

		/* Allocate list of minutia indices that upon completion of testing */
		/* should be removed from the minutiae lists.  Note: That using      */
		/* "calloc" initializes the list to FALSE.                          */
		toRemoveIndexes = new int [oMinutiae.get().getNum()];

		/* Compute number directions in full circle. */
		fullNDirs = lfsParams.getNumDirections() << 1;
		/* Compute number of directions in 45=(180/4) degrees. */
		qtrNDirs = lfsParams.getNumDirections() >> 2;
		/* Compute number of directions in 90=(180/2) degrees. */
		halfNDirs = lfsParams.getNumDirections() >> 1;

		/* Minimum allowable deltadir to consider joining minutia.               */
		/* (The closer the deltadir is to 180 degrees, the more likely the join. */
		/* When ndirs==16, then this value is 11=(3*4)-1 == 123.75 degrees.      */
		/* I chose to parameterize this threshold based on a fixed fraction of   */
		/* 'ndirs' rather than on passing in a parameter in degrees and doing    */
		/* the conversion.  I doubt the difference matters.                      */
		minDeltaDir = (3 * qtrNDirs) - 1;

		firstMinutiaIndex = 0;
		/* Foreach primary (first) minutia (except for last one in list) ... */
		while(firstMinutiaIndex < oMinutiae.get().getNum()-1){
			/* If current first minutia not previously set to be removed. */
			if (toRemoveIndexes[firstMinutiaIndex] != ILfs.TRUE)
			{
				if (isShowLogs())
					LOGGER.info("\n");

				/* Set first minutia to temporary pointer. */
				oFirstMinutia.set(oMinutiae.get().getList().get(firstMinutiaIndex));
				/* Foreach secondary (second) minutia to right of first minutia ... */
				secondMinutiaIndex = firstMinutiaIndex + 1;
				while (secondMinutiaIndex < oMinutiae.get().getNum())
				{
					/* Set second minutia to temporary pointer. */
					oSecondMinutia.set(oMinutiae.get().getList().get(secondMinutiaIndex));

					if (isShowLogs())
						LOGGER.info(String.format("1:%d(%d,%d)%d 2:%d(%d,%d)%d ", firstMinutiaIndex, oFirstMinutia.get().getX(), oFirstMinutia.get().getY(), oFirstMinutia.get().getType(), secondMinutiaIndex, oSecondMinutia.get().getX(), oSecondMinutia.get().getY(), oSecondMinutia.get().getType()));

					/* The binary image is potentially being edited during each */
					/* iteration of the secondary minutia loop, therefore       */
					/* minutia pixel values may be changed.  We need to catch   */
					/* these events by using the next 2 tests.                  */

					/* If the first minutia's pixel has been previously changed... */
					//if (*(binarizedImageData + (minutia1.y * imageWidth) + minutia1.x) != minutia1.type)
					if (binarizedImageData [(oFirstMinutia.get().getY() * imageWidth) + oFirstMinutia.get().getX()] != oFirstMinutia.get().getType())
					{
						if (isShowLogs())
							LOGGER.info("\n");
						/* Then break out of secondary loop and skip to next first. */
						break;
					}

					/* If the second minutia's pixel has been previously changed... */
					//if (*(binarizedImageData + (minutia2.y * imageWidth) + minutia2.x) != minutia2.type)
					if (binarizedImageData [(oSecondMinutia.get().getY() * imageWidth) + oSecondMinutia.get().getX()] != 
						oSecondMinutia.get().getType())
					{
						/* Set to remove second minutia. */
						toRemoveIndexes[secondMinutiaIndex] = ILfs.TRUE;
					}

					/* If the second minutia not previously set to be removed. */
					if (toRemoveIndexes[secondMinutiaIndex] != ILfs.TRUE)
					{
						/* Compute delta y between 1st & 2nd minutiae and test. */
						deltaY = oSecondMinutia.get().getY() - oFirstMinutia.get().getY();
						/* If delta y small enough (ex. < 8 pixels) ... */
						if (deltaY <= lfsParams.getMaxOverlapDist())
						{
							if (isShowLogs())
								LOGGER.info("1DY ");

							/* Compute Euclidean distance between 1st & 2nd mintuae. */
							dDistance = getLfsUtil().distance(oFirstMinutia.get().getX(), oFirstMinutia.get().getY(), oSecondMinutia.get().getX(), oSecondMinutia.get().getY());
							/* If distance is NOT too large (ex. < 8 pixels) ... */
							if (dDistance <= lfsParams.getMaxOverlapDist())
							{
								if (isShowLogs())
									LOGGER.info("2DS ");

								/* Compute "inner" difference between directions on */
								/* a full circle and test.                          */
								if ((deltaDir = getLfsUtil().closestDirDistance(oFirstMinutia.get().getDirection(), oSecondMinutia.get().getDirection(), fullNDirs)) == ILfs.INVALID_DIR)
								{
									toRemoveIndexes = null;
									LOGGER.error("ERROR : removeOverlaps : INVALID direction\n");
									return (ILfs.ERROR_CODE_651);
								}
								/* If the difference between dirs is large enough ...  */
								/* (the more 1st & 2nd point away from each other the  */
								/* more likely they should be joined)                  */
								if (deltaDir > minDeltaDir)
								{
									if (isShowLogs())
										LOGGER.info("3DD ");

									/* If 1st & 2nd minutiae are same type ... */
									if (oFirstMinutia.get().getType() == oSecondMinutia.get().getType())
									{
										/* Test to see if both are on opposite sides */
										/* of an overlap.                            */

										/* Compute direction of "joining" vector.      */
										/* First, compute direction of line from first */
										/* to second minutia points.                   */
										joinDir = getLfsUtil().lineToDirection(oFirstMinutia.get().getX(), oFirstMinutia.get().getY(), oSecondMinutia.get().getX(), oSecondMinutia.get().getY(), lfsParams.getNumDirections());

										/* Comptue opposite direction of first minutia. */
										opp1Dir = (oFirstMinutia.get().getDirection() + lfsParams.getNumDirections()) % fullNDirs;
										/* Take "inner" distance on full circle between */
										/* the first minutia's opposite direction and   */
										/* the joining direction.                       */
										joinDir = Math.abs(opp1Dir - joinDir);
										joinDir = Math.min(joinDir, fullNDirs - joinDir);

										if (isShowLogs())
											LOGGER.info(String.format("joindir=%d dist=%f ", joinDir,dDistance));

										/* If the joining angle is <= 90 degrees OR   */
										/*    the 2 points are sufficiently close AND */
										/*    a free path exists between pair ...     */
										if (((joinDir <= halfNDirs) || (dDistance <= lfsParams.getMaxOverlapJoinDist())) 
												&& getImageUtil().freePath(oFirstMinutia.get().getX(), oFirstMinutia.get().getY(), oSecondMinutia.get().getX(), oSecondMinutia.get().getY(), binarizedImageData, imageWidth, imageHeight, lfsParams) == ILfs.TRUE)
										{
											if (isShowLogs())
												LOGGER.info("4OV RM\n");

											/* Then assume overlap, so ...             */
											/* Set to remove first minutia. */
											toRemoveIndexes[firstMinutiaIndex] = ILfs.TRUE;
											/* Set to remove second minutia. */
											toRemoveIndexes[secondMinutiaIndex] = ILfs.TRUE;
										}
										/* Otherwise, pair not on an overlap, so skip */
										/* to next second minutia.                    */
										else
										{
											if (isShowLogs())
												LOGGER.info("\n");
										}
									}
									else
									{
										if (isShowLogs())
											LOGGER.info("\n");
									}
									/* End same type test. */
								} // End deltadir test.
								else
								{
									if (isShowLogs())
										LOGGER.info("\n");
								}
							} // End distance test.
							else
							{
								if (isShowLogs())
									LOGGER.info("\n");
							}
						}
						/* Otherwise, current 2nd too far below 1st, so skip to next */
						/* 1st minutia.                                              */
						else
						{
							if (isShowLogs())
								LOGGER.info("\n");
							/* Break out of inner secondary loop. */
							break;
						} // End delta-y test.
					} // End if !to_remove[s]
					else
					{
						if (isShowLogs())
							LOGGER.info("\n");
					}

					/* Bump to next second minutia in minutiae list. */
					secondMinutiaIndex++;
				} // End secondary minutiae loop.
			} // Otherwise, first minutia already flagged to be removed.

			/* Bump to next first minutia in minutiae list. */
			firstMinutiaIndex++;
		} // End primary minutiae loop.

		/* Now remove all minutiae in list that have been flagged for removal. */
		/* NOTE: Need to remove the minutia from their lists in reverse       */
		/*       order, otherwise, indices will be off.                       */
		for (minutiaIndex = oMinutiae.get().getNum() - 1; minutiaIndex >= 0; minutiaIndex--)
		{
			/* If the current minutia index is flagged for removal ... */
			if (toRemoveIndexes[minutiaIndex] == ILfs.TRUE)
			{
				/* Remove the minutia from the minutiae list. */
		    	ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
		        if(ret.get() != ILfs.FALSE){
		        	getFree().free(toRemoveIndexes);
		        	return(ret.get());
		        }
			}
		}

		/* Deallocate flag list. */
		toRemoveIndexes = null;

	   /* Return normally. */
	   return ILfs.FALSE;
	}

	/*************************************************************************
	**************************************************************************
	#cat: removePoresV2 - Attempts to detect and remove minutia points located on
	#cat:                   pore-shaped valleys and/or ridges.  Detection for
	#cat:                   these features are only performed in blocks with
	#cat:                   LOW RIDGE FLOW or HIGH CURVATURE.
	   Input:
	      oMinutiae  - list of true and false minutiae
	      binarizedImageData     - binary image data (0==while & 1==black)
	      imageWidth        - width (in pixels) of image
	      imageHeight        - height (in pixels) of image
	      oDirectionMap  - map of image blocks containing directional ridge flow
	      oLowFlowMap   - map of image blocks flagged as LOW RIDGE FLOW
	      oHighCurveMap - map of image blocks flagged as HIGH CURVATURE
	      mappedImageWidth        - width in blocks of the maps
	      mappedImageHeight        - height in blocks of the maps
	      lfsParams  - parameters and thresholds for controlling LFS
	   Output:
	      oMinutiae  - list of pruned minutiae
	   Return Code:
	      Zero     - successful completion
	      Negative - system error
	**************************************************************************/
	public int removePoresV2(AtomicReference<Minutiae> oMinutiae, 
		int[] binarizedImageData, int imageWidth, int imageHeight, 
		AtomicIntegerArray oDirectionMap, AtomicIntegerArray oLowFlowMap, AtomicIntegerArray oHighCurveMap, 
		int mappedImageWidth, int mappedImageHeight, LfsParams lfsParams) {
		int minutiaIndex;
		AtomicInteger ret = new AtomicInteger(0);
		int removed, blockX, blockY;
		int rx, ry;
		AtomicInteger px = new AtomicInteger(0), py = new AtomicInteger(0), 
			pex = new AtomicInteger(0), pey = new AtomicInteger (0);
		int bx, by, dx, dy;
		AtomicInteger qx = new AtomicInteger(0), qy = new AtomicInteger(0), 
			qex = new AtomicInteger(0), qey = new AtomicInteger(0);
		int ax, ay, cx, cy;
		AtomicReference<Minutia> minutia = new AtomicReference<Minutia>();
		double piFactor, theta, sinTheta, cosTheta;
		double ab2, cd2, ratio;
		Contour contour = null;
		AtomicInteger ncontour = new AtomicInteger(0);
		double drx, dry;
		
		/*      This routine attempts to locate the following points on all */
		/*      minutia within the feature list.                            */
		/*      1. Compute R 3 pixels opposite the feature direction from   */
		/*         feature point F.                                         */
		/*      2. Find white pixel transitions P & Q within 12 steps from  */
		/*         from R perpendicular to the feature's direction.         */
		/*      3. Find points B & D by walking white edge from P.          */
		/*      4. Find points A & C by walking white edge from Q.          */
		/*      5. Measure squared distances between A-B and C-D.           */
		/*      6. Compute ratio of squared distances and compare against   */
		/*         threshold (2.25).  If A-B sufficiently larger than C-D,  */
		/*         then assume NOT pore, otherwise flag the feature point F.*/
		/*      If along the way, finding any of these points fails, then   */
		/*      assume the feature is a pore and flag it.                   */
		/*                                                                  */
		/*                      A                                           */
		/*                _____._                                           */
		/*                       ----___     Q      C                       */
		/*                ------____    ---_.________.___                   */
		/*                          ---_                                    */
		/*                (valley)    F.\   .R  (ridge)                     */
		/*                          ____/                                   */
		/*                ______----    ___-.--------.---                   */
		/*                       ____---     P      D                       */
		/*                -----.-                                           */
		/*                      B                                           */
		/*                                                                  */
		/*              AB^2/CD^2 <= 2.25  then flag feature                */
		/*                                                                  */
		
		if (isShowLogs())
			LOGGER.info(String.format("\nREMOVING PORES:\n"));

		/* Factor for converting integer directions into radians. */
		piFactor = Math.PI / (double)lfsParams.getNumDirections();

		/* Initialize to the beginning of the minutia list. */
		minutiaIndex = 0;
		/* Foreach minutia remaining in the list ... */
		while (minutiaIndex < oMinutiae.get().getNum())
		{
			/* Set temporary minutia pointer. */
			minutia.set(oMinutiae.get().getList().get(minutiaIndex));
			
			/* Initialize remove flag to FALSE. */
			removed = ILfs.FALSE;
		      
			/* Compute block coords from minutia point. */
			blockX = minutia.get().getX() / lfsParams.getBlockSize();
			blockY = minutia.get().getY() / lfsParams.getBlockSize();

			/* If minutia in LOW RIDGE FLOW or HIGH CURVATURE block */
			/* with a valid direction ...                           */
			if ((oLowFlowMap.get((blockY * mappedImageWidth) + blockX) != ILfs.FALSE || 
				oHighCurveMap.get((blockY * mappedImageWidth) + blockX) != ILfs.FALSE) && 
				(oDirectionMap.get((blockY * mappedImageWidth) + blockX) >= ILfs.FALSE))
			{
				/* Compute radian angle from minutia direction. */
				theta = (double)minutia.get().getDirection() * piFactor;
				/* Compute sine and cosine factors of this angle. */
				sinTheta = Math.sin(theta);
				cosTheta = Math.cos(theta);
				/* Translate the minutia point (ex. 3 pixels) in opposite */
				/* direction minutia is pointing.  Call this point 'R'.   */
				drx = (double)minutia.get().getX() - (sinTheta * (double)lfsParams.getPoresTransR());
				dry = (double)minutia.get().getY() + (cosTheta * (double)lfsParams.getPoresTransR());

				/* Need to truncate precision so that answers are consistent */
				/* on different computer architectures when rounding doubles. */
				drx = getDefs().truncDoublePrecision(drx, ILfs.TRUNC_SCALE);
				dry = getDefs().truncDoublePrecision(dry, ILfs.TRUNC_SCALE);
				rx = getDefs().sRound(drx);
				ry = getDefs().sRound(dry);

				/* If 'R' is opposite color from minutia type ... */
				if (binarizedImageData[(ry * imageWidth) + rx] != minutia.get().getType())
				{
					/* Search a specified number of steps (ex. 12) from 'R' in a */
					/* perpendicular direction from the minutia direction until  */
					/* the first white pixel is found.  If a white pixel is      */
					/* found within the specified number of steps, then call     */
					/* this point 'P' (storing the point's edge pixel as well).  */
					if (getImageUtil().searchInDirection(px, py, pex, pey, minutia.get().getType(), 
						rx, ry, -cosTheta, -sinTheta, lfsParams.getPoresPerpSteps(), 
						binarizedImageData, imageWidth, imageHeight) != ILfs.FALSE)
					{
						/* Trace contour from P's edge pixel in counter-clockwise  */
						/* scan and step along specified number of steps (ex. 10). */
						contour = getContour().traceContour(ret, ncontour, lfsParams.getPoresStepsFwd(), 
							px.get(), py.get(), px.get(), py.get(), pex.get(), pey.get(), 
							ILfs.SCAN_COUNTER_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
						/* If system error occurred during trace ... */
						if (ret.get() < ILfs.FALSE)
						{
							/* Return error code. */
							return (ret.get());
						}

						/* If trace was not possible OR loop found OR */
						/* contour is incomplete ...                  */
						if ((ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.LOOP_FOUND) || 
							(ncontour.get() < lfsParams.getPoresStepsFwd()))
						{
							/* If contour allocated and returned ... */
							if ((ret.get() == ILfs.LOOP_FOUND) || (ncontour.get() < lfsParams.getPoresStepsFwd()))
							{
								/* Deallocate the contour. */
								getContour().freeContour(contour);
							}

							if (isShowLogs())
								LOGGER.info(String.format("%d,%d RMB\n", minutia.get().getX(), minutia.get().getY()));

							/* Then remove the minutia. */
							ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
							if (ret.get() != ILfs.FALSE)
							{
								/* If system error, return error code. */
								return (ret.get());
							}
							/* Set remove flag to TRUE. */
							removed = ILfs.TRUE;
						}
						/* Otherwise, traced contour is complete. */
						else
						{
							/* Store last point in contour as point 'B'. */
							bx = contour.getContourX().get(ncontour.get() - 1);
							by = contour.getContourY().get(ncontour.get() - 1);

							/* Deallocate the contour. */
							getContour().freeContour(contour);

							/* Trace contour from P's edge pixel in clockwise scan */
							/* and step along specified number of steps (ex. 8).   */
							contour = getContour().traceContour(ret, ncontour, lfsParams.getPoresStepsBwd(), 
								px.get(), py.get(), px.get(), py.get(), pex.get(), pey.get(), 
								ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
							/* If system error occurred during trace ... */
							if (ret.get() < ILfs.FALSE)
							{
								/* Return error code. */
								return (ret.get());
							}

							/* If trace was not possible OR loop found OR */
							/* contour is incomplete ...                  */
							if ((ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.LOOP_FOUND) || 
								(ncontour.get() < lfsParams.getPoresStepsBwd()))
							{
								/* If contour allocated and returned ... */
								if ((ret.get() == ILfs.LOOP_FOUND) || 
									(ncontour.get() < lfsParams.getPoresStepsBwd()))
								{
									/* Deallocate the contour. */
									getContour().freeContour(contour);
								}

								if (isShowLogs())
									LOGGER.info(String.format("%d,%d RMD\n", minutia.get().getX(), minutia.get().getY()));

								/* Then remove the minutia. */
								ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
								if (ret.get() != ILfs.FALSE)
								{
									/* If system error, return error code. */
									return (ret.get());
								}
								/* Set remove flag to TRUE. */
								removed = ILfs.TRUE;
							}
							/* Otherwise, traced contour is complete. */
							else
							{
								/* Store last point in contour as point 'D'. */
								dx = contour.getContourX().get(ncontour.get() - 1);
								dy = contour.getContourY().get(ncontour.get() - 1);

								/* Deallocate the contour. */
								getContour().freeContour(contour);
								/* Search a specified number of steps (ex. 12) from */
								/* 'R' in opposite direction of that used to find   */
								/* 'P' until the first white pixel is found.  If a  */
								/* white pixel is found within the specified number */
								/* of steps, then call this point 'Q' (storing the  */
								/* point's edge pixel as well).                     */
								if (getImageUtil().searchInDirection(qx, qy, qex, qey, minutia.get().getType(), 
									rx, ry, cosTheta, sinTheta, lfsParams.getPoresPerpSteps(), 
									binarizedImageData, imageWidth, imageHeight) != ILfs.FALSE)
								{
									/* Trace contour from Q's edge pixel in clockwise */
									/* scan and step along specified number of steps  */
									/* (ex. 10).                                      */
									contour = getContour().traceContour(ret, ncontour, 
										lfsParams.getPoresStepsFwd(), qx.get(), qy.get(), 
										qx.get(), qy.get(), qex.get(), qey.get(), 
										ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
									/* If system error occurred during trace ... */
									if (ret.get() < ILfs.FALSE)
									{
										/* Return error code. */
										return (ret.get());
									}

									/* If trace was not possible OR loop found OR */
									/* contour is incomplete ...                  */
									if ((ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.LOOP_FOUND) || 
										(ncontour.get() < lfsParams.getPoresStepsFwd()))
									{
										/* If contour allocated and returned ... */
										if ((ret.get() == ILfs.LOOP_FOUND) || 
											(ncontour.get() < lfsParams.getPoresStepsFwd()))
										{
											/* Deallocate the contour. */
											getContour().freeContour(contour);
										}

										if (isShowLogs())
											LOGGER.info(String.format("%d,%d RMA\n", minutia.get().getX(), minutia.get().getY()));

										/* Then remove the minutia. */
										ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
										if (ret.get() != ILfs.FALSE)
										{
											/* If system error, return error code. */
											return (ret.get());
										}
										/* Set remove flag to TRUE. */
										removed = ILfs.TRUE;
									}
									/* Otherwise, traced contour is complete. */
									else
									{
										/* Store last point in contour as point 'A'. */
										ax = contour.getContourX().get(ncontour.get() - 1);
										ay = contour.getContourY().get(ncontour.get() - 1);

										/* Deallocate the contour. */
										getContour().freeContour(contour);
										
										/* Trace contour from Q's edge pixel in    */
										/* counter-clockwise scan and step along a */
										/* specified number of steps (ex. 8).      */
										contour = getContour().traceContour(ret, ncontour, 
											lfsParams.getPoresStepsBwd(), qx.get(), qy.get(), 
											qx.get(), qy.get(), qex.get(), qey.get(), ILfs.SCAN_COUNTER_CLOCKWISE, 
											binarizedImageData, imageWidth, imageHeight);
										/* If system error occurred during scan ... */
										if (ret.get() < ILfs.FALSE)
										{
											/* Return error code. */
											return (ret.get());
										}
							

										/* If trace was not possible OR loop found OR */
										/* contour is incomplete ...                  */
										if ((ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.LOOP_FOUND) || 
											(ncontour.get() < lfsParams.getPoresStepsBwd()))
										{
											/* If contour allocated and returned ... */
											if ((ret.get() == ILfs.LOOP_FOUND) || 
												(ncontour.get() < lfsParams.getPoresStepsBwd()))
											{
												/* Deallocate the contour. */
												getContour().freeContour(contour);
											}

											if (isShowLogs())
												LOGGER.info(String.format("%d,%d RMC\n", minutia.get().getX(), minutia.get().getY()));

											/* Then remove the minutia. */
											ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
											if (ret.get() != ILfs.FALSE)
											{
												/* If system error, return error code. */
												return (ret.get());
											}
											/* Set remove flag to TRUE. */
											removed = ILfs.TRUE;
										}
										/* Otherwise, traced contour is complete. */
										else
										{
											/* Store last point in contour as 'C'. */
											cx = contour.getContourX().get(ncontour.get() - 1);
											cy = contour.getContourY().get(ncontour.get() - 1);
											 
											/* Deallocate the contour. */
											getContour().freeContour(contour);

											/* Compute squared distance between points */
											/* 'A' and 'B'.                            */
											ab2 = getLfsUtil().squaredDistance(ax, ay, bx, by);
											/* Compute squared distance between points */
											/* 'C' and 'D'.                            */
											cd2 = getLfsUtil().squaredDistance(cx, cy, dx, dy);
											/* If CD distance is not near zero */
											/* (ex. 0.5) ...                   */
											if (cd2 > lfsParams.getPoresMinDist2())
											{
												/* Compute ratio of squared distances. */
												ratio = ab2 / cd2;

												/* If ratio is small enough (ex. 2.25)...*/
												if (ratio <= lfsParams.getPoresMaxRatio())
												{
													if (isShowLogs())
													{
														LOGGER.info(String.format("%d,%d", minutia.get().getX(), minutia.get().getY()));
														LOGGER.info(String.format("R=%d,%d P=%d,%d B=%d,%d D=%d,%d Q=%d,%d A=%d,%d C=%d,%d ", rx, ry, px.get(), py.get(), bx, by, dx, dy, qx.get(), qy.get(), ax, ay, cx, cy));
														LOGGER.info(String.format("RMRATIO %f\n", ratio));
													}
													/* Then assume pore & remove minutia. */
													ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
													if (ret.get() != ILfs.FALSE)
													{
														/* If system error, return error code. */
														return (ret.get());
													}
													/* Set remove flag to TRUE. */
													removed = ILfs.TRUE;
												}
												/* Otherwise, ratio to big, so assume */
												/* legitimate minutia.                */
											} // Else, cd2 too small.
										} // Done with C.
									} // Done with A.
								}
								/* Otherwise, Q not found ... */
								else
								{
									if (isShowLogs())
										LOGGER.info(String.format("%d,%d RMQ\n", minutia.get().getX(), minutia.get().getY()));

									/* Then remove the minutia. */
									ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
									if (ret.get() != ILfs.FALSE)
									{
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
					else
					{
						if (isShowLogs())
							LOGGER.info(String.format("%d,%d RMP\n", minutia.get().getX(), minutia.get().getY()));

						/* Then remove the minutia. */
						ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
						if (ret.get() != ILfs.FALSE)
						{
							/* If system error, return error code. */
							return (ret.get());
						}
						/* Set remove flag to TRUE. */
						removed = ILfs.TRUE;
					}
				} // Else, R is on pixel the same color as type, so do not
				/* remove minutia point and skip to next one.            */
			} // Else block is unreliable or has INVALID direction.

			/* If current minutia not removed ... */
			if (removed == ILfs.FALSE)
			{
				/* Bump to next minutia in list. */
				minutiaIndex++;
			}
			/* Otherwise, next minutia has slid into slot of current removed one. */

		} // End While minutia remaining in list.
	
		/* Return normally. */
		return ILfs.FALSE;
	}


	/*************************************************************************
	**************************************************************************
	#cat: removeOrAdjustSideMinutiaeV2 - Removes loops or minutia points that
	#cat:              are not on complete contours of specified length. If the
	#cat:              contour is complete, then the minutia is adjusted based
	#cat:              on a minmax analysis of the rotated y-coords of the contour.
	   Input:
	      oMinutiae  - list of true and false minutiae
	      binarizedImageData     - binary image data (0==while & 1==black)
	      imageWidth        - width (in pixels) of image
	      imageHeight        - height (in pixels) of image
	      directionMap - map of image blocks containing directional ridge flow
	      mappedImageWidth        - width (in blocks) of the map
	      mappedImageHeight        - height (in blocks) of the map
	      lfsParams  - parameters and thresholds for controlling LFS
	   Output:
	      oMinutiae  - list of pruned minutiae
	   Return Code:
	      Zero     - successful completion
	      Negative - system error
	**************************************************************************/
	public int removeOrAdjustSideMinutiaeV2(AtomicReference<Minutiae> oMinutiae, int[] binarizedImageData, 
		final int imageWidth, final int imageHeight, AtomicIntegerArray directionMap, 
		final int mappedImageWidth, final int mappedImageHeight, final LfsParams lfsParams) {
		int minutiaIndex;
		AtomicInteger ret = new AtomicInteger(0);
		Minutia minutia;
		double piFactor, theta, sinTheta, cosTheta;
		Contour contour = null;
		AtomicInteger oNoOfContour = new AtomicInteger(0);
		AtomicIntegerArray oRotatedYOfMinutiaContour;
		int minLoc;
		AtomicIntegerArray minmaxValue, minmaxIndex, minmaxType;
		AtomicInteger minmaxAlloc = new AtomicInteger(0), 
			minmaxNum = new AtomicInteger(0);
		double dRotatedYValue;
		int blockX, blockY;

		if (isShowLogs())
			LOGGER.info("\nADJUSTING SIDE MINUTIA:\n");

		 /* Allocate working memory for holding rotated y-coord of a */
		/* minutia's contour.                                       */
		oRotatedYOfMinutiaContour = new AtomicIntegerArray((lfsParams.getSideHalfContour() << 1) + 1);
		/* Compute factor for converting integer directions to radians. */
		piFactor = ILfs.M_PI/ (double)lfsParams.getNumDirections();

		minutiaIndex = 0;
		/* Foreach minutia remaining in list ... */
		while (minutiaIndex < oMinutiae.get().getNum())
		{
			/* Assign a temporary pointer. */
			minutia = oMinutiae.get().getList().get(minutiaIndex);

			/* Extract a contour centered on the minutia point (ex. 7 pixels */
			/* in both directions).                                          */
			contour = getContour().getCenteredContour(ret, oNoOfContour, lfsParams.getSideHalfContour(), minutia.getX(), minutia.getY(), minutia.getEx(), minutia.getEy(), binarizedImageData, imageWidth, imageHeight);

			/* If system error occurred ... */
			if (ret.get() < 0)
			{
				/* Deallocate working memory. */
				getFree().free(oRotatedYOfMinutiaContour);
				/* Return error code. */
				return (ret.get());
			}

			/* If we didn't succeed in extracting a complete contour for any */
			/* other reason ...                                              */
			if ((ret.get() == ILfs.LOOP_FOUND) || (ret.get() == ILfs.IGNORE) || (ret.get() == ILfs.INCOMPLETE))
			{
				if (isShowLogs())
					LOGGER.info(String.format("%d,%d RM1\n", minutia.getX(), minutia.getY()));

				/* Remove minutia from list. */
				ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
				if (ret.get() != ILfs.FALSE)
				{
					/* Deallocate working memory. */
					getFree().free(oRotatedYOfMinutiaContour);
					/* Return error code. */
					return (ret.get());
				}
				/* No need to advance because next minutia has "slid" */
				/* into position pointed to by 'i'.                   */
			}
			/* Otherwise, a complete contour was found and extracted ... */
			else
			{
				/* Rotate contour points by negative angle of feature's direction. */
				/* The contour of a well-formed minutia point will form a bowl     */
				/* shape concaved in the direction of the minutia.  By rotating    */
				/* the contour points by the negative angle of feature's direction */
				/* the bowl will be transformed to be concaved upwards and minima  */
				/* and maxima of the transformed y-coords can be analyzed to       */
				/* determine if the minutia is "well-formed" or not.  If well-     */
				/* formed then the position of the minutia point is adjusted.  If  */
				/* not well-formed, then the minutia point is removed altogether.  */

				/* Normal rotation of T degrees around the origin of */
				/*      the point (x,y):                             */
				/*         rx = x*cos(T) - y*sin(T)                  */
				/*         ry = x*cos(T) + y*sin(T)                  */
				/*      The rotation here is for -T degrees:         */
				/*         rx = x*cos(-T) - y*sin(-T)                */
				/*         ry = x*cos(-T) + y*sin(-T)                */
				/*      which can be written:                        */
				/*         rx = x*cos(T) + y*sin(T)                  */
				/*         ry = x*sin(T) - y*cos(T)                  */

				/* Convert minutia's direction to radians. */
				theta = (double)minutia.getDirection() * piFactor;
				/* Compute sine and cosine values at theta for rotation. */
				sinTheta = Math.sin(theta);
				cosTheta = Math.cos(theta);

				for (int j = 0; j < oNoOfContour.get(); j++)
				{
					/* We only need to rotate the y-coord (don't worry     */
					/* about rotating the x-coord or contour edge pixels). */
					dRotatedYValue = ((double)contour.getContourX().get(j) * sinTheta) - 
						((double)contour.getContourY().get(j) * cosTheta);
					/* Need to truncate precision so that answers are consistent */
					/* on different computer architectures when rounding doubles. */
					dRotatedYValue = getDefs().truncDoublePrecision(dRotatedYValue, ILfs.TRUNC_SCALE);
					oRotatedYOfMinutiaContour.set(j, getDefs().sRound(dRotatedYValue));
				}

				/* Locate relative minima and maxima in vector of rotated */
				/* y-coords of current minutia's contour.                 */

				/* Determine maximum length for allocation of buffers. */
				/* If there are fewer than 3 items ...                */
				if (oNoOfContour.get() < 3)
				{
					/* Then no min/max is possible, so set allocated length */
					/* to 0 and return.                                     */
					return (0);
				}
				 
				/* Otherwise, set allocation length to number of items - 2    */
				/* (one for the first item in the list, and on for the last). */
				/* Every other intermediate point can potentially represent a */
				/* min or max.                                                */
				minmaxAlloc.set (oNoOfContour.get() - 2);
				minmaxValue = new AtomicIntegerArray(minmaxAlloc.get());
				minmaxType = new AtomicIntegerArray(minmaxAlloc.get());
				minmaxIndex = new AtomicIntegerArray(minmaxAlloc.get());
				
				ret.set(getLfsUtil().minMaxs(minmaxValue, minmaxType, minmaxIndex, minmaxAlloc, minmaxNum, 
					oRotatedYOfMinutiaContour, oNoOfContour.get()));
				if (ret.get() < ILfs.FALSE)
				{
					/* If system error, then deallocate working memories. */
					getFree().free(oRotatedYOfMinutiaContour);
					getContour().freeContour(contour);
					/* Return error code. */
					return (ret.get());
				}

				/* If one and only one minima was found in rotated y-coord */
				/* of contour ...                                          */
				if ((minmaxNum.get() == 1) && (minmaxType.get(0) == -1))
				{
					if (isShowLogs())
						LOGGER.info(String.format("%d,%d ", minutia.getX(), minutia.getY()));

					/* Reset loation of minutia point to contour point at minima. */
					minutia.setX(contour.getContourX().get(minmaxIndex.get(0)));
					minutia.setY(contour.getContourY().get(minmaxIndex.get(0)));
					minutia.setEx(contour.getContourEx().get(minmaxIndex.get(0)));
					minutia.setEy(contour.getContourEy().get(minmaxIndex.get(0)));

					/* Must check if adjusted minutia is now in INVALID block ... */
					blockX = minutia.getX() / lfsParams.getBlockSize();
					blockY = minutia.getY() / lfsParams.getBlockSize();
					if (getPixelValueFromAtomicArray(directionMap, blockX, blockY, mappedImageWidth, mappedImageHeight) == ILfs.INVALID_DIR)
					{
						/* Remove minutia from list. */
						ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
						if (ret.get() != ILfs.FALSE)
						{
							/* Deallocate working memory. */
							getFree().free(oRotatedYOfMinutiaContour);
							getContour().freeContour(contour);
							if (minmaxAlloc.get() > ILfs.FALSE)
							{
								getFree().free(minmaxValue);
								getFree().free(minmaxType);
								getFree().free(minmaxIndex);
							}
							/* Return error code. */
							return (ret.get());
						}
						/* No need to advance because next minutia has "slid" */
						/* into position pointed to by 'i'.                   */

						if (isShowLogs())
							LOGGER.info("RM2\n");
					}
					else
					{
						/* Advance to the next minutia in the list. */
						minutiaIndex++;
						if (isShowLogs())
							LOGGER.info(String.format("AD1 %d,%d\n", minutia.getX(), minutia.getY()));
					}
				}
				/* If exactly 3 min/max found and they are min-max-min ... */
				else if ((minmaxNum.get() == 3) && (minmaxType.get(0) == -1))
				{
					/* Choose minima location with smallest rotated y-coord. */
					if (minmaxValue.get(0) < minmaxValue.get(2))
					{
						minLoc = minmaxIndex.get(0);
					}
					else
					{
				  		minLoc = minmaxIndex.get(2);
					}

					if (isShowLogs())
						LOGGER.info(String.format("%d,%d ", minutia.getX(), minutia.getY()));

					/* Reset loation of minutia point to contour point at minima. */
					minutia.setX(contour.getContourX().get(minLoc));
					minutia.setY(contour.getContourY().get(minLoc));
					minutia.setEx(contour.getContourEx().get(minLoc));
					minutia.setEy(contour.getContourEy().get(minLoc));

					/* Must check if adjusted minutia is now in INVALID block ... */
					blockX = minutia.getX() / lfsParams.getBlockSize();
					blockY = minutia.getY() / lfsParams.getBlockSize();

					if (getPixelValueFromAtomicArray(directionMap, blockX, blockY, mappedImageWidth, mappedImageHeight) == ILfs.INVALID_DIR)
					{
					   /* Remove minutia from list. */
						ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
						if (ret.get() != ILfs.FALSE)
						{
							/* Deallocate working memory. */
							getFree().free(oRotatedYOfMinutiaContour);
							getContour().freeContour(contour);
							if (minmaxAlloc.get() > ILfs.FALSE)
							{
								getFree().free(minmaxValue);
								getFree().free(minmaxType);
								getFree().free(minmaxIndex);
							}
							/* Return error code. */
							return (ret.get());
						}
						/* No need to advance because next minutia has "slid" */
						/* into position pointed to by 'i'.                   */

						if (isShowLogs())
							LOGGER.info("RM3\n");
					}
					else
					{
						/* Advance to the next minutia in the list. */
						minutiaIndex++;
						if (isShowLogs())
							LOGGER.info(String.format("AD2 %d,%d\n", minutia.getX(), minutia.getY()));
					}
				 }
				 /* Otherwise, ... */
				else
				{					
					if (isShowLogs())
						LOGGER.info(String.format("%d,%d RM4\n", minutia.getX(), minutia.getY()));

					/* Remove minutia from list. */
					ret.set(getMinutiaHelper().removeMinutia(minutiaIndex, oMinutiae));
					if (ret.get() != ILfs.FALSE)
					{
						/* If system error, then deallocate working memories. */
						getFree().free(oRotatedYOfMinutiaContour);
						getContour().freeContour(contour);
						if (minmaxAlloc.get() > ILfs.FALSE)
						{
							getFree().free(minmaxValue);
							getFree().free(minmaxType);
							getFree().free(minmaxIndex);
						}
						/* Return error code. */
						return (ret.get());

					}
					/* No need to advance because next minutia has "slid" */
					/* into position pointed to by 'i'.                   */
				}
				/* Deallocate contour and min/max buffers. */
				getContour().freeContour(contour);
				if (minmaxAlloc.get() > ILfs.FALSE)
				{
					getFree().free(minmaxValue);
					getFree().free(minmaxType);
					getFree().free(minmaxIndex);
				}
			} // End else contour extracted.
		} // End while not end of minutiae list.

		/* Deallocate working memory. */
		oRotatedYOfMinutiaContour = null;

		/* Return normally. */		   
		return ILfs.FALSE;
	}
}