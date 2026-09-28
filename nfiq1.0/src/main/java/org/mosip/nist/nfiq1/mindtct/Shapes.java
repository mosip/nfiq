package org.mosip.nist.nfiq1.mindtct;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.IShapes;
import org.mosip.nist.nfiq1.common.ILfs.Rows;
import org.mosip.nist.nfiq1.common.ILfs.Shape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shape (scanline region) construction from closed contours for MINDTCT.
 * <p>
 * Port of NIST LFS {@code shape.c}. A {@link Shape} represents a closed region
 * as a list of scanline {@link Rows}, each holding the contour x-coords on that
 * row in left-to-right order. Shapes are derived from loop contours and are
 * used, for example, when filling or measuring small loops/islands during
 * false-minutia removal.
 * <p>
 * Implemented as a lazily created singleton; {@link #getInstance()} is
 * synchronized and the class keeps no mutable state.
 */
public class Shapes extends MindTct implements IShapes {
	/** SLF4J logger for diagnostics and error reporting in this class. */
	private static final Logger logger = LoggerFactory.getLogger(Shapes.class);
	/** Lazily created singleton instance, see {@link #getInstance()}. */
	private static Shapes instance;

	/**
	 * Private constructor; use {@link #getInstance()} to obtain the singleton.
	 */
	private Shapes() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code Shapes} instance
	 */
	public static synchronized Shapes getInstance() {
		if (instance == null) {
			synchronized (Shapes.class) {
				if (instance == null) {
					instance = new Shapes();
				}
			}
		}
		return instance;
	}

	/**
	 * Returns the shared {@link Free} helper used to release structures.
	 *
	 * @return the {@code Free} singleton
	 */
	public Free getFree() {
		return Free.getInstance();
	}

	/**
	 * Returns the shared {@link Contour} helper (contour limits).
	 *
	 * @return the {@code Contour} singleton
	 */
	public Contour getContour() {
		return Contour.getInstance();
	}

	/**
	 * Returns the shared {@link LfsUtil} helper (list searching).
	 *
	 * @return the {@code LfsUtil} singleton
	 */
	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	/**
	 * Returns the shared {@link Sort} helper (bubble sort).
	 *
	 * @return the {@code Sort} singleton
	 */
	public Sort getSort() {
		return Sort.getInstance();
	}

	/**
	 * Allocates and initializes a shape structure given the X and Y limits of
	 * the shape.
	 * <p>
	 * NIST: {@code alloc_shape()}. One row is allocated per scanline from
	 * {@code yMin} to {@code yMax} inclusive, and each row gets capacity for
	 * {@code xMax - xMin + 1} x-coords (the maximum number of contiguous pixels
	 * on a row, which is sufficiently larger than the number of actual contour
	 * points). Every row starts with zero points assigned.
	 *
	 * @param ret  output: set to zero ({@link ILfs#FALSE}) when the shape is
	 *             successfully allocated and initialized (negative would indicate
	 *             a system error)
	 * @param xMin left-most x-coord in shape
	 * @param yMin top-most y-coord in shape
	 * @param xMax right-most x-coord in shape
	 * @param yMax bottom-most y-coord in shape
	 * @return the allocated and initialized shape structure
	 */
	public Shape allocShape(AtomicInteger ret, int xMin, int yMin, int xMax, int yMax) {
		Shape shape = new Shape();

		int allocRows;
		int allocPoints;
		int i;
		int j;
		int y;

		/* Compute allocation parameters. */
		/* First, compute the number of scanlines spanned by the shape. */
		allocRows = yMax - yMin + 1;
		/* Second, compute the "maximum" number of contour points possible */
		/* on a row. Here we are allocating the maximum number of contiguous */
		/* pixels on each row which will be sufficiently larger than the */
		/* number of actual contour points. */
		allocPoints = xMax - xMin + 1;

		/* Allocate the shape structure. */
		/* Allocate the list of row pointers. We now this number will fit */
		/* the shape exactly. */
		shape.setRows(new AtomicReferenceArray<Rows>(allocRows));
		/* Initialize the shape structure's attributes. */
		shape.setYMin(yMin);
		shape.setYMax(yMax);
		/* The number of allocated rows will be exactly the number of */
		/* assigned rows for the shape. */
		shape.setAlloc(allocRows);
		shape.setNRows(allocRows);

		/* Foreach row in the shape... */
		for (i = 0, y = yMin; i < allocRows; i++, y++) {
			/* Allocate a row structure and store it in its respective position */
			/* in the shape structure's list of row pointers. */
			shape.getRows().set(i, new Rows());

			/* Allocate the current rows list of x-coords. */
			shape.getRows().get(i).setXs(new AtomicIntegerArray(allocPoints));

			/* Initialize the current row structure's attributes. */
			shape.getRows().get(i).setY(y);
			shape.getRows().get(i).setAlloc(allocPoints);
			/* There are initially ZERO points assigned to the row. */
			shape.getRows().get(i).setNoOfPts(0);
		}

		/* Return normally. */
		ret.set(ILfs.FALSE);
		return shape;
	}

	/**
	 * Deallocates a shape structure and all its allocated attributes.
	 * <p>
	 * NIST: {@code free_shape()}. Releases each row's x-coord list and row
	 * structure via {@link Free}, then clears the shape's row list reference.
	 *
	 * @param shape the shape structure to be deallocated (modified in place)
	 */
	public void freeShape(Shape shape) {
		int i;
		/* Foreach allocated row in the shape ... */
		for (i = 0; i < shape.getAlloc(); i++) {
			/* Deallocate the current row's list of x-coords. */
			getFree().free(shape.getRows().get(i).getXs());
			/* Deallocate the current row structure. */
			getFree().free(shape.getRows().get(i));
		}

		/* Deallocate the list of row pointers. */
		shape.setRows(null);
		/* Deallocate the shape structure. */
	}

	/**
	 * Takes an initialized shape structure and dumps its contents as formatted
	 * text to the specified file.
	 * <p>
	 * NIST: {@code dump_shape()}. Writes the shape's y-limits and number of
	 * scanlines, then each row's y-coord, point count and points. The file is
	 * (re)created/overwritten; I/O errors are logged rather than thrown.
	 *
	 * @param file  output file to be written to
	 * @param shape shape structure to be dumped
	 */
	public void dumpShape(File file, Shape shape) {
		int i;
		int j;

		try (FileWriter myWriter = new FileWriter(file.getAbsoluteFile())){
			/* Print the shape's y-limits and number of scanlines. */
			myWriter.write(MessageFormat.format("shape:  ymin={0}, ymax={1}, nrows={2}\n", shape.getYMin(), shape.getYMax(),
					shape.getNRows()));

			/* Foreach row in the shape... */
			for (i = 0; i < shape.getNRows(); i++) {
				/* Print the current row's y-coord and number of points on the row. */
				myWriter.write(MessageFormat.format("row {0} :   y={1}, npts={2}\n", i, shape.getRows().get(i).getY(),
						shape.getRows().get(i).getNoOfPts()));
				/* Print each successive point on the current row. */
				for (j = 0; j < shape.getRows().get(i).getNoOfPts(); j++) {
					myWriter.write(MessageFormat.format("pt {0} : {1} {2}\n", j, shape.getRows().get(i).getXs().get(j),
							shape.getRows().get(i).getY()));
				}
			}

			logger.debug("Successfully wrote Shapes to the file.");
		} catch (IOException e) {
			logger.error("An error occurred.", e);
		}
	}

	/**
	 * Converts a contour list that has been determined to form a complete loop
	 * into a shape representation where the contour points on each contiguous
	 * scanline of the shape are stored in left-to-right order.
	 * <p>
	 * NIST: {@code shape_from_contour()}. Points re-encountered on the contour
	 * (e.g. at "pinching" points of complex shapes) are stored only once per
	 * row.
	 *
	 * @param ret         output: zero ({@link ILfs#FALSE}) if the shape was
	 *                    successfully derived; negative on system error (e.g.
	 *                    -260 on row overflow)
	 * @param oContourX   x-coord list for the loop's contour points
	 * @param oContourY   y-coord list for the loop's contour points
	 * @param noOfContour number of points in contour
	 * @return the resulting shape structure (possibly partially filled if
	 *         {@code ret} reports an error)
	 */
	public Shape shapeFromContour(AtomicInteger ret, AtomicIntegerArray oContourX, AtomicIntegerArray oContourY,
			final int noOfContour) {
		Shape shape = null;
		Rows row;
		int i;
		AtomicInteger xmin = new AtomicInteger(0), ymin = new AtomicInteger(0), xmax = new AtomicInteger(0),
				ymax = new AtomicInteger(0);

		/* Find xmin, ymin, xmax, ymax on contour. */
		getContour().contourLimits(xmin, ymin, xmax, ymax, oContourX, oContourY, noOfContour);

		/* Allocate and initialize a shape structure. */
		shape = allocShape(ret, xmin.get(), ymin.get(), xmax.get(), ymax.get());
		if (ret.get() != ILfs.FALSE) {
			/* If system error, then return error code. */
			return shape;
		}

		/* Foreach point on contour ... */
		for (i = 0; i < noOfContour; i++) {
			/* Add point to corresponding row. */
			/* First set a pointer to the current row. We need to subtract */
			/* ymin because the rows are indexed relative to the top-most */
			/* scanline in the shape. */
			row = shape.getRows().get(oContourY.get(i) - ymin.get());

			/* It is possible with complex shapes to reencounter points */
			/* already visited on a contour, especially at "pinching" points */
			/* along the contour. So we need to test to see if a point has */
			/* already been stored in the row. If not in row list already ... */
			if (getLfsUtil().getValueLocationInList(oContourX.get(i), row.getXs(), row.getNoOfPts()) < ILfs.FALSE) {
				/* If row is full ... */
				if (row.getNoOfPts() >= row.getAlloc()) {
					/* This should never happen becuase we have allocated */
					/* based on shape bounding limits. */
					logger.error("ERROR : shape_from_contour : row overflow");
					ret.set(-260);
					return shape;
				}
				/* Assign the x-coord of the current contour point to the row */
				/* and bump the row's point counter. All the contour points */
				/* on the same row share the same y-coord. */
				row.getXs().set(row.getNoOfPts(), oContourX.get(i));
				row.setNoOfPts(row.getNoOfPts() + 1);
			}
			/* Otherwise, point is already stored in row, so ignore. */
		}

		/* Foreach row in the shape. */
		for (i = 0; i < shape.getNRows(); i++) {
			/* Sort row points increasing on their x-coord. */
			sortRowLeftToRightOnX(shape.getRows().get(i));
		}

		/* Assign shape structure to output pointer. */
		/* Return normally. */
		ret.set(ILfs.FALSE);
		return shape;
	}

	/**
	 * Takes a row structure and sorts its points left-to-right on X.
	 * <p>
	 * NIST: {@code sort_row_on_x()}. Uses a simple increasing bubble sort, which
	 * is satisfactory as the number of points will be relatively small.
	 *
	 * @param row input/output: row structure to be sorted; on return its points
	 *            are in increasing x order
	 */
	public void sortRowLeftToRightOnX(Rows row) {
		/* Conduct a simple increasing bubble sort on the x-coords */
		/* in the given row. A bubble sort is satisfactory as the */
		/* number of points will be relatively small. */
		getSort().bubbleSortIntArrayIncremental(row.getXs(), row.getNoOfPts());
	}
}