package togos.networkrts.tfunc;

import java.awt.Color;

import togos.networkrts.util.TMath;

public class PulsatingColorFunction implements ColorFunction {
	long prevTs;
	Color prevColor;
	
	final float baseA, baseR, baseG, baseB;
	final float varA, varR, varG, varB;
	final long interval, offset;
	
	public PulsatingColorFunction(
		float a0, float r0, float g0, float b0,
		float a1, float r1, float g1, float b1,
		long interval, long offset
	) {
		this.baseA = (a1 + a0) / 2; this.varA = (a1 - a0) / 2;
		this.baseR = (r1 + r0) / 2; this.varR = (r1 - r0) / 2;
		this.baseG = (g1 + g0) / 2; this.varG = (g1 - g0) / 2;
		this.baseB = (b1 + b0) / 2; this.varB = (b1 - b0) / 2;
		this.interval = interval;
		this.offset = offset;
	}
	
	public int getColor(long ts) {
		return getAwtColor(ts).getRGB();
	}
	
	protected static final float clamp( double x ) {
		return (float)(x < 0 ? 0 : x > 1.0 ? 1.0 : x);
	}
	
	public Color getAwtColor(final long ts) {
		if( ts != prevTs || prevColor == null ) {
			double v = TMath.periodic( ts + offset, interval );
			prevColor = new Color(
				clamp(baseR + varR * v),
				clamp(baseG + varG * v),
				clamp(baseB + varB * v),
				clamp(baseA + varA * v)
			);
		}
		return prevColor;
	}
}
