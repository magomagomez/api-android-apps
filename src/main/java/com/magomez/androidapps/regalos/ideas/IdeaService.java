package com.magomez.androidapps.regalos.ideas;

import com.magomez.androidapps.regalos.config.RegalosException;
import com.magomez.androidapps.regalos.items.ItemRequest;
import com.magomez.androidapps.regalos.items.ItemRules;
import com.magomez.androidapps.regalos.items.ValidItem;
import com.magomez.androidapps.regalos.people.PeopleService;
import com.magomez.androidapps.regalos.photos.PhotoFiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Ideas for other people. Their author writes, shares and deletes them; the people an idea is
 * shared with see it and may give it (R13, R14). To anyone else, and always to the person it is
 * for, an idea answers like a missing one.
 */
@Service
public class IdeaService {

    private static final String NOT_FOUND = "La idea no existeix";

    private final IdeaDao ideaDao;
    private final PeopleService peopleService;
    private final PhotoFiles photoFiles;

    public IdeaService(IdeaDao ideaDao, PeopleService peopleService, PhotoFiles photoFiles) {
        this.ideaDao = ideaDao;
        this.peopleService = peopleService;
        this.photoFiles = photoFiles;
    }

    /** My ideas for that person, with whom each is shared, and the ideas others shared with me for them. */
    public List<Idea> list(int meId, int forUserId) {
        requireOther(meId, forUserId);
        Map<Long, List<Integer>> shares = ideaDao.sharesFor(meId, forUserId);
        return ideaDao.findFor(meId, forUserId).stream()
                .map(idea -> idea.mine() ? idea.withSharedWith(shares.getOrDefault(idea.id(), List.of())) : idea)
                .toList();
    }

    public Idea create(int meId, int forUserId, IdeaRequest request) {
        requireOther(meId, forUserId);
        ValidItem valid = valid(request);
        List<Integer> sharedWith = sharedWith(request, meId, forUserId);
        long id = ideaDao.insert(meId, forUserId, valid, bought(request));
        ideaDao.setShares(id, List.of(), sharedWith);
        return mine(meId, id);
    }

    public Idea update(int meId, long id, IdeaRequest request) {
        Idea idea = ideaDao.find(meId, id).filter(Idea::mine).orElseThrow(() -> notMine(meId, id));
        ValidItem valid = valid(request);
        List<Integer> sharedWith = sharedWith(request, meId, idea.forUserId());
        boolean bought = bought(request);
        if (bought && idea.claim() != null) {
            throw alreadyGiven(idea.claim());
        }
        if (!ideaDao.update(meId, id, valid, bought)) {
            // Claimed or deleted between our read and the update.
            IdeaClaim claim = ideaDao.find(meId, id).map(Idea::claim).orElse(null);
            throw bought && claim != null ? alreadyGiven(claim) : RegalosException.notFound(NOT_FOUND);
        }
        ideaDao.setShares(id, ideaDao.sharesOf(meId, id), sharedWith);
        return mine(meId, id);
    }

    public void delete(int meId, long id) {
        Optional<String> photo = photoFiles.ofIdea(meId, id);
        if (!ideaDao.delete(meId, id)) {
            throw notMine(meId, id);
        }
        photo.ifPresent(photoFiles::discard);
    }

    /** «La regalo jo» or «Ja la tinc» on an idea shared with me; the first claim wins (R14). */
    public Idea claim(int meId, long id, IdeaClaimRequest request) {
        if (request == null || request.state() == null) {
            throw RegalosException.badRequest("Falta si està reservat o comprat");
        }
        Idea idea = ideaDao.find(meId, id).orElseThrow(() -> RegalosException.notFound(NOT_FOUND));
        if (idea.mine()) {
            throw RegalosException.badRequest("És una idea teva: marca-la com a comprada");
        }
        IdeaClaim claim = idea.claim();
        if (claim != null && claim.mine()) {
            ideaDao.updateClaim(id, meId, request.state());
        } else if (claim != null) {
            throw alreadyGiven(claim);
        } else if (idea.bought()) {
            throw new RegalosException(HttpStatus.CONFLICT, "already_claimed", "Ja l'ha comprada " + idea.author().name());
        } else if (!ideaDao.insertClaim(id, meId, request.state())) {
            // Someone else, or its author, got there between our read and the insert.
            Idea now = ideaDao.find(meId, id).orElseThrow(() -> RegalosException.notFound(NOT_FOUND));
            throw now.claim() != null ? alreadyGiven(now.claim())
                    : new RegalosException(HttpStatus.CONFLICT, "already_claimed", "Ja l'ha comprada " + now.author().name());
        }
        return ideaDao.find(meId, id).orElseThrow(() -> RegalosException.notFound(NOT_FOUND));
    }

    /** Only who put the claim can remove it (R4). */
    public void unclaim(int meId, long id) {
        ideaDao.find(meId, id).orElseThrow(() -> RegalosException.notFound(NOT_FOUND));
        if (!ideaDao.deleteClaim(id, meId)) {
            throw RegalosException.notFound("No la regales tu");
        }
    }

    private void requireOther(int meId, int forUserId) {
        if (forUserId == meId) {
            throw RegalosException.forbidden("Les teves idees són per regalar als altres");
        }
        if (peopleService.person(forUserId).isEmpty()) {
            throw RegalosException.notFound("Aquesta persona no existeix");
        }
    }

    /** R13: people of the app, never the author nor the person the idea is for; no repeats. */
    private List<Integer> sharedWith(IdeaRequest request, int authorId, int forUserId) {
        if (request.sharedWith() == null) {
            return List.of();
        }
        Set<Integer> people = new LinkedHashSet<>();
        for (Integer id : request.sharedWith()) {
            if (id == null || id == authorId) {
                throw RegalosException.badRequest("No pots compartir una idea amb tu mateix");
            }
            if (id == forUserId) {
                throw RegalosException.badRequest("No pots compartir una idea amb qui la rebrà");
            }
            if (peopleService.person(id).isEmpty()) {
                throw RegalosException.badRequest("Aquesta persona no existeix");
            }
            people.add(id);
        }
        return List.copyOf(people);
    }

    /** The same limits as a wish (title, http(s) link, price, notes). */
    private static ValidItem valid(IdeaRequest request) {
        if (request == null) {
            throw RegalosException.badRequest("Falten les dades de la idea");
        }
        return ItemRules.valid(new ItemRequest(request.title(), request.url(), request.store(), request.priceCents(),
                null, request.notes(), false));
    }

    private static boolean bought(IdeaRequest request) {
        return Boolean.TRUE.equals(request.bought());
    }

    /** One of my ideas, with whom it is shared. */
    private Idea mine(int meId, long id) {
        return ideaDao.find(meId, id).filter(Idea::mine)
                .map(idea -> idea.withSharedWith(ideaDao.sharesOf(meId, id)))
                .orElseThrow(() -> RegalosException.notFound(NOT_FOUND));
    }

    /** Someone it is shared with may see it but not change it; to anyone else it does not exist. */
    private RegalosException notMine(int meId, long id) {
        return ideaDao.find(meId, id).isPresent()
                ? RegalosException.forbidden("Només qui l'ha apuntada la pot canviar")
                : RegalosException.notFound(NOT_FOUND);
    }

    private static RegalosException alreadyGiven(IdeaClaim claim) {
        return new RegalosException(HttpStatus.CONFLICT, "already_claimed", "Ja la regala " + claim.giver().name());
    }
}
