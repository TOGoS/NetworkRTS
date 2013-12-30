package togos.networkrts.experimental.qt2drender.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import togos.blob.FileInputStreamable;
import togos.blob.InputStreamable;
import togos.networkrts.experimental.qt2drender.AWTDisplay;
import togos.networkrts.experimental.qt2drender.Blackifier;
import togos.networkrts.experimental.qt2drender.Display;
import togos.networkrts.experimental.qt2drender.ImageHandle;
import togos.networkrts.experimental.qt2drender.QTRenderNode;
import togos.networkrts.experimental.qt2drender.Renderer;
import togos.networkrts.experimental.qt2drender.VizState;
import togos.networkrts.experimental.qt2drender.Sprite;
import togos.networkrts.util.Getter;
import togos.networkrts.util.ResourceHandle;

public class NetRenderDemo
{
	static class RenderContext {
		Getter<InputStreamable> blobResolver;
		
		protected <T> Getter<T> makeGetter( final Class<T> c ) {
			return new Getter<T>() {
				@Override public T get(String uri) throws Exception {
					InputStreamable blob = blobResolver.get(uri);
					InputStream is = blob.openInputStream();
					try {
						ObjectInputStream ois = new ObjectInputStream(is);
						return c.cast(ois.readObject());
					} finally {
						is.close();
					}
				}
			};
		}
		
		Getter<ImageHandle[]> imagePaletteResolver = makeGetter(ImageHandle[].class);
		Getter<QTRenderNode> renderNodeResolver = makeGetter(QTRenderNode.class);
		
		public ImageHandle[] getImagePalette( ResourceHandle<ImageHandle[]> handle ) {
			return handle.getValue(imagePaletteResolver);
		}
		
		public QTRenderNode getRenderNode( ResourceHandle<QTRenderNode> handle ) {
			return handle.getValue(renderNodeResolver);
		}
		
		public QTRenderNode[] getRenderNodes( VizState.BackgroundLink[] links ) {
			QTRenderNode[] nodes = new QTRenderNode[links.length];
			for( int i=0; i<links.length; ++i ) {
				nodes[i] = links[i] == null ? null : getRenderNode(links[i].background);
			}
			return nodes;
		}
		
		ImageHandle[] fogImages = new ImageHandle[16];
		public ImageHandle getFogImage( boolean v0, boolean v1, boolean v2, boolean v3 ) {
			int idx = (v0?1:0) | (v1?2:0) | (v2?4:0) | (v3?8:0);
			if( fogImages[idx] == null ) {
				fogImages[idx] = createFogImage(16, v0, v1, v2, v3);
			}
			return fogImages[idx];
		}
		
		public ImageHandle createFogImage( int size, boolean v0, boolean v1, boolean v2, boolean v3 ) {
			BufferedImage bImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			return new ImageHandle(Blackifier.shade(bImg, 1, v0?1:0, v1?1:0, v2?1:0, v3?1:0));
		}
	}
	
	protected static boolean cellIsCompletelyInvisible( VizState vs, int x, int y ) {
		int sp1 = vs.size+1;
		int idx0 = sp1*y+x;
		int idx1 = idx0+1;
		int idx2 = idx0+sp1;
		int idx3 = idx1+sp1;
		return
			!vs.cornerVisibility[idx0] && !vs.cornerVisibility[idx1] &&
			!vs.cornerVisibility[idx2] && !vs.cornerVisibility[idx3];
	}
	
	public static void draw(
		VizState vs, float wcx, float wcy, float distance,
		Display disp, float scx, float scy, float scale, RenderContext ctx
	) {
		ImageHandle[] tileImages = ctx.getImagePalette(vs.tilePalette);
		QTRenderNode[] backgroundNodes = ctx.getRenderNodes(vs.backgroundPalette);
		
		float dscale = scale/distance;
		
		// TODO
		// Draw all backgrounds
		// Draw foreground layers
		// Draw gradient around visibilty edge
		
		for( int ti=0, ty=0; ty<vs.size; ++ty ) for( int tx=0; tx<vs.size; ++tx, ++ti ) {
			VizState.BackgroundLink bgLink = vs.backgroundPalette[vs.cellBackgrounds[ti]&0xFF];
			if( bgLink == null ) continue;
			QTRenderNode bg = backgroundNodes[vs.cellBackgrounds[ti]&0xFF];
			float bgDistance = distance + bgLink.distance;
			disp.saveClip();
			disp.clip(
				scx + (dscale*(tx-wcx)), scy + (dscale*(ty-wcy)),
				dscale, dscale
			);
			Renderer.drawPortal(
				bg, bgLink.size, wcx+bgLink.centerX, wcy+bgLink.centerY, bgDistance,
				disp, scx, scy, scale
			);
			disp.restoreClip();
		}
		
		final float cellSize = scale/distance;
		int spriteIdx = 0;
		for( int l=0; l<vs.tileLayers.length; ++l ) {
			for( int y=0; y<vs.size; ++y ) for( int x=0; x<vs.size; ++x ) {
				if( cellIsCompletelyInvisible(vs,x,y) ) continue;
				disp.draw(
					tileImages[vs.tileLayers[l][y*vs.size+x]],
					scx + (cellSize*(x-wcx)), scy + (cellSize*(y-wcy)),
					cellSize, cellSize
				);
			}
			while( spriteIdx < vs.sprites.length && vs.sprites[spriteIdx].z < l+1 ) {
				Sprite s = vs.sprites[spriteIdx]; 
				disp.draw(
					s.image,
					scx + (cellSize*(s.x-wcx)), scy + (cellSize*(s.y-wcy)),
					s.w, s.h
				);
				++spriteIdx;
			}
		}
		
		for( int y=0; y<vs.size; ++y ) for( int x=0; x<vs.size; ++x ) {
			int sp1 = vs.size+1;
			int idx0 = sp1*y+x;
			int idx1 = idx0+1;
			int idx2 = idx0+sp1;
			int idx3 = idx1+sp1;
			disp.draw(
				ctx.getFogImage(
					vs.cornerVisibility[idx0], vs.cornerVisibility[idx1],
					vs.cornerVisibility[idx2], vs.cornerVisibility[idx3]
				),
				scx + (cellSize*(x-wcx)), scy + (cellSize*(y-wcy)),
				cellSize, cellSize
			);
		}
	}
	
