package togos.networkrts.experimental.qt2drender.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JPanel;

import togos.networkrts.experimental.qt2drender.ImageHandle;
import togos.networkrts.experimental.qt2drender.Renderer;
import togos.networkrts.experimental.qt2drender.Renderer.RenderNode;

public class RenderDemo2
{
	static class Layer {
		public final int size;
		public final byte[] tileIds;
		public final boolean[] tileVisibility; 
		public Layer background;
		
		public Layer( int size, byte[] tileIds, Layer background, boolean defaultVisibility  ) {
			this.size = size;
			assert tileIds.length >= size;
			this.tileIds = tileIds;
			this.tileVisibility = new boolean[size*size];
			this.background = background;
			fillVisibility(defaultVisibility);
		}
		public Layer( int size, byte[] tileIds ) {
			this( size, tileIds, null, false );
		}
		public Layer( int size ) {
			this( size, new byte[size*size] );
		}
		
		protected boolean cornerVisibility( int x, int y ) {
			if( x > 0 ) {
				if( y > 0 ) {
					if( tileVisibility[size*(y-1)+(x-1)] ) return true;
				}
				if( y < size ) {
					if( tileVisibility[size*(y  )+(x-1)] ) return true;
				}
			}
			if( x < size ) {
				if( y > 0 ) {
					if( tileVisibility[size*(y-1)+(x  )] ) return true;
				}
				if( y < size ) {
					if( tileVisibility[size*(y  )+(x  )] ) return true;
				}
			}
			return false;
		}
		
		protected boolean regionIsInvisible( int x, int y, int s ) {
			for( int dy=0; dy<=s; ++dy ) for( int dx=0; dx<=s; ++dx ) {
				if( cornerVisibility(x+dx, y+dy) ) return false;
			}
			return true;
		}
		
		protected boolean regionIsVisiblyEmpty( int x, int y, int s ) {
			for( int dy=0; dy<s; ++dy ) for( int dx=0; dx<s; ++dx ) {
				int idx = x+dx + (y+dy)*size;
				if( tileIds[idx] != 0 || !tileVisibility[idx] ) return false;
			}
			return true;
		}
		
		public RenderNode toRenderNode(
			RenderNode bgRenderNode, float brightness,
			String[] tileImageNames, ImageHandleCache ihc,
			int x, int y, int s
		) {
			if( regionIsInvisible(x,y,s) ) {
				return RenderNode.EMPTY;
			}
			if( regionIsVisiblyEmpty(x, y, s) ) {
				if( bgRenderNode != null ) {
					return new RenderNode( bgRenderNode, x, y, size, 1, null, null, null, null, null );
				} else {
					return RenderNode.EMPTY;
				}
			}
			
			if( s == 1 ) {
				int idx = size*y + x;
				ImageHandle ih = ihc.getShaded(tileImageNames[tileIds[idx]],
					cornerVisibility(x  ,y  ) ? brightness : 0,
					cornerVisibility(x+1,y  ) ? brightness : 0,
					cornerVisibility(x  ,y+1) ? brightness : 0,
					cornerVisibility(x+1,y+1) ? brightness : 0
				);
				if( ih.hasTranslucentPixels ) {
					return new RenderNode( bgRenderNode, x, y, size, 1, ih, null, null, null, null );
				} else {
					return ih.asOpaqueRenderNode();
				}
			}
			
			// TODO: could handle case where this area is mostly translucent special
			
			int b = s/2;
			return new RenderNode( null, 0, 0, 0, 0, null,
				toRenderNode( bgRenderNode, brightness, tileImageNames, ihc, x+0, y+0, s/2),
				toRenderNode( bgRenderNode, brightness, tileImageNames, ihc, x+b, y+0, s/2),
				toRenderNode( bgRenderNode, brightness, tileImageNames, ihc, x+0, y+b, s/2),
				toRenderNode( bgRenderNode, brightness, tileImageNames, ihc, x+b, y+b, s/2)
			);
		}
		
		public RenderNode toRenderNode( String[] tileImageNames, ImageHandleCache ihc, float brightness ) {
			return toRenderNode(
				background == null ? null : background.toRenderNode(tileImageNames, ihc, brightness * 3/4), brightness,
				tileImageNames, ihc,
				0, 0, size
			);
		}
		
