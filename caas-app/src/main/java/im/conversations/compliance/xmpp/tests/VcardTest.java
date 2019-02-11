package im.conversations.compliance.xmpp.tests;

import im.conversations.compliance.annotations.ComplianceTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.extensions.disco.ServiceDiscoveryManager;
import rocks.xmpp.extensions.vcard.temp.model.VCard;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@ComplianceTest(
        short_name = "vcard",
        full_name = "XEP-0153: vCard-Based Avatar",
        description = "Support for Avatars",
        url = "https://xmpp.org"

)
public class VcardTest
		extends AbstractDiscoTest {

    public VcardTest(XmppClient client) {
        super(client);
    }

    @Override
    List<String> getNamespaces() {
        return Arrays.asList(VCard.NAMESPACE);
    }

    @Override
    boolean checkOnServer() {
        return true;
    }
}
