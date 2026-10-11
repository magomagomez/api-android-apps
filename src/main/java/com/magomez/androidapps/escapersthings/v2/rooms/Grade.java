package com.magomez.androidapps.escapersthings.v2.rooms;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * One person's scores for a room, every one optional. In the database: enigma, inmersion,
 * horror, game_master and global of user_categoria_nota. Reads accept any stored numeric;
 * only writes are held to 0-10 in quarter points.
 */
public record Grade(BigDecimal enigma, BigDecimal immersion, BigDecimal terror, BigDecimal gameMaster,
                    BigDecimal global) {

    public boolean hasAnyScore() {
        return Stream.of(enigma, immersion, terror, gameMaster, global).anyMatch(Objects::nonNull);
    }
}
