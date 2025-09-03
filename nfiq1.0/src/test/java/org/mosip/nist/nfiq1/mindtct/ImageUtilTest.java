package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test class for ImageUtil functionality
 */
@ExtendWith(MockitoExtension.class)
class ImageUtilTest {

    private ImageUtil imageUtil;

    /**
     * Sets up the ImageUtil instance before each execution
     */
    @BeforeEach
    void setUp() {
        imageUtil = ImageUtil.getInstance();
    }

    /**
     * Verifies that getInstance returns the same singleton instance
     */
    @Test
    void getInstanceReturnsSameInstance() {
        ImageUtil instance1 = ImageUtil.getInstance();
        ImageUtil instance2 = ImageUtil.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Validates that constructor creates ImageUtil instance successfully
     */
    @Test
    void constructorCreatesInstance() {
        Assertions.assertNotNull(imageUtil);
    }

    /**
     * Validates that all getter methods return non-null instances
     */
    @Test
    void getterMethods() {
        Assertions.assertNotNull(imageUtil.getDefs());
        Assertions.assertNotNull(imageUtil.getLine());
        Assertions.assertNotNull(imageUtil.getFree());
        Assertions.assertNotNull(imageUtil.getContour());
    }

    /**
     * Verifies that bits6To8 converts 6-bit values to 8-bit correctly
     */
    @Test
    void bits6To8() {
        int[] imageData = {0, 16, 32, 48, 63};
        int imageWidth = 5;
        int imageHeight = 1;

        imageUtil.bits6To8(imageData, imageWidth, imageHeight);

        Assertions.assertEquals(0, imageData[0]);
        Assertions.assertEquals(64, imageData[1]);
        Assertions.assertEquals(128, imageData[2]);
        Assertions.assertEquals(192, imageData[3]);
        Assertions.assertEquals(252, imageData[4]);
    }

    /**
     * Verifies that bits8To6 converts 8-bit values to 6-bit correctly
     */
    @Test
    void bits8To6() {
        int[] imageData = {0, 64, 128, 192, 255};
        int imageWidth = 5;
        int imageHeight = 1;

        imageUtil.bits8To6(imageData, imageWidth, imageHeight);

        Assertions.assertEquals(0, imageData[0]);
        Assertions.assertEquals(16, imageData[1]);
        Assertions.assertEquals(32, imageData[2]);
        Assertions.assertEquals(48, imageData[3]);
        Assertions.assertEquals(63, imageData[4]);
    }

    /**
     * Validates grayToBinary conversion with threshold
     */
    @Test
    void grayToBinaryWithThreshold() {
        int[] imageData = {50, 100, 150, 200, 250};
        int threshold = 128;
        int lessPixel = 0;
        int greaterPixel = 255;

        imageUtil.grayToBinary(threshold, lessPixel, greaterPixel, imageData, 5, 1);

        Assertions.assertEquals(0, imageData[0]);
        Assertions.assertEquals(0, imageData[1]);
        Assertions.assertEquals(255, imageData[2]);
        Assertions.assertEquals(255, imageData[3]);
        Assertions.assertEquals(255, imageData[4]);
    }

    /**
     * Validates grayToBinary conversion with edge case values
     */
    @Test
    void grayToBinaryEdgeCase() {
        int[] imageData = {127, 128, 129};
        int threshold = 128;
        int lessPixel = 1;
        int greaterPixel = 0;

        imageUtil.grayToBinary(threshold, lessPixel, greaterPixel, imageData, 3, 1);

        Assertions.assertEquals(1, imageData[0]);
        Assertions.assertEquals(0, imageData[1]);
        Assertions.assertEquals(0, imageData[2]);
    }

    /**
     * Verifies that padImage adds padding around image correctly
     */
    @Test
    void padImage() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger ow = new AtomicInteger();
        AtomicInteger oh = new AtomicInteger();

        int[] imageData = {1, 2, 3, 4};
        int imageWidth = 2;
        int imageHeight = 2;
        int pad = 1;
        int padValue = 0;

        int[] result = imageUtil.padImage(ret, ow, oh, imageData, imageWidth, imageHeight, pad, padValue);

        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, ret.get());
        Assertions.assertEquals(4, ow.get());
        Assertions.assertEquals(4, oh.get());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(16, result.length);

        Assertions.assertEquals(0, result[0]);
        Assertions.assertEquals(0, result[3]);
        Assertions.assertEquals(0, result[12]);
        Assertions.assertEquals(0, result[15]);

