package com.magomez.androidapps.movierec.provider.festival.wikipedia;

import com.magomez.androidapps.movierec.provider.festival.FestivalLineup;
import com.magomez.androidapps.movierec.provider.festival.FestivalLineupSource;
import com.magomez.androidapps.movierec.provider.festival.LineupEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * {@link FestivalLineupSource} backed by the English Wikipedia article for a festival
 * edition (e.g. <i>2026 Sundance Film Festival</i>). It reads every {@code table.wikitable}
 * on the page and takes each row's first cell as a film title, tagged with the nearest
 * preceding section heading.
 *
 * <p>The extraction is deliberately liberal — a non-film row (a jury member, a statistic)
 * simply never matches a candidate title, so it is harmless. Titles are matched, not
 * trusted.
 */
@Component
public class WikipediaFestivalLineupSource implements FestivalLineupSource {

    private static final Logger log = LoggerFactory.getLogger(WikipediaFestivalLineupSource.class);
    private static final int MAX_ENTRIES = 2000;

    /** festival key (lower-case) &rarr; the "{year} X" article-title suffix. */
    private static final Map<String, String> ARTICLE_SUFFIX = Map.ofEntries(
            Map.entry("sundance", "Sundance Film Festival"),
            Map.entry("cannes", "Cannes Film Festival"),
            Map.entry("venice", "Venice Film Festival"),
            Map.entry("venezia", "Venice Film Festival"),
            Map.entry("berlin", "Berlin International Film Festival"),
            Map.entry("berlinale", "Berlin International Film Festival"),
            Map.entry("locarno", "Locarno Film Festival"),
            Map.entry("sxsw", "South by Southwest"),
            Map.entry("fantasia", "Fantasia International Film Festival"),
            Map.entry("sitges", "Sitges Film Festival"),
            Map.entry("toronto", "Toronto International Film Festival"),
            Map.entry("tiff", "Toronto International Film Festival"),
            Map.entry("tribeca", "Tribeca Festival"),
            Map.entry("rotterdam", "International Film Festival Rotterdam"),
            Map.entry("san sebastian", "San Sebastián International Film Festival"));

    private final WikipediaClient client;

    public WikipediaFestivalLineupSource(WikipediaClient client) {
        this.client = client;
    }

    @Override
    public Optional<FestivalLineup> lineup(String festival, int editionYear) throws IOException {
        String page = editionYear + " " + articleSuffix(festival);
        Optional<String> html = client.pageHtml(page);
        if (html.isEmpty()) {
            log.info("No Wikipedia page for '{}'", page);
            return Optional.empty();
        }
        List<LineupEntry> entries = parse(html.get());
        return entries.isEmpty() ? Optional.empty()
                : Optional.of(new FestivalLineup(canonicalName(festival), editionYear, entries));
    }

    private static String articleSuffix(String festival) {
        return ARTICLE_SUFFIX.getOrDefault(festival.toLowerCase(Locale.ROOT).trim(),
                festival.trim() + " Film Festival");
    }

    private static String canonicalName(String festival) {
        String trimmed = festival.trim();
        return trimmed.isEmpty() ? "Festival" : Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    static List<LineupEntry> parse(String html) {
        Document doc = Jsoup.parse(html);
        List<LineupEntry> entries = new ArrayList<>();
        String currentSection = null;

        Elements nodes = doc.select("h2, h3, h4, table.wikitable");
        for (Element node : nodes) {
            String tag = node.tagName();
            if (tag.equals("table")) {
                collectRows(node, currentSection, entries);
                if (entries.size() >= MAX_ENTRIES) {
                    break;
                }
            } else {
                currentSection = headingText(node);
            }
        }
        return entries;
    }

    private static void collectRows(Element table, String section, List<LineupEntry> out) {
        for (Element row : table.select("tr")) {
            Element cell = row.selectFirst("td");
            if (cell == null) {
                continue; // header row
            }
            String title = titleOf(cell);
            if (!title.isBlank()) {
                out.add(new LineupEntry(title, section));
            }
        }
    }

    private static String titleOf(Element cell) {
        for (String selector : new String[]{"i a", "i", "a"}) {
            Element match = cell.selectFirst(selector);
            if (match != null && !match.text().isBlank()) {
                return match.text().trim();
            }
        }
        return cell.text().trim();
    }

    private static String headingText(Element heading) {
        Element headline = heading.selectFirst("span.mw-headline");
        String text = headline != null ? headline.text() : heading.text();
        return text.replace("[edit]", "").trim();
    }
}
