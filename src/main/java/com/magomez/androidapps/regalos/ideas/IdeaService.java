package com.magomez.androidapps.regalos.ideas;

import com.magomez.androidapps.regalos.config.RegalosException;
import com.magomez.androidapps.regalos.items.ItemRequest;
import com.magomez.androidapps.regalos.items.ItemRules;
import com.magomez.androidapps.regalos.items.ValidItem;
import com.magomez.androidapps.regalos.people.PeopleService;
import com.magomez.androidapps.regalos.photos.PhotoFiles;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** My ideas for other people. Someone else's idea answers like a missing one. */
@Service
public class IdeaService {

    private static final String NOT_FOUND = "La idea no existe";

    private final IdeaDao ideaDao;
    private final PeopleService peopleService;
    private final PhotoFiles photoFiles;

    public IdeaService(IdeaDao ideaDao, PeopleService peopleService, PhotoFiles photoFiles) {
        this.ideaDao = ideaDao;
        this.peopleService = peopleService;
        this.photoFiles = photoFiles;
    }

    public List<Idea> list(int meId, int forUserId) {
        requireOther(meId, forUserId);
        return ideaDao.findFor(meId, forUserId);
    }

    public Idea create(int meId, int forUserId, IdeaRequest request) {
        requireOther(meId, forUserId);
        long id = ideaDao.insert(meId, forUserId, valid(request), bought(request));
        return find(meId, id);
    }

    public Idea update(int meId, long id, IdeaRequest request) {
        if (!ideaDao.update(meId, id, valid(request), bought(request))) {
            throw RegalosException.notFound(NOT_FOUND);
        }
        return find(meId, id);
    }

    public void delete(int meId, long id) {
        Optional<String> photo = photoFiles.ofIdea(meId, id);
        if (!ideaDao.delete(meId, id)) {
            throw RegalosException.notFound(NOT_FOUND);
        }
        photo.ifPresent(photoFiles::discard);
    }

    private void requireOther(int meId, int forUserId) {
        if (forUserId == meId) {
            throw RegalosException.forbidden("Tus ideas son para regalar a los demás");
        }
        if (peopleService.person(forUserId).isEmpty()) {
            throw RegalosException.notFound("Esa persona no existe");
        }
    }

    /** The same limits as a wish (title, http(s) link, price, notes). */
    private static ValidItem valid(IdeaRequest request) {
        if (request == null) {
            throw RegalosException.badRequest("Faltan los datos de la idea");
        }
        return ItemRules.valid(new ItemRequest(request.title(), request.url(), request.store(), request.priceCents(),
                null, request.notes(), false));
    }

    private static boolean bought(IdeaRequest request) {
        return Boolean.TRUE.equals(request.bought());
    }

    private Idea find(int meId, long id) {
        return ideaDao.find(meId, id).orElseThrow(() -> RegalosException.notFound(NOT_FOUND));
    }
}
