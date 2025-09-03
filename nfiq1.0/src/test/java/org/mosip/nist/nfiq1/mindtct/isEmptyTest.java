package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicIntegerArray;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.mosip.nist.nfiq1.common.ILfs;

/**
 * Test class for {@link IsEmpty}
 *
 * This class validates the functionality of image emptiness detection based on
 * quality maps computed by NIST's Mindtct algorithm.
 */
class isEmptyTest {

    private IsEmpty isEmpty;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes the IsEmpty singleton instance for testing.
     */
    @BeforeEach
    public void setUp() {
        isEmpty = IsEmpty.getInstance();
    }

    /**
     * Verifies that getInstance returns the same singleton instance across multiple calls.
     * This ensures proper singleton pattern implementation.
     */
    @Test
    public void verifySingletonInstance() {
        IsEmpty firstInstance = IsEmpty.getInstance();
        IsEmpty secondInstance = IsEmpty.getInstance();
        assertEquals(firstInstance, secondInstance);
    }

    /**
     * Validates that an image is correctly identified as empty when the quality map
     * contains only zero values across all positions.
     */
    @Test
    public void validateEmptyQualityMapDetection() {
        int mapWidth = 3;
        int mapHeight = 3;
        AtomicIntegerArray qualityMap = new AtomicIntegerArray(mapWidth * mapHeight);
        for (int i = 0; i < qualityMap.length(); i++) {
            qualityMap.set(i, ILfs.FALSE);
        }
        int result = isEmpty.isQualityMapEmpty(qualityMap, mapWidth, mapHeight);
        assertEquals(ILfs.TRUE, result);
    }

