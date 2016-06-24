package org.graylog2;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class GelfUDPSender implements GelfSender {
	private InetAddress host;
	private int port;
	private DatagramChannel channel;

    public GelfUDPSender() {
    }

    public GelfUDPSender(String host) throws IOException {
		this(host, DEFAULT_PORT);
	}

	public GelfUDPSender(String host, int port) throws IOException {
		this.host = InetAddress.getByName(host);
		this.port = port;
		this.channel = initiateChannel();
	}

	private DatagramChannel initiateChannel() throws IOException {
		DatagramChannel resultingChannel = DatagramChannel.open();
		resultingChannel.socket().bind(new InetSocketAddress(0));
		resultingChannel.connect(new InetSocketAddress(this.host, this.port));
		resultingChannel.configureBlocking(false);

		return resultingChannel;
	}

	public GelfSenderResult sendMessage(GelfMessage message) {
		if (!message.isValid()) {
			return GelfSenderResult.MESSAGE_NOT_VALID;
		}
		return sendDatagrams(message.toBuffers());
	}

	private GelfSenderResult sendDatagrams(ByteBuffer[] bytesList) {
		try {
			for (ByteBuffer buffer : bytesList) {
				channel.write(buffer);
			}
		} catch (IOException e) {
			return new GelfSenderResult(GelfSenderResult.ERROR_CODE, e);
		}

		return GelfSenderResult.OK;
	}

	public void close() {
		try {
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
