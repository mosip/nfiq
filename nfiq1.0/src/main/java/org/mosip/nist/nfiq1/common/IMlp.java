package org.mosip.nist.nfiq1.common;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Constants, data structures and contracts of NIST's multi-layer perceptron (MLP) library (port of
 * {@code mlp.h} / {@code parms.h}).
 * <p>
 * NFIQ only uses the feed-forward classification part ({@link IRunMlp}) with activation functions from
 * {@link IAcs}; the remaining constants and parameter structures come from the original MLP training/testing
 * tools and are kept for completeness.
 */
public interface IMlp {
	/**
	 * First column (0-based) of the first line of a warning/error message produced while scanning a specfile
	 * (used by {@code strm_fmt()} and {@code lgl_tbl()}).
	 */
	public static final int MESSAGE_FIRSTCOL_FIRSTLINE = 6; // for first line of a msg
	/** First column (0-based) of continuation lines of a specfile warning/error message (indented). */
	public static final int MESSAGE_FIRSTCOL_LATERLINES = 8; // later lines indented
	/** Last column (0-based) of a specfile warning/error message line. */
	public static final int MESSAGE_LASTCOL = 70;
	/** First column (0-based) of table lines in a specfile message (indented further). */
	public static final int MESSAGE_FIRSTCOL_TABLE = 12; // table indented even more

	/** {@code get_phr()} return value: a name/value word pair was read. */
	public static final int WORD_PAIR = 0;
	/** {@code get_phr()} return value: start of a new run. */
	public static final int NEWRUN = 1;
	/** {@code get_phr()} return value: an illegal phrase was encountered. */
	public static final int ILLEGAL_PHRASE = 2;
	/** {@code get_phr()} return value: end of input reached. */
	public static final int FINISHED = 3;

	/** Minimum allowed step size in the MLP optimizer line search. */
	public static final double STPMIN = 1.e-20;
	/** Maximum allowed step size in the MLP optimizer line search. */
	public static final double STPMAX = 1.e+20;

	/** Maximum number of values handled by the median computation. */
	public static final int MAXMED = 100000;
	/** Maximum string length of a long class name. */
	public static final int LONG_CLASSNAME_MAXSTRLEN = 32;

	/** {@code a_type} argument of {@code mtch_pnm}: parameter is a file name. */
	public static final int MP_FILENAME = 0;
	/** {@code a_type} argument of {@code mtch_pnm}: parameter is an integer. */
	public static final int MP_INT = 1;
	/** {@code a_type} argument of {@code mtch_pnm}: parameter is a float. */
	public static final int MP_FLOAT = 2;
	/** {@code a_type} argument of {@code mtch_pnm}: parameter is a switch. */
	public static final int MP_SWITCH = 3;

	/** Read mode: integer values. */
	public static final int RD_INT = 0;
	/** Read mode: floating-point values. */
	public static final int RD_FLOAT = 1;

	/** Starting value for {@code xl} in the MLP optimizer. */
	public static final double XLSTART = 0.01; // Starting value for xl.
	/** Training does not stop until {@code NF * nfreq} iterations (or the other limits below) are reached. */
	public static final int NF = 3; // Don't quit until NF * nfreq iters or...
	/** Training does not stop until {@code NITER} iterations, whichever of the limits is larger. */
	public static final int NITER = 40; // ...until NITER iters, whichever is larger...
	/** Training does not stop until {@code NBOLTZ} iterations when doing Boltzmann pruning. */
	public static final int NBOLTZ = 100; // ...until NBOLTZ iters, if doing Boltzmann.
	/** Training quits if the error has not improved {@code NNOT} times in a row. */
	public static final int NNOT = 3; // Quit if not improving NNOT times in row.
	/** Training restarts after {@code NRESTART} iterations. */
	public static final int NRESTART = 100000; // Restart after NRESTART iterations.

	/** Parameter type: file name. */
	public static final int PARMTYPE_FILENAME = 0;
	/** Parameter type: integer. */
	public static final int PARMTYPE_INT = 1;
	/** Parameter type: float. */
	public static final int PARMTYPE_FLOAT = 2;
	/** Parameter type: switch. */
	public static final int PARMTYPE_SWITCH = 3;

