package org.mosip.nist.nfiq1.mindtct;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.ILfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link Sort} providing comprehensive validation of sorting
 * algorithms and array manipulation functionality.
 *
 * <p>This class validates the functionality of various sorting algorithms including
 * bubble sort, index sorting, and incremental/decremental ordering used in
 * NIST's Mindtct fingerprint analysis system for data organization operations.</p>
 */
@ExtendWith(MockitoExtension.class)
class SortTest {

    private Sort sort;

    /**
     * Sets up the test environment before each test method execution.
     * Initializes Sort singleton instance for testing.
     */
    @BeforeEach
    void setUp() {
        sort = Sort.getInstance();
    }

    /**
     * Validates that getInstance returns the same singleton instance across multiple calls.
     * Ensures proper implementation of the singleton pattern.
     */
    @Test
    void getInstanceReturnsSameInstance() {
        Sort instance1 = Sort.getInstance();
        Sort instance2 = Sort.getInstance();
        assertEquals(instance1, instance2);
    }

    /**
     * Validates sortIndicesIntArrayIncremental method with integer array sorting.
     * Tests index-based sorting where indices are arranged based on array values in ascending order.
     */
    @Test
    void sortIndicesIntArrayIncrementalSortsCorrectly() {
        AtomicIntegerArray order = new AtomicIntegerArray(5);
        AtomicIntegerArray ranks = new AtomicIntegerArray(new int[]{5, 2, 8, 1, 6});

        int result = sort.sortIndicesIntArrayIncremental(order, ranks, 5);

        assertEquals(ILfs.FALSE, result);
        assertEquals(3, order.get(0));
        assertEquals(1, order.get(1));
    }

    /**
     * Validates sortIndicesDoubleArrayIncremental method with double array sorting.
     * Tests index-based sorting where indices are arranged based on double array values in ascending order.
     */
    @Test
    void sortIndicesDoubleArrayIncrementalSortsCorrectly() {
        AtomicIntegerArray order = new AtomicIntegerArray(4);
        AtomicReferenceArray<Double> ranks = new AtomicReferenceArray<>(new Double[]{5.5, 2.1, 8.3, 1.7});

        int result = sort.sortIndicesDoubleArrayIncremental(order, ranks, 4);

        assertEquals(ILfs.FALSE, result);
        assertEquals(3, order.get(0));
        assertEquals(1, order.get(1));
    }

    /**
     * Validates bubbleSortIntArrayIncremental2 method with dual array sorting.
     * Tests bubble sort algorithm that sorts two arrays simultaneously based on the first array's values.
     */
    @Test
    void bubbleSortIntArrayIncremental2SortsArrays() {
        AtomicIntegerArray ranks = new AtomicIntegerArray(new int[]{5, 2, 8, 1});
        AtomicIntegerArray items = new AtomicIntegerArray(new int[]{0, 1, 2, 3});

        sort.bubbleSortIntArrayIncremental2(ranks, items, 4);

        assertEquals(1, ranks.get(0));
        assertEquals(2, ranks.get(1));
        assertEquals(5, ranks.get(2));
        assertEquals(8, ranks.get(3));
    }

    /**
     * Validates bubbleSortDoubleArrayIncremental2 method with double array sorting.
     * Tests bubble sort algorithm for double arrays with corresponding item array reordering.
     */
    @Test
    void bubbleSortDoubleArrayIncremental2SortsArrays() {
        AtomicReferenceArray<Double> ranks = new AtomicReferenceArray<>(new Double[]{5.5, 2.1, 8.3, 1.7});
        AtomicIntegerArray items = new AtomicIntegerArray(new int[]{0, 1, 2, 3});

        sort.bubbleSortDoubleArrayIncremental2(ranks, items, 4);

        assertEquals(1.7, ranks.get(0), 0.001);
        assertEquals(2.1, ranks.get(1), 0.001);
    }

    /**
     * Validates bubbleSortDoubleArrayDecremental2 method with descending order sorting.
     * Tests bubble sort algorithm for double arrays in descending order with item array reordering.
     */
    @Test
    void bubbleSortDoubleArrayDecremental2SortsArraysDescending() {
        AtomicReferenceArray<Double> ranks = new AtomicReferenceArray<>(new Double[]{5.5, 2.1, 8.3, 1.7});
        AtomicIntegerArray items = new AtomicIntegerArray(new int[]{0, 1, 2, 3});

        sort.bubbleSortDoubleArrayDecremental2(ranks, items, 4);

        assertEquals(8.3, ranks.get(0), 0.001);
        assertEquals(5.5, ranks.get(1), 0.001);
    }

    /**
     * Validates bubbleSortIntArrayIncremental method with single array sorting.
     * Tests basic bubble sort algorithm for integer arrays in ascending order.
     */
    @Test
    void bubbleSortIntArrayIncrementalSortsArray() {
        AtomicIntegerArray ranks = new AtomicIntegerArray(new int[]{5, 2, 8, 1});

        sort.bubbleSortIntArrayIncremental(ranks, 4);

        assertEquals(1, ranks.get(0));
        assertEquals(2, ranks.get(1));
        assertEquals(5, ranks.get(2));
        assertEquals(8, ranks.get(3));
    }

    /**
     * Validates index sorting with empty array input.
     * Tests handling when array has no elements to sort.
     */
    @Test
    void sortIndicesWithEmptyArrayReturnsImmediately() {
        AtomicIntegerArray order = new AtomicIntegerArray(0);
        AtomicIntegerArray ranks = new AtomicIntegerArray(0);

        int result = sort.sortIndicesIntArrayIncremental(order, ranks, 0);

        assertEquals(ILfs.FALSE, result);
    }

    /**
     * Validates index sorting with single element array.
     * Tests behavior when array contains only one element to sort.
     */
    @Test
    void sortIndicesWithSingleElementReturnsImmediately() {
        AtomicIntegerArray order = new AtomicIntegerArray(1);
        AtomicIntegerArray ranks = new AtomicIntegerArray(new int[]{42});

        int result = sort.sortIndicesIntArrayIncremental(order, ranks, 1);

        assertEquals(ILfs.FALSE, result);
        assertEquals(0, order.get(0));
    }
}