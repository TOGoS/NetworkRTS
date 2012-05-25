package togos.networkrts.experimental.s64;

import java.awt.Color;

import togos.networkrts.experimental.s64.fill.GridNode64Filler;
import togos.networkrts.experimental.s64.fill.RandomFiller;
import togos.networkrts.tfunc.ColorFunction;
import togos.networkrts.tfunc.ConstantColorFunction;
import togos.networkrts.tfunc.PulsatingColorFunction;

public class Blocks
{
	public static Block GRASS = new Block(null, Block.FLAG_WALKABLE, new ConstantColorFunction(new Color( 0.02f, 0.6f, 0.05f )));
	public static Block WATER = new Block(null, Block.FLAG_BOATABLE, new ConstantColorFunction(Color.BLUE));
	public static GridNode64[] WATERS = new GridNode64[4];
	static {
		for( int i=0; i<4; ++i ) {
			ColorFunction cf = new PulsatingColorFunction(
				1.0f, 0.0f, 0.15f, 0.65f,
				1.0f, 0.0f, 0.35f, 0.85f,
				4000, 1000*i
			);
			Blocks.WATERS[i] = new HomogeneousGridNode64( Blocks.WATER, new Block(null, Block.FLAG_BOATABLE, cf).getStack() );
		}
	}
	
	public static GridNode64Filler WATER_FILLER = new RandomFiller( Blocks.WATERS );
}
