package com.graffitab.server.service.job;

import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j;

@Log4j
@Service
public class JobService {

    private WorkQueue workQueue = new WorkQueue(2);

    public void execute(Runnable r) {
        workQueue.execute(r);
    }
}
