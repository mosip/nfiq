package org.mosip.nist.nfiq1.mindtct;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.Rows;
import org.mosip.nist.nfiq1.common.ILfs.Shape;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link Shapes} providing comprehensive validation of shape processing
 * functionality for fingerprint analysis operations.
 *
 * <p>This class validates the functionality of shape allocation, deallocation, file output,
 * and contour conversion operations used in NIST's Mindtct fingerprint analysis system
 * for geometric shape processing and manipulation.</p>
 */
class ShapesTest {

    private Shapes shapes;
    private Free mockFree;
    private Contour mockContour;
    private LfsUtil mockLfsUtil;
    private Sort mockSort;

    @TempDir
    File tempDir;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes Shapes spy instance and configures mock dependencies.
     */
    @BeforeEach
    void setUp() {
        shapes = spy(Shapes.getInstance());
        mockFree = mock(Free.class);
        mockContour = mock(Contour.class);
        mockLfsUtil = mock(LfsUtil.class);
        mockSort = mock(Sort.class);

        doReturn(mockFree).when(shapes).getFree();
        doReturn(mockContour).when(shapes).getContour();
        doReturn(mockLfsUtil).when(shapes).getLfsUtil();
        doReturn(mockSort).when(shapes).getSort();

        doNothing().when(mockFree).free(any());
        doNothing().when(mockSort).bubbleSortIntArrayIncremental(any(), anyInt());
    }

    /**
     * Validates singleton pattern implementation with double-checked locking.
     * Ensures proper singleton instance creation and reuse.
     */
    @Test
    void getInstanceSingleton() {
        Shapes instance1 = Shapes.getInstance();
        Shapes instance2 = Shapes.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

    /**
     * Validates dependency getter methods return non-null instances.
     * Tests proper initialization of required dependencies.
     */
    @Test
    void dependencyGetters() {
        assertNotNull(shapes.getFree());
        assertNotNull(shapes.getContour());
        assertNotNull(shapes.getLfsUtil());
        assertNotNull(shapes.getSort());
    }

    /**
     * Validates allocShape method with normal coordinate parameters.
     * Tests shape allocation, row initialization, and point allocation processes.
     */
    @Test
    void allocShapeNormal() {
        AtomicInteger ret = new AtomicInteger();
        int xMin = 10, yMin = 20, xMax = 50, yMax = 80;

        Shape shape = shapes.allocShape(ret, xMin, yMin, xMax, yMax);

        assertEquals(ILfs.FALSE, ret.get());
        assertNotNull(shape);
        assertEquals(yMin, shape.getYMin());
        assertEquals(yMax, shape.getYMax());
        assertEquals(yMax - yMin + 1, shape.getAlloc());
        assertEquals(yMax - yMin + 1, shape.getNRows());

        for (int i = 0; i < shape.getNRows(); i++) {
            Rows row = shape.getRows().get(i);
            assertNotNull(row);
            assertEquals(yMin + i, row.getY());
            assertEquals(xMax - xMin + 1, row.getAlloc());
            assertEquals(0, row.getNoOfPts());
            assertNotNull(row.getXs());
        }
    }

    /**
     * Validates allocShape method with minimal size dimensions.
     * Tests shape allocation for smallest possible 1x1 shape.
     */
    @Test
    void allocShapeMinimalSize() {
        AtomicInteger ret = new AtomicInteger();

        Shape shape = shapes.allocShape(ret, 0, 0, 0, 0);

        assertEquals(ILfs.FALSE, ret.get());
        assertNotNull(shape);
        assertEquals(1, shape.getNRows());
        assertEquals(1, shape.getAlloc());
        assertEquals(0, shape.getYMin());
        assertEquals(0, shape.getYMax());
    }

    /**
     * Validates allocShape method with large coordinate values.
     * Tests shape allocation for shapes with high coordinate ranges.
     */
    @Test
    void allocShapeLargeCoordinates() {
        AtomicInteger ret = new AtomicInteger();
        int xMin = 100, yMin = 200, xMax = 150, yMax = 250;

        Shape shape = shapes.allocShape(ret, xMin, yMin, xMax, yMax);

        assertEquals(ILfs.FALSE, ret.get());
        assertNotNull(shape);
        assertEquals(51, shape.getNRows());
        assertEquals(51, shape.getAlloc());

        assertEquals(yMin, shape.getRows().get(0).getY());
        assertEquals(yMax, shape.getRows().get(shape.getNRows() - 1).getY());
    }

    /**
     * Validates freeShape method deallocation process.
     * Tests memory cleanup including row deallocation and nullification.
     */
    @Test
    void freeShapeDeallocation() {
        AtomicInteger ret = new AtomicInteger();
        Shape shape = shapes.allocShape(ret, 10, 20, 30, 40);
        int allocatedRows = shape.getAlloc();

        shapes.freeShape(shape);

        verify(mockFree, times(allocatedRows * 2)).free(any());

        assertNull(shape.getRows());
    }

    /**
     * Validates freeShape method with empty shape having zero rows.
     * Tests proper handling of shapes with no allocated memory.
     */
    @Test
    void freeShapeEmpty() {
        Shape shape = new Shape();
        shape.setAlloc(0);

        shapes.freeShape(shape);

        verify(mockFree, never()).free(any());
        assertNull(shape.getRows());
    }

    /**
     * Validates dumpShape method with successful file writing operation.
     * Tests shape data formatting and file output generation.
     */
    @Test
    void dumpShapeSuccess() throws IOException {
        File outputFile = new File(tempDir, "test_shape.txt");

        AtomicInteger ret = new AtomicInteger();
        Shape shape = shapes.allocShape(ret, 0, 0, 2, 2);

        shape.getRows().get(0).getXs().set(0, 5);
        shape.getRows().get(0).getXs().set(1, 10);
        shape.getRows().get(0).setNoOfPts(2);

        shape.getRows().get(1).getXs().set(0, 15);
        shape.getRows().get(1).setNoOfPts(1);

        shapes.dumpShape(outputFile, shape);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("shape:  ymin=0, ymax=2, nrows=3"));
        assertTrue(content.contains("row 0 :   y=0, npts=2"));
        assertTrue(content.contains("pt 0 : 5 0"));
        assertTrue(content.contains("pt 1 : 10 0"));
        assertTrue(content.contains("row 1 :   y=1, npts=1"));
        assertTrue(content.contains("pt 0 : 15 1"));
    }

