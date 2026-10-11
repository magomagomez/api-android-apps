package com.magomez.androidapps.escapersthings.v2.rooms;

import com.magomez.androidapps.escapersthings.v2.auth.UserDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Puts a room's rows together into the v2 shapes. Pure: no database. */
final class RoomAssembler {

    static final String POSTER = "poster";
    static final String GROUP = "group";
    private static final int AVERAGE_SCALE = 2;

    private RoomAssembler() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    /** players, grades and photos are those of this room only. */
    static RoomSummary summary(RoomRows.Room room, List<RoomRows.Player> players,
                               List<RoomRows.UserGradeRow> grades, List<RoomRows.Photo> photos, int userId) {
        Optional<RoomRows.Player> me = players.stream().filter(p -> p.userId() == userId).findFirst();
        List<Grade> scored = scored(grades).map(RoomRows.UserGradeRow::grade).toList();
        Grade myGrade = scored(grades).filter(g -> g.userId() == userId)
                .map(RoomRows.UserGradeRow::grade).findFirst().orElse(null);
        return new RoomSummary(room.id(), room.name(), room.category(), room.city(),
                players.stream().map(RoomRows.Player::userId).sorted().toList(),
                me.isPresent(), me.map(RoomRows.Player::playedOn).orElse(null),
                averages(scored), scored.size(), myGrade,
                photo(photos, POSTER), photo(photos, GROUP));
    }

    static RoomDetail detail(RoomSummary summary, List<RoomRows.UserGradeRow> grades) {
        List<UserGrade> byUser = scored(grades)
                .sorted((a, b) -> Integer.compare(a.userId(), b.userId()))
                .map(g -> new UserGrade(new UserDTO(g.userId(), g.userName()), g.grade()))
                .toList();
        return new RoomDetail(summary, byUser);
    }

    static Averages averages(List<Grade> grades) {
        return new Averages(
                average(grades, Grade::enigma),
                average(grades, Grade::immersion),
                average(grades, Grade::terror),
                average(grades, Grade::gameMaster),
                average(grades, Grade::global));
    }

    /** Mean of the non-null values with 2 decimals (half up), or null when there are none. */
    static BigDecimal average(List<Grade> grades, Function<Grade, BigDecimal> criterion) {
        List<BigDecimal> values = grades.stream().map(criterion).filter(Objects::nonNull).toList();
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), AVERAGE_SCALE, RoundingMode.HALF_UP);
    }

    /** A grade row with every criterion empty counts as not graded. */
    private static java.util.stream.Stream<RoomRows.UserGradeRow> scored(List<RoomRows.UserGradeRow> grades) {
        return grades.stream().filter(g -> g.grade().hasAnyScore());
    }

    private static PhotoRef photo(List<RoomRows.Photo> photos, String kind) {
        return photos.stream().filter(p -> kind.equals(p.kind())).findFirst()
                .map(p -> new PhotoRef(p.etag())).orElse(null);
    }
}
