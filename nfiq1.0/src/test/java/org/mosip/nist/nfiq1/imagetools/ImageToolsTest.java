package org.mosip.nist.nfiq1.imagetools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for ImageTools functionality
 */
@ExtendWith(MockitoExtension.class)
class ImageToolsTest {

    private ImageTools imageTools;

    /**
     * Sets up the ImageTools instance before each test execution
     */
    @BeforeEach
    void setUp() {
        imageTools = new ImageTools();
    }

    /**
     * Verifies that the constructor successfully creates an ImageTools instance
     */
    @Test
    void constructorCreatesInstance() {
        Assertions.assertNotNull(imageTools);
    }
}