package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.ISort;

/**
 * Simple sorting utilities used throughout MINDTCT.
 * <p>
 * Port of NIST LFS {@code sort.c}. Provides in-place bubble sorts of integer
 * and double rank lists, optionally moving a parallel list of integer items
 * (typically indices) along with the ranks. Bubble sort is adequate because
 * the lists sorted by MINDTCT are small.
 * <p>
 * Implemented as a lazily created singleton; {@link #getInstance()} is
 * synchronized and the class keeps no mutable state.
 */
public class Sort extends MindTct implements ISort {
	/** Lazily created singleton instance, see {@link #getInstance()}. */
	private static Sort instance;

	/**
	 * Private constructor; use {@link #getInstance()} to obtain the singleton.
	 */
	private Sort() {
		super();
	}

	/**
	 * Returns the shared singleton instance, creating it on first use.
	 *
	 * @return the singleton {@code Sort} instance
	 */
	public static synchronized Sort getInstance() {
		if (instance == null) {
			synchronized (Sort.class) {
				if (instance == null) {
					instance = new Sort();
				}
			}
		}
		return instance;
	}

	/**
	 * Takes a list of integers and returns a list of indices referencing the
	 * integer list in increasing order. The original list of integers is also
	 * returned in sorted order.
	 * <p>
	 * NIST: {@code sort_indices_int_inc()}.
	 *
	 * @param order output: list of indices referencing the integer list in sorted
	 *              order (must hold at least {@code num} entries)
	 * @param ranks input/output: list of integers to be sorted; on return sorted
	 *              in increasing order
	 * @param num   number of integers in the list
	 * @return zero ({@link ILfs#FALSE}) on successful completion (negative would
	 *         indicate a system error)
	 */
	public int sortIndicesIntArrayIncremental(AtomicIntegerArray order, AtomicIntegerArray ranks, final int num) {
		int i;

		/* Initialize list of sequential indices. */
		for (i = 0; i < num; i++) {
			order.set(i, i);
		}

		/* Sort the indecies into rank order. */
		bubbleSortIntArrayIncremental2(ranks, order, num);

		/* Set output pointer to the resulting order of sorted indices. */
		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Takes a list of doubles and returns a list of indices referencing the
	 * double list in increasing order. The original list of doubles is also
	 * returned in sorted order.
	 * <p>
	 * NIST: {@code sort_indices_double_inc()}.
	 *
	 * @param order output: list of indices referencing the double list in sorted
	 *              order (must hold at least {@code num} entries)
	 * @param ranks input/output: list of doubles to be sorted; on return sorted
	 *              in increasing order
	 * @param num   number of doubles in the list
	 * @return zero ({@link ILfs#FALSE}) on successful completion (negative would
	 *         indicate a system error)
	 */
	public int sortIndicesDoubleArrayIncremental(AtomicIntegerArray order, AtomicReferenceArray<Double> ranks,
			final int num) {
		int i;

		/* Initialize list of sequential indices. */
		for (i = 0; i < num; i++) {
			order.set(i, i);
		}

		/* Sort the indicies into rank order. */
		bubbleSortDoubleArrayIncremental2(ranks, order, num);

		/* Set output pointer to the resulting order of sorted indices. */
		/* Return normally. */
		return (ILfs.FALSE);
	}

	/**
	 * Takes a list of integer ranks and a corresponding list of integer
	 * attributes, and sorts the ranks into increasing order moving the attributes
	 * correspondingly.
	 * <p>
	 * NIST: {@code bubble_sort_int_inc_2()}.
	 *
	 * @param ranks input/output: list of integers to sort on; on return sorted in
	 *              increasing order
	 * @param items input/output: list of corresponding integer attributes; on
	 *              return in the corresponding sorted order
	 * @param len   number of items in list
	 */
	public void bubbleSortIntArrayIncremental2(AtomicIntegerArray ranks, AtomicIntegerArray items, final int len) {
		int done = 0;
		int i;
		int p;
		int n;
		int tRank;
		int tItem;

		/* Set counter to the length of the list being sorted. */
		n = len;

		/* While swaps in order continue to occur from the */
		/* previous iteration... */
		while (done == 0) {
			/* Reset the done flag to TRUE. */
			done = ILfs.TRUE;
			/* Foreach rank in list up to current end index... */
			/* ("p" points to current rank and "i" points to the next rank.) */
			for (i = 1, p = 0; i < n; i++, p++) {
				/* If previous rank is < current rank ... */
				if (ranks.get(p) > ranks.get(i)) {
					/* Swap ranks. */
					tRank = ranks.get(i);
					ranks.set(i, ranks.get(p));
					ranks.set(p, tRank);
					/* Swap items. */
					tItem = items.get(i);
					items.set(i, items.get(p));
					items.set(p, tItem);

					/* Changes were made, so set done flag to FALSE. */
					done = ILfs.FALSE;
				}
				/* Otherwise, rank pair is in order, so continue. */
			}
			/* Decrement the ending index. */
			n--;
		}
	}

	/**
	 * Takes a list of double ranks and a corresponding list of integer
	 * attributes, and sorts the ranks into increasing order moving the attributes
	 * correspondingly.
	 * <p>
	 * NIST: {@code bubble_sort_double_inc_2()}.
	 *
	 * @param ranks input/output: list of doubles to sort on; on return sorted in
	 *              increasing order
	 * @param items input/output: list of corresponding integer attributes; on
	 *              return in the corresponding sorted order
	 * @param len   number of items in list
	 */
	public void bubbleSortDoubleArrayIncremental2(AtomicReferenceArray<Double> ranks, AtomicIntegerArray items,
			final int len) {
		int done = 0;
		int i;
		int p;
		int n;
		int tItem;
		double tRank;

		/* Set counter to the length of the list being sorted. */
		n = len;

		/* While swaps in order continue to occur from the */
		/* previous iteration... */
		while (done != ILfs.TRUE) {
			/* Reset the done flag to TRUE. */
			done = ILfs.TRUE;
			/* Foreach rank in list up to current end index... */
			/* ("p" points to current rank and "i" points to the next rank.) */
			for (i = 1, p = 0; i < n; i++, p++) {
				/* If previous rank is < current rank ... */
				if (ranks.get(p) > ranks.get(i)) {
					/* Swap ranks. */
					tRank = ranks.get(i);
					ranks.set(i, ranks.get(p));
					ranks.set(p, tRank);

					/* Swap items. */
					tItem = items.get(i);
					items.set(i, items.get(p));
					items.set(p, tItem);

					/* Changes were made, so set done flag to FALSE. */
					done = ILfs.FALSE;
				}
				/* Otherwise, rank pair is in order, so continue. */
			}
			/* Decrement the ending index. */
			n--;
		}
	}

	/**
	 * Conducts a simple bubble sort returning a list of ranks in decreasing order
	 * and their associated items in sorted order as well.
	 * <p>
	 * NIST: {@code bubble_sort_double_dec_2()}.
	 *
	 * @param ranks input/output: list of values to be sorted; on return sorted in
	 *              descending order
	 * @param items input/output: list of items, each corresponding to a
	 *              particular rank value; on return in the corresponding sorted
	 *              order of the ranks. If these items are indices, upon return
	 *              they may be used as indirect addresses reflecting the sorted
	 *              order of the ranks.
	 * @param len   length of the lists to be sorted
	 */
	public void bubbleSortDoubleArrayDecremental2(AtomicReferenceArray<Double> ranks, AtomicIntegerArray items,
			final int len) {
		int done = ILfs.FALSE;
		int i;
		int p;
		int n;
		int tItem;
		double tRank;

		n = len;
		while (done == ILfs.FALSE) {
			done = ILfs.TRUE;
			for (i = 1, p = 0; i < n; i++, p++) {
				/* If previous rank is < current rank ... */
				if (ranks.get(p) < ranks.get(i)) {
					/* Swap ranks */
					tRank = ranks.get(i);
					ranks.set(i, ranks.get(p));
					ranks.set(p, tRank);
					/* Swap corresponding items */
					tItem = items.get(i);
					items.set(i, items.get(p));
					items.set(p, tItem);
					done = ILfs.FALSE;
				}
			}
			n--;
		}
	}

	/**
	 * Takes a list of integers and sorts them into increasing order using a
	 * simple bubble sort.
	 * <p>
	 * NIST: {@code bubble_sort_int_inc()}.
	 *
	 * @param ranks input/output: list of integers to sort; on return sorted in
	 *              increasing order
	 * @param len   number of items in list
	 */
	public void bubbleSortIntArrayIncremental(AtomicIntegerArray ranks, final int len) {
		int done = 0;
		int i;
		int p;
		int n;
		int tRank;

		/* Set counter to the length of the list being sorted. */
		n = len;

		/* While swaps in order continue to occur from the */
		/* previous iteration... */
		while (done == ILfs.FALSE) {
			/* Reset the done flag to TRUE. */
			done = 1;
			/* Foreach rank in list up to current end index... */
			/* ("p" points to current rank and "i" points to the next rank.) */
			for (i = 1, p = 0; i < n; i++, p++) {
				/* If previous rank is < current rank ... */
				if (ranks.get(p) > ranks.get(i)) {
					/* Swap ranks. */
					tRank = ranks.get(i);
					ranks.set(i, ranks.get(p));
					ranks.set(p, tRank);

					/* Changes were made, so set done flag to FALSE. */
					done = ILfs.FALSE;
				}
				/* Otherwise, rank pair is in order, so continue. */
			}
			/* Decrement the ending index. */
			n--;
		}
	}
}