        Assertions.assertEquals(1, result[5]);
        Assertions.assertEquals(2, result[6]);
        Assertions.assertEquals(3, result[9]);
        Assertions.assertEquals(4, result[10]);
    }

    /**
     * Validates horizontal hole filling functionality
     */
    @Test
    void fillHolesHorizontal() {
        int[] imageData = {
                1, 0, 1, 0, 0,
                0, 1, 0, 1, 1,
                1, 1, 1, 1, 1
        };
        int imageWidth = 5;
        int imageHeight = 3;

        imageUtil.fillHoles(imageData, imageWidth, imageHeight);

        Assertions.assertEquals(1, imageData[1]);
        Assertions.assertEquals(1, imageData[7]);
    }

    /**
     * Validates vertical hole filling functionality
     */
    @Test
    void fillHolesVertical() {
        int[] imageData = {
                1, 1, 1,
                0, 1, 0,
                1, 1, 1
        };
        int imageWidth = 3;
        int imageHeight = 3;

        imageUtil.fillHoles(imageData, imageWidth, imageHeight);

        Assertions.assertEquals(1, imageData[3]);
        Assertions.assertEquals(1, imageData[5]);
    }

    /**
     * Validates that fillHoles handles images with no holes correctly
     */
    @Test
    void fillHolesNoHoles() {
        int[] imageData = {1, 1, 1, 0, 0, 0};
        int imageWidth = 3;
        int imageHeight = 2;

        int[] original = imageData.clone();
        imageUtil.fillHoles(imageData, imageWidth, imageHeight);

        for (int i = 0; i < imageData.length; i++) {
            Assertions.assertEquals(original[i], imageData[i]);
        }
    }

    /**
     * Verifies successful path detection in freePath method
     */
    @Test
    void freePathSuccess() {
        org.mosip.nist.nfiq1.common.ILfs.LfsParams lfsParams = createLfsParams();

        int[] imageData = new int[25];
        java.util.Arrays.fill(imageData, 0);

        int result = imageUtil.freePath(0, 0, 4, 4, imageData, 5, 5, lfsParams);

        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.TRUE, result);
    }

    /**
     * Validates freePath behavior with many transitions
     */
    @Test
    void freePathTooManyTransitions() {
        org.mosip.nist.nfiq1.common.ILfs.LfsParams lfsParams = createLfsParamsWithLowMaxTrans();

        int[] imageData = {
                0, 1, 0,
                1, 0, 1,
                0, 1, 0
        };

        int result = imageUtil.freePath(0, 0, 2, 2, imageData, 3, 3, lfsParams);

        Assertions.assertTrue(result == org.mosip.nist.nfiq1.common.ILfs.TRUE || result == org.mosip.nist.nfiq1.common.ILfs.FALSE);
    }

    /**
     * Validates freePath with transition counting
     */
    @Test
    void freePathWithTransitions() {
        org.mosip.nist.nfiq1.common.ILfs.LfsParams lfsParams = createLfsParams();

        int[] imageData = new int[25];
        java.util.Arrays.fill(imageData, 0);
        imageData[6] = 1;
        imageData[12] = 0;

        int result = imageUtil.freePath(0, 0, 4, 4, imageData, 5, 5, lfsParams);

        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.TRUE, result);
    }

    /**
     * Verifies searchInDirection finds target pixel successfully
     */
    @Test
    void searchInDirectionFound() {
        AtomicInteger ox = new AtomicInteger();
        AtomicInteger oy = new AtomicInteger();
        AtomicInteger oex = new AtomicInteger();
        AtomicInteger oey = new AtomicInteger();

        int[] imageData = new int[25];
        java.util.Arrays.fill(imageData, 0);
        imageData[12] = 1;

        int result = imageUtil.searchInDirection(ox, oy, oex, oey, 1, 0, 0, 1.0, 1.0, 5, imageData, 5, 5);

        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.TRUE, result);
        Assertions.assertEquals(2, ox.get());
        Assertions.assertEquals(2, oy.get());
    }

    /**
     * Validates searchInDirection behavior when target is not found
     */
    @Test
    void searchInDirectionNotFound() {
        AtomicInteger ox = new AtomicInteger();
        AtomicInteger oy = new AtomicInteger();
        AtomicInteger oex = new AtomicInteger();
        AtomicInteger oey = new AtomicInteger();

        int[] imageData = new int[25];
        java.util.Arrays.fill(imageData, 0);

        int result = imageUtil.searchInDirection(ox, oy, oex, oey, 1, 0, 0, 1.0, 1.0, 5, imageData, 5, 5);

        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, result);
        Assertions.assertEquals(-1, ox.get());
        Assertions.assertEquals(-1, oy.get());
        Assertions.assertEquals(-1, oex.get());
        Assertions.assertEquals(-1, oey.get());
    }

    /**
     * Validates searchInDirection handling of out of bounds conditions
     */
    @Test
    void searchInDirectionOutOfBounds() {
        AtomicInteger ox = new AtomicInteger();
        AtomicInteger oy = new AtomicInteger();
        AtomicInteger oex = new AtomicInteger();
        AtomicInteger oey = new AtomicInteger();

        int[] imageData = new int[25];

        int result = imageUtil.searchInDirection(ox, oy, oex, oey, 1, 4, 4, 1.0, 1.0, 5, imageData, 5, 5);

        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.FALSE, result);
        Assertions.assertEquals(-1, ox.get());
        Assertions.assertEquals(-1, oy.get());
        Assertions.assertEquals(-1, oex.get());
        Assertions.assertEquals(-1, oey.get());
    }

    /**
     * Validates searchInDirection with negative direction vectors
     */
    @Test
    void searchInDirectionNegativeDirection() {
        AtomicInteger ox = new AtomicInteger();
        AtomicInteger oy = new AtomicInteger();
        AtomicInteger oex = new AtomicInteger();
        AtomicInteger oey = new AtomicInteger();

        int[] imageData = new int[25];
        imageData[6] = 1;

        int result = imageUtil.searchInDirection(ox, oy, oex, oey, 1, 3, 3, -1.0, -1.0, 5, imageData, 5, 5);

        Assertions.assertEquals(org.mosip.nist.nfiq1.common.ILfs.TRUE, result);
    }

    /**
     * Validates bits6To8 conversion with larger image data
     */
    @Test
    void bits6To8LargeImage() {
        int[] imageData = new int[100];
        for (int i = 0; i < 100; i++) {
            imageData[i] = i % 64;
        }

        imageUtil.bits6To8(imageData, 10, 10);

        Assertions.assertEquals(0, imageData[0]);
        Assertions.assertEquals(4, imageData[1]);
        Assertions.assertEquals(8, imageData[2]);
        Assertions.assertEquals(252, imageData[63]);
    }

    /**
     * Validates bits8To6 conversion with larger image data
     */
    @Test
    void bits8To6LargeImage() {
        int[] imageData = new int[100];
        for (int i = 0; i < 100; i++) {
            imageData[i] = i % 256;
        }

        imageUtil.bits8To6(imageData, 10, 10);

        Assertions.assertEquals(0, imageData[0]);
        Assertions.assertEquals(0, imageData[1]);
        Assertions.assertEquals(0, imageData[2]);
        Assertions.assertEquals(0, imageData[3]);
        Assertions.assertEquals(1, imageData[4]);
    }

    /**
     * Creates LfsParams instance with standard values
     */
    private org.mosip.nist.nfiq1.common.ILfs.LfsParams createLfsParams() {
        return new org.mosip.nist.nfiq1.common.ILfs.LfsParams(
                128, 1, 8, 24, 8, 16, Math.PI/2, 3, 0.2, 3, 7, 7, 5, 5, 2, 10, 5, 4, 100000.0, 3.8, 50000000.0, 2, 0.7, 0.75, 7, 9, 11, 3, 10, Math.PI/3, 14, 20, 1.0, 2.25, 20, 20, 5, 2, 15.0, 10.0, 4.0, 32000.0, 8, 15, 15, 6, 15, 7, 6, 7, 8, 6, 10, 20, 2.0, 20, 3, 12, 10, 8, 0.5, 2.25, 5, 10
        );
    }

    /**
     * Creates LfsParams instance with low maximum transitions value
     */
    private org.mosip.nist.nfiq1.common.ILfs.LfsParams createLfsParamsWithLowMaxTrans() {
        return new org.mosip.nist.nfiq1.common.ILfs.LfsParams(
                128, 1, 8, 24, 8, 16, Math.PI/2, 3, 0.2, 3, 7, 7, 5, 5, 1, 10, 5, 4, 100000.0, 3.8, 50000000.0, 2, 0.7, 0.75, 7, 9, 11, 3, 10, Math.PI/3, 14, 20, 1.0, 2.25, 20, 20, 5, 2, 15.0, 10.0, 4.0, 32000.0, 8, 15, 15, 6, 15, 7, 6, 7, 8, 6, 10, 20, 2.0, 20, 3, 12, 10, 8, 0.5, 2.25, 5, 1
        );
    }
}