	/** Maximum length of a file-name parameter value. */
	public static final int PARM_FILENAME_VAL_DIM = 100;

	/** {@code errfunc} value: mean squared error. */
	public static final int MSE = 0;
	/** {@code errfunc} value: type-1 error. */
	public static final int TYPE_1 = 1;
	/** {@code errfunc} value: positive sum error. */
	public static final int POS_SUM = 2;

	/** {@code purpose} value: the MLP is a classifier. */
	public static final int CLASSIFIER = 0;
	/** {@code purpose} value: the MLP is a function fitter. */
	public static final int FITTER = 1;

	/** {@code boltzmann} value: no pruning. */
	public static final int NO_PRUNE = 0;
	/** {@code boltzmann} value: absolute-value pruning. */
	public static final int ABS_PRUNE = 2;
	/** {@code boltzmann} value: square pruning. */
	public static final int SQUARE_PRUNE = 3;

	/** {@code train_or_test} value: training run. */
	public static final int TRAIN = 0;
	/** {@code train_or_test} value: testing run. */
	public static final int TEST = 1;

	/** Activation function code ({@code acfunc_hids}/{@code acfunc_outs}): sinusoid, {@code 0.5 * (1 + sin(x))}. */
	public static final int SINUSOID = 0;
	/** Activation function code: sigmoid, {@code 1 / (1 + exp(-x))}. */
	public static final int SIGMOID = 1;
	/** Activation function code: linear. */
	public static final int LINEAR = 2;
	/** Activation function code signalling an invalid/unknown activation function. */
	public static final int BAD_AC_CODE = 127;

	/** {@code priors} value: all classes have the same prior. */
	public static final int ALLSAME = 0;
	/** {@code priors} value: per-class priors. */
	public static final int CLASS = 1;
	/** {@code priors} value: per-pattern priors. */
	public static final int PATTERN = 2;
	/** {@code priors} value: both class and pattern priors. */
	public static final int BOTH = 3;

	/** {@code patsfile_ascii_or_binary} value: patterns file is ASCII. */
	public static final int ASCII = 0;
	/** {@code patsfile_ascii_or_binary} value: patterns file is binary. */
	public static final int BINARY = 1;

	/** Maximum number of hidden nodes supported. */
	public static final int MAX_NHIDS = 1000; // Maximum number of hidden nodes
	/** Patterns file marker for a tree patterns file. */
	public static final int TREEPATSFILE = 5151;
	/** Patterns file marker for a plain patterns file. */
	public static final int JUSTPATSFILE = 0;
	/** Number of items per line when formatting output. */
	public static final int FMT_ITEMS = 8;

	/** Name/value/error/ok/line-number record produced while parsing a specfile ({@code NVEOL}). */
	@Getter
	@Setter
	@Data
	public class NVEOL {
		/** Parameter name. */
		private String namestr;
		/** Parameter value string. */
		private String valstr;
		/** Error message, if any. */
		private String errstr;
		/** Whether the entry was parsed successfully. */
		private char ok;
		/** Line number in the specfile. */
		private int linenum;
	}

	/** Two-dimensional character array descriptor ({@code TDA_CHAR}). */
	@Getter
	@Setter
	@Data
	public class TDACHAR {
		/** Length of the second dimension (row length). */
		private int dim2;
		/** Backing buffer. */
		private String buf;
	}

	/** Two-dimensional integer array descriptor ({@code TDA_INT}). */
	@Getter
	@Setter
	@Data
	public class TDAINT {
		/** Length of the second dimension (row length). */
		private int dim2;
		/** Backing buffer. */
		private AtomicInteger buf;
	}

	/** Two-dimensional float array descriptor ({@code TDA_FLOAT}). */
	@Getter
	@Setter
	@Data
	public class TDAFLOAT {
		/** Length of the second dimension (row length). */
		private int dim2;
		/** Backing buffer. */
		private AtomicReference<Float> buf;
	}

