package com.magomez.androidapps.movierec.scoring.letterboxd;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a Letterboxd ratings / diary CSV export into {@link LetterboxdRatedMovie}s.
 *
 * <p>Expected shape: {@code Date,Name,Year,Letterboxd URI,Rating} (RFC-4180 style —
 * quoted fields, commas inside quotes, UTF-8). Columns are located <b>by name</b>, not
 * by position; {@code Date} is ignored.
 *
 * <ul>
 *   <li>the header row is mandatory; a missing required column ({@code Name},
 *       {@code Year}, {@code Letterboxd URI}, {@code Rating}) is a clear error;</li>
 *   <li>empty {@code Name} &rarr; error; an empty / non-numeric / out-of-range
 *       ({@code 0.5}-{@code 5.0}) {@code Rating} &rarr; error — no rating is invented;</li>
 *   <li>empty {@code Year} &rarr; {@code null}; empty {@code Letterboxd URI} &rarr;
 *       {@code null};</li>
 *   <li>{@code Name} and {@code Letterboxd URI} are preserved verbatim;</li>
 *   <li>{@code Rating} is converted to the 0-10 scale ({@code rating * 2}).</li>
 * </ul>
 *
 * <p>No API calls, no scraping, no movie identification, no persistence.
 */
@Component
public class LetterboxdCsvParser {

    private static final String COL_NAME = "Name";
    private static final String COL_YEAR = "Year";
    private static final String COL_URI = "Letterboxd URI";
    private static final String COL_RATING = "Rating";
    private static final String[] REQUIRED_COLUMNS = {COL_NAME, COL_YEAR, COL_URI, COL_RATING};

    private static final double MIN_RATING = 0.5;
    private static final double MAX_RATING = 5.0;
    private static final double TO_TEN_SCALE = 2.0;

    /**
     * @param reader a {@link Reader} over the UTF-8 CSV text (already decoded); caller
     *               owns it and should close it
     * @return the parsed rows, in file order (immutable)
     * @throws IOException            if reading from {@code reader} fails
     * @throws LetterboxdCsvException if the CSV is not usable
     */
    public List<LetterboxdRatedMovie> parse(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException("reader must not be null");
        }

        List<List<String>> rows = readCsv(reader);
        if (rows.isEmpty()) {
            throw new LetterboxdCsvException("CSV has no header row");
        }

        Map<String, Integer> columns = headerIndex(rows.get(0));
        requireColumns(columns);

        List<LetterboxdRatedMovie> parsed = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }
            parsed.add(toRatedMovie(row, columns, i + 1));
        }
        return List.copyOf(parsed);
    }

    private static LetterboxdRatedMovie toRatedMovie(List<String> row, Map<String, Integer> columns,
                                                     int lineNumber) {
        String name = cell(row, columns.get(COL_NAME));
        if (name == null || name.isBlank()) {
            throw new LetterboxdCsvException("line " + lineNumber + ": Name is empty");
        }

        Integer year = parseYear(cell(row, columns.get(COL_YEAR)), lineNumber);
        String uri = blankToNull(cell(row, columns.get(COL_URI)));
        double rating = parseRating(cell(row, columns.get(COL_RATING)), lineNumber);

        return new LetterboxdRatedMovie(name, year, uri, rating * TO_TEN_SCALE);
    }

    private static Integer parseYear(String raw, int lineNumber) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new LetterboxdCsvException("line " + lineNumber + ": invalid Year '" + raw + "'");
        }
    }

    private static double parseRating(String raw, int lineNumber) {
        if (raw == null || raw.isBlank()) {
            throw new LetterboxdCsvException("line " + lineNumber + ": Rating is empty");
        }
        double rating;
        try {
            rating = Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new LetterboxdCsvException(
                    "line " + lineNumber + ": Rating '" + raw + "' is not a number");
        }
        if (!Double.isFinite(rating) || rating < MIN_RATING || rating > MAX_RATING) {
            throw new LetterboxdCsvException(
                    "line " + lineNumber + ": Rating " + raw + " is outside " + MIN_RATING + "-" + MAX_RATING);
        }
        return rating;
    }

    private static Map<String, Integer> headerIndex(List<String> header) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            index.putIfAbsent(header.get(i).trim(), i);
        }
        return index;
    }

    private static void requireColumns(Map<String, Integer> columns) {
        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_COLUMNS) {
            if (!columns.containsKey(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new LetterboxdCsvException("missing required column(s): " + String.join(", ", missing));
        }
    }

    private static String cell(List<String> row, Integer index) {
        if (index == null || index >= row.size()) {
            return null;
        }
        return row.get(index);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(s -> s == null || s.isEmpty());
    }

    // --- minimal RFC-4180 reader -------------------------------------------------

    private static List<List<String>> readCsv(Reader reader) throws IOException {
        String content = stripBom(readAll(reader));

        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                current.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                current.add(field.toString());
                field.setLength(0);
                rows.add(current);
                current = new ArrayList<>();
            } else {
                field.append(c);
            }
        }

        current.add(field.toString());
        if (!(current.size() == 1 && current.get(0).isEmpty())) {
            rows.add(current);
        }
        return rows;
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, read);
        }
        return sb.toString();
    }

    private static final char BOM = '\uFEFF';

    private static String stripBom(String content) {
        return (!content.isEmpty() && content.charAt(0) == BOM) ? content.substring(1) : content;
    }
}
