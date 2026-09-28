package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.ILoop;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.Minutia;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import org.mosip.nist.nfiq1.common.ILfs.Shape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loop, island and lake detection and removal for the MINDTCT minutiae detector.
 *
 * This class is the Java port of NIST's {@code loop.c} from the LFS (Latent Fingerprint System)
 * library used by MINDTCT / NFIQ 1.0. A "loop" is a closed contour in the binarized fingerprint
 * image: either an <i>island</i> (a small closed black ridge fragment) or a <i>lake</i> (a small
 * closed white valley enclosed by ridge). Such features are usually noise and generate spurious
 * minutia pairs. The routines here:
 * <ul>
 * <li>test whether a minutia (or a pair of minutiae) lies on a loop or on a hook,</li>
 * <li>determine the orientation (clockwise or counter-clockwise) of a traced contour,</li>
 * <li>measure a loop's aspect (narrowest and widest cross distances), and</li>
 * <li>either convert a large, elongated loop into two candidate minutiae, or fill (erase) the loop
 * in the binary image so that it no longer produces false minutiae.</li>
 * </ul>
 * Contour tracing itself is delegated to {@link Contour}, chain coding to {@link ChainCode}, and
 * minutia bookkeeping to {@link MinutiaHelper}.
 *
 * Pixel coordinates are zero-based with {@code x} increasing to the right and {@code y}
 * increasing downwards; image buffers are row-major ({@code index = y * imageWidth + x}).
 * Return codes follow the NIST convention: {@link ILfs#FALSE} ({@code 0}) means success or "not
 * found", positive codes such as {@link ILfs#LOOP_FOUND}, {@link ILfs#HOOK_FOUND} or
 * {@link ILfs#IGNORE} carry results, and negative values are system errors.
 *
 * The class is a lazily created singleton obtained through {@link #getInstance()}. It holds no
 * per-call mutable state, but the binary image and minutiae structures passed to its methods
 * are modified in place, so callers must not share those structures across threads without
 * external synchronization.
 */
public class Loop extends MindTct implements ILoop {
	/**
	 * SLF4J logger for this class; used to report non-fatal warnings such as an unexpected shape
	 * encountered while filling a loop.
	 */
	private static final Logger logger = LoggerFactory.getLogger(Loop.class);
	/**
	 * Lazily created singleton instance, returned by {@link #getInstance()}; guarded by the class
	 * monitor.
	 */
	private static Loop instance;

	/**
	 * Creates the singleton instance. Private to enforce the singleton pattern; use
	 * {@link #getInstance()} instead.
	 */
	private Loop() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * The method is {@code synchronized}, so initialization is thread-safe.
	 *
	 * @return the singleton {@code Loop} instance, never {@code null}
	 */
	public static synchronized Loop getInstance() {
		if (instance == null) {
			instance = new Loop();
		}
		return instance;
	}

	/**
	 * Returns the shared {@link Shapes} helper (NIST {@code shape.c}) used to convert a loop contour
	 * into row-wise shape spans for filling.
	 *
	 * @return the {@link Shapes} singleton
	 */
	public Shapes getShapes() {
		return Shapes.getInstance();
	}

	/**
	 * Returns the shared {@link ChainCode} helper (NIST {@code chaincod.c}) used to derive and
	 * analyze Freeman chain codes of contours.
	 *
	 * @return the {@link ChainCode} singleton
	 */
	public ChainCode getChainCode() {
		return ChainCode.getInstance();
	}

	/**
	 * Returns the shared {@link Free} helper, the Java counterpart of NIST's {@code free.c}
	 * deallocation routines (largely no-ops under garbage collection, kept for parity with the C
	 * source).
	 *
	 * @return the {@link Free} singleton
	 */
	public Free getFree() {
		return Free.getInstance();
	}

	/**
	 * Returns the shared {@link MinutiaHelper} (NIST {@code minutia.c}) used to create, classify,
	 * add and remove minutiae.
	 *
	 * @return the {@link MinutiaHelper} singleton
	 */
	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}

	/**
	 * Returns the shared {@link Contour} helper (NIST {@code contour.c}) used to trace feature
	 * contours in the binary image.
	 *
	 * @return the {@link Contour} singleton
	 */
	public Contour getContour() {
		return Contour.getInstance();
	}

	/**
	 * Returns the shared {@link LfsUtil} helper (NIST {@code util.c}) providing geometric utilities
	 * such as squared distance and line-to-direction conversion.
	 *
	 * @return the {@link LfsUtil} singleton
	 */
	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	/**
	 * Flags which minutiae in a list lie on loops of a specified maximum circumference.
	 *
	 * Port of NIST {@code get_loop_list()}. Every minutia in the list is examined in order:
	 * <ul>
	 * <li>Ridge endings can never lie on a loop, so their flag is set to {@link ILfs#FALSE}.</li>
	 * <li>Bifurcations are tested with {@link #onLoop}. If the minutia lies on a qualifying loop
	 * (a lake around a valley), its flag is set to {@link ILfs#TRUE}; if not, {@link ILfs#FALSE}.</li>
	 * <li>If the minutia's contour could not be traced ({@link ILfs#IGNORE}), which can happen
	 * because other routines edit the image dynamically, the minutia is removed from the list and
	 * the next minutia slides into its position.</li>
	 * </ul>
	 * Because minutiae may be removed, the flag at index {@code i} of {@code onloop} corresponds to
	 * the minutia at index {@code i} of the list <i>after</i> this call returns.
	 *
	 * @param onloop             output array of loop flags, one per minutia; must be pre-allocated by
	 *                           the caller with at least as many entries as there are minutiae. Each
	 *                           entry is set to {@link ILfs#TRUE} (on a loop) or {@link ILfs#FALSE}
	 * @param oMinutiae          reference to the list of true and false minutiae; minutiae whose
	 *                           contour cannot be traced are removed from this list in place
	 * @param loopLen            maximum length (number of contour steps) of loop searched for
	 * @param binarizedImageData row-major binary image data ({@code 0} = white, {@code 1} = black)
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @return {@link ILfs#FALSE} ({@code 0}) on successful completion; a negative system error code
	 *         propagated from {@link #onLoop} or from minutia removal otherwise
	 */
	public int getLoopList(AtomicIntegerArray onloop, AtomicReference<Minutiae> oMinutiae, final int loopLen,
			int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int i;
		int ret;
		Minutia minutia;

		/* Allocate a list of onloop flags (one for each minutia in list). */
		// Allocate from calling function

		i = 0;
		/* Foreach minutia remaining in list ... */
		while (i < oMinutiae.get().getNum()) {
			/* Assign a temporary pointer. */
			minutia = oMinutiae.get().getList().get(i);
			/* If current minutia is a bifurcation ... */
			if (minutia.getType() == ILfs.BIFURCATION) {
				/* Check to see if it is on a loop of specified length. */
				ret = onLoop(minutia, loopLen, binarizedImageData, imageWidth, imageHeight);
				/* If minutia is on a loop... */
				if (ret == ILfs.LOOP_FOUND) {
					/* Then set the onloop flag to TRUE. */
					onloop.set(i, ILfs.TRUE);
					/* Advance to next minutia in the list. */
					i++;
				}
				/* If on loop test IGNORED ... */
				else if (ret == ILfs.IGNORE) {
					/* Remove the current minutia from the list. */
					if ((ret = getMinutiaHelper().removeMinutia(i, oMinutiae)) != 0) {
						/* Return error code. */
						return (ret);
					}
					/* No need to advance because next minutia has "slid" */
					/* into position pointed to by 'i'. */
				}
				/* If the minutia is NOT on a loop... */
				else if (ret == ILfs.FALSE) {
					/* Then set the onloop flag to FALSE. */
					onloop.set(i, ILfs.FALSE);
					/* Advance to next minutia in the list. */
					i++;
				}
				/* Otherwise, an ERROR occurred while looking for loop. */
				else {
					/* Return error code. */
					return (ret);
				}
			}
			/* Otherwise, the current minutia is a ridge-ending... */
			else {
				/* Ridge-endings will never be on a loop, so set flag to FALSE. */
				onloop.set(i, ILfs.FALSE);
				/* Advance to next minutia in the list. */
				i++;
			}
		}

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Determines whether a minutia point lies on a loop (island or lake) of a specified maximum
	 * circumference.
	 *
	 * Port of NIST {@code on_loop()}. The contour of the feature is traced clockwise starting at the
	 * minutia point (using the minutia's edge pixel as the starting neighbor) for up to
	 * {@code maxLoopLen} steps. If the trace returns to the starting point within that many steps,
	 * the minutia lies on a qualifying loop.
	 *
	 * @param minutia            the minutia to test; its {@code x}/{@code y} and edge
	 *                           {@code ex}/{@code ey} pixel coordinates are used as trace start
	 * @param maxLoopLen         maximum number of contour steps (maximum loop circumference) to trace
	 * @param binarizedImageData row-major binary image data ({@code 0} = white, {@code 1} = black)
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @return {@link ILfs#LOOP_FOUND} ({@code 1}) if the minutia lies on a qualifying loop;
	 *         {@link ILfs#FALSE} ({@code 0}) if it does not; {@link ILfs#IGNORE} ({@code 2}) if the
	 *         minutia's contour could not be traced; a negative system error code otherwise
	 */
	public int onLoop(final Minutia minutia, final int maxLoopLen, int[] binarizedImageData, final int imageWidth,
			final int imageHeight) {
		AtomicInteger ret = new AtomicInteger(0);
		AtomicInteger oNoOfContour = new AtomicInteger(0);

		/* Trace the contour of the feature starting at the minutia point */
		/* and stepping along up to the specified maximum number of steps. */
		Contour contour = getContour().traceContour(ret, oNoOfContour, maxLoopLen, minutia.getX(), minutia.getY(),
				minutia.getX(), minutia.getY(), minutia.getEx(), minutia.getEy(), ILfs.SCAN_CLOCKWISE,
				binarizedImageData, imageWidth, imageHeight);
		/* If trace was not possible ... */
		if (ret.get() == ILfs.IGNORE) {
			getFree().free(contour);
			return (ret.get());
		}

		/* If the trace completed a loop ... */
		if (ret.get() == ILfs.LOOP_FOUND) {
			getFree().free(contour);
			return (ILfs.LOOP_FOUND);
		}

		/* If the trace successfully followed the minutia's contour, but did */
		/* not complete a loop within the specified number of steps ... */
		if (ret.get() == ILfs.FALSE) {
			getFree().free(contour);
			return (ILfs.FALSE);
		}

		getFree().free(contour);
		/* Otherwise, the trace had an error in following the contour ... */
		return (ret.get());
	}

	/**
	 * Determines whether two minutia points lie on the same loop (island or lake) and, if so,
	 * returns the loop's contour.
	 *
	 * Port of NIST {@code on_island_lake()}. The contour is first traced clockwise from the first
	 * minutia for up to {@code maxHalfLoop} steps, stopping if the second minutia is reached. If it
	 * is, a second clockwise trace is run from the second minutia back towards the first. When both
	 * half traces succeed, the two halves are joined into one full loop contour of
	 * {@code nContour1 + nContour2 + 2} points, stored in this order: first minutia, first half
	 * contour, second minutia, second half contour.
	 *
	 * @param ret                output result code, set to one of: {@link ILfs#LOOP_FOUND} if the
	 *                           minutiae lie on the same qualifying loop; {@link ILfs#FALSE} if they
	 *                           do not; {@link ILfs#IGNORE} if a contour could not be traced; a
	 *                           negative system error code on failure
	 * @param oncontour          output number of points in the returned loop contour (set only when
	 *                           a loop is found)
	 * @param firstMinutia       first minutia point
	 * @param secondMinutia      second minutia point
	 * @param maxHalfLoop        maximum number of contour steps for each half of the loop (half the
	 *                           loop circumference searched for)
	 * @param binarizedImageData row-major binary image data ({@code 0} = white, {@code 1} = black)
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @return the full loop {@link Contour} when {@code ret} is {@link ILfs#LOOP_FOUND}, holding the
	 *         x/y pixel coordinates of each contour point ({@code contourX}/{@code contourY}) and of
	 *         each point's edge pixel ({@code contourEx}/{@code contourEy}); otherwise
	 *         {@code null} (or an unusable contour if loop contour allocation failed)
	 */
	public Contour onIslandLake(AtomicInteger ret, AtomicInteger oncontour, Minutia firstMinutia, Minutia secondMinutia,
			final int maxHalfLoop, int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int i;
		int l;
		Contour contour1 = null;
		Contour contour2 = null;
		Contour contourLoop = null;
		AtomicInteger nContour1 = new AtomicInteger(0);
		AtomicInteger nContour2 = new AtomicInteger(0);
		AtomicInteger nLoop = new AtomicInteger(0);

		/* Trace the contour of the feature starting at the 1st minutia point */
		/* and stepping along up to the specified maximum number of steps or */
		/* until 2nd mintuia point is encountered. */
		contour1 = getContour().traceContour(ret, nContour1, maxHalfLoop, secondMinutia.getX(), secondMinutia.getY(),
				firstMinutia.getX(), firstMinutia.getY(), firstMinutia.getEx(), firstMinutia.getEy(),
				ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		/* If trace was not possible, return IGNORE. */
		if (ret.get() == ILfs.IGNORE) {
			return contourLoop;
		}

		/* If the trace encounters 2nd minutia point ... */
		if (ret.get() == ILfs.LOOP_FOUND) {
			/* Now, trace the contour of the feature starting at the 2nd minutia, */
			/* continuing to search for edge neighbors clockwise, and stepping */
			/* along up to the specified maximum number of steps or until 1st */
			/* mintuia point is encountered. */
			contour2 = getContour().traceContour(ret, nContour2, maxHalfLoop, firstMinutia.getX(), firstMinutia.getY(),
					secondMinutia.getX(), secondMinutia.getY(), secondMinutia.getEx(), secondMinutia.getEy(),
					ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
			/* If trace was not possible, return IGNORE. */
			if (ret.get() == ILfs.IGNORE) {
				getFree().free(contour1);
				return contourLoop;
			}

			/* If the 2nd trace encounters 1st minutia point ... */
			if (ret.get() == ILfs.LOOP_FOUND) {
				/* Combine the 2 half loop contours into one full loop. */
				/* Compute loop length (including the minutia pair). */
				nLoop.set(nContour1.get() + nContour2.get() + 2);

				/* Allocate loop contour. */
				contourLoop = getContour().allocateContour(ret, nLoop.get());
				if (ret.get() != ILfs.FALSE) {
					getFree().free(contour1);
					getFree().free(contour2);
					return contourLoop;
				}

				/* Store 1st minutia. */
				l = 0;
				contourLoop.getContourX().set(l, firstMinutia.getX());
				contourLoop.getContourY().set(l, firstMinutia.getY());
				contourLoop.getContourEx().set(l, firstMinutia.getEx());
				contourLoop.getContourEy().set(l++, firstMinutia.getEy());

				/* Store first contour. */
				for (i = 0; i < nContour1.get(); i++) {
					contourLoop.getContourX().set(l, contour1.getContourX().get(i));
					contourLoop.getContourY().set(l, contour1.getContourY().get(i));
					contourLoop.getContourEx().set(l, contour1.getContourEx().get(i));
					contourLoop.getContourEy().set(l++, contour1.getContourEy().get(i));
				}
				/* Store 2nd minutia. */
				contourLoop.getContourX().set(l, secondMinutia.getX());
				contourLoop.getContourY().set(l, secondMinutia.getY());
				contourLoop.getContourEx().set(l, secondMinutia.getEx());
				contourLoop.getContourEy().set(l++, secondMinutia.getEy());

				/* Store 2nd contour. */
				for (i = 0; i < nContour2.get(); i++) {
					contourLoop.getContourX().set(l, contour2.getContourX().get(i));
					contourLoop.getContourY().set(l, contour2.getContourY().get(i));
					contourLoop.getContourEx().set(l, contour2.getContourEx().get(i));
					contourLoop.getContourEy().set(l++, contour2.getContourEy().get(i));
				}

				/* Deallocate the half loop contours. */
				getFree().free(contour1);
				getFree().free(contour2);

				/* Assign loop contour to return pointers. */
				oncontour.set(nLoop.get());

				/* Then return that an island/lake WAS found (LOOP_FOUND). */
				ret.set(ILfs.LOOP_FOUND);
				return contourLoop;
			}

			/* If the trace successfully followed 2nd minutia's contour, but */
			/* did not encounter 1st minutia point within the specified number */
			/* of steps ... */
			if (ret.get() == ILfs.FALSE) {
				/* Deallocate the two contours. */
				getFree().free(contour1);
				getFree().free(contour2);
				/* Then return that an island/lake was NOT found (FALSE). */
				ret.set(ILfs.FALSE);
				return contourLoop;
			}

			/* Otherwise, the 2nd trace had an error in following the contour ... */
			getFree().free(contour1);
			return contourLoop;
		}

		/* If the 1st trace successfully followed 1st minutia's contour, but */
		/* did not encounter the 2nd minutia point within the specified number */
		/* of steps ... */
		if (ret.get() == ILfs.FALSE) {
			getFree().free(contour1);
			/* Then return that an island/lake was NOT found (FALSE). */
			ret.set(ILfs.FALSE);
			return contourLoop;
		}

		/* Otherwise, the 1st trace had an error in following the contour ... */
		return contourLoop;
	}

	/**
	 * Determines whether two minutia points lie on a hook on the side of a ridge or valley.
	 *
	 * Port of NIST {@code on_hook()}. This routine should only be called when the two minutiae are
	 * of "opposite" type. The contour is traced starting from the first minutia's edge pixel for up
	 * to {@code maxHookLen} steps, first searching edge neighbors clockwise and, if the second
	 * minutia was not reached, again counter-clockwise. Reaching the second minutia in either
	 * direction means both lie on the same hook.
	 *
	 * @param firstMinutia       first minutia point (trace starts at its edge pixel)
	 * @param secondMinutia      second minutia point (trace target)
	 * @param maxHookLen         maximum length (number of contour steps) searched along for a hook
	 * @param binarizedImageData row-major binary image data ({@code 0} = white, {@code 1} = black)
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @return {@link ILfs#HOOK_FOUND} ({@code 1}) if the minutiae lie on the same qualifying hook;
	 *         {@link ILfs#FALSE} ({@code 0}) if they do not; {@link ILfs#IGNORE} ({@code 2}) if the
	 *         contour could not be traced; a negative system error code otherwise
	 */
	public int onHook(Minutia firstMinutia, Minutia secondMinutia, final int maxHookLen, int[] binarizedImageData,
			final int imageWidth, final int imageHeight) {
		AtomicInteger ret = new AtomicInteger(0);
		Contour contour = null;
		AtomicInteger oNoOfContour = new AtomicInteger(0);

		/* NOTE: This routine should only be called when the 2 minutia points */
		/* are of "opposite" type. */

		/* Trace the contour of the feature starting at the 1st minutia's */
		/* "edge" point and stepping along up to the specified maximum number */
		/* of steps or until the 2nd minutia point is encountered. */
		/* First search for edge neighbors clockwise. */
		contour = getContour().traceContour(ret, oNoOfContour, maxHookLen, secondMinutia.getX(), secondMinutia.getY(),
				firstMinutia.getEx(), firstMinutia.getEy(), firstMinutia.getX(), firstMinutia.getY(),
				ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		/* If trace was not possible, return IGNORE. */
		if (ret.get() == ILfs.IGNORE) {
			return (ret.get());
		}

		/* If the trace encountered the second minutia point ... */
		if (ret.get() == ILfs.LOOP_FOUND) {
			getFree().free(contour);
			return (ILfs.HOOK_FOUND);
		}

		/* If trace had an error in following the contour ... */
		if (ret.get() != ILfs.FALSE) {
			return (ret.get());
		}

		/* Otherwise, the trace successfully followed the contour, but did */
		/* not encounter the 2nd minutia point within the specified number */
		/* of steps. */

		/* Deallocate previously extracted contour. */
		getFree().free(contour);

		/* Try searching contour from 1st minutia "edge" searching for */
		/* edge neighbors counter-clockwise. */
		contour = getContour().traceContour(ret, oNoOfContour, maxHookLen, secondMinutia.getX(), secondMinutia.getY(),
				firstMinutia.getEx(), firstMinutia.getEy(), firstMinutia.getX(), firstMinutia.getY(),
				ILfs.SCAN_COUNTER_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		/* If trace was not possible, return IGNORE. */
		if (ret.get() == ILfs.IGNORE) {
			return (ret.get());
		}

		/* If the trace encountered the second minutia point ... */
		if (ret.get() == ILfs.LOOP_FOUND) {
			getFree().free(contour);
			return (ILfs.HOOK_FOUND);
		}

		/* If the trace successfully followed the 1st minutia's contour, but */
		/* did not encounter the 2nd minutia point within the specified number */
		/* of steps ... */
		if (ret.get() == ILfs.FALSE) {
			getFree().free(contour);
			/* Then return hook NOT found (FALSE). */
			return (ILfs.FALSE);
		}

		/* Otherwise, the 2nd trace had an error in following the contour ... */
		return (ret.get());
	}

	/**
	 * Determines whether a feature's contour points are ordered clockwise or counter-clockwise
	 * about the feature.
	 *
	 * Port of NIST {@code is_loop_clockwise()}. A chain code is derived from the contour points and
	 * its turning direction is analyzed with {@link ChainCode#isChainClockwise}. Because the order
	 * cannot always be determined (for example when there are too few contour points), the caller
	 * supplies an application-specific default result.
	 *
	 * @param oContourX   x-coordinates (pixels) of the feature's contour points
	 * @param oContourY   y-coordinates (pixels) of the feature's contour points
	 * @param noOfContour number of points in the contour
	 * @param defaultRet  value to return when the contour order cannot be determined
	 * @return {@link ILfs#TRUE} ({@code 1}) if the contour is ordered clockwise;
	 *         {@link ILfs#FALSE} ({@code 0}) if counter-clockwise; {@code defaultRet} if the order
	 *         could not be determined; a negative system error code otherwise
	 */
	public int isLoopClockwise(AtomicIntegerArray oContourX, AtomicIntegerArray oContourY, final int noOfContour,
			final int defaultRet) {
		int ret;
		AtomicInteger nchain = new AtomicInteger(0);
		AtomicIntegerArray chain = new AtomicIntegerArray(noOfContour);

		/* Derive chain code from contour points. */
		ret = getChainCode().chainCodeLoop(chain, nchain, oContourX, oContourY, noOfContour);
		if (ret != ILfs.FALSE) {
			/* If there is a system error, return the error code. */
			return (ret);
		}

		/* If chain is empty... */
		if (nchain.get() == ILfs.FALSE) {
			/* There wasn't enough contour points to tell, so return the */
			/* the default return value. No chain needs to be deallocated */
			/* in this case. */
			return (defaultRet);
		}

		/* If the chain code for contour is clockwise ... pass default return */
		/* value on to this routine to correctly handle the case where we can't */
		/* tell the direction of the chain code. */
		ret = getChainCode().isChainClockwise(chain, nchain.get(), defaultRet);

		/* Free the chain code and return result. */
		getFree().free(chain);
		return (ret);
	}

	/**
	 * Processes a contour that has been determined to form a complete loop, either turning it into
	 * two minutiae or erasing it from the binary image.
	 *
	 * Port of NIST {@code process_loop()} (version 1). If the loop is longer than
	 * {@link LfsParams#getMinLoopLen()} and is sufficiently narrow (minimum squared cross distance
	 * below {@link LfsParams#getMinLoopAspectDist()}) or elongated (ratio of maximum to minimum
	 * squared cross distance at least {@link LfsParams#getMinLoopAspectRatio()}), and the midpoint
	 * of its longest axis has the same pixel value as the feature, then the two contour points at
	 * the ends of the longest axis (see {@link #getLoopAspect}) are each added as candidate
	 * minutiae. The first point's direction points towards its opposite point; the second's is
	 * flipped by 180 degrees. Both minutiae get {@link ILfs#DEFAULT_RELIABILITY} and type derived
	 * from the feature pixel (bifurcation or ridge ending) and are tagged {@link ILfs#LOOP_ID}.
	 * Otherwise the loop is assumed not to contain minutiae and is filled with {@link #fillLoop}.
	 *
	 * @param oMinutiae          reference to the list of detected minutiae; up to two new minutiae
	 *                           derived from the loop may be added to it
	 * @param oContourX          x-coordinates (pixels) of the loop's contour points
	 * @param oContourY          y-coordinates (pixels) of the loop's contour points
	 * @param oContourEx         x-coordinates (pixels) of each contour point's edge pixel
	 * @param oContourEy         y-coordinates (pixels) of each contour point's edge pixel
	 * @param noOfContour        number of points in the contour
	 * @param binarizedImageData row-major binary image data ({@code 0} = white, {@code 1} = black);
	 *                           modified in place if the loop is filled
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param lfsParams          LFS parameters and thresholds controlling loop processing
	 * @return {@link ILfs#FALSE} ({@code 0}) if the loop was processed successfully (minutiae added
	 *         or loop filled, or the contour was empty); a negative system error code otherwise
	 */
	public int processLoop(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray oContourX,
			AtomicIntegerArray oContourY, AtomicIntegerArray oContourEx, AtomicIntegerArray oContourEy,
			final int noOfContour, int[] binarizedImageData, final int imageWidth, final int imageHeight,
			final LfsParams lfsParams) {
		int iDir;
		int type;
		int appearing;
		int halfway;
		AtomicReference<Double> minDistance = new AtomicReference<>(0.0);
		AtomicReference<Double> maxDistance = new AtomicReference<>(0.0);
		AtomicInteger minFrom = new AtomicInteger(0);
		AtomicInteger maxFrom = new AtomicInteger(0);
		AtomicInteger minTo = new AtomicInteger(0);
		AtomicInteger maxTo = new AtomicInteger(0);
		int midPointX;
		int midPointY;
		int midPixel;
		int featurePixel;
		int ret;
		Minutia minutia;

		/* If contour is empty, then just return. */
		if (noOfContour <= ILfs.FALSE) {
			return (ILfs.FALSE);
		}

		/* If loop is large enough ... */
		if (noOfContour > lfsParams.getMinLoopLen()) {
			/* Get pixel value of feature's interior. */
			featurePixel = binarizedImageData[0 + (oContourY.get(0) * imageWidth) + oContourX.get(0)];

			/* Compute half the perimeter of the loop. */
			halfway = noOfContour >> 1;

			/* Get the aspect dimensions of the loop in units of */
			/* squared distance. */
			getLoopAspect(minFrom, minTo, minDistance, maxFrom, maxTo, maxDistance, oContourX, oContourY, noOfContour);

			/* If loop passes aspect ratio tests ... loop is sufficiently */
			/* narrow or elongated ... */
			if ((minDistance.get() < lfsParams.getMinLoopAspectDist())
					|| ((maxDistance.get() / minDistance.get()) >= lfsParams.getMinLoopAspectRatio())) {
				/* Update oMinutiae list with opposite points of max distance */
				/* on the loop. */
				/* First, check if interior point has proper pixel value. */
				midPointX = (oContourX.get(maxFrom.get()) + oContourX.get(maxTo.get())) >> 1;
				midPointY = (oContourY.get(maxFrom.get()) + oContourY.get(maxTo.get())) >> 1;
				midPixel = binarizedImageData[0 + (midPointY * imageWidth) + midPointX];
				/* If interior point is the same as the feature... */
				if (midPixel == featurePixel) {
					/* 1. Treat maximum distance point as a potential minutia. */
					/* Compute direction from maximum loop point to its */
					/* opposite point. */
					iDir = getLfsUtil().lineToDirection(oContourX.get(maxFrom.get()), oContourY.get(maxFrom.get()),
							oContourX.get(maxTo.get()), oContourY.get(maxTo.get()), lfsParams.getNumDirections());
					/* Get type of minutia: BIFURCATION or RIDGE_ENDING. */
					type = getMinutiaHelper().getMinutiaType(featurePixel);
					/* Determine if minutia is appearing or disappearing. */
					if ((appearing = getMinutiaHelper().isMinutiaAppearing(oContourX.get(maxFrom.get()),
							oContourY.get(maxFrom.get()), oContourEx.get(maxFrom.get()),
							oContourEy.get(maxFrom.get()))) < 0) {
						/* Return system error code. */
						return (appearing);
					}
					/* Create new minutia object. */
					minutia = getMinutiaHelper().createMinutia(oContourX.get(maxFrom.get()),
							oContourY.get(maxFrom.get()), oContourEx.get(maxFrom.get()), oContourEy.get(maxFrom.get()),
							iDir, ILfs.DEFAULT_RELIABILITY, type, appearing, ILfs.LOOP_ID);

					/* Update the oMinutiae list with potential new minutia. */
					ret = getMinutiaHelper().updateMinutiae(oMinutiae, minutia, binarizedImageData, imageWidth,
							imageHeight, lfsParams);
					/* If minuitia IGNORED and not added to the minutia list ... */
					if (ret == ILfs.IGNORE) {
						/* Deallocate the minutia. */
						getMinutiaHelper().freeMinutia(minutia);
					}

					/* 2. Treat point opposite of maximum distance point as */
					/* a potential minutia. */

					/* Flip the direction 180 degrees. Make sure new direction */
					/* is on the range [0..(ndirsX2)]. */
					iDir += lfsParams.getNumDirections();
					iDir %= (lfsParams.getNumDirections() << 1);

					/* The type of minutia will stay the same. */

					/* Determine if minutia is appearing or disappearing. */
					if ((appearing = getMinutiaHelper().isMinutiaAppearing(oContourX.get(maxTo.get()),
							oContourY.get(maxTo.get()), oContourEx.get(maxTo.get()),
							oContourEy.get(maxTo.get()))) < 0) {
						/* Return system error code. */
						return (appearing);
					}
					/* Create new minutia object. */
					minutia = getMinutiaHelper().createMinutia(oContourX.get(maxTo.get()), oContourY.get(maxTo.get()),
							oContourEx.get(maxTo.get()), oContourEy.get(maxTo.get()), iDir, ILfs.DEFAULT_RELIABILITY,
							type, appearing, ILfs.LOOP_ID);
					/* Update the oMinutiae list with potential new minutia. */
					ret = getMinutiaHelper().updateMinutiae(oMinutiae, minutia, binarizedImageData, imageWidth,
							imageHeight, lfsParams);
					/* If minuitia IGNORED and not added to the minutia list ... */
					if (ret == ILfs.IGNORE) {
						/* Deallocate the minutia. */
						getMinutiaHelper().freeMinutia(minutia);
					}

					/* Done successfully processing this loop, so return normally. */
					return (ILfs.FALSE);
				} // Otherwise, loop interior has problems.
			} // Otherwise, loop is not the right shape for oMinutiae.
		} // Otherwise, loop's perimeter is too small for oMinutiae.

		/* If we get here, we have a loop that is assumed to not contain */
		/* oMinutiae, so remove the loop from the image. */
		ret = fillLoop(oContourX, oContourY, noOfContour, binarizedImageData, imageWidth, imageHeight);

		/* Return either an error code from fill_loop or return normally. */
		return (ret);
	}

	/**
	 * Processes a complete loop contour (version 2), assigning minutia reliability from the Low
	 * Ridge Flow Map.
	 *
	 * Port of NIST {@code process_loop_V2()}. Identical to {@link #processLoop} except that each
	 * minutia created from the loop is given {@link ILfs#MEDIUM_RELIABILITY} when its pixel falls in
	 * a LOW RIDGE FLOW block of {@code oLowFlowMap}, and {@link ILfs#HIGH_RELIABILITY} otherwise.
	 * Minutiae are still added with the version-1 {@code updateMinutiae} routine, as in the NIST
	 * source. Loops that do not qualify for minutiae are filled with {@link #fillLoop}.
	 *
	 * @param oMinutiae          reference to the list of detected minutiae; up to two new minutiae
	 *                           derived from the loop may be added to it
	 * @param oContourX          x-coordinates (pixels) of the loop's contour points
	 * @param oContourY          y-coordinates (pixels) of the loop's contour points
	 * @param oContourEx         x-coordinates (pixels) of each contour point's edge pixel
	 * @param oContourEy         y-coordinates (pixels) of each contour point's edge pixel
	 * @param noOfContour        number of points in the contour
	 * @param binarizedImageData row-major binary image data ({@code 0} = white, {@code 1} = black);
	 *                           modified in place if the loop is filled
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @param oLowFlowMap        pixelized Low Ridge Flow Map (one entry per image pixel,
	 *                           {@link ILfs#TRUE} where the pixel lies in a low-flow block)
	 * @param lfsParams          LFS parameters and thresholds controlling loop processing
	 * @return {@link ILfs#FALSE} ({@code 0}) if the loop was processed successfully; a negative
	 *         system error code otherwise
	 */
	public int processLoopV2(AtomicReference<Minutiae> oMinutiae, AtomicIntegerArray oContourX,
			AtomicIntegerArray oContourY, AtomicIntegerArray oContourEx, AtomicIntegerArray oContourEy,
			final int noOfContour, int[] binarizedImageData, final int imageWidth, final int imageHeight,
			AtomicIntegerArray oLowFlowMap, final LfsParams lfsParams) {
		int halfway;
		int idir;
		int type;
		int appearing;
		AtomicReference<Double> oMinDistance = new AtomicReference<>(0.0);
		AtomicReference<Double> oMaxDistance = new AtomicReference<>(0.0);
		AtomicInteger oMinFrom = new AtomicInteger(0);
		AtomicInteger oMaxFrom = new AtomicInteger(0);
		AtomicInteger oMinTo = new AtomicInteger(0);
		AtomicInteger oMaxTo = new AtomicInteger(0);
		int midX;
		int midY;
		int midPixel;
		int featurePixel;
		int ret;
		Minutia minutia;
		int fmapval;
		double reliability;

		/* If contour is empty, then just return. */
		if (noOfContour <= ILfs.FALSE) {
			return (ILfs.FALSE);
		}

		/* If loop is large enough ... */
		if (noOfContour > lfsParams.getMinLoopLen()) {
			/* Get pixel value of feature's interior. */
			featurePixel = binarizedImageData[0 + (oContourY.get(0) * imageWidth) + oContourX.get(0)];

			/* Compute half the perimeter of the loop. */
			halfway = noOfContour >> 1;

			/* Get the aspect dimensions of the loop in units of */
			/* squared distance. */
			getLoopAspect(oMinFrom, oMinTo, oMinDistance, oMaxFrom, oMaxTo, oMaxDistance, oContourX, oContourY,
					noOfContour);

			/* If loop passes aspect ratio tests ... loop is sufficiently */
			/* narrow or elongated ... */
			if ((oMinDistance.get() < lfsParams.getMinLoopAspectDist())
					|| ((oMaxDistance.get() / oMinDistance.get()) >= lfsParams.getMinLoopAspectRatio())) {
				/* Update oMinutiae list with opposite points of max distance */
				/* on the loop. */

				/* First, check if interior point has proper pixel value. */
				midX = (oContourX.get(oMaxFrom.get()) + oContourX.get(oMaxTo.get())) >> 1;
				midY = (oContourY.get(oMaxFrom.get()) + oContourY.get(oMaxTo.get())) >> 1;
				midPixel = binarizedImageData[0 + (midY * imageWidth) + midX];
				/* If interior point is the same as the feature... */
				if (midPixel == featurePixel) {
					/* 1. Treat maximum distance point as a potential minutia. */
					/* Compute direction from maximum loop point to its */
					/* opposite point. */
					idir = getLfsUtil().lineToDirection(oContourX.get(oMaxFrom.get()), oContourY.get(oMaxFrom.get()),
							oContourX.get(oMaxTo.get()), oContourY.get(oMaxTo.get()), lfsParams.getNumDirections());
					/* Get type of minutia: BIFURCATION or RIDGE_ENDING. */
					type = getMinutiaHelper().getMinutiaType(featurePixel);
					/* Determine if minutia is appearing or disappearing. */
					if ((appearing = getMinutiaHelper().isMinutiaAppearing(oContourX.get(oMaxFrom.get()),
							oContourY.get(oMaxFrom.get()), oContourEx.get(oMaxFrom.get()),
							oContourEy.get(oMaxFrom.get()))) < ILfs.FALSE) {
						/* Return system error code. */
						return (appearing);
					}

					/* Is the new point in a LOW RIDGE FLOW block? */
					fmapval = oLowFlowMap
							.get(0 + (oContourY.get(oMaxFrom.get()) * imageWidth) + oContourX.get(oMaxFrom.get()));

					/* If current minutia is in a LOW RIDGE FLOW block ... */
					if (fmapval >= ILfs.TRUE) {
						reliability = ILfs.MEDIUM_RELIABILITY;
					} else {
						/* Otherwise, minutia is in a reliable block. */
						reliability = ILfs.HIGH_RELIABILITY;
					}

					/* Create new minutia object. */
					minutia = getMinutiaHelper().createMinutia(oContourX.get(oMaxFrom.get()),
							oContourY.get(oMaxFrom.get()), oContourEx.get(oMaxFrom.get()),
							oContourEy.get(oMaxFrom.get()), idir, reliability, type, appearing, ILfs.LOOP_ID);

					/* Update the oMinutiae list with potential new minutia. */
					/* NOTE: Deliberately using version one of this routine. */
					ret = getMinutiaHelper().updateMinutiae(oMinutiae, minutia, binarizedImageData, imageWidth,
							imageHeight, lfsParams);

					/* If minuitia IGNORED and not added to the minutia list ... */
					if (ret == ILfs.IGNORE) {
						/* Deallocate the minutia. */
						getMinutiaHelper().freeMinutia(minutia);
					}

					/* 2. Treat point opposite of maximum distance point as */
					/* a potential minutia. */

					/* Flip the direction 180 degrees. Make sure new direction */
					/* is on the range [0..(ndirsX2)]. */
					idir += lfsParams.getNumDirections();
					idir %= (lfsParams.getNumDirections() << 1);

					/* The type of minutia will stay the same. */

					/* Determine if minutia is appearing or disappearing. */
					if ((appearing = getMinutiaHelper().isMinutiaAppearing(oContourX.get(oMaxTo.get()),
							oContourY.get(oMaxTo.get()), oContourEx.get(oMaxTo.get()),
							oContourEy.get(oMaxTo.get()))) < ILfs.FALSE) {
						/* Return system error code. */
						return (appearing);
					}

					/* Is the new point in a LOW RIDGE FLOW block? */
					fmapval = oLowFlowMap
							.get(0 + (oContourY.get(oMaxTo.get()) * imageWidth) + oContourX.get(oMaxTo.get()));

					/* If current minutia is in a LOW RIDGE FLOW block ... */
					if (fmapval >= ILfs.TRUE) {
						reliability = ILfs.MEDIUM_RELIABILITY;
					} else {
						/* Otherwise, minutia is in a reliable block. */
						reliability = ILfs.HIGH_RELIABILITY;
					}

					/* Create new minutia object. */
					minutia = getMinutiaHelper().createMinutia(oContourX.get(oMaxTo.get()), oContourY.get(oMaxTo.get()),
							oContourEx.get(oMaxTo.get()), oContourEy.get(oMaxTo.get()), idir, reliability, type,
							appearing, ILfs.LOOP_ID);

					/* Update the oMinutiae list with potential new minutia. */
					/* NOTE: Deliberately using version one of this routine. */
					ret = getMinutiaHelper().updateMinutiae(oMinutiae, minutia, binarizedImageData, imageWidth,
							imageHeight, lfsParams);

					/* If minuitia IGNORED and not added to the minutia list ... */
					if (ret == ILfs.IGNORE) {
						/* Deallocate the minutia. */
						getMinutiaHelper().freeMinutia(minutia);
					}

					/* Done successfully processing this loop, so return normally. */
					return (ILfs.FALSE);
				} // Otherwise, loop interior has problems.
			} // Otherwise, loop is not the right shape for oMinutiae.
		} // Otherwise, loop's perimeter is too small for oMinutiae.

		/* If we get here, we have a loop that is assumed to not contain */
		/* oMinutiae, so remove the loop from the image. */
		ret = fillLoop(oContourX, oContourY, noOfContour, binarizedImageData, imageWidth, imageHeight);

		/* Return either an error code from fill_loop or return normally. */
		return (ret);
	}

	/**
	 * Measures a loop's aspect: the smallest and largest distances across the loop, and the contour
	 * points at which they occur.
	 *
	 * Port of NIST {@code get_loop_aspect()}. Pairs of opposite contour points (index {@code i} and
	 * {@code (i + noOfContour / 2) mod noOfContour}) are compared by squared Euclidean distance.
	 * For even-length loops only half the perimeter is walked, since the second half is exactly
	 * redundant; for odd-length loops the entire perimeter is walked. Distances are reported in
	 * units of squared pixels.
	 *
	 * @param oMinFrom    output contour index where the minimum aspect occurs
	 * @param oMinTo      output opposite contour index where the minimum aspect occurs
	 * @param oMinDist    output minimum (squared) distance across the loop
	 * @param oMaxFrom    output contour index where the maximum aspect occurs
	 * @param oMaxTo      output opposite contour index where the maximum aspect occurs
	 * @param oMaxDist    output maximum (squared) distance across the loop
	 * @param oContourX   x-coordinates (pixels) of the loop's contour points
	 * @param oContourY   y-coordinates (pixels) of the loop's contour points
	 * @param noOfContour number of points in the contour
	 */
	public void getLoopAspect(AtomicInteger oMinFrom, AtomicInteger oMinTo, AtomicReference<Double> oMinDist,
			AtomicInteger oMaxFrom, AtomicInteger oMaxTo, AtomicReference<Double> oMaxDist,
			AtomicIntegerArray oContourX, AtomicIntegerArray oContourY, final int noOfContour) {
		int halfway;
		int limit;
		int i, j;
		double dist;
		double minDistance;
		double maxDistance;
		int minI;
		int maxI;
		int minJ;
		int maxJ;

		/* Compute half the perimeter of the loop. */
		halfway = noOfContour >> 1;

		/* Take opposite points on the contour and walk half way */
		/* around the loop. */
		i = 0;
		j = halfway;
		/* Compute squared distance between opposite points on loop. */
		dist = getLfsUtil().squaredDistance(oContourX.get(i), oContourY.get(i), oContourX.get(j), oContourY.get(j));

		/* Initialize running minimum and maximum distances along loop. */
		minDistance = dist;
		minI = i;
		minJ = j;
		maxDistance = dist;
		maxI = i;
		maxJ = j;
		/* Bump to next pair of opposite points. */
		i++;
		/* Make sure j wraps around end of list. */
		j++;
		j %= noOfContour;

		/* If the loop is of even length, then we only need to walk half */
		/* way around as the other half will be exactly redundant. If */
		/* the loop is of odd length, then the second half will not be */
		/* be exactly redundant and the difference "may" be meaningful. */
		/* If execution speed is an issue, then probably get away with */
		/* walking only the fist half of the loop under ALL conditions. */

		/* If loop has odd length ... */
		if ((noOfContour % 2) == ILfs.TRUE)// odd
		{
			/* Walk the loop's entire perimeter. */
			limit = noOfContour;
		}
		/* Otherwise the loop has even length ... */
		else {
			/* Only walk half the perimeter. */
			limit = halfway;
		}

		/* While we have not reached our perimeter limit ... */
		while (i < limit) {
			/* Compute squared distance between opposite points on loop. */
			dist = getLfsUtil().squaredDistance(oContourX.get(i), oContourY.get(i), oContourX.get(j), oContourY.get(j));
			/* Check the running minimum and maximum distances. */
			if (dist < minDistance) {
				minDistance = dist;
				minI = i;
				minJ = j;
			}
			if (dist > maxDistance) {
				maxDistance = dist;
				maxI = i;
				maxJ = j;
			}
			/* Bump to next pair of opposite points. */
			i++;
			/* Make sure j wraps around end of list. */
			j++;
			j %= noOfContour;
		}

		/* Assign minimum and maximum distances to output pointers. */
		oMinFrom.set(minI);
		oMinTo.set(minJ);
		oMinDist.set(minDistance);
		oMaxFrom.set(maxI);
		oMaxTo.set(maxJ);
		oMaxDist.set(maxDistance);
	}

	/**
	 * Fills (erases) a complete loop in the binary image, correctly handling complex and concave
	 * shapes.
	 *
	 * Port of NIST {@code fill_loop()}. The contour is converted to a row-wise shape via
	 * {@link Shapes#shapeFromContour}; each row is then filled from the left-most to the right-most
	 * contour point with the edge pixel value (the inverse of the feature's interior pixel value),
	 * skipping over concavities where the next pixel already has the edge value.
	 *
	 * NIST note: a flood fill was tried in place of this routine, but the contour (although
	 * 8-connected) is NOT guaranteed to be completely surrounded (in an 8-connected sense) by
	 * pixels of opposite color, so the flood would occasionally escape the loop and corrupt the
	 * binary image.
	 *
	 * If a shape row unexpectedly has no contour points, a warning is logged and the fill is
	 * abandoned without error.
	 *
	 * @param oContourX          x-coordinates (pixels) of the loop's contour points
	 * @param oContourY          y-coordinates (pixels) of the loop's contour points
	 * @param noOfContour        number of points in the contour
	 * @param binarizedImageData row-major binary image data ({@code 0} = white, {@code 1} = black);
	 *                           modified in place with the loop filled
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 * @return {@link ILfs#FALSE} ({@code 0}) if the loop was filled successfully (or the fill was
	 *         abandoned due to an unexpected shape); a negative system error code from shape
	 *         creation otherwise
	 */
	public int fillLoop(AtomicIntegerArray oContourX, AtomicIntegerArray oContourY, final int noOfContour,
			int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		Shape shape;
		int ret;
		int i;
		int j;
		int x;
		int nx;
		int y;
		int lastj;
		int nextPixel;
		int featurePixel;
		int edgePixel;

		/* Create a shape structure from loop's contour. */
		AtomicInteger returnCode = new AtomicInteger(0);
		shape = getShapes().shapeFromContour(returnCode, oContourX, oContourY, noOfContour);
		ret = returnCode.get();
		if (ret != ILfs.FALSE) {
			/* If system error, then return error code. */
			return (ret);
		}

		/* Get feature pixel value (the value on the interior of the loop */
		/* to be filled). */
		// feature_pix = *(binarizedImageData.argValue + (oContourY[0] * imageWidth) +
		// oContourX[0]);
		featurePixel = binarizedImageData[(oContourY.get(0) * imageWidth) + oContourX.get(0)];
		/* Now get edge pixel value (the value on the exterior of the loop */
		/* to be used to filled the loop). We can get this value by flipping */
		/* the feature pixel value. */
		if (featurePixel == ILfs.TRUE) {
			edgePixel = 0;
		} else {
			edgePixel = 1;
		}

		/* Foreach row in shape... */
		for (i = 0; i < shape.getNRows(); i++) {
			/* Get y-coord of current row in shape. */
			y = shape.getRows().get(i).getY();

			/* There should always be at least 1 contour points in the row. */
			/* If there isn't, then something is wrong, so post a warning and */
			/* just return. This is mostly for debug purposes. */
			if (shape.getRows().get(i).getNoOfPts() < ILfs.TRUE) {
				/* Deallocate the shape. */
				getShapes().freeShape(shape);
				logger.warn(String.format("WARNING : fill_loop : unexpected shape, preempting loop fill\n"));
				/* This is unexpected, but not fatal, so return normally. */
				return (ILfs.FALSE);
			}

			/* Reset x index on row to the left-most contour point in the row. */
			j = 0;
			/* Get first x-coord corresponding to the first contour point on row. */
			x = shape.getRows().get(i).getXs().get(j);
			/* Fill the first contour point on the row. */
			binarizedImageData[(y * imageWidth) + x] = edgePixel;
			/* Set the index of last contour point on row. */
			lastj = shape.getRows().get(i).getNoOfPts() - 1;

			/* While last contour point on row has not been processed... */
			while (j < lastj) {
				/* On each interation, we have filled up to the current */
				/* contour point on the row pointed to by "j", and now we */
				/* need to determine if we need to skip some edge pixels */
				/* caused by a concavity in the shape or not. */

				/* Get the next pixel value on the row just right of the */
				/* last contour point filled. We know there are more points */
				/* on the row because we haven't processed the last contour */
				/* point on the row yet. */
				x++;
				// nextPixel = *(binarizedImageData.argValue + (y * imageWidth) + x);
				nextPixel = binarizedImageData[(y * imageWidth) + x];

				/* If the next pixel is the same value as loop's edge pixels ... */
				if (nextPixel == edgePixel) {
					/* Then assume we have found a concavity and skip to next */
					/* contour point on row. */
					j++;
					/* Fill the new contour point because we know it is on the */
					/* feature's contour. */
					x = shape.getRows().get(i).getXs().get(j);
					binarizedImageData[(y * imageWidth) + x] = edgePixel;

					/* Now we are ready to loop again. */
				}
				/* Otherwise, fill from current pixel up through the next contour */
				/* point to the right on the row. */
				else {
					/* Bump to the next contour point to the right on row. */
					j++;
					/* Set the destination x-coord to the next contour point */
					/* to the right on row. Realize that this could be the */
					/* same pixel as the current x-coord if contour points are */
					/* adjacent. */
					nx = shape.getRows().get(i).getXs().get(j);

					/* Fill between current x-coord and next contour point to the */
					/* right on the row (including the new contour point). */
					fillPartialRow(edgePixel, x, nx, y, binarizedImageData, imageWidth, imageHeight);
				}

				/* Once we are here we have filled the row up to (and including) */
				/* the contour point currently pointed to by "j". */
				/* We are now ready to loop again. */
			} // End WHILE
		} // End FOR

		getShapes().freeShape(shape);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Fills a contiguous range of pixels on one row of an 8-bit pixel image with a given value.
	 *
	 * Port of NIST {@code fill_partial_row()}. Pixels from {@code fromX} to {@code toX} (both
	 * inclusive) on row {@code yIndex} are set to {@code fillPixel}. The pixel coordinates are
	 * assumed to lie within the image boundaries; no bounds checking is performed.
	 *
	 * @param fillPixel          pixel value to fill with (expected on the range [0..255])
	 * @param fromX              x-pixel coordinate where the fill begins
	 * @param toX                x-pixel coordinate where the fill ends (inclusive)
	 * @param yIndex             y-pixel coordinate of the row being filled
	 * @param binarizedImageData 8-bit image data, row-major; modified in place
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels (unused, kept for parity with NIST)
	 */
	public void fillPartialRow(final int fillPixel, final int fromX, final int toX, final int yIndex,
			int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int binarizedImageDataIndex;

		/* Set pixel pointer to starting x-coord on current row. */
		binarizedImageDataIndex = 0 + (yIndex * imageWidth) + fromX;

		/* Foreach pixel between starting and ending x-coord on row */
		/* (including the end points) ... */
		for (int x = fromX; x <= toX; x++) {
			/* Set current pixel with fill pixel value. */
			binarizedImageData[binarizedImageDataIndex] = fillPixel;
			/* Bump to next pixel in the row. */
			binarizedImageDataIndex++;
		}
	}

	/**
	 * Fills a complete loop contour with a pixel value using a recursive 4-neighbor flood fill.
	 *
	 * Port of NIST {@code flood_loop()}. The fill value is computed as the bitwise complement of the
	 * feature pixel value found at the first contour point, masked to 8 bits. A flood fill is seeded
	 * from every contour point so that interiors "pinched" off by skipped exposed corners are also
	 * filled; simple shapes are filled from the first seed and later seeds return immediately.
	 *
	 * NIST note: this approach will NOT always work with the contours generated in this application
	 * because they are not guaranteed to be ENTIRELY surrounded by 8-connected pixels not equal to
	 * the fill value. This is unfortunate, since flood fill is a simple algorithm that handles
	 * complex and concave shapes. {@link #fillLoop} is used instead in the main pipeline.
	 *
	 * @param oContourX          x-coordinates (pixels) of the loop's contour points
	 * @param oContourY          y-coordinates (pixels) of the loop's contour points
	 * @param noOfContour        number of points in the contour
	 * @param binarizedImageData row-major binary image data ({@code 0} = white, {@code 1} = black);
	 *                           modified in place with the loop filled
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 */
	public void floodLoop(final AtomicIntegerArray oContourX, final AtomicIntegerArray oContourY, final int noOfContour,
			int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int featurePixel;
		int fillPixel;

		/* Get the pixel value of the minutia feauture. This is */
		/* the pixel value we wish to replace with the flood. */
		featurePixel = binarizedImageData[0 + (oContourY.get(0) * imageWidth) + oContourX.get(0)];

		/* Flip the feature pixel value to the value we want to */
		/* fill with and send this value to the flood routine. */
		fillPixel = (featurePixel != 0) ? 0 : 1;

		/* Flood-fill interior of contour using a 4-neighbor fill. */
		/* We are using a 4-neighbor fill because the contour was */
		/* collected using 8-neighbors, and the 4-neighbor fill */
		/* will NOT escape the 8-neighbor based contour. */
		/* The contour passed must be guarenteed to be complete for */
		/* the flood-fill to work properly. */
		/* We are initiating a flood-fill from each point on the */
		/* contour to make sure complex patterns get filled in. */
		/* The complex patterns we are concerned about are those */
		/* that "pinch" the interior of the feature off due to */
		/* skipping "exposed" corners along the contour. */
		/* Simple shapes will fill upon invoking the first contour */
		/* pixel, and the subsequent calls will immediately return */
		/* as their seed pixel will have already been flipped. */
		for (int i = 0; i < noOfContour; i++) {
			/* Start the recursive flooding. */
			floodFill4(fillPixel, oContourX.get(i), oContourY.get(i), binarizedImageData, imageWidth, imageHeight);
		}
	}

	/**
	 * Recursively floods a region of an 8-bit pixel image with a pixel value, starting from a seed
	 * point and using 4-connected neighbors.
	 *
	 * Port of NIST {@code flood_fill4()}. If the seed pixel does not already hold
	 * {@code fillPixel}, it is set and the routine recurses into its north, east, south and west
	 * neighbors that lie within the image. Recursion depth grows with the size of the region, so
	 * very large regions may exhaust the Java call stack.
	 *
	 * @param fillPixel          8-bit pixel value to fill with (on the range [0..255])
	 * @param xIndex             starting (seed) x-pixel coordinate
	 * @param yIndex             starting (seed) y-pixel coordinate
	 * @param binarizedImageData 8-bit pixel image data, row-major; modified in place
	 * @param imageWidth         width of the image, in pixels
	 * @param imageHeight        height of the image, in pixels
	 */
	public void floodFill4(final int fillPixel, final int xIndex, final int yIndex, int[] binarizedImageData,
			final int imageWidth, final int imageHeight) {
		int binarizedImageDataIndex;
		int yNorthPixel;
		int ySouthPixel;
		int xEastPixel;
		int xWestPixel;

		/* Get address of current pixel. */
		binarizedImageDataIndex = (0 + (yIndex * imageWidth) + xIndex);
		/* If pixel needs to be filled ... */
		if (binarizedImageData[binarizedImageDataIndex] != fillPixel) {
			/* Fill the current pixel. */
			binarizedImageData[binarizedImageDataIndex] = fillPixel;

			/* Recursively invoke flood on the pixel's 4 neighbors. */
			/* Test to make sure neighbors are within image boudaries */
			/* before invoking each flood. */
			yNorthPixel = yIndex - 1;
			ySouthPixel = yIndex + 1;
			xWestPixel = xIndex - 1;
			xEastPixel = xIndex + 1;

			/* Invoke North */
			if (yNorthPixel >= 0) {
				floodFill4(fillPixel, xIndex, yNorthPixel, binarizedImageData, imageWidth, imageHeight);
			}

			/* Invoke East */
			if (xEastPixel < imageWidth) {
				floodFill4(fillPixel, xEastPixel, yIndex, binarizedImageData, imageWidth, imageHeight);
			}

			/* Invoke South */
			if (ySouthPixel < imageHeight) {
				floodFill4(fillPixel, xIndex, ySouthPixel, binarizedImageData, imageWidth, imageHeight);
			}

			/* Invoke West */
			if (xWestPixel >= 0) {
				floodFill4(fillPixel, xWestPixel, yIndex, binarizedImageData, imageWidth, imageHeight);
			}
		}
		/* Otherwise, there is nothing to be done. */
	}
}