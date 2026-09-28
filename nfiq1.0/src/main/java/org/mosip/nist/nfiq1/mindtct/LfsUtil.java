package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.ILfsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Miscellaneous numeric, list and geometry utilities used across MINDTCT.
 * <p>
 * Port of NIST LFS {@code util.c}. Provides min/max search, detection of
 * relative minima/maxima, Euclidean and squared distances, list search,
 * removal and sorted-insertion helpers, angle/direction computations between
 * points, and the closest distance between two IMAP directions.
 * <p>
 * Implemented as a lazily created singleton; {@link #getInstance()} is
 * synchronized and the class keeps no mutable state.
 */
public class LfsUtil extends MindTct implements ILfsUtil {
	/** SLF4J logger for error reporting in this class. */
	private static final Logger logger = LoggerFactory.getLogger(LfsUtil.class);
	/** Lazily created singleton instance, see {@link #getInstance()}. */
	private static LfsUtil instance;

	/**
	 * Private constructor; use {@link #getInstance()} to obtain the singleton.
	 */
	private LfsUtil() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code LfsUtil} instance
	 */
	public static synchronized LfsUtil getInstance() {
		if (instance == null) {
			synchronized (LfsUtil.class) {
				if (instance == null) {
					instance = new LfsUtil();
				}
			}
		}
		return instance;
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
	 * Determines the maximum value in the given list of integers.
	 * <p>
	 * NIST: {@code maxv()}. NOTE: the list is assumed to be NOT empty.
	 *
	 * @param list non-empty list of integers to be searched
	 * @param num  number of integers in the list
	 * @return the maximum value in the list
	 */
	public int maxValue(final AtomicIntegerArray list, final int num) {
		int i;
		int maxval;

		/* NOTE: The list is assumed to be NOT empty. */
		/* Initialize running maximum to first item in list. */
		maxval = list.get(0);

		/* Foreach subsequent item in the list... */
		for (i = 1; i < num; i++) {
			/* If current item is larger than running maximum... */
			if (list.get(i) > maxval) {
				/* Set running maximum to the larger item. */
				maxval = list.get(i);
			}
			/* Otherwise, skip to next item. */
		}

		/* Return the resulting maximum. */
		return (maxval);
	}

	/**
	 * Determines the minimum value in the given list of integers.
	 * <p>
	 * NIST: {@code minv()}. NOTE: the list is assumed to be NOT empty.
	 *
	 * @param list non-empty list of integers to be searched
	 * @param num  number of integers in the list
	 * @return the minimum value in the list
	 */
	public int minValue(final AtomicIntegerArray list, final int num) {
		int i;
		int minval;

		/* NOTE: The list is assumed to be NOT empty. */
		/* Initialize running minimum to first item in list. */
		minval = list.get(0);

		/* Foreach subsequent item in the list... */
		for (i = 1; i < num; i++) {
			/* If current item is smaller than running minimum... */
			if (list.get(i) < minval) {
				/* Set running minimum to the smaller item. */
				minval = list.get(i);
			}
			/* Otherwise, skip to next item. */
		}

		/* Return the resulting minimum. */
		return (minval);
	}

	/**
	 * Takes a list of integers and identifies points of relative minima and
	 * maxima.
	 * <p>
	 * NIST: {@code minmaxs()}. The midpoint of flat plateaus and valleys is
	 * selected when they are detected. The output arrays must be pre-allocated
	 * by the caller with at least {@code num - 2} entries (every intermediate
	 * point can potentially be a min or max). If fewer than 3 items are given,
	 * no min/max is possible and both {@code oMinMaxAlloc} and
	 * {@code oMinMaxNumber} are set to -1.
	 *
	 * @param oMinMaxValue  output: value of the item at each minimum or maximum
	 * @param oMinMaxType   output: identifies a minimum as -1 and a maximum as 1
	 * @param oMinMaxIndex  output: index of each extremum's position in the list
	 * @param oMinMaxAlloc  output: number of allocated minima and/or maxima
	 *                      ({@code num - 2})
	 * @param oMinMaxNumber output: number of detected minima and/or maxima
	 * @param items         list of integers to be analyzed
	 * @param num           number of items in the list
	 * @return zero ({@link ILfs#FALSE}) on successful completion (negative would
	 *         indicate a system error)
	 */
	public int minMaxs(AtomicIntegerArray oMinMaxValue, AtomicIntegerArray oMinMaxType, AtomicIntegerArray oMinMaxIndex,
			AtomicInteger oMinMaxAlloc, AtomicInteger oMinMaxNumber, AtomicIntegerArray items, final int num) {
		int i;
		int diff;
		int state;
		int start;
		int loc;
		int nMinMaxAlloc;
		int nMinMaxNumber;

		/* Determine maximum length for allocation of buffers. */
		/* If there are fewer than 3 items ... */
		if (num < 3) {
			/* Then no min/max is possible, so set allocated length */
			/* to 0 and return. */
			oMinMaxAlloc.set(-1);
			oMinMaxNumber.set(-1);
			return (ILfs.FALSE);
		}

		/* Otherwise, set allocation length to number of items - 2 */
		/* (one for the first item in the list, and on for the last). */
		/* Every other intermediate point can potentially represent a */
		/* min or max. */
		nMinMaxAlloc = num - 2;
		/* Allocate the buffers. */
		// Allocate before calling the funtion

		/* Initialize number of min/max to 0. */
		nMinMaxNumber = 0;

		/* Start witht the first item in the list. */
		i = 0;

		/* Get starting state between first pair of items. */
		diff = items.get(1) - items.get(0);
		if (diff > 0) {
			state = 1;
		} else if (diff < 0) {
			state = -1;
		} else {
			state = 0;
		}

		/* Set start location to first item in list. */
		start = 0;

		/* Bump to next item in list. */
		i++;

		/* While not at the last item in list. */
		while (i < num - 1) {
			/* Compute difference between next pair of items. */
			diff = items.get(i + 1) - items.get(i);
			/* If items are increasing ... */
			if (diff > 0) {
				/* If previously increasing ... */
				if (state == 1) {
					/* Reset start to current location. */
					start = i;
				}
				/* If previously decreasing ... */
				else if (state == -1) {
					/* Then we have incurred a minima ... */
					/* Compute midpoint of minima. */
					loc = (start + i) / 2;
					/* Store value at minima midpoint. */
					oMinMaxValue.set(nMinMaxNumber, items.get(loc));
					/* Store type code for minima. */
					oMinMaxType.set(nMinMaxNumber, -1);
					/* Store location of minima midpoint. */
					oMinMaxIndex.set(nMinMaxNumber++, loc);
					/* Change state to increasing. */
					state = 1;
					/* Reset start location. */
					start = i;
				}
				/* If previously level (this state only can occur at the */
				/* beginning of the list of items) ... */
				else {
					/* If more than one level state in a row ... */
					if (i - start > 1) {
						/* Then consider a minima ... */
						/* Compute midpoint of minima. */
						loc = (start + i) / 2;
						/* Store value at minima midpoint. */
						oMinMaxValue.set(nMinMaxNumber, items.get(loc));
						/* Store type code for minima. */
						oMinMaxType.set(nMinMaxNumber, -1);
						/* Store location of minima midpoint. */
						oMinMaxIndex.set(nMinMaxNumber++, loc);
						/* Change state to increasing. */
						state = 1;
						/* Reset start location. */
						start = i;
					}
					/* Otherwise, ignore single level state. */
					else {
						/* Change state to increasing. */
						state = 1;
						/* Reset start location. */
						start = i;
					}
				}
			}
			/* If items are decreasing ... */
			else if (diff < 0) {
				/* If previously decreasing ... */
				if (state == -1) {
					/* Reset start to current location. */
					start = i;
				}
				/* If previously increasing ... */
				else if (state == 1) {
					/* Then we have incurred a maxima ... */
					/* Compute midpoint of maxima. */
					loc = (start + i) / 2;
					/* Store value at maxima midpoint. */
					oMinMaxValue.set(nMinMaxNumber, items.get(loc));
					/* Store type code for maxima. */
					oMinMaxType.set(nMinMaxNumber, 1);
					/* Store location of maxima midpoint. */
					oMinMaxIndex.set(nMinMaxNumber++, loc);
					/* Change state to decreasing. */
					state = -1;
					/* Reset start location. */
					start = i;
				}
				/* If previously level (this state only can occur at the */
				/* beginning of the list of items) ... */
				else {
					/* If more than one level state in a row ... */
					if (i - start > 1) {
						/* Then consider a maxima ... */
						/* Compute midpoint of maxima. */
						loc = (start + i) / 2;
						/* Store value at maxima midpoint. */
						oMinMaxValue.set(nMinMaxNumber, items.get(loc));
						/* Store type code for maxima. */
						oMinMaxType.set(nMinMaxNumber, 1);
						/* Store location of maxima midpoint. */
						oMinMaxIndex.set(nMinMaxNumber++, loc);
						/* Change state to decreasing. */
						state = -1;
						/* Reset start location. */
						start = i;
					}
					/* Otherwise, ignore single level state. */
					else {
						/* Change state to decreasing. */
						state = -1;
						/* Reset start location. */
						start = i;
					}
				}
			}
			/* Otherwise, items are level, so continue to next item pair. */
			/* Advance to next item pair in list. */
			i++;
		}

		/* Set results to output pointers. */
		oMinMaxAlloc.set(nMinMaxAlloc);
		oMinMaxNumber.set(nMinMaxNumber);

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Takes two coordinate points and computes the Euclidean distance between
	 * them.
	 * <p>
	 * NIST: {@code distance()}.
	 *
	 * @param x1 x-coord of first point
	 * @param y1 y-coord of first point
	 * @param x2 x-coord of second point
	 * @param y2 y-coord of second point
	 * @return the computed Euclidean distance (in pixels)
	 */
	public double distance(final int x1, final int y1, final int x2, final int y2) {
		double dx;
		double dy;
		double dist;

		/* Compute delta x between points. */
		dx = (x1 - x2);
		/* Compute delta y between points. */
		dy = (y1 - y2);
		/* Compute the squared distance between points. */
		dist = (dx * dx) + (dy * dy);
		/* Take square root of squared distance. */
		dist = Math.sqrt(dist);

		/* Return the squared distance. */
		return (dist);
	}

	/**
	 * Takes two coordinate points and computes the squared distance between
	 * them.
	 * <p>
	 * NIST: {@code squared_distance()}.
	 *
	 * @param x1 x-coord of first point
	 * @param y1 y-coord of first point
	 * @param x2 x-coord of second point
	 * @param y2 y-coord of second point
	 * @return the computed squared distance (in pixels squared)
	 */
	public double squaredDistance(final int x1, final int y1, final int x2, final int y2) {
		double dx;
		double dy;
		double dist;

		/* Compute delta x between points. */
		dx = (x1 - x2);
		/* Compute delta y between points. */
		dy = (y1 - y2);
		/* Compute the squared distance between points. */
		dist = (dx * dx) + (dy * dy);

		/* Return the squared distance. */
		return (dist);
	}

	/**
	 * Determines whether a specified value is stored in a list of integers and
	 * returns its location if found.
	 * <p>
	 * NIST: {@code in_int_list()}.
	 *
	 * @param item value to search for in list
	 * @param list list of integers to be searched
	 * @param len  number of integers in search list
	 * @return zero or greater: first location found equal to the search value;
	 *         negative ({@link ILfs#UNDEFINED}, -1): search value not found in
	 *         the list
	 */
	public int getValueLocationInList(final int item, AtomicIntegerArray list, final int len) {
		int i;

		/* Foreach item in list ... */
		for (i = 0; i < len; i++) {
			/* If search item found in list ... */
			if (list.get(i) == item) {
				/* Return the location in list where found. */
				return (i);
			}
		}

		/* If we get here, then search item not found in list, */
		/* so return -1 ==> NOT FOUND/UNDEFINED */
		return (ILfs.UNDEFINED);
	}

	/**
	 * Takes a position index into an integer list and removes the value from the
	 * list, collapsing the resulting list.
	 * <p>
	 * NIST: {@code remove_from_int_list()}. The remaining integers are slid up
	 * over the removed position. NOTE: decrementing the number of integers
	 * remaining in the list is the responsibility of the caller.
	 *
	 * @param index position of value to be removed from list
	 * @param list  input/output: list of integers; on return the specified
	 *              integer has been removed
	 * @param num   number of integers in the list
	 * @return zero ({@link ILfs#FALSE}) on successful completion; negative (-370)
	 *         on system error (index out of range)
	 */
	public int removeValueFromLocationInList(final int index, AtomicIntegerArray list, final int num) {
		int fr;
		int to;

		/* Make sure the requested index is within range. */
		if ((index < 0) && (index >= num)) {
			logger.error("ERROR : remove_from_int_list : index out of range");
			return (-370);
		}

		/* Slide the remaining list of integers up over top of the */
		/* position of the integer being removed. */
		for (to = index, fr = index + 1; fr < num; to++, fr++) {
			list.set(to, list.get(fr));
		}

		/* NOTE: Decrementing the number of integers remaining in the list is */
		/* the responsibility of the caller! */

		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Takes a double value and a list of doubles and determines where in the
	 * list the value may be inserted, preserving the increasing sorted order of
	 * the list.
	 * <p>
	 * NIST: {@code find_incr_position_dbl()}.
	 *
	 * @param val  value to be inserted into the list
	 * @param list list of doubles in increasing sorted order
	 * @param num  number of values in the list
	 * @return zero or positive: insertion position in the list ({@code num} if
	 *         the value belongs at the end)
	 */
	public int findIncrementalPositionInDoubleArray(final double val, AtomicReferenceArray<Double> list,
			final int num) {
		int i;

		/* Foreach item in double list ... */
		for (i = 0; i < num; i++) {
			/* If the value is smaller than the current item in list ... */
			if (val < list.get(i)) {
				/* Then we found were to insert the value in the list maintaining */
				/* an increasing sorted order. */
				return (i);
			}

			/* Otherwise, the value is still larger than current item, so */
			/* continue to next item in the list. */
		}

		/* Otherwise, we never found a slot within the list to insert the */
		/* the value, so place at the end of the sorted list. */
		return (i);
	}

	/**
	 * Takes two coordinate points and computes the angle to the line formed by
	 * the two points.
	 * <p>
	 * NIST: {@code angle2line()}. The y delta is inverted so that the angle is
	 * measured in a conventional (y-up) frame; if both deltas are smaller than
	 * {@link ILfs#MIN_SLOPE_DELTA} the angle is 0.
	 *
	 * @param fx x-coord of first point
	 * @param fy y-coord of first point
	 * @param tx x-coord of second point
	 * @param ty y-coord of second point
	 * @return the angle (in radians, range -PI..PI) to the specified line
	 */
	public double angleToLine(final int fx, final int fy, final int tx, final int ty) {
		double dx;
		double dy;
		double theta;

		/* Compute slope of line connecting the 2 specified points. */
		dy = (fy - ty);
		dx = (tx - fx);
		/* If delta's are sufficiently small ... */
		if ((Math.abs(dx) < ILfs.MIN_SLOPE_DELTA) && (Math.abs(dy) < ILfs.MIN_SLOPE_DELTA)) {
			theta = 0.0;
		}
		/* Otherwise, compute angle to the line. */
		else {
			theta = Math.atan2(dy, dx);
		}

		/* Return the compute angle in radians. */
		return (theta);
	}

	/**
	 * Takes two coordinate points and computes the direction (on a full circle)
	 * in which the first point points to the second.
	 * <p>
	 * NIST: {@code line2direction()}. Coordinates are swapped and the order of
	 * points reversed so that direction 0 is vertical and positive direction is
	 * clockwise. The angle is converted to an integer direction on the range
	 * [0..2*noOfPossibleDirs), truncating precision to {@link ILfs#TRUNC_SCALE}
	 * before rounding.
	 *
	 * @param fx               x-coord of first point (pointing from)
	 * @param fy               y-coord of first point (pointing from)
	 * @param tx               x-coord of second point (pointing to)
	 * @param ty               y-coord of second point (pointing to)
	 * @param noOfPossibleDirs number of IMAP directions (in semicircle)
	 * @return the determined direction on a "full" circle
	 */
	public int lineToDirection(final int fx, final int fy, final int tx, final int ty, final int noOfPossibleDirs) {
		double theta;
		double piFactor;
		int iDir;
		int fullNoOfDirs;
		double pi2 = ILfs.M_PI * 2.0;

		/* Compute angle to line connecting the 2 points. */
		/* Coordinates are swapped and order of points reversed to */
		/* account for 0 direction is vertical and positive direction */
		/* is clockwise. */
		theta = angleToLine(ty, tx, fy, fx);

		/* Make sure the angle is positive. */
		theta += pi2;
		theta = getDefs().fMod(theta, pi2);
		/* Convert from radians to integer direction on range [0..(ndirsX2)]. */
		/* Multiply radians by units/radian ((ndirsX2)/(2PI)), and you get */
		/* angle in integer units. */
		/* Compute number of directions on full circle. */
		fullNoOfDirs = noOfPossibleDirs << 1;
		/* Compute the radians to integer direction conversion factor. */
		piFactor = fullNoOfDirs / pi2;
		/* Convert radian angle to integer direction on full circle. */
		theta *= piFactor;
		/* Need to truncate precision so that answers are consistent */
		/* on different computer architectures when rounding doubles. */
		theta = getDefs().truncDoublePrecision(theta, ILfs.TRUNC_SCALE);
		iDir = getDefs().sRound(theta);
		/* Make sure on range [0..(ndirsX2)]. */
		iDir %= fullNoOfDirs;

		/* Return the integer direction. */
		return (iDir);
	}

	/**
	 * Takes two integer IMAP directions and determines the closest distance
	 * between them, accounting for wrap-around either at the beginning or ending
	 * of the range of directions.
	 * <p>
	 * NIST: {@code closest_dir_dist()}.
	 *
	 * @param dir1             integer value of the first direction
	 * @param dir2             integer value of the second direction
	 * @param noOfPossibleDirs the number of possible directions
	 * @return non-negative: distance between the two directions;
	 *         {@link ILfs#INVALID_DIR} (-1) if either direction is invalid
	 */
	public int closestDirDistance(final int dir1, final int dir2, final int noOfPossibleDirs) {
		int distance1;
		int distance2;
		int distance;

		/* Initialize distance to -1 = INVALID. */
		distance = ILfs.INVALID_DIR;

		/* Measure shortest distance between to directions. */
		/* If both neighbors are VALID ... */
		if ((dir1 >= 0) && (dir2 >= 0)) {
			/* Compute inner and outer distances to account for distances */
			/* that wrap around the end of the range of directions, which */
			/* may in fact be closer. */
			distance1 = Math.abs(dir2 - dir1);
			distance2 = noOfPossibleDirs - distance1;
			distance = Math.min(distance1, distance2);
		}
		/* Otherwise one or both directions are INVALID, so ignore */
		/* and return INVALID. */

		/* Return determined closest distance. */
		return (distance);
	}
}