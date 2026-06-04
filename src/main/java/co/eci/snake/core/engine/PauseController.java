package co.eci.snake.core.engine;

import java.util.concurrent.CountDownLatch;

public class PauseController {
    private boolean paused = true;
    private CountDownLatch pausedLatch;

    public synchronized void requestPause(int numberOfSnakes) {
        paused = true;
        pausedLatch = new CountDownLatch(numberOfSnakes);
    }

    public void awaitAllPaused() throws InterruptedException {
        CountDownLatch latch;
        synchronized (this) {
            latch = pausedLatch;
        }
        if (latch != null) {
            latch.await();
        }
    }

    public synchronized void resume() {
        paused = false;
        notifyAll();
    }

    public void waitIfPaused() throws InterruptedException {
        synchronized (this) {
            if (paused && pausedLatch != null) {
                pausedLatch.countDown();
            }
            while (paused) {
                wait();
            }
        }
    }
}