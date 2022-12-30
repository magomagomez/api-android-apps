package com.magomez.androidapps.jctravels.rides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public class QueueTimes implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("STANDBY")
    private QueueTime standby;

    public QueueTime getStandby() {
        return standby;
    }

    public void setStandby(QueueTime standby) {
        this.standby = standby;
    }
}