    /**
     * Validates dumpShape method with IOException handling.
     * Tests exception handling during file writing operations.
     */
    @Test
    void dumpShapeIOException() {
        File invalidFile = new File("/invalid/path/cannot/write/here.txt");

        AtomicInteger ret = new AtomicInteger();
        Shape shape = shapes.allocShape(ret, 0, 0, 1, 1);

        assertDoesNotThrow(() -> shapes.dumpShape(invalidFile, shape));
    }

    /**
     * Validates shapeFromContour method with normal contour data.
     * Tests contour processing, point addition, duplicate checking, and sorting operations.
     */
    @Test
    void shapeFromContourNormal() {
        AtomicInteger ret = new AtomicInteger();
        AtomicIntegerArray contourX = new AtomicIntegerArray(new int[]{10, 20, 30, 20, 10});
        AtomicIntegerArray contourY = new AtomicIntegerArray(new int[]{5, 5, 10, 15, 15});
        int noOfContour = 5;

        doAnswer(invocation -> {
            AtomicInteger x1 = invocation.getArgument(0);
            AtomicInteger y1 = invocation.getArgument(1);
            AtomicInteger x2 = invocation.getArgument(2);
            AtomicInteger y2 = invocation.getArgument(3);
            x1.set(10); y1.set(5); x2.set(30); y2.set(15);
            return null;
        }).when(mockContour).contourLimits(any(), any(), any(), any(), any(), any(), anyInt());

        when(mockLfsUtil.getValueLocationInList(anyInt(), any(), anyInt())).thenReturn(-1);

        Shape shape = shapes.shapeFromContour(ret, contourX, contourY, noOfContour);

        assertEquals(ILfs.FALSE, ret.get());
        assertNotNull(shape);

        verify(mockContour).contourLimits(any(), any(), any(), any(), eq(contourX), eq(contourY), eq(noOfContour));

        verify(mockSort, times(shape.getNRows())).bubbleSortIntArrayIncremental(any(), anyInt());
    }

    /**
     * Validates shapeFromContour method when allocation fails.
     * Tests error handling during shape allocation process.
     */
    @Test
    void shapeFromContourAllocationError() {
        Shapes spyShapes = spy(Shapes.getInstance());
        doReturn(mockContour).when(spyShapes).getContour();

        doAnswer(invocation -> {
            AtomicInteger x1 = invocation.getArgument(0);
            AtomicInteger y1 = invocation.getArgument(1);
            AtomicInteger x2 = invocation.getArgument(2);
            AtomicInteger y2 = invocation.getArgument(3);
            x1.set(0); y1.set(0); x2.set(10); y2.set(10);
            return null;
        }).when(mockContour).contourLimits(any(), any(), any(), any(), any(), any(), anyInt());

        doAnswer(invocation -> {
            AtomicInteger ret = invocation.getArgument(0);
            ret.set(-100);
            return new Shape();
        }).when(spyShapes).allocShape(any(), anyInt(), anyInt(), anyInt(), anyInt());

        AtomicInteger ret = new AtomicInteger();
        AtomicIntegerArray contourX = new AtomicIntegerArray(new int[]{5});
        AtomicIntegerArray contourY = new AtomicIntegerArray(new int[]{5});

        Shape shape = spyShapes.shapeFromContour(ret, contourX, contourY, 1);

        assertEquals(-100, ret.get());
        assertNotNull(shape);
    }