		protected void calculateVisibility( int x, int y ) {
			if( x < 0 || y < 0 || x >= size || y >= size ) return;
			int idx = y*size+x;
			if( tileVisibility[idx] ) return; // already visited
			if( tileIds[idx] != 0 ) return;
			tileVisibility[idx] = true;
			calculateVisibility( x+1, y );
			calculateVisibility( x, y+1 );
			calculateVisibility( x-1, y );
			calculateVisibility( x, y-1 );
		}
		
		public void fillVisibility( boolean value ) {
			for( int i=size*size-1; i>=0; --i ) tileVisibility[i] = value;
		}
		
		public void recalculateVisibilityFrom( int x, int y ) {
			fillVisibility(false);
			calculateVisibility(x,y);
			
		}
	}
	
	static Layer bgLayer5 = new Layer(8, new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 0, 0, 0, 0, 1, 1, 1,
		1, 0, 1, 1, 1, 1, 1, 1,
		1, 0, 1, 2, 1, 2, 1, 1,
		1, 0, 1, 2, 1, 2, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 0, 1, 0, 0, 0, 0, 1,
		1, 1, 1, 0, 0, 0, 1, 1,
	}, null, true);
	static Layer bgLayer4 = new Layer(8, new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 0, 0, 0, 1, 1, 1,
		1, 1, 0, 0, 0, 2, 1, 1,
		1, 1, 0, 0, 2, 0, 1, 1,
		1, 0, 2, 2, 2, 2, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 0, 2, 0, 2, 1, 0, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
	}, bgLayer5, true);
	static Layer bgLayer3 = new Layer(8, new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 2, 2, 0, 1,
		1, 1, 1, 0, 2, 0, 0, 1,
		1, 0, 2, 2, 2, 2, 2, 1,
		1, 2, 2, 0, 2, 1, 0, 1,
		1, 0, 2, 0, 2, 1, 0, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
	}, bgLayer4, true);
	static Layer bgLayer2 = new Layer(8, new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 2, 0, 1,
		1, 1, 1, 0, 1, 0, 0, 1,
		1, 0, 2, 0, 1, 2, 2, 1,
		1, 0, 2, 0, 2, 1, 0, 1,
		1, 0, 2, 0, 2, 1, 0, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
	}, bgLayer3, true);
	static Layer bgLayer1 = new Layer(8, new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 0, 0, 1,
		1, 1, 1, 0, 1, 0, 0, 1,
		1, 0, 0, 0, 1, 2, 2, 1,
		1, 0, 0, 0, 2, 1, 0, 1,
		1, 0, 0, 0, 2, 1, 0, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
	}, bgLayer2, true);
	static Layer testLayer = new Layer(8, new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 0, 0, 0, 1,
		1, 1, 1, 0, 0, 0, 0, 1,
		1, 0, 0, 0, 0, 2, 2, 1,
		1, 0, 0, 0, 2, 1, 0, 1,
		1, 0, 0, 0, 2, 1, 0, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
	}, bgLayer1, false);
	static RenderNode n;
	static {
		testLayer.recalculateVisibilityFrom(2,6);
		String[] tileNames = new String[] {
			null,
			"tile-images/2.png",
			"tile-images/2cheese.png"
		};
		ImageHandleCache ihc = new ImageHandleCache();
		n = testLayer.toRenderNode( tileNames, ihc, 1 );
	}
	
	static class TestCanvas extends JPanel {
		private static final long serialVersionUID = 1L;
		
		Renderer r = new Renderer();
		long ts = 0;
		
		public TestCanvas() {
			setPreferredSize(new Dimension(512,384));
		}
		
		@Override public void paint( Graphics g ) {
			int nodeSize = 8;
			float scale = 512;
			
			float dx = (float)(Math.cos(ts * 0.01));
			float dy = (float)(Math.sin(ts * 0.01));
			
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			r.drawPortal( n, dx-nodeSize/2, dy-nodeSize/2, nodeSize, 4, g, scale, getWidth()/2, getHeight()/2 );
		}
		
		public void setTs(long ts) {
			this.ts = ts;
			repaint();
		}
	}
	
	public static void main( String[] args ) throws InterruptedException {
		final Frame f = new Frame();
		final TestCanvas tc = new TestCanvas();
		f.setBackground(Color.BLACK);
		f.add(tc);
		f.pack();
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing( WindowEvent evt ) {
				f.dispose();
			}
		});
		f.setVisible(true);
		long ts = 0;
		while( f.isVisible() ) {
			Thread.sleep(10);
			tc.setTs(++ts);
		}
	}
}
