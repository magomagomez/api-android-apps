package com.magomez.androidapps.regalos.people;

import com.magomez.androidapps.regalos.config.RegalosException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PeopleService {

    /** Nearest birthday first, those without one last, then by name. */
    public static final Comparator<Person> BIRTHDAY_ORDER = Comparator
            .comparing(Person::daysUntilBirthday, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Person::displayName, String.CASE_INSENSITIVE_ORDER);

    private static final String NO_USER = "La cuenta no existe";

    private final RegalosUserDao userDao;
    private final Clock clock;

    public PeopleService(RegalosUserDao userDao, @Qualifier("regalosClock") Clock clock) {
        this.userDao = userDao;
        this.clock = clock;
    }

    public Me me(int meId) {
        return userDao.findMe(meId).orElseThrow(() -> RegalosException.notFound(NO_USER));
    }

    public Me updateMe(int meId, ProfileRequest request) {
        if (request == null) {
            throw RegalosException.badRequest("Faltan los datos del perfil");
        }
        String name = ProfileRules.displayName(request.displayName());
        Birthday birthday = Birthday.valid(request.birthday());
        if (!userDao.updateProfile(meId, name, birthday)) {
            throw RegalosException.notFound(NO_USER);
        }
        return me(meId);
    }

    /** Everybody but me, in {@link #BIRTHDAY_ORDER}. */
    public List<PersonSummary> people(int meId) {
        LocalDate today = today();
        return userDao.findOthers(meId).stream()
                .sorted(Comparator.comparing(row -> person(row.id(), row.displayName(), row.birthday(), today),
                        BIRTHDAY_ORDER))
                .map(row -> new PersonSummary(row.id(), row.displayName(), row.birthday(),
                        daysUntil(row.birthday(), today), row.wishCount(), row.freeCount(), row.mineCount()))
                .toList();
    }

    public Optional<Person> person(int id) {
        LocalDate today = today();
        return userDao.findMe(id).map(user -> person(user.id(), user.displayName(), user.birthday(), today));
    }

    /** For callers that already read the name and birthday with their own query. */
    public Person person(int id, String displayName, Birthday birthday) {
        return person(id, displayName, birthday, today());
    }

    private static Person person(int id, String displayName, Birthday birthday, LocalDate today) {
        return new Person(id, displayName, birthday, daysUntil(birthday, today));
    }

    private static Integer daysUntil(Birthday birthday, LocalDate today) {
        return birthday == null ? null : birthday.daysUntil(today);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
