package togos.networkrts.world;

import java.awt.Color;

public class TerrainType
{
	public static final TerrainType NULL_TERRAIN_TYPE = new TerrainType( Color.BLACK );
	
	public final Color color;
	
	public TerrainType( Color color ) {
		this.color = color;
	}
}
