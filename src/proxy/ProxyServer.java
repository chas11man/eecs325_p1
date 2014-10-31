package proxy;

import java.io.*;
import java.net.*;

public class ProxyServer {
	public static void main(String[] args)  throws IOException {
		ServerSocket ss = null;
		boolean listening = true;
		int port = 5213;

		try {
			ss = new ServerSocket(port);
		} catch (Exception e) {
			System.err.println(e + "\nCould not listen on port: " + port);
			System.exit(-1);
		}

		while(listening) {
			new ProxyThread(ss.accept()).start();
		}
		ss.close();
	}
}