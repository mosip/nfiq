package io.mosip.nist.nfiq1;

import io.mosip.nist.nfiq1.mindtct.GetMinutiae;
import io.mosip.nist.nfiq1.mindtct.Globals;
import io.mosip.nist.nfiq1.mindtct.MinutiaHelper;
import io.mosip.nist.nfiq1.mlp.RunMlp;

public class Nfiq1 extends Nist{
	private Nfiq1Globals nfiqGlobals = new Nfiq1Globals ();
	private Nfiq1ZNormalization zNorm = new Nfiq1ZNormalization();
	
	public MinutiaHelper getMinutiaHelper() {
		return MinutiaHelper.getInstance();
	}
	public Globals getGlobals() {
		return Globals.getInstance();
	}
	public GetMinutiae getGetMinutiae() {
		return GetMinutiae.getInstance();
	}

	public Nfiq1Globals getNfiqGlobals() {
		return nfiqGlobals;
	}

	public void setNfiqGlobals(Nfiq1Globals nfiqGlobals) {
		this.nfiqGlobals = nfiqGlobals;
	}
	
	public Nfiq1ZNormalization getZNorm() {
		return zNorm;
	}

	public void setZNorm(Nfiq1ZNormalization zNorm) {
		this.zNorm = zNorm;
	}
	
	public RunMlp getRunMlp() {
		return RunMlp.getInstance();
	}
}


