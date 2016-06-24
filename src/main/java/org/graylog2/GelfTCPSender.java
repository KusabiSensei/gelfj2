package org.graylog2;

import java.io.IOException;
import java.net.*;

public class GelfTCPSender implements GelfSender {
	private boolean shutdown = false;
	private InetAddress host;
	private int port;
	private Socket socket;

    public GelfTCPSender() {
    }

	public GelfTCPSender(String host, int port) throws IOException {
		this.host = InetAddress.getByName(host);
		this.port = port;
		this.socket = new Socket(host, port);
	}

	public GelfSenderResult sendMessage(GelfMessage message) {
		if (shutdown || !message.isValid()) {
			return GelfSenderResult.MESSAGE_NOT_VALID_OR_SHUTTING_DOWN;
		}

		try {
			// reconnect if necessary
			if (socket == null) {
				socket = new Socket(host, port);
			}

			socket.getOutputStream().write(message.toBuffer().array());

			return GelfSenderResult.OK;
		} catch (IOException e) {
			// if an error occours, signal failure
			socket = null;
			return new GelfSenderResult(GelfSenderResult.ERROR_CODE, e);
		}
	}

	public void close() {
		shutdown = true;
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
