package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.ILfs.DirToRad;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;

/**
 * Test class for Free functionality
 */
@ExtendWith(MockitoExtension.class)
class FreeTest {

    private Free free;

    /**
     * Sets up the Free instance before each execution
     */
    @BeforeEach
    void setUp() {
        free = Free.getInstance();
    }

    /**
     * Verifies that getInstance returns the same singleton instance
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Free instance1 = Free.getInstance();
        Free instance2 = Free.getInstance();
        Assertions.assertEquals(instance1, instance2);
    }

    /**
     * Validates that constructor creates Free instance successfully
     */
    @Test
    void constructorCreatesInstance() {
        Assertions.assertNotNull(free);
    }

    /**
     * Verifies that free method handles objects without throwing exceptions
     */
    @Test
    void freeObjectDoesNotThrow() {
        Object testObject = new Object();
        free.free(testObject);
    }

    /**
     * Validates that freeDirToRad deallocates memory without throwing exceptions
     */
    @Test
    void freeDirToRadDeallocatesMemory() {
        DirToRad dirToRad = new DirToRad(8);
        dirToRad.setCos(new double[8]);
        dirToRad.setSin(new double[8]);

        free.freeDirToRad(dirToRad);
    }

    /**
     * Verifies that freeDirToRad handles null input without throwing exceptions
     */
    @Test
    void freeDirToRadWithNullDoesNotThrow() {
        free.freeDirToRad(null);
    }

    /**
     * Validates that freeDftWaves handles null input without throwing exceptions
     */
    @Test
    void freeDftWavesWithNullDoesNotThrow() {
        free.freeDftWaves(null);
    }

    /**
     * Verifies that freeRotGrids deallocates memory without throwing exceptions
     */
    @Test
    void freeRotGridsDeallocatesMemory() {
        RotGrids rotGrids = new RotGrids(0.0, 4, 5, 5, 0);
        rotGrids.setGrids(new int[4][25]);

        free.freeRotGrids(rotGrids);
    }

    /**
     * Validates that freeRotGrids handles null input without throwing exceptions
     */
    @Test
    void freeRotGridsWithNullDoesNotThrow() {
        free.freeRotGrids(null);
    }

    /**
     * Verifies that freeDirPowers deallocates memory without throwing exceptions
     */
    @Test
    void freeDirPowersDeallocatesMemory() {
        AtomicReferenceArray<Double[]> powers = new AtomicReferenceArray<>(2);
        powers.set(0, new Double[]{1.0, 2.0});
        powers.set(1, new Double[]{3.0, 4.0});

        free.freeDirPowers(powers, 2);
    }

    /**
     * Validates that freeDirPowers handles null input without throwing exceptions
     */
    @Test
    void freeDirPowersWithNullDoesNotThrow() {
        free.freeDirPowers(null, 2);
    }
}