package togos.networkrts.experimental.game19.world;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.InvalidEncoding;
import togos.networkrts.cereal.NumberEncoding;
import togos.networkrts.experimental.game19.io.CerealWorldIO;
import togos.networkrts.experimental.game19.io.WorldObjectCCCodec;
import togos.networkrts.experimental.game19.sim.UpdateContext;
import togos.networkrts.util.ResourceNotFound;

public class BlockStackRSTNode extends BaseRSTNode
{
	public static final WorldObjectCCCodec<BlockStackRSTNode> CCC = new WorldObjectCCCodec<BlockStackRSTNode>() {
		@Override public Class<BlockStackRSTNode> getEncodableClass() { return BlockStackRSTNode.class; }

		@Override public void encode(
			BlockStackRSTNode bs, byte[] constructorPrefix, OutputStream os, CerealWorldIO cwio
		) throws IOException {
			if( bs.blocks.length == 1 ) {
				// Then that block can represent the stack
				cwio.writeObjectInline(bs.blocks[0], os);
			} else {
				for( Block b : bs.blocks ) {
					cwio.writeObjectReference(b, os);
				}
				os.write(constructorPrefix);
				NumberEncoding.writeUnsignedBase128(bs.blocks.length, os);
			}
		}

		@Override public int decode(
			byte[] data, int offset, DecodeState ds, CerealDecoder context
		) throws InvalidEncoding, ResourceNotFound {
			long blockCountDecodeResult = NumberEncoding.readUnsignedBase128(data, offset); 
			long blockCount = NumberEncoding.base128Value(blockCountDecodeResult);
			offset += NumberEncoding.base128Skip(blockCountDecodeResult);
			
			if( blockCount < 0 ) throw new InvalidEncoding("Block stack has < 0 blocks (base128 decoder bug!)");
			if( blockCount > 1024 ) throw new InvalidEncoding("Block stack has way too many blocks");
			Block[] blocks = new Block[(int)blockCount];
			for( int j=blocks.length-1; j>=0; --j ) {
				blocks[j] = context.removeStackItem(ds, -1, Block.class);
			}
			ds.pushStackItem(BlockStackRSTNode.create(blocks));
			
			return offset;
		}
	};
	
	protected static final Comparator<Block> BLOCK_ICON_Z_COMPARATOR = new Comparator<Block>() {
		@Override public int compare(Block a, Block b) {
			float za = a.icon.imageZ, zb = b.icon.imageZ;
			return za < zb ? -1 : za > zb ? 1 : 0;
		}
	};
	
	public static final BlockStackRSTNode EMPTY = BlockStackRSTNode.create( new Block[0] );
	
	protected final Block[] blocks;
	
	private BlockStackRSTNode( Block[] blocks, long minId, long maxId, long nextAutoUpdateTime ) {
		super( minId, maxId, nextAutoUpdateTime );
		this.blocks = blocks;
	}
	
	public static BlockStackRSTNode create( Block[] blocks ) {
		if( blocks.length == 0 && EMPTY != null ) return EMPTY;
		if( blocks.length == 1 && blocks[0].stack != null ) return blocks[0].stack;
		
		long aut = Long.MAX_VALUE;
		long minAddress = BitAddresses.TYPE_NODE;
		long maxAddress = BitAddresses.TYPE_NODE;
		for( Block b : blocks ) {
			long baut = b.getNextAutoUpdateTime();
			if( baut < aut ) aut = baut;
			maxAddress |= b.getMaxBitAddress();
			minAddress &= b.getMinBitAddress();
		}
		
		Arrays.sort(blocks, BLOCK_ICON_Z_COMPARATOR);
		
		return new BlockStackRSTNode( blocks, minAddress, maxAddress, aut );
	}
	
	public static BlockStackRSTNode create( BlockStack s ) {
		if( s instanceof BlockStackRSTNode ) {
			return (BlockStackRSTNode)s;
		} else {
			return create( s.getBlocks() );
		}
	}
	
