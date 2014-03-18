package togos.networkrts.experimental.game19.world.msg;

import java.util.ArrayList;
import java.util.List;

import togos.networkrts.experimental.game19.demo.ServerClientDemo.Scene;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.scene.Layer.VisibilityClip;
import togos.networkrts.experimental.game19.scene.QuadTreeLayerData;
import togos.networkrts.experimental.game19.scene.TileLayerData;
import togos.networkrts.experimental.game19.scene.VisibilityChecker;
import togos.networkrts.experimental.game19.sim.AsyncTask;
import togos.networkrts.experimental.game19.sim.UpdateContext;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.encoding.WorldConverter;
import togos.networkrts.experimental.gameengine1.index.EntityRanges;
import togos.networkrts.experimental.gameengine1.index.Visitor;

public class UploadSceneTask implements AsyncTask {
	// TODO: All sorts of stuff. Will probably want to
	// make view distance to be a parameter,
	// include various stats,
	// wrap message in a few layers of framing
	final NonTile viewer;
	final World world;
	final long uplinkBitAddress;
	
	public UploadSceneTask( NonTile viewer, World w, long uplinkBitAddress ) {
		this.viewer = viewer;
		this.world = w;
		this.uplinkBitAddress = uplinkBitAddress;
	}
	
	@Override
	public void run( UpdateContext ctx ) {
		int worldRadius = 1<<(world.rstSizePower-1);
		double centerX = viewer.x, centerY = viewer.y;
		
		int intCenterX = (int)Math.floor(centerX);
		int intCenterY = (int)Math.floor(centerY);
		
		int ldWidth = 41;
		int ldHeight = 30;
		// center of layer data
		int ldCenterX = ldWidth/2;
		int ldCenterY = ldHeight/2;
		
		// TODO: Only collect the ones actually visible
		final List<NonTile> visibleNonTiles = new ArrayList<NonTile>();
		
		world.nonTiles.forEachEntity( EntityRanges.BOUNDLESS, new Visitor<NonTile>() {
			@Override public void visit( NonTile v ) {
				visibleNonTiles.add(v);
			}
		});
		
		// There are various ways to go about this:
		// - do visibility checks, send only visible area
		// - send nearby quadtree nodes
		// - send entire world
		
		boolean sendTiles = true;
		Layer l;
		VisibilityClip visibilityClip = new Layer.VisibilityClip(centerX-(ldWidth-1)/2, centerY-(ldHeight-1)/2, centerX+(ldWidth-1)/2, centerY+(ldHeight-1)/2);
		if( sendTiles ) {
			TileLayerData layerData = new TileLayerData( ldWidth, ldHeight, 1 );
			WorldConverter.nodeToLayerData( world.rst, -worldRadius, -worldRadius, 0, 1<<world.rstSizePower, layerData, intCenterX-ldCenterX, intCenterY-ldCenterY, ldWidth, ldHeight );
			VisibilityChecker.calculateAndApplyVisibility(layerData, ldCenterX, ldCenterY, 0, 32);
			l = new Layer( layerData, intCenterX-ldCenterX, intCenterY-ldCenterY, world.background );
		} else {
			int size = 1<<world.rstSizePower;
			l = new Layer( new QuadTreeLayerData(world.rst, size), -size/2.0, -size/2.0, world.background );
		}
		Scene scene = new Scene( l, visibleNonTiles, centerX, centerY, visibilityClip );
		ctx.sendMessage(Message.create(uplinkBitAddress, uplinkBitAddress, MessageType.INCOMING_PACKET, scene));
	}
}