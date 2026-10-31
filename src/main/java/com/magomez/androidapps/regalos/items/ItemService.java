package com.magomez.androidapps.regalos.items;

import com.magomez.androidapps.regalos.config.RegalosException;
import com.magomez.androidapps.regalos.photos.PhotoFiles;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** My letter. Every answer is an {@link OwnItem}, which cannot say who gives a wish (R1). */
@Service
public class ItemService {

    private static final String NOT_FOUND = "El deseo no existe";

    private final ItemDao itemDao;
    private final PhotoFiles photoFiles;

    public ItemService(ItemDao itemDao, PhotoFiles photoFiles) {
        this.itemDao = itemDao;
        this.photoFiles = photoFiles;
    }

    /** ACTIVE by default; DELETED is never listed (to the owner it is simply gone). */
    public List<OwnItem> list(int meId, ItemStatus status) {
        ItemStatus wanted = status == null ? ItemStatus.ACTIVE : status;
        if (wanted == ItemStatus.DELETED) {
            throw RegalosException.badRequest("Solo se pueden listar los deseos pendientes o recibidos");
        }
        return itemDao.findMine(meId, wanted);
    }

    public OwnItem create(int meId, ItemRequest request) {
        long id = itemDao.insert(meId, ItemRules.valid(request));
        return find(meId, id);
    }

    public OwnItem update(int meId, long id, ItemRequest request) {
        if (!itemDao.update(meId, id, ItemRules.valid(request))) {
            throw RegalosException.notFound(NOT_FOUND);
        }
        itemDao.markClaimEdited(id);
        return find(meId, id);
    }

    public OwnItem received(int meId, long id) {
        return move(meId, id, ItemStatus.ACTIVE, ItemStatus.RECEIVED);
    }

    public OwnItem notReceived(int meId, long id) {
        return move(meId, id, ItemStatus.RECEIVED, ItemStatus.ACTIVE);
    }

    /**
     * R5: the same outcome whether someone had claimed it or not. A claimed wish keeps its photo
     * for its giver; one that really goes takes its photo with it.
     */
    public void delete(int meId, long id) {
        Optional<String> photo = photoFiles.ofItem(id);
        if (itemDao.softDelete(meId, id)) {
            return;
        }
        if (!itemDao.hardDelete(meId, id)) {
            throw RegalosException.notFound(NOT_FOUND);
        }
        photo.ifPresent(photoFiles::discard);
    }

    /** Idempotent: a wish already in the target status is returned as it is. */
    private OwnItem move(int meId, long id, ItemStatus from, ItemStatus to) {
        itemDao.setStatus(meId, id, from, to);
        OwnItem item = find(meId, id);
        if (item.status() != to) {
            throw RegalosException.notFound(NOT_FOUND);
        }
        return item;
    }

    private OwnItem find(int meId, long id) {
        return itemDao.findMine(meId, id).orElseThrow(() -> RegalosException.notFound(NOT_FOUND));
    }
}
