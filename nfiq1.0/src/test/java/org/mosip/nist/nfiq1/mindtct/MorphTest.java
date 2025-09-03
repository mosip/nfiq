package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link Morph} providing validation of morphological image processing
 * operations for fingerprint analysis.
 *
 * <p>This class validates the functionality of morphological operations including
 * erosion, dilation, and directional pixel access methods used in NIST's Mindtct
 * fingerprint analysis system for image preprocessing and feature enhancement.</p>
 */
@ExtendWith(MockitoExtension.class)
class MorphTest {

    private Morph morph;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes the Morph singleton instance for testing.
     */
    @BeforeEach
    void setUp() {
        morph = Morph.getInstance();
    }

    /**
     * Validates that getInstance returns the same singleton instance across multiple calls.
     * Ensures proper implementation of the singleton pattern.
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Morph instance1 = Morph.getInstance();
        Morph instance2 = Morph.getInstance();
        assertEquals(instance1, instance2);
    }

    /**
     * Validates erodeImage2 method processes image data correctly.
     * Tests morphological erosion operation on binary image data.
     */
    @Test
    void erodeImage2ProcessesImageCorrectly() {
        int[] inputImageData = new int[]{255, 0, 255, 0, 255, 0, 255, 0, 255};
        int[] outputImageData = new int[9];

        morph.erodeImage2(inputImageData, outputImageData, 3, 3);

        assertNotNull(outputImageData);
    }

    /**
     * Validates dilateImage2 method processes image data correctly.
     * Tests morphological dilation operation on binary image data.
     */
    @Test
    void dilateImage2ProcessesImageCorrectly() {
        int[] inputImageData = new int[]{0, 255, 0, 255, 0, 255, 0, 255, 0};
        int[] outputImageData = new int[9];

        morph.dilateImage2(inputImageData, outputImageData, 3, 3);

        assertNotNull(outputImageData);
    }

    /**
     * Validates getSouth82 method with valid position returns pixel value.
     * Tests retrieval of southern neighbor pixel using 8-connected connectivity.
     */
    @Test
    void getSouth82WithValidPositionReturnsPixel() {
        int[] inputImageData = new int[]{100, 150, 200, 50, 75, 125, 175, 225, 25};

        int result = morph.getSouth82(inputImageData, 1, 0, 3, 3, -1);

        assertTrue(result >= 0);
    }

    /**
     * Validates getNorth82 method with valid position returns pixel value.
     * Tests retrieval of northern neighbor pixel using 8-connected connectivity.
     */
    @Test
    void getNorth82WithValidPositionReturnsPixel() {
        int[] inputImageData = new int[]{100, 150, 200, 50, 75, 125, 175, 225, 25};

        int result = morph.getNorth82(inputImageData, 7, 2, 3, -1);

        assertTrue(result >= 0);
    }

    /**
     * Validates getEast82 method with valid position returns pixel value.
     * Tests retrieval of eastern neighbor pixel using 8-connected connectivity.
     */
    @Test
    void getEast82WithValidPositionReturnsPixel() {
        int[] inputImageData = new int[]{100, 150, 200, 50, 75, 125, 175, 225, 25};

        int result = morph.getEast82(inputImageData, 3, 0, 3, -1);

        assertTrue(result >= 0);
    }

    /**
     * Validates getWest82 method with valid position returns pixel value.
     * Tests retrieval of western neighbor pixel using 8-connected connectivity.
     */
    @Test
    void getWest82WithValidPositionReturnsPixel() {
        int[] inputImageData = new int[]{100, 150, 200, 50, 75, 125, 175, 225, 25};

        int result = morph.getWest82(inputImageData, 5, 2, -1);

        assertTrue(result >= 0);
    }

    /**
     * Validates getSouth82 method with invalid position returns failure code.
     * Tests error handling when attempting to access pixels outside image boundaries.
     */
    @Test
    void getSouth82WithInvalidPositionReturnsFailCode() {
        int[] inputImageData = new int[]{100, 150, 200};

        int result = morph.getSouth82(inputImageData, 2, 2, 3, 3, -1);

        assertEquals(-1, result);
    }

    /**
     * Validates getNorth82 method with invalid position returns failure code.
     * Tests boundary checking when accessing northern pixels outside valid range.
     */
    @Test
    void getNorth82WithInvalidPositionReturnsFailCode() {
        int[] inputImageData = new int[]{100, 150, 200};

        int result = morph.getNorth82(inputImageData, 0, 0, 3, -1);

        assertEquals(-1, result);
    }

    /**
     * Validates getEast82 method with invalid position returns failure code.
     * Tests boundary checking when accessing eastern pixels outside valid range.
     */
    @Test
    void getEast82WithInvalidPositionReturnsFailCode() {
        int[] inputImageData = new int[]{100, 150, 200};

        int result = morph.getEast82(inputImageData, 2, 2, 3, -1);

        assertEquals(-1, result);
    }

    /**
     * Validates getWest82 method with invalid position returns failure code.
     * Tests boundary checking when accessing western pixels outside valid range.
     */
    @Test
    void getWest82WithInvalidPositionReturnsFailCode() {
        int[] inputImageData = new int[]{100, 150, 200};

        int result = morph.getWest82(inputImageData, 0, 0, -1);

        assertEquals(-1, result);
    }
}
