package de.uni_bremen.comnets.maniac.util;

/**
 * Created by Isaac Supeene on 7/4/13.
 */
public class DelayableTimerTask extends Thread {
    private int delay;
    private Runnable runnable;
    private boolean stop = false;

    public DelayableTimerTask(int delayInMillis, Runnable runnable) {
        delay = delayInMillis;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                sleep(delay);
                runnable.run();
            }
            catch (InterruptedException ex) {
                continue;
            }
            break;
        }
    }

    public void executeImmediately() {
        if (!isAlive()) {
            throw new IllegalStateException("Tried to execute a timer task that was already complete.");
        }

        stop = true;
        interrupt();
        runnable.run();
    }

    public void cancel() {
        if (!isAlive()) {
            throw new IllegalStateException("Tried to cancel a timer task that was already complete.");
        }

        stop = true;
        interrupt();
    }

    public void resetTimer() {
        if (!isAlive()) {
            throw new IllegalStateException("Tried to reset a timer task that was already complete.");
        }

        interrupt();
    }
}