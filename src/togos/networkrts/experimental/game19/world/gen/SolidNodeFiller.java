package togos.networkrts.experimental.game19.world.gen;

import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.NodeUpdater;
import togos.networkrts.experimental.game19.world.QuadTreeNode;
import togos.networkrts.experimental.game19.world.WorldNode;

public class SolidNodeFiller implements NodeUpdater
{
	final WorldNode leafNode;
	final int leafSizePower;
	public SolidNodeFiller( WorldNode n, int sizePower ) {
		assert n != null;
		assert sizePower >= 0;
		
		this.leafNode = n;
		this.leafSizePower = sizePower;
	}
	
	public SolidNodeFiller( BlockStack bs ) {
		this( bs.toLeafNode(), 0 );
	}
	
	
	transient WorldNode[] upscaled;
	protected synchronized WorldNode upscaled( int sizePower ) {
		int scalePower = sizePower - leafSizePower;
		assert scalePower >= 0;
		
		if( upscaled == null ) {
			upscaled = new WorldNode[16];
			upscaled[0] = leafNode;
		}
		// TODO: could expand array as needed here
		assert upscaled.length > scalePower;
		
		if( upscaled[scalePower] == null ) {
			for( int i=1; i<=scalePower; ++i ) {
				if( upscaled[i] == null ) {
					upscaled[i] = QuadTreeNode.createHomogeneousQuad( upscaled[i-1] );
				}
			}
		}
		
		return upscaled[scalePower];
	}
	
	@Override public WorldNode update( WorldNode oldNode, int x, int y, int sizePower ) {
		if( sizePower == leafSizePower ) {
			return leafNode;
		} else if( sizePower > leafSizePower ) {
			return upscaled(sizePower);
		} else /* if( size < leafSize ) */ {
			throw new UnsupportedOperationException("Not yet implemented; leaf size power="+leafSizePower+"; asked for node with size power="+sizePower);
		}
	}

}
