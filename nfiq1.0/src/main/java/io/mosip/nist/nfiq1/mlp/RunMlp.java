package io.mosip.nist.nfiq1.mlp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.nist.nfiq1.common.IMlp;
import io.mosip.nist.nfiq1.common.IMlp.IRunMlp;
import io.mosip.nist.nfiq1.mindtct.Free;

public class RunMlp extends Mlp implements IRunMlp {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunMlp.class);	
	private static volatile RunMlp instance;
    public static RunMlp getInstance() {
        if (instance == null) {
            synchronized (RunMlp.class) {
                if (instance == null) {
                    instance = new RunMlp();
                }
            }
        }
        return instance;
    }    
    private RunMlp()
    {
    	super();
    }
    
	public Acs getAcs() {
		return Acs.getInstance();
	}
	public Free getFree() {
		return Free.getInstance();
	}	
	public MlpCla getMlpCla() {
		return MlpCla.getInstance();
	}

	/*************************************************************/
	/* runmlp: Runs the Multi-Layer Perceptron (MLP) on a feature vector.
	Input args:
	  nInps, nHids, nOuts: Numbers of input, hidden, and output nodes
	    of the MLP.
	  acFuncHidsCode: Code character specifying the type of activation
	    function to be used on the hidden nodes: must be LINEAR,
	    SIGMOID, or SINUSOID (defined in parms.h).
	  acFuncOutsCode: Code character specifying the type of activation
	    function to be used on the output nodes.
	  weights: The MLP weights.
	  featvec: The feature vector that the MLP is to be run on; its first
	    nInps elts will be used.
	Output args:
	  outAcs: The output activations.  This buffer must be provided by
	    caller, allocated to (at least) nOuts floats.
	  hypClass: The hypothetical class, as an integer in the range
	    0 through nOuts - 1.
	  confidence: A floating-point value in the range 0. through 1.
	    Defined to be outAcs[hypClass], i.e. the highest
	    output-activation value.
	*/
	public void runMlp(final int nInps, final int nHids, final int nOuts, 
		final int acFuncHidsCode, final int acFuncOutsCode, 
		AtomicReferenceArray<Double> weights, double [] featureVectorArr,
		AtomicReferenceArray<Double> outAcs, AtomicInteger hypClass, 
		AtomicReference<Double> confidence) {

		AtomicReference<Character> runmlp1_t = new AtomicReference<Character>();
		runmlp1_t.set('t');
		
		AtomicInteger runmlp1_i1 = new AtomicInteger();
		runmlp1_i1.set(1);

		AtomicReference<Double> runmlp1_f1 = new AtomicReference<Double>();
		runmlp1_f1.set(1.0d);

		String str = new String(new char[100]);
		AtomicReferenceArray<Double> w1, b1, w2, b2;
		AtomicReferenceArray<Double> hidacs = new AtomicReferenceArray<Double>(IMlp.MAX_NHIDS);
		double pvalue;
		double pevalue;
		double maxac_p;
		double maxac, ac;

		if (nHids > IMlp.MAX_NHIDS)
		{
			str = String.format("nHids, %d, is > MAX_NHIDS, defined as %d in runmlp.c", nHids, IMlp.MAX_NHIDS);
			LOGGER.error("ERROR : runmlp " + str);
			System.exit(-1); ;
		}

		/* Where the weights and biases of the two layers begin in weights. */
		w1 = weights;
		b1 = new AtomicReferenceArray<Double>(w1.length() + nHids * nInps);
		w2 = new AtomicReferenceArray<Double>(b1.length() + nHids);
		b2 = new AtomicReferenceArray<Double>(w2.length() + nOuts * nHids);

		int index = 0;
		/* Start hidden activations out as first-layer biases. */
		for (index = 0; index < nHids; index++)
			hidacs.set(index, b1.get(index));

		AtomicReferenceArray<Double> featvec = new AtomicReferenceArray<Double>(featureVectorArr.length);
		for (index = 0; index < featureVectorArr.length; index++)
		{
			featvec.set(index, featureVectorArr[index]);
		}

		/* Add product of first-layer weights with feature vector. */
		getMlpCla().mlpSgemV(runmlp1_t, nInps, nHids, runmlp1_f1, w1, nInps, featvec, runmlp1_i1, runmlp1_f1, hidacs, runmlp1_i1);

		/* Finish each hidden activation by applying activation function. */
		index = 0;
		for (pevalue = (pvalue = (double)hidacs.get(index)) + nHids; pvalue < pevalue; index++)
		{
			/* Resolve the activation function codes to functions. */
			switch (acFuncHidsCode)
			{
				case IMlp.LINEAR:
					getAcs().acVLinear(hidacs, index);
					break;
				case IMlp.SIGMOID:
					getAcs().acVSigmoid(hidacs, index);
					break;
				case IMlp.SINUSOID:
					getAcs().acVSinusoid(hidacs, index);
					break;
				default:
					str = String.format("unsupported acFuncHidsCode %d.\nSupported codes are LINEAR (%d), SIGMOID (%d), and SINUSOID (%d).", (int)acFuncHidsCode, (int)IMlp.LINEAR, (int)IMlp.SIGMOID, (int)IMlp.SINUSOID);
					System.out.println("runmlp" + str);
					break;
			}
		}

		/* Same steps again for second layer. */
		for (index = 0; index < nOuts; index++)
			outAcs.set(index, b2.get(index));

		getMlpCla().mlpSgemV(runmlp1_t, nHids, nOuts, runmlp1_f1, w2, nHids, hidacs, runmlp1_i1, runmlp1_f1, outAcs, runmlp1_i1);
		index = 0;
		for (pevalue = (pvalue = outAcs.get(index)) + nOuts; pvalue < pevalue; index++)
		{
			switch (acFuncOutsCode)
			{
				case IMlp.LINEAR:
					getAcs().acVLinear(outAcs, index);
					break;
				case IMlp.SIGMOID:
					getAcs().acVSigmoid(outAcs, index);
					break;
				case IMlp.SINUSOID:
					getAcs().acVSinusoid(outAcs, index);
					break;
				default:
					str = String.format("unsupported acFuncOutsCode %d.\nSupported codes are LINEAR (%d), SIGMOID (%d), and SINUSOID (%d).", (int)acFuncOutsCode, (int)IMlp.LINEAR, (int)IMlp.SIGMOID, (int)IMlp.SINUSOID);
					System.out.println("runmlp" + str);
					break;
			}
		}

		/* Find the hypothetical class -- the class whose output node
		activated most strongly -- and the confidence -- that activation
		value. */
		index = 0;
		for (pevalue = (maxac_p = pvalue = outAcs.get(index)) + nOuts, maxac = pvalue, index++; pvalue < pevalue; index++)
		{
			if ((ac = pvalue) > maxac)
			{
				maxac = ac;
				maxac_p = pvalue;
			}
		}

		hypClass.set((int) ((int)maxac_p - outAcs.get(0)));
		confidence.set(maxac);
	}

	/*************************************************************/
	/* runmlp2: Runs the Multi-Layer Perceptron (MLP) on a feature vector.
	Input args:
	  nInps, nHids, nOuts:
	     Numbers of input, hidden, and output nodes
	     of the MLP.
	  acFuncHidsCode:
	     Code character specifying the type of activation
	     function to be used on the hidden nodes: must be LINEAR,
	     SIGMOID, or SINUSOID (defined in parms.h).
	  acFuncOutsCode:
	     Code character specifying the type of activation
	     function to be used on the output nodes.
	  weights: 
	     The MLP weights.
	  featvec:
	     The feature vector that the MLP is to be run on; its first
	     nInps elts will be used.
	Output args:
	  outAcs:
	     The output activations.  This buffer must be provided by
	     caller, allocated to (at least) nOuts floats.
	  hypClass:
	     The hypothetical class, as an integer in the range
	     0 through nOuts - 1.
	  confidence:
	     A floating-point value in the range 0. through 1.
	     Defined to be outAcs[hypClass], i.e. the highest
	     output-activation value.
	*/
	public int runMlp2(final int nInps, final int nHids, final int nOuts, 
		final int acFuncHidsCode, final int acFuncOutsCode, 
		AtomicReferenceArray<Double> weights, double[] featureVectorArr,
		AtomicReferenceArray<Double> outAcs, AtomicInteger hypClass, 
		AtomicReference<Double> confidence) 
	{		
		AtomicReference<Character> runMlp2T = new AtomicReference<Character>();
		runMlp2T.set('t');
		
		AtomicInteger runMlp2I1 = new AtomicInteger();
		runMlp2I1.set(1);

		AtomicReference<Double> runMlp2F1 = new AtomicReference<Double>();
		runMlp2F1.set(1.0d);
		
		double [] hidacsArr = new double [IMlp.MAX_NHIDS];
		double maxac = 0.0d, ac;
				
		if (nHids > IMlp.MAX_NHIDS)
		{
			LOGGER.error(String.format("ERROR : runmlp2 : nHids : %d > %d\n", nHids, IMlp.MAX_NHIDS));
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
			hidacsArr [index] = weights.get(b1Index + index);

		AtomicReferenceArray<Double> hidacs = new AtomicReferenceArray<Double>(hidacsArr.length);
		for (index = 0; index < hidacsArr.length; index++)
		{
			hidacs.set(index, hidacsArr[index]);
		}
		
		AtomicReferenceArray<Double> featvec = new AtomicReferenceArray<Double>(featureVectorArr.length);
		for (index = 0; index < featureVectorArr.length; index++)
		{
			featvec.set(index, featureVectorArr[index]);
		}

		AtomicReferenceArray<Double> w1 = new AtomicReferenceArray<Double>(weights.length() - w1Index);
		for (index = 0; index < w1.length(); index++)
		{
			w1.set(index, weights.get(w1Index + index));
		}

		/* Add product of first-layer weights with feature vector. */
		getMlpCla().mlpSgemV(runMlp2T, nInps, nHids, runMlp2F1, w1, nInps, featvec, runMlp2I1, runMlp2F1, hidacs, runMlp2I1);

		for (index = 0; index < featvec.length(); index++)
		{
			featureVectorArr [index] = featvec.get(index);
		}

		int pIndex = 0;
		int peIndex = pIndex + nHids;  
		/* Finish each hidden activation by applying activation function. */
		for (; pIndex < peIndex; pIndex++)
		{
			/* Resolve the activation function codes to functions. */
			switch (acFuncHidsCode)
			{
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
					LOGGER.error (String.format("ERROR : runmlp2 : acFuncHidsCode : %d unsupported\n",  acFuncHidsCode));
					return (-3);
			}
		}

		/* Same steps again for second layer. */
		//outAcs = new AtomicReferenceArray<Double>(nOuts); 
		AtomicReferenceArray<Double> b2 = new AtomicReferenceArray<Double>(weights.length() - b2Index);
		for (index = 0; index < b2.length(); index++)
		{
			b2.set(index, weights.get(b2Index + index));
		}

		for (index = 0; index < nOuts; index++)
		{
			outAcs.set(index, b2.get(index));
		}

		AtomicReferenceArray<Double> w2 = new AtomicReferenceArray<Double>(weights.length() - w2Index);
		for (index = 0; index < w2.length(); index++)
		{
			w2.set(index, weights.get(w2Index + index));
		}

		getMlpCla().mlpSgemV(runMlp2T, nHids, nOuts, runMlp2F1, w2, nHids, hidacs, runMlp2I1, runMlp2F1, outAcs, runMlp2I1);

		pIndex = 0;
		peIndex = pIndex + nOuts;  
		/* Finish each hidden activation by applying activation function. */
		for (; pIndex < peIndex; pIndex++)
		{
			switch (acFuncOutsCode)
			{
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
					LOGGER.error (String.format("ERROR : runmlp2 : acFuncOutsCode : %d unsupported\n", acFuncOutsCode));
					return (-4);
			}
		}
			
		/* Find the hypothetical class -- the class whose output node
		activated most strongly -- and the confidence -- that activation
		value. */

		pIndex = 0;
		int maxac_pIndex = pIndex;
		peIndex = maxac_pIndex + nOuts;  

		for (maxac = outAcs.get(pIndex), pIndex++; pIndex < peIndex; pIndex++)
		{
			if ((ac = outAcs.get(pIndex)) > maxac)
			{
				maxac = ac;
				maxac_pIndex++;
			}
		}

		hypClass.set((int) (maxac_pIndex - 0));
		confidence.set(maxac);

		//LOGGER.info(String.format("runmlp2 final(%2d, %2f)\n", hypClass.get(), confidence.get()));

		return 0;
	}
}


