package togos.networkrts.experimental.spacegame;

import java.awt.Canvas;
import java.awt.Frame;
import java.awt.Graphics;

public class Renderer
{
	public final int width, height;
	public final int[] buffer;
	
	public Renderer( int width, int height ) {
		this.width = width;
		this.height = height;
		this.buffer = new int[width*height];
	}
	
	public static void main( String[] args ) {
		final Frame f = new Frame("Space Game");
		f.add(new Canvas() {
			private static final long serialVersionUID = 1L;
			
			@Override public void paint(Graphics g) {
				
			}
			
			@Override public void update(Graphics g) { paint(g); } 
		});
	}
}
