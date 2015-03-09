package togos.networkrts.experimental.qt2drender.demo;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

import togos.networkrts.experimental.qt2drender.CrappyCodec;
import togos.networkrts.experimental.qt2drender.VizState;
import togos.networkrts.experimental.rocopro.RCMessage;
import togos.networkrts.experimental.rocopro.RCMessage.MessageType;
import togos.networkrts.repo.BitprintFileRepository;
import togos.networkrts.util.ResourceNotFound;

public class GameServer
{
	DatagramSocket sock;
	DatagramPacket pack = new DatagramPacket(new byte[2048], 2048);
	SocketAddress clientAddress;
	
	BitprintFileRepository blobRepo = new BitprintFileRepository(new File(".ccouch"));
	
	public VizState getVizState() throws ResourceNotFound {
		try {
			return NetRenderDemo.makeVizState(blobRepo, 0);
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void packetReceived( DatagramPacket pack ) throws IOException {
		try {
			RCMessage mess = CrappyCodec.decode(pack.getData(), 0, pack.getLength(), RCMessage.class);
			switch( mess.messageType ) {
			case OPEN_SUBSCRIPTION:
				clientAddress = pack.getSocketAddress();
				System.err.println("Client's at "+clientAddress);
				
				VizState vs;
				try {
					vs = getVizState();
				} catch( ResourceNotFound e ) {
					System.err.println("Failed to make viz state");
					e.printStackTrace();
					return;
				}
				
				RCMessage response = new RCMessage(
					mess.channelId, mess.order+1, MessageType.INFO, "/visual",
					vs, false
				);
				int z = CrappyCodec.encode(response, pack.getData());
				if( z >= 0 ) {
					pack.setLength(z);
					assert sock != null;
					assert pack != null;
					System.err.println("Sender: Sending "+pack.getLength()+"-byte packet");
					sock.send(pack);
				} else {
					System.err.println("Couldn't fit the VizState in a packet!");
				}
				
				break;
			default:
				System.err.println("Server: Ignoring "+mess.messageType);
			}
		} catch( ClassCastException e ) {
			e.printStackTrace();
		} catch( ClassNotFoundException e ) {
			e.printStackTrace();
		}
	}
	
	public void run() throws Exception {
		sock = new DatagramSocket(55667);
		while(true) {
			pack.setLength(pack.getData().length);
			sock.receive(pack);
			System.err.println("Server: received "+pack.getLength()+"-length packet");
			packetReceived(pack);
		}
	}
	
	public static void main(String[] args) throws Exception {
		new GameServer().run();
	}
}
