package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;

import org.mosip.nist.nfiq1.Defs;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.ILine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Line extends MindTct implements ILine {
	private static final Logger logger = LoggerFactory.getLogger(Line.class);
	private static Line instance;

	private Line() {
		super();
	}

	public static synchronized Line getInstance() {
		if (instance == null) {
			instance = new Line();
		}
		return instance;
	}

	public Defs getDefs() {
		return Defs.getInstance();
	}

	public Free getFree() {
		return Free.getInstance();
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: linePoints - Returns the contiguous coordinates of a line connecting
	 * #cat: 2 specified points. Input: x1 - x-coord of first point y1 - y-coord of
	 * first point x2 - x-coord of second point y2 - y-coord of second point Output:
	 * xList - x-coords along line trajectory yList - y-coords along line trajectory
	 * oNoOfPointsOnLine - number of points along line trajectory Return Code: Zero
	 * - successful completion Negative - system error
	 **************************************************************************/
	public int linePoints(int[] xList, int[] yList, AtomicInteger oNoOfPointsOnLine, final int x1, final int y1,
			final int x2, final int y2) {
		int dx;
		int dy;
		int adx;
		int ady;
		int xIncr;
		int yIncr;
		int i;
		int inx;
		int iny;
		int intx;
		int inty;
		double xFactor;
		double yFactor;
		double rx;
		double ry;
		int ix;
		int iy;

		/* Compute maximum number of points needed to hold line segment. */
		// init xList and yList before calling
		int asize = Math.max(Math.abs(x2 - x1) + 2, Math.abs(y2 - y1) + 2);
		// initialize at the source call xList, yList with asize

		/* Compute delta x and y. */
		dx = x2 - x1;
		dy = y2 - y1;

		/* Set x and y increments. */
		if (dx >= 0) {
			xIncr = 1;
		} else {
			xIncr = -1;
		}

		if (dy >= 0) {
			yIncr = 1;
		} else {
			yIncr = -1;
		}

		/* Compute |DX| and |DY|. */
		adx = Math.abs(dx);
		ady = Math.abs(dy);

		/* Set x-orientation. */
		if (adx > ady) {
			inx = 1;
		} else {
			inx = 0;
		}

		/* Set y-orientation. */
		if (ady > adx) {
			iny = 1;
		} else {
			iny = 0;
		}

		/* CASE 1: |DX| > |DY| */
		/* Increment in X by +-1 */
		/* in Y by +-|DY|/|DX| */
		/* inx = 1 */
		/* iny = 0 */
		/* intx = 1 (inx) */
		/* inty = 0 (iny) */
		/* CASE 2: |DX| < |DY| */
		/* Increment in Y by +-1 */
		/* in X by +-|DX|/|DY| */
		/* inx = 0 */
		/* iny = 1 */
		/* intx = 0 (inx) */
		/* inty = 1 (iny) */
		/* CASE 3: |DX| == |DY| */
		/* inx = 0 */
		/* iny = 0 */
		/* intx = 1 */
		/* inty = 1 */
		intx = 1 - iny;
		inty = 1 - inx;

		/* DX */
		/* xFactor = (inx * +-1) + ( iny * ------------ ) */
		/* max(1, |DY|) */
		/*                                                     */
		xFactor = (inx * xIncr) + (iny * ((double) dx / Math.max(1, ady)));

		/* DY */
		/* yFactor = (iny * +-1) + ( inx * ------------ ) */
		/* max(1, |DX|) */
		/*                                                     */
		yFactor = (iny * yIncr) + (inx * ((double) dy / Math.max(1, adx)));

		/* Initialize integer coordinates. */
		ix = x1;
		iy = y1;
		/* Set floating point coordinates. */
		rx = x1;
		ry = y1;

		/* Initialize to first point in line segment. */
		i = 0;

		/* Assign first point into coordinate list. */
		xList[i] = x1;
		yList[i++] = y1;

		while ((ix != x2) || (iy != y2)) {
			if (i >= asize) {
				logger.error("ERROR : linePoints : coord list overflow\n");
				getFree().free(xList);
				getFree().free(yList);
				return (ILfs.ERROR_CODE_412);
			}

			rx += xFactor;
			ry += yFactor;

			/* Need to truncate precision so that answers are consistent */
			/* on different computer architectures when truncating doubles. */
			rx = getDefs().truncDoublePrecision(rx, ILfs.TRUNC_SCALE);
			ry = getDefs().truncDoublePrecision(ry, ILfs.TRUNC_SCALE);

			/* Compute new x and y-pixel coords in floating point and */
			/* then round to the nearest integer. */
			ix = (intx * (ix + xIncr)) + (iny * (int) (rx + 0.5));
			iy = (inty * (iy + yIncr)) + (inx * (int) (ry + 0.5));

			/* Assign first point into coordinate list. */
			xList[i] = ix;
			yList[i++] = iy;
		}

		/* Set output pointers. */
		oNoOfPointsOnLine.set(i);

		/* Return normally. */
		return (ILfs.FALSE);
	}
}