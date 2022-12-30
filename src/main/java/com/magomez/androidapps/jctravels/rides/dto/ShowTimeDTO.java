package com.magomez.androidapps.jctravels.rides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class ShowTimeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("end_time")
    private Date endTime;
    @JsonProperty("start_time")
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
