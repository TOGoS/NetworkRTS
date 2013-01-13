package togos.networkrts.experimental.gensim5;

import java.io.IOException;

public interface RealTimeEventSource<EventClass>
{
	public EventClass recv( long returnBy ) throws IOException, InterruptedException;
	public long getCurrentTime();
}