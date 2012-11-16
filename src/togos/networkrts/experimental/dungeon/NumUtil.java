package togos.networkrts.experimental.dungeon;

public class NumUtil
{
	public static int tmod( int num, int den ) {
		int rem = num % den;
		return rem < 0 ? den + rem : rem;
	}
}
