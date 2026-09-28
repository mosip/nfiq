package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IContour;

/**
 * Contour tracing and contour geometry routines for MINDTCT, and the contour
 * data structure itself.
 * <p>
 * Port of NIST LFS {@code contour.c}. A contour of a minutia feature (a ridge
 * or valley ending) is represented by two parallel lists of coordinate pairs:
 * the 8-connected chain of "contour points" interior to the feature, and the
 * corresponding "edge points" adjacent to each contour point on the exterior
 * of the feature. This class serves a dual role:
 * <ul>
 * <li>as a stateless service (the singleton returned by
 * {@link #getInstance()}) providing contour extraction, tracing, loop
 * detection and geometric helpers; and</li>
 * <li>as a contour value object (instances returned by
 * {@link #allocateContour(AtomicInteger, int)} and the tracing methods)
 * holding the four coordinate lists and the allocated point count.</li>
 * </ul>
 * The singleton accessors are synchronized. Individual contour instances are
 * mutable and not thread-safe; the singleton itself should not be used as a
 * contour value (its lists are {@code null} unless created via
 * {@link #getInstance(int)}).
 */
public class Contour extends MindTct implements IContour {
	/**
	 * Lazily created singleton instance, see {@link #getInstance()} and
	 * {@link #getInstance(int)}.
	 */
	private static Contour instance;

