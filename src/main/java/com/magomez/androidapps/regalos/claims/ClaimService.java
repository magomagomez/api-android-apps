package com.magomez.androidapps.regalos.claims;

import com.magomez.androidapps.regalos.config.RegalosException;
import com.magomez.androidapps.regalos.items.ItemStatus;
import com.magomez.androidapps.regalos.people.PeopleService;
import com.magomez.androidapps.regalos.people.Person;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Giving. The owner of a wish never gets past the first check of any method here, so nothing
 * of its claim can reach them (R1).
 */
@Service
public class ClaimService {

    static final int MAX_GIVER_NAME = 60;

    private static final String ITEM_NOT_FOUND = "El deseo no existe";
    private static final String OWN_WISH = "Es un deseo tuyo";

    private final ClaimDao claimDao;
    private final PeopleService peopleService;

    public ClaimService(ClaimDao claimDao, PeopleService peopleService) {
        this.claimDao = claimDao;
        this.peopleService = peopleService;
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

    /** Creates the claim, or changes it when it is mine to change (R4); anyone else's is a 409 (R2). */
    public OthersItem claim(int meId, long itemId, ClaimRequest request) {
        ClaimRow row = claimDao.item(itemId)
                .filter(found -> found.status() == ItemStatus.ACTIVE)
                .orElseThrow(() -> RegalosException.notFound(ITEM_NOT_FOUND));
        if (row.ownerId() == meId) {
            throw RegalosException.forbidden(OWN_WISH);
        }
        if (request == null || request.state() == null) {
            throw RegalosException.badRequest("Falta si está reservado o comprado");
        }
        ResolvedGiver giver = giver(meId, row.ownerId(), request.giver());
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
        ClaimRow row = claimDao.item(itemId).orElseThrow(() -> RegalosException.notFound(ITEM_NOT_FOUND));
        if (row.ownerId() == meId) {
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
            claimDao.purgeDeletedItem(itemId);
        }
    }

    public MyClaims myClaims(int meId) {
        Map<Integer, List<MyClaim>> byOwner = new LinkedHashMap<>();
        Map<Integer, Person> owners = new LinkedHashMap<>();
        long spent = 0;
        long pending = 0;
        int bought = 0;
        int total = 0;
        for (ClaimRow row : claimDao.myClaims(meId)) {
            owners.computeIfAbsent(row.ownerId(),
                    id -> peopleService.person(id, row.ownerName(), row.ownerBirthday()));
            byOwner.computeIfAbsent(row.ownerId(), id -> new ArrayList<>()).add(myClaim(row, meId));
            if (row.givenBy(meId)) {
                total++;
                int price = row.priceCents() == null ? 0 : row.priceCents();
                if (row.claimState() == ClaimState.BOUGHT) {
                    bought++;
                    spent += price;
                } else if (row.status() == ItemStatus.ACTIVE) {
                    pending += price;
                }
            }
        }
        List<ClaimGroup> groups = owners.values().stream()
                .sorted(PeopleService.BIRTHDAY_ORDER)
                .map(person -> new ClaimGroup(person, List.copyOf(byOwner.get(person.id()))))
                .toList();
        return new MyClaims(spent, pending, bought, total, groups);
    }

    private ResolvedGiver giver(int meId, int ownerId, GiverRequest giver) {
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
                if (userId == ownerId) {
                    throw RegalosException.badRequest("No se le puede asignar a quien lo pide");
                }
                if (peopleService.person(userId).isEmpty()) {
                    throw RegalosException.badRequest("Esa persona no existe");
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
                row.notes(), row.wantedALot(), claim);
    }

    private static MyClaim myClaim(ClaimRow row, int meId) {
        return new MyClaim(row.id(), row.title(), row.url(), row.store(), row.priceCents(), row.imageUrl(),
                row.notes(), row.claimState(), new Giver(row.giverUserId(), row.giverName()), row.givenBy(meId),
                notice(row));
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
}
