package com.magomez.androidapps.mustsee.mdb.datasource;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetails;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsList;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class MovieDBDatasource {

    private static final String URL = "https://api.themoviedb.org/3/";
    private static final String MOVIE_PATH = "movie";
    private static final String TV_PATH = "tv";
    private static final String SEARCH = "search";
    private static final String LANGUAGE = "es-ES";

    private static final Integer MOVIE_TYPE = 1;

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final ObjectMapper objectMapper =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final String apiKey;

    public MovieDBDatasource(@Value("${tmdb.api.key}") String apiKey) {
        this.apiKey = apiKey;
    }

    public MovieDBDetails getMovieDetails(Integer id, Integer filmType) throws IOException {
        String path = MOVIE_TYPE.equals(filmType) ? MOVIE_PATH : TV_PATH;
        return get(url(path + "/" + id).build(), MovieDBDetails.class);
    }

    public MovieDBDetailsList getMovies(String search) throws IOException {
        HttpUrl url = url(SEARCH + "/" + MOVIE_PATH).addQueryParameter("query", search).build();
        return get(url, MovieDBDetailsList.class);
    }

    public MovieDBDetails getTvShowsDetails(Integer id) throws IOException {
        return get(url(TV_PATH + "/" + id).build(), MovieDBDetails.class);
    }

    public MovieDBDetailsList getTvShows(String search) throws IOException {
        HttpUrl url = url(SEARCH + "/" + TV_PATH).addQueryParameter("query", search).build();
        return get(url, MovieDBDetailsList.class);
    }

    /** Query parameters are percent-encoded by HttpUrl, so a search like "Fast & Furious" stays one value. */
    private HttpUrl.Builder url(String path) {
        return HttpUrl.get(URL + path).newBuilder()
                .addQueryParameter("api_key", apiKey)
                .addQueryParameter("language", LANGUAGE);
    }

    private <T> T get(HttpUrl url, Class<T> type) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response from MovieDB: " + response.code());
            }
            return objectMapper.readValue(body.string(), type);
        }
    }

}
