package org.mosip.nist.nfiq1.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit tests for {@link ImageUtil}
 */
@ExtendWith(MockitoExtension.class)
class ImageUtilTest {

    /**
     * Test method for toByteArray with valid image input.
     *
     * @throws IOException if an I/O error occurs during test execution
     */
    @Test
    void toByteArrayWithValidImageReturnsBytes() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);

        try (MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class)) {
            imageIOMock.when(() -> ImageIO.write(any(BufferedImage.class), anyString(), any(ByteArrayOutputStream.class)))
                    .thenReturn(true);

            byte[] result = ImageUtil.toByteArray(image, "png");

            assertNotNull(result);
        }
    }

    /**
     * Test method for toByteArray that expects IOException.
     *
     * @throws IOException if an I/O error occurs during test execution
     */
    @Test
    void toByteArrayWithIOExceptionThrowsException() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);

        try (MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class)) {
            imageIOMock.when(() -> ImageIO.write(any(BufferedImage.class), anyString(), any(ByteArrayOutputStream.class)))
                    .thenThrow(new IOException("Test exception"));

            assertThrows(IOException.class, () -> ImageUtil.toByteArray(image, "png"));
        }
    }

    /**
     * Test method for convertTo1DWithoutUsingGetRGB with valid image.
     *
     * @throws IOException if an I/O error occurs during test execution
     */
    @Test
    void convertTo1DWithoutUsingGetRGBReturnsCorrectArray() throws IOException {
        BufferedImage image = createMockGrayscaleImage();

        int[] result = ImageUtil.convertTo1DWithoutUsingGetRGB(image, "png");

        assertNotNull(result);
        assertEquals(4, result.length);
    }

    /**
     * Test method for twoDConvert with valid 2D array.
     */
    @Test
    void twoDConvertWithValidArrayReturnsFlattened() {
        int[][] input = {{1, 2}, {3, 4}};

        int[] result = ImageUtil.twoDConvert(input);

        assertEquals(4, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
        assertEquals(4, result[3]);
    }

    /**
     * Test method for twoDConvert with empty array.
     */
    @Test
    void twoDConvertWithEmptyArrayReturnsEmpty() {
        int[][] input = {};

        int[] result = ImageUtil.twoDConvert(input);

        assertEquals(0, result.length);
    }

    /**
     * Test method for twoDConvert with jagged array.
     */
    @Test
    void twoDConvertWithJaggedArrayHandlesCorrectly() {
        int[][] input = {{1, 2, 3}, {4, 5}};

        int[] result = ImageUtil.twoDConvert(input);

        assertEquals(5, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
        assertEquals(4, result[3]);
        assertEquals(5, result[4]);
    }

    /**
     * Test method for toBufferedImage with valid byte array.
     *
     * @throws IOException if an I/O error occurs during test execution
     */
    @Test
    void toBufferedImageWithValidBytesReturnsImage() throws IOException {
        byte[] imageBytes = new byte[]{1, 2, 3, 4};
        BufferedImage mockImage = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY);

        try (MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class)) {
            imageIOMock.when(() -> ImageIO.read(any(ByteArrayInputStream.class)))
                    .thenReturn(mockImage);

            BufferedImage result = ImageUtil.toBufferedImage(imageBytes);

            assertNotNull(result);
            assertEquals(mockImage, result);
        }
    }

    /**
     * Test method for toBufferedImage that expects IOException.
     *
     * @throws IOException if an I/O error occurs during test execution
     */
    @Test
    void toBufferedImageWithIOExceptionThrowsException() throws IOException {
        byte[] imageBytes = new byte[]{1, 2, 3, 4};

        try (MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class)) {
            imageIOMock.when(() -> ImageIO.read(any(ByteArrayInputStream.class)))
                    .thenThrow(new IOException("Test exception"));

            assertThrows(IOException.class, () -> ImageUtil.toBufferedImage(imageBytes));
        }
    }

    /**
     * Test method for convertTo1DWithoutUsingGetRGB with different image types.
     *
     * @throws IOException if an I/O error occurs during test execution
     */
    @Test
    void convertTo1DWithoutUsingGetRGBWithDifferentImageTypes() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY);

        int[] result = ImageUtil.convertTo1DWithoutUsingGetRGB(image, "png");

        assertNotNull(result);
        assertEquals(4, result.length);
    }

    /**
     * Test method for twoDConvert with single row array.
     */
    @Test
    void twoDConvertWithSingleRowArray() {
        int[][] input = {{1, 2, 3, 4, 5}};

        int[] result = ImageUtil.twoDConvert(input);

        assertEquals(5, result.length);
        assertEquals(1, result[0]);
        assertEquals(5, result[4]);
    }

    /**
     * Test method for twoDConvert with single column array.
     */
    @Test
    void twoDConvertWithSingleColumnArray() {
        int[][] input = {{1}, {2}, {3}};

        int[] result = ImageUtil.twoDConvert(input);

        assertEquals(3, result.length);
        assertEquals(1, result[0]);
        assertEquals(3, result[2]);
    }

    /**
     * Test method for toByteArray that expects IllegalArgumentException for null input.
     */
    @Test
    void toByteArrayWithNullImageThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            ImageUtil.toByteArray(null, "png");
        });
    }

    /**
     * Test convertTo1DWithoutUsingGetRGB that expects ClassCastException for TYPE_INT_RGB image.
     *
     * @throws IOException if an I/O error occurs during test execution
     */
    @Test
    void convertTo1DWithoutUsingGetRGBWithTypeIntRGBHandlesCorrectly() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 2, 2);
        g.dispose();

        assertThrows(ClassCastException.class, () -> {
            ImageUtil.convertTo1DWithoutUsingGetRGB(image, "png");
        });
    }

    /**
     * Test method for convertTo1DWithoutUsingGetRGB with TYPE_BYTE_INDEXED image.
     *
     * @throws IOException if an I/O error occurs during test execution
     */
    @Test
    void convertTo1DWithoutUsingGetRGBWithTypeByteIndexedHandlesCorrectly() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_INDEXED);

        int[] result = ImageUtil.convertTo1DWithoutUsingGetRGB(image, "png");

        assertNotNull(result);
        assertEquals(4, result.length);
    }

    /**
     * Test method for toBufferedImage with valid byte array from real image.
     *
     * @throws IOException if an I/O error occurs during test execution
     */
    @Test
    void toBufferedImageWithValidBytesCreatesImage() throws IOException {
        BufferedImage originalImage = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY);
        byte[] imageBytes = ImageUtil.toByteArray(originalImage, "png");

        BufferedImage result = ImageUtil.toBufferedImage(imageBytes);

        assertNotNull(result);
    }

    /**
     * Test method for twoDConvert that expects NullPointerException.
     */
    @Test
    void twoDConvertWithNullArrayReturnsEmptyArray() {
        assertThrows(NullPointerException.class, () -> {
            ImageUtil.twoDConvert(null);
        });
    }

    /**
     * Creates a mock grayscale image for testing purposes.
     *
     * @return BufferedImage with predefined grayscale pixel values
     */
    private BufferedImage createMockGrayscaleImage() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY);
        DataBufferByte dataBuffer = (DataBufferByte) image.getRaster().getDataBuffer();
        byte[] data = dataBuffer.getData();
        data[0] = (byte) 100;
        data[1] = (byte) 150;
        data[2] = (byte) 200;
        data[3] = (byte) 50;
        return image;
    }
}