package togos.networkrts.experimental.qt2drender;

public class QTRenderNode {
	public static final Sprite[] EMPTY_SPRITE_LIST = new Sprite[0];
	public static final QTRenderNode EMPTY = new QTRenderNode(
		null, 0, 0, 0, 0,
		EMPTY_SPRITE_LIST, ImageHandle.EMPTY_ARRAY,
		null, null, null, null
	);
	
	final QTRenderNode background;
	
	/** Size of background node in world units */
	final float backgroundSize;
	/**
	 * Position of background node's center relative to this node's center
	 */
	final float backgroundCenterX, backgroundCenterY;
	/** Distance behind this node of background node */
	final float backgroundDistance;
	
	final Sprite[] sprites;
	
	final ImageHandle[] tileImages;
	final QTRenderNode n0, n1, n2, n3;
	
	/**
	 * z should increase monotonically
	 */
	static boolean spritesSortedProperly( Sprite[] sprites ) {
		float prevDist = Float.NEGATIVE_INFINITY;
		for( Sprite s : sprites ) {
			if( s.z < prevDist ) return false;
			prevDist = s.z;
		}
		return true;
	}
	
	public QTRenderNode( QTRenderNode background, float bgSize, float bgX, float bgY, float bgDistance, Sprite[] sprites, ImageHandle[] tileImages, QTRenderNode n0, QTRenderNode n1, QTRenderNode n2, QTRenderNode n3 ) {
		assert spritesSortedProperly(sprites);
		assert tileImages != null;
		
		this.background = background;
		this.backgroundSize = bgSize;
		this.backgroundCenterX = bgX;
		this.backgroundCenterY = bgY;
		this.backgroundDistance = bgDistance;
		this.sprites = sprites;
		this.tileImages = tileImages;
		this.n0 = n0;
		this.n1 = n1;
		this.n2 = n2;
		this.n3 = n3;
	}
	
	public QTRenderNode withSprite( Sprite...additionalSprites ) {
		if( additionalSprites.length == 0 ) return this;
		Sprite[] newSprites = new Sprite[sprites.length+additionalSprites.length];
		int i=0;
		for( Sprite s : sprites ) newSprites[i++] = s;
		for( Sprite s : additionalSprites ) newSprites[i++] = s;
		return new QTRenderNode(
			background, backgroundSize, backgroundCenterX, backgroundCenterY, backgroundDistance,
			newSprites, tileImages, n0, n1, n2, n3
		);
	}
}