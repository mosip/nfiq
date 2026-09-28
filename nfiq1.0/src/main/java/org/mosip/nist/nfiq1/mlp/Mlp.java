package org.mosip.nist.nfiq1.mlp;

import org.mosip.nist.nfiq1.Nist;

/**
 * Common base class for the NFIQ multi-layer perceptron (MLP) classifier components: {@link Acs} (activation
 * functions), {@link MlpCla} (BLAS/LAPACK-style linear algebra) and {@link RunMlp} (network evaluation).
 * <p>
 * It adds no behaviour of its own; it mirrors NIST's {@code mlp} library as a shared supertype derived from
 * {@link Nist}.
 */
public class Mlp extends Nist {
}