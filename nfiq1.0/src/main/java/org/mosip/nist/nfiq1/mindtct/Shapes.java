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

public class Shapes extends MindTct implements IShapes {
	private static final Logger logger = LoggerFactory.getLogger(Shapes.class);
	private static Shapes instance;

	private Shapes() {
		super();
	}

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

	public Free getFree() {
		return Free.getInstance();
	}

	public Contour getContour() {
		return Contour.getInstance();
	}

	public LfsUtil getLfsUtil() {
		return LfsUtil.getInstance();
	}

	public Sort getSort() {
		return Sort.getInstance();
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: Constructor allocShape - Allocates and initializes a shape structure
	 * given the #cat: the X and Y limits of the shape. Input: xMin - left-most
	 * x-coord in shape yMin - top-most y-coord in shape xMax - right-most x-coord
	 * in shape yMax - bottom-most y-coord in shape Output: ret - Zero - Shape
	 * successfully allocated and initialized - Negative - System error Return Code:
	 * shape - pointer to the allocated & initialized shape structure
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: freeShape - Deallocates a shape structure and all its allocated #cat:
	 * attributes. Input: shape - pointer to the shape structure to be deallocated
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: dumpShape - Takes an initialized shape structure and dumps its contents
	 * #cat: as formatted text to the specified open file pointer. Input: shape -
	 * shape structure to be dumped Output: file - open file pointer to be written
	 * to
	 **************************************************************************/
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

			logger.info("Successfully wrote Shapes to the file.");
		} catch (IOException e) {
			logger.error("An error occurred.", e);
		}
	}

	/*************************************************************************
	 **************************************************************************
	 * #cat: shapeFromContour - Converts a contour list that has been determined
	 * #cat: to form a complete loop into a shape representation where #cat: the
	 * contour points on each contiguous scanline of the shape #cat: are stored in
	 * left-to-right order. Input: oContourX - x-coord list for loop's contour
	 * points oContourY - y-coord list for loop's contour points noOfContour -
	 * number of points in contour Output: shape - points to the resulting shape
	 * structure Return Code: Zero - shape successfully derived Negative - system
	 * error
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: sortRowLeftToRightOnX - Takes a row structure and sorts its points
	 * left-to- #cat: right on X. Input: row - row structure to be sorted output:
	 * row - row structure with points in sorted order
	 **************************************************************************/
	public void sortRowLeftToRightOnX(Rows row) {
		/* Conduct a simple increasing bubble sort on the x-coords */
		/* in the given row. A bubble sort is satisfactory as the */
		/* number of points will be relatively small. */
		getSort().bubbleSortIntArrayIncremental(row.getXs(), row.getNoOfPts());
	}
}