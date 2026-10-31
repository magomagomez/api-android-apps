package com.magomez.androidapps.regalos.linkpreview;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/** What a shop page proposes, from its preview tags, its product data or plain HTML; the page is untrusted data. */
class LinkPreviewParserTest {

    private static final URI PAGE = URI.create("https://www.tienda.es/p/jarron-azul?ref=1");

    @Test
    void openGraphTagsComeFirst() {
        String html = """
                <html><head><title>Jarrón | Tienda</title>
                <meta property="og:title" content="  Jarrón de cerámica
                   azul ">
                <meta property="og:image" content="/img/jarron.jpg">
                <meta property="og:site_name" content="La Tienda">
                <meta property="product:price:amount" content="34.90">
                <meta property="product:price:currency" content="EUR">
                </head><body></body></html>
                """;

        assertThat(LinkPreviewParser.parse(html, PAGE))
                .isEqualTo(new LinkPreview("Jarrón de cerámica azul", "https://www.tienda.es/img/jarron.jpg", 3490, "La Tienda"));
    }

    @Test
    void schemaOrgProductFillsWhatTheTagsLeaveOut() {
        String html = """
                <html><head><title>Zapatillas</title>
                <script type="application/ld+json">{"@context":"https://schema.org","@graph":[
                  {"@type":"WebPage","name":"Página"},
                  {"@type":["Product","Thing"],"name":"Zapatillas de trail","image":["https://cdn.tienda.es/z.jpg"],
                   "offers":[{"@type":"Offer","price":"119.90","priceCurrency":"EUR"}]}]}</script>
                <script type="application/ld+json">{ esto no es json </script>
                </head></html>
                """;

        assertThat(LinkPreviewParser.parse(html, PAGE))
                .isEqualTo(new LinkPreview("Zapatillas de trail", "https://cdn.tienda.es/z.jpg", 11990, "tienda.es"));
    }

    @Test
    void microdataPriceWrittenForPeopleIsRead() {
        String html = """
                <html><head><title>Bicicleta</title></head><body>
                <span itemprop="price">1.299,00 €</span><meta itemprop="priceCurrency" content="EUR">
                </body></html>
                """;

        assertThat(LinkPreviewParser.parse(html, PAGE).priceCents()).isEqualTo(129900);
    }

    @Test
    void pricesInOtherCurrenciesAreLeftOut() {
        String html = """
                <meta property="og:price:amount" content="20.00"><meta property="og:price:currency" content="USD">
                """;

        assertThat(LinkPreviewParser.parse(html, PAGE).priceCents()).isNull();
    }

    @Test
    void euroAmountsInTheirUsualShapes() {
        assertThat(LinkPreviewParser.euros("34.99", "EUR", true)).isEqualTo(3499);
        assertThat(LinkPreviewParser.euros("34,99", null, true)).isEqualTo(3499);
        assertThat(LinkPreviewParser.euros("29.990", "EUR", true)).isEqualTo(2999);
        assertThat(LinkPreviewParser.euros("1,299.00", "EUR", true)).isEqualTo(129900);
        assertThat(LinkPreviewParser.euros("1.299", null, false)).isEqualTo(129900);
        assertThat(LinkPreviewParser.euros("1,299.50 €", null, false)).isEqualTo(129950);
        assertThat(LinkPreviewParser.euros("12,5 €", null, false)).isEqualTo(1250);
        assertThat(LinkPreviewParser.euros("gratis", null, false)).isNull();
        assertThat(LinkPreviewParser.euros("0", null, true)).isNull();
        assertThat(LinkPreviewParser.euros("99999999", null, true)).isNull();
    }

    @Test
    void onlyHttpImagesAndTrimmedTextGetThrough() {
        String html = """
                <html><head><title>%s</title>
                <meta property="og:image" content="javascript:alert(1)">
                <meta name="twitter:image" content="data:image/png;base64,AAAA">
                <meta property="og:image:secure_url" content="https://tienda.es/logo.svg?v=2">
                </head></html>
                """.formatted("x".repeat(300));

        LinkPreview preview = LinkPreviewParser.parse(html, PAGE);

        assertThat(preview.imageUrl()).isNull();
        assertThat(preview.title()).hasSize(LinkPreviewParser.MAX_TITLE).endsWith("…");
    }

    @Test
    void aPageThatSaysNothingStillGivesTheShopFromItsAddress() {
        assertThat(LinkPreviewParser.parse("<html><body>hola</body></html>", PAGE))
                .isEqualTo(new LinkPreview(null, null, null, "tienda.es"));
        assertThat(LinkPreviewParser.fromAddress(URI.create("https://amazon.es/dp/1")))
                .isEqualTo(new LinkPreview(null, null, null, "amazon.es"));
    }
}
