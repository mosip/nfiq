package org.mosip.nist.nfiq1.imagetools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test class for ImageType functionality
 */
@ExtendWith(MockitoExtension.class)
class ImageTypeTest {

    private ImageType imageType;
    private AtomicInteger imageTypeResult;

    /**
     * Sets up ImageType instance and AtomicInteger before each test execution
     */
    @BeforeEach
    void setUp() {
        imageType = ImageType.getInstance();
        imageTypeResult = new AtomicInteger();
    }

    /**
     * Verifies that getInstance returns the same singleton instance
     */
    @Test
    void getInstanceReturnsSameInstance() {
        ImageType instance1 = ImageType.getInstance();
        ImageType instance2 = ImageType.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Validates that WSQ format data is correctly identified as WSQ type
     */
    @Test
    void getImageTypeWithWSQDataReturnsWSQType() {
        byte[] wsqData = {(byte) 0xFF, (byte) 0xA0, 0x00, 0x00, (byte) 0xFF, (byte) 0xA1};

        int result = imageType.getImageType(imageTypeResult, wsqData, wsqData.length);

        Assertions.assertEquals(0, result);
        Assertions.assertEquals(ImageType.WSQ_IMG, imageTypeResult.get());
    }

    /**
     * Validates that JPEG2000 format data is correctly identified as JP2 type
     */
    @Test
    void getImageTypeWithJP2000DataReturnsJP2Type() {
        byte[] jp2Data = {0x00, 0x00, 0x00, 0x0C, 'j', 'P', ' ', ' '};

        int result = imageType.getImageType(imageTypeResult, jp2Data, jp2Data.length);

        Assertions.assertEquals(0, result);
        Assertions.assertEquals(ImageType.JP2_IMG, imageTypeResult.get());
    }

    /**
     * Validates that unrecognized data format is identified as unknown type
     */
    @Test
    void getImageTypeWithUnknownDataReturnsUnknownType() {
        byte[] unknownData = {0x01, 0x02, 0x03, 0x04};

        int result = imageType.getImageType(imageTypeResult, unknownData, unknownData.length);

        Assertions.assertEquals(-1, result);
        Assertions.assertEquals(ImageType.UNKNOWN_IMG, imageTypeResult.get());
    }

    /**
     * Verifies that valid WSQ data is correctly recognized by isWSQ method
     */
    @Test
    void isWSQWithValidWSQDataReturnsOne() {
        byte[] wsqData = {(byte) 0xFF, (byte) 0xA0, 0x00, 0x00, (byte) 0xFF, (byte) 0xA1};

        int result = imageType.isWSQ(wsqData, wsqData.length);

        Assertions.assertEquals(1, result);
    }

    /**
     * Verifies that invalid data is correctly rejected by isWSQ method
     */
    @Test
    void isWSQWithInvalidDataReturnsZero() {
        byte[] invalidData = {0x01, 0x02, 0x03, 0x04};

        int result = imageType.isWSQ(invalidData, invalidData.length);

        Assertions.assertEquals(0, result);
    }

    /**
     * Verifies that valid JPEG2000 data is correctly recognized by isJP2000 method
     */
    @Test
    void isJP2000WithValidJP2DataReturnsOne() {
        byte[] jp2Data = {0x00, 0x00, 0x00, 0x0C, 'j', 'P', ' ', ' '};

        int result = imageType.isJP2000(jp2Data, jp2Data.length);

        Assertions.assertEquals(1, result);
    }

    /**
     * Verifies that invalid data is correctly rejected by isJP2000 method
     */
    @Test
    void isJP2000WithInvalidDataReturnsZero() {
        byte[] invalidData = {0x00, 0x00, 0x00, 0x0C, 'x', 'x', 'x', 'x'};

        int result = imageType.isJP2000(invalidData, invalidData.length);

        Assertions.assertEquals(0, result);
    }

    /**
     * Validates that all image type constants have the expected values
     */
    @Test
    void constantsHaveCorrectValues() {
        Assertions.assertEquals(-1, ImageType.UNKNOWN_IMG);
        Assertions.assertEquals(0, ImageType.RAW_IMG);
        Assertions.assertEquals(1, ImageType.WSQ_IMG);
        Assertions.assertEquals(2, ImageType.JPEGL_IMG);
        Assertions.assertEquals(3, ImageType.JPEGB_IMG);
        Assertions.assertEquals(4, ImageType.IHEAD_IMG);
        Assertions.assertEquals(5, ImageType.ANSI_NIST_IMG);
        Assertions.assertEquals(6, ImageType.JP2_IMG);
        Assertions.assertEquals(7, ImageType.PNG_IMG);
    }

    /**
     * Verifies that partial WSQ data is correctly rejected
     */
    @Test
    void isWSQWithPartialWSQDataReturnsZero() {
        byte[] partialWSQData = {(byte) 0xFF, (byte) 0xA0, 0x00, 0x00};

        int result = imageType.isWSQ(partialWSQData, partialWSQData.length);

        Assertions.assertEquals(0, result);
    }

    /**
     * Verifies that partial JPEG2000 data is correctly rejected
     */
    @Test
    void isJP2000WithPartialJP2DataReturnsZero() {
        byte[] partialJP2Data = {0x00, 0x00, 0x00, 0x0C};

        int result = imageType.isJP2000(partialJP2Data, partialJP2Data.length);

        Assertions.assertEquals(0, result);
    }

    /**
     * Validates WSQ format identification during image type detection
     */
    @Test
    void getImageTypeWithWSQDataFirst() {
        byte[] wsqData = {(byte) 0xFF, (byte) 0xA0, 0x00, 0x00, (byte) 0xFF, (byte) 0xA1};

        int result = imageType.getImageType(imageTypeResult, wsqData, wsqData.length);

        Assertions.assertEquals(0, result);
        Assertions.assertEquals(ImageType.WSQ_IMG, imageTypeResult.get());
    }

    /**
     * Validates JPEG2000 format identification when WSQ check fails
     */
    @Test
    void getImageTypeWithJP2DataAfterWSQCheck() {
        byte[] jp2Data = {0x00, 0x00, 0x00, 0x0C, 'j', 'P', ' ', ' '};

        int result = imageType.getImageType(imageTypeResult, jp2Data, jp2Data.length);

        Assertions.assertEquals(0, result);
        Assertions.assertEquals(ImageType.JP2_IMG, imageTypeResult.get());
    }

    /**
     * Verifies recognition of complete WSQ data structure
     */
    @Test
    void isWSQWithCompleteWSQDataReturnsOne() {
        byte[] wsqData = {(byte) 0xFF, (byte) 0xA0, 0x00, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xA1};

        int result = imageType.isWSQ(wsqData, wsqData.length);

        Assertions.assertEquals(1, result);
    }

    /**
     * Verifies rejection of incomplete WSQ data structure
     */
    @Test
    void isWSQWithIncompleteWSQDataReturnsZero() {
        byte[] wsqData = {(byte) 0xFF, (byte) 0xA0, 0x00, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xA2};

        int result = imageType.isWSQ(wsqData, wsqData.length);

        Assertions.assertEquals(0, result);
    }

    /**
     * Verifies recognition of complete JPEG2000 data structure
     */
    @Test
    void isJP2000WithCompleteJP2DataReturnsOne() {
        byte[] jp2Data = {0x00, 0x00, 0x00, 0x0C, 'j', 'P', ' ', ' ', 0x0D, 0x0A, (byte) 0x87, 0x0A};

        int result = imageType.isJP2000(jp2Data, jp2Data.length);

        Assertions.assertEquals(1, result);
    }

    /**
     * Verifies rejection of incomplete JPEG2000 data structure
     */
    @Test
    void isJP2000WithIncompleteJP2DataReturnsZero() {
        byte[] jp2Data = {0x00, 0x00, 0x00, 0x0C, 'j', 'X', ' ', ' '};

        int result = imageType.isJP2000(jp2Data, jp2Data.length);

        Assertions.assertEquals(0, result);
    }

    /**
     * Validates that data matching neither WSQ nor JPEG2000 format returns unknown type
     */
    @Test
    void getImageTypeWithNeitherWSQNorJP2ReturnsUnknown() {
        byte[] unknownData = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

        int result = imageType.getImageType(imageTypeResult, unknownData, unknownData.length);

        Assertions.assertEquals(-1, result);
        Assertions.assertEquals(ImageType.UNKNOWN_IMG, imageTypeResult.get());
    }
}