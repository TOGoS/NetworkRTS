package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.shape.RectIntersector;
import togos.networkrts.util.BitAddressUtil;

public class WorldUtil
{
	public static WorldNode createSolid( BlockStack blockStack, int sizePower ) {
		assert sizePower >= 0;
		
		if( sizePower == 0 ) return WorldLeafNode.create( blockStack );
		
		return WorldBranchNode.createHomogeneousQuad( createSolid( blockStack, sizePower-1 ) );
	}
	
	public static WorldNode fillShape( WorldNode orig, int x, int y, int sizePower, RectIntersector shape, NodeUpdater filler ) {
		int size = 1<<sizePower;
		switch( shape.rectIntersection( x, y, size, size ) ) {
		case RectIntersector.INCLUDES_NONE: return orig;
		case RectIntersector.INCLUDES_SOME:
			if( !orig.isLeaf() ) {
				int subSizePower = sizePower-1;
				int subSize = 1<<subSizePower;
				WorldNode[] subNodes = orig.getSubNodes();
				return WorldBranchNode.create( new WorldNode[] {
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
	public static WorldNode updateNodeContaining(
		WorldNode node, int nodeX, int nodeY, int nodeSizePower,
		int x0, int y0, int x1, int y1,
		NodeUpdater updater
	) {
		int nodeSize = 1<<nodeSizePower;
		// This is a no-op if the node and the rectangle don't overlap: 
		if( x0 >= nodeX+nodeSize || x0 < nodeX || y0 >= nodeY+nodeSize || y0 < nodeY ) return node; 
		
		if( node.isLeaf() ) return updater.update(node, nodeX, nodeY, nodeSizePower);
		
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
		
		WorldNode[] subNodes = node.getSubNodes();
		WorldNode subNode = subNodes[cIdx];
		WorldNode newSubNode = updateNodeContaining( subNode, subX, subY, subSizePower, x0, y0, x1, y1, updater );
		if( newSubNode == subNode ) return node;
		
		WorldNode[] newSubNodes = new WorldNode[4];
		for( int i=0; i<4; ++i ) {
			newSubNodes[i] = i == cIdx ? newSubNode : subNodes[i];
		}
		return WorldBranchNode.create( newSubNodes );
	}
	
	public static BlockStack getBlockStackAt( WorldNode node, int nodeX, int nodeY, int nodeSizePower, int x, int y ) {
		int nodeSize = 1<<nodeSizePower;
		if( x < nodeX || y < nodeY || x >= nodeX+nodeSize || y >= nodeY+nodeSize ) return null;
		if( node.isLeaf() ) return node.getBlockStack();
		
		WorldNode[] subNodes = node.getSubNodes();
		int subSizePower = nodeSizePower-1;
		int subSize = 1<<subSizePower;
		
		BlockStack bs;
		if( (bs = getBlockStackAt(subNodes[0], nodeX        , nodeY        , subSizePower, x, y)) != null ) return bs;
		if( (bs = getBlockStackAt(subNodes[1], nodeX+subSize, nodeY        , subSizePower, x, y)) != null ) return bs;
		if( (bs = getBlockStackAt(subNodes[2], nodeX        , nodeY+subSize, subSizePower, x, y)) != null ) return bs;
		if( (bs = getBlockStackAt(subNodes[3], nodeX+subSize, nodeY+subSize, subSizePower, x, y)) != null ) return bs;
		
		return null;
	}
	
	public static WorldNode updateBlockStackAt( WorldNode node, int nodeX, int nodeY, int nodeSizePower, int x, int y, Block toBeAdded, Block toBeRemoved ) {
		int nodeSize = 1<<nodeSizePower;
		if( x < nodeX || y < nodeY || x >= nodeX+nodeSize || y >= nodeY+nodeSize ) return node;
		
		if( node.isLeaf() ) {
			BlockStack blockStack = node.getBlockStack();
			BlockStack newBlockStack = blockStack;
			
			if( toBeRemoved != null ) newBlockStack = newBlockStack.without(toBeRemoved);
			if( toBeAdded   != null ) newBlockStack = newBlockStack.with(toBeAdded);
			
			if( blockStack == newBlockStack ) return node;
			return WorldLeafNode.create( newBlockStack );
		}
		
		WorldNode[] subNodes = node.getSubNodes();
		int subSizePower = nodeSizePower-1;
		int subSize = 1<<subSizePower;
		WorldNode[] newSubNodes = new WorldNode[4];
		newSubNodes[0] = updateBlockStackAt(subNodes[0], nodeX        , nodeY        , subSizePower, x, y, toBeAdded, toBeRemoved );
		newSubNodes[1] = updateBlockStackAt(subNodes[1], nodeX+subSize, nodeY        , subSizePower, x, y, toBeAdded, toBeRemoved );
		newSubNodes[2] = updateBlockStackAt(subNodes[2], nodeX        , nodeY+subSize, subSizePower, x, y, toBeAdded, toBeRemoved );
		newSubNodes[3] = updateBlockStackAt(subNodes[3], nodeX+subSize, nodeY+subSize, subSizePower, x, y, toBeAdded, toBeRemoved );
		return WorldBranchNode.createBasedOn( newSubNodes, node );
	}
	
	public static Block findBlock( BlockStack bs, long minBa, long maxBa ) {
		for( Block b : bs.blocks ) {
			if( BitAddressUtil.rangeContains(minBa, maxBa, b.bitAddress) ) {
				return b;
			}
		}
		return null;
	}
	
	public static NodePosition findBlock( WorldNode n, int x, int y, int sizePower, long minBa, long maxBa ) {
		if( !BitAddressUtil.rangesIntersect(n, minBa, maxBa) ) return null;
		
		if( n.isLeaf() ) {
			if( findBlock(n.getBlockStack(), minBa, maxBa) != null ) {
				return new NodePosition(x,y,sizePower);
			}
		} else {
			NodePosition p;
			int subSizePower = sizePower-1;
			int subSize = 1<<subSizePower;
			WorldNode[] subNodes = n.getSubNodes();
			if( (p = findBlock(subNodes[0], x        , y        , subSizePower, minBa, maxBa)) != null ) return p;
			if( (p = findBlock(subNodes[1], x+subSize, y        , subSizePower, minBa, maxBa)) != null ) return p;
			if( (p = findBlock(subNodes[2], x        , y+subSize, subSizePower, minBa, maxBa)) != null ) return p;
			if( (p = findBlock(subNodes[3], x+subSize, y+subSize, subSizePower, minBa, maxBa)) != null ) return p;
		}
		return null;
	}
	
	public static NodePosition findBlock( WorldNode n, int x, int y, int sizePower, int id ) {
		return findBlock( n, x, y, sizePower, BitAddresses.withMinFlags(id), BitAddresses.withMaxFlags(id) );
	}
}
