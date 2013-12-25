package togos.networkrts.experimental.qt2drender.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

import togos.networkrts.experimental.qt2drender.ImageHandle;
import togos.networkrts.experimental.qt2drender.Renderer;
import togos.networkrts.experimental.qt2drender.Renderer.RenderNode;
import togos.networkrts.experimental.qt2drender.Renderer.Sprite;

public class RenderDemo2
{
	static class BlockType {
		final String imageName;
		final boolean opaque;
		final boolean hard;
		
		public BlockType( String imageName, boolean opaque, boolean hard ) {
			this.imageName = imageName;
			this.opaque = opaque;
			this.hard = hard;
		}
	}
	
	static class Layer {
		public final int size;
		public final byte[] tileIds;
		public final BlockType[] blockTypes;
		public final boolean[] tileVisibility;
		public Layer background;
		
		public Layer( int size, byte[] tileIds, BlockType[] blockTypes, Layer background, boolean defaultVisibility  ) {
			this.size = size;
			assert tileIds.length >= size;
			this.tileIds = tileIds;
			this.blockTypes = blockTypes;
			this.tileVisibility = new boolean[size*size];
			this.background = background;
			fillVisibility(defaultVisibility);
		}
		public Layer( int size, byte[] tileIds, BlockType[] blockTypes ) {
			this( size, tileIds, blockTypes, null, false );
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
			ImageHandleCache ihc,
			int x, int y, int s
		) {
			if( regionIsInvisible(x,y,s) ) {
				return RenderNode.EMPTY;
			}
			if( regionIsVisiblyEmpty(x, y, s) ) {
				if( bgRenderNode != null ) {
					return new RenderNode( bgRenderNode, x, y, size, 1, RenderNode.EMPTY_SPRITE_LIST, null, null, null, null, null );
				} else {
					return RenderNode.EMPTY;
				}
			}
			
			if( s == 1 ) {
				int idx = size*y + x;
				ImageHandle ih = ihc.getShaded(blockTypes[tileIds[idx]].imageName,
					cornerVisibility(x  ,y  ) ? brightness : 0,
					cornerVisibility(x+1,y  ) ? brightness : 0,
					cornerVisibility(x  ,y+1) ? brightness : 0,
					cornerVisibility(x+1,y+1) ? brightness : 0
				);
				if( ih.hasTranslucentPixels ) {
					return new RenderNode( bgRenderNode, x, y, size, 1, RenderNode.EMPTY_SPRITE_LIST, ih, null, null, null, null );
				} else {
					return ih.asOpaqueRenderNode();
				}
			}
			
			// TODO: could handle case where this area is mostly translucent special
			
			int b = s/2;
			return new RenderNode( null, 0, 0, 0, 0, RenderNode.EMPTY_SPRITE_LIST, null,
				toRenderNode( bgRenderNode, brightness, ihc, x+0, y+0, s/2),
				toRenderNode( bgRenderNode, brightness, ihc, x+b, y+0, s/2),
				toRenderNode( bgRenderNode, brightness, ihc, x+0, y+b, s/2),
				toRenderNode( bgRenderNode, brightness, ihc, x+b, y+b, s/2)
			);
		}
		
		public RenderNode toRenderNode( ImageHandleCache ihc, float brightness ) {
			return toRenderNode(
				background == null ? null : background.toRenderNode( ihc, brightness * 3/4), brightness,
				ihc, 0, 0, size
			);
		}
		
