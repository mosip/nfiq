package io.mosip.nist.nfiq1.mindtct;

import java.util.concurrent.atomic.AtomicReferenceArray;

import io.mosip.nist.nfiq1.common.ILfs.DftWave;
import io.mosip.nist.nfiq1.common.ILfs.DftWaves;
import io.mosip.nist.nfiq1.common.ILfs.DirToRad;
import io.mosip.nist.nfiq1.common.ILfs.IFree;
import io.mosip.nist.nfiq1.common.ILfs.RotGrids;

public class Free extends MindTct implements IFree {
	private static volatile Free instance;
    public static Free getInstance() {
        if (instance == null) {
            synchronized (Free.class) {
                if (instance == null) {
                    instance = new Free();
                }
            }
        }
        return instance;
    }    
    private Free()
    {
    	super();
    }
    
	/*************************************************************************
	**************************************************************************
	#cat: free_dir2rad - Deallocates memory associated with a DIR2RAD structure
	   Input:
	      Object - pointer to memory to be freed
	*************************************************************************/
	public void free (Object object) {
		object = null;
	}

	/*************************************************************************
	**************************************************************************
	#cat: freeDirToRad - Deallocates memory associated with a DIR2RAD structure
	   Input:
	      dir2rad - pointer to memory to be freed
	*************************************************************************/
	public void freeDirToRad(DirToRad dir2Rad) {
		if (dir2Rad != null)
		{
			dir2Rad.setCos(null);
			dir2Rad.setSin(null);
		}
		free (dir2Rad);
	}

	/*************************************************************************
	**************************************************************************
	#cat: freeDftWaves - Deallocates the memory associated with a DFTWAVES
	#cat:                 structure
	   Input:
	      dftwaves - pointer to memory to be freed
	**************************************************************************/
	public void freeDftWaves(DftWaves dftWaves) {
		int i;

		if (dftWaves != null)
		{
			for(i = 0; i < dftWaves.getNWaves(); i++){
				DftWave[] dftWavesArr = dftWaves.getWaves();
				if (dftWavesArr != null)
				{
					free(dftWavesArr[i].getCos());
					free(dftWavesArr[i].getSin());
					free(dftWavesArr[i]);
				}
			}
			dftWaves.setWaves (null);
		}
		free (dftWaves);
	}

	/*************************************************************************
	**************************************************************************
	#cat: freeRotGrids - Deallocates the memory associated with a ROTGRIDS
	#cat:                 structure
	   Input:
	      rotgrids - pointer to memory to be freed
	**************************************************************************/
	public void freeRotGrids(RotGrids rotGrids) {
		int i;

		if (rotGrids != null)
		{
			for(i = 0; i < rotGrids.getNoOfGrids() ; i++)
				rotGrids.getGrids()[i] = null;

			rotGrids.setGrids(null);
		}
		free (rotGrids);
	}
	
	/*************************************************************************
	**************************************************************************
	#cat: freeDirPowers - Deallocate memory associated with DFT power vectors
	   Input:
	      powers - vectors of DFT power values (N Waves X M Directions)
	      nwaves - number of DFT wave forms used
	**************************************************************************/
	public void freeDirPowers(AtomicReferenceArray<Double[]> powers, final int nwaves) {
		int w;

		for(w = 0; w < nwaves; w++)
		{
			if (powers != null)
				powers.set(w, null);
		}
		free (powers);
	}
}


