package com.magomez.androidapps.regalos.linkpreview;

import com.magomez.androidapps.regalos.config.RegalosException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Reads a pasted shop link and proposes the name, photo, price and shop of the wish. A page that
 * cannot be read gives an empty proposal (only the shop, from the address), never an error: the
 * user just types the wish. Only a link that is not a web address, or that points somewhere a
 * preview may not go, is refused.
 */
@Service
public class LinkPreviewService {

    private static final Set<String> WEB_SCHEMES = Set.of("http", "https");
    private static final String NOT_A_LINK = "El enlace debe ser una dirección web (http o https)";

    private final LinkFetcher fetcher;

    public LinkPreviewService(LinkFetcher fetcher) {
        this.fetcher = fetcher;
    }

    public LinkPreview preview(LinkPreviewRequest request) {
        URI link = link(request);
        try {
            return fetcher.fetch(link)
                    .map(page -> LinkPreviewParser.parse(page.html(), page.url()))
                    .orElseGet(() -> LinkPreviewParser.fromAddress(link));
        } catch (BlockedLinkException e) {
            throw RegalosException.badRequest("Ese enlace no se puede leer");
        }
    }

    private static URI link(LinkPreviewRequest request) {
        String raw = request == null || request.url() == null ? "" : request.url().trim();
        if (raw.isEmpty() || raw.length() > LinkPreviewParser.MAX_URL) {
            throw RegalosException.badRequest(NOT_A_LINK);
        }
        try {
            URI uri = new URI(raw);
            String scheme = uri.getScheme();
            if (scheme == null || !WEB_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT)) || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw RegalosException.badRequest(NOT_A_LINK);
            }
            return uri;
        } catch (URISyntaxException e) {
            throw RegalosException.badRequest(NOT_A_LINK);
        }
    }
}
