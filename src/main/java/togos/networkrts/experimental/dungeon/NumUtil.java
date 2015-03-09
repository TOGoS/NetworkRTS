package togos.networkrts.experimental.dungeon;

public class NumUtil
{
	public static int tmod( int num, int den ) {
		int rem = num % den;
		return rem < 0 ? den + rem : rem;
	}
	
	public static void main( String[] args ) {
		for( int i=-10; i<=10; ++i ) {
			System.err.println(String.format("% 3d %% % 2d = % 3d", i, 5, tmod(i,5)));
		}
	}
}
