package togos.networkrts.experimental.dungeon;

import java.awt.Graphics;

public class BlockFieldRenderer
{
	public int tileSize = 16;
	
	/**
	 * @param bf block field to draw
	 * @param cx X position within block field on which to center
	 * @param cy Y position within block field on which to center 
	 * @param g graphics to draw to
	 * @param canvX X position on canvas of left of drawing area
	 * @param canvY Y position on canvas of top of drawing area
	 * @param canvWidth width of drawing area
	 * @param canvHeight height of drawing area
	 */
	public void render( BlockField bf, float cx, float cy, Graphics g, int canvX, int canvY, int canvWidth, int canvHeight ) {
		for( int y=0; y<bf.h; ++y ) {
			for( int x=0; x<bf.w; ++x ) {
				int highestOpaqueLayer = bf.d-1;
				findOpaque: for( int z=bf.d-1; z>=0; --z ) {
					Block[] stack = bf.getStack( x, y, z );
					for( Block b : stack ) {
						if( b.getOpacity() == 1 ) break findOpaque;
					}
					--highestOpaqueLayer; 
				}
				for( int z=highestOpaqueLayer; z<bf.d; ++z ) {
					Block[] stack = bf.getStack( x, y, z );
					for( Block b : stack ) {
						g.setColor(b.getColor());
						g.fillRect( canvX + (int)(canvWidth/2f + (x-cx) * tileSize), canvY + (int)(canvHeight/2f + (y-cy) * tileSize), tileSize, tileSize );
					}
				}
			}
		}
	}
}
