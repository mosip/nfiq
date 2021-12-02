package io.mosip.nist.nfiq1.common;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public interface IMlp {
	/* For use by strm_fmt() and lgl_tbl(), which format the warning and
	error messages that may be written as the result of scanning a
	specfile.  Columns are numbered starting at 0. */
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
	public static final int MSE     = 0;
	public static final int TYPE_1  = 1;
	public static final int POS_SUM = 2;

	/* For purpose: */
	public static final int CLASSIFIER = 0;
	public static final int FITTER     = 1;

	/* For boltzmann: */
	public static final int NO_PRUNE     = 0;
	public static final int ABS_PRUNE    = 2;
	public static final int SQUARE_PRUNE = 3;

	/* For train_or_test: */
	public static final int TRAIN = 0;
	public static final int TEST  = 1;

	/* For acfunc_hids and acfunc_outs: */
	public static final int SINUSOID    = 0;
	public static final int SIGMOID     = 1;
	public static final int LINEAR      = 2;
	public static final int BAD_AC_CODE = 127;

	/* For priors: */
	public static final int ALLSAME = 0;
	public static final int CLASS   = 1;
	public static final int PATTERN = 2;
	public static final int BOTH    = 3;

	/* For patsfile_ascii_or_binary: */
	public static final int ASCII  = 0;
	public static final int BINARY = 1;
	
	public static final int MAX_NHIDS = 1000; // Maximum number of hidden nodes
	public static final int TREEPATSFILE = 5151;
	public static final int JUSTPATSFILE = 0;
	public static final int FMT_ITEMS = 8;
	
	public class NVEOL
	{
	  public String namestr;
	  public String valstr;
	  public String errstr;
	  public char ok;
	  public int linenum;
	}
	
	public class TDACHAR
	{
	  public int dim2;
	  public String buf;
	}

	public class TDAINT
	{
	  public int dim2;
	  public AtomicInteger buf;
	}

	public class TDAFLOAT
	{
	  public int dim2;
	  public AtomicReference<Float> buf;
	}

	public class SSL
	{
	  public char set_tried;
	  public char set;
	  public int linenum;
	}

	public class PARM_FILENAME
	{
	  public String val = new String(new char[IMlp.PARM_FILENAME_VAL_DIM]);
	  public SSL ssl = new SSL();
	}

	public class PARM_INT
	{
	  public int val;
	  public SSL ssl = new SSL();
	}

	public class PARM_FLOAT
	{
	  public float val;
	  public SSL ssl = new SSL();
	}

	public class PARM_SWITCH
	{
	  public char val;
	  public SSL ssl = new SSL();
	}

	public class PARMS
	{
	  public PARM_FILENAME long_outfile = new PARM_FILENAME();
	  public PARM_FILENAME short_outfile = new PARM_FILENAME();
	  public PARM_FILENAME patterns_infile = new PARM_FILENAME();
	  public PARM_FILENAME wts_infile = new PARM_FILENAME();
	  public PARM_FILENAME wts_outfile = new PARM_FILENAME();
	  public PARM_FILENAME class_wts_infile = new PARM_FILENAME();
	  public PARM_FILENAME pattern_wts_infile = new PARM_FILENAME();
	  public PARM_FILENAME lcn_scn_infile = new PARM_FILENAME();
	  public PARM_INT npats = new PARM_INT();
	  public PARM_INT ninps = new PARM_INT();
	  public PARM_INT nhids = new PARM_INT();
	  public PARM_INT nouts = new PARM_INT();
	  public PARM_INT seed = new PARM_INT();
	  public PARM_INT niter_max = new PARM_INT();
	  public PARM_INT nfreq = new PARM_INT();
	  public PARM_INT nokdel = new PARM_INT();
	  public PARM_INT lbfgs_mem = new PARM_INT();
	  public PARM_FLOAT regfac = new PARM_FLOAT();
	  public PARM_FLOAT alpha = new PARM_FLOAT();
	  public PARM_FLOAT temperature = new PARM_FLOAT();
	  public PARM_FLOAT egoal = new PARM_FLOAT();
	  public PARM_FLOAT gwgoal = new PARM_FLOAT();
	  public PARM_FLOAT errdel = new PARM_FLOAT();
	  public PARM_FLOAT oklvl = new PARM_FLOAT();
	  public PARM_FLOAT trgoff = new PARM_FLOAT();
	  public PARM_FLOAT scg_earlystop_pct = new PARM_FLOAT();
	  public PARM_FLOAT lbfgs_gtol = new PARM_FLOAT();
	  public PARM_SWITCH errfunc = new PARM_SWITCH();
	  public PARM_SWITCH purpose = new PARM_SWITCH();
	  public PARM_SWITCH boltzmann = new PARM_SWITCH();
	  public PARM_SWITCH train_or_test = new PARM_SWITCH();
	  public PARM_SWITCH acfunc_hids = new PARM_SWITCH();
	  public PARM_SWITCH acfunc_outs = new PARM_SWITCH();
	  public PARM_SWITCH priors = new PARM_SWITCH();
	  public PARM_SWITCH patsfile_ascii_or_binary = new PARM_SWITCH();
	  public PARM_SWITCH do_confuse = new PARM_SWITCH();
	  public PARM_SWITCH show_acs_times_1000 = new PARM_SWITCH();
	  public PARM_SWITCH do_cvr = new PARM_SWITCH();
	}

	public class MlpParam
	{
	  public String[] class_map;
	  public int ninps;
	  public int nhids;
	  public int nouts;
	  public char acfunc_hids;
	  public char acfunc_outs;
	  public AtomicReference<Float> weights;
	  public String cls_str = new String(new char[50]);
	  public int trnsfrm_rws;
	  public int trnsfrm_cls;
	}

	/***********************************************************************/
	/* Acs.java : */
	public interface IAcs{
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
	public interface IRunMlp{
		public void runMlp(final int nInps, final int nHids, final int nOuts, 
			final int acFuncHidsCode, final int acFuncOutsCode, 
			AtomicReferenceArray<Double> weights, double [] featureVectorArr,
			AtomicReferenceArray<Double> outAcs, AtomicInteger hypClass, 
			AtomicReference<Double> confidence);
		public int runMlp2(final int nInps, final int nHids, final int nOuts, 
			final int acFuncHidsCode, final int acFuncOutsCode, 
			AtomicReferenceArray<Double> weights, double[] featureVectorArr,
			AtomicReferenceArray<Double> outAcs, AtomicInteger hypClass, 
			AtomicReference<Double> confidence);
	}
}