	/** "Set-tried / set / line number" status of a specfile parameter ({@code SSL}). */
	@Getter
	@Setter
	@Data
	public class SSL {
		/** Whether setting the parameter was attempted. */
		private char set_tried;
		/** Whether the parameter was successfully set. */
		private char set;
		/** Specfile line number where the parameter was set. */
		private int linenum;
	}

	/** File-name MLP parameter with its set status ({@code PARM_FILENAME}). */
	@Getter
	@Setter
	@Data
	public class PARMFILENAME {
		/** Parameter value (file name). */
		private String val = new String(new char[IMlp.PARM_FILENAME_VAL_DIM]);
		/** Set status of the parameter. */
		private SSL ssl = new SSL();
	}

	/** Integer MLP parameter with its set status ({@code PARM_INT}). */
	@Getter
	@Setter
	@Data
	public class PARMINT {
		/** Parameter value. */
		private int val;
		/** Set status of the parameter. */
		private SSL ssl = new SSL();
	}

	/** Float MLP parameter with its set status ({@code PARM_FLOAT}). */
	@Getter
	@Setter
	@Data
	public class PARMFLOAT {
		/** Parameter value. */
		private float val;
		/** Set status of the parameter. */
		private SSL ssl = new SSL();
	}

	/** Switch MLP parameter with its set status ({@code PARM_SWITCH}). */
	@Getter
	@Setter
	@Data
	public class PARMSWITCH {
		/** Parameter value (switch code). */
		private char val;
		/** Set status of the parameter. */
		private SSL ssl = new SSL();
	}

	/** Full set of MLP training/testing parameters read from a specfile ({@code PARMS}). */
	@Getter
	@Setter
	@Data
	public class PARMS {
		/** Long output file. */
		private PARMFILENAME longOutfile = new PARMFILENAME();
		/** Short output file. */
		private PARMFILENAME shortOutfile = new PARMFILENAME();
		/** Input patterns file. */
		private PARMFILENAME patternsInfile = new PARMFILENAME();
		/** Input weights file. */
		private PARMFILENAME wtsInfile = new PARMFILENAME();
		/** Output weights file. */
		private PARMFILENAME wtsOutfile = new PARMFILENAME();
		/** Input class-weights file. */
		private PARMFILENAME classWtsInfile = new PARMFILENAME();
		/** Input pattern-weights file. */
		private PARMFILENAME patternWtsInfile = new PARMFILENAME();
		/** Input file for the local-connection scheme. */
		private PARMFILENAME lcnScnInfile = new PARMFILENAME();
		/** Number of patterns. */
		private PARMINT npats = new PARMINT();
		/** Number of input nodes. */
		private PARMINT ninps = new PARMINT();
		/** Number of hidden nodes. */
		private PARMINT nhids = new PARMINT();
		/** Number of output nodes. */
		private PARMINT nouts = new PARMINT();
		/** Random number generator seed. */
		private PARMINT seed = new PARMINT();
		/** Maximum number of training iterations. */
		private PARMINT niterMax = new PARMINT();
		/** Frequency (in iterations) of progress checks. */
		private PARMINT nfreq = new PARMINT();
		/** Number of allowed "not OK" iterations before stopping. */
		private PARMINT nokdel = new PARMINT();
		/** Memory size of the L-BFGS optimizer. */
		private PARMINT lbfgsMem = new PARMINT();
		/** Regularization factor. */
		private PARMFLOAT regfac = new PARMFLOAT();
		/** Alpha (learning rate / momentum) factor. */
		private PARMFLOAT alpha = new PARMFLOAT();
		/** Boltzmann temperature. */
		private PARMFLOAT temperature = new PARMFLOAT();
		/** Error goal. */
		private PARMFLOAT egoal = new PARMFLOAT();
		/** Gradient-to-weight ratio goal. */
		private PARMFLOAT gwgoal = new PARMFLOAT();
		/** Error delta threshold. */
		private PARMFLOAT errdel = new PARMFLOAT();
		/** OK level threshold. */
		private PARMFLOAT oklvl = new PARMFLOAT();
		/** Target offset. */
		private PARMFLOAT trgoff = new PARMFLOAT();
		/** Early-stop percentage for scaled conjugate gradient. */
		private PARMFLOAT scgEarlystopPct = new PARMFLOAT();
		/** Gradient tolerance for L-BFGS. */
		private PARMFLOAT lbfgsGtol = new PARMFLOAT();
		/** Error function switch ({@link IMlp#MSE}, {@link IMlp#TYPE_1}, {@link IMlp#POS_SUM}). */
		private PARMSWITCH errfunc = new PARMSWITCH();
		/** Purpose switch ({@link IMlp#CLASSIFIER} or {@link IMlp#FITTER}). */
		private PARMSWITCH purpose = new PARMSWITCH();
		/** Boltzmann pruning switch. */
		private PARMSWITCH boltzmann = new PARMSWITCH();
		/** Train-or-test switch. */
		private PARMSWITCH trainOrTest = new PARMSWITCH();
		/** Hidden-layer activation function switch. */
		private PARMSWITCH acfunc_hids = new PARMSWITCH();
		/** Output-layer activation function switch. */
		private PARMSWITCH acfuncOuts = new PARMSWITCH();
		/** Priors switch. */
		private PARMSWITCH priors = new PARMSWITCH();
		/** Patterns file format switch (ASCII or binary). */
		private PARMSWITCH patsfileAsciiOrBinary = new PARMSWITCH();
		/** Whether to produce a confusion matrix. */
		private PARMSWITCH doConfuse = new PARMSWITCH();
		/** Whether to show activations multiplied by 1000. */
		private PARMSWITCH showAcsTimes1000 = new PARMSWITCH();
		/** Whether to produce a correct-vs-rejected table. */
		private PARMSWITCH doCvr = new PARMSWITCH();
	}

