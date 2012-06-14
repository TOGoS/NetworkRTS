package togos.networkrts.experimental.entree2;

import java.util.Collections;


public class SetEntreeTest extends EntreeTest<SetEntree>
{
	public void setUp() {
		entree = new SetEntree<SimpleWorldObject>(Collections.EMPTY_SET);
	}
	
	static class SimpleWorldObject extends WorldObject {
		final long autoUpdateTime;
		final long flags;
		@Override public long getAutoUpdateTime() { return autoUpdateTime; }
		@Override public long getFlags() { return flags; }
		public SimpleWorldObject( double x, double y, double rad, long aat, long flags ) {
			super( x, y, rad );
			this.autoUpdateTime = aat;
			this.flags = flags;
		}
	}
}