    /**
     * Validates that an image is identified as non-empty when the quality map
     * contains at least one non-zero value.
     */
    @Test
    public void validateNonEmptyQualityMapDetection() {
        int mapWidth = 3;
        int mapHeight = 3;
        AtomicIntegerArray qualityMap = new AtomicIntegerArray(mapWidth * mapHeight);
        for (int i = 0; i < qualityMap.length(); i++) {
            qualityMap.set(i, ILfs.FALSE);
        }
        qualityMap.set(4, 1);
        int result = isEmpty.isQualityMapEmpty(qualityMap, mapWidth, mapHeight);
        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates quality map emptiness detection with a single element map containing zero.
     */
    @Test
    public void validateSingleElementEmptyMap() {
        int mapWidth = 1;
        int mapHeight = 1;
        AtomicIntegerArray qualityMap = new AtomicIntegerArray(1);
        qualityMap.set(0, ILfs.FALSE);
        int result = isEmpty.isQualityMapEmpty(qualityMap, mapWidth, mapHeight);
        assertEquals(ILfs.TRUE, result);
    }

    /**
     * Validates quality map emptiness detection with a single element map containing non-zero.
     */
    @Test
    public void validateSingleElementNonEmptyMap() {
        int mapWidth = 1;
        int mapHeight = 1;
        AtomicIntegerArray qualityMap = new AtomicIntegerArray(1);
        qualityMap.set(0, 5);
        int result = isEmpty.isQualityMapEmpty(qualityMap, mapWidth, mapHeight);
        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates quality map emptiness detection with a larger map containing all zero values.
     */
    @Test
    public void validateLargeEmptyQualityMap() {
        int mapWidth = 10;
        int mapHeight = 10;
        AtomicIntegerArray qualityMap = new AtomicIntegerArray(mapWidth * mapHeight);
        for (int i = 0; i < qualityMap.length(); i++) {
            qualityMap.set(i, ILfs.FALSE);
        }
        int result = isEmpty.isQualityMapEmpty(qualityMap, mapWidth, mapHeight);
        assertEquals(ILfs.TRUE, result);
    }

    /**
     * Validates quality map emptiness detection when non-zero value is at the first position.
     */
    @Test
    public void validateNonZeroAtFirstPosition() {
        int mapWidth = 5;
        int mapHeight = 5;
        AtomicIntegerArray qualityMap = new AtomicIntegerArray(mapWidth * mapHeight);
        qualityMap.set(0, 3);
        for (int i = 1; i < qualityMap.length(); i++) {
            qualityMap.set(i, ILfs.FALSE);
        }
        int result = isEmpty.isQualityMapEmpty(qualityMap, mapWidth, mapHeight);
        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates quality map emptiness detection when non-zero value is at the last position.
     */
    @Test
    public void validateNonZeroAtLastPosition() {
        int mapWidth = 4;
        int mapHeight = 4;
        AtomicIntegerArray qualityMap = new AtomicIntegerArray(mapWidth * mapHeight);
        for (int i = 0; i < qualityMap.length() - 1; i++) {
            qualityMap.set(i, ILfs.FALSE);
        }
        qualityMap.set(qualityMap.length() - 1, 7);
        int result = isEmpty.isQualityMapEmpty(qualityMap, mapWidth, mapHeight);
        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates image emptiness detection when quality map is empty.
     */
    @Test
    public void validateImageEmptyWithEmptyQualityMap() {
        int mapWidth = 3;
        int mapHeight = 3;
        AtomicIntegerArray qualityMap = new AtomicIntegerArray(mapWidth * mapHeight);
        for (int i = 0; i < qualityMap.length(); i++) {
            qualityMap.set(i, ILfs.FALSE);
        }
        int result = isEmpty.isImageEmpty(qualityMap, mapWidth, mapHeight);
        assertEquals(ILfs.TRUE, result);
    }

    /**
     * Validates image emptiness detection when quality map is non-empty.
     */
    @Test
    public void validateImageNonEmptyWithNonEmptyQualityMap() {
        int mapWidth = 3;
        int mapHeight = 3;
        AtomicIntegerArray qualityMap = new AtomicIntegerArray(mapWidth * mapHeight);
        for (int i = 0; i < qualityMap.length(); i++) {
            qualityMap.set(i, ILfs.FALSE);
        }
        qualityMap.set(2, 8);
        int result = isEmpty.isImageEmpty(qualityMap, mapWidth, mapHeight);
        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates rectangular map sizes scenarios.
     */
    @Test
    public void validateRectangularMapDimensions() {
        int mapWidth = 6;
        int mapHeight = 4;
        AtomicIntegerArray qualityMap = new AtomicIntegerArray(mapWidth * mapHeight);
        for (int i = 0; i < qualityMap.length(); i++) {
            qualityMap.set(i, ILfs.FALSE);
        }
        qualityMap.set(10, 2);
        int result = isEmpty.isQualityMapEmpty(qualityMap, mapWidth, mapHeight);
        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates non-zero value detection for both positive and negative values.
     */
    @Test
    public void validateVariousNonZeroValues() {
        int mapWidth = 2;
        int mapHeight = 2;
        AtomicIntegerArray qualityMapPositive = new AtomicIntegerArray(mapWidth * mapHeight);
        for (int i = 0; i < qualityMapPositive.length(); i++) {
            qualityMapPositive.set(i, ILfs.FALSE);
        }
        qualityMapPositive.set(1, 100);
        int resultPositive = isEmpty.isQualityMapEmpty(qualityMapPositive, mapWidth, mapHeight);
        assertEquals(ILfs.FALSE, resultPositive);

        AtomicIntegerArray qualityMapNegative = new AtomicIntegerArray(mapWidth * mapHeight);
        for (int i = 0; i < qualityMapNegative.length(); i++) {
            qualityMapNegative.set(i, ILfs.FALSE);
        }
        qualityMapNegative.set(3, -50);
        int resultNegative = isEmpty.isQualityMapEmpty(qualityMapNegative, mapWidth, mapHeight);
        assertEquals(ILfs.FALSE, resultNegative);
    }
}