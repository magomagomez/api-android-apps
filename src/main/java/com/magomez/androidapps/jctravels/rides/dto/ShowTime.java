package com.magomez.androidapps.jctravels.rides.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class ShowTime implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Date endTime;
    private Date startTime;

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
}
