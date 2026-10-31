package com.magomez.androidapps.regalos.photos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Photos left behind when a wish, an idea or a list really goes. Callers read the file ids before
 * deleting the rows and discard them after; a photo Drive will not delete only gets logged, it
 * never undoes the delete.
 */
@Component
public class PhotoFiles {

    private static final Logger log = LoggerFactory.getLogger(PhotoFiles.class);

    private final RegalosPhotoDao photoDao;
    private final PhotoStore photoStore;

    public PhotoFiles(RegalosPhotoDao photoDao, PhotoStore photoStore) {
        this.photoDao = photoDao;
        this.photoStore = photoStore;
    }

    public Optional<String> ofItem(long itemId) {
        return photoDao.itemFile(itemId);
    }

    public List<String> ofList(long listId) {
        return photoDao.listFiles(listId);
    }

    /** Only my own idea's photo: another author's idea has none as far as I am concerned (R7). */
    public Optional<String> ofIdea(int authorId, long ideaId) {
        return photoDao.idea(authorId, ideaId).map(RegalosPhotoDao.Slot::fileId);
    }

    public void discard(Collection<String> fileIds) {
        fileIds.forEach(this::discard);
    }

    public void discard(String fileId) {
        if (fileId == null) {
            return;
        }
        if (!photoStore.isConfigured()) {
            log.warn("regalos: a photo was left in Drive because Drive is not configured");
            return;
        }
        try {
            photoStore.delete(fileId);
        } catch (PhotoStoreException e) {
            log.warn("regalos: a photo was left in Drive ({})", e.getMessage());
        }
    }
}
