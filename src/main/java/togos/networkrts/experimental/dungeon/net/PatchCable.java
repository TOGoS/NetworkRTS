package togos.networkrts.experimental.dungeon.net;

import togos.networkrts.experimental.dungeon.DungeonGame.InternalUpdater;

public class PatchCable<Payload>
{
	public final long delay;
	public final AbstractConnector<Payload> right, left;
	protected final InternalUpdater updater;
	
	public PatchCable( ConnectorType leftCType, ConnectorType rightCType, Class<Payload> payloadClass, long _delay, InternalUpdater _updater ) {
		this.delay = _delay;
		this.updater = _updater;
		this.left = new AbstractConnector<Payload>(leftCType, payloadClass) {
			@Override public boolean isLocked() { return false; }
			@Override public void messageReceived(Payload message) {
				updater.addTimer(updater.getCurrentTime() + delay, right.backside, message);
			};
		};
		this.right = new AbstractConnector<Payload>(rightCType, payloadClass) {
			@Override public boolean isLocked() { return false; }
			@Override public void messageReceived(Payload message) {
				updater.addTimer(updater.getCurrentTime() + delay, left.backside, message);
			};
		};

	}
}
