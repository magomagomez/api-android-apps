package com.magomez.androidapps.jctravels.rides.dto;

import java.io.Serial;
import java.io.Serializable;

public class QueueTime implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer waitTime;

    public Integer getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Integer waitTime) {
        this.waitTime = waitTime;
    }
}
