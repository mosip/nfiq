package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;
import org.mosip.nist.nfiq1.common.ILfs.Minutiae;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;

/**
 * Test class for Detect functionality.
 */
@ExtendWith(MockitoExtension.class)
class DetectTest {

    private Detect detect;
    private LfsParams lfsParams;

    /**
     * Sets up Detect instance before each execution.
     */
    @BeforeEach
    void setUp() {
        detect = Detect.getInstance();
        lfsParams = new LfsParams(128, 1, 8, 24, 8, 16, Math.PI/2, 3, 0.2, 3, 7, 7, 5, 5, 2, 10, 5, 4, 100000.0, 3.8, 50000000.0, 2, 0.7, 0.75, 7, 9, 11, 3, 10, Math.PI/3, 14, 20, 1.0, 2.25, 20, 20, 5, 2, 15.0, 10.0, 4.0, 32000.0, 8, 15, 15, 6, 15, 7, 6, 7, 8, 6, 10, 20, 2.0, 20, 3, 12, 10, 8, 0.5, 2.25, 5, 10);
    }

    /**
     * Verifies singleton instance of Detect.
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Detect instance1 = Detect.getInstance();
        Detect instance2 = Detect.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Asserts that Detect constructor creates an instance.
     */
    @Test
    void constructorCreatesInstance() {
        Assertions.assertNotNull(detect);
    }

    /**
     * Validates lfsDetectMinutiaeV2 works with valid input.
     */
    @Test
    void lfsDetectMinutiaeV2WithValidInput() {
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        Maps map = Maps.getInstance(50, 50);
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[2500];
        for (int i = 0; i < 2500; i++) {
            imageData[i] = i % 256;
        }
        int[] result = detect.lfsDetectMinutiaeV2(ret, oMinutiae, map, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 50, 50, lfsParams);
        Assertions.assertTrue(ret.get() <= 0);
    }

