package com.magomez.androidapps.jctravels.rides.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class WaitTime implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Date time;
    private Integer waitingTime;


    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Integer getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(Integer waitingTime) {
        this.waitingTime = waitingTime;
    }

}
