/**
 * 
 */
package org.graylog2;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


/**
 * @author KusabiSensei
 *
 */
public class GelfAMQPSender implements GelfSender {

	private volatile boolean shutdown = false;
	
	private final ConnectionFactory factory;
	private Connection connection;
	private Channel channel;
	
	private final String exchangeName;
	private final String routingKey;
	private final int maxRetries;
	private final String channelMutex = "channelMutex";
	
	/**
	 * @throws URISyntaxException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	public GelfAMQPSender(String uri, String exchangeName, String routingKey, int maxRetries) throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException {
		factory = new ConnectionFactory();
		factory.setUri(uri);
		this.exchangeName = exchangeName;
		this.routingKey = routingKey;
		this.maxRetries = maxRetries;
	}

	/* (non-Javadoc)
	 * @see org.graylog2.GelfSender#sendMessage(org.graylog2.GelfMessage)
	 */
	@Override
	public GelfSenderResult sendMessage(GelfMessage message) {
		if (shutdown || !message.isValid()) {
			return GelfSenderResult.MESSAGE_NOT_VALID_OR_SHUTTING_DOWN;
		}
		
		String uuid = UUID.randomUUID().toString();
		String messageid = "gelf"+message.getHost()+message.getFacility()+message.getTimestamp()+uuid;
		
		int tries = 0;
		Exception lastException = null;
		do {
			try {
				//Establish a connection to the broker
				if (channel == null) {
					synchronized(channelMutex) { //Do this only one thread at a time
						if (channel == null) {
							connection = factory.newConnection();
							channel = connection.createChannel();
							channel.confirmSelect();
						}
					}
				}
				
				BasicProperties.Builder propertiesBuilder = new BasicProperties.Builder();
				propertiesBuilder.contentType("application/json; charset=utf-8"); //Tell RabbitMQ that we are using UTF-8 in our JSON
				propertiesBuilder.contentEncoding("gzip");
				propertiesBuilder.messageId(messageid);
				propertiesBuilder.timestamp(new Date(message.getTimestamp()));
				BasicProperties properties = propertiesBuilder.build();
				
				channel.basicPublish(exchangeName, routingKey, properties, message.toBuffer().array());
				channel.waitForConfirms();
				
				return GelfSenderResult.OK;
				
			} catch (Exception e) {
				channel = null;
				tries++;
				lastException = e;
			}
		} while (tries <= maxRetries || maxRetries < 0);
		
		return new GelfSenderResult(GelfSenderResult.ERROR_CODE, lastException);
	}

	/* (non-Javadoc)
	 * @see org.graylog2.GelfSender#close()
	 */
	@Override
	public void close() {
		shutdown = true;
		try {
			channel.close();
		} catch (Exception e) {
		}
		try {
			connection.close();
		} catch (Exception e) {
		}
	}
}
