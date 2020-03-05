package im.conversations.compliance.xmpp.tests;

import com.google.gson.GsonBuilder;
import im.conversations.compliance.annotations.ComplianceTest;
import im.conversations.compliance.xmpp.utils.HttpUtils;
import im.conversations.compliance.xrd.ExtensibleResourceDescriptor;
import im.conversations.compliance.xrd.Link;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.xmpp.core.session.XmppClient;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.*;

@ComplianceTest(
        short_name = "xep0156",
        informational = true,
        full_name = "XEP-0156: Discovering Alternative XMPP Connection Methods (HTTP)",
        url = "https://xmpp.org/extensions/xep-0156.html",
        description = "Allows web clients to discover connections methods"
)
public class AlternateConnections extends AbstractTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlternateConnections.class);

    private static final GsonBuilder GSON_BUILDER = new GsonBuilder();
    private static final List<String> rels = Arrays.asList("urn:xmpp:alt-connections:xbosh", "urn:xmpp:alt-connections:websocket");


    public AlternateConnections(XmppClient client) {
        super(client);
    }

    private static boolean testAltConnectionsFromXml(InputStream is) throws Throwable {
        final JAXBContext jaxbContext = JAXBContext.newInstance(ExtensibleResourceDescriptor.class, Link.class);
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        final ExtensibleResourceDescriptor xdr = (ExtensibleResourceDescriptor) unmarshaller.unmarshal(is);
        final List<Link> links = xdr.getLinks(rels);
        return checkConnections(links);
    }

    private static boolean testAltConnectionsFromJson(InputStream is) {
        final ExtensibleResourceDescriptor xdr = GSON_BUILDER.create().fromJson(new InputStreamReader(is), ExtensibleResourceDescriptor.class);
        final List<Link> links = xdr.getLinks(rels);
        return checkConnections(links);
    }

    private static boolean checkConnections(List<Link> links) {
        for (Link link : links) {
            if (!checkConnection(link.getHref())) {
                return false;
            }
        }
        return links.size() > 0;
    }

    private static boolean checkConnection(URI uri) {
        int port = uri.getPort();
        if (port < 0) {
            if (Arrays.asList("wss", "https").contains(uri.getScheme())) {
                port = 443;
            } else if (Arrays.asList("ws", "http").contains(uri.getScheme())) {
                port = 80;
            }
        }
        return checkConnection(uri, port);
    }

    private static boolean checkConnection(final URI uri, final int port) {
        final String url;
        switch (uri.getScheme()) {
            case "wss":
                url = String.format("https://%s:%d%s", uri.getHost(), port, uri.getPath());
                break;
            case "ws":
                url = String.format("http://%s:%d%s", uri.getHost(), port, uri.getPath());
                break;
            default:
                url = String.format("%s://%s:%d%s", uri.getScheme(), uri.getHost(), port, uri.getPath());
                break;
        }
        final OkHttpClient okHttpClient = new OkHttpClient.Builder().protocols(Collections.singletonList(Protocol.HTTP_1_1)).build();
        final boolean ws = Arrays.asList("ws", "wss").contains(uri.getScheme());
        if (ws) {
            LOGGER.debug(String.format("checking reachability of %s", url));
        } else {
            LOGGER.debug(String.format("checking CORS Headers on %s", url));
        }
        try {
            final Request.Builder builder = new Request.Builder().url(url);
            if (ws) {
                builder.header("Connection", "Upgrade");
                builder.header("Upgrade", "websocket");
                builder.header("Sec-WebSocket-Protocol", "xmpp");
                builder.header("Sec-WebSocket-Version", "13");
                final byte[] uuidBytes = UUID.randomUUID().toString().getBytes();
                final String securityKey = Base64.getEncoder().encodeToString(uuidBytes);
                builder.header("Sec-WebSocket-Key", securityKey);
                builder.get();
            } else {
                builder.method("OPTIONS", null);
            }
            final Response response = okHttpClient.newCall(builder.build()).execute();
            final Map<String, List<String>> headers = response.headers().toMultimap();
            if (ws) {
                final boolean validResponseCode = response.code() <= 299;
                if (response.code() <= 299) {
                    return true;
                }
                LOGGER.debug(String.format("check of %s returned invalid response code (%d)", url, response.code()));
                return false;
            } else {
                final boolean corsHeaders = containsIgnoreCase(headers, "Access-Control-Allow-Origin", "*");
                if (response.code() == 200 && corsHeaders) {
                    return true;
                }
                LOGGER.debug(String.format("check of %s failed. response code=%d, CORS=%b",url, response.code(), corsHeaders));
                return false;
            }
        } catch (Throwable t) {
            LOGGER.debug("CORS request failed", t);
            return false;
        } finally {
            HttpUtils.shutdownAndIgnoreException(okHttpClient);
        }
    }

    private static boolean containsIgnoreCase(final Map<String, List<String>> headers, final String needle, final String expectedValue) {
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            if (header.getKey().equalsIgnoreCase(needle)) {
                return header.getValue().contains(expectedValue);
            }
        }
        return false;
    }

    private boolean discoverAndTestAltConnections(final String domain) {
        return discoverAndTestAltConnections(domain, false, true)
                || discoverAndTestAltConnections(domain, true, true)
                || discoverAndTestAltConnections(domain, false, false)
                || discoverAndTestAltConnections(domain, true, false);
    }

    private boolean discoverAndTestAltConnections(final String domain, final boolean json, final boolean https) {
        final OkHttpClient okHttpClient = new OkHttpClient();
        try {
            final URL url = new URL(https ? "https" : "http", domain, "/.well-known/host-meta" + (json ? ".json" : ""));
            LOGGER.debug(String.format("checking on %s ", url.toString()));
            final Request request = new Request.Builder().url(url).build();
            final Response response = okHttpClient.newCall(request).execute();
            final ResponseBody body = response.body();
            if (body == null) {
                response.close();
                return false;
            }
            final int code = response.code();
            if (code != 200) {
                LOGGER.debug(String.format("%s had response code %d", url, code));
                response.close();
                return false;
            }
            final Map<String, List<String>> headers = response.headers().toMultimap();
            if (!containsIgnoreCase(headers, "Access-Control-Allow-Origin", "*")) {
                response.close();
                LOGGER.debug(url + " did not set Access-Control-Allow-Origin to *");
                return false;
            }
            try (final InputStream is = body.byteStream()) {
                return json ? testAltConnectionsFromJson(is) : testAltConnectionsFromXml(is);
            } finally {
                response.close();
            }
        } catch (Throwable throwable) {
            LOGGER.debug("unable to discover ", throwable);
            return false;
        } finally {
            HttpUtils.shutdownAndIgnoreException(okHttpClient);
        }
    }

    @Override
    public boolean run() {
        return discoverAndTestAltConnections(client.getDomain().toString());
    }
}
