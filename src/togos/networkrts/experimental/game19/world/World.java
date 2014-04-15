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
import togos.networkrts.experimental.game19.scene.Layer.LayerLink;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityAggregation;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.util.BitAddressUtil;
import togos.networkrts.util.ResourceNotFound;

public class World implements EntityAggregation
{
	public static final WorldObjectCCCodec<World> CCC1 = new WorldObjectCCCodec<World>() {
		@Override public Class<World> getEncodableClass() { return World.class; }
		
		static final int PROP_RST_SIZE_POWER = 0x01;
		static final int PROP_RST            = 0x02;
		static final int KNOWN_PROP_MASK     = 0x03;
		
		@Override public void encode(
			World world, byte[] constructorPrefix, OutputStream os, CerealWorldIO cwio
		) throws IOException {
			cwio.writeObjectReference(world.rst, os);
			StandardValueOps.writeNumberCompact(world.rstSizePower, os);
			os.write(constructorPrefix);
			NumberEncoding.writeInt32(PROP_RST_SIZE_POWER|PROP_RST, os);
		}

		@Override
		public int decode(
			byte[] data, int offset, DecodeState ds, CerealDecoder context
		) throws InvalidEncoding, ResourceNotFound {
			int props = NumberEncoding.readInt32(data, offset);
			offset += 4;
			if( (props | KNOWN_PROP_MASK) != KNOWN_PROP_MASK ) {
				throw new InvalidEncoding(String.format("Unknown world properties encoded: %x", props));
			}
			
			long sizePower = -1234;
			RSTNode rst = null;
			EntitySpatialTreeIndex<NonTile> nonTiles = new EntitySpatialTreeIndex<NonTile>();
			LayerLink backgroundLink = null;
			
			if( (props & PROP_RST_SIZE_POWER) != 0 ) {
				sizePower = context.removeStackItem(ds, -1, Number.class).longValue();
			}
			if( (props & PROP_RST) != 0 ) {
				rst = context.removeStackItem(ds, -1, RSTNode.class);
			}
			
			if( sizePower == -1234 ) throw new InvalidEncoding("World encoded without an RST size power");
			if( sizePower < 0 ) throw new InvalidEncoding("World encoded with negative RST size power: "+sizePower);
			if( sizePower > 30 ) throw new InvalidEncoding("World encoded with overly large RST size power: "+sizePower);
			if( rst == null ) throw new InvalidEncoding("World encoded with no RST");
			
			ds.pushStackItem(new World(rst, (int)sizePower, nonTiles, backgroundLink));
			
			return offset;
		}
	};
	
	public final RSTNode rst;
	public final int rstSizePower;
	public final EntitySpatialTreeIndex<NonTile> nonTiles;
	// May need a separate index for 'watchers'
	public final LayerLink background;
	
	public World(RSTNode rst, int rstSizePower, EntitySpatialTreeIndex<NonTile> nonTiles, LayerLink background ) {
		this.rst = rst;
		this.rstSizePower = rstSizePower;
		this.nonTiles = nonTiles.freeze();
		this.background = background;
	}
	
	public World(RSTNode rst, int rstSizePower, EntitySpatialTreeIndex<NonTile> nonTiles ) {
		this( rst, rstSizePower, nonTiles, null );
	}
	
	public RSTNodeInstance getRstNodeInstance() {
		return new RSTNodeInstance() {
			@Override public RSTNode getNode() { return rst; }
			@Override public int getNodeX() { return -(1<<(rstSizePower-1)); }
			@Override public int getNodeY() { return -(1<<(rstSizePower-1)); }
			@Override public int getNodeSizePower() { return rstSizePower; }
		};
	}
	
	public long getNextAutoUpdateTime() {
		return Math.min( rst.getNextAutoUpdateTime(), nonTiles.getNextAutoUpdateTime() );
	}
	
	//// Some convenience method
	
	public World withNonTile(NonTile nt) {
		return new World( rst, rstSizePower, nonTiles.with(nt), background );
	}
	
	// Could limit this to union of RST and node trees.
	@Override public AABB getAabb() { return AABB.BOUNDLESS; }
	
	@Override public long getMinBitAddress() {
		return BitAddressUtil.minAddress(rst.getMinBitAddress(), nonTiles.getMinBitAddress());
	}
	
	@Override public long getMaxBitAddress() {
		return BitAddressUtil.maxAddress(rst.getMaxBitAddress(), nonTiles.getMaxBitAddress());
	}
}
