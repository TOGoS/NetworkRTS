package togos.networkrts.experimental.qt2drender.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import togos.blob.InputStreamable;
import togos.networkrts.experimental.game18.demo.DemoUtil;
import togos.networkrts.experimental.qt2drender.AWTDisplay;
import togos.networkrts.experimental.qt2drender.Blackifier;
import togos.networkrts.experimental.qt2drender.ImageHandle;
import togos.networkrts.experimental.qt2drender.QTRenderNode;
import togos.networkrts.experimental.qt2drender.Renderer;
import togos.networkrts.experimental.qt2drender.Sprite;
import togos.networkrts.experimental.qt2drender.VizState;
import togos.networkrts.repo.BitprintFileRepository;
import togos.networkrts.util.Getter;
import togos.networkrts.util.ResourceHandle;
import togos.networkrts.util.ResourceNotFound;
import togos.networkrts.util.SoftResourceHandle;
import togos.networkrts.util.StorageUtil;

public class NetRenderDemo
{
	public static class RenderContext {
		protected final Getter<InputStreamable> blobResolver;
		
		public RenderContext( Getter<InputStreamable> blobResolver ) {
			assert blobResolver != null;
			this.blobResolver = blobResolver;
		}
		
		protected <T> Getter<T> makeGetter( final Class<T> c ) {
			return new Getter<T>() {
				@Override public T get(String uri) throws ResourceNotFound {
					InputStreamable blob = blobResolver.get(uri);
					try {
						InputStream is = blob.openInputStream();
						try {
							ObjectInputStream ois = new ObjectInputStream(is);
							return c.cast(ois.readObject());
						} finally {
							is.close();
						}
					} catch( ClassNotFoundException e ) {
						throw new ResourceNotFound(uri, e);
					} catch( IOException e ) {
						throw new ResourceNotFound(uri, e);
					}
				}
			};
		}
		
		static final Pattern FOG_PATTERN = Pattern.compile("fog:(\\d+),(\\d+)");
		
		public final Getter<ImageHandle[]> imagePaletteResolver = makeGetter(ImageHandle[].class);
		public final Getter<QTRenderNode> renderNodeResolver = makeGetter(QTRenderNode.class);
		public final Getter<BufferedImage> imageResolver = new Getter<BufferedImage>() {
			public BufferedImage get(String uri) throws ResourceNotFound {
				Matcher m;
				if( (m = FOG_PATTERN.matcher(uri)).matches() ) {
					int size = Integer.parseInt(m.group(1));
					int fog = Integer.parseInt(m.group(2));
					return createFogImage(size,
						(fog&1) != 0, (fog&2) != 0, (fog&4) != 0, (fog&8) != 0
					);
				}
				
				try {
					InputStream is = blobResolver.get(uri).openInputStream();
					try {
						return ImageIO.read(is);
					} finally {
						is.close();
					}
				} catch( IOException e ) {
					throw new ResourceNotFound(uri, e);
				}
			}
		};
		
		public ImageHandle[] getImagePalette( ResourceHandle<ImageHandle[]> handle ) throws ResourceNotFound {
			return handle.getValue(imagePaletteResolver);
		}
		
		public QTRenderNode getRenderNode( ResourceHandle<QTRenderNode> handle ) throws ResourceNotFound {
			return handle.getValue(renderNodeResolver);
		}
		
		public QTRenderNode[] getRenderNodes( VizState.BackgroundLink[] links ) throws ResourceNotFound {
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
				fogImages[idx] = createFogImageHandle(16, idx);
			}
			return fogImages[idx];
		}
		
		protected BufferedImage createFogImage( int size, boolean v0, boolean v1, boolean v2, boolean v3 ) {
			BufferedImage bImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			return Blackifier.shade(bImg, 1, v0?1:0, v1?1:0, v2?1:0, v3?1:0);
		}
		