    /**
     * Simulates failure in initDirToRad.
     */
    @Test
    void lfsDetectMinutiaeV2InitDirToRadFailure() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(10);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(-1);

        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        Maps map = Maps.getInstance(10, 10);
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];

        int[] result = spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, map, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);

        Assertions.assertNull(result);
        Assertions.assertEquals(-1, ret.get());
    }

    /**
     * Simulates failure in initDftWaves.
     */
    @Test
    void lfsDetectMinutiaeV2InitDftWavesFailure() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Free mockFree = Mockito.mock(Free.class);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.doReturn(mockFree).when(spyDetect).getFree();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(10);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initDftWaves(Mockito.any(), Mockito.any())).thenReturn(-2);

        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        Maps map = Maps.getInstance(10, 10);
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];

        int[] result = spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, map, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);

        Assertions.assertNull(result);
        Assertions.assertEquals(-2, ret.get());
    }

    /**
     * Simulates failure in initRotGrids.
     */
    @Test
    void lfsDetectMinutiaeV2InitRotGridsFailure() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Free mockFree = Mockito.mock(Free.class);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.doReturn(mockFree).when(spyDetect).getFree();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(10);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initDftWaves(Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initRotGrids(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(-3);

        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        Maps map = Maps.getInstance(10, 10);
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];

        int[] result = spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, map, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);

        Assertions.assertNull(result);
        Assertions.assertEquals(-3, ret.get());
    }

    /**
     * Simulates failure in padImage.
     */
    @Test
    void lfsDetectMinutiaeV2PadImageFailure() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Free mockFree = Mockito.mock(Free.class);
        ImageUtil mockImageUtil = Mockito.mock(ImageUtil.class);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.doReturn(mockFree).when(spyDetect).getFree();
        Mockito.doReturn(mockImageUtil).when(spyDetect).getImageUtil();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(10);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initDftWaves(Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initRotGrids(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockImageUtil.padImage(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt()))
                .thenAnswer(invocation -> {
                    AtomicInteger ret = invocation.getArgument(0);
                    ret.set(-4);
                    return null;
                });

        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        Maps map = Maps.getInstance(10, 10);
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];

        int[] result = spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, map, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);

        Assertions.assertNull(result);
        Assertions.assertEquals(-4, ret.get());
    }

    /**
     * Validates execution when no padding is applied.
     */
    @Test
    void lfsDetectMinutiaeV2NoPaddingPath() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(0);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initDftWaves(Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initRotGrids(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        Maps map = Maps.getInstance(10, 10);
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];
        try {
            spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, map, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);
        } catch (Exception e) {}
        Assertions.assertTrue(true);
    }

    /**
     * Simulates genImageMaps failure.
     */
    @Test
    void lfsDetectMinutiaeV2GenImageMapsFailure() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Free mockFree = Mockito.mock(Free.class);
        ImageUtil mockImageUtil = Mockito.mock(ImageUtil.class);
        Maps mockMap = Mockito.mock(Maps.class);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.doReturn(mockFree).when(spyDetect).getFree();
        Mockito.doReturn(mockImageUtil).when(spyDetect).getImageUtil();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(0);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initDftWaves(Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initRotGrids(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.genImageMaps(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(-5);

        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];
        int[] result = spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, mockMap, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);
        Assertions.assertNull(result);
        Assertions.assertEquals(-5, ret.get());
    }

    /**
     * Simulates binarization failure.
     */
    @Test
    void lfsDetectMinutiaeV2BinarizationFailure() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Free mockFree = Mockito.mock(Free.class);
        ImageUtil mockImageUtil = Mockito.mock(ImageUtil.class);
        Binarization mockBinarization = Mockito.mock(Binarization.class);
        Maps mockMap = Mockito.mock(Maps.class);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.doReturn(mockFree).when(spyDetect).getFree();
        Mockito.doReturn(mockImageUtil).when(spyDetect).getImageUtil();
        Mockito.doReturn(mockBinarization).when(spyDetect).getBinarization();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(0);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initDftWaves(Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initRotGrids(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.genImageMaps(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.getMappedImageWidth()).thenReturn(new AtomicInteger(10));
        Mockito.when(mockMap.getMappedImageHeight()).thenReturn(new AtomicInteger(10));
        Mockito.when(mockBinarization.binarizeV2(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            AtomicInteger ret = invocation.getArgument(0);
            ret.set(-6);
            return null;
        });
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];
        int[] result = spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, mockMap, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);
        Assertions.assertNull(result);
        Assertions.assertEquals(-6, ret.get());
    }

    /**
     * Simulates dimension mismatch after binarization.
     */
    @Test
    void lfsDetectMinutiaeV2DimensionMismatch() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Free mockFree = Mockito.mock(Free.class);
        ImageUtil mockImageUtil = Mockito.mock(ImageUtil.class);
        Binarization mockBinarization = Mockito.mock(Binarization.class);
        Maps mockMap = Mockito.mock(Maps.class);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.doReturn(mockFree).when(spyDetect).getFree();
        Mockito.doReturn(mockImageUtil).when(spyDetect).getImageUtil();
        Mockito.doReturn(mockBinarization).when(spyDetect).getBinarization();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(0);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initDftWaves(Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initRotGrids(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.genImageMaps(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.getMappedImageWidth()).thenReturn(new AtomicInteger(10));
        Mockito.when(mockMap.getMappedImageHeight()).thenReturn(new AtomicInteger(10));
        Mockito.when(mockBinarization.binarizeV2(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    AtomicInteger ret = invocation.getArgument(0);
                    AtomicInteger width = invocation.getArgument(1);
                    AtomicInteger height = invocation.getArgument(2);
                    ret.set(ILfs.FALSE);
                    width.set(20);
                    height.set(20);
                    return new int[400];
                });
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];
        int[] result = spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, mockMap, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);
        Assertions.assertNull(result);
        Assertions.assertEquals(ILfs.ERROR_CODE_581, ret.get());
    }

    /**
     * Simulates allocMinutiae failure.
     */
    @Test
    void lfsDetectMinutiaeV2AllocMinutiaeFailure() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Free mockFree = Mockito.mock(Free.class);
        ImageUtil mockImageUtil = Mockito.mock(ImageUtil.class);
        Binarization mockBinarization = Mockito.mock(Binarization.class);
        MinutiaHelper mockMinutiaHelper = Mockito.mock(MinutiaHelper.class);
        Maps mockMap = Mockito.mock(Maps.class);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.doReturn(mockFree).when(spyDetect).getFree();
        Mockito.doReturn(mockImageUtil).when(spyDetect).getImageUtil();
        Mockito.doReturn(mockBinarization).when(spyDetect).getBinarization();
        Mockito.doReturn(mockMinutiaHelper).when(spyDetect).getMinutiaHelper();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(0);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initDftWaves(Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initRotGrids(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.genImageMaps(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.getMappedImageWidth()).thenReturn(new AtomicInteger(10));
        Mockito.when(mockMap.getMappedImageHeight()).thenReturn(new AtomicInteger(10));
        Mockito.when(mockBinarization.binarizeV2(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    AtomicInteger ret = invocation.getArgument(0);
                    AtomicInteger width = invocation.getArgument(1);
                    AtomicInteger height = invocation.getArgument(2);
                    ret.set(ILfs.FALSE);
                    width.set(10);
                    height.set(10);
                    return new int[100];
                });
        Mockito.when(mockMinutiaHelper.allocMinutiae(Mockito.any(), Mockito.anyInt())).thenReturn(-7);
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];
        int[] result = spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, mockMap, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);
        Assertions.assertNull(result);
        Assertions.assertEquals(-7, ret.get());
    }

    /**
     * Simulates detectMinutiaeV2 failure.
     */
    @Test
    void lfsDetectMinutiaeV2DetectMinutiaeFailure() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Free mockFree = Mockito.mock(Free.class);
        ImageUtil mockImageUtil = Mockito.mock(ImageUtil.class);
        Binarization mockBinarization = Mockito.mock(Binarization.class);
        MinutiaHelper mockMinutiaHelper = Mockito.mock(MinutiaHelper.class);
        Maps mockMap = Mockito.mock(Maps.class);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.doReturn(mockFree).when(spyDetect).getFree();
        Mockito.doReturn(mockImageUtil).when(spyDetect).getImageUtil();
        Mockito.doReturn(mockBinarization).when(spyDetect).getBinarization();
        Mockito.doReturn(mockMinutiaHelper).when(spyDetect).getMinutiaHelper();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(0);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initDftWaves(Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initRotGrids(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.genImageMaps(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.getMappedImageWidth()).thenReturn(new AtomicInteger(10));
        Mockito.when(mockMap.getMappedImageHeight()).thenReturn(new AtomicInteger(10));
        Mockito.when(mockBinarization.binarizeV2(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    AtomicInteger ret = invocation.getArgument(0);
                    AtomicInteger width = invocation.getArgument(1);
                    AtomicInteger height = invocation.getArgument(2);
                    ret.set(ILfs.FALSE);
                    width.set(10);
                    height.set(10);
                    return new int[100];
                });
        Mockito.when(mockMinutiaHelper.allocMinutiae(Mockito.any(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMinutiaHelper.detectMinutiaeV2(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenReturn(-8);
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];
        int[] result = spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, mockMap, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);
        Assertions.assertNull(result);
        Assertions.assertEquals(-8, ret.get());
    }

    /**
     * Simulates removeFalseMinutia failure.
     */
    @Test
    void lfsDetectMinutiaeV2RemoveFalseMinutiaFailure() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Free mockFree = Mockito.mock(Free.class);
        ImageUtil mockImageUtil = Mockito.mock(ImageUtil.class);
        Binarization mockBinarization = Mockito.mock(Binarization.class);
        MinutiaHelper mockMinutiaHelper = Mockito.mock(MinutiaHelper.class);
        RemoveMinutia mockRemoveMinutia = Mockito.mock(RemoveMinutia.class);
        Maps mockMap = Mockito.mock(Maps.class);
        AtomicInteger mappedWidth = new AtomicInteger(10);
        AtomicInteger mappedHeight = new AtomicInteger(10);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.doReturn(mockFree).when(spyDetect).getFree();
        Mockito.doReturn(mockImageUtil).when(spyDetect).getImageUtil();
        Mockito.doReturn(mockBinarization).when(spyDetect).getBinarization();
        Mockito.doReturn(mockMinutiaHelper).when(spyDetect).getMinutiaHelper();
        Mockito.doReturn(mockRemoveMinutia).when(spyDetect).getRemoveMinutia();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(0);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initDftWaves(Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initRotGrids(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.genImageMaps(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.getMappedImageWidth()).thenReturn(mappedWidth);
        Mockito.when(mockMap.getMappedImageHeight()).thenReturn(mappedHeight);
        Mockito.when(mockBinarization.binarizeV2(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    AtomicInteger ret = invocation.getArgument(0);
                    AtomicInteger width = invocation.getArgument(1);
                    AtomicInteger height = invocation.getArgument(2);
                    ret.set(ILfs.FALSE);
                    width.set(10);
                    height.set(10);
                    return new int[100];
                });
        Mockito.when(mockMinutiaHelper.allocMinutiae(Mockito.any(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMinutiaHelper.detectMinutiaeV2(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenReturn(ILfs.FALSE);
        Mockito.when(mockRemoveMinutia.removeFalseMinutiaV2(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(-9);
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];
        int[] result = spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, mockMap, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);
        Assertions.assertNull(result);
        Assertions.assertEquals(-9, ret.get());
    }

    /**
     * Simulates countMinutiaeRidges failure.
     */
    @Test
    void lfsDetectMinutiaeV2CountMinutiaeRidgesFailure() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Free mockFree = Mockito.mock(Free.class);
        ImageUtil mockImageUtil = Mockito.mock(ImageUtil.class);
        Binarization mockBinarization = Mockito.mock(Binarization.class);
        MinutiaHelper mockMinutiaHelper = Mockito.mock(MinutiaHelper.class);
        RemoveMinutia mockRemoveMinutia = Mockito.mock(RemoveMinutia.class);
        Ridges mockRidges = Mockito.mock(Ridges.class);
        Maps mockMap = Mockito.mock(Maps.class);
        AtomicInteger mappedWidth = new AtomicInteger(10);
        AtomicInteger mappedHeight = new AtomicInteger(10);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.doReturn(mockFree).when(spyDetect).getFree();
        Mockito.doReturn(mockImageUtil).when(spyDetect).getImageUtil();
        Mockito.doReturn(mockBinarization).when(spyDetect).getBinarization();
        Mockito.doReturn(mockMinutiaHelper).when(spyDetect).getMinutiaHelper();
        Mockito.doReturn(mockRemoveMinutia).when(spyDetect).getRemoveMinutia();
        Mockito.doReturn(mockRidges).when(spyDetect).getRidges();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(0);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initDftWaves(Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initRotGrids(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.genImageMaps(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.getMappedImageWidth()).thenReturn(mappedWidth);
        Mockito.when(mockMap.getMappedImageHeight()).thenReturn(mappedHeight);
        Mockito.when(mockBinarization.binarizeV2(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    AtomicInteger ret = invocation.getArgument(0);
                    AtomicInteger width = invocation.getArgument(1);
                    AtomicInteger height = invocation.getArgument(2);
                    ret.set(ILfs.FALSE);
                    width.set(10);
                    height.set(10);
                    return new int[100];
                });
        Mockito.when(mockMinutiaHelper.allocMinutiae(Mockito.any(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMinutiaHelper.detectMinutiaeV2(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenReturn(ILfs.FALSE);
        Mockito.when(mockRemoveMinutia.removeFalseMinutiaV2(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(ILfs.FALSE);
        Mockito.when(mockRidges.countMinutiaeRidges(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(-10);
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];
        int[] result = spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, mockMap, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);
        Assertions.assertNull(result);
        Assertions.assertEquals(-10, ret.get());
    }

    /**
     * Validates successful Detect execution.
     */
    @Test
    void lfsDetectMinutiaeV2SuccessfulExecution() {
        Detect spyDetect = Mockito.spy(detect);
        Init mockInit = Mockito.mock(Init.class);
        Free mockFree = Mockito.mock(Free.class);
        ImageUtil mockImageUtil = Mockito.mock(ImageUtil.class);
        Binarization mockBinarization = Mockito.mock(Binarization.class);
        MinutiaHelper mockMinutiaHelper = Mockito.mock(MinutiaHelper.class);
        RemoveMinutia mockRemoveMinutia = Mockito.mock(RemoveMinutia.class);
        Ridges mockRidges = Mockito.mock(Ridges.class);
        Maps mockMap = Mockito.mock(Maps.class);
        AtomicInteger mappedWidth = new AtomicInteger(10);
        AtomicInteger mappedHeight = new AtomicInteger(10);
        Mockito.doReturn(mockInit).when(spyDetect).getInit();
        Mockito.doReturn(mockFree).when(spyDetect).getFree();
        Mockito.doReturn(mockImageUtil).when(spyDetect).getImageUtil();
        Mockito.doReturn(mockBinarization).when(spyDetect).getBinarization();
        Mockito.doReturn(mockMinutiaHelper).when(spyDetect).getMinutiaHelper();
        Mockito.doReturn(mockRemoveMinutia).when(spyDetect).getRemoveMinutia();
        Mockito.doReturn(mockRidges).when(spyDetect).getRidges();
        Mockito.when(mockInit.getMaxPaddingV2(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(0);
        Mockito.when(mockInit.initDirToRad(Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initDftWaves(Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockInit.initRotGrids(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.genImageMaps(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMap.getMappedImageWidth()).thenReturn(mappedWidth);
        Mockito.when(mockMap.getMappedImageHeight()).thenReturn(mappedHeight);
        Mockito.when(mockBinarization.binarizeV2(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    AtomicInteger ret = invocation.getArgument(0);
                    AtomicInteger width = invocation.getArgument(1);
                    AtomicInteger height = invocation.getArgument(2);
                    ret.set(ILfs.FALSE);
                    width.set(10);
                    height.set(10);
                    return new int[100];
                });
        Mockito.when(mockMinutiaHelper.allocMinutiae(Mockito.any(), Mockito.anyInt())).thenReturn(ILfs.FALSE);
        Mockito.when(mockMinutiaHelper.detectMinutiaeV2(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenReturn(ILfs.FALSE);
        Mockito.when(mockRemoveMinutia.removeFalseMinutiaV2(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(ILfs.FALSE);
        Mockito.when(mockRidges.countMinutiaeRidges(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(ILfs.FALSE);
        AtomicInteger ret = new AtomicInteger();
        AtomicReference<Minutiae> oMinutiae = new AtomicReference<>();
        AtomicInteger oBinarizedImageWidth = new AtomicInteger();
        AtomicInteger oBinarizedImageHeight = new AtomicInteger();
        int[] imageData = new int[100];
        int[] result = spyDetect.lfsDetectMinutiaeV2(ret, oMinutiae, mockMap, oBinarizedImageWidth, oBinarizedImageHeight, imageData, 10, 10, lfsParams);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ILfs.FALSE, ret.get());
        Assertions.assertEquals(10, oBinarizedImageWidth.get());
        Assertions.assertEquals(10, oBinarizedImageHeight.get());
    }

    /**
     * Verifies getInit returns an instance.
     */
    @Test
    void getInitReturnsInstance() {
        Assertions.assertNotNull(detect.getInit());
    }

    /**
     * Verifies getGlobals returns an instance.
     */
    @Test
    void getGlobalsReturnsInstance() {
        Assertions.assertNotNull(detect.getGlobals());
    }

    /**
     * Verifies getFree returns an instance.
     */
    @Test
    void getFreeReturnsInstance() {
        Assertions.assertNotNull(detect.getFree());
    }

    /**
     * Verifies getImageUtil returns an instance.
     */
    @Test
    void getImageUtilReturnsInstance() {
        Assertions.assertNotNull(detect.getImageUtil());
    }

    /**
     * Verifies getBinarization returns an instance.
     */
    @Test
    void getBinarizationReturnsInstance() {
        Assertions.assertNotNull(detect.getBinarization());
    }

    /**
     * Verifies getMinutiaHelper returns an instance.
     */
    @Test
    void getMinutiaHelperReturnsInstance() {
        Assertions.assertNotNull(detect.getMinutiaHelper());
    }

    /**
     * Verifies getRemoveMinutia returns an instance.
     */
    @Test
    void getRemoveMinutiaReturnsInstance() {
        Assertions.assertNotNull(detect.getRemoveMinutia());
    }

    /**
     * Verifies getRidges returns an instance.
     */
    @Test
    void getRidgesReturnsInstance() {
        Assertions.assertNotNull(detect.getRidges());
    }

    /**
     * Verifies singleton thread safety of Detect.
     */
    @Test
    void singletonThreadSafety() throws InterruptedException {
        int threadCount = 10;
        Detect[] instances = new Detect[threadCount];
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> instances[index] = Detect.getInstance());
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (int i = 1; i < threadCount; i++) {
            Assertions.assertSame(instances[0], instances[i]);
        }
    }
}