package de.uni_bremen.comnets.maniac.log;

import android.util.Log;

import de.uni_bremen.comnets.maniac.util.Reflection;

/**
 * Extends the capabilities of the Tracer to log the result of an operation.
 *
 * Created by Isaac Supeene on 6/11/13.
 */
public class ResultTracer<T> extends Tracer {

    public ResultTracer(String tag, Object... state) {
        super(tag, Reflection.getPreviousMethodName(1), state);
    }

    public ResultTracer(String tag, String action, Object... state) {
        super(tag, action, state);
    }

    public ResultTracer(String tag, String action, int logLevel, Object... state) {
        super(tag, action, logLevel, state);
    }

    /**
     *
     * @param result
     * @return result
     */
    public T finish(T result) {
        if (ignore || finished) {
            return result;
        }

        super.finish();
        Log.println(logLevel, tag, String.format(getResultMessage(), action, result));

        return result;
    }

    /**
     * Calling the no-parameter finish method will log a null result.
     */
    @Override
    public void finish() {
        finish(null);
    }

    protected String getResultMessage() {
        return "Result of action '%s': %s";
    }
}
