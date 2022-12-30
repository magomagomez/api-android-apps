package com.magomez.androidapps.jctravels.rides.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class RideInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private RideType entityType;
    private QueueTimes queue;
    private StatusType status;
    private List<WaitTime> forecast;
    private List<ShowTime> showtimes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RideType getEntityType() {
        return entityType;
    }

    public void setEntityType(RideType entityType) {
        this.entityType = entityType;
    }

    public QueueTimes getQueue() {
        return queue;
    }

    public void setQueue(QueueTimes queue) {
        this.queue = queue;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public List<WaitTime> getForecast() {
        return forecast;
    }

    public void setForecast(List<WaitTime> forecast) {
        this.forecast = forecast;
    }

    public List<ShowTime> getShowtimes() {
        return showtimes;
    }

    public void setShowtimes(List<ShowTime> showtimes) {
        this.showtimes = showtimes;
    }
}
