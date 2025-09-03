package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mosip.nist.nfiq1.common.ILfs;

/**
 * Test class for {@link LfsUtil}
 */
class LfsUtilTest {

    private LfsUtil lfsUtil;

    /**
     * Sets up the LfsUtil singleton instance before each test.
     */
    @BeforeEach
    void setUp() {
        lfsUtil = LfsUtil.getInstance();
    }

    /**
     * Verifies that getInstance returns the same singleton instance.
     */
    @Test
    void getInstanceReturnsSameInstance() {
        LfsUtil instance1 = LfsUtil.getInstance();
        LfsUtil instance2 = LfsUtil.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Validates that getDefs returns a non-null value.
     */
    @Test
    void getDefsReturnsNotNull() {
        Assertions.assertNotNull(lfsUtil.getDefs());
    }

    /**
     * Checks that maxValue returns the maximum in a list.
     */
    @Test
    void maxValueReturnsMaximum() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{1, 5, 3, 9, 2});
        int result = lfsUtil.maxValue(list, 5);
        Assertions.assertEquals(9, result);
    }

    /**
     * Checks that minValue returns the minimum in a list.
     */
    @Test
    void minValueReturnsMinimum() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{1, 5, 3, 9, 2});
        int result = lfsUtil.minValue(list, 5);
        Assertions.assertEquals(1, result);
    }

    /**
     * Validates calculation of Euclidean distance.
     */
    @Test
    void distanceCalculatesEuclideanDistance() {
        double result = lfsUtil.distance(0, 0, 3, 4);
        Assertions.assertEquals(5.0, result, 0.001);
    }

    /**
     * Validates calculation of squared distance.
     */
    @Test
    void squaredDistanceCalculatesSquaredDistance() {
        double result = lfsUtil.squaredDistance(0, 0, 3, 4);
        Assertions.assertEquals(25.0, result, 0.001);
    }

    /**
     * Checks the index returned when value exists in list.
     */
    @Test
    void getValueLocationInListFindsValue() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{1, 5, 3, 9, 2});
        int result = lfsUtil.getValueLocationInList(3, list, 5);
        Assertions.assertEquals(2, result);
    }

    /**
     * Returns UNDEFINED if value does not exist in the list.
     */
    @Test
    void getValueLocationInListReturnsUndefinedWhenNotFound() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{1, 5, 3, 9, 2});
        int result = lfsUtil.getValueLocationInList(7, list, 5);
        Assertions.assertEquals(ILfs.UNDEFINED, result);
    }

    /**
     * Ensures value is removed from list by location.
     */
    @Test
    void removeValueFromLocationInListRemovesValue() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{1, 5, 3, 9, 2});
        int result = lfsUtil.removeValueFromLocationInList(2, list, 5);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertEquals(9, list.get(2));
    }

    /**
     * Finds the correct incremental position in a double array.
     */
    @Test
    void findIncrementalPositionInDoubleArrayFindsPosition() {
        AtomicReferenceArray<Double> list = new AtomicReferenceArray<>(new Double[]{1.0, 3.0, 5.0, 7.0});
        int result = lfsUtil.findIncrementalPositionInDoubleArray(4.0, list, 4);
        Assertions.assertEquals(2, result);
    }

    /**
     * Calculates correct angle to a line based on input points.
     */
    @Test
    void angleToLineCalculatesAngle() {
        double result = lfsUtil.angleToLine(0, 0, 1, 1);
        Assertions.assertEquals(-Math.PI/4, result, 0.001);
    }

    /**
     * Returns zero angle if delta is too small.
     */
    @Test
    void angleToLineWithSmallDeltaReturnsZero() {
        double result = lfsUtil.angleToLine(0, 0, 0, 0);
        Assertions.assertEquals(0.0, result, 0.001);
    }

    /**
     * Computes the discrete direction for a line.
     */
    @Test
    void lineToDirectionCalculatesDirection() {
        int result = lfsUtil.lineToDirection(0, 0, 1, 0, 8);
        Assertions.assertTrue(result >= 0 && result < 16);
    }

    /**
     * Validates closest direction distance computation.
     */
    @Test
    void closestDirDistanceCalculatesDistance() {
        int result = lfsUtil.closestDirDistance(1, 3, 8);
        Assertions.assertEquals(2, result);
    }

    /**
     * Returns INVALID_DIR when main direction is invalid.
     */
    @Test
    void closestDirDistanceWithInvalidDirectionReturnsInvalid() {
        int result = lfsUtil.closestDirDistance(-1, 3, 8);
        Assertions.assertEquals(ILfs.INVALID_DIR, result);
    }

    /**
     * Returns error if too few items for minMaxs.
     */
    @Test
    void minMaxsWithTooFewItemsReturnsError() {
        AtomicIntegerArray oMinMaxValue = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxType = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxIndex = new AtomicIntegerArray(10);
        AtomicInteger oMinMaxAlloc = new AtomicInteger();
        AtomicInteger oMinMaxNumber = new AtomicInteger();
        AtomicIntegerArray items = new AtomicIntegerArray(new int[]{1, 2});
        int result = lfsUtil.minMaxs(oMinMaxValue, oMinMaxType, oMinMaxIndex, oMinMaxAlloc, oMinMaxNumber, items, 2);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertEquals(-1, oMinMaxAlloc.get());
        Assertions.assertEquals(-1, oMinMaxNumber.get());
    }

    /**
     * Finds maxima in increasing sequence.
     */
    @Test
    void minMaxsWithIncreasingSequenceFindsMaxima() {
        AtomicIntegerArray oMinMaxValue = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxType = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxIndex = new AtomicIntegerArray(10);
        AtomicInteger oMinMaxAlloc = new AtomicInteger();
        AtomicInteger oMinMaxNumber = new AtomicInteger();
        AtomicIntegerArray items = new AtomicIntegerArray(new int[]{1, 3, 2, 4, 1});
        int result = lfsUtil.minMaxs(oMinMaxValue, oMinMaxType, oMinMaxIndex, oMinMaxAlloc, oMinMaxNumber, items, 5);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(oMinMaxNumber.get() > 0);
    }

    /**
     * Finds minima in decreasing sequence.
     */
    @Test
    void minMaxsWithDecreasingSequenceFindsMinima() {
        AtomicIntegerArray oMinMaxValue = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxType = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxIndex = new AtomicIntegerArray(10);
        AtomicInteger oMinMaxAlloc = new AtomicInteger();
        AtomicInteger oMinMaxNumber = new AtomicInteger();
        AtomicIntegerArray items = new AtomicIntegerArray(new int[]{4, 2, 3, 1, 2});
        int result = lfsUtil.minMaxs(oMinMaxValue, oMinMaxType, oMinMaxIndex, oMinMaxAlloc, oMinMaxNumber, items, 5);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(oMinMaxNumber.get() > 0);
    }

    /**
     * Handles sequences with level plateaus.
     */
    @Test
    void minMaxsWithLevelSequenceHandlesPlateaus() {
        AtomicIntegerArray oMinMaxValue = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxType = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxIndex = new AtomicIntegerArray(10);
        AtomicInteger oMinMaxAlloc = new AtomicInteger();
        AtomicInteger oMinMaxNumber = new AtomicInteger();
        AtomicIntegerArray items = new AtomicIntegerArray(new int[]{2, 2, 2, 4, 1});
        int result = lfsUtil.minMaxs(oMinMaxValue, oMinMaxType, oMinMaxIndex, oMinMaxAlloc, oMinMaxNumber, items, 5);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(oMinMaxNumber.get() >= 0);
    }

    /**
     * Returns the end position if not found in sorted double array.
     */
    @Test
    void findIncrementalPositionInDoubleArrayReturnsEndPosition() {
        AtomicReferenceArray<Double> list = new AtomicReferenceArray<>(new Double[]{1.0, 3.0, 5.0, 7.0});
        int result = lfsUtil.findIncrementalPositionInDoubleArray(9.0, list, 4);
        Assertions.assertEquals(4, result);
    }

    /**
     * Throws exception for invalid index in removeValueFromLocationInList.
     */
    @Test
    void removeValueFromLocationInListWithInvalidIndexReturnsError() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{1, 5, 3, 9, 2});
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            lfsUtil.removeValueFromLocationInList(-1, list, 5);
        });
    }

    /**
     * Calculates correct direction difference with wrap around.
     */
    @Test
    void closestDirDistanceWithWrapAroundCalculatesCorrectly() {
        int result = lfsUtil.closestDirDistance(0, 7, 8);
        Assertions.assertEquals(1, result);
    }

    /**
     * Returns INVALID_DIR if both directions are invalid.
     */
    @Test
    void closestDirDistanceWithBothInvalidReturnsInvalid() {
        int result = lfsUtil.closestDirDistance(-1, -1, 8);
        Assertions.assertEquals(ILfs.INVALID_DIR, result);
    }

    /**
     * Tests maxValue with single element array.
     */
    @Test
    void maxValueWithSingleElement() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{42});
        int result = lfsUtil.maxValue(list, 1);
        Assertions.assertEquals(42, result);
    }

    /**
     * Tests minValue with single element array.
     */
    @Test
    void minValueWithSingleElement() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{42});
        int result = lfsUtil.minValue(list, 1);
        Assertions.assertEquals(42, result);
    }

    /**
     * Tests maxValue with negative numbers.
     */
    @Test
    void maxValueWithNegativeNumbers() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{-5, -1, -10, -3});
        int result = lfsUtil.maxValue(list, 4);
        Assertions.assertEquals(-1, result);
    }

    /**
     * Tests minValue with negative numbers.
     */
    @Test
    void minValueWithNegativeNumbers() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{-5, -1, -10, -3});
        int result = lfsUtil.minValue(list, 4);
        Assertions.assertEquals(-10, result);
    }

    /**
     * Tests distance with zero distance.
     */
    @Test
    void distanceWithSamePoints() {
        double result = lfsUtil.distance(5, 5, 5, 5);
        Assertions.assertEquals(0.0, result, 0.001);
    }

    /**
     * Tests squaredDistance with zero distance.
     */
    @Test
    void squaredDistanceWithSamePoints() {
        double result = lfsUtil.squaredDistance(5, 5, 5, 5);
        Assertions.assertEquals(0.0, result, 0.001);
    }

    /**
     * Tests getValueLocationInList with first element.
     */
    @Test
    void getValueLocationInListFindsFirstElement() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{7, 5, 3, 9, 2});
        int result = lfsUtil.getValueLocationInList(7, list, 5);
        Assertions.assertEquals(0, result);
    }

    /**
     * Tests getValueLocationInList with last element.
     */
    @Test
    void getValueLocationInListFindsLastElement() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{1, 5, 3, 9, 7});
        int result = lfsUtil.getValueLocationInList(7, list, 5);
        Assertions.assertEquals(4, result);
    }

    /**
     * Tests removeValueFromLocationInList with first element.
     */
    @Test
    void removeValueFromLocationInListRemovesFirstElement() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{1, 5, 3, 9, 2});
        int result = lfsUtil.removeValueFromLocationInList(0, list, 5);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertEquals(5, list.get(0));
    }

    /**
     * Tests removeValueFromLocationInList with last element.
     */
    @Test
    void removeValueFromLocationInListRemovesLastElement() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{1, 5, 3, 9, 2});
        int result = lfsUtil.removeValueFromLocationInList(4, list, 5);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertEquals(2, list.get(4));
    }

    /**
     * Tests findIncrementalPositionInDoubleArray with value at beginning.
     */
    @Test
    void findIncrementalPositionInDoubleArrayAtBeginning() {
        AtomicReferenceArray<Double> list = new AtomicReferenceArray<>(new Double[]{2.0, 4.0, 6.0, 8.0});
        int result = lfsUtil.findIncrementalPositionInDoubleArray(1.0, list, 4);
        Assertions.assertEquals(0, result);
    }

    /**
     * Tests findIncrementalPositionInDoubleArray with exact match.
     */
    @Test
    void findIncrementalPositionInDoubleArrayExactMatch() {
        AtomicReferenceArray<Double> list = new AtomicReferenceArray<>(new Double[]{1.0, 3.0, 5.0, 7.0});
        int result = lfsUtil.findIncrementalPositionInDoubleArray(5.0, list, 4);
        Assertions.assertEquals(3, result);
    }

    /**
     * Tests angleToLine with vertical line (dx = 0).
     */
    @Test
    void angleToLineWithVerticalLine() {
        double result = lfsUtil.angleToLine(0, 0, 0, 5);
        Assertions.assertEquals(-Math.PI/2, result, 0.001);
    }

    /**
     * Tests angleToLine with horizontal line (dy = 0).
     */
    @Test
    void angleToLineWithHorizontalLine() {
        double result = lfsUtil.angleToLine(0, 0, 5, 0);
        Assertions.assertEquals(0.0, result, 0.001);
    }

    /**
     * Tests lineToDirection with various angles.
     */
    @Test
    void lineToDirectionWithDifferentAngles() {
        int result1 = lfsUtil.lineToDirection(0, 0, 1, 0, 8);
        int result2 = lfsUtil.lineToDirection(0, 0, 0, 1, 8);
        Assertions.assertTrue(result1 >= 0 && result1 < 16);
        Assertions.assertTrue(result2 >= 0 && result2 < 16);
        Assertions.assertNotEquals(result1, result2);
    }

    /**
     * Tests closestDirDistance with maximum distance.
     */
    @Test
    void closestDirDistanceWithMaxDistance() {
        int result = lfsUtil.closestDirDistance(0, 4, 8);
        Assertions.assertEquals(4, result);
    }

    /**
     * Tests closestDirDistance with one invalid direction.
     */
    @Test
    void closestDirDistanceWithOneInvalidDirection() {
        int result1 = lfsUtil.closestDirDistance(-1, 3, 8);
        int result2 = lfsUtil.closestDirDistance(3, -1, 8);
        Assertions.assertEquals(ILfs.INVALID_DIR, result1);
        Assertions.assertEquals(ILfs.INVALID_DIR, result2);
    }

    /**
     * Tests minMaxs with all equal values (flat sequence).
     */
    @Test
    void minMaxsWithFlatSequence() {
        AtomicIntegerArray oMinMaxValue = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxType = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxIndex = new AtomicIntegerArray(10);
        AtomicInteger oMinMaxAlloc = new AtomicInteger();
        AtomicInteger oMinMaxNumber = new AtomicInteger();
        AtomicIntegerArray items = new AtomicIntegerArray(new int[]{5, 5, 5, 5, 5});
        int result = lfsUtil.minMaxs(oMinMaxValue, oMinMaxType, oMinMaxIndex, oMinMaxAlloc, oMinMaxNumber, items, 5);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertEquals(0, oMinMaxNumber.get());
    }

    /**
     * Tests minMaxs with single peak.
     */
    @Test
    void minMaxsWithSinglePeak() {
        AtomicIntegerArray oMinMaxValue = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxType = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxIndex = new AtomicIntegerArray(10);
        AtomicInteger oMinMaxAlloc = new AtomicInteger();
        AtomicInteger oMinMaxNumber = new AtomicInteger();
        AtomicIntegerArray items = new AtomicIntegerArray(new int[]{1, 2, 5, 2, 1});
        int result = lfsUtil.minMaxs(oMinMaxValue, oMinMaxType, oMinMaxIndex, oMinMaxAlloc, oMinMaxNumber, items, 5);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(oMinMaxNumber.get() > 0);
    }

    /**
     * Tests minMaxs with single valley.
     */
    @Test
    void minMaxsWithSingleValley() {
        AtomicIntegerArray oMinMaxValue = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxType = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxIndex = new AtomicIntegerArray(10);
        AtomicInteger oMinMaxAlloc = new AtomicInteger();
        AtomicInteger oMinMaxNumber = new AtomicInteger();
        AtomicIntegerArray items = new AtomicIntegerArray(new int[]{5, 4, 1, 4, 5});
        int result = lfsUtil.minMaxs(oMinMaxValue, oMinMaxType, oMinMaxIndex, oMinMaxAlloc, oMinMaxNumber, items, 5);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(oMinMaxNumber.get() > 0);
    }

    /**
     * Tests minMaxs starting with level plateau.
     */
    @Test
    void minMaxsWithStartingPlateau() {
        AtomicIntegerArray oMinMaxValue = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxType = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxIndex = new AtomicIntegerArray(10);
        AtomicInteger oMinMaxAlloc = new AtomicInteger();
        AtomicInteger oMinMaxNumber = new AtomicInteger();
        AtomicIntegerArray items = new AtomicIntegerArray(new int[]{3, 3, 3, 5, 1});
        int result = lfsUtil.minMaxs(oMinMaxValue, oMinMaxType, oMinMaxIndex, oMinMaxAlloc, oMinMaxNumber, items, 5);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(oMinMaxNumber.get() >= 0);
    }

    /**
     * Tests lineToDirection with same points.
     */
    @Test
    void lineToDirectionWithSamePoints() {
        int result = lfsUtil.lineToDirection(5, 5, 5, 5, 8);
        Assertions.assertTrue(result >= 0 && result < 16);
    }

    /**
     * Tests angleToLine with very small deltas.
     */
    @Test
    void angleToLineWithVerySmallDeltas() {
        double result = lfsUtil.angleToLine(0, 0, 0, 0);
        Assertions.assertEquals(0.0, result, 0.001);
    }

    /**
     * Tests distance with negative coordinates.
     */
    @Test
    void distanceWithNegativeCoordinates() {
        double result = lfsUtil.distance(-3, -4, 0, 0);
        Assertions.assertEquals(5.0, result, 0.001);
    }

    /**
     * Tests squaredDistance with negative coordinates.
     */
    @Test
    void squaredDistanceWithNegativeCoordinates() {
        double result = lfsUtil.squaredDistance(-3, -4, 0, 0);
        Assertions.assertEquals(25.0, result, 0.001);
    }

    /**
     * Tests getValueLocationInList with empty search (num = 0).
     */
    @Test
    void getValueLocationInListWithEmptySearch() {
        AtomicIntegerArray list = new AtomicIntegerArray(new int[]{1, 5, 3, 9, 2});
        int result = lfsUtil.getValueLocationInList(5, list, 0);
        Assertions.assertEquals(ILfs.UNDEFINED, result);
    }

    /**
     * Tests findIncrementalPositionInDoubleArray with empty list.
     */
    @Test
    void findIncrementalPositionInDoubleArrayWithEmptyList() {
        AtomicReferenceArray<Double> list = new AtomicReferenceArray<>(new Double[]{1.0, 3.0, 5.0, 7.0});
        int result = lfsUtil.findIncrementalPositionInDoubleArray(4.0, list, 0);
        Assertions.assertEquals(0, result);
    }

    /**
     * Tests closestDirDistance with identical directions.
     */
    @Test
    void closestDirDistanceWithIdenticalDirections() {
        int result = lfsUtil.closestDirDistance(5, 5, 8);
        Assertions.assertEquals(0, result);
    }

    /**
     * Tests minMaxs with alternating values.
     */
    @Test
    void minMaxsWithAlternatingValues() {
        AtomicIntegerArray oMinMaxValue = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxType = new AtomicIntegerArray(10);
        AtomicIntegerArray oMinMaxIndex = new AtomicIntegerArray(10);
        AtomicInteger oMinMaxAlloc = new AtomicInteger();
        AtomicInteger oMinMaxNumber = new AtomicInteger();
        AtomicIntegerArray items = new AtomicIntegerArray(new int[]{1, 3, 2, 4, 1, 5});
        int result = lfsUtil.minMaxs(oMinMaxValue, oMinMaxType, oMinMaxIndex, oMinMaxAlloc, oMinMaxNumber, items, 6);
        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(oMinMaxNumber.get() > 0);
    }

    /**
     * Tests lineToDirection with negative coordinates.
     */
    @Test
    void lineToDirectionWithNegativeCoordinates() {
        int result = lfsUtil.lineToDirection(-5, -5, 0, 0, 8);
        Assertions.assertTrue(result >= 0 && result < 16);
    }
}