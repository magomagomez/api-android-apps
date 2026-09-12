package com.magomez.androidapps.movierec.api;

import com.magomez.androidapps.movierec.recommendation.ExclusionReason;

import java.util.List;

/**
 * Response of {@code POST /api/recommendations}. Web contract only, built from the domain
 * {@link com.magomez.androidapps.movierec.recommendation.RecommendationResult} by
 * {@link RecommendationApiMapper}.
 *
 * <p>Not a final frontend shape — it is meant to be actually read, so it favours
 * tangible, human-facing facts (a poster, per-source ratings, real festival selections,
 * a plain-language reason) over the raw internals of how the score was computed.
 *
 * @param summary         counts for the whole request
 * @param recommendations every identified, unseen candidate, in <b>one</b> list ordered by
 *                        {@code personalMatchScore} descending
 * @param excluded        every candidate removed before ranking, with the reason
 */
public record RecommendationResponse(
        Summary summary,
        List<RecommendationItem> recommendations,
        List<ExcludedItem> excluded) {

    public RecommendationResponse {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        excluded = excluded == null ? List.of() : List.copyOf(excluded);
    }

    /**
     * @param totalCandidates               entries in the request
     * @param identified                    candidates resolved to exactly one TMDB movie
     * @param notFound                      candidates with no match
     * @param ambiguous                     candidates with several plausible matches
     * @param alreadyWatched                candidates already in the Letterboxd library
     * @param duplicateCandidate            candidates dropped because an earlier candidate
     *                                      resolved to the same TMDB id
     * @param recommended                   candidates in {@link #recommendations()}
     * @param withQualityScore              of those, how many have trustworthy external ratings
     * @param withCompletePersonalMatchScore of those, how many have a full, confirmed
     *                                       PERSONAL MATCH SCORE (quality and affinity);
     *                                       the rest carry a provisional estimate instead
     *                                       (see {@link RecommendationItem#qualityConfirmed()})
     */
    public record Summary(
            int totalCandidates,
            int identified,
            int notFound,
            int ambiguous,
            int alreadyWatched,
            int duplicateCandidate,
            int recommended,
            int withQualityScore,
            int withCompletePersonalMatchScore) {
    }

    /**
     * One recommended movie: the tangible facts behind it and the explanation. Deliberately
     * leaves out the scoring internals (the individual quality/affinity/bonus components,
     * the raw pattern-match and similarity numbers) — {@code recommendationReason} /
     * {@code reasons} already say, in plain language, what earned it its place.
     *
     * @param position             1-based rank in the unified list
     * @param title                the requested title
     * @param tmdbId               TMDB id
     * @param imdbId               IMDb id, or {@code null}
     * @param posterUrl            poster image URL, or {@code null} when TMDB has none
     * @param year                 release year, or {@code null}
     * @param synopsis             the movie's overview from TMDB, or {@code null}
     * @param director             director name, or {@code null}
     * @param actors               principal cast (TMDB order), may be empty
     * @param genres               genres, may be empty
     * @param ratings              per-source ratings as reported (IMDb, Rotten Tomatoes,
     *                             Metacritic, TMDB…), each on its own native scale; empty
     *                             when none is available yet. FilmAffinity / Letterboxd
     *                             community ratings are not gathered yet — never invented
     * @param personalMatchScore   the score this list is ordered by, in {@code [0, 100]};
     *                             {@code null} only when even the personal affinity is
     *                             unknown (e.g. an empty taste profile)
     * @param qualityConfirmed     {@code true} when {@code personalMatchScore} is a full,
     *                             confirmed PERSONAL MATCH SCORE backed by trustworthy
     *                             external ratings; {@code false} when it is a provisional
     *                             estimate for a candidate with no trustworthy rating yet
     *                             (e.g. an unreleased film) — still real, just not final
     * @param festivalRecognition  specific festival selections / awards found for this film
     * @param accoladeHighlights   short human-readable recognition phrases ("Ganó el Óscar",
     *                             "Selección oficial de Sundance 2026")
     * @param reasons              the structured facts behind the recommendation, in plain language
     * @param recommendationReason the composed, deterministic explanation
     * @param note                 optional diagnostic (e.g. a partial enrichment failure)
     */
    public record RecommendationItem(
            int position,
            String title,
            Integer tmdbId,
            String imdbId,
            String posterUrl,
            Integer year,
            String synopsis,
            String director,
            List<String> actors,
            List<String> genres,
            List<RatingView> ratings,
            Double personalMatchScore,
            boolean qualityConfirmed,
            List<FestivalAchievementView> festivalRecognition,
            List<String> accoladeHighlights,
            List<String> reasons,
            String recommendationReason,
            String note) {

        public RecommendationItem {
            actors = actors == null ? List.of() : List.copyOf(actors);
            genres = genres == null ? List.of() : List.copyOf(genres);
            ratings = ratings == null ? List.of() : List.copyOf(ratings);
            festivalRecognition = festivalRecognition == null ? List.of() : List.copyOf(festivalRecognition);
            accoladeHighlights = accoladeHighlights == null ? List.of() : List.copyOf(accoladeHighlights);
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
        }
    }

    /**
     * One external source's rating, exactly as reported — scales are never mixed.
     *
     * @param source    e.g. {@code "IMDb"}, {@code "Rotten Tomatoes"}, {@code "Metacritic"}, {@code "TMDB"}
     * @param score     the value on that source's own scale
     * @param scale     the maximum of that scale (e.g. {@code 10} for IMDb, {@code 100} for
     *                  Rotten Tomatoes), or {@code null} for a source this API doesn't know the scale of
     * @param voteCount number of votes backing it, or {@code null} when the source doesn't report one
     */
    public record RatingView(String source, double score, Integer scale, Integer voteCount) {
    }

    /**
     * @param festival    festival name, e.g. {@code "Cannes"}
     * @param editionYear the edition year
     * @param section     the section, or {@code null}
     * @param type        {@code SELECTION} or {@code AWARD}
     * @param awardName   the award name for an {@code AWARD}, otherwise {@code null}
     */
    public record FestivalAchievementView(
            String festival,
            int editionYear,
            String section,
            String type,
            String awardName) {
    }

    /**
     * @param title      the requested title
     * @param tmdbId     resolved TMDB id when identified, otherwise {@code null}
     * @param reason     {@code NOT_FOUND} / {@code AMBIGUOUS} / {@code ALREADY_WATCHED} /
     *                   {@code DUPLICATE_CANDIDATE}
     * @param candidates competing matches, present only when {@code reason == AMBIGUOUS}
     * @param note       human-readable explanation
     */
    public record ExcludedItem(
            String title,
            Integer tmdbId,
            ExclusionReason reason,
            List<String> candidates,
            String note) {

        public ExcludedItem {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
        }
    }
}
