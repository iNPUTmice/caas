package im.conversations.compliance;

import im.conversations.compliance.pojo.Help;
import im.conversations.compliance.pojo.ServerHelp;
import im.conversations.compliance.pojo.TestHelp;
import im.conversations.compliance.xmpp.utils.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class HelpsTest {
    List<String> tests;

    @Before
    public void initHelp() {
        tests = TestUtils.getAllTestNames();
    }

    @Test
    public void validHelpExistsForEjabberd() {
        checkHelpFile("ejabberd");
    }

    @Test
    public void validHelpExistsForProsody() {
        checkHelpFile("prosody");
        checkHelpFile("Prosody");
    }

    @Test
    public void validHelpExistsForOpenfire() {
        checkHelpFile("openfire");
        checkHelpFile("Openfire");
    }

    void checkHelpFile(String server) {
        ServerHelp help = Help.getInstance().getHelpFor(server).get();
        List<String> testsForWhichHelpAvailable = help.getTestsHelp()
                .stream()
                .map(TestHelp::getName)
                .sorted()
                .collect(Collectors.toList());
        Assert.assertEquals(tests, testsForWhichHelpAvailable);
    }

}
