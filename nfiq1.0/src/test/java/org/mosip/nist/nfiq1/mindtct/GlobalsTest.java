package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mosip.nist.nfiq1.common.ILfs.FeaturePattern;
import org.mosip.nist.nfiq1.common.ILfs.LfsParams;

/**
 * Test class for Globals functionality
 */
@ExtendWith(MockitoExtension.class)
class GlobalsTest {

    private Globals globals;

    /**
     * Sets up the Globals instance before each execution
     */
    @BeforeEach
    void setUp() {
        globals = Globals.getInstance();
    }

    /**
     * Verifies that getInstance returns the same singleton instance
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Globals instance1 = Globals.getInstance();
        Globals instance2 = Globals.getInstance();
        Assertions.assertSame(instance1, instance2);
    }

    /**
     * Validates that setDftCoefs updates the DFT coefficients array
     */
    @Test
    void setDftCoefsUpdatesArray() {
        double[] newCoefs = {5, 6, 7, 8};
        globals.setDftCoefs(newCoefs);
        Assertions.assertArrayEquals(newCoefs, globals.getDftCoefs());
    }

    /**
     * Verifies that getLfsParams returns valid LFS parameters
     */
    @Test
    void getLfsParamsReturnsValidParams() {
        LfsParams params = globals.getLfsParams();
        Assertions.assertNotNull(params);
    }

    /**
     * Validates that setLfsParams updates the LFS parameters
     */
    @Test
    void setLfsParamsUpdatesParams() {
        LfsParams newParams = new LfsParams(1, 2, 3, 4, 5, 6, 7.0, 8, 9.0, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19.0, 20.0, 21.0, 22, 23.0, 24.0, 25, 26, 27, 28, 29, 30.0, 31, 32, 33.0, 34.0, 35, 36, 37, 38, 39.0, 40.0, 41.0, 42.0, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 56.0, 57, 58, 59, 60, 61, 62.0, 63.0, 64, 65);
        globals.setLfsParams(newParams);
        Assertions.assertSame(newParams, globals.getLfsParams());
    }

    /**
     * Verifies that getLfsParamsV2 returns valid LFS parameters version 2
     */
    @Test
    void getLfsParamsV2ReturnsValidParams() {
        LfsParams paramsV2 = globals.getLfsParamsV2();
        Assertions.assertNotNull(paramsV2);
    }

    /**
     * Validates that setLfsParamsV2 updates the LFS parameters version 2
     */
    @Test
    void setLfsParamsV2UpdatesParams() {
        LfsParams newParamsV2 = new LfsParams(1, 2, 3, 4, 5, 6, 7.0, 8, 9.0, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19.0, 20.0, 21.0, 22, 23.0, 24.0, 25, 26, 27, 28, 29, 30.0, 31, 32, 33.0, 34.0, 35, 36, 37, 38, 39.0, 40.0, 41.0, 42.0, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 56.0, 57, 58, 59, 60, 61, 62.0, 63.0, 64, 65);
        globals.setLfsParamsV2(newParamsV2);
        Assertions.assertSame(newParamsV2, globals.getLfsParamsV2());
    }

    /**
     * Verifies that getNbr8Dx returns a valid 8-neighbor X-direction array
     */
    @Test
    void getNbr8DxReturnsValidArray() {
        int[] nbr8Dx = globals.getNbr8Dx();
        Assertions.assertNotNull(nbr8Dx);
        Assertions.assertEquals(8, nbr8Dx.length);
        Assertions.assertArrayEquals(new int[]{0, 1, 1, 1, 0, -1, -1, -1}, nbr8Dx);
    }

    /**
     * Validates that setNbr8Dx updates the 8-neighbor X-direction array
     */
    @Test
    void setNbr8DxUpdatesArray() {
        int[] newNbr8Dx = {1, 2, 3, 4, 5, 6, 7, 8};
        globals.setNbr8Dx(newNbr8Dx);
        Assertions.assertArrayEquals(newNbr8Dx, globals.getNbr8Dx());
    }

    /**
     * Verifies that getNbr8Dy returns a valid 8-neighbor Y-direction array
     */
    @Test
    void getNbr8DyReturnsValidArray() {
        int[] nbr8Dy = globals.getNbr8Dy();
        Assertions.assertNotNull(nbr8Dy);
        Assertions.assertEquals(8, nbr8Dy.length);
        Assertions.assertArrayEquals(new int[]{-1, -1, 0, 1, 1, 1, 0, -1}, nbr8Dy);
    }

    /**
     * Validates that setNbr8Dy updates the 8-neighbor Y-direction array
     */
    @Test
    void setNbr8DyUpdatesArray() {
        int[] newNbr8Dy = {8, 7, 6, 5, 4, 3, 2, 1};
        globals.setNbr8Dy(newNbr8Dy);
        Assertions.assertArrayEquals(newNbr8Dy, globals.getNbr8Dy());
    }

    /**
     * Verifies that getChaincodesNbr8 returns a valid chain codes array
     */
    @Test
    void getChaincodesNbr8ReturnsValidArray() {
        int[] chaincodesNbr8 = globals.getChaincodesNbr8();
        Assertions.assertNotNull(chaincodesNbr8);
        Assertions.assertEquals(9, chaincodesNbr8.length);
        Assertions.assertArrayEquals(new int[]{3, 2, 1, 4, -1, 0, 5, 6, 7}, chaincodesNbr8);
    }

    /**
     * Validates that setChaincodesNbr8 updates the chain codes array
     */
    @Test
    void setChaincodesNbr8UpdatesArray() {
        int[] newChaincodesNbr8 = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        globals.setChaincodesNbr8(newChaincodesNbr8);
        Assertions.assertArrayEquals(newChaincodesNbr8, globals.getChaincodesNbr8());
    }

    /**
     * Validates that setFeaturePatterns updates the feature patterns array
     */
    @Test
    void setFeaturePatternsUpdatesArray() {
        FeaturePattern[] newPatterns = new FeaturePattern[2];
        newPatterns[0] = new FeaturePattern(1, 0, new int[]{0, 0}, new int[]{0, 1}, new int[]{0, 0});
        newPatterns[1] = new FeaturePattern(2, 1, new int[]{1, 1}, new int[]{1, 0}, new int[]{1, 1});

        globals.setFeaturePatterns(newPatterns);
        Assertions.assertArrayEquals(newPatterns, globals.getFeaturePatterns());
    }

    /**
     * Verifies that Globals inherits from MindTct class
     */
    @Test
    void globalsInheritsFromMindTct() {
        Assertions.assertTrue(globals instanceof MindTct);
    }
}