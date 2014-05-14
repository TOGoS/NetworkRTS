package togos.networkrts.util;

public interface RealTimeKeeper
{
	public static final RealTimeKeeper SYSTEM_NANOTIME = new RealTimeKeeper() {
		@Override public long currentTime() { return System.nanoTime(); }
	};
	
	public long currentTime();
}
