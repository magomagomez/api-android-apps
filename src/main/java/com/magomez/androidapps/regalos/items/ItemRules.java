package com.magomez.androidapps.regalos.items;

import com.magomez.androidapps.regalos.config.RegalosException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/** Validation of a wish, the same limits as the CHECKs of regalos_items. */
final class ItemRules {

    static final int MAX_TITLE = 120;
    static final int MAX_URL = 2048;
    static final int MAX_STORE = 80;
    static final int MAX_NOTES = 500;
    static final int MAX_PRICE_CENTS = 100_000_000;

    private static final Set<String> WEB_SCHEMES = Set.of("http", "https");
    private static final String WWW = "www.";

    private ItemRules() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    static ValidItem valid(ItemRequest request) {
        if (request == null) {
            throw RegalosException.badRequest("Faltan los datos del deseo");
        }
        String title = blankToNull(request.title());
        if (title == null || title.length() > MAX_TITLE) {
            throw RegalosException.badRequest("El nombre del deseo debe tener entre 1 y " + MAX_TITLE + " caracteres");
        }
        URI url = webLink(request.url(), "El enlace de la tienda");
        String store = blankToNull(request.store());
        if (store == null && url != null) {
            store = storeOf(url);
        }
        if (store != null && store.length() > MAX_STORE) {
            throw RegalosException.badRequest("La tienda no puede pasar de " + MAX_STORE + " caracteres");
        }
        Integer price = request.priceCents();
        if (price != null && (price < 0 || price > MAX_PRICE_CENTS)) {
            throw RegalosException.badRequest("El precio debe estar entre 0 y 1.000.000 €");
        }
        URI image = webLink(request.imageUrl(), "El enlace de la foto");
        String notes = blankToNull(request.notes());
        if (notes != null && notes.length() > MAX_NOTES) {
            throw RegalosException.badRequest("Las notas no pueden pasar de " + MAX_NOTES + " caracteres");
        }
        return new ValidItem(title, url == null ? null : url.toString(), store, price,
                image == null ? null : image.toString(), notes, Boolean.TRUE.equals(request.wantedALot()));
    }

    /** Only http(s) links with a host: javascript:, file: and the like never reach the app's browser. */
    private static URI webLink(String raw, String what) {
        String link = blankToNull(raw);
        if (link == null) {
            return null;
        }
        String invalid = what + " debe ser una dirección web (http o https)";
        if (link.length() > MAX_URL) {
            throw RegalosException.badRequest(what + " es demasiado largo");
        }
        try {
            URI uri = new URI(link);
            String scheme = uri.getScheme();
            if (scheme == null || !WEB_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT)) || uri.getHost() == null) {
                throw RegalosException.badRequest(invalid);
            }
            return uri;
        } catch (URISyntaxException e) {
            throw RegalosException.badRequest(invalid);
        }
    }

    private static String storeOf(URI url) {
        String host = url.getHost().toLowerCase(Locale.ROOT);
        String store = host.startsWith(WWW) ? host.substring(WWW.length()) : host;
        return store.length() > MAX_STORE ? store.substring(0, MAX_STORE) : store;
    }

    private static String blankToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
