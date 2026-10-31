package com.magomez.androidapps.regalos.linkpreview;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static org.assertj.core.api.Assertions.assertThat;

/** A link becomes a proposal; an unreadable page is an empty one, and only a bad or forbidden link is an error. */
class LinkPreviewServiceTest {

    private final List<URI> fetched = new ArrayList<>();

    private LinkPreviewService service(Optional<LinkFetcher.Page> page) {
        return new LinkPreviewService(new LinkFetcher() {
            @Override
            public Optional<Page> fetch(URI link) {
                fetched.add(link);
                return page;
            }
        });
    }

    @Test
    void aShopPageBecomesAProposal() {
        URI page = URI.create("https://www.tienda.es/p/1");
        LinkPreview preview = service(Optional.of(new LinkFetcher.Page("<meta property=\"og:title\" content=\"Jarrón\">", page)))
                .preview(new LinkPreviewRequest("  https://www.tienda.es/p/1 "));

        assertThat(preview).isEqualTo(new LinkPreview("Jarrón", null, null, "tienda.es"));
        assertThat(fetched).containsExactly(page);
    }

    @Test
    void anUnreadablePageStillGivesTheShop() {
        assertThat(service(Optional.empty()).preview(new LinkPreviewRequest("https://www.amazon.es/dp/B0")))
                .isEqualTo(new LinkPreview(null, null, null, "amazon.es"));
    }

    @Test
    void whatIsNotAWebLinkIsRefusedWithoutFetching() {
        LinkPreviewService service = service(Optional.empty());
        for (String link : new String[]{"", "   ", "ftp://tienda.es/x", "javascript:alert(1)", "file:///etc/passwd",
                "https://user:pass@tienda.es/", "no es un enlace", "https://" + "a".repeat(2100) + ".es"}) {
            assertRegalosError(() -> service.preview(new LinkPreviewRequest(link)), HttpStatus.BAD_REQUEST, "bad_request");
        }
        assertRegalosError(() -> service.preview(null), HttpStatus.BAD_REQUEST, "bad_request");
        assertThat(fetched).isEmpty();
    }

    @Test
    void aForbiddenTargetIsABadRequest() {
        LinkPreviewService service = new LinkPreviewService(new LinkFetcher() {
            @Override
            public Optional<Page> fetch(URI link) {
                throw new BlockedLinkException();
            }
        });

        assertRegalosError(() -> service.preview(new LinkPreviewRequest("http://169.254.169.254/")),
                HttpStatus.BAD_REQUEST, "bad_request");
    }
}
