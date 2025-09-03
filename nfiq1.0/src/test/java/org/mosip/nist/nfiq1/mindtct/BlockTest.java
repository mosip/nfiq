package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;

/**
 * Test class for Block functionality
 */
@ExtendWith(MockitoExtension.class)
class BlockTest {

    private Block block;

    @Mock
    private LfsParams mockLfsParams;

    /**
     * Sets up the Block instance before each execution
     */
    @BeforeEach
    void setUp() {
        block = Block.getInstance();
    }

    /**
     * Verifies that getInstance returns the same singleton instance
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Block instance1 = Block.getInstance();
        Block instance2 = Block.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Validates that getDefs returns a non-null object
     */
    @Test
    void getDefsReturnsNotNull() {
        Assertions.assertNotNull(block.getDefs());
    }

    /**
     * Verifies that blockOffsets with valid input returns proper offset arrays
     */
    @Test
    void blockOffsetsWithValidInputReturnsOffsets() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oImageWidth = new AtomicInteger();
        AtomicInteger oImageHeight = new AtomicInteger();

        AtomicIntegerArray result = block.blockOffsets(ret, oImageWidth, oImageHeight,
                100, 100, 10, 16);

        Assertions.assertEquals(ILfs.FALSE, ret.get());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(oImageWidth.get() > 0);
        Assertions.assertTrue(oImageHeight.get() > 0);
    }

    /**
     * Validates that blockOffsets with small image dimensions returns error
     */
    @Test
    void blockOffsetsWithSmallImageReturnsError() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oImageWidth = new AtomicInteger();
        AtomicInteger oImageHeight = new AtomicInteger();

        AtomicIntegerArray result = block.blockOffsets(ret, oImageWidth, oImageHeight,
                10, 10, 0, 16);

        Assertions.assertEquals(ILfs.ERROR_CODE_80, ret.get());
        Assertions.assertNull(result);
    }

    /**
     * Verifies that lowContrastBlock returns false for high contrast data
     */
    @Test
    void lowContrastBlockWithHighContrastReturnsFalse() {
        Mockito.when(mockLfsParams.getPercentileMinMax()).thenReturn(10);
        Mockito.when(mockLfsParams.getMinContrastDelta()).thenReturn(5);

        int[] paddedImageData = new int[256];
        for (int i = 0; i < paddedImageData.length; i++) {
            paddedImageData[i] = i % 64;
        }

        int result = block.lowContrastBlock(0, 16, paddedImageData, 16, 16, mockLfsParams);

        Assertions.assertEquals(ILfs.FALSE, result);
    }

    /**
     * Verifies that lowContrastBlock returns true for low contrast data
     */
    @Test
    void lowContrastBlockWithLowContrastReturnsTrue() {
        Mockito.when(mockLfsParams.getPercentileMinMax()).thenReturn(10);
        Mockito.when(mockLfsParams.getMinContrastDelta()).thenReturn(50);

        int[] paddedImageData = new int[256];
        for (int i = 0; i < paddedImageData.length; i++) {
            paddedImageData[i] = 32;
        }

        int result = block.lowContrastBlock(0, 16, paddedImageData, 16, 16, mockLfsParams);

        Assertions.assertEquals(ILfs.TRUE, result);
    }

    /**
     * Validates that findValidBlock returns found status with valid direction
     */
    @Test
    void findValidBlockWithValidDirectionReturnsFound() {
        AtomicInteger nbrDir = new AtomicInteger();
        AtomicInteger nbrX = new AtomicInteger();
        AtomicInteger nbrY = new AtomicInteger();

        AtomicIntegerArray directionMap = new AtomicIntegerArray(9);
        AtomicIntegerArray lowContrastMap = new AtomicIntegerArray(9);

        directionMap.set(5, 2);
        lowContrastMap.set(5, 0);

        int result = block.findValidBlock(nbrDir, nbrX, nbrY, directionMap, lowContrastMap,
                1, 1, 3, 3, 1, 0);

        Assertions.assertEquals(ILfs.FOUND, result);
        Assertions.assertEquals(2, nbrDir.get());
    }

    /**
     * Validates that findValidBlock returns not found status for low contrast blocks
     */
    @Test
    void findValidBlockWithLowContrastReturnsNotFound() {
        AtomicInteger nbrDir = new AtomicInteger();
        AtomicInteger nbrX = new AtomicInteger();
        AtomicInteger nbrY = new AtomicInteger();

        AtomicIntegerArray directionMap = new AtomicIntegerArray(9);
        AtomicIntegerArray lowContrastMap = new AtomicIntegerArray(9);

        lowContrastMap.set(5, 1);

        int result = block.findValidBlock(nbrDir, nbrX, nbrY, directionMap, lowContrastMap,
                1, 1, 3, 3, 1, 0);

        Assertions.assertEquals(ILfs.NOT_FOUND, result);
    }

    /**
     * Validates that findValidBlock returns not found for out of bounds coordinates
     */
    @Test
    void findValidBlockOutOfBoundsReturnsNotFound() {
        AtomicInteger nbrDir = new AtomicInteger();
        AtomicInteger nbrX = new AtomicInteger();
        AtomicInteger nbrY = new AtomicInteger();

        AtomicIntegerArray directionMap = new AtomicIntegerArray(9);
        AtomicIntegerArray lowContrastMap = new AtomicIntegerArray(9);

        int result = block.findValidBlock(nbrDir, nbrX, nbrY, directionMap, lowContrastMap,
                2, 2, 3, 3, 1, 1);

        Assertions.assertEquals(ILfs.NOT_FOUND, result);
    }

    /**
     * Verifies that setMarginBlocks correctly sets perimeter block values
     */
    @Test
    void setMarginBlocksSetsPerimeterValues() {
        AtomicIntegerArray oMap = new AtomicIntegerArray(9);

        block.setMarginBlocks(oMap, 3, 3, 255);

        Assertions.assertEquals(255, oMap.get(0));
        Assertions.assertEquals(255, oMap.get(1));
        Assertions.assertEquals(255, oMap.get(2));
        Assertions.assertEquals(255, oMap.get(3));
        Assertions.assertEquals(255, oMap.get(5));
        Assertions.assertEquals(255, oMap.get(6));
        Assertions.assertEquals(255, oMap.get(7));
        Assertions.assertEquals(255, oMap.get(8));
    }

    /**
     * Validates that blockOffsets calculates correctly for larger image dimensions
     */
    @Test
    void blockOffsetsWithLargerImageCalculatesCorrectly() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oImageWidth = new AtomicInteger();
        AtomicInteger oImageHeight = new AtomicInteger();

        AtomicIntegerArray result = block.blockOffsets(ret, oImageWidth, oImageHeight,
                200, 150, 5, 16);

        Assertions.assertEquals(ILfs.FALSE, ret.get());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(oImageWidth.get() > 0);
        Assertions.assertTrue(oImageHeight.get() > 0);
    }

    /**
     * Validates that blockOffsets handles exact multiple dimensions correctly
     */
    @Test
    void blockOffsetsWithExactMultipleCalculatesCorrectly() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oImageWidth = new AtomicInteger();
        AtomicInteger oImageHeight = new AtomicInteger();

        AtomicIntegerArray result = block.blockOffsets(ret, oImageWidth, oImageHeight,
                64, 64, 0, 16);

        Assertions.assertEquals(ILfs.FALSE, ret.get());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, oImageWidth.get());
        Assertions.assertEquals(4, oImageHeight.get());
    }
}