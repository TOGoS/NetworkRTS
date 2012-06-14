package togos.networkrts.experimental.entree2;

import togos.networkrts.experimental.netsim2.Sink;
import togos.networkrts.experimental.shape.RectIntersector;

public final class QuadEntreeNode
{
	public static final WorldObject[] EMPTY_OBJECT_LIST = new WorldObject[0];
	public static final QuadEntreeNode EMPTY = new QuadEntreeNode();
	
	/** Array of objects contained *directly* in this node, not including those in sub-nodes. */
	public final WorldObject[] objects;
	/** Count of all objects in this node and all sub-nodes. */
	public final int objectCount;
	/** Flags of all objects in this node and sub-nodes ORed together. */
	public final long allFlags;
	/** Minimum auto update time of all objects in this node and all sub-nodes. */
	public final long minAutoUpdateTime;
	public final QuadEntreeNode n0;
	public final QuadEntreeNode n1;
	public final QuadEntreeNode n2;
	public final QuadEntreeNode n3;
	
	private QuadEntreeNode() {
		this.objects = EMPTY_OBJECT_LIST;
		this.allFlags = 0;
		this.objectCount = 0;
		this.minAutoUpdateTime = Long.MAX_VALUE;
		this.n0 = this; this.n1 = this;
		this.n2 = this; this.n3 = this;
	}
	
	public QuadEntreeNode(
		WorldObject[] objects, QuadEntreeNode n0, QuadEntreeNode n1, QuadEntreeNode n2, QuadEntreeNode n3
	) {
		this.objects = objects;
		this.n0 = n0; this.n1 = n1;
		this.n2 = n2; this.n3 = n3;
		
		long objectFlags = 0;
		long objectAutoUpdateTime = Long.MAX_VALUE;
		for( int i=0; i<this.objects.length; ++i ) {
			objectFlags |= this.objects[i].getFlags();
			objectAutoUpdateTime = Math.min( objectAutoUpdateTime, this.objects[i].getAutoUpdateTime() );
		}
		
		this.allFlags = objectFlags | n0.allFlags | n1.allFlags | n2.allFlags | n3.allFlags;
		this.objectCount = objects.length + n0.objectCount + n1.objectCount + n2.objectCount + n3.objectCount;
		
		// It might not mess things up too bad, but this would be unneccessary creation
		// of objects!
		assert this.objectCount > 0;
		
		this.minAutoUpdateTime = Math.min( objectAutoUpdateTime, Math.min(
			Math.min( n0.minAutoUpdateTime, n1.minAutoUpdateTime ),
			Math.min( n2.minAutoUpdateTime, n3.minAutoUpdateTime )
		));
	}
	
	protected static final int CAT_SUB0    = 0;
	protected static final int CAT_SUB1    = 1;
	protected static final int CAT_SUB2    = 2;
	protected static final int CAT_SUB3    = 3;
	protected static final int CAT_DIRECT  = 4;
	protected static final int CAT_OUTSIDE = 5;
	
	protected static final boolean fits( WorldObjectUpdate up, double x, double y, double w, double h ) {
		final WorldObject obj = up.worldObject;
		final double rad = obj.getMaxRadius();
		
		final double maxx = x+w, maxy=y+h;
		if( obj.x < x || obj.y < y || obj.x >= maxx || obj.y >= maxy ) return false;
		
		if( obj.x - rad < x-w/2 ) return false;
		if( obj.y - rad < y-h/2 ) return false;
		if( obj.x + rad > maxx+w/2 ) return false;
		if( obj.y + rad > maxy+h/2 ) return false;
		
		return true;
	}
	
	protected static final boolean inList( Object obj, Object[] arr, int begin, int end ) {
		for( int i=begin; i<end; ++i ) {
			if( arr[i] == obj ) return true;
		}
		return false;
	}
	
	protected static final boolean allUpdatesFit( WorldObjectUpdate[] updates, int begin, int end, double x, double y, double w, double h ) {
		for( int i=begin; i<end; ++i ) {
			if( !fits(updates[i], x, y, w, h) ) return false;
		}
		return true;
	}
	
