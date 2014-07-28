package biz.paluch.logging.gelf.jboss7;

import static biz.paluch.logging.gelf.jboss7.JBoss7LogTestUtil.getJBoss7GelfLogHandler;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jboss.logmanager.MDC;
import org.junit.Before;
import org.junit.Test;

import biz.paluch.logging.gelf.GelfTestSender;
import biz.paluch.logging.gelf.intern.GelfMessage;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 27.09.13 08:36
 */
public class JBoss7GelfLogHandlerTest {

    public static final String LOG_MESSAGE = "foo bar test log message";
    public static final String EXPECTED_LOG_MESSAGE = LOG_MESSAGE;

    @Before
    public void before() throws Exception {
        GelfTestSender.getMessages().clear();
        LogManager.getLogManager().reset();
        MDC.remove("mdcField1");
    }

    @Test
    public void testSimple() throws Exception {

        JBoss7GelfLogHandler handler = getJBoss7GelfLogHandler();

        Logger logger = Logger.getLogger(getClass().getName());
        logger.addHandler(handler);

        logger.info(LOG_MESSAGE);
        assertEquals(1, GelfTestSender.getMessages().size());

        GelfMessage gelfMessage = GelfTestSender.getMessages().get(0);

        assertEquals(EXPECTED_LOG_MESSAGE, gelfMessage.getFullMessage());
        assertEquals(EXPECTED_LOG_MESSAGE, gelfMessage.getShortMessage());
        assertEquals("6", gelfMessage.getLevel());
        assertEquals(8192, gelfMessage.getMaximumMessageSize());

    }

    @Test
    public void testSimpleWithMsgFormatSubstitution() throws Exception {

        JBoss7GelfLogHandler handler = getJBoss7GelfLogHandler();

        Logger logger = Logger.getLogger(getClass().getName());
        logger.addHandler(handler);

        String logMessage = "foo bar test log message {0}";
        String expectedMessage = "foo bar test log message aaa";
        logger.log(Level.INFO, logMessage, new String[] { "aaa" });
        assertEquals(1, GelfTestSender.getMessages().size());

        GelfMessage gelfMessage = GelfTestSender.getMessages().get(0);

        assertEquals(expectedMessage, gelfMessage.getFullMessage());
        assertEquals(expectedMessage, gelfMessage.getShortMessage());
        assertEquals("6", gelfMessage.getLevel());
        assertEquals(8192, gelfMessage.getMaximumMessageSize());

    }

    @Test
    public void testSimpleWithStringFormatSubstitution() throws Exception {

        JBoss7GelfLogHandler handler = getJBoss7GelfLogHandler();

        Logger logger = Logger.getLogger(getClass().getName());
        logger.addHandler(handler);

        String logMessage = "foo bar test log message %s";
        String expectedMessage = "foo bar test log message aaa";

        logger.log(Level.INFO, logMessage, new String[] { "aaa" });
        assertEquals(1, GelfTestSender.getMessages().size());

        GelfMessage gelfMessage = GelfTestSender.getMessages().get(0);

        assertEquals(expectedMessage, gelfMessage.getFullMessage());
        assertEquals(expectedMessage, gelfMessage.getShortMessage());

    }

    @Test
    public void testFields() throws Exception {

        JBoss7GelfLogHandler handler = getJBoss7GelfLogHandler();

        Logger logger = Logger.getLogger(getClass().getName());
        logger.addHandler(handler);

        MDC.put("mdcField1", "a value");

        logger.info(LOG_MESSAGE);
        assertEquals(1, GelfTestSender.getMessages().size());

        GelfMessage gelfMessage = GelfTestSender.getMessages().get(0);

        assertEquals("fieldValue1", gelfMessage.getField("fieldName1"));
        assertEquals("fieldValue2", gelfMessage.getField("fieldName2"));
        assertEquals("a value", gelfMessage.getField("mdcField1"));
        assertNull(gelfMessage.getField("mdcField2"));

    }

}
