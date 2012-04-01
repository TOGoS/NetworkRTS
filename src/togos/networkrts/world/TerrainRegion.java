package togos.networkrts.world;

import org.bitpedia.util.Base32;

import togos.networkrts.resource.SerializationUtil;

public class TerrainRegion
{
	static byte[] TERRAINREGION_SCHEMA_HASH = Base32.decode("LQJSVLBZPU6RBBT77BNUEOVI5ITLKEUB");

	final Handle terrainTypePaletteHandle;
	final Handle subregionPaletteHandle;
	
	final byte[] terrainTypeIndexes;
	final byte[] subregionIndexes;
	
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
