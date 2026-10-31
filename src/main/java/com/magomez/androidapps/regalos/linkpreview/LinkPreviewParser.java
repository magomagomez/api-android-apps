package com.magomez.androidapps.regalos.linkpreview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 * What a shop page says about itself: Open Graph and Twitter tags first, then schema.org Product
 * data (JSON-LD and microdata), then plain HTML. The page is untrusted data: everything is
 * trimmed, cut to the limits of a wish, and links must be http(s).
 */
final class LinkPreviewParser {

    static final int MAX_TITLE = 120;
    static final int MAX_STORE = 80;
    static final int MAX_URL = 2048;
    static final int MAX_PRICE_CENTS = 100_000_000;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String EURO = "EUR";
    private static final String WWW = "www.";
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern NUMBER = Pattern.compile("\\d[\\d.,]*");

    private LinkPreviewParser() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    static LinkPreview parse(String html, URI page) {
        Document doc = Jsoup.parse(html, page.toString());
        List<JsonNode> products = products(doc);
        String title = first(meta(doc, "og:title"), meta(doc, "twitter:title"),
                products.stream().map(p -> p.path("name").asText(null)).findFirst().orElse(null),
                doc.title());
        String image = firstLink(page, meta(doc, "og:image:secure_url"), meta(doc, "og:image"), meta(doc, "twitter:image"),
                products.stream().map(LinkPreviewParser::imageOf).findFirst().orElse(null));
        Integer price = Optional.ofNullable(metaPrice(doc)).orElseGet(() -> productPrice(products));
        String store = first(meta(doc, "og:site_name"));
        return new LinkPreview(cut(title, MAX_TITLE), image, price, store == null ? storeOf(page) : cut(store, MAX_STORE));
    }

    /** Only the shop's name from its address, when the page says nothing. */
    static LinkPreview fromAddress(URI page) {
        return new LinkPreview(null, null, null, storeOf(page));
    }

    private static String meta(Document doc, String key) {
        Element tag = doc.selectFirst("meta[property=" + key + "], meta[name=" + key + "]");
        return tag == null ? null : tag.attr("content");
    }

    /** Open Graph product price, or microdata, when in euros (or with no currency at all). */
    private static Integer metaPrice(Document doc) {
        String amount = first(meta(doc, "product:price:amount"), meta(doc, "og:price:amount"));
        String currency = first(meta(doc, "product:price:currency"), meta(doc, "og:price:currency"));
        if (amount != null) {
            return euros(amount, currency, true);
        }
        Element itemprop = doc.selectFirst("[itemprop=price]");
        if (itemprop == null) {
            return null;
        }
        Element currencyTag = doc.selectFirst("[itemprop=priceCurrency]");
        currency = currencyTag == null ? null : first(currencyTag.attr("content"), currencyTag.text());
        String content = itemprop.attr("content");
        // The content attribute is machine format; the visible text is how the shop writes prices.
        return content.isBlank() ? euros(itemprop.text(), currency, false) : euros(content, currency, true);
    }

    /** schema.org Product objects in the JSON-LD blocks, also inside @graph and arrays. */
    private static List<JsonNode> products(Document doc) {
        List<JsonNode> found = new ArrayList<>();
        for (Element script : doc.select("script[type=application/ld+json]")) {
            try {
                collectProducts(MAPPER.readTree(script.data()), found);
            } catch (JsonProcessingException e) {
                // Broken JSON-LD on a shop page: just ignore that block.
            }
        }
        return found;
    }

    private static void collectProducts(JsonNode node, List<JsonNode> found) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectProducts(child, found));
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode type = node.path("@type");
        boolean product = type.isArray()
                ? StreamSupport.stream(type.spliterator(), false).anyMatch(t -> "Product".equals(t.asText()))
                : "Product".equals(type.asText());
        if (product) {
            found.add(node);
        }
        collectProducts(node.get("@graph"), found);
    }

    private static Integer productPrice(List<JsonNode> products) {
        for (JsonNode product : products) {
            JsonNode offers = product.path("offers");
            JsonNode offer = offers.isArray() ? offers.path(0) : offers;
            String amount = first(text(offer.get("price")), text(offer.get("lowPrice")),
                    text(offer.path("priceSpecification").get("price")));
            String currency = first(text(offer.get("priceCurrency")), text(offer.path("priceSpecification").get("priceCurrency")));
            Integer cents = euros(amount, currency, true);
            if (cents != null) {
                return cents;
            }
        }
        return null;
    }

    private static String imageOf(JsonNode product) {
        JsonNode image = product.get("image");
        if (image == null) {
            return null;
        }
        if (image.isArray()) {
            image = image.path(0);
        }
        return image.isObject() ? text(image.get("url")) : text(image);
    }

    /**
     * The amount as cents; null when not in euros or nonsense. Machine format (meta tags, JSON-LD) has a
     * decimal point ("29.990" is 29,99 €); text written for people may be "1.299,00 €" or "1,299.00".
     */
    static Integer euros(String amount, String currency, boolean machine) {
        if (amount == null || (currency != null && !currency.isBlank() && !EURO.equalsIgnoreCase(currency.trim()))) {
            return null;
        }
        var matcher = NUMBER.matcher(amount);
        if (!matcher.find()) {
            return null;
        }
        String number = matcher.group();
        if (machine) {
            return cents(number.indexOf('.') < 0 ? number.replace(',', '.') : number.replace(",", ""));
        }
        int dot = number.lastIndexOf('.');
        int comma = number.lastIndexOf(',');
        String decimal = dot > comma ? "." : ",";
        String thousands = dot > comma ? "," : ".";
        int decimalAt = Math.max(dot, comma);
        // A lone separator followed by exactly three digits is a thousands mark: "1.299" is 1299 €.
        if ((dot < 0 || comma < 0) && decimalAt >= 0 && number.length() - decimalAt - 1 == 3) {
            number = number.replace(decimal, "");
        } else {
            number = number.replace(thousands, "").replace(decimal, ".");
        }
        return cents(number);
    }

    private static Integer cents(String number) {
        try {
            BigDecimal cents = new BigDecimal(number).movePointRight(2).setScale(0, RoundingMode.HALF_UP);
            return cents.signum() <= 0 || cents.compareTo(BigDecimal.valueOf(MAX_PRICE_CENTS)) > 0 ? null : cents.intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    /** The first candidate that resolves to an absolute http(s) link within the size limit. */
    private static String firstLink(URI page, String... candidates) {
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            try {
                URI link = page.resolve(new URI(candidate.trim().replace(" ", "%20")));
                String scheme = link.getScheme() == null ? "" : link.getScheme().toLowerCase(Locale.ROOT);
                if ((scheme.equals("http") || scheme.equals("https")) && link.getHost() != null
                        && link.toString().length() <= MAX_URL) {
                    return link.toString();
                }
            } catch (URISyntaxException | IllegalArgumentException e) {
                // Not a link: try the next candidate.
            }
        }
        return null;
    }

    static String storeOf(URI page) {
        String host = page.getHost() == null ? "" : page.getHost().toLowerCase(Locale.ROOT);
        String store = host.startsWith(WWW) ? host.substring(WWW.length()) : host;
        return store.isEmpty() ? null : cut(store, MAX_STORE);
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() || node.isContainerNode() ? null : node.asText();
    }

    private static String first(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private static String cut(String text, int max) {
        if (text == null) {
            return null;
        }
        String clean = SPACES.matcher(text).replaceAll(" ").trim();
        if (clean.isEmpty()) {
            return null;
        }
        return clean.length() <= max ? clean : clean.substring(0, max - 1).trim() + "…";
    }
}
