package im.conversations.compliance.xmpp.tests;

import im.conversations.compliance.annotations.ComplianceTest;
import rocks.xmpp.addr.Jid;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.extensions.data.model.DataForm;
import rocks.xmpp.extensions.disco.ServiceDiscoveryManager;

import java.util.List;

@ComplianceTest(
        short_name = "xep0157",
        full_name = "XEP-0157: Contact Addresses for XMPP Services",
        url = "https://xmpp.org/extensions/xep-0163.html",
        description = "Checks if the server provides contact addresses.",
        informational = true
)
public class ContactTest extends AbstractTest {

    // Registered fields for server information scoped by the "http://jabber.org/network/serverinfo" FORM_TYPE.
    public static String[] FIELDS = {
            "abuse-addresses",
            "admin-addresses",
            "feedback-addresses",
            "sales-addresses",
            "security-addresses",
            "support-addresses"
    };

    public ContactTest( XmppClient client) {
        super(client);
    }

    @Override
    public boolean run() {
        Jid target = Jid.of(client.getConnectedResource().getDomain());
        final ServiceDiscoveryManager serviceDiscoveryManager = client.getManager(ServiceDiscoveryManager.class);
        try {
            List<DataForm> extensions = serviceDiscoveryManager.discoverInformation(target).getResult().getExtensions();
            for (DataForm extension : extensions) {
                for (String field : FIELDS) {
                    final DataForm.Field addr = extension.findField( field );
                    if (addr != null && addr.getValues().size() > 0) {
                        return true;
                    }
                }
            }
            return false;
        } catch (XmppException e) {
            return false;
        }
    }
}
