package com.magomez.androidapps.movierec.recommendation;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.scoring.PersonalAffinitySignals;
import com.magomez.androidapps.movierec.scoring.PersonalMatchScore;
import com.magomez.androidapps.movierec.scoring.TastePattern;
import com.magomez.androidapps.movierec.scoring.UserTasteProfile;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdLibrary;
import com.magomez.androidapps.movierec.scoring.pattern.NarrativePattern;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds a deterministic, transparent {@link RecommendationReason} for one candidate.
 *
 * <p>Every statement is derived from data we actually hold — the candidate's TMDB
 * metadata ({@code director}, {@code genres}, {@code actors}, {@code overview},
 * {@code countries}, {@code similarMovies}), its computed {@link PersonalAffinitySignals}
 * / {@link com.magomez.androidapps.movierec.scoring.QualityScore}, and the user's
 * {@link UserTasteProfile} (weighted affinities + narrative {@link TastePattern}s) and
 * Letterboxd history. Nothing is invented.
 *
 * <p>Explanation priority (which signals lead and make the cut):
 * <ol>
 *   <li>similar movies already in the user's history;</li>
 *   <li>director;</li>
 *   <li>narrative taste patterns;</li>
 *   <li>genre;</li>
 *   <li>actor — a secondary, testimonial mention only;</li>
 *   <li>external quality — additional information appended at the end.</li>
 * </ol>
 * The text combines the first 2-3 available signals.
 *
 * <p>No LLM: the text is stitched from fixed fragments. A future LLM step may rephrase
 * {@link RecommendationReason#reasons()} into a more natural
 * {@link RecommendationReason#text()} but can neither rank nor add facts.
 */
@Component
public class RecommendationReasoner {

    /** Signal (0-100) at/above which a genre match is a strong one. */
    private static final double STRONG_SIGNAL = 55.0;
    /** Signal (0-100) at/above which a dimension is worth mentioning at all. */
    private static final double MODERATE_SIGNAL = 25.0;
    /** Actor signal (0-100) at/above which the cast overlap is worth a testimonial line. */
    private static final double ACTOR_MENTION_SIGNAL = 20.0;

    private static final double FAVOURITE_WEIGHT = 0.70;
    private static final double FOLLOWED_WEIGHT = 0.30;

    private static final double PATTERN_STRONG = 0.50;
    private static final double PATTERN_PRESENT = 0.20;

    private static final double QUALITY_GREAT = 78.0;
    private static final double QUALITY_SOLID = 68.0;

    private static final int MAX_NAMES = 2;
    private static final int MAX_REASONS_IN_TEXT = 3;

    public RecommendationReason explain(Movie movie, UserTasteProfile profile,
                                        PersonalMatchScore score, SimilaritySignal similaritySignal,
                                        LetterboxdLibrary library) {
        Objects.requireNonNull(movie, "movie");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(similaritySignal, "similaritySignal");
        Objects.requireNonNull(library, "library");

        PersonalAffinitySignals signals = score.affinitySignals();
        List<String> reasons = new ArrayList<>();
        List<PatternSignal> patternSignals = patternSignals(movie, profile);

        // Order here IS the explanation priority:
        //   1 similar  2 director  3 pattern  4 genre  ... then actor (secondary) ... then quality.
        Optional<String> similar = similar(similaritySignal, reasons);
        Optional<String> director = director(movie, profile, reasons);
        Optional<String> pattern = pattern(patternSignals, profile, reasons);
        Optional<String> genre = genre(movie, signals, profile, reasons);
        Optional<String> actor = actor(movie, signals, profile, reasons);
        Optional<String> quality = quality(score, reasons);

        String text = compose(List.of(similar, director, pattern, genre), actor, quality, score);
        return new RecommendationReason(text, reasons, patternSignals);
    }

    /**
     * <b>patternMatch × profileStrength</b> for every active pattern the film fits,
     * ordered by {@code relevance} descending (then id). Diagnostic — not a scoring input.
     */
    List<PatternSignal> patternSignals(Movie movie, UserTasteProfile profile) {
        List<String> genres = names(movie.genres().stream().map(Genre::name).toList());
        List<String> countries = names(movie.countries().stream().map(Country::name).toList());
        return profile.patterns().stream()
                .flatMap(tp -> definitionOf(tp).stream()
                        .map(def -> Map.entry(tp, def.match(genres, countries)))
                        .filter(e -> e.getValue() > 0.0)
                        .map(e -> PatternSignal.of(e.getKey().id(), e.getKey().name(),
                                e.getKey().strength(), e.getValue())))
                .sorted(Comparator.comparingDouble(PatternSignal::relevance).reversed()
                        .thenComparing(PatternSignal::id))
                .toList();
    }

    // --- individual clauses, in priority order --------------------------------

    /** Driven by the (already strong-filtered) {@link SimilaritySignal}; behaviour unchanged. */
    private Optional<String> similar(SimilaritySignal similaritySignal, List<String> reasons) {
        if (!similaritySignal.hasMatches()) {
            return Optional.empty();
        }
        String list = joinNames(similaritySignal.matches().stream()
                .map(SimilarityMatch::watchedTitle)
                .filter(t -> t != null && !t.isBlank())
                .limit(MAX_NAMES)
                .toList());
        if (list.isBlank()) {
            return Optional.empty();
        }
        reasons.add("TMDB la relaciona con películas de tu historial: " + list);
        return Optional.of("tiene elementos similares a " + list + ", que están en tu historial");
    }

    private Optional<String> director(Movie movie, UserTasteProfile profile, List<String> reasons) {
        if (movie.director() == null || movie.director().name() == null
                || movie.director().name().isBlank()) {
            return Optional.empty();
        }
        String name = profile.directorDisplayName(movie.director());
        double weight = profile.directorAffinityOf(movie.director());
        if (weight <= 0.0) {
            return Optional.empty();
        }
        if (weight >= FAVOURITE_WEIGHT) {
            reasons.add("Has valorado muy positivamente a " + name + ", uno de tus directores de cabecera");
            return Optional.of("su director, " + name + ", es uno de tus directores de cabecera");
        }
        if (weight >= FOLLOWED_WEIGHT) {
            reasons.add("Mismo director que otras películas que te gustan: " + name);
            return Optional.of("su director, " + name + ", es alguien a quien ya sigues");
        }
        reasons.add("Su director, " + name + ", ya aparece en tu historial");
        return Optional.of("su director, " + name + ", ya conecta con tu historial");
    }

    /** Leads with the matched pattern of highest {@code relevance} (= profileStrength × patternMatch). */
    private Optional<String> pattern(List<PatternSignal> patternSignals, UserTasteProfile profile,
                                     List<String> reasons) {
        if (patternSignals.isEmpty()) {
            return Optional.empty();
        }
        PatternSignal top = patternSignals.get(0); // already sorted by relevance desc
        Optional<NarrativePattern> def = safeValueOf(top.id());
        Optional<TastePattern> tp = profile.patterns().stream()
                .filter(p -> p.id().equals(top.id())).findFirst();
        if (def.isEmpty() || tp.isEmpty()) {
            return Optional.empty();
        }

        String tail = top.profileStrength() >= PATTERN_STRONG
                ? ", un patrón muy presente entre tus películas mejor valoradas"
                : top.profileStrength() >= PATTERN_PRESENT
                        ? ", un patrón que aparece en tu historial"
                        : ", algo que también aparece en tu historial";

        String example = tp.get().supportingMovies().isEmpty()
                ? "" : " — como en " + tp.get().supportingMovies().get(0);
        reasons.add("Patrón de gusto «" + top.name() + "»" + tail + example);
        return Optional.of(def.get().reasonFragment() + tail);
    }

    private Optional<String> genre(Movie movie, PersonalAffinitySignals signals,
                                   UserTasteProfile profile, List<String> reasons) {
        List<String> matched = matchedByWeight(
                names(movie.genres().stream().map(Genre::name).toList()),
                profile.genreAffinity());
        if (matched.isEmpty()) {
            return Optional.empty();
        }
        String list = joinNames(matched);
        if (signals.genreAffinity() >= STRONG_SIGNAL) {
            reasons.add("Géneros con alta afinidad: " + list);
            return Optional.of("encaja de lleno con tu gusto por " + list);
        }
        if (signals.genreAffinity() >= MODERATE_SIGNAL) {
            reasons.add("Géneros que frecuentas: " + list);
            return Optional.of("toca géneros que frecuentas como " + list);
        }
        return Optional.empty();
    }

    /** Actor is a secondary, testimonial signal — never a lead, phrased as an aside. */
    private Optional<String> actor(Movie movie, PersonalAffinitySignals signals,
                                   UserTasteProfile profile, List<String> reasons) {
        List<String> matched = movie.actors().stream()
                .filter(a -> profile.actorAffinityOf(a) > 0.0)
                .distinct()
                .sorted(Comparator.comparingDouble((Actor a) -> profile.actorAffinityOf(a)).reversed()
                        .thenComparing(a -> a.name() == null ? "" : a.name()))
                .limit(MAX_NAMES)
                .map(profile::actorDisplayName)
                .toList();
        if (matched.isEmpty()) {
            return Optional.empty();
        }
        String list = joinNames(matched);
        if (signals.actorAffinity() >= ACTOR_MENTION_SIGNAL) {
            reasons.add("Reparto presente en varias de tus películas: " + list);
            return Optional.of("comparte reparto contigo (" + list + ")");
        }
        reasons.add("En el reparto aparece " + list + ", que ya has visto");
        return Optional.of("en el reparto aparece " + list + ", que ya has visto");
    }

    private Optional<String> quality(PersonalMatchScore score, List<String> reasons) {
        if (score.qualityScore().value().isEmpty()) {
            return Optional.empty();
        }
        double q = score.qualityScore().value().getAsDouble();
        if (q >= QUALITY_GREAT) {
            reasons.add("Muy buena valoración externa");
            return Optional.of("además, tiene muy buena valoración externa");
        }
        if (q >= QUALITY_SOLID) {
            reasons.add("Valoración externa sólida");
            return Optional.of("además, tiene una valoración externa sólida");
        }
        return Optional.empty();
    }

    // --- composition ---------------------------------------------------------

    private String compose(List<Optional<String>> primaryClauses, Optional<String> actor,
                           Optional<String> quality, PersonalMatchScore score) {
        List<String> primary = primaryClauses.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .toList();

        List<String> chosen = new ArrayList<>(primary.stream().limit(MAX_REASONS_IN_TEXT).toList());
        // Actor is a secondary, testimonial signal: it only reaches the text when there is
        // barely anything stronger to say (never alongside both a director and a pattern/similar).
        if (chosen.size() < 2 && actor.isPresent() && !chosen.contains(actor.get())) {
            chosen.add(actor.get());
        }

        if (chosen.isEmpty()) {
            return score.qualityScore().value().isPresent()
                    ? "Entra sobre todo por su valoración externa; por ahora la conexión con tu "
                            + "perfil es débil."
                    : "Coincidencia limitada con tu perfil y sin valoración externa disponible.";
        }

        StringBuilder text = new StringBuilder("Te la recomendaría porque ")
                .append(naturalJoin(chosen)).append('.');
        if (quality.isPresent() && chosen.size() < MAX_REASONS_IN_TEXT
                && chosen.stream().noneMatch(c -> c.contains("valoración externa"))) {
            text.append(' ').append(capitalize(quality.get())).append('.');
        }
        return text.toString();
    }

    private static String naturalJoin(List<String> fragments) {
        return switch (fragments.size()) {
            case 1 -> fragments.get(0);
            case 2 -> fragments.get(0) + ", y " + fragments.get(1);
            default -> fragments.get(0) + "; " + fragments.get(1) + "; y " + fragments.get(2);
        };
    }

    // --- helpers ------------------------------------------------------------

    private static Optional<NarrativePattern> definitionOf(TastePattern pattern) {
        return safeValueOf(pattern.id());
    }

    private static Optional<NarrativePattern> safeValueOf(String id) {
        try {
            return Optional.of(NarrativePattern.valueOf(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Movie values the user has any preference for, strongest weight first, capped. */
    private static List<String> matchedByWeight(List<String> movieValues, Map<String, Double> affinities) {
        Map<String, Double> weights = lookup(affinities);
        return movieValues.stream()
                .filter(v -> weights.getOrDefault(normalize(v), 0.0) > 0.0)
                .distinct()
                .sorted(Comparator.comparingDouble((String v) -> weights.get(normalize(v))).reversed()
                        .thenComparing(Comparator.naturalOrder()))
                .limit(MAX_NAMES)
                .toList();
    }

    private static Map<String, Double> lookup(Map<String, Double> affinities) {
        Map<String, Double> out = new LinkedHashMap<>();
        affinities.forEach((k, v) -> {
            String n = normalize(k);
            if (!n.isEmpty()) {
                out.merge(n, v, Math::max);
            }
        });
        return out;
    }

    private static List<String> names(List<String> raw) {
        return raw.stream().filter(n -> n != null && !n.isBlank()).map(String::trim).toList();
    }

    private static String joinNames(List<String> names) {
        List<String> capped = names.stream().limit(MAX_NAMES).toList();
        if (capped.isEmpty()) {
            return "";
        }
        return capped.size() == 1 ? capped.get(0) : capped.get(0) + " y " + capped.get(1);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