	/**
	 * Returns the number of objects present in both arrays.
	 */
	protected static final int intersectionSize( Object[] l1, int l1begin, int l1end, Object[] l2, int l2begin, int l2end ) {
		int count = 0;
		for( int i=l1begin; i<l1end; ++i ) {
			for( int j=l2begin; j<l2end; ++j ) {
				if( l1[i] == l2[j] ) ++count;
			}
		}
		return count;
	}
	
	protected final QuadEntreeNode updateDirectUnchecked(
		WorldObjectUpdate[] updates, boolean[] catScratch, WorldObject[] scratch,
		final int begin, final int end, double x, double y, double w, double h,
		QuadEntreeNode n0, QuadEntreeNode n1, QuadEntreeNode n2, QuadEntreeNode n3
	) {
		assert catScratch.length >= updates.length;
		assert scratch.length >= updates.length;
		
		int addEnd = begin;
		int removeBegin = end;
		
		for( int i=begin; i<end; ++i ) {
			if( updates[i].isAddition ) {
				scratch[addEnd++] = updates[i].worldObject;
			} else {
				assert inList(updates[i].worldObject, objects, 0, objects.length);
				scratch[--removeBegin] = updates[i].worldObject;
			}
		}
		
		assert intersectionSize( scratch, begin, addEnd, scratch, removeBegin, end ) == 0;
		
		if( addEnd == begin && removeBegin == end ) {
			if( n0 == this.n0 && n1 == this.n1 && n2 == this.n2 && n3 == this.n3 ) {
				return this;
			} else {
				return new QuadEntreeNode( this.objects, n0, n1, n2, n3 );
			}
		} else {
			int newObjectCount = objects.length+(addEnd-begin)-(end-removeBegin);
			if( newObjectCount == 0 && n0 == EMPTY && n1 == EMPTY && n2 == EMPTY && n3 == EMPTY ) return EMPTY;
			
			WorldObject[] newObjects = new WorldObject[newObjectCount];
			int j=0;
			for( int i=0; i<objects.length; ++i ) {
				if( !inList(objects[i],scratch,removeBegin,end) ) {
					newObjects[j++] = objects[i];
				}
			}
			for( int i=begin; i<addEnd; ++i ) {
				newObjects[j++] = scratch[i];
			}
			return new QuadEntreeNode( newObjects, n0, n1, n2, n3 );
		}
	}
	
