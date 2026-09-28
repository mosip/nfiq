package org.mosip.nist.nfiq1;

import org.mosip.nist.nfiq1.mindtct.GetMinutiae;
import org.mosip.nist.nfiq1.mindtct.Globals;
import org.mosip.nist.nfiq1.mindtct.MinutiaHelper;
import org.mosip.nist.nfiq1.mlp.RunMlp;

/**
 * Base class for the NFIQ 1.0 computation, wiring together the collaborators needed by
 * {@link Nfiq1Helper}.
 * <p>
 * Provides access to the MINDTCT minutiae extractor ({@link GetMinutiae}, {@link MinutiaHelper},
 * {@link Globals}), the multi-layer perceptron runner ({@link RunMlp}) and the NFIQ-specific default
 * statistics/weights ({@link Nfiq1Globals}) and Z-normalization routines ({@link Nfiq1ZNormalization}).
 * The MINDTCT and MLP collaborators are process-wide singletons; the NFIQ globals and Z-normalizer are
 * per-instance and replaceable through setters.
 */
public class Nfiq1 extends Nist {
	/** Default Z-normalization statistics and MLP weights used by {@code computeNfiq}. */
	private Nfiq1Globals nfiqGlobals = new Nfiq1Globals();
	/** Z-normalization routines applied to the NFIQ feature vector before MLP classification. */
	private Nfiq1ZNormalization zNorm = new Nfiq1ZNormalization();

	/**
	 * Returns the shared MINDTCT minutia helper (allocation, detection and bookkeeping of minutiae).
	 *
	 * @return the singleton {@link MinutiaHelper} instance
	 */
	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}

	/**
	 * Returns the shared MINDTCT globals holding default LFS parameter sets (e.g. {@code getLfsParamsV2()}).
	 *
	 * @return the singleton {@link Globals} instance
	 */
	public Globals getGlobals() {
		return Globals.getInstance();
	}

	/**
	 * Returns the shared MINDTCT entry point that binarizes an image and extracts its minutiae and maps.
	 *
	 * @return the singleton {@link GetMinutiae} instance
	 */
	public GetMinutiae getGetMinutiae() {
		return GetMinutiae.getInstance();
	}

	/**
	 * Returns the NFIQ default globals (Z-normalization means/stddevs and MLP weights/topology).
	 *
	 * @return the {@link Nfiq1Globals} used by this instance
	 */
	public Nfiq1Globals getNfiqGlobals() {
		return nfiqGlobals;
	}

	/**
	 * Replaces the NFIQ default globals used by this instance.
	 *
	 * @param nfiqGlobals the new globals holder; must not be {@code null} when {@code computeNfiq} is invoked
	 */
	public void setNfiqGlobals(Nfiq1Globals nfiqGlobals) {
		this.nfiqGlobals = nfiqGlobals;
	}

	/**
	 * Returns the Z-normalization helper used on NFIQ feature vectors.
	 *
	 * @return the {@link Nfiq1ZNormalization} used by this instance
	 */
	public Nfiq1ZNormalization getZNorm() {
		return zNorm;
	}

	/**
	 * Replaces the Z-normalization helper used on NFIQ feature vectors.
	 *
	 * @param zNorm the new Z-normalization helper
	 */
	public void setZNorm(Nfiq1ZNormalization zNorm) {
		this.zNorm = zNorm;
	}

	/**
	 * Returns the shared feed-forward MLP runner used to classify the normalized feature vector.
	 *
	 * @return the singleton {@link RunMlp} instance
	 */
	public RunMlp getRunMlp() {
		return RunMlp.getInstance();
	}
}