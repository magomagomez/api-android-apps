package com.magomez.androidapps.mustsee.mdb.datasource;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetails;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsList;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class MovieDBDatasource {

    OkHttpClient okHttpClient = new OkHttpClient();

    private static final String URL = "https://api.themoviedb.org/3/";
    private static final String MOVIE_PATH = "movie";
    private static final String TV_PATH = "tv";
    private static final String SEARCH = "search";
    private static final String API_KEY = "api_key=f954a6db296d1cc9fbcfb6c03739f5db";
    private static final String LANGUAGE = "language=es-ES";
    private static final String URL_SEARCH_MOVIE =  URL + SEARCH + "/" + MOVIE_PATH;
    private static final String URL_SEARCH_TV =  URL + SEARCH + "/" + TV_PATH;


    public MovieDBDetails getMovieDetails(Integer id) throws IOException {
        String requestUrl= URL + MOVIE_PATH + "/" + id + "?" + API_KEY + "&" + LANGUAGE;
        ResponseBody response = httpCall(requestUrl);

        ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return objectMapper.readValue(response.string(), MovieDBDetails.class);
    }

    public MovieDBDetailsList getMovies(String search) throws IOException {

        String requestUrl= URL_SEARCH_MOVIE + "?" + API_KEY + "&" + LANGUAGE + "&query=" + search;
        ResponseBody response = httpCall(requestUrl);

        ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return objectMapper.readValue(response.string(), MovieDBDetailsList.class);
    }

    public MovieDBDetails getTvShowsDetails(Integer id) throws IOException {
        String requestUrl= URL + TV_PATH + "/" + id + "?" + API_KEY + "&" + LANGUAGE;
        ResponseBody response = httpCall(requestUrl);

        ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return objectMapper.readValue(response.string(), MovieDBDetails.class);
    }

    public MovieDBDetailsList getTvShows(String search) throws IOException {

        String requestUrl= URL_SEARCH_TV + "?" + API_KEY + "&" + LANGUAGE + "&query=" + search;
        ResponseBody response = httpCall(requestUrl);

        ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return objectMapper.readValue(response.string(), MovieDBDetailsList.class);
    }

    private ResponseBody httpCall(String requestUrl) throws IOException {
        Request request = new Request.Builder()
                .url(requestUrl)
                .build();
        return okHttpClient.newCall(request).execute().body();
    }

}
