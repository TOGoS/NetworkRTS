package togos.networkrts.experimental.game19.util;

public enum Dir8
{
	RIGHT      (+1, 0),
	DOWN_RIGHT (+1,+1),
	DOWN       ( 0,+1),
	DOWN_LEFT  (-1,+1),
	LEFT       (-1, 0),
	UP_LEFT    (-1,-1),
	UP         ( 0,-1),
	UP_RIGHT   (+1,-1);
	
	protected final int x, y;
	private Dir8(int x, int y) {
		this.x = x;
		this.y = y;
	}
}
