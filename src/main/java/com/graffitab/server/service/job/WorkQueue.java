package com.graffitab.server.service.job;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;

class WorkQueue {

    private static Logger log = LogManager.getLogger();

    private final int nThreads;
    private final PoolWorker[] threads;
    private final LinkedList<Runnable> queue;

    public WorkQueue(int nThreads) {
        this.nThreads = nThreads;
        this.queue = new LinkedList<>();
        this.threads = new PoolWorker[nThreads];

        for (int i = 0; i < nThreads; i++) {
            threads[i] = new PoolWorker();
            threads[i].start();
        }
    }

    public void execute(Runnable r) {
        synchronized(queue) {
            queue.addLast(r);
            queue.notify();
        }
    }

    private class PoolWorker extends Thread {

        @Override
        public void run() {
            Runnable r;

            while (true) {
                synchronized(queue) {
                    while (queue.isEmpty()) {
                        try {
                            queue.wait();
                        } catch (InterruptedException ignored) {
                            // No-op.
                        }
                    }

                    r = queue.removeFirst();
                }

                // If we don't catch RuntimeException, the pool could leak threads
                try {
                    r.run();
                } catch (RuntimeException e) {
                    // You might want to log something here
                    e.printStackTrace();
                    log.error("Error while running job", e);
                }
            }
        }
    }
}
