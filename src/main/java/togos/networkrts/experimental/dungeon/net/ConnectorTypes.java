package togos.networkrts.experimental.dungeon.net;

public class ConnectorTypes
{
	public static class ConnectorTypePair {
		public final ConnectorType male, female;
		public ConnectorTypePair( ConnectorType male, ConnectorType female ) {
			this.male = male;
			this.female = female;
		}
	}
	
	public static class PairedConnectorType implements ConnectorType {
		private final String name;
		private ConnectorType mate;
		
		private PairedConnectorType(String name) {
			this.name = name;
		}
		
		public static ConnectorTypePair createPair(String name) {
			PairedConnectorType male   = new PairedConnectorType(name + " (male)");
			PairedConnectorType female = new PairedConnectorType(name + " (female)");
			female.mate = male;
			male.mate = female;
			return new ConnectorTypePair(male, female);
		}
		
		@Override public boolean canConnectTo(ConnectorType other) {
			return other == mate;
		}
		
		@Override public String getName() {
			return name;
		}
	}
	
	public static final ConnectorTypePair rj45 = PairedConnectorType.createPair("RJ45");
}
