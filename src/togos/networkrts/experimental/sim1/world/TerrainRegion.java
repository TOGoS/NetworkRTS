package togos.networkrts.experimental.sim1.world;

import org.bitpedia.util.Base32;

import togos.networkrts.resource.SerializationUtil;

public class TerrainRegion
{
	public static final TerrainRegion NULL_REGION = new TerrainRegion(
		Handle.NULL_HANDLE, Handle.NULL_HANDLE,
		new byte[64], new byte[64]
	);
	
	static byte[] TERRAINREGION_SCHEMA_HASH = Base32.decode("LQJSVLBZPU6RBBT77BNUEOVI5ITLKEUB");

	public final Handle terrainTypePaletteHandle;
	public final Handle subregionPaletteHandle;
	
	public final byte[] terrainTypeIndexes;
	public final byte[] subregionIndexes;
	
	public TerrainRegion(
		Handle terrainTypePaletteHandle, Handle subregionPaletteHandle,
		byte[] terrainTypeIndexes, byte[] subregionIndexes
	) {
		this.terrainTypePaletteHandle = terrainTypePaletteHandle;
		this.subregionPaletteHandle = subregionPaletteHandle;
		this.terrainTypeIndexes = terrainTypeIndexes;
		this.subregionIndexes = subregionIndexes;
	}
	
	public byte[] serialize() {
		byte[] dest = new byte[192];
		SerializationUtil.writeTbbHeader(dest, TERRAINREGION_SCHEMA_HASH);
		SerializationUtil.copyHash( dest, 24, terrainTypePaletteHandle.id );
		SerializationUtil.copyHash( dest, 44, subregionPaletteHandle.id );
		SerializationUtil.copy( dest,  64, terrainTypeIndexes, 0, 64 );
		SerializationUtil.copy( dest, 128, subregionIndexes, 0, 64 );
		return dest;
	}
	
	public static TerrainRegion unserialize( byte[] data ) {
		if( data.length != 192 ) {
			throw new RuntimeException("I can't unserialize this; it's the wrong length!");
		}
		return new TerrainRegion(
			Handle.getInstance( data, 24, 20 ),
			Handle.getInstance( data, 44, 20 ),
			SerializationUtil.copyOf( data,  64, 64 ),
			SerializationUtil.copyOf( data, 128, 64 )
		);
	}
}
