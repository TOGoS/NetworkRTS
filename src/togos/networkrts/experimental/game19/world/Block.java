package togos.networkrts.experimental.game19.world;

import java.io.IOException;
import java.io.OutputStream;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.InvalidEncoding;
import togos.networkrts.cereal.NumberEncoding;
import togos.networkrts.cereal.StandardValueOps;
import togos.networkrts.experimental.game19.io.CerealWorldIO;
import togos.networkrts.experimental.game19.io.WorldObjectCCCodec;
import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.world.beh.BoringBlockInternals;
import togos.networkrts.util.BitAddressRange;
import togos.networkrts.util.BitAddressUtil;
import togos.networkrts.util.ResourceNotFound;

public class Block implements BitAddressRange, HasAutoUpdateTime
{
	public static final WorldObjectCCCodec<Block> CCC1 = new WorldObjectCCCodec<Block>() {
		@Override public Class<Block> getEncodableClass() { return Block.class; }
		
		static final int PROP_BIT_ADDRESS = 0x01;
		static final int PROP_FLAGS       = 0x02;
		static final int PROP_ICON        = 0x04;
		static final int PROP_INTERNALS   = 0x08;
		static final int KNOWN_PROP_MASK  = 0x0F;
		
		@Override public void encode(
			Block block, byte[] constructorPrefix, OutputStream os, CerealWorldIO cwio
		) throws IOException {
			int propsWritten = 0;
			if( block.bitAddress != 0 ) {
				StandardValueOps.writeNumberCompact(block.bitAddress, os);
				propsWritten |= PROP_BIT_ADDRESS;
			}
			if( block.flags != 0 ) {
				StandardValueOps.writeNumberCompact(block.flags, os);
				propsWritten |= PROP_FLAGS;
			}
			if( block.icon != null) {
				cwio.writeObjectReference(block.icon, os);
				propsWritten |= PROP_ICON;
			}
			if( block.internals != BoringBlockInternals.INSTANCE ) {
				cwio.writeObjectReference(block.internals, os);
				propsWritten |= PROP_INTERNALS;
			}
			os.write(constructorPrefix);
			NumberEncoding.writeUnsignedBase128(propsWritten, os);
		}

		@Override public int decode(
			byte[] data, int offset, DecodeState ds, CerealDecoder context
		) throws InvalidEncoding, ResourceNotFound {
			long propDecodeResult = NumberEncoding.readUnsignedBase128(data, offset); 
			long props = NumberEncoding.base128Value(propDecodeResult); 
			offset += NumberEncoding.base128Skip(propDecodeResult);
			
			if( (props | KNOWN_PROP_MASK) != KNOWN_PROP_MASK ) {
				throw new InvalidEncoding(String.format("Unknown block properties encoded: %x", props));
			}
			
			long bitAddress = 0;
			long flags = 0;
			Icon icon = null;
			BlockInternals internals = BoringBlockInternals.INSTANCE;
			
			if( (props & PROP_INTERNALS) != 0 ) {
				internals = context.removeStackItem(ds, -1, BlockInternals.class);
			}
			if( (props & PROP_ICON) != 0 ) {
				icon = context.removeStackItem(ds, -1, Icon.class);
			}
			if( (props & PROP_FLAGS) != 0 ) {
				flags = context.removeStackItem(ds, -1, Number.class).longValue();
			}
			if( (props & PROP_BIT_ADDRESS) != 0 ) {
				bitAddress = context.removeStackItem(ds, -1, Number.class).longValue();
			}
			
			// TODO: We should probably have the no-icon-specified case be Icon.INVISIBLE or something
			if( icon == null ) throw new InvalidEncoding("Block encoded with no icon");
			
			ds.pushStackItem(new Block(bitAddress, flags, icon, internals));
			
			return offset;
		}
	};

	
	public static final long FLAG_SOLID  = 0x0000000000000001l; 
	public static final long FLAG_OPAQUE = 0x0000000000000002l;
	// TODO: Replace spikey with some more flexible mechanism
	public static final long FLAG_SPIKEY = 0x0000000000000004l;
	
	public final BlockStackRSTNode stack;
		
	public final long bitAddress;
	/** Additional flags that are not part of the bit address */
	public final long flags;
	public final Icon icon;
	public final BlockInternals internals;
	
	public Block( long bitAddress, long flags, Icon icon, BlockInternals behavior ) {
		this.bitAddress = BitAddresses.forceType( BitAddresses.TYPE_BLOCK, bitAddress );
		this.flags = flags;
		this.icon = icon;
		this.internals = behavior;
		this.stack = BlockStackRSTNode.create( this );
	}
	
	public Block( Icon icon ) {
		this( 0, 0, icon, BoringBlockInternals.INSTANCE );
	}
	
	public Block withBehavior( BlockInternals beh ) {
		return new Block( bitAddress, flags, icon, beh );
	}
	
	@Override public long getMinBitAddress() {
		return BitAddressUtil.minAddressAI( internals.getMinBitAddress(), bitAddress );
	}
	
	@Override public long getMaxBitAddress() {
		return BitAddressUtil.maxAddressAI( internals.getMaxBitAddress(), bitAddress );
	}
	
	@Override public long getNextAutoUpdateTime() {
		return internals.getNextAutoUpdateTime();
	}
}
