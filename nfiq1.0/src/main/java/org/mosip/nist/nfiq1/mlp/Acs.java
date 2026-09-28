package org.mosip.nist.nfiq1.mlp;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.mosip.nist.nfiq1.common.IMlp.IAcs;

/**
 * Activation functions (and their derivatives) for the NFIQ multi-layer perceptron (MLP) classifier.
 * <p>
 * Port of NIST's MLP activation routines ({@code acs.c}: {@code ac_sinusoid}, {@code ac_v_sinusoid},
 * {@code ac_sigmoid}, {@code ac_v_sigmoid}, {@code ac_linear}, {@code ac_v_linear}). The "value only"
 * ({@code acV*}) variants update an element of an array in place and are what {@link RunMlp} applies to the
 * hidden and output layers.
 * <p>
 * Lazily created singleton; {@link #getInstance()} is synchronized and the class keeps no mutable state.
 */
public class Acs extends Mlp implements IAcs {
	/** Lazily initialized singleton instance; guarded by the class lock in {@link #getInstance()}. */
	private static Acs instance;

	/**
	 * Private constructor enforcing the singleton pattern; use {@link #getInstance()}.
	 */
	private Acs() {
		super();
	}

	/**
	 * Returns the shared {@code Acs} singleton, creating it on first use.
	 *
	 * @return the singleton instance (never {@code null})
	 */
	public static synchronized Acs getInstance() {
		if (instance == null) {
			instance = new Acs();
		}
		return instance;
	}

	/**
	 * Sinusoid activation function and its derivative (NIST {@code ac_sinusoid}).
	 * <p>
	 * Computes {@code val = 0.5 * (1 + sin(0.5 * x))} and {@code deriv = 0.25 * cos(0.5 * x)}. Scaling by 0.5
	 * before the sine, then adding 1 and scaling by 0.5 afterwards, gives the function sigmoid-like properties:
	 * its range is [0, 1] (almost the same as the sigmoid's (0, 1)), its value at 0 is 1/2 and its derivative at
	 * 0 is 1/4.
	 *
	 * @param x     the input (net activation) value
	 * @param val   output: the activation value
	 * @param deriv output: the derivative of the activation at {@code x}
	 */
	public void acSinusoid(float x, AtomicReference<Float> val, AtomicReference<Float> deriv) {
		double a;

		a = 0.5f * x;
		val.set(0.5f * (1.0f + (float) Math.sin(a)));
		deriv.set(0.25f * (float) Math.cos(a));
	}

	/**
	 * Sinusoid activation function, value only, applied in place (NIST {@code ac_v_sinusoid}).
	 * <p>
	 * Replaces {@code p[index]} with {@code 0.5 * (1 + sin(0.5 * p[index]))}.
	 *
	 * @param p     input/output array: element {@code index} holds the input value and receives the result
	 * @param index position of the element to transform
	 */
	public void acVSinusoid(AtomicReferenceArray<Double> p, int index) {
		p.set(index, (0.5d * (1.0d + Math.sin(0.5d * p.get(index)))));
	}

	/**
	 * Sigmoid (logistic) activation function and its derivative (NIST {@code ac_sigmoid}).
	 * <p>
	 * Computes {@code val = 1 / (1 + exp(-x))}, or 0 when {@code x < SMIN}, and {@code deriv = val * (1 - val)}.
	 * {@code SMIN} is a large negative number chosen so that {@code exp(-SMIN)}, a large positive number, only
	 * just avoids overflow.
	 *
	 * @param x     the input (net activation) value
	 * @param val   output: the activation value, in [0, 1)
	 * @param deriv output: the derivative of the activation at {@code x}
	 */
	public void acSigmoid(double x, AtomicReference<Double> val, AtomicReference<Double> deriv) {
		double v = (x >= SMIN ? 1.0d / (1.0d + Math.exp(-x)) : 0.0d);
		val.set(v);
		deriv.set(v * (1.0d - v));
	}

	/**
	 * Sigmoid (logistic) activation function, value only, applied in place (NIST {@code ac_v_sigmoid}).
	 * <p>
	 * Replaces {@code p[index]} with {@code 1 / (1 + exp(-p[index]))}, or 0 when the input is below
	 * {@code SMIN}.
	 *
	 * @param p     input/output array: element {@code index} holds the input value and receives the result
	 * @param index position of the element to transform
	 */
	public void acVSigmoid(AtomicReferenceArray<Double> p, int index) {
		p.set(index, (p.get(index) >= SMIN ? 1.0d / (1.0d + Math.exp(-p.get(index))) : 0.0d));
	}

	/**
	 * Linear activation function and its derivative (NIST {@code ac_linear}).
	 * <p>
	 * Computes {@code val = 0.25 * x} and {@code deriv = 0.25}.
	 *
	 * @param x     the input (net activation) value
	 * @param val   output: the activation value
	 * @param deriv output: the derivative, always 0.25
	 */
	public void acLinear(float x, AtomicReference<Double> val, AtomicReference<Double> deriv) {
		val.set(0.25d * x);
		deriv.set(0.25d);
	}

	/**
	 * Linear activation function, value only, applied in place (NIST {@code ac_v_linear}).
	 * <p>
	 * Replaces {@code p[index]} with {@code 0.25 * p[index]}.
	 *
	 * @param p     input/output array: element {@code index} holds the input value and receives the result
	 * @param index position of the element to transform
	 */
	public void acVLinear(AtomicReferenceArray<Double> p, int index) {
		p.set(index, 0.25f * p.get(index));
	}
}