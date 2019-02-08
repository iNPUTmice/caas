package im.conversations.compliance.xmpp.tests;

import im.conversations.compliance.annotations.ComplianceTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.extensions.disco.ServiceDiscoveryManager;
import rocks.xmpp.extensions.muc.ChatRoom;
import rocks.xmpp.extensions.muc.ChatService;
import rocks.xmpp.extensions.muc.MultiUserChatManager;
import rocks.xmpp.extensions.vcard.temp.model.VCard;

import java.util.List;
import java.util.Set;

import static im.conversations.compliance.utils.ConversationsUtils.generateConversationsLikePronounceableName;

@ComplianceTest(
        short_name = "vcard",
        full_name = "XEP-0153: vCard-Based Avatar",
        description = "Support for Avatars",
        url = "https://xmpp.org"

)
public class Vcard
		extends AbstractTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Vcard.class);

    public Vcard(XmppClient client) {
        super(client);
    }

    @Override
    public boolean run() {
        final ServiceDiscoveryManager serviceDiscoveryManager = client.getManager(ServiceDiscoveryManager.class);
        try {
            final Set<String> features = serviceDiscoveryManager.discoverInformation(client.getDomain()).getResult().getFeatures();
            return features.contains(VCard.NAMESPACE);
        } catch (XmppException e) {
            LOGGER.debug(e.getMessage());
        }
        return false;
    }
}
