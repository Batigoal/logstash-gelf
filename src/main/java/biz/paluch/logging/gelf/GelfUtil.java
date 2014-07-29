package biz.paluch.logging.gelf;

import biz.paluch.logging.gelf.intern.GelfMessage;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.09.13 10:47
 */
public class GelfUtil {

    /**
     * Profiling Start in MDC (msec).
     */
    public static final String MDC_REQUEST_START_MS = "profiling.requestStart.millis";
    /**
     * Profiling End in MDC.
     */
    public static final String MDC_REQUEST_END = "profiling.requestEnd";

    /**
     * Profiling Duration in MDC.
     */
    public static final String MDC_REQUEST_DURATION = "profiling.requestDuration";

    private GelfUtil() {

    }

    public static void addMdcProfiling(LogEvent logEvent, GelfMessage gelfMessage) {
        Object requestStartMs = logEvent.getMdcValue(MDC_REQUEST_START_MS);
        long timestamp = -1;

        if (requestStartMs instanceof Long) {
            timestamp = ((Long) requestStartMs).longValue();
        }

        if (timestamp == -1 && requestStartMs instanceof String) {
            String requestStartMsString = (String) requestStartMs;
            if (requestStartMsString.length() == 0) {
                return;
            }
            timestamp = Long.parseLong(requestStartMsString);
        } else {
            return;
        }

        if (timestamp > 0) {
            long now = System.currentTimeMillis();
            long duration = now - timestamp;

            String durationText;

            if (duration > 10000) {
                duration = duration / 1000;
                durationText = duration + "sec";
            } else {
                durationText = duration + "ms";
            }
            gelfMessage.addField(MDC_REQUEST_DURATION, durationText);
            gelfMessage.addField(MDC_REQUEST_END, new Date(now).toString());
        }
    }

    public static String getSimpleClassName(String className) {

        if (className == null) {
            return null;
        }

        int index = className.lastIndexOf('.');
        if (index != -1) {
            return className.substring(index + 1);
        }
        return className;
    }

    public static Set<String> getMatchingMdcNames(DynamicMdcMessageField field, Set<String> mdcNames) {
        Set<String> matchingMdcNames = new HashSet<String>();

        for (String mdcName : mdcNames) {
            if (field.getPattern().matcher(mdcName).matches()) {

                matchingMdcNames.add(mdcName);

            }
        }
        return matchingMdcNames;
    }
}
