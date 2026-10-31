package com.magomez.androidapps.regalos.linkpreview;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Downloads the HTML of a pasted link without letting it reach anything but the public internet
 * (SSRF). Every host name is resolved through a DNS that refuses non-public addresses, and the
 * socket connects to exactly those addresses, so a name cannot resolve one way when checked and
 * another when used. IP literals skip DNS in OkHttp, so they are checked on the URL. Redirects are
 * followed by hand and each hop goes through the same checks. Only http and https on their usual
 * ports, at most {@link #MAX_BYTES} of text/html, within {@link #TIMEOUT}.
 */
@Component
public class LinkFetcher {

    private static final Logger log = LoggerFactory.getLogger(LinkFetcher.class);

    static final int MAX_BYTES = 1024 * 1024;
    static final int MAX_REDIRECTS = 4;
    static final Duration TIMEOUT = Duration.ofSeconds(4);
    /**
     * Who we are, honestly: a link previewer. A shop that turns such clients away simply gives no
     * preview and the user types the wish; we never pretend to be a browser to get past it.
     */
    static final String USER_AGENT = "CorreoDeRegalos/1.0 (vista previa de enlaces; app privada de listas de deseos)";

    private static final Set<String> HTML = Set.of("text/html", "application/xhtml+xml");
    private static final Pattern IPV4_LITERAL = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    /** 2130706433, 0x7f.1, 017700000001: other spellings of an IPv4 address, which no shop uses. */
    private static final Pattern NUMERIC_HOST = Pattern.compile("(?i)(0x[0-9a-f]+|\\d+)(\\.(0x[0-9a-f]+|\\d+)){0,3}");

    /** What a fetch may connect to. Production: public addresses on the scheme's default port. */
    interface Policy {
        boolean allowsAddress(InetAddress address);

        boolean allowsPort(String scheme, int port);
    }

    static final Policy PUBLIC_INTERNET = new Policy() {
        @Override
        public boolean allowsAddress(InetAddress address) {
            return PublicAddresses.isPublic(address);
        }

        @Override
        public boolean allowsPort(String scheme, int port) {
            return port == HttpUrl.defaultPort(scheme);
        }
    };

    private final Policy policy;
    private final OkHttpClient http;

    @Autowired
    public LinkFetcher() {
        this(PUBLIC_INTERNET);
    }

    LinkFetcher(Policy policy) {
        this.policy = policy;
        this.http = new OkHttpClient.Builder()
                .dns(host -> resolve(host, policy))
                .proxy(Proxy.NO_PROXY)
                .followRedirects(false)
                .followSslRedirects(false)
                .callTimeout(TIMEOUT)
                .build();
    }

    /** The page, or empty when it cannot be read as HTML; throws {@link BlockedLinkException} for a forbidden target. */
    public Optional<Page> fetch(URI link) {
        HttpUrl url = HttpUrl.parse(link.toString());
        for (int hop = 0; url != null && hop <= MAX_REDIRECTS; hop++) {
            check(url);
            Request request = new Request.Builder().url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("Accept-Language", "es-ES,es;q=0.9")
                    .get().build();
            try (Response response = http.newCall(request).execute()) {
                if (response.isRedirect()) {
                    String location = response.header("Location");
                    url = location == null ? null : url.resolve(location);
                    continue;
                }
                if (!response.isSuccessful()) {
                    return Optional.empty();
                }
                return html(response.body()).map(text -> new Page(text, response.request().url().uri()));
            } catch (BlockedAddressException e) {
                throw new BlockedLinkException();
            } catch (IOException e) {
                // Unreachable, too slow or cut short: there is simply no preview.
                log.info("regalos: a link preview could not be read ({})", e.getClass().getSimpleName());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private void check(HttpUrl url) {
        if (!policy.allowsPort(url.scheme(), url.port())) {
            throw new BlockedLinkException();
        }
        String host = url.host();
        if (!IPV4_LITERAL.matcher(host).matches() && NUMERIC_HOST.matcher(host).matches()) {
            throw new BlockedLinkException();
        }
        if (host.contains(":") || IPV4_LITERAL.matcher(host).matches()) {
            try {
                // A literal: getByName parses it without any DNS lookup.
                if (!policy.allowsAddress(InetAddress.getByName(host))) {
                    throw new BlockedLinkException();
                }
            } catch (UnknownHostException e) {
                throw new BlockedLinkException();
            }
        }
    }

    private static List<InetAddress> resolve(String host, Policy policy) throws UnknownHostException {
        List<InetAddress> addresses = Arrays.asList(InetAddress.getAllByName(host));
        // All or nothing: one private address among public ones is enough to refuse the name.
        if (addresses.isEmpty() || !addresses.stream().allMatch(policy::allowsAddress)) {
            throw new BlockedAddressException();
        }
        return addresses;
    }

    private static Optional<String> html(ResponseBody body) throws IOException {
        if (body == null) {
            return Optional.empty();
        }
        MediaType type = body.contentType();
        String mime = type == null ? "" : (type.type() + "/" + type.subtype()).toLowerCase(Locale.ROOT);
        if (!HTML.contains(mime)) {
            return Optional.empty();
        }
        Charset charset = type.charset(StandardCharsets.UTF_8);
        try (InputStream in = body.byteStream()) {
            // The preview tags live in <head>: the first megabyte is plenty, the rest is not read.
            return Optional.of(new String(in.readNBytes(MAX_BYTES), charset));
        }
    }

    /** The HTML of a page and where it really came from, after redirects (relative image links resolve against it). */
    public record Page(String html, URI url) {
    }

    /** A name that resolves to an address the policy refuses. */
    static final class BlockedAddressException extends UnknownHostException {
        BlockedAddressException() {
            super("Blocked address for a link preview");
        }
    }
}
