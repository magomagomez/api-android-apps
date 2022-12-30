package com.magomez.androidapps.jctravels.rides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class RideDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Integer id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("image_path")
    private String imagePath;
    @JsonProperty("location_path")
    private String locationPath;
    @JsonProperty("done")
    private Boolean done;
    @JsonProperty("park")
    private String park;
    @JsonProperty("queue_time")
    private Integer queueTime;
    @JsonProperty("status")
    private StatusType status;
    @JsonProperty("type")
    private RideType entityType;
    @JsonProperty("forecast")
    private List<WaitTimeDTO> forecast;
    @JsonProperty("show_time")
    private List<ShowTimeDTO> showtimes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public String getPark() {
        return park;
    }

    public void setPark(String park) {
        this.park = park;
    }

    public Integer getQueueTime() {
        return queueTime;
    }

    public void setQueueTime(Integer queueTime) {
        this.queueTime = queueTime;
    }

    public String getLocationPath() {
        return locationPath;
    }

    public void setLocationPath(String locationPath) {
        this.locationPath = locationPath;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public RideType getEntityType() {
        return entityType;
    }

    public void setEntityType(RideType entityType) {
        this.entityType = entityType;
    }

    public List<WaitTimeDTO> getForecast() {
        return forecast;
    }

    public void setForecast(List<WaitTimeDTO> forecast) {
        this.forecast = forecast;
    }

    public List<ShowTimeDTO> getShowtimes() {
        return showtimes;
    }

    public void setShowtimes(List<ShowTimeDTO> showtimes) {
        this.showtimes = showtimes;
    }
}
