package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.world.RSTNode.NodeType;
import togos.networkrts.experimental.shape.RectIntersector;
import togos.networkrts.util.BitAddressUtil;

public class RSTUtil
{
	public static RSTNode fillShape( RSTNode orig, int x, int y, int sizePower, RectIntersector shape, RSTNodeUpdater filler ) {
		int size = 1<<sizePower;
		switch( shape.rectIntersection( x, y, size, size ) ) {
		case RectIntersector.INCLUDES_NONE: return orig;
		case RectIntersector.INCLUDES_SOME:
			if( orig.getNodeType() == NodeType.QUADTREE ) {
				int subSizePower = sizePower-1;
				int subSize = 1<<subSizePower;
				RSTNode[] subNodes = orig.getSubNodes();
				return QuadRSTNode.create( new RSTNode[] {
					// TODO: Could ensure that at least one sub node has actually been changed
					fillShape( subNodes[0], x        , y        , subSizePower, shape, filler ),
					fillShape( subNodes[1], x+subSize, y        , subSizePower, shape, filler ),
					fillShape( subNodes[2], x        , y+subSize, subSizePower, shape, filler ),
					fillShape( subNodes[3], x+subSize, y+subSize, subSizePower, shape, filler ),
				});
			}
			// Otherwise fall through
		case RectIntersector.INCLUDES_ALL:
			return filler.update(orig, x, y, sizePower);
		default:
			throw new RuntimeException("Invalid rect intersection value from "+shape.getClass());
		}
	}
	
	public static boolean nodeFullyContainsRectangle( int nodeX0, int nodeY0, int nodeSizePower, int x0, int y0, int x1, int y1 ) {
		int nodeSize = 1<<nodeSizePower;
		int nodeX1 = nodeX0+nodeSize, nodeY1 = nodeY0+nodeSize;
		
		return x0 >= nodeX0 && x1 <= nodeX1 && y0 >= nodeY0 && y1 <= nodeY1;
	}
	
	public static int childNodeFullyContainingRectangle( int nodeX0, int nodeY0, int nodeSizePower, int x0, int y0, int x1, int y1 ) {
		int subSizePower = nodeSizePower-1;
		int subSize = 1<<subSizePower;
		if( nodeFullyContainsRectangle(nodeX0          , nodeY0          , subSizePower, x0, y0, x1, y1) ) return 0;
		if( nodeFullyContainsRectangle(nodeX0 + subSize, nodeY0          , subSizePower, x0, y0, x1, y1) ) return 1;
		if( nodeFullyContainsRectangle(nodeX0          , nodeY0 + subSize, subSizePower, x0, y0, x1, y1) ) return 2;
		if( nodeFullyContainsRectangle(nodeX0 + subSize, nodeY0 + subSize, subSizePower, x0, y0, x1, y1) ) return 3;
		return -1;
	}
	
	/**
	 * Finds the smallest single node containing the rectangle between x0,y0 and x1,y1,
	 * and replaces it with the given node updater.
	 * 
	 * This can be used to reduce the number of rewrites
	 * being done toward the root when several updates
	 * are being done in a small area.
	 */
	public static RSTNode updateNodeContaining(
		RSTNode node, int nodeX, int nodeY, int nodeSizePower,
		int x0, int y0, int x1, int y1,
		RSTNodeUpdater updater
	) {
		int nodeSize = 1<<nodeSizePower;
		// This is a no-op if the node and the rectangle don't overlap: 
		if( x0 >= nodeX+nodeSize || x0 < nodeX || y0 >= nodeY+nodeSize || y0 < nodeY ) return node; 
		
		switch( node.getNodeType() ) {
		case BLOCKSTACK:
			return updater.update(node, nodeX, nodeY, nodeSizePower);
		default:
			throw new RuntimeException("Don't know how to updateNodeContaining "+node.getNodeType()+" node");
		case QUADTREE:
		}
		
		int cIdx = childNodeFullyContainingRectangle(nodeX, nodeY, nodeSizePower, x0, y0, x1, y1);
		if( cIdx == -1 ) return updater.update(node, nodeX, nodeY, nodeSizePower);
		
		int subSizePower = nodeSizePower-1;
		int subSize = 1<<subSizePower;
		
		int subX, subY;
		switch( cIdx ) {
		case 0: subX = nodeX          ; subY = nodeY          ; break;
		case 1: subX = nodeX + subSize; subY = nodeY          ; break;
		case 2: subX = nodeX          ; subY = nodeY + subSize; break;
		case 3: subX = nodeX + subSize; subY = nodeY + subSize; break;
		default: throw new RuntimeException("Unpossible!");
		}
		
		RSTNode[] subNodes = node.getSubNodes();
		RSTNode subNode = subNodes[cIdx];
		RSTNode newSubNode = updateNodeContaining( subNode, subX, subY, subSizePower, x0, y0, x1, y1, updater );
		if( newSubNode == subNode ) return node;
		
		RSTNode[] newSubNodes = new RSTNode[4];
		for( int i=0; i<4; ++i ) {
			newSubNodes[i] = i == cIdx ? newSubNode : subNodes[i];
		}
		return QuadRSTNode.create( newSubNodes );
	}
	
	public static RSTNode updateNodeContaining(
		RSTNodeInstance n,
		int x0, int y0, int x1, int y1,
		RSTNodeUpdater updater
	) {
		return updateNodeContaining( n.getNode(), n.getNodeX(), n.getNodeY(), n.getNodeSizePower(), x0, y0, x1, y1, updater );
	}
	
