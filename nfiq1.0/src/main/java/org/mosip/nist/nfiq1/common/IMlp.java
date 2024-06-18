package org.mosip.nist.nfiq1.common;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public interface IMlp {
	/*
	 * For use by strm_fmt() and lgl_tbl(), which format the warning and error
	 * messages that may be written as the result of scanning a specfile. Columns
	 * are numbered starting at 0.
	 */
	public static final int MESSAGE_FIRSTCOL_FIRSTLINE = 6; // for first line of a msg
	public static final int MESSAGE_FIRSTCOL_LATERLINES = 8; // later lines indented
	public static final int MESSAGE_LASTCOL = 70;
	public static final int MESSAGE_FIRSTCOL_TABLE = 12; // table indented even more

	/* Names of get_phr()'s return values: */
	public static final int WORD_PAIR = 0;
	public static final int NEWRUN = 1;
	public static final int ILLEGAL_PHRASE = 2;
	public static final int FINISHED = 3;

	public static final double STPMIN = 1.e-20;
	public static final double STPMAX = 1.e+20;

	public static final int MAXMED = 100000;
	public static final int LONG_CLASSNAME_MAXSTRLEN = 32;

	/* Names of the values of the a_type parm of mtch_pnm. */
	public static final int MP_FILENAME = 0;
	public static final int MP_INT = 1;
	public static final int MP_FLOAT = 2;
	public static final int MP_SWITCH = 3;

	public static final int RD_INT = 0;
	public static final int RD_FLOAT = 1;

	public static final double XLSTART = 0.01; // Starting value for xl.
	public static final int NF = 3; // Don't quit until NF * nfreq iters or...
	public static final int NITER = 40; // ...until NITER iters, whichever is larger...
	public static final int NBOLTZ = 100; // ...until NBOLTZ iters, if doing Boltzmann.
	public static final int NNOT = 3; // Quit if not improving NNOT times in row.
	public static final int NRESTART = 100000; // Restart after NRESTART iterations.

	public static final int PARMTYPE_FILENAME = 0;
	public static final int PARMTYPE_INT = 1;
	public static final int PARMTYPE_FLOAT = 2;
	public static final int PARMTYPE_SWITCH = 3;

	public static final int PARM_FILENAME_VAL_DIM = 100;

	/* For errfunc: */
	public static final int MSE = 0;
	public static final int TYPE_1 = 1;
	public static final int POS_SUM = 2;

	/* For purpose: */
	public static final int CLASSIFIER = 0;
	public static final int FITTER = 1;

	/* For boltzmann: */
	public static final int NO_PRUNE = 0;
	public static final int ABS_PRUNE = 2;
	public static final int SQUARE_PRUNE = 3;

	/* For train_or_test: */
	public static final int TRAIN = 0;
	public static final int TEST = 1;

	/* For acfunc_hids and acfunc_outs: */
	public static final int SINUSOID = 0;
	public static final int SIGMOID = 1;
	public static final int LINEAR = 2;
	public static final int BAD_AC_CODE = 127;

	/* For priors: */
	public static final int ALLSAME = 0;
	public static final int CLASS = 1;
	public static final int PATTERN = 2;
	public static final int BOTH = 3;

	/* For patsfile_ascii_or_binary: */
	public static final int ASCII = 0;
	public static final int BINARY = 1;

	public static final int MAX_NHIDS = 1000; // Maximum number of hidden nodes
	public static final int TREEPATSFILE = 5151;
	public static final int JUSTPATSFILE = 0;
	public static final int FMT_ITEMS = 8;

	@Getter
	@Setter
	@Data
	public class NVEOL {
		private String namestr;
		private String valstr;
		private String errstr;
		private char ok;
		private int linenum;
	}

	@Getter
	@Setter
	@Data
	public class TDACHAR {
		private int dim2;
		private String buf;
	}

	@Getter
	@Setter
	@Data
	public class TDAINT {
		private int dim2;
		private AtomicInteger buf;
	}

	@Getter
	@Setter
	@Data
	public class TDAFLOAT {
		private int dim2;
		private AtomicReference<Float> buf;
	}

	@Getter
	@Setter
	@Data
	public class SSL {
		private char set_tried;
		private char set;
		private int linenum;
	}

	@Getter
	@Setter
	@Data
	public class PARMFILENAME {
		private String val = new String(new char[IMlp.PARM_FILENAME_VAL_DIM]);
		private SSL ssl = new SSL();
	}

	@Getter
	@Setter
	@Data
	public class PARMINT {
		private int val;
		private SSL ssl = new SSL();
	}

	@Getter
	@Setter
	@Data
	public class PARMFLOAT {
		private float val;
		private SSL ssl = new SSL();
	}

	@Getter
	@Setter
	@Data
	public class PARMSWITCH {
		private char val;
		private SSL ssl = new SSL();
	}

	@Getter
	@Setter
	@Data
	public class PARMS {
		private PARMFILENAME longOutfile = new PARMFILENAME();
		private PARMFILENAME shortOutfile = new PARMFILENAME();
		private PARMFILENAME patternsInfile = new PARMFILENAME();
		private PARMFILENAME wtsInfile = new PARMFILENAME();
		private PARMFILENAME wtsOutfile = new PARMFILENAME();
		private PARMFILENAME classWtsInfile = new PARMFILENAME();
		private PARMFILENAME patternWtsInfile = new PARMFILENAME();
		private PARMFILENAME lcnScnInfile = new PARMFILENAME();
		private PARMINT npats = new PARMINT();
		private PARMINT ninps = new PARMINT();
		private PARMINT nhids = new PARMINT();
		private PARMINT nouts = new PARMINT();
		private PARMINT seed = new PARMINT();
		private PARMINT niterMax = new PARMINT();
		private PARMINT nfreq = new PARMINT();
		private PARMINT nokdel = new PARMINT();
		private PARMINT lbfgsMem = new PARMINT();
		private PARMFLOAT regfac = new PARMFLOAT();
		private PARMFLOAT alpha = new PARMFLOAT();
		private PARMFLOAT temperature = new PARMFLOAT();
		private PARMFLOAT egoal = new PARMFLOAT();
		private PARMFLOAT gwgoal = new PARMFLOAT();
		private PARMFLOAT errdel = new PARMFLOAT();
		private PARMFLOAT oklvl = new PARMFLOAT();
		private PARMFLOAT trgoff = new PARMFLOAT();
		private PARMFLOAT scgEarlystopPct = new PARMFLOAT();
		private PARMFLOAT lbfgsGtol = new PARMFLOAT();
		private PARMSWITCH errfunc = new PARMSWITCH();
		private PARMSWITCH purpose = new PARMSWITCH();
		private PARMSWITCH boltzmann = new PARMSWITCH();
		private PARMSWITCH trainOrTest = new PARMSWITCH();
		private PARMSWITCH acfunc_hids = new PARMSWITCH();
		private PARMSWITCH acfuncOuts = new PARMSWITCH();
		private PARMSWITCH priors = new PARMSWITCH();
		private PARMSWITCH patsfileAsciiOrBinary = new PARMSWITCH();
		private PARMSWITCH doConfuse = new PARMSWITCH();
		private PARMSWITCH showAcsTimes1000 = new PARMSWITCH();
		private PARMSWITCH doCvr = new PARMSWITCH();
	}

	@Getter
	@Setter
	@Data
	public class MlpParam {
		private String[] classMap;
		private int ninps;
		private int nhids;
		private int nouts;
		private char acfuncHids;
		private char acfuncOuts;
		private AtomicReference<Float> weights;
		private String clsStr = new String(new char[50]);
		private int trnsfrmRws;
		private int trnsfrmCls;
	}

	/***********************************************************************/
	/* Acs.java : */
	public interface IAcs {
		public static final double SMIN = -1.e6; // ok for now

		public void acSinusoid(float x, AtomicReference<Float> val, AtomicReference<Float> deriv);

		public void acSigmoid(double x, AtomicReference<Double> val, AtomicReference<Double> deriv);

		public void acLinear(float x, AtomicReference<Double> val, AtomicReference<Double> deriv);

		public void acVSinusoid(AtomicReferenceArray<Double> p, int index);

		public void acVSigmoid(AtomicReferenceArray<Double> p, int index);

		public void acVLinear(AtomicReferenceArray<Double> p, int index);
	}

	/***********************************************************************/
	/* RunMlp.java : Feedforward MLP Utilities */
	public interface IRunMlp {
		@SuppressWarnings({ "java:S107" })
		public void runMlp(final int nInps, final int nHids, final int nOuts, final int acFuncHidsCode,
				final int acFuncOutsCode, AtomicReferenceArray<Double> weights, double[] featureVectorArr,
				AtomicReferenceArray<Double> outAcs, AtomicInteger hypClass, AtomicReference<Double> confidence);

		@SuppressWarnings({ "java:S107" })
		public int runMlp2(final int nInps, final int nHids, final int nOuts, final int acFuncHidsCode,
				final int acFuncOutsCode, AtomicReferenceArray<Double> weights, double[] featureVectorArr,
				AtomicReferenceArray<Double> outAcs, AtomicInteger hypClass, AtomicReference<Double> confidence);
	}
}