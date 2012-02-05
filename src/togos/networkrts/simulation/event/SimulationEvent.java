package togos.networkrts.simulation.event;

public class SimulationEvent implements Comparable
{
	public long triggerTimestamp;
	
	public SimulationEvent( long ts ) {
		this.triggerTimestamp = ts;
	}
	
	public int compareTo(Object o) {
		if( o instanceof SimulationEvent ) {
			SimulationEvent e = (SimulationEvent)o;
			return e.triggerTimestamp < this.triggerTimestamp ? -1 : e.triggerTimestamp > this.triggerTimestamp ? 1 : 0;
		}
		throw new RuntimeException("Don't compare a SimulationEvent to a "+o.getClass()+"!");
	}
}
