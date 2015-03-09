package togos.networkrts.experimental.game19.util;

public enum Dir4
{
	RIGHT      (+1, 0),
	DOWN       ( 0,+1),
	LEFT       (-1, 0),
	UP         ( 0,-1);
	
	protected final int x, y;
	private Dir4(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public byte toDirBits() {
		return (byte)(1<<this.ordinal());
	}
}
