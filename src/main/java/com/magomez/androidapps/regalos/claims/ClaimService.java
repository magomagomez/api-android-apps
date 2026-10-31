package com.magomez.androidapps.regalos.claims;

import com.magomez.androidapps.regalos.config.RegalosException;
import com.magomez.androidapps.regalos.ideas.Idea;
import com.magomez.androidapps.regalos.ideas.IdeaDao;
import com.magomez.androidapps.regalos.ideas.IdeaWithPerson;
import com.magomez.androidapps.regalos.items.ItemStatus;
import com.magomez.androidapps.regalos.lists.ListDao;
import com.magomez.androidapps.regalos.people.PeopleService;
import com.magomez.androidapps.regalos.people.Person;
import com.magomez.androidapps.regalos.photos.PhotoFiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Giving. The owner of a wish never gets past the first check of any method here, so nothing
 * of its claim can reach them (R1). A wish of a shared list has no owner: there the check is
 * being invited, and to anyone else the wish does not exist (R8).
 */
@Service
public class ClaimService {

    static final int MAX_GIVER_NAME = 60;

    private static final String ITEM_NOT_FOUND = "El deseo no existe";
    private static final String OWN_WISH = "Es un deseo tuyo";
    private static final String PERSON_KEY = "P";
    private static final String LIST_KEY = "L";

    private final ClaimDao claimDao;
    private final PeopleService peopleService;
    private final ListDao listDao;
    private final IdeaDao ideaDao;
    private final PhotoFiles photoFiles;

    public ClaimService(ClaimDao claimDao, PeopleService peopleService, ListDao listDao, IdeaDao ideaDao,
                        PhotoFiles photoFiles) {
        this.claimDao = claimDao;
        this.peopleService = peopleService;
        this.listDao = listDao;
        this.ideaDao = ideaDao;
        this.photoFiles = photoFiles;
    }

    public PersonItems listOf(int meId, int userId) {
        if (userId == meId) {
            throw RegalosException.forbidden("Tu carta se ve en «Mi carta»");
        }
        Person person = peopleService.person(userId)
                .orElseThrow(() -> RegalosException.notFound("Esa persona no existe"));
        List<OthersItem> items = claimDao.activeItemsOf(userId).stream().map(row -> othersItem(row, meId)).toList();
        return new PersonItems(person, items);
    }

    /** The ACTIVE wishes of a shared list; the caller has already checked I am invited. */
    public List<OthersItem> listItems(int meId, long listId) {
        return claimDao.activeItemsOfList(listId).stream().map(row -> othersItem(row, meId)).toList();
    }

    /** One wish as a giver sees it, right after it was written into a shared list. */
    public OthersItem item(int meId, long itemId) {
        return claimDao.item(itemId).map(row -> othersItem(row, meId))
                .orElseThrow(() -> RegalosException.notFound(ITEM_NOT_FOUND));
    }

    /** Creates the claim, or changes it when it is mine to change (R4); anyone else's is a 409 (R2). */
    public OthersItem claim(int meId, long itemId, ClaimRequest request) {
        ClaimRow row = claimDao.item(itemId)
                .filter(found -> found.status() == ItemStatus.ACTIVE)
                .filter(found -> !found.inList() || listDao.isMember(found.listId(), meId))
                .orElseThrow(() -> RegalosException.notFound(ITEM_NOT_FOUND));
        if (row.ownedBy(meId)) {
            throw RegalosException.forbidden(OWN_WISH);
        }
        if (request == null || request.state() == null) {
            throw RegalosException.badRequest("Falta si está reservado o comprado");
        }
        ResolvedGiver giver = giver(meId, row, request.giver());
        if (!row.claimed()) {
            boolean inserted;
            try {
                inserted = claimDao.insert(itemId, meId, giver.userId(), giver.name(), request.state());
            } catch (DataIntegrityViolationException e) {
                // Deleted by its owner between our read and the insert.
                throw RegalosException.notFound(ITEM_NOT_FOUND);
            }
            if (!inserted) {
                throw alreadyClaimed(claimDao.item(itemId).orElse(row));
            }
        } else if (!row.editableBy(meId)
                || !claimDao.update(itemId, meId, giver.userId(), giver.name(), request.state())) {
            throw alreadyClaimed(row);
        }
        return othersItem(claimDao.item(itemId).orElse(row), meId);
    }

