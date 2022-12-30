package com.magomez.androidapps.jctravels.rides.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class WaitTime implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Date time;
    private Integer waitTime;


    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Integer getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Integer waitTime) {
        this.waitTime = waitTime;
    }

}
