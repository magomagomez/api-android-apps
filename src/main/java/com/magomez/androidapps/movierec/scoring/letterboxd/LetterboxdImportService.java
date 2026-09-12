package com.magomez.androidapps.movierec.scoring.letterboxd;

import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.provider.MovieDataProvider;
import com.magomez.androidapps.movierec.provider.MovieProviderException;
import com.magomez.androidapps.movierec.scoring.RatedMovie;
import com.magomez.androidapps.movierec.support.ExternalCallExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Objects;

/**
 * Bridges a Letterboxd ratings / diary CSV to the existing movie identification.
 *
 * <p>Per entry: {@link LetterboxdRatedMovie} &rarr; {@link MovieQuery} (title + year,
 * {@code director = null}) &rarr; {@link MovieDataProvider#identify(MovieQuery)} &rarr;
 * a {@link LetterboxdImportResult}. When the movie is {@code IDENTIFIED} the result also
 * carries a {@link RatedMovie} (the identified movie + the Letterboxd userScore).
 *
 * <p>It reuses the shared provider's identification as-is: no alternative matching, no
 * lookup by Letterboxd URI, no OMDb, no title search of its own. Every CSV entry yields
 * a result in CSV order; a per-entry identification failure is recorded and does not
 * stop the rest. Parser failures propagate.
 */
@Service
public class LetterboxdImportService {

    private static final Logger log = LoggerFactory.getLogger(LetterboxdImportService.class);

    private final MovieDataProvider movieDataProvider;
    private final LetterboxdCsvParser letterboxdCsvParser;
    private final ExternalCallExecutor externalCallExecutor;

    @Autowired
    public LetterboxdImportService(MovieDataProvider movieDataProvider,
                                   LetterboxdCsvParser letterboxdCsvParser,
                                   ExternalCallExecutor externalCallExecutor) {
        this.movieDataProvider = movieDataProvider;
        this.letterboxdCsvParser = letterboxdCsvParser;
        this.externalCallExecutor = externalCallExecutor;
    }

    /** Test constructor: identifies entries inline, on the caller thread. */
    public LetterboxdImportService(MovieDataProvider movieDataProvider,
                                   LetterboxdCsvParser letterboxdCsvParser) {
        this(movieDataProvider, letterboxdCsvParser, ExternalCallExecutor.sequential());
    }

    /**
     * @throws IOException            if reading the CSV fails
     * @throws LetterboxdCsvException if the CSV is not usable (propagated from the parser)
     */
    public List<LetterboxdImportResult> importRatings(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "reader");

        List<LetterboxdRatedMovie> entries = letterboxdCsvParser.parse(reader);
        return List.copyOf(externalCallExecutor.map(entries, this::resolve));
    }

    private LetterboxdImportResult resolve(LetterboxdRatedMovie entry) {
        MovieMatch match;
        try {
            match = movieDataProvider.identify(new MovieQuery(entry.title(), entry.year(), null));
        } catch (MovieProviderException e) {
            log.warn("Provider {} failed to identify Letterboxd entry '{}': {}",
                    movieDataProvider.sourceName(), entry.title(), e.getMessage());
            return LetterboxdImportResult.identificationFailed(entry, e.getMessage());
        }

        return switch (match.status()) {
            case IDENTIFIED -> LetterboxdImportResult.identified(entry,
                    new RatedMovie(match.movie(), entry.userScore()));
            case NOT_FOUND -> LetterboxdImportResult.notFound(entry);
            case AMBIGUOUS -> LetterboxdImportResult.ambiguous(entry, match.candidates());
        };
    }
}
