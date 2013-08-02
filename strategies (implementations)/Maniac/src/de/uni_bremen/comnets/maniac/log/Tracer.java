package de.uni_bremen.comnets.maniac.log;

import android.util.Log;

import de.uni_bremen.comnets.maniac.util.Reflection;

/**
 * Provides tracing utilities using the standard android Log.
 * A Tracer object outputs a start message on construction, and a
 * finish message including the elapsed time after finish() is called.
 *
 * Created by Isaac Supeene on 6/11/13.
 */
public class Tracer {
    public static final int DEFAULT_LOG_LEVEL = Log.VERBOSE;
    public static final int MINIMUM_LOG_LEVEL = Log.VERBOSE;
    protected static final String PRIVATE_TAG = "Maniac Tracer";

    protected long startTime;
    protected String tag;
    protected String action;
    protected int logLevel;

    /**
     * Allows the Tracer to store and log some state, if it is provided.
     */
    protected ObjectPacket state;

    /**
     * An action should only be finished once. We keep track of this,
     * and log an error if it is called multiple times.
     */
    protected boolean finished = false;

    /**
     * If the provided log level is below our minimum log level, we
     * will simply do nothing, to avoid a lot of useless operations.
     */
    protected boolean ignore = false;

    /**
     * Creates a new Tracer with the specified tag and action
     * and the default log level, and logs the beginning of the
     * traced action.
     *
     * @param tag
     * @param state Optional state information to be logged at the beginning and end of the action.
     */
    public Tracer(String tag, Object... state) {
        this(tag, Reflection.getPreviousMethodName(1), state);
    }

    /**
     * Creates a new Tracer with the specified tag and action
     * and the default log level, and logs the beginning of the
     * traced action.
     *
     * @param tag
     * @param action
     * @param state Optional state information to be logged at the beginning and end of the action.
     */
    public Tracer(String tag, String action, Object... state) {
        this(tag, action, DEFAULT_LOG_LEVEL, state);
    }

    /**
     * Creates a new Tracer with the specified tag, action
     * and log level, and logs the beginning of the traced action.
     *
     * @param tag
     * @param action
     * @param logLevel
     * @param state Optional state information to be logged at the beginning and end of the action.
     */
    public Tracer(String tag, String action, int logLevel, Object... state) {
        if (logLevel < MINIMUM_LOG_LEVEL) { // See android.util.Log - lower log levels are more verbose.
            ignore = true;
            return;
        }

        this.tag = tag;
        this.action = action;
        this.logLevel = logLevel;
        this.startTime = System.currentTimeMillis();

        Log.println(logLevel, tag, String.format(getBeginActionMessage(), action));

        this.state = new ObjectPacket(state);
        if (!this.state.empty()) {
            Log.println(logLevel, tag, String.format(getInitialStateMessage(), action, this.state));
        }
    }

    /**
     * Logs the end of the traced action, if this method has not
     * been called previously.  Otherwise, logs an error message.
     */
    public void finish() {
        if (ignore) {
            return;
        }

        if (finished) {
            Log.e(PRIVATE_TAG, getInvalidFinishMessage());
        }
        else {
            finished = true;
            long elapsedTime = System.currentTimeMillis() - startTime;
            Log.println(logLevel, tag, String.format(getEndActionMessage(), action, elapsedTime));

            if (!state.empty()) {
                Log.println(logLevel, tag, String.format(getFinalStateMessage(), action, this.state));
            }
        }
    }

    /**
     * If this object is being disposed of, but the finish() method
     * has not been called, log an error message.
     */
    @Override
    protected void finalize() throws Throwable {
        // The ignore variable is intentionally left out of the below expression, because
        // failing to call finish on a traced action might be indicative of a FoC bug,
        // regardless of this Tracer's log level.
        if (!finished) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            Log.e(PRIVATE_TAG, String.format(getInvalidFinalizeMessage(), tag, action, elapsedTime));
        }

        super.finalize();
    }

    /**
     * Convenience class for converting an array of objects to a string.
     */
    protected class ObjectPacket {
        Object[] contents;

        public ObjectPacket(Object... items) {
            contents = items;
        }

        public boolean empty() {
            return contents == null || contents.length == 0;
        }

        @Override
        public String toString() {
            if (empty()) {
                return "null ObjectPacket";
            }

            StringBuilder builder = new StringBuilder(contents[0].toString());
            for (int i = 1; i < contents.length; ++i) {
                builder.append(", ").append(contents[i]);
            }

            return builder.toString();
        }
    }

    // These messages are returned by methods so that
    // derived classes can override them, if they so choose.

    protected String getBeginActionMessage() {
        return "Beginning action '%s'";
    }

    protected String getInitialStateMessage() {
        return "State before action '%s': %s";
    }

    protected String getEndActionMessage() {
        return "Completed action '%s'. Elapsed time: %d ms";
    }

    protected String getFinalStateMessage() {
        return "State after action '%s': %s";
    }

    protected String getInvalidFinishMessage() {
        return "Attempted to finish a tracing event that had already been finished!";
    }

    protected String getInvalidFinalizeMessage() {
        return "Tracer.finish was not called before the object was disposed of.  " +
               "Tag: '%s', Action: '%s', Elapsed time: %d ms";
    }
}