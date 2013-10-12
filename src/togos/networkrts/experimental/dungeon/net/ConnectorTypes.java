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
		private final Class<?> payloadClass;
		private ConnectorType mate;
		
		private PairedConnectorType(String name, Class<?> payloadClass) {
			this.name = name;
			this.payloadClass = payloadClass;
		}
		
		public static ConnectorTypePair createPair(String name, Class<?> payloadClass) {
			PairedConnectorType male   = new PairedConnectorType(name + " (male)", payloadClass);
			PairedConnectorType female = new PairedConnectorType(name + " (female)", payloadClass);
			female.mate = male;
			male.mate = female;
			return new ConnectorTypePair(male, female);
		}
		
		@Override public boolean canCarry(Class<?> payloadClass) {
			return this.payloadClass.isAssignableFrom(payloadClass);
		}
		
		@Override public boolean canConnectTo(ConnectorType other) {
			return other == mate;
		}
		
		@Override public String getName() {
			return name;
		}
	}
}
