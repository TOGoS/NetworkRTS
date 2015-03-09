package togos.networkrts.experimental.game19.world.gen;

import togos.networkrts.experimental.game19.world.QuadRSTNode;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.RSTNodeUpdater;

public class SolidNodeFiller implements RSTNodeUpdater
{
	final RSTNode leafNode;
	final int leafSizePower;
	
	public SolidNodeFiller( RSTNode n, int sizePower ) {
		assert n != null;
		assert sizePower >= 0;
		
		this.leafNode = n;
		this.leafSizePower = sizePower;
	}
	
	public SolidNodeFiller( RSTNode n ) {
		this( n, 0 );
	}
	
	transient RSTNode[] upscaled;
	protected synchronized RSTNode upscaled( int sizePower ) {
		int scalePower = sizePower - leafSizePower;
		assert scalePower >= 0;
		
		if( upscaled == null ) {
			upscaled = new RSTNode[16];
			upscaled[0] = leafNode;
		}
		// TODO: could expand array as needed here
		assert upscaled.length > scalePower;
		
		if( upscaled[scalePower] == null ) {
			for( int i=1; i<=scalePower; ++i ) {
				if( upscaled[i] == null ) {
					upscaled[i] = QuadRSTNode.createHomogeneousQuad( upscaled[i-1] );
				}
			}
		}
		
		return upscaled[scalePower];
	}
	
	@Override public RSTNode update( RSTNode oldNode, int x, int y, int sizePower ) {
		if( sizePower == leafSizePower ) {
			return leafNode;
		} else if( sizePower > leafSizePower ) {
			return upscaled(sizePower);
		} else /* if( size < leafSize ) */ {
			throw new UnsupportedOperationException("Not yet implemented; leaf size power="+leafSizePower+"; asked for node with size power="+sizePower);
		}
	}

}