    /**
     * Validates shapeFromContour method with duplicate point detection.
     * Tests proper handling and skipping of duplicate contour points.
     */
    @Test
    void shapeFromContourDuplicatePoints() {
        AtomicInteger ret = new AtomicInteger();
        AtomicIntegerArray contourX = new AtomicIntegerArray(new int[]{10, 10, 20});
        AtomicIntegerArray contourY = new AtomicIntegerArray(new int[]{5, 5, 5});
        int noOfContour = 3;

        doAnswer(invocation -> {
            AtomicInteger x1 = invocation.getArgument(0);
            AtomicInteger y1 = invocation.getArgument(1);
            AtomicInteger x2 = invocation.getArgument(2);
            AtomicInteger y2 = invocation.getArgument(3);
            x1.set(10); y1.set(5); x2.set(20); y2.set(5);
            return null;
        }).when(mockContour).contourLimits(any(), any(), any(), any(), any(), any(), anyInt());

        when(mockLfsUtil.getValueLocationInList(eq(10), any(), anyInt()))
                .thenReturn(-1)
                .thenReturn(0);
        when(mockLfsUtil.getValueLocationInList(eq(20), any(), anyInt())).thenReturn(-1);

        Shape shape = shapes.shapeFromContour(ret, contourX, contourY, noOfContour);

        assertEquals(ILfs.FALSE, ret.get());
        assertNotNull(shape);

        verify(mockLfsUtil, times(3)).getValueLocationInList(anyInt(), any(), anyInt());
    }

    /**
     * Validates shapeFromContour method when row overflow occurs.
     * Tests error handling when row capacity is exceeded during processing.
     */
    @Test
    void shapeFromContourRowOverflow() {
        AtomicInteger ret = new AtomicInteger();
        AtomicIntegerArray contourX = new AtomicIntegerArray(new int[]{10});
        AtomicIntegerArray contourY = new AtomicIntegerArray(new int[]{5});
        int noOfContour = 1;

        doAnswer(invocation -> {
            AtomicInteger x1 = invocation.getArgument(0);
            AtomicInteger y1 = invocation.getArgument(1);
            AtomicInteger x2 = invocation.getArgument(2);
            AtomicInteger y2 = invocation.getArgument(3);
            x1.set(10); y1.set(5); x2.set(10); y2.set(5);
            return null;
        }).when(mockContour).contourLimits(any(), any(), any(), any(), any(), any(), anyInt());

        Shapes spyShapes = spy(Shapes.getInstance());
        doReturn(mockContour).when(spyShapes).getContour();
        doReturn(mockLfsUtil).when(spyShapes).getLfsUtil();
        doReturn(mockSort).when(spyShapes).getSort();

        doAnswer(invocation -> {
            AtomicInteger retVal = invocation.getArgument(0);
            Shape shape = new Shape();
            shape.setYMin(5);
            shape.setYMax(5);
            shape.setAlloc(1);
            shape.setNRows(1);

            Rows row = new Rows();
            row.setY(5);
            row.setAlloc(0);
            row.setNoOfPts(0);
            row.setXs(new AtomicIntegerArray(0));

            AtomicReferenceArray<Rows> rows = new AtomicReferenceArray<>(1);
            rows.set(0, row);
            shape.setRows(rows);

            retVal.set(ILfs.FALSE);
            return shape;
        }).when(spyShapes).allocShape(any(), anyInt(), anyInt(), anyInt(), anyInt());

        when(mockLfsUtil.getValueLocationInList(anyInt(), any(), anyInt())).thenReturn(-1);

        Shape shape = spyShapes.shapeFromContour(ret, contourX, contourY, noOfContour);

        assertEquals(-260, ret.get());
        assertNotNull(shape);
    }

    /**
     * Validates sortRowLeftToRightOnX method with bubble sort delegation.
     * Tests row sorting functionality using external sort utility.
     */
    @Test
    void sortRowLeftToRightOnX() {
        Rows row = new Rows();
        row.setNoOfPts(3);
        row.setXs(new AtomicIntegerArray(new int[]{30, 10, 20}));

        shapes.sortRowLeftToRightOnX(row);

        verify(mockSort).bubbleSortIntArrayIncremental(eq(row.getXs()), eq(3));
    }

    /**
     * Validates sortRowLeftToRightOnX method with empty row.
     * Tests sorting behavior when row contains no points.
     */
    @Test
    void sortRowLeftToRightOnXEmptyRow() {
        Rows row = new Rows();
        row.setNoOfPts(0);
        row.setXs(new AtomicIntegerArray(0));

        shapes.sortRowLeftToRightOnX(row);

        verify(mockSort).bubbleSortIntArrayIncremental(eq(row.getXs()), eq(0));
    }
}