	public static BlockStackRSTNode create( Block block ) {
		if( block.stack != null ) return block.stack;
		return create( new Block[]{ block } );
	}
	
	@Override public NodeType getNodeType() { return NodeType.BLOCKSTACK; }
	@Override public Block[] getBlocks() { return blocks; }
	@Override public RSTNode[] getSubNodes() { return RSTNode.EMPTY_LIST; }
	
	protected static RSTNode updateBlocks( BlockStackRSTNode a, int x, int y, int sizePower, long time, MessageSet messages, UpdateContext updateContext ) {
		Block[] newBlocks = new Block[a.blocks.length];
		boolean anyBlocksUpdated = false;
		for( int i=0; i<a.blocks.length; ++i ) {
			newBlocks[i] = a.blocks[i].internals.update( a.blocks[i], x, y, sizePower, time, messages, updateContext );
			if( newBlocks[i] != a.blocks[i] ) anyBlocksUpdated = true;
		}
		return anyBlocksUpdated ? BlockStackRSTNode.create(newBlocks) : a;
	}
	
	protected static BlockStackRSTNode updateStack( BlockStackRSTNode a, int x, int y, int sizePower, long time, MessageSet messages, UpdateContext updateContext ) {
		final int size = 1<<sizePower;
		final MessageSet nodeMessages = messages.subsetApplicableTo(x, y, x+size, y+size, BitAddresses.TYPE_NODE, BitAddresses.maxForType(BitAddresses.TYPE_NODE));
		for( Message m : nodeMessages ) {
			switch( m.type ) {
			case ADD_BLOCKS:
				a = withBlocks(a, (BlockStack)m.payload);
				break;
			case REPLACE_BLOCKS:
				a = BlockStackRSTNode.create( (BlockStack)m.payload );
				break;
			default:
				// Nothin
			}
		}
		return a;
	}
	
	@Override protected RSTNode _update( int x, int y, int sizePower, long time, MessageSet messages, UpdateContext updateContext ) {
		BlockStackRSTNode a = updateStack( this, x, y, sizePower, time, messages, updateContext );
		return updateBlocks( a, x, y, sizePower, time, messages, updateContext );
	}
	
	public static RSTNode withoutBlock(RSTNode n, Block toBeRemoved) {
		int matches = 0;
		Block[] blocks = n.getBlocks();
		
		for( int i=0; i<blocks.length; ++i ) if( blocks[i].equals(toBeRemoved) ) ++matches;
		
		if( matches == 0 ) return n;
		if( matches == blocks.length ) return EMPTY;
		
		Block[] newBlocks = new Block[blocks.length-matches];
		for( int i=0, j=0; i<blocks.length; ++i ) {
			if( !blocks[i].equals(toBeRemoved) ) {
				newBlocks[j++] = blocks[i];
			}
		}
		
		return create(newBlocks);
	}
	
	public static BlockStackRSTNode withBlocks(RSTNode n, BlockStack toBeAdded) {
		Block[] blocks = n.getBlocks();
		Block[] addBlocks = toBeAdded.getBlocks();
		if( blocks.length == 0 ) return create(toBeAdded);
		
		Block[] newBlocks = new Block[blocks.length+addBlocks.length];
		for( int i=0; i<blocks.length; ++i ) {
			newBlocks[i] = blocks[i];
		}
		for( int i=blocks.length, j=0; j<addBlocks.length; ++i, ++j ) {
			newBlocks[i] = addBlocks[j];
		}
		
		return create(newBlocks);
	}
	
	public static BlockStackRSTNode withBlock(RSTNode n, Block toBeAdded) {
		Block[] blocks = n.getBlocks();
		if( blocks.length == 0 ) return toBeAdded.stack;
		
		Block[] newBlocks = new Block[blocks.length+1];
		for( int i=0; i<blocks.length; ++i ) {
			newBlocks[i] = blocks[i];
		}
		newBlocks[blocks.length] = toBeAdded; 
		
		return create(newBlocks);
	}
}