	protected final QuadEntreeNode updateUnchecked(
		WorldObjectUpdate[] updates, boolean[] catScratch, WorldObject[] scratch,
		final int begin, final int end, double x, double y, double w, double h, int maxSubdivision
	) {
		assert catScratch.length >= updates.length;
		assert scratch.length >= updates.length;
		
		assert allUpdatesFit( updates, begin, end, x, y, w, h );
		
		if( begin == end ) return this; // No updates!
		
		if( maxSubdivision == 0 ) {
			return updateDirectUnchecked(updates, catScratch, scratch, begin, end, x, y, w, h, this.n0, this.n1, this.n2, this.n3);
		}
		
		final double halfW = w/2, halfH = h/2;
		final double halfX = x+halfW, halfY = y+halfH;
		// Divide top/bottom
		for( int i=begin; i<end; ++i ) catScratch[i] = updates[i].worldObject.y >= halfY;
		int midoff = CatSort.sort(updates, catScratch, begin, end);
		
		// Ubdates will be sorted like so:
		//  n0     n1     direct  n2       n3
		// ^begin ^n0end ^n1end  ^n2begin ^n3begin ^end
		
		// Update n0 (begin***n0end...midoff)
		for( int i=begin; i<midoff; ++i ) catScratch[i] = !fits(updates[i], x, y, halfW, halfH);
		final int n0end = CatSort.sort(updates, catScratch, begin, midoff);
		final QuadEntreeNode n0 = this.n0.updateUnchecked(updates, catScratch, scratch, begin, n0end, x, y, halfW, halfX, maxSubdivision-1);
		// Update n1 (n0end***n1end...midoff)
		for( int i=n0end; i<midoff; ++i ) catScratch[i] = !fits(updates[i], halfX, y, halfW, halfH);
		final int n1end = CatSort.sort(updates, catScratch, n0end, midoff);
		final QuadEntreeNode n1 = this.n1.updateUnchecked(updates, catScratch, scratch, n0end, n1end, halfX, y, halfW, halfX, maxSubdivision-1); 
		
		// Update n3 (midoff...n3begin***end)
		for( int i=midoff; i<end; ++i ) catScratch[i] = fits(updates[i], halfX, halfY, halfW, halfH);
		final int n3begin = CatSort.sort(updates, catScratch, midoff, end);
		final QuadEntreeNode n3 = this.n3.updateUnchecked(updates, catScratch, scratch, n3begin, end, halfX, halfY, halfW, halfX, maxSubdivision-1);
		// Update n2 (midoff...n2begin***n3begin)
		for( int i=midoff; i<n3begin; ++i ) catScratch[i] = fits(updates[i], x, halfY, halfW, halfH);
		final int n2begin = CatSort.sort(updates, catScratch, midoff, n3begin);
		final QuadEntreeNode n2 = this.n2.updateUnchecked(updates, catScratch, scratch, n2begin, n3begin, x, halfY, halfW, halfX, maxSubdivision-1);
		
		return updateDirectUnchecked( updates, catScratch, scratch, n1end, n2begin, x, y, w, h, n0, n1, n2, n3 );
	}
	
	protected final QuadEntreeNode update(
		WorldObjectUpdate[] updates, boolean[] catScratch, WorldObject[] scratch,
		int off, int end, double x, double y, double w, double h,
		int maxSubdivision
	) {
		assert catScratch.length >= updates.length;
		assert scratch.length >= updates.length;
		
		if( off == end ) return this; // No updates!
		
		for( int i=off; i<end; ++i ) {
			catScratch[i] = !fits(updates[i], x, y, w, h);
		}
		end = CatSort.sort( updates, catScratch, off, end );
		
		return updateUnchecked(updates, catScratch, scratch, off, end, x, y, w, h, maxSubdivision);
	}

	public final <WorldObjectClass extends WorldObject> void forEachObject(long requireFlags, long maxAutoUpdateTime, RectIntersector s, Sink<WorldObjectClass> callback, double x, double y, double w, double h)
		throws Exception
	{
		if( this.objectCount == 0 ) return;
		if( (this.allFlags & requireFlags) != requireFlags ) return;
		if( maxAutoUpdateTime < this.minAutoUpdateTime ) return;
		if( s.rectIntersection(x, y, w, h) == RectIntersector.INCLUDES_NONE ) return;
		
		final double halfW = w/2, halfH = h/2;
		final double halfX = x+halfW, halfY = y+halfH; 
		n0.forEachObject(requireFlags, maxAutoUpdateTime, s, callback, x    , y    , halfW, halfH);
		n1.forEachObject(requireFlags, maxAutoUpdateTime, s, callback, halfX, y    , halfW, halfH);
		n2.forEachObject(requireFlags, maxAutoUpdateTime, s, callback, x    , halfY, halfW, halfH);
		n3.forEachObject(requireFlags, maxAutoUpdateTime, s, callback, halfX, halfY, halfW, halfH);
		for( WorldObject o : objects ) {
			if( s.rectIntersection(o.x - o.getMaxRadius(), o.y - o.getMaxRadius(), o.getMaxRadius()*2, o.getMaxRadius()*2) == RectIntersector.INCLUDES_NONE ) continue;
			if( (o.getFlags() & requireFlags) != requireFlags ) continue;
			if( o.getAutoUpdateTime() > maxAutoUpdateTime ) continue;
			
			callback.give( (WorldObjectClass)o );
		}
	}
}
