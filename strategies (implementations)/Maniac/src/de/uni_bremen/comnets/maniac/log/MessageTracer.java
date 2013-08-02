package de.uni_bremen.comnets.maniac.log;

import android.util.Log;

/**
 * A custom tracer designed specifically for Messages.  Logs
 * when the messages is created, when processing begins, and
 * when processing is completed.
 *
 * Created by Isaac Supeene on 6/11/13.
 */
public class MessageTracer extends Tracer {

    protected boolean processing = false;

    public MessageTracer(String tag, String action, Object... state) {
        super(tag, action, state);
    }

    public MessageTracer(String tag, String action, int logLevel, Object... state) {
        super(tag, action, logLevel, state);
    }

    public void beginProcessing() {
        if (ignore) {
            return;
        }

        if (finished) {
            Log.e(PRIVATE_TAG, getInvalidFinishMessage());
            return;
        }
        if (processing) {
            Log.e(PRIVATE_TAG, getInvalidBeginProcessingMessage());
            return;
        }

        processing = true;
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.println(logLevel, tag, String.format(getBeginProcessingMessage(), action, elapsedTime));
        startTime = System.currentTimeMillis();

        if (!state.empty()) {
            Log.println(logLevel, tag, String.format(getProcessingStateMessage(), action, state));
        }
    }

    protected String getBeginProcessingMessage() {
        return "Processing message '%s'. Delay before processing: %d ms";
    }

    protected String getProcessingStateMessage() {
        return "State at the beginning of processing message '%s': %s";
    }

    protected String getInvalidBeginProcessingMessage() {
        return "Attempted to begin processing a Message that is already being processed!";
    }

    @Override
    protected String getBeginActionMessage() {
        return "Posting message '%s'";
    }

    @Override
    protected String getInitialStateMessage() {
        return "State on posting message '%s': %s";
    }

    @Override
    protected String getEndActionMessage() {
        return "Completed processing message '%s'. Elapsed time: %d ms";
    }

    @Override
    protected String getFinalStateMessage() {
        return "State after processing message '%s': %s";
    }

    @Override
    protected String getInvalidFinalizeMessage() {
        if (processing) {
            return "Message was disposed of while being processed!  " +
                   "Tag: '%s', Action: '%s', Elapsed time: %d ms";
        }
        else {
            return "Message was disposed of without being processed!  " +
                    "Tag: '%s', Action: '%s', Elapsed time: %d ms";
        }
    }
}