	/**
	 * Private constructor for the stateless singleton (contour lists are left
	 * {@code null}); use {@link #getInstance()}.
	 */
	private Contour() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code Contour} instance
	 */
	public static synchronized Contour getInstance() {
		if (instance == null) {
			instance = new Contour();
		}
		return instance;
	}

	/**
	 * Returns the shared singleton instance, creating it on first use with
	 * contour lists pre-allocated to the given length.
	 * <p>
	 * If the singleton already exists, the argument is ignored and the existing
	 * instance is returned unchanged.
	 *
	 * @param noOfContour number of items in each coordinate list to allocate
	 * @return the singleton {@code Contour} instance
	 */
	public static synchronized Contour getInstance(int noOfContour) {
		if (instance == null) {
			instance = new Contour(noOfContour);
		}
		return instance;
	}

	/** X-pixel coords of the feature's contour points (interior to feature). */
	private AtomicIntegerArray contourX;
	/** Y-pixel coords of the feature's contour points (interior to feature). */
	private AtomicIntegerArray contourY;
	/** X-pixel coords of the feature's edge points (exterior to feature). */
	private AtomicIntegerArray contourEx;
	/** Y-pixel coords of the feature's edge points (exterior to feature). */
	private AtomicIntegerArray contourEy;
	/**
	 * Allocated number of items in each coordinate list (the actual number of
	 * points traced is reported separately by the tracing methods).
	 */
	private int noOfContour;

	/**
	 * Creates a contour value object with all four coordinate lists allocated
	 * (zero-filled) to the given length.
	 *
	 * @param noOfContour number of items in each coordinate list
	 */
	private Contour(int noOfContour) {
		super();
		this.contourX = new AtomicIntegerArray(noOfContour);
		this.contourY = new AtomicIntegerArray(noOfContour);
		this.contourEx = new AtomicIntegerArray(noOfContour);
		this.contourEy = new AtomicIntegerArray(noOfContour);
		this.noOfContour = noOfContour;
	}

	/**
	 * Returns the shared {@link Defs} helper (rounding, modulo and precision
	 * truncation).
	 *
	 * @return the {@code Defs} singleton
	 */
	public Defs getDefs() {
		return Defs.getInstance();
	}

	/**
	 * Returns the shared {@link Free} helper used to release lists.
	 *
	 * @return the {@code Free} singleton
	 */
	public Free getFree() {
		return Free.getInstance();
	}

	/**
	 * Returns the shared {@link Globals} tables (8-neighbour offsets and chain
	 * codes).
	 *
	 * @return the {@code Globals} singleton
	 */
	public Globals getGlobals() {
		return Globals.getInstance();
	}

	/**
	 * Returns the shared {@link LfsUtil} helper.
	 *
	 * @return the {@code LfsUtil} singleton
	 */
	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	/**
	 * Allocates the lists needed to represent the contour of a minutia feature
	 * (a ridge or valley ending).
	 * <p>
	 * NIST: {@code allocate_contour()}. This includes two lists of coordinate
	 * pairs. The first is the 8-connected chain of points interior to the
	 * feature, called the feature's "contour points". The second is a list of
	 * corresponding points, each adjacent to its respective feature contour point
	 * in the first list and on the exterior of the feature; these are called the
	 * feature's "edge points". Don't be confused: both lists of points are on the
	 * "edge". The first set is guaranteed 8-connected and the color of the
	 * feature. The second set is NOT guaranteed to be 8-connected and its points
	 * are opposite the color of the feature. Remember that "feature" means either
	 * ridge ending (black pixels) or valley ending (white pixels).
	 *
	 * @param ret         output: zero ({@link ILfs#FALSE}) if the lists were
	 *                    successfully allocated (negative would indicate a system
	 *                    allocation error)
	 * @param noOfContour number of items in each coordinate list to be allocated
	 * @return a new contour holding the allocated x/y lists for the feature's
	 *         contour points and x/y lists for its edge points
	 */
	public Contour allocateContour(AtomicInteger ret, final int noOfContour) {
		ret.set(ILfs.FALSE);
		return new Contour(noOfContour);
	}

	/**
	 * Deallocates the lists used to represent the contour of a minutia feature
	 * (a ridge or valley ending).
	 * <p>
	 * NIST: {@code free_contour()}. Releases the contour-point x/y lists and
	 * edge-point x/y lists (see {@link #allocateContour(AtomicInteger, int)})
	 * and sets them to {@code null}. Does nothing if {@code contour} is
	 * {@code null}.
	 *
	 * @param contour the contour whose lists are to be released (may be
	 *                {@code null})
	 */
	public void freeContour(Contour contour) {
		if (contour != null) {
			getFree().free(contour.getContourX());
			getFree().free(contour.getContourY());
			getFree().free(contour.getContourEx());
			getFree().free(contour.getContourEy());
			contour.setContourX(null);
			contour.setContourY(null);
			contour.setContourEx(null);
			contour.setContourEy(null);
		}
	}

	/**
	 * Takes the pixel coordinate of a detected minutia feature point and its
	 * corresponding/adjacent edge pixel and attempts to extract a contour of
	 * specified length of the feature's edge, for high curvature analysis.
	 * <p>
	 * NIST: {@code get_high_curvature_contour()}. The contour is extracted by
	 * walking the feature's edge a specified number of steps clockwise and then
	 * counter-clockwise. If a loop is detected while extracting the contour, the
	 * contour of the loop is returned with a return code of
	 * {@link ILfs#LOOP_FOUND}. If the process fails to extract a contour of total
	 * specified length, then the returned contour length is set to zero, NO
	 * contour is returned, and the return code is set to zero. (An alternative
	 * implementation would be to return the incomplete contour with a return
	 * code of INCOMPLETE.)
	 *
	 * @param ret                output: zero ({@link ILfs#FALSE}) if the
	 *                           resulting contour was successfully extracted or
	 *                           is empty; {@link ILfs#LOOP_FOUND} if the
	 *                           resulting contour forms a complete loop;
	 *                           negative on system error
	 * @param oNoOfContour       output: number of contour points returned
	 * @param halfContour        half the length of the extracted contour
	 *                           (full-length non-loop contour =
	 *                           {@code halfContour * 2 + 1})
	 * @param xPixelLoc          starting x-pixel coord of feature (interior to
	 *                           feature)
	 * @param yPixelLoc          starting y-pixel coord of feature (interior to
	 *                           feature)
	 * @param xEdgePixelLoc      x-pixel coord of corresponding edge pixel
	 *                           (exterior to feature)
	 * @param yEdgePixelLoc      y-pixel coord of corresponding edge pixel
	 *                           (exterior to feature)
	 * @param binarizedImageData binary image data (0 = white, 1 = black)
	 * @param imageWidth         width (in pixels) of image
	 * @param imageHeight        height (in pixels) of image
	 * @return the extracted contour (contour-point and edge-point x/y lists), or
	 *         {@code null} if no contour was extracted
	 */
	@SuppressWarnings({ "java:S3776" })
	public Contour getHighCurvatureContour(AtomicInteger ret, AtomicInteger oNoOfContour, final int halfContour,
			final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc, final int yEdgePixelLoc,
			int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		Contour contour = null;
		Contour contourHalf1 = null;
		Contour contourHalf2 = null;
		int maxContour;
		AtomicInteger nHalf1 = new AtomicInteger(0);
		AtomicInteger nHalf2 = new AtomicInteger(0);
		int contourCount;
		int i;
		int j;

		/* Compute maximum length of complete contour */
		/* (2 half contours + feature point). */
		maxContour = (halfContour << 1) + 1;

		/* Initialize output contour length to 0. */
		oNoOfContour.set(0);

		/* Get 1st half contour with clockwise neighbor trace. */
		contourHalf1 = traceContour(ret, nHalf1, halfContour, xPixelLoc, yPixelLoc, xPixelLoc, yPixelLoc, xEdgePixelLoc,
				yEdgePixelLoc, ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		if (ret.get() != ILfs.FALSE) {
			/* If trace was not possible ... */
			if (ret.get() == ILfs.IGNORE) {
				/* Return, with nothing allocated and contour length equal to 0. */
				ret.set(ILfs.FALSE);
				return contour;
			}

			/* If 1st half contour forms a loop ... */
			if (ret.get() == ILfs.LOOP_FOUND) {
				/* Need to reverse the 1st half contour so that the points are */
				/* in consistent order. */
				/* We need to add the original feature point to the list, so */
				/* set new contour length to one plus length of 1st half contour. */
				contourCount = nHalf1.get() + 1;
				/* Allocate new contour list. */
				contour = allocateContour(ret, contourCount);
				if (ret.get() != ILfs.FALSE) {
					/* If allcation error, then deallocate memory allocated to */
					/* this point in this routine. */
					freeContour(contourHalf1);
					/* Return error code. */
					return (contour);
				}

				/* Otherwise, we have the new contour allocated, so store the */
				/* original feature point. */
				contour.getContourX().set(0, xPixelLoc);
				contour.getContourY().set(0, yPixelLoc);
				contour.getContourEx().set(0, xEdgePixelLoc);
				contour.getContourEy().set(0, yEdgePixelLoc);

				/* Now store the first half contour in reverse order. */
				for (i = 1, j = nHalf1.get() - 1; i < contourCount; i++, j--) {
					contour.getContourX().set(i, contourHalf1.getContourX().get(j));
					contour.getContourY().set(i, contourHalf1.getContourY().get(j));
					contour.getContourEx().set(i, contourHalf1.getContourEx().get(j));
					contour.getContourEy().set(i, contourHalf1.getContourEy().get(j));
				}

				/* Deallocate the first half contour. */
				freeContour(contourHalf1);

				/* Assign the output pointers. */
				oNoOfContour.set(contourCount);

				/* Return LOOP_FOUND for further processing. */
				ret.set(ILfs.LOOP_FOUND);
				return contour;
			}

			/* Otherwise, return the system error code from the first */
			/* call to trace_contour. */
			return contour;
		}

		/* If 1st half contour not complete ... */
		if (nHalf1.get() < halfContour) {
			/* Deallocate the partial contour. */
			freeContour(contourHalf1);
			/* Return, with nothing allocated and contour length equal to 0. */
			ret.set(ILfs.FALSE);
			return contour;
		}

		/* Otherwise, we have a complete 1st half contour... */
		/* Get 2nd half contour with counter-clockwise neighbor trace. */
		/* Use the last point from the first contour trace as the */
		/* point to test for a loop when tracing the second contour. */
		contourHalf2 = traceContour(ret, nHalf2, halfContour, contourHalf1.getContourX().get(nHalf1.get() - 1),
				contourHalf1.getContourY().get(nHalf1.get() - 1), xPixelLoc, yPixelLoc, xEdgePixelLoc, yEdgePixelLoc,
				ILfs.SCAN_COUNTER_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		if (ret.get() != ILfs.FALSE) {
			/* If 2nd trace was not possible ... */
			if (ret.get() == ILfs.IGNORE) {
				/* Deallocate the 1st half contour. */
				freeContour(contourHalf1);
				/* Return, with nothing allocated and contour length equal to 0. */
				ret.set(ILfs.FALSE);
				return contour;
			}

			/* If non-zero return code is NOT LOOP_FOUND, then system error ... */
			if (ret.get() != ILfs.LOOP_FOUND) {
				/* Deallocate the 1st half contour. */
				freeContour(contourHalf1);
				/* Return system error. */
				return contour;
			}
		}

		/* If 2nd half NOT a loop AND not complete ... */
		if ((ret.get() != ILfs.LOOP_FOUND) && (nHalf2.get() < halfContour)) {
			/* Deallocate both half contours. */
			freeContour(contourHalf1);
			freeContour(contourHalf2);
			/* Return, with nothing allocated and contour length equal to 0. */
			ret.set(ILfs.FALSE);
			return contour;
		}

		/* Otherwise we have a full 1st half contour and a 2nd half contour */
		/* that is either a loop or complete. In either case we need to */
		/* concatenate the two half contours into one longer contour. */

		/* Allocate output contour list. Go ahead and allocate the */
		/* "max_contour" amount even though the resulting contour will */
		/* likely be shorter if it forms a loop. */
		contour = allocateContour(ret, maxContour);
		if (ret.get() != ILfs.FALSE) {
			/* If allcation error, then deallocate memory allocated to */
			/* this point in this routine. */
			freeContour(contourHalf1);
			freeContour(contourHalf2);
			/* Return error code. */
			return contour;
		}

		/* Set the current contour point counter to 0 */
		contourCount = 0;

		/* Copy 1st half contour into output contour buffers. */
		/* This contour was collected clockwise, so it's points */
		/* are entered in reverse order of the trace. The result */
		/* is the first point in the output contour if farthest */
		/* from the starting feature point. */
		for (i = 0, j = nHalf1.get() - 1; i < nHalf1.get(); i++, j--) {
			contour.getContourX().set(i, contourHalf1.getContourX().get(j));
			contour.getContourY().set(i, contourHalf1.getContourY().get(j));
			contour.getContourEx().set(i, contourHalf1.getContourEx().get(j));
			contour.getContourEy().set(i, contourHalf1.getContourEy().get(j));

			contourCount++;
		}

		/* Deallocate 1st half contour. */
		freeContour(contourHalf1);

		/* Next, store starting feature point into output contour buffers. */
		contour.getContourX().set(nHalf1.get(), xPixelLoc);
		contour.getContourY().set(nHalf1.get(), yPixelLoc);
		contour.getContourEx().set(nHalf1.get(), xEdgePixelLoc);
		contour.getContourEy().set(nHalf1.get(), yEdgePixelLoc);

		contourCount++;

		/* Now, append 2nd half contour to permanent contour buffers. */
		for (i = 0, j = nHalf1.get() + 1; i < nHalf2.get(); i++, j++) {
			contour.getContourX().set(j, contourHalf2.getContourX().get(i));
			contour.getContourY().set(j, contourHalf2.getContourY().get(i));
			contour.getContourEx().set(j, contourHalf2.getContourEx().get(i));
			contour.getContourEy().set(j, contourHalf2.getContourEy().get(i));

			contourCount++;
		}

		/* Deallocate 2nd half contour. */
		freeContour(contourHalf2);

		/* Assign outputs contour to output ponters. */
		oNoOfContour.set(contourCount);

		/* Return the resulting return code form the 2nd call to trace_contour */
		/* (the value will either be 0 or LOOP_FOUND). */
		return contour;
	}

	/**
	 * Takes the pixel coordinate of a detected minutia feature point and its
	 * corresponding/adjacent edge pixel and attempts to extract a contour of
	 * specified length centered on the feature point.
	 * <p>
	 * NIST: {@code get_centered_contour()}. The contour is extracted by walking
	 * the feature's edge a specified number of steps clockwise and then
	 * counter-clockwise. If a loop is detected while extracting the contour, no
	 * contour is returned, with a return code of {@link ILfs#LOOP_FOUND}. If the
	 * process fails to extract a complete contour, a code of
	 * {@link ILfs#INCOMPLETE} is returned.
	 *
	 * @param ret                output: zero ({@link ILfs#FALSE}) if the
	 *                           resulting contour was successfully extracted or
	 *                           is empty; {@link ILfs#LOOP_FOUND} if the
	 *                           resulting contour forms a complete loop;
	 *                           {@link ILfs#IGNORE} if the contour could not be
	 *                           traced due to problem starting conditions;
	 *                           {@link ILfs#INCOMPLETE} if the resulting contour
	 *                           was not long enough; negative on system error
	 * @param oNoOfContour       output: number of contour points returned
	 * @param halfContour        half the length of the extracted contour
	 *                           (full-length non-loop contour =
	 *                           {@code halfContour * 2 + 1})
	 * @param xPixelLoc          starting x-pixel coord of feature (interior to
	 *                           feature)
	 * @param yPixelLoc          starting y-pixel coord of feature (interior to
	 *                           feature)
	 * @param xEdgePixelLoc      x-pixel coord of corresponding edge pixel
	 *                           (exterior to feature)
	 * @param yEdgePixelLoc      y-pixel coord of corresponding edge pixel
	 *                           (exterior to feature)
	 * @param binarizedImageData binary image data (0 = white, 1 = black)
	 * @param imageWidth         width (in pixels) of image
	 * @param imageHeight        height (in pixels) of image
	 * @return the extracted contour (contour-point and edge-point x/y lists), or
	 *         {@code null} if no contour was extracted
	 */
	public Contour getCenteredContour(AtomicInteger ret, AtomicInteger oNoOfContour, final int halfContour,
			final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc, final int yEdgePixelLoc,
			int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		Contour contour = null;
		Contour contourHalf1 = null;
		Contour contourHalf2 = null;
		int maxContour;
		AtomicInteger nHalf1 = new AtomicInteger(0);
		AtomicInteger nHalf2 = new AtomicInteger(0);
		int contourCount;
		int i;
		int j;

		/* Compute maximum length of complete contour */
		/* (2 half contours + feature point). */
		maxContour = (halfContour << 1) + 1;

		/* Initialize output contour length to 0. */
		oNoOfContour.set(0);

		/* Get 1st half contour with clockwise neighbor trace. */
		contourHalf1 = traceContour(ret, nHalf1, halfContour, xPixelLoc, yPixelLoc, xPixelLoc, yPixelLoc, xEdgePixelLoc,
				yEdgePixelLoc, ILfs.SCAN_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		/* If system error occurred ... */
		if (ret.get() < ILfs.FALSE) {
			/* Return error code. */
			return contour;
		}

		/* If trace was not possible ... */
		if (ret.get() == ILfs.IGNORE) {
			/* Return IGNORE, with nothing allocated. */
			ret.set(ILfs.IGNORE);
			return contour;
		}

		/* If 1st half contour forms a loop ... */
		if (ret.get() == ILfs.LOOP_FOUND) {
			/* Deallocate loop's contour. */
			freeContour(contourHalf1);
			/* Return LOOP_FOUND, with nothing allocated. */
			ret.set(ILfs.LOOP_FOUND);
			return contour;
		}

		/* If 1st half contour not complete ... */
		if (nHalf1.get() < halfContour) {
			/* Deallocate the partial contour. */
			freeContour(contourHalf1);
			/* Return, with nothing allocated and contour length equal to 0. */
			ret.set(ILfs.INCOMPLETE);
			return contour;
		}

		/* Otherwise, we have a complete 1st half contour... */
		/* Get 2nd half contour with counter-clockwise neighbor trace. */
		/* Use the last point from the first contour trace as the */
		/* point to test for a loop when tracing the second contour. */
		contourHalf2 = traceContour(ret, nHalf2, halfContour, contourHalf1.getContourX().get(nHalf1.get() - 1),
				contourHalf1.getContourY().get(nHalf1.get() - 1), xPixelLoc, yPixelLoc, xEdgePixelLoc, yEdgePixelLoc,
				ILfs.SCAN_COUNTER_CLOCKWISE, binarizedImageData, imageWidth, imageHeight);
		/* If system error occurred on 2nd trace ... */
		if (ret.get() < ILfs.FALSE) {
			/* Return error code. */
			return contour;
		}

		/* If 2nd trace was not possible ... */
		if (ret.get() == ILfs.IGNORE) {
			/* Deallocate the 1st half contour. */
			freeContour(contourHalf1);
			/* Return, with nothing allocated and contour length equal to 0. */

			ret.set(ILfs.IGNORE);
			return contour;
		}

		/* If 2nd trace forms a loop ... */
		if (ret.get() == ILfs.LOOP_FOUND) {
			/* Deallocate 1st and 2nd half contours. */
			freeContour(contourHalf1);
			freeContour(contourHalf2);
			/* Return LOOP_FOUND, with nothing allocated. */
			ret.set(ILfs.LOOP_FOUND);
			return contour;
		}

		/* If 2nd half contour not complete ... */
		if (nHalf2.get() < halfContour) {
			/* Deallocate 1st and 2nd half contours. */
			freeContour(contourHalf1);
			freeContour(contourHalf2);
			/* Return, with nothing allocated and contour length equal to 0. */
			ret.set(ILfs.INCOMPLETE);
			return contour;
		}

		/* Otherwise we have a full 1st half contour and a 2nd half contour */
		/* that do not form a loop and are complete. We now need to */
		/* concatenate the two half contours into one longer contour. */

		/* Allocate output contour list. */
		contour = allocateContour(ret, maxContour);
		if (ret.get() != ILfs.FALSE) {
			/* If allcation error, then deallocate memory allocated to */
			/* this point in this routine. */
			freeContour(contourHalf1);
			freeContour(contourHalf2);
			/* Return error code. */
			return contour;
		}

		/* Set the current contour point counter to 0 */
		contourCount = 0;

		/* Copy 1st half contour into output contour buffers. */
		/* This contour was collected clockwise, so it's points */
		/* are entered in reverse order of the trace. The result */
		/* is the first point in the output contour if farthest */
		/* from the starting feature point. */
		for (i = 0, j = nHalf1.get() - 1; i < nHalf1.get(); i++, j--) {
			contour.getContourX().set(i, contourHalf1.getContourX().get(j));
			contour.getContourY().set(i, contourHalf1.getContourY().get(j));
			contour.getContourEx().set(i, contourHalf1.getContourEx().get(j));
			contour.getContourEy().set(i, contourHalf1.getContourEy().get(j));
			contourCount++;
		}

		/* Deallocate 1st half contour. */
		freeContour(contourHalf1);

		/* Next, store starting feature point into output contour buffers. */
		contour.getContourX().set(nHalf1.get(), xPixelLoc);
		contour.getContourY().set(nHalf1.get(), yPixelLoc);
		contour.getContourEx().set(nHalf1.get(), xEdgePixelLoc);
		contour.getContourEy().set(nHalf1.get(), yEdgePixelLoc);

		contourCount++;

		/* Now, append 2nd half contour to permanent contour buffers. */
		for (i = 0, j = nHalf1.get() + 1; i < nHalf2.get(); i++, j++) {
			contour.getContourX().set(j, contourHalf2.getContourX().get(i));
			contour.getContourY().set(j, contourHalf2.getContourY().get(i));
			contour.getContourEx().set(j, contourHalf2.getContourEx().get(i));
			contour.getContourEy().set(j, contourHalf2.getContourEy().get(i));

			contourCount++;
		}

		/* Deallocate 2nd half contour. */
		freeContour(contourHalf2);

		/* Assign outputs contour to output ponters. */
		oNoOfContour.set(contourCount);

		/* Return normally. */
		ret.set(ILfs.FALSE);
		return contour;
	}

	/**
	 * Takes the pixel coordinate of a detected minutia feature point and its
	 * corresponding/adjacent edge pixel and extracts a contour (up to a specified
	 * maximum length) of the feature's edge in either a clockwise or
	 * counter-clockwise direction.
	 * <p>
	 * NIST: {@code trace_contour()}. A second point is specified, such that if
	 * this point is encountered while extracting the contour, it is assumed that
	 * a loop has been found and a code of {@link ILfs#LOOP_FOUND} is returned
	 * with the contour. By independently specifying this point, successive calls
	 * can be made to this routine from the same starting point, and loops across
	 * successive calls can be detected. The trace stops early (normally) if no
	 * next contour pixel can be found.
	 *
	 * @param ret                output: zero ({@link ILfs#FALSE}) if the
	 *                           resulting contour was successfully allocated and
	 *                           extracted; {@link ILfs#LOOP_FOUND} if the
	 *                           resulting contour forms a complete loop;
	 *                           {@link ILfs#IGNORE} if the trace is not possible
	 *                           due to the state of the inputs (feature and edge
	 *                           pixels have the same value); negative on system
	 *                           error
	 * @param oNoOfContour       output: number of contour points returned
	 * @param maxLenOfContour    maximum length of contour to be extracted
	 * @param xLoop              x-pixel coord of point which, if encountered,
	 *                           triggers {@link ILfs#LOOP_FOUND}
	 * @param yLoop              y-pixel coord of point which, if encountered,
	 *                           triggers {@link ILfs#LOOP_FOUND}
	 * @param xPixelLoc          starting x-pixel coord of feature (interior to
	 *                           feature)
	 * @param yPixelLoc          starting y-pixel coord of feature (interior to
	 *                           feature)
	 * @param xEdgePixelLoc      x-pixel coord of corresponding edge pixel
	 *                           (exterior to feature)
	 * @param yEdgePixelLoc      y-pixel coord of corresponding edge pixel
	 *                           (exterior to feature)
	 * @param scanClock          direction in which neighbouring pixels are to be
	 *                           scanned for the next contour pixel
	 *                           ({@link ILfs#SCAN_CLOCKWISE} or
	 *                           {@link ILfs#SCAN_COUNTER_CLOCKWISE})
	 * @param binarizedImageData binary image data (0 = white, 1 = black)
	 * @param imageWidth         width (in pixels) of image
	 * @param imageHeight        height (in pixels) of image
	 * @return the traced contour (allocated to {@code maxLenOfContour} points;
	 *         only the first {@code oNoOfContour} are valid), or {@code null} if
	 *         the trace was ignored
	 */
	public Contour traceContour(AtomicInteger ret, AtomicInteger oNoOfContour, final int maxLenOfContour,
			final int xLoop, final int yLoop, final int xPixelLoc, final int yPixelLoc, final int xEdgePixelLoc,
			final int yEdgePixelLoc, final int scanClock, int[] binarizedImageData, final int imageWidth,
			final int imageHeight) {
		Contour contour = null;
		int contourCount;
		int currentXPixelLoc;
		int currentYPixelLoc;
		int currentXEdgePixelLoc;
		int currentYEdgePixelLoc;
		AtomicInteger nextXPixelLoc = new AtomicInteger(0);
		AtomicInteger nextYPixelLoc = new AtomicInteger(0);
		AtomicInteger nextXEdgePixelLoc = new AtomicInteger(0);
		AtomicInteger nextYEdgePixelLoc = new AtomicInteger(0);
		int i;

		/* Check to make sure that the feature and edge values are opposite. */
		if (binarizedImageData[0 + (yPixelLoc * imageWidth) + xPixelLoc] == binarizedImageData[0
				+ (yEdgePixelLoc * imageWidth) + xEdgePixelLoc]) {
			/* If not opposite, then the trace will not work, so return IGNORE. */
			ret.set(ILfs.IGNORE);
			return (contour);
		}

		/* Allocate contour buffers. */
		contour = allocateContour(ret, maxLenOfContour);
		if (ret.get() != ILfs.FALSE) {
			/* If allocation error, return code. */
			return (contour);
		}

		/* Set pixel counter to 0. */
		contourCount = 0;

		/* Set up for finding first contour pixel. */
		currentXPixelLoc = xPixelLoc;
		currentYPixelLoc = yPixelLoc;
		currentXEdgePixelLoc = xEdgePixelLoc;
		currentYEdgePixelLoc = yEdgePixelLoc;

		/* Foreach pixel to be collected on the feature's contour... */
		for (i = 0; i < maxLenOfContour; i++) {
			/* Find the next contour pixel. */
			if (nextContourPixel(nextXPixelLoc, nextYPixelLoc, nextXEdgePixelLoc, nextYEdgePixelLoc, currentXPixelLoc,
					currentYPixelLoc, currentXEdgePixelLoc, currentYEdgePixelLoc, scanClock, binarizedImageData,
					imageWidth, imageHeight) == ILfs.TRUE) {
				/* If we trace back around to the specified starting */
				/* feature location... */
				if ((nextXPixelLoc.get() == xLoop) && (nextYPixelLoc.get() == yLoop)) {
					/* Then we have found a loop, so return what we */
					/* have traced to this point. */

					oNoOfContour.set(contourCount);
					ret.set(ILfs.LOOP_FOUND);
					return (contour);
				}

				/* Otherwise, we found another point on our feature's contour, */
				/* so store the new contour point. */
				contour.getContourX().set(i, nextXPixelLoc.get());
				contour.getContourY().set(i, nextYPixelLoc.get());
				contour.getContourEx().set(i, nextXEdgePixelLoc.get());
				contour.getContourEy().set(i, nextYEdgePixelLoc.get());

				/* Bump the number of points stored. */
				contourCount++;

				/* Set up for finding next contour pixel. */
				currentXPixelLoc = nextXPixelLoc.get();
				currentYPixelLoc = nextYPixelLoc.get();
				currentXEdgePixelLoc = nextXEdgePixelLoc.get();
				currentYEdgePixelLoc = nextYEdgePixelLoc.get();
			}
			/* Otherwise, no new contour point found ... */
			else {
				/* So, stop short and return the number of pixels found */
				/* on the contour to this point. */
				oNoOfContour.set(contourCount);

				/* Return normally. */
				ret.set(ILfs.FALSE);
				return (contour);
			}
		}

		/* If we get here, we successfully found the maximum points we */
		/* were looking for on the feature contour, so assign the contour */
		/* buffers to the output pointers and return. */
		oNoOfContour.set(contourCount);

		/* Return normally. */
		ret.set(ILfs.FALSE);
		return (contour);
	}

	/**
	 * Walks the contour of a minutia feature starting at a specified point on
	 * the feature and walking N steps in the specified direction (clockwise or
	 * counter-clockwise), looking for a second specified point.
	 * <p>
	 * NIST: {@code search_contour()}. In this code, "feature" consistently
	 * refers to either the black interior edge of a ridge ending or the white
	 * interior edge of a valley ending (bifurcation). The term "edge of the
	 * feature" refers to neighbouring pixels on the "exterior" edge of the
	 * feature, so "edge" pixels are opposite in color from the interior feature
	 * pixels.
	 *
	 * @param xPixelSearch       x-pixel coord of point being searched for
	 * @param yPixelSearch       y-pixel coord of point being searched for
	 * @param searchLen          number of steps to walk the contour in search
	 * @param xPixelLoc          starting x-pixel coord of feature (interior to
	 *                           feature)
	 * @param yPixelLoc          starting y-pixel coord of feature (interior to
	 *                           feature)
	 * @param xEdgePixelLoc      x-pixel coord of corresponding edge pixel
	 *                           (exterior to feature)
	 * @param yEdgePixelLoc      y-pixel coord of corresponding edge pixel
	 *                           (exterior to feature)
	 * @param scanClock          direction in which neighbour pixels are to be
	 *                           scanned (clockwise or counter-clockwise)
	 * @param binarizedImageData binary image data (0 = white, 1 = black)
	 * @param imageWidth         width (in pixels) of image
	 * @param imageHeight        height (in pixels) of image
	 * @return {@link ILfs#FOUND} if the desired pixel WAS found along N steps of
	 *         the feature's contour; {@link ILfs#NOT_FOUND} otherwise
	 */
	public int searchContour(final int xPixelSearch, final int yPixelSearch, final int searchLen, final int xPixelLoc,
			final int yPixelLoc, final int xEdgePixelLoc, final int yEdgePixelLoc, final int scanClock,
			int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int currentXPixelLoc;
		int currentYPixelLoc;
		int currentXEdgePixelLoc;
		int currentYEdgePixelLoc;
		AtomicInteger nextXPixelLoc = new AtomicInteger(0);
		AtomicInteger nextYPixelLoc = new AtomicInteger(0);
		AtomicInteger nextXEdgePixelLoc = new AtomicInteger(0);
		AtomicInteger nextYEdgePixelLoc = new AtomicInteger(0);
		int i;

		/* Set up for finding first contour pixel. */
		currentXPixelLoc = xPixelLoc;
		currentYPixelLoc = yPixelLoc;
		currentXEdgePixelLoc = xEdgePixelLoc;
		currentYEdgePixelLoc = yEdgePixelLoc;

		/* Foreach point to be collected on the feature's contour... */
		for (i = 0; i < searchLen; i++) {
			/* Find the next contour pixel. */
			if (nextContourPixel(nextXPixelLoc, nextYPixelLoc, nextXEdgePixelLoc, nextYEdgePixelLoc, currentXPixelLoc,
					currentYPixelLoc, currentXEdgePixelLoc, currentYEdgePixelLoc, scanClock, binarizedImageData,
					imageWidth, imageHeight) == ILfs.TRUE) {
				/* If we find the point we are looking for on the contour... */
				if ((nextXPixelLoc.get() == xPixelSearch) && (nextYPixelLoc.get() == yPixelSearch)) {
					/* Then return FOUND. */
					return (ILfs.FOUND);
				}

				/* Otherwise, set up for finding next contour pixel. */
				currentXPixelLoc = nextXPixelLoc.get();
				currentYPixelLoc = nextYPixelLoc.get();
				currentXEdgePixelLoc = nextXEdgePixelLoc.get();
				currentYEdgePixelLoc = nextYEdgePixelLoc.get();
			}
			/* Otherwise, no new contour point found ... */
			else {
				/* So, stop searching, and return NOT_FOUND. */
				return (ILfs.NOT_FOUND);
			}
		}

		/* If we get here, we successfully searched the maximum points */
		/* without finding our desired point, so return NOT_FOUND. */
		return (ILfs.NOT_FOUND);
	}

	/**
	 * Takes a pixel coordinate of a point determined to be on the interior edge
	 * of a feature (ridge or valley ending), and attempts to locate a
	 * neighbouring pixel on the feature's contour.
	 * <p>
	 * NIST: {@code next_contour_pixel()}. Neighbours of the current feature pixel
	 * are searched in a specified direction (clockwise or counter-clockwise) and
	 * the first pair of adjacent/neighbouring pixels found with the first pixel
	 * having the color of the feature and the second the opposite color are
	 * returned as the next point on the contour. One exception happens when the
	 * new point is on an "exposed" corner, which is skipped. Failure (no next
	 * pixel, or a neighbour outside the image) usually means a single isolated
	 * pixel was found.
	 *
	 * @param nextXPixelLoc        output: x-pixel coord of next point on
	 *                             feature's interior contour
	 * @param nextYPixelLoc        output: y-pixel coord of next point on
	 *                             feature's interior contour
	 * @param nextXEdgePixelLoc    output: x-pixel coord of corresponding edge
	 *                             (exterior to feature)
	 * @param nextYEdgePixelLoc    output: y-pixel coord of corresponding edge
	 *                             (exterior to feature)
	 * @param currentXPixelLoc     x-pixel coord of current point on feature's
	 *                             interior contour
	 * @param currentYPixelLoc     y-pixel coord of current point on feature's
	 *                             interior contour
	 * @param currentXEdgePixelLoc x-pixel coord of corresponding edge pixel
	 *                             (exterior to feature)
	 * @param currentYEdgePixelLoc y-pixel coord of corresponding edge pixel
	 *                             (exterior to feature)
	 * @param scanClock            direction in which neighbouring pixels are to
	 *                             be scanned for the next contour pixel
	 * @param binarizedImageData   binary image data (0 = white, 1 = black)
	 * @param imageWidth           width (in pixels) of image
	 * @param imageHeight          height (in pixels) of image
	 * @return {@link ILfs#TRUE} if the next contour point was found and returned;
	 *         {@link ILfs#FALSE} if the next contour point was NOT found
	 */
	@SuppressWarnings({ "java:S3776" })
	public int nextContourPixel(AtomicInteger nextXPixelLoc, AtomicInteger nextYPixelLoc,
			AtomicInteger nextXEdgePixelLoc, AtomicInteger nextYEdgePixelLoc, final int currentXPixelLoc,
			final int currentYPixelLoc, final int currentXEdgePixelLoc, final int currentYEdgePixelLoc,
			final int scanClock, int[] binarizedImageData, final int imageWidth, final int imageHeight) {
		int featurePixel;
		int edgePixel;
		int previousNbrPixel;
		int previousNbrXPixel;
		int previousNbrYPixel;
		int currentNbrPixel;
		int currentNbrXPixel;
		int currentNbrYPixel;
		int ni;
		int nx;
		int ny;
		int npix;
		int nbrIndex;
		int i;

		/* Get the feature's pixel value. */
		featurePixel = binarizedImageData[0 + (currentYPixelLoc * imageWidth) + currentXPixelLoc];
		/* Get the feature's edge pixel value. */
		edgePixel = binarizedImageData[0 + (currentYEdgePixelLoc * imageWidth) + currentXEdgePixelLoc];

		/* Get the nieghbor position of the feature's edge pixel in relationship */
		/* to the feature's actual position. */
		/* REMEBER: The feature's position is always interior and on a ridge */
		/* ending (black pixel) or (for bifurcations) on a valley ending (white */
		/* pixel). The feature's edge pixel is an adjacent pixel to the feature */
		/* pixel that is exterior to the ridge or valley ending and opposite in */
		/* pixel value. */
		nbrIndex = startScanNbr(currentXPixelLoc, currentYPixelLoc, currentXEdgePixelLoc, currentYEdgePixelLoc);

		/* Set current neighbor scan pixel to the feature's edge pixel. */
		currentNbrXPixel = currentXEdgePixelLoc;
		currentNbrYPixel = currentYEdgePixelLoc;
		currentNbrPixel = edgePixel;

		/* Foreach pixel neighboring the feature pixel ... */
		for (i = 0; i < 8; i++) {
			/* Set current neighbor scan pixel to previous scan pixel. */
			previousNbrXPixel = currentNbrXPixel;
			previousNbrYPixel = currentNbrYPixel;
			previousNbrPixel = currentNbrPixel;

			/* Bump pixel neighbor index clockwise or counter-clockwise. */
			nbrIndex = nextScanNbr(nbrIndex, scanClock);

			/* Set current scan pixel to the new neighbor. */
			/* REMEMBER: the neighbors are being scanned around the original */
			/* feature point. */
			currentNbrXPixel = currentXPixelLoc + getGlobals().getNbr8Dx()[nbrIndex];
			currentNbrYPixel = currentYPixelLoc + getGlobals().getNbr8Dy()[nbrIndex];

			/* If new neighbor is not within image boundaries... */
			if ((currentNbrXPixel < 0) || (currentNbrXPixel >= imageWidth) || (currentNbrYPixel < 0)
					|| (currentNbrYPixel >= imageHeight)) {
				/* Return (FALSE==>Failure) if neighbor out of bounds. */
				return ILfs.FALSE;
			}

			/* Get the new neighbor's pixel value. */
			currentNbrPixel = binarizedImageData[0 + (currentNbrYPixel * imageWidth) + currentNbrXPixel];

			/* If the new neighbor's pixel value is the same as the feature's */
			/* pixel value AND the previous neighbor's pixel value is the same */
			/* as the features's edge, then we have "likely" found our next */
			/* contour pixel. */
			if ((currentNbrPixel == featurePixel) && (previousNbrPixel == edgePixel)) {
				/* Check to see if current neighbor is on the corner of the */
				/* neighborhood, and if so, test to see if it is "exposed". */
				/* The neighborhood corners have odd neighbor indicies. */
				if ((nbrIndex % 2) != ILfs.FALSE) {
					/* To do this, look ahead one more neighbor pixel. */
					ni = nextScanNbr(nbrIndex, scanClock);
					nx = currentXPixelLoc + getGlobals().getNbr8Dx()[ni];
					ny = currentYPixelLoc + getGlobals().getNbr8Dy()[ni];
					/* If new neighbor is not within image boundaries... */
					if ((nx < 0) || (nx >= imageWidth) || (ny < 0) || (ny >= imageHeight)) {
						/* Return (FALSE==>Failure) if neighbor out of bounds. */
						return ILfs.FALSE;
					}
					npix = binarizedImageData[0 + (ny * imageWidth) + nx];

					/* If the next neighbor's value is also the same as the */
					/* feature's pixel, then corner is NOT exposed... */
					if (npix == featurePixel) {
						/* Assign the current neighbor pair to the output pointers. */
						nextXPixelLoc.set(currentNbrXPixel);
						nextYPixelLoc.set(currentNbrYPixel);
						nextXEdgePixelLoc.set(previousNbrXPixel);
						nextYEdgePixelLoc.set(previousNbrYPixel);
						/* Return TRUE==>Success. */
						return ILfs.TRUE;
					}
					/* Otherwise, corner pixel is "exposed" so skip it. */
					else {
						/* Skip current corner neighbor by resetting it to the */
						/* next neighbor, which upon the iteration will immediately */
						/* become the previous neighbor. */
						currentNbrXPixel = nx;
						currentNbrYPixel = ny;
						currentNbrPixel = npix;
						/* Advance neighbor index. */
						nbrIndex = ni;
						/* Advance neighbor count. */
						i++;
					}
				}
				/* Otherwise, current neighbor is not a corner ... */
				else {
					/* Assign the current neighbor pair to the output pointers. */
					nextXPixelLoc.set(currentNbrXPixel);
					nextYPixelLoc.set(currentNbrYPixel);
					nextXEdgePixelLoc.set(previousNbrXPixel);
					nextYEdgePixelLoc.set(previousNbrYPixel);
					/* Return TRUE==>Success. */
					return ILfs.TRUE;
				}
			}
		}

		/* If we get here, then we did not find the next contour pixel */
		/* within the 8 neighbors of the current feature pixel so */
		/* return (FALSE==>Failure). */
		/* NOTE: This must mean we found a single isolated pixel. */
		/* Perhaps this should be filled? */
		return ILfs.FALSE;
	}

	/**
	 * Takes two pixel coordinates that are either aligned north-to-south or
	 * east-to-west, and returns the position the second pixel is in relationship
	 * to the first.
	 * <p>
	 * NIST: {@code start_scan_nbr()}. The positions returned are based on
	 * 8-connectedness (neighbour indices as in {@link Globals#getNbr8Dx()}).
	 * NOTE: this routine does NOT account for diagonal positions.
	 *
	 * @param previousXPixelLoc x-coord of first point
	 * @param previousYPixelLoc y-coord of first point
	 * @param nextXPixelLoc     x-coord of second point
	 * @param nextYPixelLoc     y-coord of second point
	 * @return {@link ILfs#NORTH} if the second pixel is above the first;
	 *         {@link ILfs#SOUTH} if below; {@link ILfs#EAST} if right of the
	 *         first; {@link ILfs#WEST} if left of the first;
	 *         {@link ILfs#INVALID_DIR} (-1) otherwise (should never happen)
	 */
	public int startScanNbr(final int previousXPixelLoc, final int previousYPixelLoc, final int nextXPixelLoc,
			final int nextYPixelLoc) {
		if ((previousXPixelLoc == nextXPixelLoc) && (nextYPixelLoc > previousYPixelLoc)) {
			return (ILfs.SOUTH);
		} else if ((previousXPixelLoc == nextXPixelLoc) && (nextYPixelLoc < previousYPixelLoc)) {
			return (ILfs.NORTH);
		} else if ((nextXPixelLoc > previousXPixelLoc) && (previousYPixelLoc == nextYPixelLoc)) {
			return (ILfs.EAST);
		} else if ((nextXPixelLoc < previousXPixelLoc) && (previousYPixelLoc == nextYPixelLoc)) {
			return (ILfs.WEST);
		}

		/* Added by MDG on 03-16-05 */
		/* Should never reach here. Added to remove compiler warning. */
		return (ILfs.INVALID_DIR); // -1
	}

	/**
	 * Advances the given 8-connected neighbour index one location in the
	 * specified direction (clockwise or counter-clockwise), wrapping around 0..7.
	 * <p>
	 * NIST: {@code next_scan_nbr()}.
	 *
	 * @param nbrIndex  current 8-connected neighbour index
	 * @param scanClock direction in which the neighbour index is to be advanced
	 *                  ({@link ILfs#SCAN_CLOCKWISE} or any other value for
	 *                  counter-clockwise)
	 * @return the 8-connected index of the next neighbour
	 */
	public int nextScanNbr(final int nbrIndex, final int scanClock) {
		int nextNeighborIndex;

		/* If scanning neighbors clockwise ... */
		if (scanClock == ILfs.SCAN_CLOCKWISE) {
			/* Advance one neighbor clockwise. */
			nextNeighborIndex = (nbrIndex + 1) % 8;
		}
		/* Otherwise, scanning neighbors counter-clockwise ... */
		else {
			/* Advance one neighbor counter-clockwise. */
			/* There are 8 pixels in the neighborhood, so to */
			/* decrement with wrapping from 0 around to 7, add */
			/* the nieghbor index by 7 and mod with 8. */
			nextNeighborIndex = (nbrIndex + 7) % 8;
		}

		/* Return the new neighbor index. */
		return (nextNeighborIndex);
	}

	/**
	 * Takes a contour list and analyzes it, locating the point at which the
	 * contour has highest curvature (or minimum interior angle).
	 * <p>
	 * NIST: {@code min_contour_theta()}. The angle of curvature is computed by
	 * searching a majority of points on the contour. At each of these points, a
	 * left and right segment (or edge) are extended out N pixels from the center
	 * point on the contour. The angle is formed between the straight line
	 * connecting the center point to the end point on the left edge and the line
	 * connecting the center point to the end of the right edge. The point of
	 * highest curvature is determined by locating where the minimum of these
	 * angles occurs. If the contour is perfectly flat, the middle point is
	 * returned.
	 *
	 * @param oMinContourPoint output: index of contour point where the minimum
	 *                         occurred
	 * @param oMinThetaAngle   output: minimum angle (in radians) found along the
	 *                         contour
	 * @param angleEdge        length of the left and right edges extending from a
	 *                         common/centered pixel on the contour
	 * @param contourX         x-coord list for contour points
	 * @param contourY         y-coord list for contour points
	 * @param noOfContour      number of points in contour
	 * @return zero ({@link ILfs#FALSE}) if the minimum angle was successfully
	 *         located; {@link ILfs#IGNORE} if the contour is too short and should
	 *         be ignored (negative would indicate a system error)
	 */
	public int minContourTheta(AtomicInteger oMinContourPoint, AtomicReference<Double> oMinThetaAngle,
			final int angleEdge, AtomicIntegerArray contourX, AtomicIntegerArray contourY, final int noOfContour) {
		int pointLeft;
		int pointCenter;
		int pointRight;
		double theta1;
		double theta2;
		double dtheta;
		int minContourPoint;
		double minThetaAngle;

		/* If the contour length is too short for processing... */
		if (noOfContour < (angleEdge << 1) + 1) {
			/* Return IGNORE. */
			return (ILfs.IGNORE);
		}

		/* Intialize running minimum values. */
		minThetaAngle = Math.PI;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when comparing doubles. */
		minThetaAngle = getDefs().truncDoublePrecision(minThetaAngle, ILfs.TRUNC_SCALE);
		minContourPoint = -1;

		/* Set left angle point to first contour point. */
		pointLeft = 0;
		/* Set center angle point to "angleEdge" points into contour. */
		pointCenter = angleEdge;
		/* Set right angle point to "angleEdge" points from pcenter. */
		pointRight = pointCenter + angleEdge;

		/* Loop until the right angle point exceeds the contour list. */
		while (pointRight < noOfContour) {
			/* Compute angle to first edge line (connecting pcenter to pleft). */
			theta1 = getLfsUtil().angleToLine(contourX.get(pointCenter), contourY.get(pointCenter),
					contourX.get(pointLeft), contourY.get(pointLeft));
			/* Compute angle to second edge line (connecting pcenter to pright). */
			theta2 = getLfsUtil().angleToLine(contourX.get(pointCenter), contourY.get(pointCenter),
					contourX.get(pointRight), contourY.get(pointRight));

			/* Compute delta between angles accounting for an inner */
			/* and outer distance between the angles. */
			dtheta = Math.abs(theta2 - theta1);
			dtheta = Math.min(dtheta, (Math.PI * 2.0) - dtheta);
			/* Need to truncate precision so that answers are consistent */
			/* on different computer architectures when comparing doubles. */
			dtheta = getDefs().truncDoublePrecision(dtheta, ILfs.TRUNC_SCALE);

			/* Keep track of running minimum theta. */
			if (dtheta < minThetaAngle) {
				minContourPoint = pointCenter;
				minThetaAngle = dtheta;
			}

			/* Bump to next points on contour. */
			pointLeft++;
			pointCenter++;
			pointRight++;
		}

		/* If no minimum found (then contour is perfectly flat) so minimum */
		/* to center point on contour. */
		if (minContourPoint == ILfs.UNDEFINED) {
			oMinContourPoint.set(noOfContour >> 1);
			oMinThetaAngle.set(minThetaAngle);
		} else {
			/* Assign minimum theta information to output pointers. */
			oMinContourPoint.set(minContourPoint);
			oMinThetaAngle.set(minThetaAngle);
		}

		/* Return successfully. */
		return (ILfs.FALSE);
	}

	/**
	 * Determines the X and Y coordinate limits of the given contour list.
	 * <p>
	 * NIST: {@code contour_limits()}.
	 *
	 * @param xMin        output: left-most x-coord in contour
	 * @param yMin        output: top-most y-coord in contour
	 * @param xMax        output: right-most x-coord in contour
	 * @param yMax        output: bottom-most y-coord in contour
	 * @param contourX    x-coord list for contour points
	 * @param contourY    y-coord list for contour points
	 * @param noOfContour number of points in contour (must be positive)
	 */
	public void contourLimits(AtomicInteger xMin, AtomicInteger yMin, AtomicInteger xMax, AtomicInteger yMax,
			AtomicIntegerArray contourX, AtomicIntegerArray contourY, final int noOfContour) {
		/* Find the minimum x-coord from the list of contour points. */
		xMin.set(getLfsUtil().minValue(contourX, noOfContour));
		/* Find the minimum y-coord from the list of contour points. */
		yMin.set(getLfsUtil().minValue(contourY, noOfContour));
		/* Find the maximum x-coord from the list of contour points. */
		xMax.set(getLfsUtil().maxValue(contourX, noOfContour));
		/* Find the maximum y-coord from the list of contour points. */
		yMax.set(getLfsUtil().maxValue(contourY, noOfContour));
	}

	/**
	 * Takes a pair of pixel points, with the first pixel on a feature and the
	 * second adjacent and off the feature, and determines whether the pair
	 * neighbour diagonally.
	 * <p>
	 * NIST: {@code fix_edge_pixel_pair()}. If they do, their locations are
	 * adjusted so that the resulting pair retains the same pixel values but are
	 * neighbouring either to the N, S, E or W. This routine is needed in order to
	 * prepare the pixel pair for contour tracing. If the pair is not diagonal,
	 * the inputs are left unchanged.
	 *
	 * @param featureXPixel      input/output: x-pixel coord on feature
	 * @param featureYPixel      input/output: y-pixel coord on feature
	 * @param featureEdgeXPixel  input/output: x-pixel coord on edge of feature
	 * @param featureEdgeYPixel  input/output: y-pixel coord on edge of feature
	 * @param binarizedImageData binary image data (0 = white, 1 = black)
	 * @param imageWidth         width (in pixels) of image
	 * @param imageHeight        height (in pixels) of image
	 */
	public void fixEdgePixelPair(AtomicInteger featureXPixel, AtomicInteger featureYPixel,
			AtomicInteger featureEdgeXPixel, AtomicInteger featureEdgeYPixel, int[] binarizedImageData,
			final int imageWidth, final int imageHeight) {
		int distPixelX;
		int distPixelY;
		int previousFeaturePixelX;
		int previousFeaturePixelY;
		int currentFeaturePixelX;
		int currentFeaturePixelY;
		int featurePixel;

		/* Get the pixel value of the feature. */
		featurePixel = binarizedImageData[0 + (featureYPixel.get() * imageWidth) + featureXPixel.get()];

		/* Store the input points to current and previous points. */
		currentFeaturePixelX = featureXPixel.get();
		currentFeaturePixelY = featureYPixel.get();
		previousFeaturePixelX = featureEdgeXPixel.get();
		previousFeaturePixelY = featureEdgeYPixel.get();

		/* Compute detlas between current and previous point. */
		distPixelX = previousFeaturePixelX - currentFeaturePixelX;
		distPixelY = previousFeaturePixelY - currentFeaturePixelY;

		/* If previous point (P) is diagonal neighbor of */
		/* current point (C)... This is a problem because */
		/* the contour tracing routine requires that the */
		/* "edge" pixel be north, south, east, or west of */
		/* of the feature point. If the previous pixel is */
		/* diagonal neighbor, then we need to adjust either */
		/* the positon of te previous or current pixel. */
		if ((Math.abs(distPixelX) == ILfs.TRUE) && (Math.abs(distPixelY) == ILfs.TRUE)) {
			/* Then we have one of the 4 following conditions: */
			/*                                                  */
			/* *C C* */
			/* 1. P* 2. P* 3. *P 4. *P */
			/* *C C* */
			/*                                                  */
			/* dx = -1 -1 1 1 */
			/* dy = 1 -1 -1 1 */
			/*                                                  */
			/* Want to test values in positions of '*': */
			/* Let point P == (px, py) */
			/* p1 == '*' positon where x changes */
			/* p2 == '*' positon where y changes */
			/*                                                  */
			/* p1 = px+1,py px+1,py px-1,py px-1,py */
			/* p2 = px,py-1 px,py+1 px,py+1 px,py-1 */
			/*                                                  */
			/* These can all be rewritten: */
			/* p1 = px-dx,py */
			/* p2 = px,py-dy */

			/* Check if 'p1' is NOT the value we are searching for... */
			if (binarizedImageData[0 + (previousFeaturePixelY * imageWidth)
					+ (previousFeaturePixelX - distPixelX)] != featurePixel) {
				/* Then set x-coord of edge pixel to p1. */
				previousFeaturePixelX -= distPixelX;
			}
			/* Check if 'p2' is NOT the value we are searching for... */
			else if (binarizedImageData[0 + ((previousFeaturePixelY - distPixelY) * imageWidth)
					+ previousFeaturePixelX] != featurePixel) {
				/* Then set y-coord of edge pixel to p2. */
				previousFeaturePixelY -= distPixelY;
			}
			/* Otherwise, the current pixel 'C' is exposed on a corner ... */
			else {
				/* Set pixel 'C' to 'p1', which also has the pixel */
				/* value we are searching for. */
				currentFeaturePixelY += distPixelY;
			}

			/* Set the pointers to the resulting values. */
			featureXPixel.set(currentFeaturePixelX);
			featureYPixel.set(currentFeaturePixelY);
			featureEdgeXPixel.set(previousFeaturePixelX);
			featureEdgeYPixel.set(previousFeaturePixelY);
		}

		/* Otherwise, nothing has changed. */
	}

	/**
	 * Returns the x-pixel coords of the contour points (interior to feature).
	 *
	 * @return the contour-point x list, or {@code null} if freed/unallocated
	 */
	public AtomicIntegerArray getContourX() {
		return contourX;
	}

	/**
	 * Sets the x-pixel coords of the contour points (interior to feature).
	 *
	 * @param contourX the contour-point x list (stored by reference)
	 */
	public void setContourX(AtomicIntegerArray contourX) {
		this.contourX = contourX;
	}

	/**
	 * Returns the y-pixel coords of the contour points (interior to feature).
	 *
	 * @return the contour-point y list, or {@code null} if freed/unallocated
	 */
	public AtomicIntegerArray getContourY() {
		return contourY;
	}

	/**
	 * Sets the y-pixel coords of the contour points (interior to feature).
	 *
	 * @param contourY the contour-point y list (stored by reference)
	 */
	public void setContourY(AtomicIntegerArray contourY) {
		this.contourY = contourY;
	}

	/**
	 * Returns the x-pixel coords of the edge points (exterior to feature).
	 *
	 * @return the edge-point x list, or {@code null} if freed/unallocated
	 */
	public AtomicIntegerArray getContourEx() {
		return contourEx;
	}

	/**
	 * Sets the x-pixel coords of the edge points (exterior to feature).
	 *
	 * @param contourEx the edge-point x list (stored by reference)
	 */
	public void setContourEx(AtomicIntegerArray contourEx) {
		this.contourEx = contourEx;
	}

	/**
	 * Returns the y-pixel coords of the edge points (exterior to feature).
	 *
	 * @return the edge-point y list, or {@code null} if freed/unallocated
	 */
	public AtomicIntegerArray getContourEy() {
		return contourEy;
	}

	/**
	 * Sets the y-pixel coords of the edge points (exterior to feature).
	 *
	 * @param contourEy the edge-point y list (stored by reference)
	 */
	public void setContourEy(AtomicIntegerArray contourEy) {
		this.contourEy = contourEy;
	}

	/**
	 * Returns the allocated number of items in each coordinate list.
	 *
	 * @return the allocated contour length
	 */
	public int getNoOfContour() {
		return noOfContour;
	}

	/**
	 * Sets the allocated number of items in each coordinate list.
	 *
	 * @param noOfContour the allocated contour length
	 */
	public void setNoOfcontour(int noOfContour) {
		this.noOfContour = noOfContour;
	}

	/**
	 * Returns a debug representation of this contour, listing its four
	 * coordinate lists and allocated length.
	 *
	 * @return a string describing this contour
	 */
	@Override
	public String toString() {
		return "Contour [contourX=" + contourX + ", contourY=" + contourY + ", contourEx=" + contourEx + ", contourEy="
				+ contourEy + ", noOfContour=" + noOfContour + "]";
	}
}