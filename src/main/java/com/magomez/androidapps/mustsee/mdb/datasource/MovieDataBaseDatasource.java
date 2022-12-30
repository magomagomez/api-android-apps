package com.magomez.androidapps.mustsee.mdb.datasource;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDetails;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDetailsList;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class MovieDataBaseDatasource {

    OkHttpClient okHttpClient = new OkHttpClient();

    private static final String URL = "https://api.themoviedb.org/3/";
    private static final String MOVIE_PATH = "movie";
    private static final String SEARCH = "search";
    private static final String API_KEY = "api_key=f954a6db296d1cc9fbcfb6c03739f5db";
    private static final String LANGUAGE = "language=es-ES";
    private static final String URL_SEARCH_MOVIE =  URL + SEARCH + "/" + MOVIE_PATH;


    public MovieDetails getMovieDetails(Integer id) throws IOException {
        String requestUrl= URL + MOVIE_PATH + "/" + id + "?" + API_KEY + "&" + LANGUAGE;
        Request request = new Request.Builder()
                .url(requestUrl)
                .build();
        ResponseBody response = okHttpClient.newCall(request).execute().body();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return objectMapper.readValue(response.string(), MovieDetails.class);
    }

    public MovieDetailsList getMovies(String search) throws IOException {

        String requestUrl= URL_SEARCH_MOVIE + "?" + API_KEY + "&" + LANGUAGE + "&query=" + search;
        Request request = new Request.Builder()
                .url(requestUrl)
                .build();
        ResponseBody response = okHttpClient.newCall(request).execute().body();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return objectMapper.readValue(response.string(), MovieDetailsList.class);
    }

}
