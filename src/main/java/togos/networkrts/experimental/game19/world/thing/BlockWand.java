package togos.networkrts.experimental.game19.world.thing;

import togos.networkrts.experimental.game19.sim.UpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.shape.TRectangle;
import togos.networkrts.util.BitAddressUtil;

public class BlockWand
{
	public static class Application {
		public final double x, y, radius;
		public final boolean additive;
		public final BlockStack blocks;
		
		public Application( double x, double y, double radius, boolean additive, BlockStack blocks ) {
			this.x = x; this.y = y; this.radius = radius;
			this.additive = additive; this.blocks = blocks;
		}
	}
	
	public static void apply( Application app, UpdateContext ctx ) {
		long minBa = BitAddresses.TYPE_NODE;
		long maxBa = BitAddresses.maxForType(BitAddresses.TYPE_NODE);
		
		ctx.sendMessage(
			new Message(
				minBa, maxBa, //new TCircle(app.x, app.y, app.radius),
				new TRectangle(app.x-app.radius, app.y-app.radius, app.radius*2, app.radius*2),
				app.additive ? MessageType.ADD_BLOCKS : MessageType.REPLACE_BLOCKS, BitAddressUtil.NO_ADDRESS,
				app.blocks
			)
		);
	}
}
