package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.ILfsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LfsUtil extends MindTct implements ILfsUtil {
	private static final Logger logger = LoggerFactory.getLogger(LfsUtil.class);
	private static LfsUtil instance;

	private LfsUtil() {
		super();
	}

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

	public Defs getDefs() {
		return Defs.getInstance();
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: maxValue - Determines the maximum value in the given list of integers.
	 * #cat: NOTE, the list is assumed to be NOT empty! Input: list - non-empty list
	 * of integers to be searched num - number of integers in the list Return Code:
	 * Maximum - maximum value in the list
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: minValue - Determines the minimum value in the given list of integers.
	 * #cat: NOTE, the list is assumed to be NOT empty! Input: list - non-empty list
	 * of integers to be searched num - number of integers in the list Return Code:
	 * Minimum - minimum value in the list
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: minMaxs - Takes a list of integers and identifies points of relative
	 * #cat: minima and maxima. The midpoint of flat plateaus and valleys #cat: are
	 * selected when they are detected. Input: items - list of integers to be
	 * analyzed num - number of items in the list Output: oMinMaxValue - value of
	 * the item at each minima or maxima oMinMaxType - identifies a minima as '-1'
	 * and maxima as '1' oMinMaxIndex - index of item's position in list
	 * oMinMaxAlloc - number of allocated minima and/or maxima oMinMaxNumber -
	 * number of detected minima and/or maxima Return Code: Zero - successful
	 * completion Negative - system error
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: distance - Takes two coordinate points and computes the #cat: Euclidean
	 * distance between the two points. Input: x1 - x-coord of first point y1 -
	 * y-coord of first point x2 - x-coord of second point y2 - y-coord of second
	 * point Return Code: Distance - computed Euclidean distance
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: squaredDistance - Takes two coordinate points and computes the #cat:
	 * squared distance between the two points. Input: x1 - x-coord of first point
	 * y1 - y-coord of first point x2 - x-coord of second point y2 - y-coord of
	 * second point Return Code: Distance - computed squared distance
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: getValueLocationInList - Determines if a specified value is store in a
	 * list of #cat: integers and returns its location if found. Input: item - value
	 * to search for in list list - list of integers to be searched len - number of
	 * integers in search list Return Code: Zero or greater - first location found
	 * equal to search value Negative - search value not found in the list of
	 * integers
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: removeValueFromLocationInList - Takes a position index into an integer
	 * list and #cat: removes the value from the list, collapsing the resulting
	 * #cat: list. Input: index - position of value to be removed from list list -
	 * input list of integers num - number of integers in the list Output: list -
	 * list with specified integer removed num - decremented number of integers in
	 * list Return Code: Zero - successful completion Negative - system error
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: findIncrementalPositionInDoubleArray - Takes a double value and a list
	 * of doubles and #cat: determines where in the list the double may be inserted,
	 * #cat: preserving the increasing sorted order of the list. Input: val - value
	 * to be inserted into the list list - list of double in increasing sorted order
	 * num - number of values in the list Return Code: Zero or Positive - insertion
	 * position in the list
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: angleToLine - Takes two coordinate points and computes the angle #cat:
	 * to the line formed by the two points. Input: fx - x-coord of first point fy -
	 * y-coord of first point tx - x-coord of second point ty - y-coord of second
	 * point Return Code: Angle - angle to the specified line
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: lineToDirection - Takes two coordinate points and computes the #cat:
	 * directon (on a full circle) in which the first points #cat: to the second.
	 * Input: fx - x-coord of first point (pointing from) fy - y-coord of first
	 * point (pointing from) tx - x-coord of second point (pointing to) ty - y-coord
	 * of second point (pointing to) noOfPossibleDirs - number of IMAP directions
	 * (in semicircle) Return Code: Direction - determined direction on a "full"
	 * circle
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: closestDirDistance - Takes to integer IMAP directions and determines
	 * the #cat: closest distance between them accounting for #cat: wrap-around
	 * either at the beginning or ending of #cat: the range of directions. Input:
	 * dir1 - integer value of the first direction dir2 - integer value of the
	 * second direction noOfPossibleDirs - the number of possible directions Return
	 * Code: Non-negative - distance between the 2 directions
	 **************************************************************************/
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