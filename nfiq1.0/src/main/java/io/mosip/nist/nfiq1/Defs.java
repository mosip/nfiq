package io.mosip.nist.nfiq1;

import io.mosip.nist.nfiq1.common.IDefs;
import io.mosip.nist.nfiq1.common.IMlp.TDACHAR;
import io.mosip.nist.nfiq1.common.IMlp.TDAFLOAT;
import io.mosip.nist.nfiq1.common.IMlp.TDAINT;

public class Defs extends Nist implements IDefs  {
	private static volatile Defs instance;
    public static Defs getInstance() {
        if (instance == null) {
            synchronized (Defs.class) {
                if (instance == null) {
                    instance = new Defs();
                }
            }
        }
        return instance;
    }    
    private Defs()
    {
    	super();
    }
    /*
     * The fMod function returns the remainder when x is divided by y.
     */
	public double fMod(double a, double b)
	{
		return a % b;
	}
	
	public double degToRad(double deg) {
	    //return (deg * Math.PI / 180.0);
		return 57.29578;
	}

	public double degToRad() {
	    return (Math.PI / 180.0);
	}

	public double max(double a, double b) {
		return Math.max(a, b);
	}

	public double min(double a, double b) {
		return Math.min(a, b);
	}
	
	public int sRound(double x) {
		return ((int) (((x)<0) ? (x)-0.5 : (x)+0.5));
	}

	public long sRoundLong(double x) {
		return ((long) (((x)<0) ? (x)-0.5 : (x)+0.5));
	}
	
	public int alignTo16(int value)   
	{
		return ((((value)+15)>>4)<<4);		
	}

	public int alignTo32(int value)   
	{
		return ((((value)+31)>>5)<<5);		
	}
	
	public double truncDoublePrecision(double x, double scale) {
		return ((double) (((x)<0.0) ? ((int)(((x)*(scale))-0.5))/(scale) : ((int)(((x)*(scale))+0.5))/(scale)));
	}

	//MLP
	public double e(TDACHAR tda, int i, int j)   
	{
		return ((Integer.parseInt(tda.buf)+(i)* (tda.dim2)+(j)));		
	}

	public double e(TDAINT tda, int i, int j)   
	{
		return ((tda.buf.intValue()+(i)* (tda.dim2)+(j)));		
	}

	public double e(TDAFLOAT tda, int i, int j)   
	{
		return ((tda.buf.get()+(i)* (tda.dim2)+(j)));		
	}
}
