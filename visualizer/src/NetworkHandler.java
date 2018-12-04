import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NetworkHandler extends Thread {

	public InetSocketAddress address;

	public NetworkHandler(InetSocketAddress address) {
		this.address = address;
	}
	public Socket socket = new Socket();
	public OutputStream output;

	public void run() {
		while(true) {
			try {
				socket = new Socket();
				socket.setSoTimeout(1000);
				socket.connect(address, 1000);
				output = socket.getOutputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				while(true) {
					String line = reader.readLine();
					if(line == null) {
						socket.close();
						break;
					}
					try {
						String[] splitted = line.split(" ", 2);
						Monitor m = QuaternionVisualizer.getMonitor(splitted[0]);
						if(m != null) {
							m.onData(splitted[1]);
						}
					} catch(RuntimeException e) {
						e.printStackTrace();
					}

				}
			} catch(ConnectException | SocketTimeoutException e) {
				System.err.println(e);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void sendPacket(int type, byte[] data) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(8 + data.length);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.putInt(type);
			buffer.putInt(data.length);
			buffer.put(data);
			output.write(buffer.array());
		} catch(Exception e) {
		}
	}

	public void sendDouble(int type, double d) {
		sendPacket(type, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(d).array());
	}

}
