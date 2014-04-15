package togos.networkrts.experimental.game19.io;

import java.io.File;

import junit.framework.TestCase;
import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.demo.DemoWorld;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.util.HasURI;

public class WorldCoolCerealizationTest extends TestCase
{
	public void testEncodeDecode() throws Exception {
		ResourceContext rc = new ResourceContext(new File(".ccouch"));
		
		HasURI worldRef;
		{
			CerealWorldIO worldIo = new CerealWorldIO(rc.getByteArrayRepository());
			
			// TODO: build a simpler world so this test doesn't take so long
			World w = DemoWorld.initWorld(rc);
			
			worldRef = worldIo.storeObject(w);
			assertNotNull(worldRef);
		}
		
		{
			// Now let's try to load it again!
			CerealWorldIO worldLoader = new CerealWorldIO(rc.getByteArrayRepository());
			World w = worldLoader.getObject(worldRef, World.class);
			assertNotNull(w);
		}
	}
}
