package togos.networkrts.lwjglshaderdemo;

import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GLContext;

public class LWJGLTextureRenderDemo
{
	static class BoundFrameBuffer {
		public final int framebufferId;
		public final int textureId;
		public final int width, height;
		
		public BoundFrameBuffer( int f, int t, int w, int h ) {
			this.framebufferId = f;
			this.textureId = t;
			this.width = w;
			this.height = h;
		}
	}
	
	protected static BoundFrameBuffer newFrameBuffer( int w, int h ) {
		int framebufferId = glGenFramebuffersEXT();
		int textureId = glGenTextures();
		
		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, framebufferId); 						// switch to the new framebuffer
		
		glBindTexture(GL_TEXTURE_2D, textureId);									// Bind the colorbuffer texture
		//glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);				// make it linear filterd
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0,GL_RGBA, GL_INT, (java.nio.ByteBuffer) null);	// Create the texture data
		glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT,GL_COLOR_ATTACHMENT0_EXT,GL_TEXTURE_2D, textureId, 0); // attach it to the framebuffer
		
		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);									// Swithch back to normal framebuffer rendering
		
		return new BoundFrameBuffer(framebufferId, textureId, w, h);
	}
	
	public static void main( String[] args ) throws Exception {
		int screenWidth = 640;
		int screenHeight = 480;
		
		Display.setDisplayMode(new DisplayMode(screenWidth, screenHeight));
		Display.setResizable(true);
		Display.create();

		System.err.println(
			"Normal buffer bits: "+
			glGetInteger(GL_RED_BITS)+","+
			glGetInteger(GL_GREEN_BITS)+","+
			glGetInteger(GL_BLUE_BITS)+","+
			glGetInteger(GL_ALPHA_BITS)
		);

		System.err.println(
			"Accumulation buffer bits: "+
			glGetInteger(GL_ACCUM_RED_BITS)+","+
			glGetInteger(GL_ACCUM_GREEN_BITS)+","+
			glGetInteger(GL_ACCUM_BLUE_BITS)+","+
			glGetInteger(GL_ACCUM_ALPHA_BITS)
		);
		
		boolean fboEnabled = GLContext.getCapabilities().GL_EXT_framebuffer_object;
		System.err.println(
			"Frame Buffer Object support: "+fboEnabled
		);
		
		BoundFrameBuffer primaryFb = newFrameBuffer(100, 100);
		BoundFrameBuffer blurFb = newFrameBuffer(4, primaryFb.height/4);
		
		int iters = 1;
		float x = 0, y = 0, ang = 0, size=16, dx=100.6f, dy=70.7f;
		
		long prevTickTime = System.nanoTime();
		while( !Display.isCloseRequested() ) {
			long currentTime = System.nanoTime();
			long interval = currentTime-prevTickTime;
			x += dx * interval / 1000000000d / iters;
			y += dy * interval / 1000000000d / iters;
			if( x < 0   ) { x = 0  ; dx *= -1; }
			if( x > 300 ) { x = 300; dx *= -1; }
			if( y < 0   ) { y = 0  ; dy *= -1; }
			if( y > 300 ) { y = 300; dy *= -1; }
			ang += 0.1;
			prevTickTime = currentTime;
			
			glBindTexture(GL_TEXTURE_2D, 0);								// unlink textures because if we dont it all is gonna fail
			glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, primaryFb.framebufferId);		// switch to rendering on our FBO

			
			// Uhm
			glViewport (0, 0, primaryFb.width, primaryFb.height);									// set The Current Viewport to the fbo size
			glMatrixMode (GL_PROJECTION);								// Select The Projection Matrix
			glLoadIdentity ();											// Reset The Projection Matrix
			glOrtho(0, 300, 0, 300, 100, -100);
			glMatrixMode (GL_MODELVIEW);								// Select The Modelview Matrix
			glLoadIdentity ();											// Reset The Modelview Matrix
			
			glClearColor(0f, 0.0f, 0.0f, 1);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glColor3f(0.5f, 0.4f, 0);
			glEnable(GL_BLEND);
			glBlendFunc(GL_ONE, GL_ONE);
			glPushMatrix();
			{
				glTranslatef(x, y, 0);
				glRotatef(ang, 0, 0.1f, 1);
				glScalef(2, 2, 1);
				glBegin(GL_QUADS);
				{
					glVertex2f(-size, +size);
					glVertex2f(+size, +size);
					glVertex2f(+size, -size);
					glVertex2f(-size, -size);
				}
				glEnd();
			}
			glPopMatrix();
			glPushMatrix();
			{
				glTranslatef(x, y, 0);
				glRotatef(ang*2, 0, 0.1f, 1);
				glScalef(2, 2, 1);
				glBegin(GL_QUADS);
				{
					glVertex2f(-size, +size);
					glVertex2f(+size, +size);
					glVertex2f(+size, -size);
					glVertex2f(-size, -size);
				}
				glEnd();
			}
			
			
			
			glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, blurFb.framebufferId);					// switch to rendering on the blur framebuffer
			
			glViewport (0, 0, blurFb.width, blurFb.height);									// set The Current Viewport to the fbo size
			glMatrixMode (GL_PROJECTION);								// Select The Projection Matrix
			glLoadIdentity ();											// Reset The Projection Matrix
			glOrtho(0, 1, 0, 1, 100, -100);
			glMatrixMode (GL_MODELVIEW);								// Select The Modelview Matrix
			glLoadIdentity ();											// Reset The Modelview Matrix
			
			glEnable(GL_TEXTURE_2D);										// enable texturing
			glEnable(GL_BLEND);
			glEnable( GL_POLYGON_SMOOTH );
			glHint( GL_POLYGON_SMOOTH_HINT, GL_NICEST );
			glClearColor (0.0f, 0.0f, 0.0f, 0.5f);
			glClear (GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);			// Clear Screen And Depth Buffer on the framebuffer to black
			//glOrtho(0, 1, 0, 1, 0, -100);
			glBindTexture(GL_TEXTURE_2D, primaryFb.textureId);					// bind our FBO texture
			glBegin(GL_QUADS);
			{
				glTexCoord2f(0.0f, 0.0f); glVertex2f(0, 0);	// Bottom Left Of The Texture and Quad
				glTexCoord2f(1.0f, 0.0f); glVertex2f(1, 0);	// Bottom Right Of The Texture and Quad
				glTexCoord2f(1.0f, 1.0f); glVertex2f(1, 1);	// Top Right Of The Texture and Quad
				glTexCoord2f(0.0f, 1.0f); glVertex2f(0, 1);	// Top Left Of The Texture and Quad
			}
			glEnd();
			
			
			
			
			
			glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);					// switch to rendering on the screen
			
			int drawSize = Math.min(Display.getWidth(), Display.getHeight());
			glViewport((Display.getWidth()-drawSize)/2, (Display.getHeight()-drawSize)/2, drawSize, drawSize);
			glMatrixMode (GL_PROJECTION);								// Select The Projection Matrix
			glLoadIdentity ();											// Reset The Projection Matrix
			glOrtho(0, 1, 0, 1, 100, -100);
			glMatrixMode (GL_MODELVIEW);								// Select The Modelview Matrix
			glLoadIdentity ();											// Reset The Modelview Matrix
			glClearColor (0.0f, 0.0f, 0.0f, 0.5f);
			glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT); // Color because blending, depth because that buffer's shared
			//glOrtho(0, 1, 0, 1, 0, -100);
			glEnable(GL_BLEND);
			glBlendFunc(GL_ONE, GL_ONE);
			
			glBindTexture(GL_TEXTURE_2D, primaryFb.textureId);					// bind our FBO texture
			glColor3f(0.7f, 0.7f, 0.7f);
			glBegin(GL_QUADS);
			{
				glTexCoord2f(0.0f, 0.0f); glVertex2f(0, 0);	// Bottom Left Of The Texture and Quad
				glTexCoord2f(1.0f, 0.0f); glVertex2f(1, 0);	// Bottom Right Of The Texture and Quad
				glTexCoord2f(1.0f, 1.0f); glVertex2f(1, 1);	// Top Right Of The Texture and Quad
				glTexCoord2f(0.0f, 1.0f); glVertex2f(0, 1);	// Top Left Of The Texture and Quad
			}
			glEnd();
			glBindTexture(GL_TEXTURE_2D, blurFb.textureId);					// bind our FBO texture
			glColor3f(0.5f, 0.5f, 0.5f);
			glBegin(GL_QUADS);
			{
				glTexCoord2f(0.0f, 0.0f); glVertex2f(0, 0);	// Bottom Left Of The Texture and Quad
				glTexCoord2f(1.0f, 0.0f); glVertex2f(1, 0);	// Bottom Right Of The Texture and Quad
				glTexCoord2f(1.0f, 1.0f); glVertex2f(1, 1);	// Top Right Of The Texture and Quad
				glTexCoord2f(0.0f, 1.0f); glVertex2f(0, 1);	// Top Left Of The Texture and Quad
			}
			glEnd();
			
			glDisable(GL_TEXTURE_2D);
			glFlush ();
			
			Display.update();
		}
		Display.destroy();
	}
}