    public void unclaim(int meId, long itemId) {
        ClaimRow row = claimDao.item(itemId)
                .filter(found -> !found.inList() || listDao.isMember(found.listId(), meId))
                .orElseThrow(() -> RegalosException.notFound(ITEM_NOT_FOUND));
        if (row.ownedBy(meId)) {
            throw RegalosException.forbidden(OWN_WISH);
        }
        if (!row.claimed()) {
            throw RegalosException.notFound("Nadie ha marcado este deseo");
        }
        if (!row.editableBy(meId)) {
            throw RegalosException.forbidden("Solo puede quitarla quien la puso o a quien se la asignaron");
        }
        if (!claimDao.delete(itemId, meId)) {
            throw RegalosException.notFound("Nadie ha marcado este deseo");
        }
        if (row.status() == ItemStatus.DELETED) {
            // Its owner removed it earlier (R5); without the claim nothing keeps it, nor its photo.
            Optional<String> photo = photoFiles.ofItem(itemId);
            if (claimDao.purgeDeletedItem(itemId)) {
                photo.ifPresent(photoFiles::discard);
            }
        }
    }

    /** My gifts grouped by person or shared list, with my ideas for each person. */
    public MyClaims myClaims(int meId) {
        Map<String, GroupBuilder> groups = new LinkedHashMap<>();
        Totals totals = new Totals();
        for (ClaimRow row : claimDao.myClaims(meId)) {
            GroupBuilder group = row.inList()
                    ? groups.computeIfAbsent(LIST_KEY + row.listId(), key -> GroupBuilder.of(new SharedListRef(row.listId(),
                    row.listName(), row.listBirthday(), peopleService.daysUntil(row.listBirthday()))))
                    : groups.computeIfAbsent(PERSON_KEY + row.ownerId(),
                    key -> GroupBuilder.of(peopleService.person(row.ownerId(), row.ownerName(), row.ownerBirthday())));
            group.claims.add(myClaim(row, meId));
            if (row.givenBy(meId)) {
                totals.addClaim(row);
            }
        }
        for (IdeaWithPerson mine : ideaDao.findAllMine(meId)) {
            Idea idea = mine.idea();
            groups.computeIfAbsent(PERSON_KEY + idea.forUserId(),
                            key -> GroupBuilder.of(peopleService.person(idea.forUserId(), mine.forName(), mine.forBirthday())))
                    .ideas.add(idea);
            totals.addIdea(idea);
        }
        List<ClaimGroup> sorted = groups.values().stream()
                .sorted(GroupBuilder.BIRTHDAY_ORDER)
                .map(GroupBuilder::build)
                .toList();
        return new MyClaims(totals.spent, totals.pending, totals.bought, totals.total, sorted);
    }

    private ResolvedGiver giver(int meId, ClaimRow row, GiverRequest giver) {
        if (giver == null || giver.type() == null) {
            throw RegalosException.badRequest("Falta quién lo regala");
        }
        return switch (giver.type()) {
            case ME -> new ResolvedGiver(meId, null);
            case USER -> {
                Integer userId = giver.userId();
                if (userId == null) {
                    throw RegalosException.badRequest("Falta a quién de la app se lo asignas");
                }
                if (userId == meId) {
                    yield new ResolvedGiver(meId, null);
                }
                if (row.ownedBy(userId)) {
                    throw RegalosException.badRequest("No se le puede asignar a quien lo pide");
                }
                if (peopleService.person(userId).isEmpty()) {
                    throw RegalosException.badRequest("Esa persona no existe");
                }
                if (row.inList() && !listDao.isMember(row.listId(), userId)) {
                    throw RegalosException.badRequest("Esa persona no está invitada a la lista");
                }
                yield new ResolvedGiver(userId, null);
            }
            case EXTERNAL -> {
                String name = giver.name() == null ? "" : giver.name().trim();
                if (name.isEmpty() || name.length() > MAX_GIVER_NAME) {
                    throw RegalosException.badRequest(
                            "El nombre de quien lo regala debe tener entre 1 y " + MAX_GIVER_NAME + " caracteres");
                }
                yield new ResolvedGiver(null, name);
            }
        };
    }

