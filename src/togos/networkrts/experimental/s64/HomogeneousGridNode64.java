package togos.networkrts.experimental.s64;

public class HomogeneousGridNode64 extends GridNode64
{
	public HomogeneousGridNode64( Block[] stack ) {
		for( int i=0; i<64; ++i ) subNodes[i] = this;
		for( int i=0; i<64; ++i ) blockStacks[i] = stack;
	}
	
	public boolean isHomogeneous() {
		return true;
	}
}
