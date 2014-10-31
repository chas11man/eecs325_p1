package proxy;

import java.io.IOException;
import java.net.ServerSocket;

public class ProxyServer extends Thread {
	public static void main(String[] args) throws IOException {
		ServerSocket ss = null;
		int port = 5213;
		
		try {
			ss = new ServerSocket(port);
		} catch (Exception e) {
			System.err.println(e + "\nCould not listen on port: " + port);
			System.exit(-1);
		}
			
		while (true) {
			new ProxyThread(ss.accept()).start();
		}
	}
}