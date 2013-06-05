package togos.networkrts.experimental.dungeon;

public class Raycast
{
	static void raycastXY( final Room r, final float x, final float y, final int z, BlockField dest, int destX, int destY ) {
		dest.clear();
		
		CellCursor cursor = new CellCursor();
		
		final int rayCount=1024;
		for( int i=0; i<rayCount; ++i ) {
			// 64 rays!
			float angle = i*(float)Math.PI*2/rayCount;
			float dx = (float)Math.cos(angle);
			float dy = (float)Math.sin(angle);
			float ox = x - (float)Math.floor(x), oy = y - (float)Math.floor(y); 
			cursor.set( r, x, y, z );
			float visibility = 1;
			for( int j=100; j>=0 && visibility > 0; cursor.move(dx,dy,0), ox += dx, oy += dy, --j ) {
				//int cellX = (int)Math.floor(cx), cellY = (int)Math.floor(cy);
				Block[] stack = cursor.getStack();
				if( stack == null ) break;
				
				int destCX = (int)(destX+ox);
				int destCY = (int)(destY+oy);
				
				if( destCX < 0 || destCX >= dest.w ) break;
				if( destCY < 0 || destCY >= dest.h ) break;
				
				for( int zz=0; zz<dest.d && zz<cursor.room.blockField.d; ++zz ) {
					dest.setStack( destCX, destCY, zz, cursor.getStackAtZ(zz) );
				}
				
				for( Block b : stack ) visibility -= b.opacity;
			}
		}
	}

}
