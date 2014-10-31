package proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.Socket;

class ProxyThread extends Thread {
	private Socket pSocket;	
	
	public ProxyThread(Socket socket) {
		pSocket = socket;
	}

	public void run() {
		try {
			BufferedInputStream fromClient = new BufferedInputStream(pSocket.getInputStream());
			BufferedOutputStream toClient = new BufferedOutputStream(pSocket.getOutputStream());

			Socket server = null;

			byte[] request = null;
			int requestLength = 0;
			int pos = -1;
			StringBuilder host = new StringBuilder("");
			String hostName = "";
			int hostPort = 80;

			request = getHTTPData(fromClient, host, false);
			requestLength = Array.getLength(request);

			hostName = host.toString();
			pos = hostName.indexOf(":");
			if (pos > 0) {
				try {
					hostPort = Integer.parseInt(hostName.substring(pos + 1));
				}  catch (Exception e)  { }
				hostName = hostName.substring(0, pos);
			}

			try {
				server = new Socket(hostName, hostPort);
			} catch (Exception e) {
				String errMsg = "HTTP/1.0 500\nContent Type: text/plain\n\nError connecting to the server:\n" + e + "\n";
				toClient.write(errMsg.getBytes(), 0, errMsg.length());
			}
			
			if (server != null) {
				BufferedInputStream toServer = new BufferedInputStream(server.getInputStream());
				BufferedOutputStream fromServer = new BufferedOutputStream(server.getOutputStream());

				fromServer.write(request, 0, requestLength);
				fromServer.flush();
				streamHTTPData(toServer, toClient, new StringBuilder(""), true);

				toServer.close();
				fromServer.close();
			}

			toClient.close();
			fromClient.close();
			pSocket.close();
		} catch (Exception e) {
			System.out.println("Error in ProxyThread: " + e);
		}
	}
	
	
	private byte[] getHTTPData(InputStream in, StringBuilder host, boolean waitForDisconnect) {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		streamHTTPData(in, bs, host, waitForDisconnect);
		return bs.toByteArray();
	}	

	private void streamHTTPData(InputStream in, OutputStream out, StringBuilder host, boolean waitForDisconnect) {
		StringBuilder header = new StringBuilder("");
		String data = "";
		int responseCode = 200;
		int contentLength = 0;
		int pos = -1;
		int byteCount = 0;

		try {
			data = readLine(in);
			if (data != null) {
				header.append(data + "\r\n");
				pos = data.indexOf(" ");
				if ((data.toLowerCase().startsWith("http")) && (pos >= 0) && (data.indexOf(" ", pos+1) >= 0)) {
					responseCode = Integer.parseInt(data.substring(pos+1, data.indexOf(" ", pos+1)));
				}
			}

			while ((data = readLine(in)) != null) {
				if (data.length() == 0)
					break;
				header.append(data + "\r\n");

				pos = data.toLowerCase().indexOf("host:");
				if (pos >= 0) {
					host.setLength(0);
					host.append(data.substring(pos + 5).trim());
				}

				pos = data.toLowerCase().indexOf("content-length:");
				if (pos >= 0)
					contentLength = Integer.parseInt(data.substring(pos + 15).trim());
			}
			
			header.append("\r\n");
			out.write(header.toString().getBytes(), 0, header.length());

			if ((responseCode != 200) && (contentLength == 0)) {
				out.flush();
			} else if (contentLength > 0) {
				waitForDisconnect = false;
			}
			
			if ((contentLength > 0) || (waitForDisconnect)) {
				try {
					byte[] buf = new byte[4096];
					int bytesIn = 0;
					while (((byteCount < contentLength) || (waitForDisconnect)) && ((bytesIn = in.read(buf)) >= 0) ) {
						out.write(buf, 0, bytesIn);
						byteCount += bytesIn;
					}
				} catch (Exception e) {
					System.out.println("Error getting HTTP body: " + e);
				}
			}
		} catch (Exception e) {
			System.out.println("Error getting HTTP data: " + e);
		}
		
		try {
			out.flush();
		} catch (Exception e) { }
	}

	private String readLine (InputStream in) {
		StringBuilder data = new StringBuilder("");
		int c;
		
		try {
			in.mark(1);
			if (in.read() == -1) {
				return null;
			} else {
				in.reset();
			}

			while ((c = in.read()) >= 0) {
				if ((c == 0) || (c == 10) || (c == 13)) {
					break;
				} else {
					data.append((char)c);
				}
			}
			
			if (c == 13) {
				in.mark(1);
				if (in.read() != 10) {
					in.reset();
				}
			}
		} catch (Exception e) {
			System.out.println("Error getting header: " + e);
		}
		return data.toString();
	}
}