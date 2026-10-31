package com.magomez.androidapps.regalos.lists;

import com.magomez.androidapps.regalos.claims.ClaimService;
import com.magomez.androidapps.regalos.claims.OthersItem;
import com.magomez.androidapps.regalos.config.RegalosException;
import com.magomez.androidapps.regalos.items.ItemRequest;
import com.magomez.androidapps.regalos.items.ItemRules;
import com.magomez.androidapps.regalos.people.Birthday;
import com.magomez.androidapps.regalos.people.PeopleService;
import com.magomez.androidapps.regalos.people.Person;
import com.magomez.androidapps.regalos.photos.PhotoFiles;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lists for someone outside the app. To anyone not invited a list does not exist (R8): every
 * method answers 404 for them before anything else.
 */
@Service
public class ListService {

    static final int MAX_NAME = 60;

    private static final String NOT_FOUND = "La lista no existe";
    private static final Comparator<ListSummary> BIRTHDAY_ORDER = Comparator
            .comparing(ListSummary::daysUntilBirthday, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(ListSummary::name, String.CASE_INSENSITIVE_ORDER);

    private final ListDao listDao;
    private final ClaimService claimService;
    private final PeopleService peopleService;
    private final PhotoFiles photoFiles;

    public ListService(ListDao listDao, ClaimService claimService, PeopleService peopleService,
                       PhotoFiles photoFiles) {
        this.listDao = listDao;
        this.claimService = claimService;
        this.peopleService = peopleService;
        this.photoFiles = photoFiles;
    }

    /** The lists I am invited to, nearest birthday first. */
    public List<ListSummary> lists(int meId) {
        return listDao.findMine(meId).stream().map(row -> summary(row, meId)).sorted(BIRTHDAY_ORDER).toList();
    }

    public ListDetail create(int meId, ListRequest request) {
        String name = name(request);
        Birthday birthday = Birthday.valid(request.birthday());
        Set<Integer> members = members(request, meId);
        long id = listDao.insert(name, birthday, meId);
        members.forEach(member -> listDao.addMember(id, member));
        return detail(meId, id);
    }

    public ListDetail detail(int meId, long listId) {
        ListRow row = requireMember(meId, listId);
        List<Person> members = listDao.members(listId).stream()
                .map(member -> peopleService.person(member.id(), member.displayName(), member.birthday()))
                .toList();
        return new ListDetail(summary(row, meId), members, claimService.listItems(meId, listId));
    }

    /** Name, birthday and members; members dropped from the list take their gifts with them. */
    public ListDetail update(int meId, long listId, ListRequest request) {
        ListRow row = requireCreator(meId, listId);
        String name = name(request);
        Birthday birthday = Birthday.valid(request.birthday());
        Set<Integer> wanted = members(request, row.createdById());
        listDao.update(listId, name, birthday);
        List<Integer> current = listDao.members(listId).stream().map(ListDao.Member::id).toList();
        current.stream().filter(id -> !wanted.contains(id)).forEach(id -> listDao.removeMember(listId, id));
        wanted.stream().filter(id -> !current.contains(id)).forEach(id -> listDao.addMember(listId, id));
        return detail(meId, listId);
    }

    /** The list goes with its wishes, their claims and their photos. */
    public void delete(int meId, long listId) {
        requireCreator(meId, listId);
        List<String> photos = photoFiles.ofList(listId);
        listDao.delete(listId);
        photoFiles.discard(photos);
    }

    public void leave(int meId, long listId) {
        ListRow row = requireMember(meId, listId);
        if (row.createdById() == meId) {
            throw RegalosException.badRequest("Quien creó la lista no puede salirse; si ya no hace falta, bórrala");
        }
        listDao.removeMember(listId, meId);
    }

    public OthersItem addItem(int meId, long listId, ItemRequest request) {
        requireMember(meId, listId);
        long itemId = listDao.insertItem(listId, meId, ItemRules.valid(request));
        return claimService.item(meId, itemId);
    }

    public OthersItem updateItem(int meId, long listId, long itemId, ItemRequest request) {
        requireMember(meId, listId);
        if (!listDao.updateItem(listId, itemId, ItemRules.valid(request))) {
            throw RegalosException.notFound("El deseo no existe");
        }
        return claimService.item(meId, itemId);
    }

    public void deleteItem(int meId, long listId, long itemId) {
        requireMember(meId, listId);
        Optional<String> photo = photoFiles.ofItem(itemId);
        if (listDao.softDeleteItem(listId, itemId)) {
            return;
        }
        if (!listDao.hardDeleteItem(listId, itemId)) {
            throw RegalosException.notFound("El deseo no existe");
        }
        photo.ifPresent(photoFiles::discard);
    }

    private ListRow requireMember(int meId, long listId) {
        if (!listDao.isMember(listId, meId)) {
            throw RegalosException.notFound(NOT_FOUND);
        }
        return listDao.find(meId, listId).orElseThrow(() -> RegalosException.notFound(NOT_FOUND));
    }

    private ListRow requireCreator(int meId, long listId) {
        ListRow row = requireMember(meId, listId);
        if (row.createdById() != meId) {
            throw RegalosException.forbidden("Solo quien creó la lista puede cambiarla o borrarla");
        }
        return row;
    }

    private ListSummary summary(ListRow row, int meId) {
        return new ListSummary(row.id(), row.name(), row.birthday(), peopleService.daysUntil(row.birthday()),
                new ListCreator(row.createdById(), row.createdByName()), row.memberCount(), row.wishCount(),
                row.freeCount(), row.mineCount(), row.createdById() == meId);
    }

    private static String name(ListRequest request) {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > MAX_NAME) {
            throw RegalosException.badRequest("El nombre de la lista debe tener entre 1 y " + MAX_NAME + " caracteres");
        }
        return name;
    }

    /** The requested people (who must exist) plus the creator, without repeats. */
    private Set<Integer> members(ListRequest request, int creatorId) {
        Set<Integer> members = new LinkedHashSet<>();
        members.add(creatorId);
        List<Integer> requested = request.memberIds() == null ? List.of() : request.memberIds();
        for (Integer id : requested) {
            if (id == null || (id != creatorId && peopleService.person(id).isEmpty())) {
                throw RegalosException.badRequest("Esa persona no existe");
            }
            members.add(id);
        }
        return members;
    }
}