	/** Loaded MLP model: topology, activation functions and weights ({@code MLP_PARAM}). */
	@Getter
	@Setter
	@Data
	public class MlpParam {
		/** Class names indexed by output node. */
		private String[] classMap;
		/** Number of input nodes. */
		private int ninps;
		/** Number of hidden nodes. */
		private int nhids;
		/** Number of output nodes. */
		private int nouts;
		/** Hidden-layer activation function code. */
		private char acfuncHids;
		/** Output-layer activation function code. */
		private char acfuncOuts;
		/** Network weights. */
		private AtomicReference<Float> weights;
		/** Class string buffer. */
		private String clsStr = new String(new char[50]);
		/** Number of rows of the input transform matrix. */
		private int trnsfrmRws;
		/** Number of columns of the input transform matrix. */
		private int trnsfrmCls;
	}

	/***********************************************************************/
	/**
	 * Activation functions and their derivatives (port of NIST's {@code acs.c}); implemented by
	 * {@code org.mosip.nist.nfiq1.mlp.Acs}.
	 */
	public interface IAcs {
		/** Lower input bound for the sigmoid; below it the activation and derivative are taken as 0. */
		public static final double SMIN = -1.e6; // ok for now

		/**
		 * Sinusoid activation: {@code val = 0.5 * (1 + sin(x))}, {@code deriv = 0.5 * cos(x)}.
		 *
		 * @param x     input value
		 * @param val   output receiving the activation value
		 * @param deriv output receiving the derivative
		 */
		public void acSinusoid(float x, AtomicReference<Float> val, AtomicReference<Float> deriv);

		/**
		 * Sigmoid activation: {@code val = 1 / (1 + exp(-x))}, {@code deriv = val * (1 - val)}; both are 0 when
		 * {@code x} is below {@link #SMIN}.
		 *
		 * @param x     input value
		 * @param val   output receiving the activation value
		 * @param deriv output receiving the derivative
		 */
		public void acSigmoid(double x, AtomicReference<Double> val, AtomicReference<Double> deriv);

