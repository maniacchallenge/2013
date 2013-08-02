package de.uni_bremen.comnets.maniac.agents;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import de.uni_bremen.comnets.maniac.log.MessageTracer;
import de.uni_bremen.comnets.maniac.log.Tracer;

/**
 * A simple abstract super-class for the Agent hierarchy.
 * It features a simple message pump, which processes messages
 * from a PriorityBlockingQueue.
 *
 * The Messages themselves are derived from the abstract Message
 * class defined here. Concrete subclasses of Message are defined
 * as inner classes of the concrete Agent classes, so that we can
 * more easily implement the Command Pattern by having a closure
 * over the class's state.
 *
 * In general, Agents should be constantly doing computations in
 * the background, so that when requested to provide information,
 * they are able to do so immediately, looking up a value from a
 * cache, or providing an approximation if necessary.
 *
 * Agents should also accept messages which will cause them to
 * refocus their efforts on building cache values in a particular
 * region of values, in preparation for an impending request in
 * that area.
 *
 * Created by Isaac Supeene on 6/11/13.
 */
public abstract class Agent extends Thread {
    protected String TAG() { return "Maniac " + getClass().getSimpleName(); }

    PriorityBlockingQueue<Message> messages = new PriorityBlockingQueue<Message>(10, new MessageComparator()); // TODO: 10 initial capacity is pretty arbitrary...

    @Override
    public void run() {
        Tracer t = new Tracer(TAG());

        while (!isInterrupted())  {
        try {
            Message nextMessage = messages.poll(1, TimeUnit.SECONDS); // TODO: 1 second is pretty arbitrary...
            if (nextMessage != null) {
                nextMessage.process();
            }
        }
        catch (InterruptedException ex) {
            break;
        }}

        t.finish();
    }

    public void postMessage(Message message) {
        Tracer t = new Tracer(TAG());

        messages.put(message);

        t.finish();
    }

    /**
     * Serves as the abstract base class for all Messages
     * that can be sent to an Agent.  Each message has a
     * priority, which defaults to PRIORITY_NORMAL, and a
     * processing method, which must be implemented in the
     * derived class.
     *
     * This class provides full message tracing using the
     * MessageTracer class, completely transparently to the
     * implementing classes.
     *
     * Messages should typically be implemented as inner
     * classes of an agent, to more easily implement the
     * command pattern.
     */
    protected abstract class Message {
        public final int PRIORITY_TOP = 1;
        public final int PRIORITY_HIGH = 2;
        public final int PRIORITY_NORMAL = 3;
        public final int PRIORITY_LOW = 4;

        private MessageTracer tracer;

        protected Message(Object... state) {
            tracer = new MessageTracer(TAG(), getClass().getName(), state);
        }

        protected Message(String messageIdentifier, Object... state) {
            tracer = new MessageTracer(TAG(), messageIdentifier, state);
        }

        protected Message(String messageIdentifier, int priority, Object... state) {
            tracer = new MessageTracer(TAG(), messageIdentifier, priority, state);
        }

        public final void process() {
            tracer.beginProcessing();
            processImpl();
            tracer.finish();
        }

        protected abstract void processImpl();
        public int priority() { return PRIORITY_NORMAL; }
    }

    /**
     * Compares two Messages based on priority.  The message
     * queue uses this comparator to process higher priority
     * messages first.
     */
    private static class MessageComparator implements Comparator<Message> {
        @Override
        public int compare(Message message1, Message message2) {
            return message1.priority() - message2.priority();
        }
    }
}
