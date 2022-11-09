import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ReceiveUDPTest {
    public static void main(String[] args) throws SocketException, UnknownHostException {
        DatagramSocket socket = new DatagramSocket(12559, InetAddress.getByName("192.168.68.105"));
        byte[] resource = new byte[1024];

        while (true) {
			try {
				DatagramPacket packet = new DatagramPacket(resource, resource.length);
				socket.setSoTimeout(500);
				socket.receive(packet);
			
				String receivedData = new String(packet.getData(), 0, packet.getLength());
                System.out.println(receivedData);
			} catch (IOException e) {
				// System.out.println(e);
			}
        }
    }
}
