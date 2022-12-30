package com.magomez.androidapps.jctravels.rides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class WaitTimeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("time")
    private Date time;
    @JsonProperty("wait_time")
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
