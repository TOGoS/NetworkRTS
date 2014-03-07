package togos.networkrts.experimental.gameengine1.index;

import togos.networkrts.util.BitAddressRange;
import togos.networkrts.util.HasNextAutoUpdateTime;

// TODO: It might make things more symmetrical
// to change next auto update time to min, max update time
public interface EntityRange extends HasAABB, BitAddressRange, HasNextAutoUpdateTime { }
