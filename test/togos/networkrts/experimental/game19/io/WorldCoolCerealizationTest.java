package togos.networkrts.experimental.game19.io;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.demo.DemoWorld;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.util.HasURI;

public class WorldCoolCerealizationTest extends TestCase
{
	ResourceContext rc = new ResourceContext(new File(".ccouch"));
	
	World createWorld() throws IOException {
		return DemoWorld.initLittleWorld(rc);
	}
	
	public void testEncodeDecode() throws Exception {
		HasURI worldRef;
		{
			CerealWorldIO worldIo = new CerealWorldIO(rc.getByteArrayRepository());
			
			World w = createWorld();
			
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
