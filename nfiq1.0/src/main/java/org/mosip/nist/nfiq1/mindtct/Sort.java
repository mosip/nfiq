package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.ILfs;
import org.mosip.nist.nfiq1.common.ILfs.ISort;

public class Sort extends MindTct implements ISort {
	private static Sort instance;

	private Sort() {
		super();
	}

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

	/*************************************************************************
	 **************************************************************************
	 * #cat: sortIndicesIntArrayIncremental - Takes a list of integers and returns a
	 * list of #cat: indices referencing the integer list in increasing order. #cat:
	 * The original list of integers is also returned in sorted #cat: order. Input:
	 * ranks - list of integers to be sorted num - number of integers in the list
	 * Output: order - list of indices referencing the integer list in sorted order
	 * ranks - list of integers in increasing order Return Code: Zero - successful
	 * completion Negative - system error
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: sortIndicesDoubleArrayIncremental - Takes a list of doubles and returns
	 * a list of #cat: indices referencing the double list in increasing order.
	 * #cat: The original list of doubles is also returned in sorted #cat: order.
	 * Input: ranks - list of doubles to be sorted num - number of doubles in the
	 * list Output: optr - list of indices referencing the double list in sorted
	 * order ranks - list of doubles in increasing order Return Code: Zero -
	 * successful completion Negative - system error
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: bubbleSortIntArrayIncremental2 - Takes a list of integer ranks and a
	 * corresponding #cat: list of integer attributes, and sorts the ranks #cat:
	 * into increasing order moving the attributes #cat: correspondingly. Input:
	 * ranks - list of integers to be sort on items - list of corresponding integer
	 * attributes len - number of items in list Output: ranks - list of integers
	 * sorted in increasing order items - list of attributes in corresponding sorted
	 * order
	 **************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: bubbleSortDoubleArrayIncremental2 - Takes a list of double ranks and a
	 * #cat: corresponding list of integer attributes, and sorts the #cat: ranks
	 * into increasing order moving the attributes #cat: correspondingly. Input:
	 * ranks - list of double to be sort on items - list of corresponding integer
	 * attributes len - number of items in list Output: ranks - list of doubles
	 * sorted in increasing order items - list of attributes in corresponding sorted
	 * order
	 **************************************************************************/
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

	/***************************************************************************
	 **************************************************************************
	 * #cat: bubbleSortDoubleArrayDecremental2 - Conducts a simple bubble sort
	 * returning a list #cat: of ranks in decreasing order and their associated
	 * items in sorted #cat: order as well. Input: ranks - list of values to be
	 * sorted items - list of items, each corresponding to a particular rank value
	 * len - length of the lists to be sorted Output: ranks - list of values sorted
	 * in descending order items - list of items in the corresponding sorted order
	 * of the ranks. If these items are indices, upon return, they may be used as
	 * indirect addresses reflecting the sorted order of the ranks.
	 ****************************************************************************/
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

	/*************************************************************************
	 **************************************************************************
	 * #cat: bubbleSortIntArrayIncremental - Takes a list of integers and sorts them
	 * into #cat: increasing order using a simple bubble sort. Input: ranks - list
	 * of integers to be sort on len - number of items in list Output: ranks - list
	 * of integers sorted in increasing order
	 **************************************************************************/
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