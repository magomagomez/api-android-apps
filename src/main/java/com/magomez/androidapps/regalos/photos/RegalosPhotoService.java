package com.magomez.androidapps.regalos.photos;

import com.magomez.androidapps.regalos.config.RegalosException;
import com.magomez.androidapps.regalos.items.ItemStatus;
import com.magomez.androidapps.regalos.lists.ListDao;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/**
 * One photo per wish and per idea. Whoever may write the wish or idea may set its photo (R11),
 * and a photo reaches exactly those who may see its wish or idea (R10); to anyone else it does
 * not exist. Every upload is cleaned by {@link PhotoImages} first (R12).
 */
@Service
public class RegalosPhotoService {

    private static final String ITEM_NOT_FOUND = "El desig no existeix";
    private static final String IDEA_NOT_FOUND = "La idea no existeix";
    private static final String NO_PHOTO = "No té foto";
    private static final int NAME_SUFFIX_BYTES = 2;

    private final RegalosPhotoDao photoDao;
    private final PhotoStore photoStore;
    private final PhotoFiles photoFiles;
    private final ListDao listDao;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public RegalosPhotoService(RegalosPhotoDao photoDao, PhotoStore photoStore, PhotoFiles photoFiles, ListDao listDao,
                        @Qualifier("regalosClock") Clock clock) {
        this.photoDao = photoDao;
        this.photoStore = photoStore;
        this.photoFiles = photoFiles;
        this.listDao = listDao;
        this.clock = clock;
    }

    public PhotoRef setOwnItemPhoto(int meId, long itemId, byte[] upload) {
        requireStore();
        RegalosPhotoDao.Slot slot = photoDao.ownItem(meId, itemId).orElseThrow(() -> RegalosException.notFound(ITEM_NOT_FOUND));
        PhotoRef ref = replace(slot, upload, "item-" + itemId, ITEM_NOT_FOUND,
                (fileId, at) -> photoDao.setOwnItem(meId, itemId, fileId, at));
        photoDao.markClaimEdited(itemId);
        return ref;
    }

    public void deleteOwnItemPhoto(int meId, long itemId) {
        requireStore();
        RegalosPhotoDao.Slot slot = photoDao.ownItem(meId, itemId).orElseThrow(() -> RegalosException.notFound(ITEM_NOT_FOUND));
        if (remove(slot, ITEM_NOT_FOUND, (fileId, at) -> photoDao.setOwnItem(meId, itemId, null, at))) {
            photoDao.markClaimEdited(itemId);
        }
    }

    public PhotoRef setListItemPhoto(int meId, long listId, long itemId, byte[] upload) {
        requireStore();
        RegalosPhotoDao.Slot slot = listItem(meId, listId, itemId);
        PhotoRef ref = replace(slot, upload, "item-" + itemId, ITEM_NOT_FOUND,
                (fileId, at) -> photoDao.setListItem(listId, itemId, fileId, at));
        photoDao.markClaimEdited(itemId);
        return ref;
    }

    public void deleteListItemPhoto(int meId, long listId, long itemId) {
        requireStore();
        RegalosPhotoDao.Slot slot = listItem(meId, listId, itemId);
        if (remove(slot, ITEM_NOT_FOUND, (fileId, at) -> photoDao.setListItem(listId, itemId, null, at))) {
            photoDao.markClaimEdited(itemId);
        }
    }

    public PhotoRef setIdeaPhoto(int meId, long ideaId, byte[] upload) {
        requireStore();
        RegalosPhotoDao.Slot slot = photoDao.idea(meId, ideaId).orElseThrow(() -> RegalosException.notFound(IDEA_NOT_FOUND));
        return replace(slot, upload, "idea-" + ideaId, IDEA_NOT_FOUND,
                (fileId, at) -> photoDao.setIdea(meId, ideaId, fileId, at));
    }

