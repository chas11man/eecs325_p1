package proxy;

import java.net.*;
import java.io.*;
import java.util.*;

public class ProxyThread extends Thread {
	private Socket socket = null;
	private static final int BUFFER_SIZE = 32768;
	
	public ProxyThread(Socket socket) {
		super("ProxyThread");
		this.socket = socket;
	}

	public void run() {
		try {
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			String input;
			int count = 0;
			String urlRequest = "";

			while ((input = in.readLine()) != null) {
				try {
					StringTokenizer strTok = new StringTokenizer(input);
					strTok.nextToken();
				} catch(Exception e) {
					break;
				}

				if (count == 0) {
					String[] tokens = input.split(" ");
					urlRequest = tokens[1];
				}

				count++;
			}

			BufferedReader bufRd = null;
			try {
				URL url = new URL(urlRequest);
				URLConnection conn = url.openConnection();
				
				conn.setDoInput(true);
				conn.setDoOutput(false);
				
				InputStream response = conn.getInputStream();

				bufRd = new BufferedReader(new InputStreamReader(response));
	
				byte bte[] = new byte[BUFFER_SIZE];
				int index = response.read(bte, 0, BUFFER_SIZE);
				
				while (index != -1) {
					out.write(bte, 0, index);
					index = response.read(bte, 0, BUFFER_SIZE);
				}
				out.flush();
			} catch (Exception e) {
				System.err.println(e);
				out.writeBytes("");
			}

			if (bufRd != null)
				bufRd.close();
			if (out != null)
				out.close();
			if (in != null)
				in.close();
			if (socket != null)
				socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}