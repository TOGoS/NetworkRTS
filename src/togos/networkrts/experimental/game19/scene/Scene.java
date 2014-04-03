package togos.networkrts.experimental.game19.scene;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import togos.networkrts.experimental.game19.scene.Layer.VisibilityClip;
import togos.networkrts.experimental.game19.world.NonTile;

/**
 * A scene represents a portion of the world.
 * The primary use for a scene is to send the part of the world
 * that a character can see to a client to display.
 */
public class Scene
{
	// TODO: Move cell visibility into Scene
	//   (so it can be drawn over everything, including sprites)
	
	public final Layer layer;
	public final List<NonTile> nonTiles;
	// Point within the scene that should be centered on (usually the player)
	public final double poiX, poiY;
	/**
	 * Section of the scene that is visible
	 * (offsets are relative to the layer's origin)
	 **/
	public final VisibilityClip visibilityClip;
	
	protected static final Comparator<NonTile> NONTILE_COMPARATOR = new Comparator<NonTile>() {
		public int compare(NonTile arg0, NonTile arg1) {
			float z0 = arg0.getIcon().imageZ, z1 = arg1.getIcon().imageZ;  
			return z0 < z1 ? -1 : z0 > z1 ? 1 : 0;
		}
	};
	
	public Scene( Layer layer,  List<NonTile> nonTiles, double poiX, double poiY, VisibilityClip visibilityClip ) {
		this.layer = layer;
		Collections.sort(nonTiles, NONTILE_COMPARATOR);
		this.nonTiles = nonTiles;
		this.poiX = poiX;
		this.poiY = poiY;
		this.visibilityClip = visibilityClip;
	}
}