    public void deleteIdeaPhoto(int meId, long ideaId) {
        requireStore();
        RegalosPhotoDao.Slot slot = photoDao.idea(meId, ideaId).orElseThrow(() -> RegalosException.notFound(IDEA_NOT_FOUND));
        remove(slot, IDEA_NOT_FOUND, (fileId, at) -> photoDao.setIdea(meId, ideaId, null, at));
    }

    /** The photo of a wish, for whoever may see the wish (R10). */
    public byte[] itemPhoto(int meId, long itemId) {
        requireStore();
        RegalosPhotoDao.ViewableItem item = photoDao.viewableItem(itemId)
                .filter(found -> canSee(found, meId))
                .orElseThrow(() -> RegalosException.notFound(ITEM_NOT_FOUND));
        return download(item.fileId());
    }

    /** The photo of an idea, for its author and the people it is shared with; nobody else (R7, R13). */
    public byte[] ideaPhoto(int meId, long ideaId) {
        requireStore();
        RegalosPhotoDao.Slot slot = photoDao.viewableIdea(meId, ideaId).orElseThrow(() -> RegalosException.notFound(IDEA_NOT_FOUND));
        return download(slot.fileId());
    }

    /**
     * The owner sees their own wish; on someone else's letter, everyone sees ACTIVE wishes and the
     * giver still sees one received or removed later (in My gifts); on a list, only its members.
     */
    private boolean canSee(RegalosPhotoDao.ViewableItem item, int meId) {
        if (item.listId() != null) {
            return listDao.isMember(item.listId(), meId);
        }
        if (Objects.equals(item.ownerId(), meId)) {
            return item.status() != ItemStatus.DELETED;
        }
        return item.status() == ItemStatus.ACTIVE
                || Objects.equals(item.markedBy(), meId) || Objects.equals(item.giverUserId(), meId);
    }

    private RegalosPhotoDao.Slot listItem(int meId, long listId, long itemId) {
        if (!listDao.isMember(listId, meId)) {
            throw RegalosException.notFound("La llista no existeix");
        }
        return photoDao.listItem(listId, itemId).orElseThrow(() -> RegalosException.notFound(ITEM_NOT_FOUND));
    }

    /** Upload first, then point the row at it; the old photo goes only once the row has the new one. */
    private PhotoRef replace(RegalosPhotoDao.Slot slot, byte[] upload, String name, String notFound, Saver saver) {
        byte[] jpeg = PhotoImages.clean(upload);
        String fileId = photoStore.upload("regalos-" + name + "-" + suffix() + ".jpg", jpeg);
        Instant at = clock.instant();
        if (!saver.save(fileId, at)) {
            // Deleted or no longer editable while uploading: the new photo has nowhere to go.
            photoFiles.discard(fileId);
            throw RegalosException.notFound(notFound);
        }
        photoFiles.discard(slot.fileId());
        return new PhotoRef(at);
    }

    /** False when there was no photo to remove. */
    private boolean remove(RegalosPhotoDao.Slot slot, String notFound, Saver saver) {
        if (slot.fileId() == null) {
            return false;
        }
        if (!saver.save(null, clock.instant())) {
            throw RegalosException.notFound(notFound);
        }
        photoFiles.discard(slot.fileId());
        return true;
    }

    private byte[] download(String fileId) {
        if (fileId == null) {
            throw RegalosException.notFound(NO_PHOTO);
        }
        return photoStore.download(fileId).orElseThrow(() -> RegalosException.notFound(NO_PHOTO));
    }

    private void requireStore() {
        if (!photoStore.isConfigured()) {
            throw new RegalosException(HttpStatus.SERVICE_UNAVAILABLE, "photos_unavailable",
                    "Les fotos no estan disponibles");
        }
    }

    /** A few random hex digits so a replaced photo never shares a name with the old one. */
    private String suffix() {
        byte[] bytes = new byte[NAME_SUFFIX_BYTES];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    @FunctionalInterface
    private interface Saver {
        boolean save(String fileId, Instant at);
    }
}
