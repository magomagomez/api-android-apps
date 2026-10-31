package com.magomez.androidapps.regalos.linkpreview;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link LinkFetcher} against a throwaway local server. The tests allow only that server's own
 * loopback address (on any port); the real policy is checked on addresses no request ever reaches.
 */
class LinkFetcherTest {

    /** Only 127.0.0.1, the test server: anything else is as forbidden as in production. */
    private static final LinkFetcher.Policy ONLY_TEST_SERVER = new LinkFetcher.Policy() {
        @Override
        public boolean allowsAddress(InetAddress address) {
            return address.getHostAddress().equals("127.0.0.1");
        }

        @Override
        public boolean allowsPort(String scheme, int port) {
            return true;
        }
    };

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<String> userAgents = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/page", exchange -> respond(exchange, 200, "text/html; charset=utf-8", "<title>Jarrón</title>"));
        server.createContext("/moved", exchange -> redirect(exchange, "/page"));
        server.createContext("/to-metadata", exchange -> redirect(exchange, "http://169.254.169.254/latest/meta-data/"));
        server.createContext("/loop", exchange -> redirect(exchange, "/loop"));
        server.createContext("/image", exchange -> respond(exchange, 200, "image/jpeg", "jpeg"));
        server.createContext("/missing", exchange -> respond(exchange, 404, "text/html", "no"));
        server.createContext("/huge", exchange -> respond(exchange, 200, "text/html",
                "<title>Grande</title>" + "x".repeat(LinkFetcher.MAX_BYTES * 2)));
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void readsTheHtmlOfAPageAsABrowserWould() {
        LinkFetcher.Page page = fetcher().fetch(url("/page")).orElseThrow();

        assertThat(page.html()).isEqualTo("<title>Jarrón</title>");
        assertThat(userAgents.get(0)).contains("CorreoDeRegalos");
    }

    @Test
    void followsRedirectsAndKnowsWhereThePageReallyIs() {
        LinkFetcher.Page page = fetcher().fetch(url("/moved")).orElseThrow();

        assertThat(requests).containsExactly("/moved", "/page");
        assertThat(page.url().getPath()).isEqualTo("/page");
    }

    @Test
    void aRedirectToAPrivateAddressIsBlocked() {
        assertThatThrownBy(() -> fetcher().fetch(url("/to-metadata"))).isInstanceOf(BlockedLinkException.class);
        assertThat(requests).containsExactly("/to-metadata");
    }

    @Test
    void endlessRedirectsGiveNoPage() {
        assertThat(fetcher().fetch(url("/loop"))).isEmpty();
        assertThat(requests).hasSize(LinkFetcher.MAX_REDIRECTS + 1);
    }

    @Test
    void whatIsNotHtmlOrFailsGivesNoPage() {
        assertThat(fetcher().fetch(url("/image"))).isEmpty();
        assertThat(fetcher().fetch(url("/missing"))).isEmpty();
    }

    @Test
    void onlyTheFirstMegabyteIsRead() {
        LinkFetcher.Page page = fetcher().fetch(url("/huge")).orElseThrow();

        assertThat(page.html()).startsWith("<title>Grande</title>").hasSize(LinkFetcher.MAX_BYTES);
    }

    @Test
    void aNameResolvingToAForbiddenAddressIsBlockedBeforeConnecting() {
        // "localhost" resolves to 127.0.0.1 (allowed here) and ::1 (not): all or nothing.
        LinkFetcher strict = new LinkFetcher(new LinkFetcher.Policy() {
            @Override
            public boolean allowsAddress(InetAddress address) {
                return false;
            }

            @Override
            public boolean allowsPort(String scheme, int port) {
                return true;
            }
        });

        assertThatThrownBy(() -> strict.fetch(URI.create("http://localhost:" + port() + "/page")))
                .isInstanceOf(BlockedLinkException.class);
        assertThat(requests).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1/", "http://169.254.169.254/latest/meta-data/", "http://10.0.0.5/admin",
            "http://[::1]/", "http://[fd00::1]/", "http://localhost/", "http://2130706433/", "http://0x7f.1/", "http://017700000001/",
            "http://example.com:8080/", "https://example.com:22/", "http://0.0.0.0/"})
    void theRealPolicyBlocksTheServerItsNetworkAndOddPorts(String link) {
        assertThatThrownBy(() -> new LinkFetcher().fetch(URI.create(link))).isInstanceOf(BlockedLinkException.class);
    }

    private LinkFetcher fetcher() {
        return new LinkFetcher(ONLY_TEST_SERVER);
    }

    private int port() {
        return server.getAddress().getPort();
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + port() + path);
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        requests.add(exchange.getRequestURI().getPath());
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void respond(HttpExchange exchange, int status, String type, String body) throws IOException {
        requests.add(exchange.getRequestURI().getPath());
        userAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        } catch (IOException e) {
            // The fetcher stops reading after a megabyte and closes the connection.
        }
    }
}