		protected ImageHandle createFogImageHandle( int size, int fogged ) {
			return new ImageHandle("fog:"+size+","+fogged);
		}
	
	}
	public static boolean cellIsCompletelyInvisible( VizState vs, int x, int y ) {
		int sp1 = vs.size+1;
		int idx0 = sp1*y+x;
		int idx1 = idx0+1;
		int idx2 = idx0+sp1;
		int idx3 = idx1+sp1;
		return
			!vs.cornerVisibility[idx0] && !vs.cornerVisibility[idx1] &&
			!vs.cornerVisibility[idx2] && !vs.cornerVisibility[idx3];
	}
	
	public static class VizStateCanvas extends JPanel {
		private static final long serialVersionUID = 1L;
		
		final RenderContext ctx;
		final AWTDisplay disp;
		public VizStateCanvas( RenderContext ctx ) {
			this.ctx = ctx;
			this.disp = new AWTDisplay(96, ctx.imageResolver);
		}
		
		protected VizState vs;
		public void setState( VizState vs ) {
			this.vs = vs;
			repaint();
		}
		
		float distance = 4, scale = 128;
		public void setDistance( float d ) {
			distance = d;
			repaint();
		}
		public void setScale( float s ) {
			scale = s;
			repaint();
		}
		@Override public void paint( Graphics g ) {
			g.setColor(Color.BLACK);
			g.fillRect(0,0,getWidth(),getHeight());
			disp.init(g, getWidth(), getHeight());
			VizState vs = this.vs;
			if( vs == null ) return;
			try {
				Renderer.draw(vs, vs.focusX, vs.focusY, distance, disp, getWidth()/2, getHeight()/2, scale, ctx);
			} catch( ResourceNotFound e ) {
				e.printStackTrace();
			}
		}
	}
	
	public static VizState makeVizState( BitprintFileRepository br, long ts )
		throws IOException, ResourceNotFound
	{
		int size = 5;
		//for( int i=size*size-1; i>=0; --i ) bgLinks[i]
				
		//Storage stor = new Storage();
		
		BufferedImage tile1 = ImageIO.read(new File("tile-images/1.png"));
		BufferedImage tile1Shaded = Blackifier.shade(tile1, 0.7f, 1, 1, 1, 1);
		
		ImageHandle bgIh = new ImageHandle(StorageUtil.storeImage(br, tile1Shaded));
		QTRenderNode bgNode = new QTRenderNode(null, 0, 0, 0, 0, QTRenderNode.EMPTY_SPRITE_LIST,
			bgIh.getSingle(), null, null, null, null
		);

		String bgNodeUrn = StorageUtil.storeSerialized(br, bgNode);
		
		VizState.BackgroundLink[] bgLinks = new VizState.BackgroundLink[] {
			null,
			new VizState.BackgroundLink(new SoftResourceHandle<QTRenderNode>(bgNodeUrn), 5, 0, 0, 1)
		};
		byte[] cellBackgrounds = new byte[] {
			1, 1, 0, 0, 0,
			1, 1, 0, 0, 0,
			0, 1, 1, 0, 0,
			0, 0, 1, 0, 0,
			0, 0, 0, 0, 0
		};
		
		ImageHandle ih0 = new ImageHandle(StorageUtil.storeImage(br, new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB)));
		ImageHandle ih1 = new ImageHandle(StorageUtil.storeImage(br, ImageIO.read(new File("tile-images/2.png"))));
		
		//String thang = stor.storeObject(new ImageHandle[]{ih});
		SoftResourceHandle<ImageHandle[]> tilePalette = new SoftResourceHandle<ImageHandle[]>(StorageUtil.storeSerialized(br, new ImageHandle[]{ih0, ih1}));
		
		byte[][] tileLayers = new byte[1][size*size];
		tileLayers[0] = new byte[] {
			0, 0, 1, 1, 1,
			0, 0, 1, 1, 1,
			1, 0, 0, 1, 1,
			1, 1, 0, 1, 1,
			1, 1, 1, 1, 1,
		};
		
		boolean[] cornerVisibility = new boolean[] {
			false, false, false, false, false, false,
			false, true , true , false, false, false,
			false, true , true , true , false, false,
			false, true , true , true , false, false,
			false, false, true , true , false, false,
			false, false, false, false, false, false,
		};
		Sprite[] sprites = new Sprite[0];
		
		return new VizState(
			5,
			2.5f, 2.5f,
			(float)Math.sin(ts/20f), (float)Math.cos(ts/19f),
			bgLinks, cellBackgrounds,
			tilePalette, tileLayers, 
			cornerVisibility, sprites
		);
	}
	
	public static void main( String[] args ) throws Exception {
		final JFrame f = new JFrame("NetRenderDemo");
		final VizStateCanvas vsc = new VizStateCanvas(new RenderContext(DemoUtil.DEFAULT_BLOB_REPO.toBlobGetter()));
		vsc.setPreferredSize(new Dimension(800,600));
		f.add(vsc);
		f.pack();
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		long ts = 0;
		while(true) {
			vsc.setState(makeVizState(DemoUtil.DEFAULT_BLOB_REPO, ts++));
			Thread.sleep(10);
		}
	}
}
