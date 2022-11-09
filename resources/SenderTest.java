import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SenderTest {
	public static void main(String[] args) throws SocketException, UnknownHostException {
		// DatagramSocket socket = new DatagramSocket(9000);
		DatagramSocket socket = new DatagramSocket();
		

		while (true) {
			try {
				String data = "Vejaaaa";
				byte[] resource = data.getBytes();
				DatagramPacket packet = new DatagramPacket(resource, resource.length, InetAddress.getByName("10.132.240.76"), 12532);
				socket.send(packet);

				System.out.println(resource);
				socket.close();

				// String receivedData = new String(packet.getData(), 0, packet.getLength());
				// System.out.println(receivedData);
			} catch (IOException e) {
				// System.out.println(e);
				socket.close();
			}
		}
	}
}
