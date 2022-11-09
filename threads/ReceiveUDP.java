package threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ReceiveUDP extends Thread {

    private DatagramSocket socket;
    private byte[] resource = new byte[1024];
    private String receivedData;
    private DatagramPacket packet;

    public ReceiveUDP(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }
    
    public void run(){        
        while (true) {
			try {
				packet = new DatagramPacket(resource, resource.length);
				socket.setSoTimeout(500);
				socket.receive(packet);
			
				receivedData = new String(packet.getData(), 0, packet.getLength());
                System.out.println("received ==> " + receivedData);
			} catch (IOException e) {
				// System.out.println(e);
			}
        }
    }

    public String getDatag() {
        return receivedData;
    }
}
