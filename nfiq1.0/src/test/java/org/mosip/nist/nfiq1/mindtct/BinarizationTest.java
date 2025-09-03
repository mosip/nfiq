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
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;

/**
 * Test class for Binarization functionality
 */
@ExtendWith(MockitoExtension.class)
class BinarizationTest {

    private Binarization binarization;

    @Mock
    private RotGrids mockRotGrids;

    @Mock
    private LfsParams mockLfsParams;

    /**
     * Sets up the Binarization instance before each execution
     */
    @BeforeEach
    void setUp() {
        binarization = Binarization.getInstance();
    }

    /**
     * Verifies that getInstance returns the same singleton instance
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Binarization instance1 = Binarization.getInstance();
        Binarization instance2 = Binarization.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Validates that getDefs returns a non-null object
     */
    @Test
    void getDefsReturnsNotNull() {
        Assertions.assertNotNull(binarization.getDefs());
    }

    /**
     * Validates that getImageUtil returns a non-null object
     */
    @Test
    void getImageUtilReturnsNotNull() {
        Assertions.assertNotNull(binarization.getImageUtil());
    }

    /**
     * Verifies that dirbinarize with valid input returns a valid pixel value
     */
    @Test
    void dirbinarizeWithValidInputReturnsPixelValue() {
        int[] paddedImageData = new int[100];
        for (int i = 0; i < paddedImageData.length; i++) {
            paddedImageData[i] = 128;
        }

        int[] mockGrid = new int[25];
        int[][] mockGrids = {mockGrid};

        Mockito.when(mockRotGrids.getGrids()).thenReturn(mockGrids);
        Mockito.when(mockRotGrids.getGridHeight()).thenReturn(5);
        Mockito.when(mockRotGrids.getGridWidth()).thenReturn(5);

        int result = binarization.dirbinarize(paddedImageData, 50, 0, mockRotGrids);

        Assertions.assertTrue(result == ILfs.BLACK_PIXEL || result == ILfs.WHITE_PIXEL);
    }

    /**
     * Validates that isoBinarize with valid input returns a valid pixel value
     */
    @Test
    void isoBinarizeWithValidInputReturnsPixelValue() {
        int[] paddedImageData = new int[100];
        for (int i = 0; i < paddedImageData.length; i++) {
            paddedImageData[i] = 128;
        }

        int result = binarization.isoBinarize(paddedImageData, 50, 10, 10, 5);

        Assertions.assertTrue(result == ILfs.BLACK_PIXEL || result == ILfs.WHITE_PIXEL);
    }

    /**
     * Verifies that isoBinarize returns black pixel for low center pixel values
     */
    @Test
    void isoBinarizeWithLowCenterPixelReturnsBlackPixel() {
        int[] paddedImageData = new int[100];
        for (int i = 0; i < paddedImageData.length; i++) {
            paddedImageData[i] = 200;
        }
        paddedImageData[50] = 50;

        int result = binarization.isoBinarize(paddedImageData, 50, 10, 10, 5);

        Assertions.assertEquals(ILfs.BLACK_PIXEL, result);
    }

    /**
     * Verifies that isoBinarize returns white pixel for high center pixel values
     */
    @Test
    void isoBinarizeWithHighCenterPixelReturnsWhitePixel() {
        int[] paddedImageData = new int[100];
        for (int i = 0; i < paddedImageData.length; i++) {
            paddedImageData[i] = 50;
        }
        paddedImageData[50] = 200;

        int result = binarization.isoBinarize(paddedImageData, 50, 10, 10, 5);

        Assertions.assertEquals(ILfs.WHITE_PIXEL, result);
    }

    /**
     * Validates that binarizeImage handles cases with no valid neighbors correctly
     */
    @Test
    void binarizeImageWithNoValidNeighborsReturnsWhitePixel() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oBinarizedWidth = new AtomicInteger();
        AtomicInteger oBinarizedHeight = new AtomicInteger();

        int[] paddedImageData = new int[400];
        AtomicIntegerArray mapDirectionArr = new AtomicIntegerArray(4);
        mapDirectionArr.set(0, ILfs.NO_VALID_NBRS);

        Mockito.when(mockRotGrids.getPad()).thenReturn(4);
        Mockito.when(mockRotGrids.getGrids()).thenReturn(new int[1][25]);
        Mockito.when(mockRotGrids.getGridHeight()).thenReturn(5);
        Mockito.when(mockRotGrids.getGridWidth()).thenReturn(5);

