package togos.networkrts.experimental.game19.demo;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import togos.networkrts.experimental.game19.graphics.AWTSurface;
import togos.networkrts.experimental.game19.graphics.Renderer;
import togos.networkrts.experimental.game19.graphics.Surface;
import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.scene.TileLayerData;
import togos.networkrts.experimental.game19.util.ResourceContext;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.BlockStackRSTNode;
import togos.networkrts.ui.ImageCanvas;

public class RenderDemo
{
	ResourceContext rc = new ResourceContext(new File(".ccouch"));
	
	protected Icon loadBlockIcon(String filename) throws IOException {
		String urn = rc.storeFile(new File(filename));
		return new Icon(urn, -0.5f, -0.5f, 0.5f, 1f, 1f);
	}
	
	protected Layer makeLayer() throws IOException {
		Block bricks = new Block(loadBlockIcon("tile-images/dumbrick1.png"));
		Block cheese = new Block(loadBlockIcon("tile-images/2cheese.png"));
		
		BlockStack[] blockStacks = new BlockStack[50]; 
		Random r = new Random();
		for( int i=0; i<50; ++i ) {
			int k = r.nextInt(3);
			switch(k) {
			case 0: blockStacks[i] = BlockStackRSTNode.EMPTY; break; 
			case 1: blockStacks[i] = bricks.stack; break;
			case 2: blockStacks[i] = cheese.stack; break;
			}
		}
		return new Layer(
			new TileLayerData( 5, 5, 2, blockStacks ), -2.5, -2.5, null
		);
	}
	
	public void run() throws Exception {
		Layer layer = makeLayer();
		Renderer renderer = new Renderer(rc);
		BufferedImage sceneBuffer = new BufferedImage(512,384,BufferedImage.TYPE_INT_ARGB);
		Graphics sceneGraphics = sceneBuffer.getGraphics();
		sceneGraphics.setColor(Color.BLACK);
		sceneGraphics.fillRect(0, 0, 512, 384);
		Surface sceneSurface = new AWTSurface(sceneGraphics, rc); 
		
		renderer.draw( layer, 0, 0, 1, sceneSurface, 32, 256, 192 );
		
		final Frame f = new Frame("Game19 Render Demo");
		ImageCanvas ic = new ImageCanvas();
		ic.setImage(sceneBuffer);
		f.add(ic);
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent evt) {
				f.dispose();
			}
		});
		f.pack();
		f.setVisible(true);
	}
	public static void main(String[] args) throws Exception {
		new RenderDemo().run();
	}
}
