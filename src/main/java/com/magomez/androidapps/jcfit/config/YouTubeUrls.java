package com.magomez.androidapps.jcfit.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The video id of a YouTube link, which is what the app's player needs. Accepts
 * {@code youtube.com/watch?v=}, {@code youtu.be/}, {@code youtube.com/shorts/} and
 * {@code youtube.com/embed/}, over http or https, with or without www or m.
 */
public final class YouTubeUrls {

    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Set<String> LONG_HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com");
    private static final String SHORT_HOST = "youtu.be";
    private static final Set<String> ID_PATH_PREFIXES = Set.of("/shorts/", "/embed/");
    private static final String WATCH_PATH = "/watch";
    private static final String VIDEO_PARAM = "v=";

    private YouTubeUrls() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    /** Empty when the text is not a link to one YouTube video. */
    public static Optional<String> videoId(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (!scheme.equals("https") && !scheme.equals("http")) {
            return Optional.empty();
        }
        String candidate = null;
        if (host.equals(SHORT_HOST)) {
            candidate = path.startsWith("/") ? path.substring(1) : path;
        } else if (LONG_HOSTS.contains(host)) {
            if (path.equals(WATCH_PATH)) {
                candidate = queryValue(uri.getRawQuery());
            } else {
                candidate = ID_PATH_PREFIXES.stream()
                        .filter(path::startsWith)
                        .map(prefix -> path.substring(prefix.length()))
                        .findFirst()
                        .orElse(null);
            }
        }
        return Optional.ofNullable(candidate)
                .map(id -> id.endsWith("/") ? id.substring(0, id.length() - 1) : id)
                .filter(id -> VIDEO_ID.matcher(id).matches());
    }

    private static String queryValue(String query) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            if (pair.startsWith(VIDEO_PARAM)) {
                return pair.substring(VIDEO_PARAM.length());
            }
        }
        return null;
    }
}