	static class VizStateCanvas extends JPanel {
		AWTDisplay disp = new AWTDisplay(96);
		RenderContext ctx = new RenderContext();
		public VizStateCanvas() {
		}
		
		protected VizState vs;
		public void setState( VizState vs ) {
			this.vs = vs;
			repaint();
		}
		@Override public void paint( Graphics g ) {
			g.setColor(Color.BLACK);
			g.fillRect(0,0,getWidth(),getHeight());
			disp.init(g, getWidth(), getHeight());
			VizState vs = this.vs;
			if( vs == null ) return;
			draw(vs, vs.centerX, vs.centerY, 1, disp, getWidth()/2, getHeight()/2, 32, ctx);
		}
	}
	
	static class Storage implements Getter<InputStreamable> {
		// TODO: replace with one of those urn:bitprint: thingers
		public String storeObject( Object obj ) throws IOException {
			String name = UUID.randomUUID().toString();
			File f = new File(name);
			FileOutputStream fos = new FileOutputStream(f);
			try {
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(obj);
				oos.close();
			} finally {
				fos.close();
			}
			return name;
		}
		
		@Override public InputStreamable get(String uri) throws Exception {
			return new FileInputStreamable(new File(uri));
		}
	}
	
	public static void main( String[] args ) throws IOException {
		final JFrame f = new JFrame("NetRenderDemo");
		final VizStateCanvas vsc = new VizStateCanvas();
		vsc.setPreferredSize(new Dimension(800,600));
		f.add(vsc);
		f.pack();
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		int size = 5;
		//for( int i=size*size-1; i>=0; --i ) bgLinks[i]
				
		//Storage stor = new Storage();
		
		ImageHandle bgIh = new ImageHandle(Blackifier.shade(ImageIO.read(new File("tile-images/1.png")), 0.7f, 1, 1, 1, 1));
		QTRenderNode bgNode = new QTRenderNode(null, 0, 0, 0, 0, QTRenderNode.EMPTY_SPRITE_LIST,
			bgIh.single, null, null, null, null
		);

		VizState.BackgroundLink[] bgLinks = new VizState.BackgroundLink[] {
			null,
			new VizState.BackgroundLink(new ResourceHandle<QTRenderNode>("kq",bgNode), 5, 0, 0, 1)
		};
		byte[] cellBackgrounds = new byte[] {
			0, 0, 0, 0, 0,
			0, 1, 0, 0, 0,
			0, 1, 1, 0, 0,
			0, 0, 1, 0, 0,
			0, 0, 0, 0, 0
		};
		
		ImageHandle ih0 = new ImageHandle(new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB));
		ImageHandle ih1 = new ImageHandle(ImageIO.read(new File("tile-images/2.png")));
		
		//String thang = stor.storeObject(new ImageHandle[]{ih});
		ResourceHandle<ImageHandle[]> tilePalette = new ResourceHandle<ImageHandle[]>("urn:foo",
			new ImageHandle[]{ih0, ih1});
		
		byte[][] tileLayers = new byte[1][size*size];
		tileLayers[0] = new byte[] {
			1, 1, 1, 1, 1,
			1, 0, 1, 1, 1,
			1, 0, 0, 1, 1,
			1, 1, 0, 1, 1,
			1, 1, 1, 1, 1,
		};
		
		boolean[] cornerVisibility = new boolean[] {
			false, true , false, false, false, false,
			false, true , true , false, false, false,
			false, true , true , true , false, false,
			false, true , true , true , false, false,
			false, false, true , true , false, false,
			false, false, false, false, false, false,
		};
		Sprite[] sprites = new Sprite[0];
		
		vsc.setState(new VizState(
			2, 2, 5,
			bgLinks, cellBackgrounds,
			tilePalette, tileLayers, 
			cornerVisibility, sprites
		));
	}
}
