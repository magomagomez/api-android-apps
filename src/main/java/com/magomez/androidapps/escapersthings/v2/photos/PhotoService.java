package com.magomez.androidapps.escapersthings.v2.photos;

import com.magomez.androidapps.escapersthings.v2.config.ApiException;
import com.magomez.androidapps.escapersthings.v2.config.Sha256;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/** Poster and group photo of a room, kept in Postgres (escape_room_photos.data, bytea). */
@Service
public class PhotoService {

    static final int MAX_BYTES = 2 * 1024 * 1024;

    private static final String INVALID_PHOTO = "invalid_photo";
    private static final String PHOTO_NOT_FOUND = "La foto no existe";
    private static final String ANY_ETAG = "*";
    private static final String WEAK_PREFIX = "W/";

    private final PhotoDao photoDao;
    private final RoomService roomService;

    public PhotoService(PhotoDao photoDao, RoomService roomService) {
        this.photoDao = photoDao;
        this.roomService = roomService;
    }

    /** ifNoneMatch is the request header, or null; a matching etag means "not modified". */
    public PhotoReply read(int roomId, String kind, String ifNoneMatch) {
        PhotoKind photoKind = kind(kind);
        StoredPhoto photo = photoDao.find(roomId, photoKind).orElseThrow(PhotoService::photoNotFound);
        return new PhotoReply(photo, matches(ifNoneMatch, photo.etag()));
    }

    /** Reads at most {@link #MAX_BYTES} + 1 bytes, so an oversized upload never fills the memory. */
    public void save(int roomId, String kind, InputStream body) throws IOException {
        PhotoKind photoKind = kind(kind);
        byte[] data = body.readNBytes(MAX_BYTES + 1);
        if (data.length > MAX_BYTES) {
            throw invalid("La foto no puede pasar de 2 MB");
        }
        String contentType = PhotoType.detect(data)
                .orElseThrow(() -> invalid("La foto debe ser JPEG, PNG o WebP"));
        roomService.requireExists(roomId);
        photoDao.save(roomId, photoKind, new StoredPhoto(contentType, data, Sha256.hex(data)));
    }

    /** If-None-Match may list several etags, quoted or weak, or be "*". */
    static boolean matches(String ifNoneMatch, String etag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
            return false;
        }
        return Arrays.stream(ifNoneMatch.split(","))
                .map(String::trim)
                .map(tag -> tag.startsWith(WEAK_PREFIX) ? tag.substring(WEAK_PREFIX.length()) : tag)
                .anyMatch(tag -> tag.equals(ANY_ETAG) || tag.equals("\"" + etag + "\""));
    }

    private static PhotoKind kind(String kind) {
        return PhotoKind.fromPath(kind).orElseThrow(PhotoService::photoNotFound);
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, INVALID_PHOTO, message);
    }

    private static ApiException photoNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", PHOTO_NOT_FOUND);
    }
}
