package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;

/**
 * Test class for GetMinutiae functionality
 */
@ExtendWith(MockitoExtension.class)
class GetMinutiaeTest {

    private GetMinutiae getMinutiae;
    private LfsParams lfsParams;

    /**
     * Sets up GetMinutiae instance and LfsParams before each execution
     */
    @BeforeEach
    void setUp() {
        getMinutiae = GetMinutiae.getInstance();
        lfsParams = new LfsParams(128, 1, 8, 24, 8, 16, Math.PI/2, 3, 0.2, 3, 7, 7, 5, 5, 2, 10, 5, 4, 100000.0, 3.8, 50000000.0, 2, 0.7, 0.75, 7, 9, 11, 3, 10, Math.PI/3, 14, 20, 1.0, 2.25, 20, 20, 5, 2, 15.0, 10.0, 4.0, 32000.0, 8, 15, 15, 6, 15, 7, 6, 7, 8, 6, 10, 20, 2.0, 20, 3, 12, 10, 8, 0.5, 2.25, 5, 10);
    }

    /**
     * Verifies that getInstance returns the same singleton instance
     */
    @Test
    void getInstanceReturnsSameInstance() {
        GetMinutiae instance1 = GetMinutiae.getInstance();
        GetMinutiae instance2 = GetMinutiae.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Validates getMinutiae with valid input parameters
     */
    @Test
    void getMinutiaeWithValidInput() {
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        Maps imageMap = Maps.getInstance(50, 50);
        Quality qualityMap = Quality.getInstance();
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        AtomicInteger oBinarizedImageDepth = new AtomicInteger();
        int[] imageData = new int[2500];
        for (int i = 0; i < 2500; i++) {
            imageData[i] = i % 256;
        }

        int[] result = getMinutiae.getMinutiae(ret, oMinutiae, imageMap, qualityMap,
                oBinarizedImageWidth, oBinarizedImageHeight, oBinarizedImageDepth,
                imageData, 50, 50, 8, 500.0, lfsParams);

        Assertions.assertTrue(ret.get() <= 0);
    }

    /**
     * Validates that getMinutiae returns error for invalid image depth
     */
    @Test
    void getMinutiaeWithInvalidImageDepthReturnsError() {
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        Maps imageMap = Maps.getInstance(10, 10);
        Quality qualityMap = Quality.getInstance();
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        AtomicInteger oBinarizedImageDepth = new AtomicInteger();
        int[] imageData = new int[100];

        int[] result = getMinutiae.getMinutiae(ret, oMinutiae, imageMap, qualityMap,
                oBinarizedImageWidth, oBinarizedImageHeight, oBinarizedImageDepth,
                imageData, 10, 10, 16, 500.0, lfsParams);

        Assertions.assertEquals(ILfs.ERROR_CODE_02, ret.get());
    }

    /**
     * Validates that getMinutiaHelper returns a non-null instance
     */
    @Test
    void getMinutiaHelperReturnsInstance() {
        Assertions.assertNotNull(getMinutiae.getMinutiaHelper());
    }

    /**
     * Validates that getDetect returns a non-null instance
     */
    @Test
    void getDetectReturnsInstance() {
        Assertions.assertNotNull(getMinutiae.getDetect());
    }

    /**
     * Validates handling of quality map generation error
     */
    @Test
    void getMinutiaeQualityMapGenerationError() {
        GetMinutiae spyGetMinutiae = Mockito.spy(getMinutiae);
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        Maps imageMap = Maps.getInstance(10, 10);
        Quality mockQualityMap = Mockito.mock(Quality.class);
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        AtomicInteger oBinarizedImageDepth = new AtomicInteger();
        int[] imageData = new int[100];
        for (int i = 0; i < 100; i++) {
            imageData[i] = i % 256;
        }

        Detect mockDetect = Mockito.mock(Detect.class);
        Mockito.doReturn(mockDetect).when(spyGetMinutiae).getDetect();

        int[] mockBinarizedData = new int[100];
        Mockito.when(mockDetect.lfsDetectMinutiaeV2(
                        Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenAnswer(invocation -> {
                    AtomicInteger retArg = invocation.getArgument(0);
                    retArg.set(ILfs.FALSE);
                    return mockBinarizedData;
                });

        Mockito.when(mockQualityMap.generateQualityMap(Mockito.any()))
                .thenReturn(-1);

        MinutiaHelper mockMinutiaHelper = Mockito.mock(MinutiaHelper.class);
        Mockito.doReturn(mockMinutiaHelper).when(spyGetMinutiae).getMinutiaHelper();

        int[] result = spyGetMinutiae.getMinutiae(ret, oMinutiae, imageMap, mockQualityMap,
                oBinarizedImageWidth, oBinarizedImageHeight, oBinarizedImageDepth,
                imageData, 10, 10, 8, 500.0, lfsParams);

        Assertions.assertEquals(-1, ret.get());
        Assertions.assertEquals(null, result);

        Mockito.verify(mockMinutiaHelper).freeMinutiae(oMinutiae);
    }

    /**
     * Validates handling of combined minutia quality error
     */
    @Test
    void getMinutiaeCombinedQualityError() {
        GetMinutiae spyGetMinutiae = Mockito.spy(getMinutiae);
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        Maps imageMap = Maps.getInstance(10, 10);
        Quality mockQualityMap = Mockito.mock(Quality.class);
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        AtomicInteger oBinarizedImageDepth = new AtomicInteger();
        int[] imageData = new int[100];
        for (int i = 0; i < 100; i++) {
            imageData[i] = i % 256;
        }

        Detect mockDetect = Mockito.mock(Detect.class);
        Mockito.doReturn(mockDetect).when(spyGetMinutiae).getDetect();

        int[] mockBinarizedData = new int[100];
        Mockito.when(mockDetect.lfsDetectMinutiaeV2(
                        Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenAnswer(invocation -> {
                    AtomicInteger retArg = invocation.getArgument(0);
                    retArg.set(ILfs.FALSE);
                    return mockBinarizedData;
                });

        Mockito.when(mockQualityMap.generateQualityMap(Mockito.any()))
                .thenReturn(ILfs.FALSE);

        Mockito.when(mockQualityMap.combinedMinutiaQuality(
                        Mockito.any(), Mockito.any(), Mockito.anyInt(),
                        Mockito.any(), Mockito.anyInt(), Mockito.anyInt(),
                        Mockito.anyInt(), Mockito.anyDouble()))
                .thenReturn(-2);

        MinutiaHelper mockMinutiaHelper = Mockito.mock(MinutiaHelper.class);
        Mockito.doReturn(mockMinutiaHelper).when(spyGetMinutiae).getMinutiaHelper();

        int[] result = spyGetMinutiae.getMinutiae(ret, oMinutiae, imageMap, mockQualityMap,
                oBinarizedImageWidth, oBinarizedImageHeight, oBinarizedImageDepth,
                imageData, 10, 10, 8, 500.0, lfsParams);

        Assertions.assertEquals(-2, ret.get());
        Assertions.assertEquals(null, result);

        Mockito.verify(mockMinutiaHelper).freeMinutiae(oMinutiae);
    }

    /**
     * Validates successful execution of getMinutiae method
     */
    @Test
    void getMinutiaeSuccessfulExecution() {
        GetMinutiae spyGetMinutiae = Mockito.spy(getMinutiae);
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        Maps imageMap = Maps.getInstance(10, 10);
        Quality mockQualityMap = Mockito.mock(Quality.class);
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        AtomicInteger oBinarizedImageDepth = new AtomicInteger();
        int[] imageData = new int[100];
        for (int i = 0; i < 100; i++) {
            imageData[i] = i % 256;
        }

        Detect mockDetect = Mockito.mock(Detect.class);
        Mockito.doReturn(mockDetect).when(spyGetMinutiae).getDetect();

        int[] mockBinarizedData = new int[100];
        Mockito.when(mockDetect.lfsDetectMinutiaeV2(
                        Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenAnswer(invocation -> {
                    AtomicInteger retArg = invocation.getArgument(0);
                    retArg.set(ILfs.FALSE);
                    return mockBinarizedData;
                });

        Mockito.when(mockQualityMap.generateQualityMap(Mockito.any()))
                .thenReturn(ILfs.FALSE);

        Mockito.when(mockQualityMap.combinedMinutiaQuality(
                        Mockito.any(), Mockito.any(), Mockito.anyInt(),
                        Mockito.any(), Mockito.anyInt(), Mockito.anyInt(),
                        Mockito.anyInt(), Mockito.anyDouble()))
                .thenReturn(ILfs.FALSE);

        int[] result = spyGetMinutiae.getMinutiae(ret, oMinutiae, imageMap, mockQualityMap,
                oBinarizedImageWidth, oBinarizedImageHeight, oBinarizedImageDepth,
                imageData, 10, 10, 8, 500.0, lfsParams);

        Assertions.assertEquals(ILfs.FALSE, ret.get());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(8, oBinarizedImageDepth.get());
    }
}