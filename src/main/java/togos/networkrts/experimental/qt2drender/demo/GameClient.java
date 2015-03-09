package togos.networkrts.experimental.qt2drender.demo;

import java.awt.Dimension;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import javax.swing.JFrame;

import togos.networkrts.experimental.qt2drender.CrappyCodec;
import togos.networkrts.experimental.qt2drender.VizState;
import togos.networkrts.experimental.qt2drender.demo.NetRenderDemo.RenderContext;
import togos.networkrts.experimental.qt2drender.demo.NetRenderDemo.VizStateCanvas;
import togos.networkrts.experimental.rocopro.RCMessage;
import togos.networkrts.experimental.rocopro.RCMessage.MessageType;
import togos.networkrts.repo.BitprintFileRepository;

public class GameClient
{
	interface StateListener<T> {
		public void update(T v);
	}
	
	public VizStateCanvas canv;
	
	public void setVizState(VizState vs) {
		if( canv != null ) canv.setState(vs);
	}
	
	public void messageReceived( RCMessage m ) {
		switch( m.messageType ) {
		case INFO:
			if( "/visual".equals(m.resourceName) ) {
				System.err.println("Client: Setting vizstate!");
				setVizState( (VizState)m.payload );
			} else {
				System.err.println("Client: Don't know what to do with INFO "+m.resourceName);
			}
			break;
		default:
			System.err.println("Client: Don't know what to do with "+m.messageType);
		}
	}
	
	public static void main(String[] args)
		throws Exception
	{
		BitprintFileRepository br = new BitprintFileRepository(new File(".ccouch"));
		
		final JFrame f = new JFrame("NetRenderDemo");
		final VizStateCanvas vsc = new VizStateCanvas(new RenderContext(br.toBlobGetter()));
		vsc.setPreferredSize(new Dimension(800,600));
		f.add(vsc);
		f.pack();
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		
		GameClient gc = new GameClient();
		gc.canv = vsc;
		DatagramSocket sock = new DatagramSocket(55668);
		DatagramPacket pack = new DatagramPacket(new byte[2048], 2048);
		
		RCMessage osm = new RCMessage(10, 0, MessageType.OPEN_SUBSCRIPTION, "/visual", null, false);
		pack.setLength( CrappyCodec.encode(osm, pack.getData(), 0, pack.getData().length) );
		pack.setSocketAddress(new InetSocketAddress("localhost", 55667));
		System.err.println("Sender: Sending "+pack.getLength()+"-byte packet");
		sock.send( pack );
		
		while( true ) {
			pack.setLength(pack.getData().length);
			sock.receive(pack);
			System.err.println("Client: got a "+pack.getLength()+"-byte packet from "+pack.getSocketAddress());
			try {
				RCMessage rcm = CrappyCodec.decode(pack.getData(), 0, pack.getLength(), RCMessage.class);
				gc.messageReceived(rcm);
			} catch( ClassCastException e ) {
				e.printStackTrace();
			} catch( ClassNotFoundException e ) {
				e.printStackTrace();
			}
		}
	}
}
