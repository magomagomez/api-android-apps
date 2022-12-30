package com.magomez.androidapps.jctravels.rides.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.jctravels.rides.dto.ParkInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.logging.Logger;

@Service
public class RideQueueService {
    OkHttpClient client = new OkHttpClient();

    static Logger logger = Logger.getLogger(RideQueueService.class.getName());

    public ParkInfo getRideQueueTimes(String id) throws IOException {
        return getRideInfo(id);
    }

    private ParkInfo getRideInfo(String id) throws IOException {
        String url = "https://api.themeparks.wiki/v1/entity/"+id+"/live";
        Request request = new Request.Builder()
                .url(url)
                .build();
        ResponseBody response = client.newCall(request).execute().body();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return objectMapper.readValue(response.string(), ParkInfo.class);
    }

}
