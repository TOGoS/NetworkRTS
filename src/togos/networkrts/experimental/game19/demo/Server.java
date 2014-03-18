package togos.networkrts.experimental.game19.demo;

import java.io.File;
import java.io.IOException;

import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.sim.Simulator;
import togos.networkrts.experimental.game19.world.IDGenerator;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManIcons;

public class Server
{
	final ResourceContext resourceContext = new ResourceContext(new File(".ccouch"));
	final IDGenerator idGenerator = new IDGenerator();
	
	final JetManIcons jetManIcons = JetManIcons.load(resourceContext);
	
	public Server() throws IOException {
	}
	
	/*
	protected NonTile makePlayerJetMan( ) {
		int playerId = idGenerator.newId();
		long playerBa = BitAddresses.forceType(BitAddresses.TYPE_NONTILE, playerId);
		return JetManBehavior.createJetMan(playerBa, clientBa, jetManIcons);
	}
	*/
	
	Simulator sim;
	public void init() throws IOException {
		sim = new Simulator(DemoWorld.initWorld(resourceContext), 50);
	}
	
	public void start() {
		sim.start();
		// TODO: start listening for packets or whatever, idk
	}
}
