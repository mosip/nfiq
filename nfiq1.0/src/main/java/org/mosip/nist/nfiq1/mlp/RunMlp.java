package org.mosip.nist.nfiq1.mlp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.IMlp;
import org.mosip.nist.nfiq1.common.IMlp.IRunMlp;
import org.mosip.nist.nfiq1.mindtct.Free;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the NFIQ two-layer multi-layer perceptron (MLP) on a feature vector to produce the quality class and
 * confidence.
 * <p>
 * Port of NIST's {@code mlp/runmlp.c} ({@code runmlp} and {@code runmlp2}). The network has one hidden layer.
 * {@code weights} is laid out as in NIST's MLP weight files: first-layer weights (nHids x nInps), first-layer
 * biases (nHids), second-layer weights (nOuts x nHids), then second-layer biases (nOuts). Each layer is
 * computed with {@link MlpCla#mlpSgemV} (transposed form) followed by the chosen activation function from
 * {@link Acs}. The winning output node gives the hypothetical class (the NFIQ level minus 1) and its activation
 * gives the confidence.
 * <p>
 * Lazily created singleton; {@link #getInstance()} is synchronized. Not fully thread-safe, because it
 * delegates to {@link MlpCla}, which keeps scratch state in static fields.
 */
public class RunMlp extends Mlp implements IRunMlp {
	/** SLF4J logger for configuration errors (unsupported activation codes, too many hidden nodes). */
	private static final Logger logger = LoggerFactory.getLogger(RunMlp.class);
	/** Lazily initialized singleton instance; guarded by the class lock in {@link #getInstance()}. */
	private static RunMlp instance;

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()}.
	 */
	private RunMlp() {
		super();
	}

	/**
	 * Returns the shared {@code RunMlp} singleton, creating it on first use.
	 *
	 * @return the singleton instance (never {@code null})
	 */
	public static synchronized RunMlp getInstance() {
		if (instance == null) {
			instance = new RunMlp();
		}
		return instance;
	}

	/**
	 * Returns the activation-function helper.
	 *
	 * @return the {@link Acs} singleton
	 */
	public Acs getAcs() {
		return Acs.getInstance();
	}

	/**
	 * Returns the memory-release helper (kept for parity with the NIST C code).
	 *
	 * @return the {@link Free} singleton
	 */
	public Free getFree() {
		return Free.getInstance();
	}

	/**
	 * Returns the BLAS-style linear algebra helper.
	 *
	 * @return the {@link MlpCla} singleton
	 */
	public MlpCla getMlpCla() {
		return MlpCla.getInstance();
	}

	/**
	 * Runs the multi-layer perceptron on a feature vector (NIST {@code runmlp}).
	 * <p>
	 * Hidden activations start as the first-layer biases, have the first-layer weights times the feature
	 * vector added, and are then passed through the hidden activation function. The output layer is computed
	 * the same way from the hidden activations. The output node that activates most strongly is the
	 * hypothetical class, and its activation is the confidence.
	 * <p>
	 * The computation is identical to {@link #runMlp2}. Where the C original aborts the process on an error,
	 * this method throws {@link IllegalArgumentException} instead.
	 *
	 * @param nInps            number of input nodes; the first {@code nInps} elements of the feature vector are used
	 * @param nHids            number of hidden nodes; must not exceed {@code IMlp.MAX_NHIDS}
	 * @param nOuts            number of output nodes (classes)
	 * @param acFuncHidsCode   activation function for the hidden nodes: {@code IMlp.LINEAR}, {@code IMlp.SIGMOID}
	 *                         or {@code IMlp.SINUSOID} (defined in NIST {@code parms.h})
	 * @param acFuncOutsCode   activation function for the output nodes (same codes)
	 * @param weights          the MLP weights and biases, laid out as described in the class documentation
	 * @param featureVectorArr the feature vector to classify
	 * @param outAcs           output: the output activations; must be allocated by the caller with at least
	 *                         {@code nOuts} elements
	 * @param hypClass         output: the hypothetical class, an integer from 0 to {@code nOuts - 1}
	 * @param confidence       output: a value from 0 to 1, defined as {@code outAcs[hypClass]} (the highest
	 *                         output activation)
	 * @throws IllegalArgumentException if {@code nHids} exceeds {@code IMlp.MAX_NHIDS} or an activation code is
	 *                                  not supported
	 */
	public void runMlp(final int nInps, final int nHids, final int nOuts, final int acFuncHidsCode,
			final int acFuncOutsCode, AtomicReferenceArray<Double> weights, double[] featureVectorArr,
			AtomicReferenceArray<Double> outAcs, AtomicInteger hypClass, AtomicReference<Double> confidence) {
		int ret = runMlp2(nInps, nHids, nOuts, acFuncHidsCode, acFuncOutsCode, weights, featureVectorArr, outAcs,
				hypClass, confidence);
		if (ret != 0) {
			throw new IllegalArgumentException("runmlp failed with code " + ret);
		}
	}

	/**
	 * Runs the multi-layer perceptron on a feature vector, returning an error code instead of exiting (NIST
	 * {@code runmlp2}).
	 * <p>
	 * This is the variant NFIQ uses. Offsets {@code w1}, {@code b1}, {@code w2} and {@code b2} are computed
	 * into {@code weights}. Hidden activations start as the first-layer biases, have the first-layer weights
	 * times the feature vector added, and are then passed through the hidden activation function. The output
	 * layer is computed the same way from the hidden activations. The output node that activates most strongly
	 * is the hypothetical class, and its activation is the confidence. The feature vector values are copied
	 * back into {@code featureVectorArr} after the first layer; they are not changed.
	 *
	 * @param nInps            number of input nodes; the first {@code nInps} elements of the feature vector are used
	 * @param nHids            number of hidden nodes; must not exceed {@code IMlp.MAX_NHIDS}
	 * @param nOuts            number of output nodes (classes)
	 * @param acFuncHidsCode   activation function for the hidden nodes: {@code IMlp.LINEAR}, {@code IMlp.SIGMOID}
	 *                         or {@code IMlp.SINUSOID} (defined in NIST {@code parms.h})
	 * @param acFuncOutsCode   activation function for the output nodes (same codes)
	 * @param weights          the MLP weights and biases, laid out as described in the class documentation
	 * @param featureVectorArr the feature vector to classify
	 * @param outAcs           output: the output activations; must be allocated by the caller with at least
	 *                         {@code nOuts} elements
	 * @param hypClass         output: the hypothetical class, an integer from 0 to {@code nOuts - 1}
	 * @param confidence       output: a value from 0 to 1, defined as {@code outAcs[hypClass]} (the highest
	 *                         output activation)
	 * @return 0 on success; -2 if {@code nHids > IMlp.MAX_NHIDS}; -3 if {@code acFuncHidsCode} is unsupported;
	 *         -4 if {@code acFuncOutsCode} is unsupported
	 */
	public int runMlp2(final int nInps, final int nHids, final int nOuts, final int acFuncHidsCode,
			final int acFuncOutsCode, AtomicReferenceArray<Double> weights, double[] featureVectorArr,
			AtomicReferenceArray<Double> outAcs, AtomicInteger hypClass, AtomicReference<Double> confidence) {
		AtomicReference<Character> runMlp2T = new AtomicReference<>();
		runMlp2T.set('t');

		AtomicInteger runMlp2I1 = new AtomicInteger();
		runMlp2I1.set(1);

		AtomicReference<Double> runMlp2F1 = new AtomicReference<Double>();
		runMlp2F1.set(1.0d);

		double[] hidacsArr = new double[IMlp.MAX_NHIDS];
		double maxac = 0.0d;
		double ac;

		if (nHids > IMlp.MAX_NHIDS) {
			logger.error("ERROR : runmlp2 : nHids : {} > {}", nHids, IMlp.MAX_NHIDS);
			return (-2);
		}

		/* Where the weights and biases of the two layers begin in weights. */
		int wIndex = 0;
		int w1Index = wIndex;
		int b1Index = w1Index + nHids * nInps;
		int w2Index = b1Index + nHids;
		int b2Index = w2Index + nOuts * nHids;

		/* Start hidden activations out as first-layer biases. */
		int index = 0;
		for (index = 0; index < nHids; index++)
			hidacsArr[index] = weights.get(b1Index + index);

		AtomicReferenceArray<Double> hidacs = new AtomicReferenceArray<>(hidacsArr.length);
		for (index = 0; index < hidacsArr.length; index++) {
			hidacs.set(index, hidacsArr[index]);
		}

		AtomicReferenceArray<Double> featvec = new AtomicReferenceArray<>(featureVectorArr.length);
		for (index = 0; index < featureVectorArr.length; index++) {
			featvec.set(index, featureVectorArr[index]);
		}

		AtomicReferenceArray<Double> w1 = new AtomicReferenceArray<>(weights.length() - w1Index);
		for (index = 0; index < w1.length(); index++) {
			w1.set(index, weights.get(w1Index + index));
		}

		/* Add product of first-layer weights with feature vector. */
		getMlpCla().mlpSgemV(runMlp2T, nInps, nHids, runMlp2F1, w1, nInps, featvec, runMlp2I1, runMlp2F1, hidacs,
				runMlp2I1);

		for (index = 0; index < featvec.length(); index++) {
			featureVectorArr[index] = featvec.get(index);
		}

		int pIndex = 0;
		int peIndex = pIndex + nHids;
		/* Finish each hidden activation by applying activation function. */
		for (; pIndex < peIndex; pIndex++) {
			/* Resolve the activation function codes to functions. */
			switch (acFuncHidsCode) {
			case IMlp.LINEAR:
				getAcs().acVLinear(hidacs, pIndex);
				break;
			case IMlp.SIGMOID:
				getAcs().acVSigmoid(hidacs, pIndex);
				break;
			case IMlp.SINUSOID:
				getAcs().acVSinusoid(hidacs, pIndex);
				break;
			default:
				logger.error("ERROR : runmlp2 : acFuncHidsCode :{} unsupported\n", acFuncHidsCode);
				return (-3);
			}
		}

		/* Same steps again for second layer. */
		AtomicReferenceArray<Double> b2 = new AtomicReferenceArray<>(weights.length() - b2Index);
		for (index = 0; index < b2.length(); index++) {
			b2.set(index, weights.get(b2Index + index));
		}

		for (index = 0; index < nOuts; index++) {
			outAcs.set(index, b2.get(index));
		}

		AtomicReferenceArray<Double> w2 = new AtomicReferenceArray<>(weights.length() - w2Index);
		for (index = 0; index < w2.length(); index++) {
			w2.set(index, weights.get(w2Index + index));
		}

		getMlpCla().mlpSgemV(runMlp2T, nHids, nOuts, runMlp2F1, w2, nHids, hidacs, runMlp2I1, runMlp2F1, outAcs,
				runMlp2I1);

		pIndex = 0;
		peIndex = pIndex + nOuts;
		/* Finish each hidden activation by applying activation function. */
		for (; pIndex < peIndex; pIndex++) {
			switch (acFuncOutsCode) {
			case IMlp.LINEAR:
				getAcs().acVLinear(outAcs, pIndex);
				break;
			case IMlp.SIGMOID:
				getAcs().acVSigmoid(outAcs, pIndex);
				break;
			case IMlp.SINUSOID:
				getAcs().acVSinusoid(outAcs, pIndex);
				break;
			default:
				logger.error("ERROR : runmlp2 : acFuncOutsCode : {} unsupported\n", acFuncOutsCode);
				return (-4);
			}
		}

		/*
		 * Find the hypothetical class -- the class whose output node activated most
		 * strongly -- and the confidence -- that activation value.
		 */

		pIndex = 0;
		int maxacpIndex = pIndex;
		peIndex = maxacpIndex + nOuts;

		for (maxac = outAcs.get(pIndex), pIndex++; pIndex < peIndex; pIndex++) {
			if ((ac = outAcs.get(pIndex)) > maxac) {
				maxac = ac;
				maxacpIndex = pIndex;
			}
		}

		hypClass.set(maxacpIndex);
		confidence.set(maxac);

		return 0;
	}
}