		protected void calculateVisibility( int x, int y ) {
			if( x < 0 || y < 0 || x >= size || y >= size ) return;
			int idx = y*size+x;
			if( tileVisibility[idx] ) return; // already visited
			if( blockTypes[tileIds[idx]].opaque ) return;
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
	
	static class Entity {
		final long id;
		final float x, y, vx, vy, w, h;
		final String imageName;
		
		public Entity( long id, float x, float y, float vx, float vy, float w, float h, String imageName ) {
			this.id = id;
			this.x = x; this.y = y;
			this.vx = vx; this.vy = vy;
			this.w = w; this.h = h;
			this.imageName = imageName;
		}
	}
	
	static class Room {
		final int size;
		final byte[] tileIds;
		final BlockType[] blockTypes;
		final List<Entity> entities;
		final RenderNode background;
		float backgroundDistance;
		
		public Room( int size, byte[] tileIds, BlockType[] blockTypes, List<Entity> entities, RenderNode background, float backgroundDistance ) {
			assert tileIds.length >= size*size;
			
			this.size = size;
			this.tileIds = tileIds;
			this.blockTypes = blockTypes;
			this.entities = entities;
			this.background = background;
			this.backgroundDistance = backgroundDistance;
		}
		
		public Room update( float time ) {
			ArrayList<Entity> updatedEntities = new ArrayList<Entity>();
			for( Entity e : entities ) {
				float newX = e.x + e.vx*time;
				float newY = e.y + e.vy*time;
				float newVX = e.vx;
				float newVY = e.vy + 9.8f*time;
				
				if( blockTypes[tileIds[(int)e.x + size*(int)(newY + e.h)]].hard ) {
					if( newVY > 0 ) newVY = 0;
					newY = (int)(newY + e.h)-e.h;
				}
				
				updatedEntities.add(new Entity(
					e.id, newX, newY, newVX, newVY, e.w, e.h, e.imageName
				));
			}
			return new Room( size, tileIds, blockTypes, updatedEntities, background, backgroundDistance );
		}
		
		public RenderNode toRenderNode( float viewX, float viewY, ImageHandleCache ihc ) {
			Layer l = new Layer( size, tileIds, blockTypes );
			l.recalculateVisibilityFrom( (int)viewX, (int)viewY );
			
			Sprite[] sprites = new Sprite[entities.size()];
			int i=0;
			for( Entity e : entities ) {
				sprites[i++] = new Sprite( e.x-e.w/2, e.y-e.h/2, 0, ihc.get(e.imageName), e.w, e.h );		
			}
			
			return l.toRenderNode( background, 1, ihc, 0, 0, size ).withSprite(sprites);
		}
		
		public Entity findEntity( long id ) {
			for( Entity e : entities ) {
				if( e.id == id ) return e;
			}
			return null;
		}
	}
	
	static long PLAYER_ID = 0x113322;
	
	static BlockType[] blockTypes = new BlockType[] {
		new BlockType( null, false, false ),
		new BlockType( "tile-images/2.png", true, true ),
		new BlockType( "tile-images/2cheese.png", false, true )
	};
	
	static ImageHandleCache ihc = new ImageHandleCache();
	static Layer bgLayer5 = new Layer(8, new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 0, 0, 0, 0, 1, 1, 1,
		1, 0, 1, 1, 1, 1, 1, 1,
		1, 0, 1, 2, 1, 2, 1, 1,
		1, 0, 1, 2, 1, 2, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 0, 1, 0, 0, 0, 0, 1,
		1, 1, 1, 0, 0, 0, 1, 1,
	}, blockTypes, null, true);
	static Layer bgLayer4 = new Layer(8, new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 0, 0, 0, 1, 1, 1,
		1, 1, 0, 0, 0, 2, 1, 1,
		1, 1, 0, 0, 2, 0, 1, 1,
		1, 0, 2, 2, 2, 2, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 0, 2, 0, 2, 1, 0, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
	}, blockTypes, bgLayer5, true);
	static Layer bgLayer3 = new Layer(8, new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 2, 2, 0, 1,
		1, 1, 1, 0, 2, 0, 0, 1,
		1, 0, 2, 2, 2, 2, 2, 1,
		1, 2, 2, 0, 2, 1, 0, 1,
		1, 0, 2, 0, 2, 1, 0, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
	}, blockTypes, bgLayer4, true);
	static Layer bgLayer2 = new Layer(8, new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 2, 0, 1,
		1, 1, 1, 0, 1, 0, 0, 1,
		1, 0, 2, 0, 1, 2, 2, 1,
		1, 0, 2, 0, 2, 1, 0, 1,
		1, 0, 2, 0, 2, 1, 0, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
	}, blockTypes, bgLayer3, true);
	static Layer bgLayer1 = new Layer(8, new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 0, 0, 1,
		1, 1, 1, 0, 1, 0, 0, 1,
		1, 0, 0, 0, 1, 2, 2, 1,
		1, 0, 0, 0, 2, 1, 0, 1,
		1, 0, 0, 0, 2, 1, 0, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
	}, blockTypes, bgLayer2, true);
	static Layer testLayer = new Layer(8, new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 0, 0, 0, 1,
		1, 1, 1, 0, 0, 0, 0, 1,
		1, 0, 0, 0, 0, 2, 2, 1,
		1, 0, 0, 0, 2, 1, 0, 1,
		1, 0, 0, 0, 2, 1, 0, 1,
		1, 1, 1, 1, 1, 1, 1, 1,
	}, blockTypes, bgLayer1, false);
	static RenderNode n;
	static {
		testLayer.recalculateVisibilityFrom(2,6);
		n = testLayer.toRenderNode( ihc, 1 );
	}
	
	static class TestCanvas extends JPanel {
		private static final long serialVersionUID = 1L;
		
		Renderer r = new Renderer();
		long ts = 0;
		
		public TestCanvas() {
			setPreferredSize(new Dimension(512,384));
		}
		
		Room room;
		
		public void setRoom( Room r ) {
			this.room = r;
			repaint();
		}
		
		@Override public void paint( Graphics g ) {
			int nodeSize = 8;
			float scale = 512;
			
			//float dx = (float)(Math.cos(ts * 0.01));
			//float dy = (float)(Math.sin(ts * 0.01));
			
			Room room = this.room;
			if( room == null ) return;
			
			Entity player = room.findEntity(PLAYER_ID);
			if( player == null ) return;
			
			RenderNode n = room.toRenderNode( (int)player.x, (int)player.y, ihc );
			
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			r.drawPortal( n, -player.x, -player.y, nodeSize, 4, g, scale, getWidth()/2, getHeight()/2 );
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
		
		ArrayList<Entity> roomEntities = new ArrayList<Entity>();
		roomEntities.add(new Entity(PLAYER_ID, 3.5f, 3.5f, 0, 0, 0.8f, 0.8f, "tile-images/dude.png"));
		
		Room room = new Room(testLayer.size, testLayer.tileIds, testLayer.blockTypes, roomEntities,
			bgLayer1.toRenderNode(ihc, 0.75f), 1);

		f.setVisible(true);
		long ts = 0;
		while( f.isVisible() ) {
			Thread.sleep(10);
			tc.setRoom(room);
			room = room.update(0.01f);
		}
	}
}
