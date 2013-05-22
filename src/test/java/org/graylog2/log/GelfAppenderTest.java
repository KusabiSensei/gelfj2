package org.graylog2.log;

import org.apache.log4j.*;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.LoggingEvent;
import org.graylog2.GelfMessage;
import org.graylog2.GelfTCPSender;
import org.graylog2.GelfUDPSender;
import org.graylog2.log4j2.GelfAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

/**
 * @author Anton Yakimov
 * @author Jochen Schalanda
 */
public class GelfAppenderTest {

	private static final String CLASS_NAME = GelfAppenderTest.class.getCanonicalName();
	private TestGelfSender gelfSender;
	private GelfAppender gelfAppender;

	@Before
	public void setUp() throws IOException {
		gelfSender = new TestGelfSender("localhost");

		gelfAppender = GelfAppender.createAppender("test", "127.0.0.1", "12201", "USER", "true", "localhost", "true", "true",
				"{'environment': 'DEV', 'application': 'MyAPP'}", null, null, null);
	}

	@After
	public void tearDown() {
		if (gelfAppender.isAddExtendedInformation()) {
			NDC.clear();
		}
	}

	@Test
	public void ensureHostnameForMessage() {

//		LogEvent event = new Log4jLogEvent(CLASS_NAME, Category.getInstance(GelfAppenderTest.class), 123L, Level.INFO, "Das Auto",
//				new RuntimeException("Volkswagen"));
//		gelfAppender.append(event);

		assertThat("Message hostname", gelfSender.getLastMessage().getHost(), notNullValue());

		gelfAppender.setOriginHost("example.com");
//		gelfAppender.append(event);
		assertThat(gelfSender.getLastMessage().getHost(), is("example.com"));
	}

	@Test
	public void handleNullInAppend() {

		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Level.INFO, null, new RuntimeException("LOL"));
		//gelfAppender.append(event);

		assertThat("Message short message", gelfSender.getLastMessage().getShortMessage(), notNullValue());
		assertThat("Message full message", gelfSender.getLastMessage().getFullMessage(), notNullValue());
	}

	@Test
	public void handleMDC() {

		gelfAppender.setAddExtendedInformation(true);

		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Level.INFO, "", new RuntimeException("LOL"));
		MDC.put("foo", "bar");

//		gelfAppender.append(event);

		assertEquals("bar", gelfSender.getLastMessage().getAdditonalFields().get("foo"));
		assertNull(gelfSender.getLastMessage().getAdditonalFields().get("non-existent"));
	}

	@Test
	public void handleNDC() {

		gelfAppender.setAddExtendedInformation(true);

		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Level.INFO, "", new RuntimeException("LOL"));
		NDC.push("Foobar");

//		gelfAppender.append(event);

		assertEquals("Foobar", gelfSender.getLastMessage().getAdditonalFields().get("loggerNdc"));
	}

	@Test
	public void disableExtendedInformation() {

		gelfAppender.setAddExtendedInformation(false);

		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(this.getClass()), 123L, Level.INFO, "", new RuntimeException("LOL"));

		MDC.put("foo", "bar");
		NDC.push("Foobar");

//		gelfAppender.append(event);

		assertNull(gelfSender.getLastMessage().getAdditonalFields().get("loggerNdc"));
		assertNull(gelfSender.getLastMessage().getAdditonalFields().get("foo"));
	}

	@Test
	public void checkExtendedInformation() throws UnknownHostException, SocketException {

		gelfAppender.setAddExtendedInformation(true);

		LoggingEvent event = new LoggingEvent(CLASS_NAME, Category.getInstance(GelfAppenderTest.class), 123L, Level.INFO, "Das Auto", new RuntimeException("LOL"));

//		gelfAppender.append(event);

		assertEquals(gelfSender.getLastMessage().getAdditonalFields().get("logger"), CLASS_NAME);
	}

	private class TestGelfSender extends GelfUDPSender {

		private GelfMessage lastMessage;

		public TestGelfSender(String host) throws IOException {
			super(host);
		}

		@Override
		public boolean sendMessage(GelfMessage message) {
			this.lastMessage = message;
			return true;
		}

		public GelfMessage getLastMessage() {
			return lastMessage;
		}
	}

	private class TestingEH implements ErrorHandler {

		private String errorMessage = "";

		@Override
		public void setLogger(Logger logger) {
		}

		@Override
		public void error(String s, Exception e, int i) {
			errorMessage = s;
		}

		@Override
		public void error(String s) {
			errorMessage = s;
		}

		@Override
		public void error(String s, Exception e, int i, LoggingEvent loggingEvent) {
			errorMessage = s;
		}

		@Override
		public void setAppender(Appender appender) {
		}

		@Override
		public void setBackupAppender(Appender appender) {
		}

		@Override
		public void activateOptions() {
		}

		public String getErrorMessage() {
			return errorMessage;
		}
	}

	private class MockGelfUDPSender extends GelfUDPSender {

		private MockGelfUDPSender(String host, int port) throws IOException {
			if (host.contains("udp:")) {
				throw new UnknownHostException("udp: found in host");
			}
		}

	}

	private class MockGelfTCPSender extends GelfTCPSender {

		private MockGelfTCPSender(String host, int port) throws IOException {
			if (host.contains("tcp:")) {
				throw new UnknownHostException("tcp: found in host");
			}
		}
	}
}
