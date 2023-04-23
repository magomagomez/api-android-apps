package com.magomez.androidapps.jctravels.rides.converter;

import com.magomez.androidapps.jctravels.rides.dto.CreateRide;
import com.magomez.androidapps.jctravels.rides.dto.CreateRideRequest;
import com.magomez.androidapps.jctravels.rides.dto.ParksName;
import com.magomez.androidapps.jctravels.rides.dto.Ride;
import com.magomez.androidapps.jctravels.rides.dto.RideDTO;
import com.magomez.androidapps.jctravels.rides.dto.RideFilter;
import com.magomez.androidapps.jctravels.rides.dto.RideFilterRequest;
import com.magomez.androidapps.jctravels.rides.dto.ShowTime;
import com.magomez.androidapps.jctravels.rides.dto.ShowTimeDTO;
import com.magomez.androidapps.jctravels.rides.dto.UpdateRide;
import com.magomez.androidapps.jctravels.rides.dto.UpdateRideRequest;
import com.magomez.androidapps.jctravels.rides.dto.WaitTime;
import com.magomez.androidapps.jctravels.rides.dto.WaitTimeDTO;

import java.util.List;
import java.util.Objects;

public class RideConverter {

    private RideConverter(){}

    public static RideDTO toDto(Ride ride) {
        RideDTO rideDTO = new RideDTO();
        rideDTO.setId(ride.id());
        rideDTO.setName(ride.name());
        rideDTO.setImagePath(ride.imagePath());
        rideDTO.setDone(ride.done());
        rideDTO.setPark(Objects.requireNonNull(ParksName.get(ride.park())).name());
        rideDTO.setLocationPath(ride.locationPath());
        return rideDTO;
    }

    public static List<WaitTimeDTO> toWaitTimeList(List<WaitTime> waitTimes){
        return waitTimes.stream()
                .map(RideConverter::toDto)
                .toList();
    }

    public static WaitTimeDTO toDto(WaitTime waitTime) {
        WaitTimeDTO waitTimeDTO = new WaitTimeDTO();
        waitTimeDTO.setTime(waitTime.getTime());
        waitTimeDTO.setWaitTime(waitTime.getWaitingTime());
        return waitTimeDTO;
    }

    public static List<ShowTimeDTO> toShowTimeList(List<ShowTime> showTimes){
        return showTimes.stream()
                .map(RideConverter::toDto)
                .toList();
    }

    public static ShowTimeDTO toDto(ShowTime showTime) {
        ShowTimeDTO showTimeDTO = new ShowTimeDTO();
        showTimeDTO.setStartTime(showTime.getStartTime());
        showTimeDTO.setEndTime(showTime.getEndTime());
        return showTimeDTO;
    }

    public static CreateRide toRecord(CreateRideRequest request) {
        return new CreateRide(
                request.name(),
                request.park()
        );
    }

    public static UpdateRide toRecord(UpdateRideRequest request) {
        return new UpdateRide(
                request.name(),
                request.park(),
                request.done()
        );
    }

    public static RideFilter toFilter(RideFilterRequest request) {
        return new RideFilter(
                request.park()
        );
    }
}
