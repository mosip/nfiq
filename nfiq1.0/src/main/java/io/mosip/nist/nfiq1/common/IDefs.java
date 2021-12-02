package io.mosip.nist.nfiq1.common;

import io.mosip.nist.nfiq1.common.IMlp.TDACHAR;
import io.mosip.nist.nfiq1.common.IMlp.TDAFLOAT;
import io.mosip.nist.nfiq1.common.IMlp.TDAINT;

public interface IDefs {
	public double fMod(double a, double b);

	public double degToRad(double deg);
	public double degToRad();

	public double max(double a, double b);

	public double min(double a, double b);
	
	public int sRound(double x);

	public long sRoundLong(double x);
	
	public int alignTo16(int value);

	public int alignTo32(int value) ;
	
	public double truncDoublePrecision(double x, double scale);

	//MLP
	public double e(TDACHAR tda, int i, int j);
	public double e(TDAINT tda, int i, int j);
	public double e(TDAFLOAT tda, int i, int j);

}
