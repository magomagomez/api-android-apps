package com.magomez.androidapps.movierec.scoring.letterboxd;

import com.magomez.androidapps.movierec.scoring.RatedMovie;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import com.magomez.androidapps.movierec.scoring.UserTasteProfileBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Objects;

/**
 * Builds a {@link UserTasteProfile} straight from a Letterboxd ratings / diary CSV.
 *
 * <p>Pure orchestration — it reuses the existing pieces without re-implementing any of
 * their logic:
 * <ol>
 *   <li>{@link LetterboxdImportService#importRatings(Reader)} — CSV parsing + movie
 *       identification;</li>
 *   <li>keep only the {@code IDENTIFIED} results and take their {@link RatedMovie}s, in
 *       order;</li>
 *   <li>{@link UserTasteProfileBuilder#build(List)} — favourite selection
 *       ({@code userScore >= 7.0}) and preference extraction.</li>
 * </ol>
 *
 * <p>{@code NOT_FOUND} / {@code AMBIGUOUS} entries simply produce no {@link RatedMovie}
 * and therefore do not reach the builder. No persistence, no HTTP.
 */
@Service
public class LetterboxdTasteProfileService {

    private final LetterboxdImportService letterboxdImportService;
    private final UserTasteProfileBuilder userTasteProfileBuilder;

    public LetterboxdTasteProfileService(LetterboxdImportService letterboxdImportService,
                                         UserTasteProfileBuilder userTasteProfileBuilder) {
        this.letterboxdImportService = letterboxdImportService;
        this.userTasteProfileBuilder = userTasteProfileBuilder;
    }

    /**
     * @throws IOException            if reading the CSV fails
     * @throws LetterboxdCsvException if the CSV is not usable (propagated)
     */
    public UserTasteProfile buildProfile(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "reader");

        List<RatedMovie> identifiedFavourites = letterboxdImportService.importRatings(reader).stream()
                .filter(LetterboxdImportResult::isIdentified)
                .map(LetterboxdImportResult::ratedMovie)
                .toList();

        return userTasteProfileBuilder.build(identifiedFavourites);
    }
}