    private static RegalosException alreadyClaimed(ClaimRow row) {
        String message = row.giverName() == null ? "Otra persona acaba de marcarlo" : "Ya lo regala " + row.giverName();
        return new RegalosException(HttpStatus.CONFLICT, "already_claimed", message);
    }

    private static OthersItem othersItem(ClaimRow row, int meId) {
        Claim claim = row.claimed()
                ? new Claim(row.claimState(), new Giver(row.giverUserId(), row.giverName()), row.givenBy(meId),
                row.editableBy(meId))
                : null;
        return new OthersItem(row.id(), row.title(), row.url(), row.store(), row.priceCents(), row.imageUrl(),
                row.notes(), row.wantedALot(), row.inList() ? row.addedByName() : null, claim, row.photoVersion());
    }

    private static MyClaim myClaim(ClaimRow row, int meId) {
        return new MyClaim(row.id(), row.title(), row.url(), row.store(), row.priceCents(), row.imageUrl(),
                row.notes(), row.claimState(), new Giver(row.giverUserId(), row.giverName()), row.givenBy(meId),
                notice(row), row.photoVersion());
    }

    private static Notice notice(ClaimRow row) {
        if (row.status() == ItemStatus.DELETED) {
            return Notice.REMOVED;
        }
        if (row.status() == ItemStatus.RECEIVED) {
            return Notice.RECEIVED;
        }
        return row.editedSinceClaim() ? Notice.EDITED : null;
    }

    private record ResolvedGiver(Integer userId, String name) {
    }

    /** Sums of the gifts I give: claims given by me and my bought ideas. */
    private static final class Totals {
        long spent;
        long pending;
        int bought;
        int total;

        void addClaim(ClaimRow row) {
            total++;
            int price = row.priceCents() == null ? 0 : row.priceCents();
            if (row.claimState() == ClaimState.BOUGHT) {
                bought++;
                spent += price;
            } else if (row.status() == ItemStatus.ACTIVE) {
                pending += price;
            }
        }

        void addIdea(Idea idea) {
            if (idea.bought()) {
                total++;
                bought++;
                spent += idea.priceCents() == null ? 0 : idea.priceCents();
            }
        }
    }

    /** A group being filled: a person or a shared list, and its gifts and ideas. */
    private static final class GroupBuilder {
        static final Comparator<GroupBuilder> BIRTHDAY_ORDER = Comparator
                .comparing(GroupBuilder::daysUntilBirthday, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(GroupBuilder::name, String.CASE_INSENSITIVE_ORDER);

        final Person person;
        final SharedListRef list;
        final List<MyClaim> claims = new ArrayList<>();
        final List<Idea> ideas = new ArrayList<>();

        private GroupBuilder(Person person, SharedListRef list) {
            this.person = person;
            this.list = list;
        }

        static GroupBuilder of(Person person) {
            return new GroupBuilder(person, null);
        }

        static GroupBuilder of(SharedListRef list) {
            return new GroupBuilder(null, list);
        }

        Integer daysUntilBirthday() {
            return person != null ? person.daysUntilBirthday() : list.daysUntilBirthday();
        }

        String name() {
            return person != null ? person.displayName() : list.name();
        }

        ClaimGroup build() {
            return new ClaimGroup(person, list, List.copyOf(claims), List.copyOf(ideas));
        }
    }
}
