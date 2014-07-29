package biz.paluch.logging.gelf.jul;

import biz.paluch.logging.gelf.GelfUtil;
import biz.paluch.logging.gelf.LogEvent;
import biz.paluch.logging.gelf.LogMessageField;
import biz.paluch.logging.gelf.MessageField;
import biz.paluch.logging.gelf.Values;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.09.13 15:22
 */
public class JulLogEvent implements LogEvent {

    private static Map<String, String> threadNameCache = new ConcurrentHashMap<String, String>();

    private LogRecord logRecord;

    public JulLogEvent(LogRecord logRecord) {
        this.logRecord = logRecord;
    }

    @Override
    public String getMessage() {
        return createMessage(logRecord);
    }

    @Override
    public Object[] getParameters() {
        return logRecord.getParameters();
    }

    @Override
    public Throwable getThrowable() {
        return logRecord.getThrown();
    }

    @Override
    public long getLogTimestamp() {
        return logRecord.getMillis();
    }

    @Override
    public String getSyslogLevel() {
        return "" + levelToSyslogLevel(logRecord.getLevel());
    }

    private String createMessage(LogRecord record) {
        String message = record.getMessage();
        Object[] parameters = record.getParameters();

        if (message == null) {
            message = "";
        }
        if (parameters != null && parameters.length > 0) {
            if (record.getResourceBundle() != null && record.getResourceBundle().containsKey(record.getMessage())) {
                message = record.getResourceBundle().getString(record.getMessage());
            }
            String originalMessage = message;

            // by default, using {0}, {1}, etc. -> MessageFormat

            try {
                message = MessageFormat.format(message, parameters);
            } catch (IllegalArgumentException e) {
                // leaving message as it is to avoid compatibility problems
                message = record.getMessage();
            } catch (NullPointerException e) {
                // ignore
            }

            if (message.equals(originalMessage)) {
                // if the text is the same, assuming this is String.format type log (%s, %d, etc.)
                try {
                    message = String.format(message, parameters);
                } catch (IllegalFormatException e) {
                    // leaving message as it is to avoid compatibility problems
                    message = record.getMessage();
                } catch (NullPointerException e) {
                    // ignore
                }
            }
        }
        return message;
    }

    private String getThreadName(LogRecord record) {

        String cacheKey = "" + record.getThreadID();
        if (threadNameCache.containsKey(cacheKey)) {
            return threadNameCache.get(cacheKey);
        }

        long threadId = record.getThreadID();
        String threadName = "" + record.getThreadID();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        for (long id : threadMXBean.getAllThreadIds()) {
            if (id == threadId) {
                ThreadInfo threadInfo = threadMXBean.getThreadInfo(id);
                if (threadInfo != null) {
                    threadName = threadInfo.getThreadName();
                }
                break;
            }
        }

        threadNameCache.put(cacheKey, threadName);

        return threadName;
    }

    private int levelToSyslogLevel(final Level level) {
        final int syslogLevel;
        if (level == Level.SEVERE) {
            syslogLevel = 3;
        } else if (level == Level.WARNING) {
            syslogLevel = 4;
        } else if (level == Level.INFO) {
            syslogLevel = 6;
        } else {
            syslogLevel = 7;
        }
        return syslogLevel;
    }

    public Values getValues(MessageField field) {
        if (field instanceof LogMessageField) {
            return new Values(field.getName(), getValue((LogMessageField) field));
        }

        throw new UnsupportedOperationException("Cannot provide value for " + field);
    }

    public String getValue(LogMessageField field) {

        switch (field.getNamedLogField()) {
            case Severity:
                return logRecord.getLevel().getName();
            case ThreadName:
                return getThreadName(logRecord);
            case SourceClassName:
                return logRecord.getSourceClassName();
            case SourceMethodName:
                return logRecord.getSourceMethodName();
            case SourceSimpleClassName:
                return GelfUtil.getSimpleClassName(logRecord.getSourceClassName());
            case LoggerName:
                return logRecord.getLoggerName();
        }

        throw new UnsupportedOperationException("Cannot provide value for " + field);
    }

    @Override
    public String getMdcValue(String mdcName) {
        return null;
    }

    @Override
    public Set<String> getMdcNames() {
        return Collections.EMPTY_SET;
    }
}
