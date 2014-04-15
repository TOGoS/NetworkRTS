package togos.networkrts.experimental.game19.demo;

import java.io.File;

import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.io.CerealWorldIO;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.util.HasURI;

public class WorldSaveDemo
{
	public static void main(String[] args) throws Exception {
		ResourceContext rc = new ResourceContext(new File(".ccouch"));
		CerealWorldIO worldIo = new CerealWorldIO(rc.getByteArrayRepository());
		World w = DemoWorld.initWorld(rc);
		HasURI uri = worldIo.storeObject(w);
		System.err.println("World URI: "+uri);
	}
}