		/**
		 * Linear activation: {@code val = 0.25 * x}, {@code deriv = 0.25}.
		 *
		 * @param x     input value
		 * @param val   output receiving the activation value
		 * @param deriv output receiving the derivative
		 */
		public void acLinear(float x, AtomicReference<Double> val, AtomicReference<Double> deriv);

		/**
		 * Applies the sinusoid activation in place to the element at {@code index}.
		 *
		 * @param p     array of activations; element {@code index} is overwritten
		 * @param index index of the element to transform
		 */
		public void acVSinusoid(AtomicReferenceArray<Double> p, int index);

		/**
		 * Applies the sigmoid activation in place to the element at {@code index}.
		 *
		 * @param p     array of activations; element {@code index} is overwritten
		 * @param index index of the element to transform
		 */
		public void acVSigmoid(AtomicReferenceArray<Double> p, int index);

		/**
		 * Applies the linear activation in place to the element at {@code index}.
		 *
		 * @param p     array of activations; element {@code index} is overwritten
		 * @param index index of the element to transform
		 */
		public void acVLinear(AtomicReferenceArray<Double> p, int index);
	}

	/***********************************************************************/
	/**
	 * Feed-forward MLP utilities (port of NIST's {@code runmlp.c}); implemented by
	 * {@code org.mosip.nist.nfiq1.mlp.RunMlp}.
	 */
	public interface IRunMlp {
		/**
		 * Runs a feature vector through a two-layer feed-forward MLP and reports the winning class (NIST
		 * {@code runmlp}).
		 *
		 * @param nInps            number of input nodes (feature vector length)
		 * @param nHids            number of hidden nodes
		 * @param nOuts            number of output nodes (classes)
		 * @param acFuncHidsCode   hidden-layer activation function code ({@link IMlp#SINUSOID}, {@link IMlp#SIGMOID}
		 *                         or {@link IMlp#LINEAR})
		 * @param acFuncOutsCode   output-layer activation function code
		 * @param weights          network weights: input-to-hidden weights and biases followed by hidden-to-output
		 *                         weights and biases
		 * @param featureVectorArr input feature vector (length {@code nInps})
		 * @param outAcs           output receiving the {@code nOuts} output activations
		 * @param hypClass         output receiving the zero-based index of the output with the highest activation
		 * @param confidence       output receiving the highest output activation
		 */
		@SuppressWarnings({ "java:S107" })
		public void runMlp(final int nInps, final int nHids, final int nOuts, final int acFuncHidsCode,
				final int acFuncOutsCode, AtomicReferenceArray<Double> weights, double[] featureVectorArr,
				AtomicReferenceArray<Double> outAcs, AtomicInteger hypClass, AtomicReference<Double> confidence);

		/**
		 * Runs a feature vector through a two-layer feed-forward MLP, validating activation codes (NIST
		 * {@code runmlp2}).
		 *
		 * @param nInps            number of input nodes (feature vector length)
		 * @param nHids            number of hidden nodes
		 * @param nOuts            number of output nodes (classes)
		 * @param acFuncHidsCode   hidden-layer activation function code ({@link IMlp#SINUSOID}, {@link IMlp#SIGMOID}
		 *                         or {@link IMlp#LINEAR})
		 * @param acFuncOutsCode   output-layer activation function code
		 * @param weights          network weights: input-to-hidden weights and biases followed by hidden-to-output
		 *                         weights and biases
		 * @param featureVectorArr input feature vector (length {@code nInps})
		 * @param outAcs           output receiving the {@code nOuts} output activations
		 * @param hypClass         output receiving the zero-based index of the output with the highest activation
		 * @param confidence       output receiving the highest output activation
		 * @return {@code 0} on success, or a non-zero error code (e.g. for an unsupported activation function code)
		 */
		@SuppressWarnings({ "java:S107" })
		public int runMlp2(final int nInps, final int nHids, final int nOuts, final int acFuncHidsCode,
				final int acFuncOutsCode, AtomicReferenceArray<Double> weights, double[] featureVectorArr,
				AtomicReferenceArray<Double> outAcs, AtomicInteger hypClass, AtomicReference<Double> confidence);
	}
}