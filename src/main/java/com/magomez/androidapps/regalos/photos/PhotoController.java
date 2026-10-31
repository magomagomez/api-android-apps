package com.magomez.androidapps.regalos.photos;

import com.magomez.androidapps.regalos.auth.RegalosCurrentUser;
import com.magomez.androidapps.regalos.config.RegalosApi;
import com.magomez.androidapps.regalos.config.RegalosException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

/** Photos of wishes and ideas. Uploads are the raw image as the body (image/jpeg or image/png). */
@RestController
@RequestMapping(RegalosApi.BASE_URL)
@Tag(name = "Regalos - fotos", description = "Una foto por deseo o idea, guardada en Google Drive")
public class PhotoController {

    /** The URL carries the photo version, so a cached photo is never stale for long. */
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(7)).cachePrivate();

    private final RegalosPhotoService photoService;

    public PhotoController(RegalosPhotoService photoService) {
        this.photoService = photoService;
    }

    @Operation(summary = "Set the photo of a wish of mine (replaces the old one)")
    @PutMapping(value = "/me/items/{id}/photo", consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoRef setOwnItemPhoto(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                                    @PathVariable long id, HttpServletRequest request) {
        return photoService.setOwnItemPhoto(me.id(), id, body(request));
    }

    @Operation(summary = "Remove the photo of a wish of mine")
    @DeleteMapping("/me/items/{id}/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOwnItemPhoto(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                                   @PathVariable long id) {
        photoService.deleteOwnItemPhoto(me.id(), id);
    }

    @Operation(summary = "Set the photo of a wish of a shared list (any member)")
    @PutMapping(value = "/lists/{listId}/items/{itemId}/photo",
            consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoRef setListItemPhoto(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                                     @PathVariable long listId, @PathVariable long itemId, HttpServletRequest request) {
        return photoService.setListItemPhoto(me.id(), listId, itemId, body(request));
    }

    @Operation(summary = "Remove the photo of a wish of a shared list (any member)")
    @DeleteMapping("/lists/{listId}/items/{itemId}/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteListItemPhoto(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                                    @PathVariable long listId, @PathVariable long itemId) {
        photoService.deleteListItemPhoto(me.id(), listId, itemId);
    }

    @Operation(summary = "Set the photo of one of my ideas")
    @PutMapping(value = "/ideas/{id}/photo", consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoRef setIdeaPhoto(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                                 @PathVariable long id, HttpServletRequest request) {
        return photoService.setIdeaPhoto(me.id(), id, body(request));
    }

    @Operation(summary = "Remove the photo of one of my ideas")
    @DeleteMapping("/ideas/{id}/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIdeaPhoto(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                                @PathVariable long id) {
        photoService.deleteIdeaPhoto(me.id(), id);
    }

    @Operation(summary = "The photo of a wish, for whoever may see the wish (404 for anyone else)")
    @GetMapping("/items/{id}/photo")
    public ResponseEntity<byte[]> itemPhoto(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                                            @PathVariable long id) {
        return jpeg(photoService.itemPhoto(me.id(), id));
    }

    @Operation(summary = "The photo of one of my ideas")
    @GetMapping("/ideas/{id}/photo")
    public ResponseEntity<byte[]> ideaPhoto(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                                            @PathVariable long id) {
        return jpeg(photoService.ideaPhoto(me.id(), id));
    }

    private static ResponseEntity<byte[]> jpeg(byte[] photo) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CACHE).body(photo);
    }

    /** The upload, read only up to one byte past the limit. */
    private static byte[] body(HttpServletRequest request) {
        if (request.getContentLengthLong() > PhotoImages.MAX_BYTES) {
            throw PhotoImages.tooLarge();
        }
        try (InputStream in = request.getInputStream()) {
            byte[] bytes = in.readNBytes(PhotoImages.MAX_BYTES + 1);
            if (bytes.length > PhotoImages.MAX_BYTES) {
                throw PhotoImages.tooLarge();
            }
            return bytes;
        } catch (IOException e) {
            throw RegalosException.badRequest("La foto no ha llegado entera");
        }
    }
}
