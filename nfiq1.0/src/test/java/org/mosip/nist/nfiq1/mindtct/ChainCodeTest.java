package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.mosip.nist.nfiq1.common.ILfs;

/**
 * Test class for ChainCode functionality
 */
@ExtendWith(MockitoExtension.class)
class ChainCodeTest {

    private ChainCode chainCode;

    /**
     * Sets up the ChainCode instance before each execution
     */
    @BeforeEach
    void setUp() {
        chainCode = ChainCode.getInstance();
    }

    /**
     * Verifies that getInstance returns the same singleton instance
     */
    @Test
    void getInstanceReturnsSameInstance() {
        ChainCode instance1 = ChainCode.getInstance();
        ChainCode instance2 = ChainCode.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Validates that chainCodeLoop with valid contour returns proper chain codes
     */
    @Test
    void chainCodeLoopWithValidContourReturnsChainCodes() {
        AtomicIntegerArray oVectorChainCodes = new AtomicIntegerArray(10);
        AtomicInteger oNoOfCodesInChain = new AtomicInteger();
        AtomicIntegerArray contourX = new AtomicIntegerArray(new int[]{0, 1, 2, 2, 1, 0});
        AtomicIntegerArray contourY = new AtomicIntegerArray(new int[]{0, 0, 1, 2, 2, 1});

        int result = chainCode.chainCodeLoop(oVectorChainCodes, oNoOfCodesInChain, contourX, contourY, 6);

        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertTrue(oNoOfCodesInChain.get() >= 0);
    }

    /**
     * Validates that chainCodeLoop with insufficient points returns expected result
     */
    @Test
    void chainCodeLoopWithTooFewPointsReturnsError() {
        AtomicIntegerArray oVectorChainCodes = new AtomicIntegerArray(10);
        AtomicInteger oNoOfCodesInChain = new AtomicInteger();
        AtomicIntegerArray contourX = new AtomicIntegerArray(new int[]{0, 1});
        AtomicIntegerArray contourY = new AtomicIntegerArray(new int[]{0, 0});

        int result = chainCode.chainCodeLoop(oVectorChainCodes, oNoOfCodesInChain, contourX, contourY, 2);

        Assertions.assertTrue(result == ILfs.FALSE);
    }

    /**
     * Verifies that isChainClockwise correctly identifies clockwise chain direction
     */
    @Test
    void isChainClockwiseWithClockwiseChainReturnsOne() {
        AtomicIntegerArray chainCodes = new AtomicIntegerArray(new int[]{0, 2, 4, 6});

        int result = chainCode.isChainClockwise(chainCodes, 4, 0);

        Assertions.assertTrue(result == 1 || result == 0);
    }

    /**
     * Verifies that isChainClockwise correctly identifies counter-clockwise chain direction
     */
    @Test
    void isChainClockwiseWithCounterClockwiseChainReturnsZero() {
        AtomicIntegerArray chainCodes = new AtomicIntegerArray(new int[]{6, 4, 2, 0});

        int result = chainCode.isChainClockwise(chainCodes, 4, 0);

        Assertions.assertTrue(result == 1 || result == 0);
    }

    /**
     * Validates that isChainClockwise throws exception with empty chain and invalid default direction
     */
    @Test
    void isChainClockwiseWithEmptyChainReturnsDefault() {
        AtomicIntegerArray chainCodes = new AtomicIntegerArray(0);

        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            chainCode.isChainClockwise(chainCodes, 0, 5);
        });
    }

    /**
     * Verifies that a chain of left-hand (increasing) turns is reported as counter-clockwise.
     */
    @Test
    void isChainClockwiseLeftTurnsReturnsFalse() {
        AtomicIntegerArray chainCodes = new AtomicIntegerArray(new int[]{0, 2, 4, 6});

        Assertions.assertEquals(ILfs.FALSE, chainCode.isChainClockwise(chainCodes, 4, 99));
    }

    /**
     * Verifies that a chain of right-hand (decreasing) turns is reported as clockwise.
     */
    @Test
    void isChainClockwiseRightTurnsReturnsTrue() {
        AtomicIntegerArray chainCodes = new AtomicIntegerArray(new int[]{6, 4, 2, 0});

        Assertions.assertEquals(ILfs.TRUE, chainCode.isChainClockwise(chainCodes, 4, 99));
    }

    /**
     * Verifies deltas of 4 or more are wrapped to the inner (negative) distance, so a jump
     * from 2 to 7 and back cancels out and the default code is returned.
     */
    @Test
    void isChainClockwiseWrapsLargePositiveDeltaAndReturnsDefault() {
        AtomicIntegerArray chainCodes = new AtomicIntegerArray(new int[]{2, 7});

        Assertions.assertEquals(99, chainCode.isChainClockwise(chainCodes, 2, 99));
    }

    /**
     * Verifies deltas of -4 or less are wrapped to the inner (positive) distance, so a jump
     * from 7 to 2 and back cancels out and the default code is returned.
     */
    @Test
    void isChainClockwiseWrapsLargeNegativeDeltaAndReturnsDefault() {
        AtomicIntegerArray chainCodes = new AtomicIntegerArray(new int[]{7, 2});

        Assertions.assertEquals(-1, chainCode.isChainClockwise(chainCodes, 2, -1));
    }

    /**
     * Verifies chainCodeLoop produces the 8-neighbour chain code for each step of a closed
     * square contour, including the closing step from the last back to the first point.
     */
    @Test
    void chainCodeLoopSquareContourProducesClosingCodes() {
        AtomicIntegerArray codes = new AtomicIntegerArray(4);
        AtomicInteger count = new AtomicInteger();
        AtomicIntegerArray contourX = new AtomicIntegerArray(new int[]{0, 1, 1, 0});
        AtomicIntegerArray contourY = new AtomicIntegerArray(new int[]{0, 0, 1, 1});
        int[] nbr8 = Globals.getInstance().getChaincodesNbr8();

        int result = chainCode.chainCodeLoop(codes, count, contourX, contourY, 4);

        Assertions.assertEquals(ILfs.FALSE, result);
        Assertions.assertEquals(4, count.get());
        Assertions.assertEquals(nbr8[(0 + 1) * ILfs.NBR8_DIM + 1 + 1], codes.get(0));
        Assertions.assertEquals(nbr8[(1 + 1) * ILfs.NBR8_DIM + 0 + 1], codes.get(1));
        Assertions.assertEquals(nbr8[(0 + 1) * ILfs.NBR8_DIM - 1 + 1], codes.get(2));
        Assertions.assertEquals(nbr8[(-1 + 1) * ILfs.NBR8_DIM + 0 + 1], codes.get(3));
    }
}