package org.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.ILfs.DftWave;
import org.mosip.nist.nfiq1.common.ILfs.DftWaves;
import org.mosip.nist.nfiq1.common.ILfs.DirToRad;
import org.mosip.nist.nfiq1.common.ILfs.IFree;
import org.mosip.nist.nfiq1.common.ILfs.RotGrids;

/**
 * Java counterparts of the MINDTCT/LFS memory-release routines (NIST {@code free.c}).
 * <p>
 * The JVM garbage-collects memory, so these methods only clear references inside the given structures
 * ({@link DirToRad}, {@link DftWaves}, {@link RotGrids}, DFT power vectors) to help the collector; the
 * underlying {@link #free(Object)} does nothing. They are kept so the C call structure can still be followed.
 * <p>
 * Lazily created singleton; {@link #getInstance()} is synchronized and the class keeps no mutable state.
 */
public class Free extends MindTct implements IFree {
	/** Lazily initialized singleton instance; guarded by the class lock in {@link #getInstance()}. */
	private static Free instance;

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()}.
	 */
	private Free() {
		super();
	}

	/**
	 * Returns the shared {@code Free} singleton, creating it on first use.
	 *
	 * @return the singleton instance (never {@code null})
	 */
	public static synchronized Free getInstance() {
		if (instance == null) {
			instance = new Free();
		}
		return instance;
	}

	/**
	 * Placeholder for C {@code free()}: deallocates the memory behind an object.
	 * <p>
	 * Does nothing in Java; memory is reclaimed by the garbage collector once no references remain.
	 *
	 * @param object the object to "free" (may be {@code null})
	 */
	@SuppressWarnings({ "java:S1186" })
	public void free(Object object) {
	}

	/**
	 * Releases a DIR2RAD structure (NIST {@code free_dir2rad}).
	 * <p>
	 * Clears its cosine and sine lookup tables, then calls {@link #free(Object)}.
	 *
	 * @param dir2Rad the direction-to-radians lookup structure to release (may be {@code null})
	 */
	public void freeDirToRad(DirToRad dir2Rad) {
		if (dir2Rad != null) {
			dir2Rad.setCos(null);
			dir2Rad.setSin(null);
		}
		free(dir2Rad);
	}

	/**
	 * Releases a DFTWAVES structure (NIST {@code free_dftwaves}).
	 * <p>
	 * For each wave, "frees" its cosine and sine tables and the wave itself, then clears the waves array and
	 * calls {@link #free(Object)} on the container.
	 *
	 * @param dftWaves the DFT wave forms structure to release (may be {@code null})
	 */
	public void freeDftWaves(DftWaves dftWaves) {
		int i;

		if (dftWaves != null) {
			for (i = 0; i < dftWaves.getNWaves(); i++) {
				DftWave[] dftWavesArr = dftWaves.getWaves();
				if (dftWavesArr != null) {
					free(dftWavesArr[i].getCos());
					free(dftWavesArr[i].getSin());
					free(dftWavesArr[i]);
				}
			}
			dftWaves.setWaves(null);
		}
		free(dftWaves);
	}

	/**
	 * Releases a ROTGRIDS structure (NIST {@code free_rotgrids}).
	 * <p>
	 * Clears every rotated-grid offset array and the grids array, then calls {@link #free(Object)}.
	 *
	 * @param rotGrids the rotated grids structure to release (may be {@code null})
	 */
	public void freeRotGrids(RotGrids rotGrids) {
		int i;

		if (rotGrids != null) {
			for (i = 0; i < rotGrids.getNoOfGrids(); i++)
				rotGrids.getGrids()[i] = null;

			rotGrids.setGrids(null);
		}
		free(rotGrids);
	}

	/**
	 * Releases the DFT power vectors (NIST {@code free_dir_powers}).
	 * <p>
	 * Sets each of the first {@code nwaves} rows of {@code powers} to {@code null}, then calls
	 * {@link #free(Object)}.
	 *
	 * @param powers vectors of DFT power values (N waves x M directions); modified in place (may be {@code null})
	 * @param nwaves number of DFT wave forms used
	 */
	public void freeDirPowers(AtomicReferenceArray<Double[]> powers, final int nwaves) {
		int w;

		for (w = 0; w < nwaves; w++) {
			if (powers != null)
				powers.set(w, null);
		}
		free(powers);
	}
}