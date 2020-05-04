package im.conversations.compliance.web;

import com.google.gson.Gson;
import im.conversations.compliance.annotations.ComplianceTest;
import im.conversations.compliance.email.MailBuilder;
import im.conversations.compliance.email.MailSender;
import im.conversations.compliance.email.MailVerification;
import im.conversations.compliance.persistence.DBOperations;
import im.conversations.compliance.pojo.*;
import im.conversations.compliance.utils.JsonReader;
import im.conversations.compliance.utils.TimeUtils;
import im.conversations.compliance.xmpp.CredentialVerifier;
import im.conversations.compliance.xmpp.OneOffTestRunner;
import im.conversations.compliance.xmpp.utils.TestUtils;
import org.apache.commons.validator.routines.EmailValidator;
import spark.ModelAndView;
import spark.Route;
import spark.TemplateViewRoute;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Controller {

    private static final Gson gson = JsonReader.gson;

    public static TemplateViewRoute getRoot = (request, response) -> {
        List<Server> servers = DBOperations.getServers(true);
        HashMap<String, Object> model = new HashMap<>();
        model.put("servers", gson.toJson(servers.stream().map(Server::getDomain).collect(Collectors.toList())));
        List<String> compliantServerNames = DBOperations.getCompliantServers();
        Collections.shuffle(compliantServerNames);
        compliantServerNames = compliantServerNames.stream().limit(5).collect(Collectors.toList());
        model.put("recommendations", compliantServerNames);
        return new ModelAndView(model, "root.ftl");
    };

    public static TemplateViewRoute getOld = (request, response) -> {
        Map<String, HashMap<String, Boolean>> resultsByServer = DBOperations.getCurrentResultsByServer();
        HashMap<String, Object> model = new HashMap<>();
        WebUtils.addDataForComplianceTable(model, resultsByServer);
        WebUtils.addRootUrl(model, request);
        return new ModelAndView(model, "old.ftl");
    };

    public static TemplateViewRoute getTests = (request, response) -> {
        HashMap<String, Object> model = new HashMap<>();
        model.put("tests", TestUtils.getComplianceTests());
        model.put("informationalTests", TestUtils.getInformationalTests());
        return new ModelAndView(model, "tests.ftl");
    };

    public static TemplateViewRoute getAbout = (request, response) -> new ModelAndView(null, "about.ftl");

    public static TemplateViewRoute getAdd = ((request, response) -> {
        HashMap<String, Object> model = new HashMap<>();
        WebUtils.addRootUrl(model, request);
        return new ModelAndView(model, "add.ftl");
    });

    public static Route postAdd = (request, response) -> {
        String jid = request.queryParams("jid");
        String password = request.queryParams("password");
        boolean listedServer = Boolean.parseBoolean(request.queryParams("listed"));
        final Credential credential;
        ServerResponse postResponse;
        try {
            credential = new Credential(jid, password);
        } catch (Exception ex) {
            postResponse = new ServerResponse(
                    false,
                    "ERROR: Invalid credentials provided",
                    null);
            return gson.toJson(postResponse);
        }

        // Verify credentials
        boolean verified = CredentialVerifier.verifyCredential(credential);
        if (!verified) {
            postResponse = new ServerResponse(
                    false,
                    "ERROR: Credentials could not be verified",
                    null);
            return gson.toJson(postResponse);
        }

        Credential existingCredential = DBOperations.getCredentialFor(credential.getDomain()).orElse(null);
        if (existingCredential != null) {
            //Update server's listed status
            Server server = DBOperations.getServer(credential.getDomain()).get();
            DBOperations.setListed(server.getDomain(), listedServer);

            //If existing credential does not work update it
            boolean existingVerified = CredentialVerifier.verifyCredential(existingCredential);
            if (!existingVerified) {
                DBOperations.removeCredential(existingCredential);
                DBOperations.addCredential(credential);
                postResponse = new ServerResponse(
                        false,
                        "UPDATE: Credential updated and set server as " +
                                (listedServer ? "listed" : "unlisted"),
                        null
                );
                MailSender.sendMails(
                        MailBuilder.getInstance().buildCredentialUpdateEmails(credential)
                );
                return gson.toJson(postResponse);
            }

            //If existing credential was working, just notify about new listing status
            postResponse = new ServerResponse(
                    false,
                    "UPDATE: Credential already exists for server. " +
                            "Server will now be " +
                            (listedServer ? "included in" : "excluded from") +
                            " the public lists and tables",
                    null
            );
            return gson.toJson(postResponse);
        }

        //Check if domain exists, if not add to domains table
        if (!DBOperations.getServer(credential.getDomain()).isPresent()) {
            boolean domainAdded = DBOperations.addServer(new Server(credential.getDomain(), listedServer));
            if (!domainAdded) {
                postResponse = new ServerResponse(
                        false,
                        "ERROR: Could not add/update domain to the database",
                        null
                );
                return gson.toJson(postResponse);
            }
        }
        //Update server's listing for new credentials for an existing server
        else {
            DBOperations.setListed(credential.getDomain(), listedServer);
            MailSender.sendMails(
                    MailBuilder.getInstance().buildCredentialUpdateEmails(credential)
            );
        }

        // Add credentials to database
        boolean dbAdded = DBOperations.addCredential(credential);
        if (!dbAdded) {
            postResponse = new ServerResponse(
                    false,
                    "ERROR: Could not add server with the provided credentials to the database",
                    null
            );
            return gson.toJson(postResponse);
        }

        postResponse = new ServerResponse(
                true,
                "Successfully added credentials",
                "/live/" + credential.getDomain()
        );
        return gson.toJson(postResponse);
    };

    public static TemplateViewRoute getLive = (request, response) -> {
        HashMap<String, Object> model = new HashMap<>();
        String domain = request.params("domain");
        if (!OneOffTestRunner.runOneOffTestsFor(domain)) {
            model.put("error_code", 404);
            String howToAdd = "You can add credentials by going to " + WebUtils.getRootUrlFrom(request) + "/add";
            model.put("error_msg", "No credentials for " + domain + "  found in the database. " + howToAdd);
            return new ModelAndView(model, "error.ftl");
        }
        model.put("domain", domain);
        return new ModelAndView(model, "live.ftl");
    };

    public static TemplateViewRoute getServer = (request, response) -> {
        HashMap<String, Object> model = new HashMap<>();
        String domain = request.params("domain");
        Server server = DBOperations.getServer(domain).orElse(null);
        if (server == null) {
            model.put("error_code", 404);
            model.put("error_msg", "Credentials unavailable for " + domain);
            return new ModelAndView(model, "error.ftl");
        }
        List<Result> results = DBOperations.getCurrentResultsForServer(domain);
        if (results.isEmpty()) {
            model.put("error_code", 404);
            model.put("error_msg", "Results unavailable for " + domain + ". Tests might still be running");
            return new ModelAndView(model, "error.ftl");
        }
        List<String> failedTests = results.stream()
                .filter(result -> !result.isSuccess())
                .map(result -> result.getTest().short_name())
                .collect(Collectors.toList());

        WebUtils.addResultStats(model, results);

        HashMap<String, String> help = Help.getInstance().getHelpFor(server.getSoftwareName()).orElse(null);
        model.put("helps", help);
        Instant lastRun = DBOperations.getLastRunFor(domain);
        model.put("domain", domain);
        model.put("results", results);
        model.put("historic_data", gson.toJson(DBOperations.getHistoricResultsGroupedByServer().get(domain)));
        model.put("softwareName", server.getSoftwareName());
        model.put("softwareVersion", server.getSoftwareVersion());
        model.put("timeSince", TimeUtils.getTimeSince(lastRun));
        model.put("timestamp", lastRun);
        model.put("tests", TestUtils.getComplianceTestMap());
        String imageUrl = WebUtils.getRootUrlFrom(request) + "/badge/" + domain;
        String resultUrl = WebUtils.getRootUrlFrom(request) + "/server/" + domain;
        model.put("badgeCode", "<a href='" + resultUrl + "'><img src='" + imageUrl + "'></a>");
        boolean mailExists = Configuration.getInstance().getMailConfig() != null;
        model.put("mailExists", mailExists);
        return new ModelAndView(model, "server.ftl");
    };

    public static TemplateViewRoute getTest = (request, response) -> {
        ComplianceTest test = TestUtils.getTestFrom(request.params("test"));
        HashMap<String, Object> model = new HashMap<>();
        if (test == null) {
            model.put("error_code", 404);
            model.put("error_msg", "Test " + request.params("test") + " not found");
            return new ModelAndView(model, "error.ftl");
        }
        model.put("test", test);
        Map<String, Boolean> results = DBOperations.getCurrentResultsForTest(test);

        if (results.isEmpty()) {
            model.put("error_code", 404);
            model.put("error_msg", "Results unavailable for " + test.full_name());
            return new ModelAndView(model, "error.ftl");
        } else {
            int passed = results.entrySet().stream()
                    .map(it -> it.getValue() ? 1 : 0)
                    .reduce((it, ac) -> ac + it)
                    .get();
            int percent = passed * 100 / results.size();
            model.put("stats", new HashMap<String, String>() {
                {
                    put("Servers compliant", percent + "%");
                }
            });
        }
        model.put("results", results);
        model.put("historic_data", gson.toJson(
                DBOperations.getHistoricResultsGroupedByTest()
                        .get(test.short_name())
        ));
        return new ModelAndView(model, "test.ftl");
    };

    public static TemplateViewRoute getBadge = (request, response) -> {
        response.type("image/svg+xml");
        HashMap<String, Object> model = new HashMap<>();
        String domain = request.params("domain");
        List<Result> results = DBOperations.getCurrentResultsForServer(domain);
        WebUtils.addResultStats(model, results);
        String resultLink = WebUtils.getRootUrlFrom(request) + "/server/" + domain;
        model.put("domain", domain);
        model.put("resultLink", resultLink);
        return new ModelAndView(model, "badge.ftl");
    };

    public static TemplateViewRoute getHistoricTable = (request, response) -> {
        HashMap<String, Object> model = new HashMap<>();
        int iterationNumber;
        try {
            iterationNumber = Integer.parseInt(request.params("iteration"));
            Iteration iteration = DBOperations.getIteration(iterationNumber);
            model.put("iteration", iteration);
            model.put("timeSince", TimeUtils.getTimeSince(iteration.getBegin()));
        } catch (Exception ex) {
            model.put("error_code", 404);
            model.put("error_msg", "ERROR: Invalid historical point requested");
            return new ModelAndView(model, "error.ftl");
        }
        Map<String, HashMap<String, Boolean>> resultsByServer = DBOperations.getHistoricalTableFor(iterationNumber);
        WebUtils.addDataForComplianceTable(model, resultsByServer);
        return new ModelAndView(model, "historic.ftl");
    };

    public static TemplateViewRoute getHistoricForServer = (request, response) -> {
        HashMap<String, Object> model = new HashMap<>();
        int iterationNumber;
        try {
            iterationNumber = Integer.parseInt(request.params("iteration"));
            Iteration iteration = DBOperations.getIteration(iterationNumber);
            model.put("iteration", iteration);
            model.put("timeSince", TimeUtils.getTimeSince(iteration.getBegin()));
        } catch (Exception ex) {
            model.put("error_code", 404);
            model.put("error_msg", "ERROR: Invalid historical point requested");
            return new ModelAndView(model, "error.ftl");
        }
        String domain = request.params("domain");
        model.put("domain", domain);
        List<Result> results = DBOperations.getHistoricalResultsFor(domain, iterationNumber);
        model.put("results", results);
        return new ModelAndView(model, "historic_server.ftl");
    };

    public static Route getConfirmation = (request, response) -> {
        String code = request.params("code");
        return MailVerification.verifyEmail(code);
    };

    public static Route postSubscription = (request, response) -> {
        MailVerification.removeExpiredRequests();
        String email = request.queryParams("email");
        String domain = request.queryParams("domain");
        String rootUrl = WebUtils.getRootUrlFrom(request);
        ServerResponse serverResponse;
        boolean validDomain = DBOperations.getServers(false).stream().map(Server::getDomain).anyMatch(it -> it.equals(domain));
        if (!validDomain) {
            serverResponse = new ServerResponse(false, "ERROR: Subscription request made for invalid domain", null);
            return gson.toJson(serverResponse);
        }
        EmailValidator validator = EmailValidator.getInstance();
        if (validator.isValid(email)) {
            boolean exists = DBOperations.getSubscribersFor(domain).stream()
                    .anyMatch(it -> it.getEmail().equals(email));
            if (exists) {
                serverResponse = new ServerResponse(false, "ERROR: You are already subscribed!", null);
                return gson.toJson(serverResponse);
            }
            MailVerification.addEmailToList(email, domain);
            serverResponse = new ServerResponse(true, "Verification mail sent", null);
            return gson.toJson(serverResponse);
        }
        serverResponse = new ServerResponse(false, "Invalid email ID", null);
        return gson.toJson(serverResponse);
    };

    public static Route getUnsubscribe = (request, response) -> {
        String code = request.params("code");
        Subscriber subscriber = DBOperations.removeSubscriber(code);
        if (subscriber == null) {
            return "Invalid unsubscription code";
        }
        return "Unsubscribed " + subscriber.getEmail()
                + " from receiving alerts, periodic compliance reports for "
                + subscriber.getDomain();
    };
}
