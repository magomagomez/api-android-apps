package com.magomez.androidapps.escapersthings.v2.photos;

import com.magomez.androidapps.escapersthings.v2.config.ApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping(ApiConfig.BASE_URL + "/escape-rooms/{roomId}/photos/{kind}")
@Tag(name = "Escapers v2 - fotos", description = "Cartel (poster) y foto del grupo (group)")
public class PhotosController {

    static final String CACHE_CONTROL = "private, max-age=86400";

    private final PhotoService photoService;

    public PhotosController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @Operation(summary = "The photo with its real type, ETag and a one-day private cache (304 with If-None-Match)")
    @GetMapping
    public ResponseEntity<byte[]> get(@PathVariable int roomId, @PathVariable String kind,
                                      @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        PhotoReply reply = photoService.read(roomId, kind, ifNoneMatch);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(reply.notModified() ? HttpStatus.NOT_MODIFIED : HttpStatus.OK)
                .eTag("\"" + reply.photo().etag() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL);
        if (reply.notModified()) {
            return response.build();
        }
        return response.contentType(MediaType.parseMediaType(reply.photo().contentType())).body(reply.photo().data());
    }

    @Operation(summary = "Upload or replace the photo: JPEG, PNG or WebP bytes, at most 2 MB")
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(@PathVariable int roomId, @PathVariable String kind, InputStream body) throws IOException {
        photoService.save(roomId, kind, body);
    }
}
