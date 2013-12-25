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

public class RenderDemo
{
	static class TestCanvas extends JPanel {
		private static final long serialVersionUID = 1L;
		
		Renderer r = new Renderer();
		RenderNode n;
		
		public TestCanvas() {
			ImageHandleCache rc = new ImageHandleCache();
			ImageHandle cheese2 = rc.get("tile-images/2cheese.png");
			
			RenderNode greenPanel = new RenderNode( null, 0, 0, 0, 0, rc.get("tile-images/3.png"), null, null, null, null );
			RenderNode yellowPanel = new RenderNode( null, 0, 0, 0, 0, rc.get("tile-images/2.png"), null, null, null, null );
			RenderNode whitePanel = new RenderNode( null, 0, 0, 0, 0, rc.get("tile-images/1.png"), null, null, null, null );
			
			RenderNode bg3 = new RenderNode( null, 0, 0, 0, 0, null,
				greenPanel, null, null, greenPanel
			);
			
			RenderNode bg2 = new RenderNode( null, 0, 0, 0, 0, null,
				yellowPanel,
				new RenderNode( bg3, 64,  0, 128, 1, cheese2, null, null, null, null ),
				new RenderNode( bg3,  0, 64, 128, 1, cheese2, null, null, null, null ),
				yellowPanel
			);
			
			setPreferredSize(new Dimension(512,384));
			setBackground(Color.BLACK);
			
			RenderNode bg1 = new RenderNode( null, 0, 0, 0, 0, null,
				whitePanel,
				new RenderNode( bg2, 64,  0, 128, 1, null, null, null, null, null ),
				new RenderNode( bg2,  0, 64, 128, 1, cheese2, null, null, null, null ),
				whitePanel
			);
			
			RenderNode bg1a = new RenderNode( null, 0, 0, 0, 0, null,
				bg1, bg1, bg1, bg1
			);
			
			RenderNode bg1b = new RenderNode( null, 0, 0, 0, 0, null,
				bg1, bg1a, bg1a, bg1
			);
			
			RenderNode bg1c = new RenderNode( null, 0, 0, 0, 0, null,
				bg1b, bg1a, bg1, bg1b
			);
			
			n = bg1c;
		}
		
		long ts = 0;
		
		@Override public void paint( Graphics g ) {
			int nodeSize = 512;
			float scale = 4;
			
			float dx = (float)(Math.cos(ts * 0.01) * 100);
			float dy = (float)(Math.sin(ts * 0.01) * 100);
			
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			r.drawPortal( n, dx-nodeSize/2, dy-nodeSize/2, nodeSize, 2, g, scale, getWidth()/2, getHeight()/2 );
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
			Thread.sleep(5);
			tc.setTs(++ts);
		}
	}
}
