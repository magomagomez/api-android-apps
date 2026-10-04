package com.magomez.androidapps.jctravels.rides.service;

import com.magomez.androidapps.jctravels.parks.dao.ParkDao;
import com.magomez.androidapps.jctravels.parks.dto.Park;
import com.magomez.androidapps.jctravels.rides.dao.RideDao;
import com.magomez.androidapps.jctravels.rides.dto.ParkInfo;
import com.magomez.androidapps.jctravels.rides.dto.Ride;
import com.magomez.androidapps.jctravels.rides.dto.RideForecastDTO;
import com.magomez.androidapps.jctravels.rides.dto.RideInfo;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * A ride's forecast from park.fan. park.fan names rides as themeparks.wiki does, so the ride
 * is looked up by its official name there. The forecast is extra: when anything is missing
 * or fails, the answer is an empty forecast, never an error.
 */
@Service
public class RideForecastService {

    private static final Logger logger = Logger.getLogger(RideForecastService.class.getName());

    private final RideDao rideDao;
    private final ParkDao parkDao;
    private final RideQueueService rideQueueService;
    private final ParkFanService parkFanService;

    public RideForecastService(RideDao rideDao, ParkDao parkDao, RideQueueService rideQueueService, ParkFanService parkFanService) {
        this.rideDao = rideDao;
        this.parkDao = parkDao;
        this.rideQueueService = rideQueueService;
        this.parkFanService = parkFanService;
    }

    public RideForecastDTO forecast(Integer rideId) {
        Ride ride = rideDao.getRide(rideId);
        Park park = parkDao.getPark(ride.park());
        try {
            ParkInfo live = rideQueueService.getRideQueueTimes(park.queueId());
            String officialName = officialName(live, ride.code());
            if (officialName == null) {
                return RideForecasts.empty();
            }
            String url = parkFanService.attractionUrl(officialName, live.getName());
            if (url == null) {
                logger.info(() -> "No park.fan match for ride " + rideId + " (" + officialName + ")");
                return RideForecasts.empty();
            }
            return RideForecasts.toDto(parkFanService.attraction(url));
        } catch (IOException ex) {
            logger.warning(() -> "No forecast for ride " + rideId + ": " + ex.getMessage());
            return RideForecasts.empty();
        }
    }

    private static String officialName(ParkInfo live, String code) {
        if (live == null || live.getLiveData() == null || code == null) {
            return null;
        }
        return live.getLiveData().stream()
                .filter(info -> code.equals(info.getId()))
                .map(RideInfo::getName)
                .findFirst()
                .orElse(null);
    }
}
