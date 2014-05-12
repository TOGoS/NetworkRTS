package togos.networkrts.lwjglshaderdemo;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

public class LWJGLShaderDemo
{
	public static void main( String[] args ) throws Exception {
		Display.setDisplayMode(new DisplayMode(640, 480));
		Display.setResizable(true);
		Display.create();

		System.err.println(
			"Normal buffer bits: "+
			GL11.glGetInteger(GL11.GL_RED_BITS)+","+
			GL11.glGetInteger(GL11.GL_GREEN_BITS)+","+
			GL11.glGetInteger(GL11.GL_BLUE_BITS)+","+
			GL11.glGetInteger(GL11.GL_ALPHA_BITS)
		);

		System.err.println(
			"Accumulation buffer bits: "+
			GL11.glGetInteger(GL11.GL_ACCUM_RED_BITS)+","+
			GL11.glGetInteger(GL11.GL_ACCUM_GREEN_BITS)+","+
			GL11.glGetInteger(GL11.GL_ACCUM_BLUE_BITS)+","+
			GL11.glGetInteger(GL11.GL_ACCUM_ALPHA_BITS)
		);
		
		int iters = 1;
		float x = 0, y = 0, size=16, dx=10.6f, dy=7.7f;
		
		while( !Display.isCloseRequested() ) {
			GL11.glClear(GL11.GL_ACCUM_BUFFER_BIT);
			
			for( int i=0; i<iters; ++i ) {
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
				GL11.glMatrixMode(GL11.GL_MODELVIEW);
				GL11.glLoadIdentity();
				GL11.glOrtho(0, 300, 0, 300, 0, -100);
				GL11.glColor3f(1f, 0, 0);
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
				GL11.glBegin(GL11.GL_QUADS);
				{
					GL11.glVertex2f(x-size, y+size);
					GL11.glVertex2f(x+size, y+size);
					GL11.glVertex2f(x+size, y-size);
					GL11.glVertex2f(x-size, y-size);
				}
				GL11.glEnd();
				if( iters != 1 ) GL11.glAccum(GL11.GL_ACCUM, 1f/iters);
				x += dx / iters; y += dy / iters;
				if( x < 0   ) { x = 0  ; dx *= -1; }
				if( x > 300 ) { x = 300; dx *= -1; }
				if( y < 0   ) { y = 0  ; dy *= -1; }
				if( y > 300 ) { y = 300; dy *= -1; }
			}
			if( iters != 1 ) GL11.glAccum(GL11.GL_RETURN, 1);
			Display.update();
		}
		Display.destroy();
	}
}
