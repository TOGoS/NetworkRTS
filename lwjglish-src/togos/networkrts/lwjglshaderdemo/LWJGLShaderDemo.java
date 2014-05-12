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
		while( !Display.isCloseRequested() ) {
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glLoadIdentity();
			GL11.glOrtho(0, 300, 0, 300, 0, -100);
			GL11.glColor3f(1, 0.5f, 0.3f);
			GL11.glBegin(GL11.GL_QUADS);
			{
				GL11.glVertex2f(100, 100);
				GL11.glVertex2f(200, 100);
				GL11.glVertex2f(200, 200);
				GL11.glVertex2f(100, 200);
			}
			GL11.glEnd();
			Display.update();
		}
		Display.destroy();
	}
}
