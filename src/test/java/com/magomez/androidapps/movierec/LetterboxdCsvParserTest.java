package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdCsvException;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdCsvParser;
import com.magomez.androidapps.movierec.scoring.letterboxd.LetterboxdRatedMovie;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LetterboxdCsvParserTest {

    private final LetterboxdCsvParser parser = new LetterboxdCsvParser();

    private List<LetterboxdRatedMovie> parse(String csv) throws Exception {
        return parser.parse(new StringReader(csv));
    }

    private static final String HEADER = "Date,Name,Year,Letterboxd URI,Rating\n";

    @Test
    void parsesMultipleRows() throws Exception {
        List<LetterboxdRatedMovie> rows = parse(HEADER
                + "2025-12-31,Love Hurts,2025,https://boxd.it/KyBc,2.5\n"
                + "2026-03-08,\"tick, tick... BOOM!\",2021,https://boxd.it/jz2e,4\n");

        assertThat(rows).containsExactly(
                new LetterboxdRatedMovie("Love Hurts", 2025, "https://boxd.it/KyBc", 5.0),
                new LetterboxdRatedMovie("tick, tick... BOOM!", 2021, "https://boxd.it/jz2e", 8.0));
    }

    @Test
    void rating4BecomesUserScore8() throws Exception {
        assertThat(parse(HEADER + "d,Name,2020,uri,4\n").get(0).userScore()).isEqualTo(8.0);
    }

    @Test
    void rating3Point5BecomesUserScore7() throws Exception {
        assertThat(parse(HEADER + "d,Name,2020,uri,3.5\n").get(0).userScore()).isEqualTo(7.0);
    }

    @Test
    void rating2Point5BecomesUserScore5() throws Exception {
        assertThat(parse(HEADER + "d,Name,2020,uri,2.5\n").get(0).userScore()).isEqualTo(5.0);
    }

    @Test
    void keepsTitlesWithCommasInsideQuotes() throws Exception {
        LetterboxdRatedMovie row = parse(HEADER
                + "2026-03-08,\"tick, tick... BOOM!\",2021,uri,4\n").get(0);

        assertThat(row.title()).isEqualTo("tick, tick... BOOM!");
    }

    @Test
    void locatesColumnsByNameNotByPosition() throws Exception {
        List<LetterboxdRatedMovie> rows = parse(
                "Rating,Name,Letterboxd URI,Year,Date\n"
                        + "2.5,Love Hurts,https://boxd.it/KyBc,2025,2025-12-31\n");

        assertThat(rows).containsExactly(
                new LetterboxdRatedMovie("Love Hurts", 2025, "https://boxd.it/KyBc", 5.0));
    }

    @Test
    void anEmptyTitleIsAnError() {
        assertThatThrownBy(() -> parse(HEADER + "d,,2020,uri,4\n"))
                .isInstanceOf(LetterboxdCsvException.class)
                .hasMessageContaining("Name");
    }

    @Test
    void anEmptyRatingIsAnError() {
        assertThatThrownBy(() -> parse(HEADER + "d,Name,2020,uri,\n"))
                .isInstanceOf(LetterboxdCsvException.class)
                .hasMessageContaining("Rating is empty");
    }

    @Test
    void aNonNumericRatingIsAnError() {
        assertThatThrownBy(() -> parse(HEADER + "d,Name,2020,uri,great\n"))
                .isInstanceOf(LetterboxdCsvException.class)
                .hasMessageContaining("not a number");
    }

    @Test
    void aRatingOutsideZeroPointFiveToFiveIsAnError() {
        assertThatThrownBy(() -> parse(HEADER + "d,Name,2020,uri,6\n"))
                .isInstanceOf(LetterboxdCsvException.class)
                .hasMessageContaining("outside");
        assertThatThrownBy(() -> parse(HEADER + "d,Name,2020,uri,0.2\n"))
                .isInstanceOf(LetterboxdCsvException.class)
                .hasMessageContaining("outside");
    }

    @Test
    void aHeaderMissingRequiredColumnsIsAnError() {
        assertThatThrownBy(() -> parse("Date,Name,Year,Letterboxd URI\n"
                + "2025-12-31,Love Hurts,2025,https://boxd.it/KyBc\n"))
                .isInstanceOf(LetterboxdCsvException.class)
                .hasMessageContaining("Rating");
    }

    @Test
    void anEmptyYearIsKeptAsNull() throws Exception {
        LetterboxdRatedMovie row = parse(HEADER + "d,Name,,uri,4\n").get(0);

        assertThat(row.year()).isNull();
    }

    @Test
    void anEmptyLetterboxdUriIsKeptAsNull() throws Exception {
        LetterboxdRatedMovie row = parse(HEADER + "d,Name,2020,,4\n").get(0);

        assertThat(row.letterboxdUri()).isNull();
    }

    @Test
    void readsUtf8Titles() throws Exception {
        List<LetterboxdRatedMovie> rows = parse(HEADER
                + "d,Amélie,2001,uri,4.5\n"
                + "d,\"君の名は。\",2016,uri,5\n");

        assertThat(rows).extracting(LetterboxdRatedMovie::title)
                .containsExactly("Amélie", "君の名は。");
    }

    @Test
    void returnsAnEmptyListWhenThereAreNoRowsAfterTheHeader() throws Exception {
        assertThat(parse(HEADER)).isEmpty();
        assertThat(parse("Date,Name,Year,Letterboxd URI,Rating")).isEmpty(); // no trailing newline
    }

    @Test
    void aMissingHeaderIsAnError() {
        assertThatThrownBy(() -> parse(""))
                .isInstanceOf(LetterboxdCsvException.class)
                .hasMessageContaining("header");
    }

    @Test
    void preservesTitleAndUriExactlyAndDoesNotAlterTheParsedValues() throws Exception {
        String exactTitle = "Se7en: Director's \"Cut\", vol. 2";
        String exactUri = "https://boxd.it/AbCd";
        List<LetterboxdRatedMovie> rows = parse(HEADER
                + "2025-01-01,\"" + exactTitle.replace("\"", "\"\"") + "\",1995," + exactUri + ",4\n");

        LetterboxdRatedMovie row = rows.get(0);
        assertThat(row.title()).isEqualTo(exactTitle);
        assertThat(row.letterboxdUri()).isEqualTo(exactUri);
        assertThat(row.year()).isEqualTo(1995);
        assertThat(row.userScore()).isEqualTo(8.0);
    }

    @Test
    void handlesCarriageReturnLineEndingsAndBlankLines() throws Exception {
        List<LetterboxdRatedMovie> rows = parse(
                "Date,Name,Year,Letterboxd URI,Rating\r\n"
                        + "d,First,2020,u1,4\r\n"
                        + "\r\n"
                        + "d,Second,2021,u2,3\r\n");

        assertThat(rows).extracting(LetterboxdRatedMovie::title).containsExactly("First", "Second");
    }

    @Test
    void letterboxdRatedMovieValidatesItsInvariants() {
        assertThatThrownBy(() -> new LetterboxdRatedMovie(null, 2020, "u", 8.0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LetterboxdRatedMovie("", 2020, "u", 8.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LetterboxdRatedMovie("t", 2020, "u", 10.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LetterboxdRatedMovie("t", 2020, "u", 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
