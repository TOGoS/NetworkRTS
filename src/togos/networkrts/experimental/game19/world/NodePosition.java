package togos.networkrts.experimental.game19.world;

public class NodePosition
{
	public final int x, y, sizePower;
	
	public NodePosition( int x, int y, int sizePower ) {
		this.x = x; this.y = y;
		this.sizePower = sizePower;
	}
	
	public double getCenterX() {
		return x + (1<<sizePower)/2.0;
	}
	
	public double getCenterY() {
		return y + (1<<sizePower)/2.0;
	}
}
