package im.conversations.compliance;


import im.conversations.compliance.pojo.Credential;
import im.conversations.compliance.pojo.Result;
import im.conversations.compliance.xmpp.TestExecutor;
import im.conversations.compliance.xmpp.TestFactory;
import org.apache.commons.cli.*;
import rocks.xmpp.addr.Jid;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.extensions.version.SoftwareVersionManager;
import rocks.xmpp.extensions.version.model.SoftwareVersion;

import java.util.*;
import java.util.stream.Collectors;

public class CommandLineLauncher {
    public static void main(String[] input) {

        Options options = new Options();
        options.addOption("v","verbose",false,null);

        final CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(options, input);
        } catch (ParseException e) {
            System.err.println("Unable to parse command line arguments");
            e.printStackTrace();
            return;
        }
        final List<String> args = cmd.getArgList();
        String password;
        if (args.size() > 2 || args.size() < 1) {
            System.err.println("java -jar caas-app.jar username@domain.tld [password]");
            System.exit(1);
            return;
        }

        if (cmd.hasOption("v")) {
            Properties properties = System.getProperties();
            properties.setProperty("org.slf4j.simpleLogger.defaultLogLevel","debug");
        }

        Jid jid = Jid.of(args.get(0));

        if (args.size() == 2) {
            password = args.get(1);
            AccountStore.storePassword(jid, password);
        } else {
            String storedPassword = AccountStore.getPassword(jid);
            if (storedPassword != null) {
                password = storedPassword;
            } else {
                System.err.println("password for " + jid + " was not stored. trying to register");
                try {
                    password = RegistrationHelper.register(jid);
                    AccountStore.storePassword(jid, password);
                } catch (RegistrationHelper.RegistrationNotSupported e) {
                    System.err.println("server " + jid.getDomain() + " does not support registration");
                    System.exit(1);
                    return;
                } catch (RegistrationHelper.RegistrationFailed e) {
                    System.out.println("registration failed on server " + jid.getDomain() + " " + e.getMessage());
                    System.exit(1);
                    return;
                }

            }
        }
        Credential credential = new Credential(jid, password);
        try {
            Map<Boolean, List<Result>> results = TestExecutor.executeTestsFor(credential, (client -> {
                final Optional<SoftwareVersion> version = getSoftwareVersion(client);
                if (version.isPresent()) {
                    System.out.println("Server is running " + version.get().getName() + " " + version.get().getVersion());
                } else {
                    System.out.println("Server is running unknown software");
                }
            })).stream().collect(Collectors.partitioningBy(r -> r.getTest().informational()));
            System.out.println("\nCompliance report for " + credential.getDomain());
            int padding = Collections.max(results.values().stream().flatMap(List::stream).collect(Collectors.toList()), Comparator.comparing(r -> r.getTest().full_name().length())).getTest().full_name().length() + 1;

            final List<Result> nonInformational = new ArrayList<>(results.get(false));
            final List<Result> informational = new ArrayList<>(results.get(true));
            nonInformational.sort(Comparator.comparing(a -> a.getTest().full_name()));
            informational.sort(Comparator.comparing(a -> a.getTest().full_name()));
            for (Result result : nonInformational) {
                print(result, padding);
            }
            System.out.println("\nInformational tests:");
            for (Result result : informational) {
                print(result, padding);
            }
        } catch (TestFactory.TestCreationException | XmppException e) {
            e.printStackTrace();
        }
    }

    private static Optional<SoftwareVersion> getSoftwareVersion(XmppClient client) {
        try {
            return Optional.ofNullable(client.getManager(SoftwareVersionManager.class).getSoftwareVersion(client.getDomain()).getResult());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static void print(Result result, int padding) {
        System.out.println(String.format("%1$-" + padding + "s", result.getTest().full_name() + " ") + (result.isSuccess() ? "\u001B[32mPASSED\u001B[0m" : "\u001B[31mFAILED\u001B[0m"));
    }
}