	public static BlockStack getBlockStackAt( RSTNode node, int nodeX, int nodeY, int nodeSizePower, int x, int y ) {
		int nodeSize = 1<<nodeSizePower;
		if( x < nodeX || y < nodeY || x >= nodeX+nodeSize || y >= nodeY+nodeSize ) return null;
		
		switch( node.getNodeType() ) {
		case BLOCKSTACK:
			return node;
		default:
			throw new RuntimeException("Don't know how to getBlockStackAt from "+node.getNodeType()+" node");
		case QUADTREE:
		}
		
		RSTNode[] subNodes = node.getSubNodes();
		int subSizePower = nodeSizePower-1;
		int subSize = 1<<subSizePower;
		
		BlockStack bs;
		if( (bs = getBlockStackAt(subNodes[0], nodeX        , nodeY        , subSizePower, x, y)) != null ) return bs;
		if( (bs = getBlockStackAt(subNodes[1], nodeX+subSize, nodeY        , subSizePower, x, y)) != null ) return bs;
		if( (bs = getBlockStackAt(subNodes[2], nodeX        , nodeY+subSize, subSizePower, x, y)) != null ) return bs;
		if( (bs = getBlockStackAt(subNodes[3], nodeX+subSize, nodeY+subSize, subSizePower, x, y)) != null ) return bs;
		
		return null;
	}
	
	public static RSTNode updateBlockStackAt( RSTNode node, int nodeX, int nodeY, int nodeSizePower, int x, int y, Block toBeAdded, Block toBeRemoved ) {
		int nodeSize = 1<<nodeSizePower;
		if( x < nodeX || y < nodeY || x >= nodeX+nodeSize || y >= nodeY+nodeSize ) return node;
		
		switch( node.getNodeType() ) {
		case BLOCKSTACK:
			RSTNode blockStack = node;
			RSTNode newBlockStack = node;
			
			if( toBeRemoved != null ) newBlockStack = BlockStackRSTNode.withoutBlock(newBlockStack, toBeRemoved);
			if( toBeAdded   != null ) newBlockStack = BlockStackRSTNode.withBlock(newBlockStack, toBeAdded);
			
			if( blockStack == newBlockStack ) return node;
			return newBlockStack;
		default:
			throw new RuntimeException("Don't know how to getBlockStackAt from "+node.getNodeType()+" node");
		case QUADTREE:
		}
		
		RSTNode[] subNodes = node.getSubNodes();
		int subSizePower = nodeSizePower-1;
		int subSize = 1<<subSizePower;
		RSTNode[] newSubNodes = new RSTNode[4];
		newSubNodes[0] = updateBlockStackAt(subNodes[0], nodeX        , nodeY        , subSizePower, x, y, toBeAdded, toBeRemoved );
		newSubNodes[1] = updateBlockStackAt(subNodes[1], nodeX+subSize, nodeY        , subSizePower, x, y, toBeAdded, toBeRemoved );
		newSubNodes[2] = updateBlockStackAt(subNodes[2], nodeX        , nodeY+subSize, subSizePower, x, y, toBeAdded, toBeRemoved );
		newSubNodes[3] = updateBlockStackAt(subNodes[3], nodeX+subSize, nodeY+subSize, subSizePower, x, y, toBeAdded, toBeRemoved );
		return QuadRSTNode.createBasedOn( newSubNodes, node );
	}
	
	public static RSTNode updateBlockStackAt( RSTNodeInstance n, int x, int y, Block toBeAdded, Block toBeRemoved ) {
		return updateBlockStackAt( n.getNode(), n.getNodeX(), n.getNodeY(), n.getNodeSizePower(), x, y, toBeAdded, toBeRemoved );
	}
	
	public static BlockInstance findBlock( RSTNode n, int x, int y, int sizePower, long minBa, long maxBa ) {
		if( !BitAddressUtil.rangesIntersect(n, minBa, maxBa) ) return null;
		
		switch( n.getNodeType() ) {
		case BLOCKSTACK:
			Block[] blocks = n.getBlocks();
			for( int i=0; i<blocks.length; ++i ) {
				Block b = blocks[i];
				if( BitAddressUtil.rangeContains(minBa, maxBa, b.bitAddress) ) {
					return new BlockInstance(x,y,i,b);
				}
			}
			break;
		case QUADTREE:
			BlockInstance p;
			int subSizePower = sizePower-1;
			int subSize = 1<<subSizePower;
			RSTNode[] subNodes = n.getSubNodes();
			if( (p = findBlock(subNodes[0], x        , y        , subSizePower, minBa, maxBa)) != null ) return p;
			if( (p = findBlock(subNodes[1], x+subSize, y        , subSizePower, minBa, maxBa)) != null ) return p;
			if( (p = findBlock(subNodes[2], x        , y+subSize, subSizePower, minBa, maxBa)) != null ) return p;
			if( (p = findBlock(subNodes[3], x+subSize, y+subSize, subSizePower, minBa, maxBa)) != null ) return p;
			break;
		default:
			throw new RuntimeException("Don't know how to findBlock within "+n.getNodeType()+" node");
		}
		return null;
	}
	
	public static BlockInstance findBlock( RSTNode n, int x, int y, int sizePower, int id ) {
		return findBlock( n, x, y, sizePower, BitAddresses.withMinFlags(id), BitAddresses.withMaxFlags(id) );
	}
	
	public static BlockInstance findBlock( RSTNodeInstance n, int id ) {
		return findBlock( n.getNode(), n.getNodeX(), n.getNodeY(), n.getNodeSizePower(), BitAddresses.withMinFlags(id), BitAddresses.withMaxFlags(id) );
	}

}
