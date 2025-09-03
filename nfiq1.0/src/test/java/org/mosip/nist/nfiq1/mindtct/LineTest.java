package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import org.mosip.nist.nfiq1.common.ILfs;

/**
 * Test class for Line, covering various geometric and edge scenarios.
 */
@ExtendWith(MockitoExtension.class)
class LineTest {

    private Line line;

    /**
     * Initializes the Line singleton instance before each test.
     */
    @BeforeEach
    void setUp() {
        line = Line.getInstance();
    }

    /**
     * Verifies that getInstance returns the same singleton instance.
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Line instance1 = Line.getInstance();
        Line instance2 = Line.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Validates that getDefs returns a non-null value.
     */
    @Test
    void getDefsReturnsNotNull() {
        Assertions.assertNotNull(line.getDefs());
    }

    /**
     * Validates output points for a horizontal line.
     */
    @Test
    void linePointsWithHorizontalLineReturnsPoints() {
        int[] oxList = new int[10];
        int[] oyList = new int[10];
        AtomicInteger onum = new AtomicInteger();
        int result = line.linePoints(oxList, oyList, onum, 0, 0, 5, 0);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(onum.get() > 0);
        Assertions.assertEquals(0, oxList[0]);
        Assertions.assertEquals(5, oxList[onum.get() - 1]);
    }

    /**
     * Validates output points for a vertical line.
     */
    @Test
    void linePointsWithVerticalLineReturnsPoints() {
        int[] oxList = new int[10];
        int[] oyList = new int[10];
        AtomicInteger onum = new AtomicInteger();
        int result = line.linePoints(oxList, oyList, onum, 0, 0, 0, 5);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(onum.get() > 0);
        Assertions.assertEquals(0, oyList[0]);
        Assertions.assertEquals(5, oyList[onum.get() - 1]);
    }

    /**
     * Validates output points for a diagonal line.
     */
    @Test
    void linePointsWithDiagonalLineReturnsPoints() {
        int[] oxList = new int[20];
        int[] oyList = new int[20];
        AtomicInteger onum = new AtomicInteger();
        int result = line.linePoints(oxList, oyList, onum, 0, 0, 3, 3);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(onum.get() > 0);
    }

    /**
     * Validates output for a line where start and end points are the same.
     */
    @Test
    void linePointsWithSamePointReturnsOnePoint() {
        int[] oxList = new int[10];
        int[] oyList = new int[10];
        AtomicInteger onum = new AtomicInteger();
        int result = line.linePoints(oxList, oyList, onum, 5, 5, 5, 5);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertEquals(1, onum.get());
        Assertions.assertEquals(5, oxList[0]);
        Assertions.assertEquals(5, oyList[0]);
    }

    /**
     * Validates handling of a long line (greater than capacity).
     */
    @Test
    void linePointsWithLongLineHandlesCorrectly() {
        int[] oxList = new int[50];
        int[] oyList = new int[50];
        AtomicInteger onum = new AtomicInteger();
        int result = line.linePoints(oxList, oyList, onum, 0, 0, 20, 0);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(onum.get() > 0);
    }

    /**
     * Tests line with negative coordinates.
     */
    @Test
    void linePointsWithNegativeCoordinates() {
        int[] oxList = new int[10];
        int[] oyList = new int[10];
        AtomicInteger onum = new AtomicInteger();
        int result = line.linePoints(oxList, oyList, onum, -3, -3, 3, 3);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(onum.get() > 0);
        Assertions.assertEquals(-3, oxList[0]);
        Assertions.assertEquals(-3, oyList[0]);
    }

    /**
     * Tests line going from right to left (negative dx).
     */
    @Test
    void linePointsWithNegativeDx() {
        int[] oxList = new int[10];
        int[] oyList = new int[10];
        AtomicInteger onum = new AtomicInteger();
        int result = line.linePoints(oxList, oyList, onum, 5, 0, 0, 0);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(onum.get() > 0);
        Assertions.assertEquals(5, oxList[0]);
        Assertions.assertEquals(0, oxList[onum.get() - 1]);
    }

    /**
     * Tests line going from bottom to top (negative dy).
     */
    @Test
    void linePointsWithNegativeDy() {
        int[] oxList = new int[10];
        int[] oyList = new int[10];
        AtomicInteger onum = new AtomicInteger();
        int result = line.linePoints(oxList, oyList, onum, 0, 5, 0, 0);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(onum.get() > 0);
        Assertions.assertEquals(5, oyList[0]);
        Assertions.assertEquals(0, oyList[onum.get() - 1]);
    }

    /**
     * Tests steep line where |dy| > |dx|.
     */
    @Test
    void linePointsWithSteepLine() {
        int[] oxList = new int[15];
        int[] oyList = new int[15];
        AtomicInteger onum = new AtomicInteger();
        int result = line.linePoints(oxList, oyList, onum, 0, 0, 2, 10);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(onum.get() > 0);
        Assertions.assertEquals(0, oxList[0]);
        Assertions.assertEquals(0, oyList[0]);
        Assertions.assertEquals(2, oxList[onum.get() - 1]);
        Assertions.assertEquals(10, oyList[onum.get() - 1]);
    }

    /**
     * Tests line with equal absolute dx and dy (45 degree angle).
     */
    @Test
    void linePointsWithEqualDxDy() {
        int[] oxList = new int[10];
        int[] oyList = new int[10];
        AtomicInteger onum = new AtomicInteger();
        int result = line.linePoints(oxList, oyList, onum, 0, 0, 4, 4);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(onum.get() > 0);
        Assertions.assertEquals(0, oxList[0]);
        Assertions.assertEquals(0, oyList[0]);
        Assertions.assertEquals(4, oxList[onum.get() - 1]);
        Assertions.assertEquals(4, oyList[onum.get() - 1]);
    }

    /**
     * Tests line with negative diagonal (both dx and dy negative).
     */
    @Test
    void linePointsWithNegativeDiagonal() {
        int[] oxList = new int[10];
        int[] oyList = new int[10];
        AtomicInteger onum = new AtomicInteger();
        int result = line.linePoints(oxList, oyList, onum, 5, 5, 0, 0);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(onum.get() > 0);
        Assertions.assertEquals(5, oxList[0]);
        Assertions.assertEquals(5, oyList[0]);
        Assertions.assertEquals(0, oxList[onum.get() - 1]);
        Assertions.assertEquals(0, oyList[onum.get() - 1]);
    }

    /**
     * Tests line with mixed signs (positive dx, negative dy).
     */
    @Test
    void linePointsWithMixedSigns() {
        int[] oxList = new int[10];
        int[] oyList = new int[10];
        AtomicInteger onum = new AtomicInteger();
        int result = line.linePoints(oxList, oyList, onum, 0, 5, 5, 0);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(onum.get() > 0);
        Assertions.assertEquals(0, oxList[0]);
        Assertions.assertEquals(5, oyList[0]);
        Assertions.assertEquals(5, oxList[onum.get() - 1]);
        Assertions.assertEquals(0, oyList[onum.get() - 1]);
    }

    /**
     * Tests getFree method returns non-null instance.
     */
    @Test
    void getFreeReturnsNotNull() {
        Assertions.assertNotNull(line.getFree());
    }
}