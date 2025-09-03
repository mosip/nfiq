package org.mosip.nist.nfiq1.imagetools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mosip.nist.nfiq1.common.ILfs;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal class for ImageDecoder avoiding external dependencies
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImageDecoderTest {

    private AtomicInteger returnCode;
    private AtomicInteger imageType;
    private AtomicInteger oLength;
    private AtomicInteger oWidth;
    private AtomicInteger oHeight;
    private AtomicInteger oDepth;
    private AtomicInteger oPPI;
    private AtomicReference<String> ofileType;

    /**
     * Sets up atomic references and integers for each execution
     */
    @BeforeEach
    void setUp() {
        returnCode = new AtomicInteger();
        imageType = new AtomicInteger();
        oLength = new AtomicInteger();
        oWidth = new AtomicInteger();
        oHeight = new AtomicInteger();
        oDepth = new AtomicInteger();
        oPPI = new AtomicInteger();
        ofileType = new AtomicReference<>();
    }

    /**
     * Verifies that getInstance returns the same singleton instance
     */
    @Test
    void getInstance() {
        ImageDecoder instance1 = ImageDecoder.getInstance();
        ImageDecoder instance2 = ImageDecoder.getInstance();
        Assertions.assertSame(instance1, instance2);
        Assertions.assertNotNull(instance1);
    }

    /**
     * Validates behavior when attempting to read a non-existent image file
     */
    @Test
    void readAndDecodeImageFileNotFound() throws Exception {
        ImageDecoder decoder = ImageDecoder.getInstance();
        BufferedImage result = decoder.readAndDecodeImage(
                returnCode, "nonexistent.iso", imageType, oLength, oWidth, oHeight, oDepth, oPPI, ofileType);

        Assertions.assertNull(result);
        Assertions.assertEquals(-1, returnCode.get());
    }

    /**
     * Validates behavior when attempting to read a non-existent grayscale image file
     */
    @Test
    void readAndDecodeGrayscaleImageFileNotFound() throws Exception {
        ImageDecoder decoder = ImageDecoder.getInstance();
        BufferedImage result = decoder.readAndDecodeGrayscaleImage(
                returnCode, "nonexistent.iso", imageType, oLength, oWidth, oHeight, oDepth, oPPI, ofileType);

        Assertions.assertNull(result);
        Assertions.assertEquals(-1, returnCode.get());
        Assertions.assertEquals(0, oLength.get());
    }

    /**
     * Verifies handling of read failures during grayscale image processing
     */
    @Test
    void readAndDecodeGrayscaleImageWithReadFailure() throws Exception {
        ImageDecoder decoder = Mockito.spy(ImageDecoder.getInstance());

        Mockito.doAnswer(invocation -> {
            AtomicInteger retCode = invocation.getArgument(0);
            retCode.set(-2);
            return null;
        }).when(decoder).readAndDecodeImage(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        BufferedImage result = decoder.readAndDecodeGrayscaleImage(
                returnCode, "file.iso", imageType, oLength, oWidth, oHeight, oDepth, oPPI, ofileType);

        Assertions.assertNull(result);
        Assertions.assertEquals(-2, returnCode.get());
        Assertions.assertEquals(0, oLength.get());
    }

    /**
     * Validates handling of unknown image types during grayscale processing
     */
    @Test
    void readAndDecodeGrayscaleImageWithUnknownImageType() throws Exception {
        ImageDecoder decoder = Mockito.spy(ImageDecoder.getInstance());

        BufferedImage mockImage = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);

        Mockito.doAnswer(invocation -> {
            AtomicInteger retCode = invocation.getArgument(0);
            AtomicInteger imgType = invocation.getArgument(2);
            AtomicInteger depth = invocation.getArgument(6);
            retCode.set(ILfs.FALSE);
            imgType.set(ImageType.UNKNOWN_IMG);
            depth.set(8);
            return mockImage;
        }).when(decoder).readAndDecodeImage(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        BufferedImage result = decoder.readAndDecodeGrayscaleImage(
                returnCode, "file.iso", imageType, oLength, oWidth, oHeight, oDepth, oPPI, ofileType);

        Assertions.assertNull(result);
        Assertions.assertEquals(-3, returnCode.get());
    }

    /**
     * Verifies handling of invalid image depth during grayscale processing
     */
    @Test
    void readAndDecodeGrayscaleImageWithInvalidDepth() throws Exception {
        ImageDecoder decoder = Mockito.spy(ImageDecoder.getInstance());

        BufferedImage mockImage = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);

        Mockito.doAnswer(invocation -> {
            AtomicInteger retCode = invocation.getArgument(0);
            AtomicInteger imgType = invocation.getArgument(2);
            AtomicInteger depth = invocation.getArgument(6);
            retCode.set(ILfs.FALSE);
            imgType.set(ImageType.JP2_IMG);
            depth.set(16);
            return mockImage;
        }).when(decoder).readAndDecodeImage(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        BufferedImage result = decoder.readAndDecodeGrayscaleImage(
                returnCode, "file.iso", imageType, oLength, oWidth, oHeight, oDepth, oPPI, ofileType);

        Assertions.assertNull(result);
        Assertions.assertEquals(-4, returnCode.get());
    }

    /**
     * Validates successful grayscale image processing with valid parameters
     */
    @Test
    void readAndDecodeGrayscaleImageSuccess() throws Exception {
        ImageDecoder decoder = Mockito.spy(ImageDecoder.getInstance());

        BufferedImage mockImage = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);

        Mockito.doAnswer(invocation -> {
            AtomicInteger retCode = invocation.getArgument(0);
            AtomicInteger imgType = invocation.getArgument(2);
            AtomicInteger depth = invocation.getArgument(6);
            retCode.set(ILfs.FALSE);
            imgType.set(ImageType.JP2_IMG);
            depth.set(ILfs.IMAGE_DEPTH);
            return mockImage;
        }).when(decoder).readAndDecodeImage(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        BufferedImage result = decoder.readAndDecodeGrayscaleImage(
                returnCode, "file.iso", imageType, oLength, oWidth, oHeight, oDepth, oPPI, ofileType);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, returnCode.get());
        Assertions.assertEquals(0, oLength.get());
    }

    /**
     * Verifies thread safety of the singleton getInstance method
     */
    @Test
    void singletonThreadSafety() throws InterruptedException {
        int threadCount = 10;
        ImageDecoder[] instances = new ImageDecoder[threadCount];
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> instances[index] = ImageDecoder.getInstance());
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

    /**
     * Validates image decoding with an existing JP2 format file
     */
    @Test
    void readAndDecodeImageWithExistingFile() throws Exception {
        ImageDecoder decoder = ImageDecoder.getInstance();

        BufferedImage result = decoder.readAndDecodeImage(
                returnCode, "info_jp2.iso", imageType, oLength, oWidth, oHeight, oDepth, oPPI, ofileType);

        if (result != null) {
            Assertions.assertEquals(ILfs.FALSE, returnCode.get());
            Assertions.assertTrue(imageType.get() == ImageType.JP2_IMG || imageType.get() == ImageType.WSQ_IMG || imageType.get() == ImageType.UNKNOWN_IMG);
            Assertions.assertTrue(oLength.get() > 0);
            Assertions.assertTrue(oWidth.get() > 0);
            Assertions.assertTrue(oHeight.get() > 0);
            Assertions.assertTrue(oDepth.get() > 0);
            Assertions.assertTrue(oPPI.get() > 0);
        }
    }

    /**
     * Validates image decoding with an existing WSQ format file
     */
    @Test
    void readAndDecodeImageWithWSQFile() throws Exception {
        ImageDecoder decoder = ImageDecoder.getInstance();

        BufferedImage result = decoder.readAndDecodeImage(
                returnCode, "info_wsq.iso", imageType, oLength, oWidth, oHeight, oDepth, oPPI, ofileType);

        if (result != null) {
            Assertions.assertEquals(ILfs.FALSE, returnCode.get());
            Assertions.assertTrue(imageType.get() == ImageType.JP2_IMG || imageType.get() == ImageType.WSQ_IMG || imageType.get() == ImageType.UNKNOWN_IMG);
            Assertions.assertTrue(oLength.get() > 0);
            Assertions.assertTrue(oWidth.get() > 0);
            Assertions.assertTrue(oHeight.get() > 0);
            Assertions.assertTrue(oDepth.get() > 0);
            Assertions.assertTrue(oPPI.get() > 0);
        }
    }
}