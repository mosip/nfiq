package org.mosip.nist.nfiq1.common;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.Spy;

public class IDefsTest {

	@Spy
	private IDefs iDefs;

	@Before
	public void setUp() {
		// Provide full dummy implementation for all abstract methods
		iDefs = Mockito.spy(new IDefs() {

			@Override
			public double fMod(double a, double b) {
				return 0;
			}

			@Override
			public double degToRad(double deg) {
				return 0;
			}

			@Override
			public double degToRad() {
				return 0;
			}

			@Override
			public double max(double a, double b) {
				return 0;
			}

			@Override
			public double min(double a, double b) {
				return 0;
			}

			@Override
			public int sRound(double x) {
				return 0;
			}

			@Override
			public long sRoundLong(double x) {
				return 0;
			}

			@Override
			public int alignTo16(int value) {
				return 0;
			}

			@Override
			public int alignTo32(int value) {
				return 0;
			}

			@Override
			public double truncDoublePrecision(double x, double scale) {
				return 0;
			}

			@Override
			public double e(IMlp.TDACHAR tda, int i, int j) {
				return 0;
			}

			@Override
			public double e(IMlp.TDAINT tda, int i, int j) {
				return 0;
			}

			@Override
			public double e(IMlp.TDAFLOAT tda, int i, int j) {
				return 0;
			}
		});
	}

	@Test
	public void testConstructor() {
		IDefs obj = new IDefs() {
			// Same dummy implementation as above
			public double fMod(double a, double b) { return 0; }
			public double degToRad(double deg) { return 0; }
			public double degToRad() { return 0; }
			public double max(double a, double b) { return 0; }
			public double min(double a, double b) { return 0; }
			public int sRound(double x) { return 0; }
			public long sRoundLong(double x) { return 0; }
			public int alignTo16(int value) { return 0; }
			public int alignTo32(int value) { return 0; }
			public double truncDoublePrecision(double x, double scale) { return 0; }
			public double e(IMlp.TDACHAR tda, int i, int j) { return 0; }
			public double e(IMlp.TDAINT tda, int i, int j) { return 0; }
			public double e(IMlp.TDAFLOAT tda, int i, int j) { return 0; }
		};
	}

	@Test
	public void testInvokeAllMethods() throws Exception {
		// Call every method with dummy data
		try { iDefs.fMod(1, 2); } catch (Exception ignore) {}
		try { iDefs.degToRad(10); } catch (Exception ignore) {}
		try { iDefs.degToRad(); } catch (Exception ignore) {}
		try { iDefs.max(1, 2); } catch (Exception ignore) {}
		try { iDefs.min(1, 2); } catch (Exception ignore) {}
		try { iDefs.sRound(5.5); } catch (Exception ignore) {}
		try { iDefs.sRoundLong(7.8); } catch (Exception ignore) {}
		try { iDefs.alignTo16(3); } catch (Exception ignore) {}
		try { iDefs.alignTo32(9); } catch (Exception ignore) {}
		try { iDefs.truncDoublePrecision(3.14, 1); } catch (Exception ignore) {}

		try { iDefs.e((IMlp.TDACHAR) null, 0, 0); } catch (Exception ignore) {}
		try { iDefs.e((IMlp.TDAINT) null, 0, 0); } catch (Exception ignore) {}
		try { iDefs.e((IMlp.TDAFLOAT) null, 0, 0); } catch (Exception ignore) {}
	}
}