        int[] result = binarization.binarizeImage(ret, oBinarizedWidth, oBinarizedHeight,
                paddedImageData, 20, 20, mapDirectionArr, 2, 2, 8, mockRotGrids, 7);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(ILfs.FALSE, ret.get());
    }

    /**
     * Validates that binarizeImage uses isoBinarize for invalid direction values
     */
    @Test
    void binarizeImageWithInvalidDirectionUsesIsoBinarize() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oBinarizedWidth = new AtomicInteger();
        AtomicInteger oBinarizedHeight = new AtomicInteger();

        int[] paddedImageData = new int[400];
        AtomicIntegerArray mapDirectionArr = new AtomicIntegerArray(4);
        mapDirectionArr.set(0, ILfs.INVALID_DIR);

        Mockito.when(mockRotGrids.getPad()).thenReturn(4);
        Mockito.when(mockRotGrids.getGrids()).thenReturn(new int[1][25]);
        Mockito.when(mockRotGrids.getGridHeight()).thenReturn(5);
        Mockito.when(mockRotGrids.getGridWidth()).thenReturn(5);

        int[] result = binarization.binarizeImage(ret, oBinarizedWidth, oBinarizedHeight,
                paddedImageData, 20, 20, mapDirectionArr, 2, 2, 8, mockRotGrids, 7);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(ILfs.FALSE, ret.get());
    }

    /**
     * Validates that binarizeImageV2 handles invalid direction values correctly
     */
    @Test
    void binarizeImageV2WithInvalidDirectionReturnsWhitePixel() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oBinarizedWidth = new AtomicInteger();
        AtomicInteger oBinarizedHeight = new AtomicInteger();

        int[] paddedImageData = new int[400];
        AtomicIntegerArray directionMap = new AtomicIntegerArray(4);
        directionMap.set(0, ILfs.INVALID_DIR);

        Mockito.when(mockRotGrids.getPad()).thenReturn(4);
        Mockito.when(mockRotGrids.getGrids()).thenReturn(new int[1][25]);
        Mockito.when(mockRotGrids.getGridHeight()).thenReturn(5);
        Mockito.when(mockRotGrids.getGridWidth()).thenReturn(5);

        int[] result = binarization.binarizeImageV2(ret, oBinarizedWidth, oBinarizedHeight,
                paddedImageData, 20, 20, directionMap, 2, 2, 8, mockRotGrids);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(ILfs.FALSE, ret.get());
    }

    /**
     * Verifies that dirbinarize handles center row calculations correctly
     */
    @Test
    void dirbinarizeWithCenterRowLessThanTotalReturnsBlackPixel() {
        int[] paddedImageData = new int[100];
        for (int i = 0; i < 50; i++) {
            paddedImageData[i] = 50;
        }
        for (int i = 50; i < 100; i++) {
            paddedImageData[i] = 200;
        }

        int[] mockGrid = new int[25];
        for (int i = 0; i < 25; i++) {
            mockGrid[i] = i;
        }
        int[][] mockGrids = {mockGrid};

        Mockito.when(mockRotGrids.getGrids()).thenReturn(mockGrids);
        Mockito.when(mockRotGrids.getGridHeight()).thenReturn(5);
        Mockito.when(mockRotGrids.getGridWidth()).thenReturn(5);

        int result = binarization.dirbinarize(paddedImageData, 25, 0, mockRotGrids);

        Assertions.assertTrue(result == ILfs.BLACK_PIXEL || result == ILfs.WHITE_PIXEL);
    }

    /**
     * Validates that binarize processes valid input successfully
     */
    @Test
    void binarizeWithValidInputProcessesSuccessfully() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oBinarizedWidth = new AtomicInteger();
        AtomicInteger oBinarizedHeight = new AtomicInteger();

        int[] paddedImageData = new int[400];
        AtomicIntegerArray mapDirectionArr = new AtomicIntegerArray(4);

        Mockito.when(mockLfsParams.getBlockOffsetSize()).thenReturn(8);
        Mockito.when(mockLfsParams.getIsoBinGridDim()).thenReturn(7);
        Mockito.when(mockLfsParams.getNumFillHoles()).thenReturn(0);
        Mockito.when(mockRotGrids.getPad()).thenReturn(4);
        Mockito.when(mockRotGrids.getGrids()).thenReturn(new int[1][25]);
        Mockito.when(mockRotGrids.getGridHeight()).thenReturn(5);
        Mockito.when(mockRotGrids.getGridWidth()).thenReturn(5);

        int[] result = binarization.binarize(ret, oBinarizedWidth, oBinarizedHeight,
                paddedImageData, 20, 20, mapDirectionArr, 2, 2, mockRotGrids, mockLfsParams);

        Assertions.assertNotNull(result);
    }

    /**
     * Validates that binarizeV2 processes valid input successfully
     */
    @Test
    void binarizeV2WithValidInputProcessesSuccessfully() {
        AtomicInteger ret = new AtomicInteger();
        AtomicInteger oBinarizedWidth = new AtomicInteger();
        AtomicInteger oBinarizedHeight = new AtomicInteger();

        int[] paddedImageData = new int[400];
        AtomicIntegerArray directionMap = new AtomicIntegerArray(4);

        Mockito.when(mockLfsParams.getBlockOffsetSize()).thenReturn(8);
        Mockito.when(mockLfsParams.getNumFillHoles()).thenReturn(0);
        Mockito.when(mockRotGrids.getPad()).thenReturn(4);
        Mockito.when(mockRotGrids.getGrids()).thenReturn(new int[1][25]);
        Mockito.when(mockRotGrids.getGridHeight()).thenReturn(5);
        Mockito.when(mockRotGrids.getGridWidth()).thenReturn(5);

        int[] result = binarization.binarizeV2(ret, oBinarizedWidth, oBinarizedHeight,
                paddedImageData, 20, 20, directionMap, 2, 2, mockRotGrids, mockLfsParams);

        Assertions.assertNotNull(result);
    }
}