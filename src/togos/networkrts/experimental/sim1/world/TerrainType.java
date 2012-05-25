package togos.networkrts.experimental.sim1.world;

import java.awt.Color;

public class TerrainType
{
	public static final TerrainType NULL_TERRAIN_TYPE = new TerrainType( Color.BLACK );
	
	public final Color color;
	
	public TerrainType( Color color ) {
		this.color = color;
	}
}
