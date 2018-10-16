package im.conversations.compliance.xmpp.tests;

import im.conversations.compliance.annotations.ComplianceTest;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.extensions.bytestreams.s5b.model.Socks5ByteStream;

@ComplianceTest(
        short_name = "xep0065",
        full_name = "XEP-0065: SOCKS5 Bytestreams (Proxy)",
        url = "https://xmpp.org/extensions/xep-0065.html",
        description = "Provides a generic protocol for streaming binary data " +
                "between any two entities on an XMPP network. " +
                "It establishes an out-of-band bytestream between any two XMPP users " +
                "which can be direct(peer-to-peer) or mediated."
)
public class Proxy65 extends AbstractServiceTest {

    public Proxy65(XmppClient client) {
        super(client);
    }

    @Override
    public String getNamespace() {
        return Socks5ByteStream.NAMESPACE;
    }
}
