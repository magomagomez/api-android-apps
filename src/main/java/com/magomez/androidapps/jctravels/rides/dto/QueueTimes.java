package com.magomez.androidapps.jctravels.rides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public class QueueTimes implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("STANDBY")
    private QueueTime standby;
    @JsonProperty("SINGLE_RIDER")
    private QueueTime singleRider;
    @JsonProperty("RETURN_TIME")
    private ReturnTime returnTime;
    @JsonProperty("PAID_RETURN_TIME")
    private ReturnTime paidReturnTime;

    public QueueTime getStandby() {
        return standby;
    }

    public void setStandby(QueueTime standby) {
        this.standby = standby;
    }

    public QueueTime getSingleRider() {
        return singleRider;
    }

    public void setSingleRider(QueueTime singleRider) {
        this.singleRider = singleRider;
    }

    public ReturnTime getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(ReturnTime returnTime) {
        this.returnTime = returnTime;
    }

    public ReturnTime getPaidReturnTime() {
        return paidReturnTime;
    }

    public void setPaidReturnTime(ReturnTime paidReturnTime) {
        this.paidReturnTime = paidReturnTime;
    }
}
