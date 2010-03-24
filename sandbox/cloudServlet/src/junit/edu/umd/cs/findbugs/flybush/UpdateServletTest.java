package edu.umd.cs.findbugs.flybush;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.SetBugLink;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.SetBugLink.Builder;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;

import javax.jdo.Query;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.encodeHash;
import static edu.umd.cs.findbugs.flybush.DbIssue.DbBugLinkType.GOOGLE_CODE;
import static edu.umd.cs.findbugs.flybush.DbIssue.DbBugLinkType.JIRA;
import static edu.umd.cs.findbugs.flybush.UpdateServlet.ONE_DAY_IN_MILLIS;

@SuppressWarnings({"UnusedDeclaration"})
public abstract class UpdateServletTest extends AbstractFlybushServletTest {

    @Override
    protected AbstractFlybushServlet createServlet() {
        return new UpdateServlet();
    }

    @SuppressWarnings({"unchecked"})
    public void testExpireSqlSessions() throws Exception {
        DbUser oldUser = persistenceHelper.createDbUser("http://some.website", "old@test.com");
        SqlCloudSession oldSession = persistenceHelper.createSqlCloudSession(100,
                                                                             new Date(System.currentTimeMillis() - 8 * ONE_DAY_IN_MILLIS),
                                                                             oldUser.createKeyObject());
        DbUser recentUser = persistenceHelper.createDbUser("http://some.website2", "recent@test.com");
        SqlCloudSession recentSession = persistenceHelper.createSqlCloudSession(101,
                                                                                new Date(System.currentTimeMillis() - 6 * ONE_DAY_IN_MILLIS),
                                                                                recentUser.createKeyObject());
        getPersistenceManager().makePersistentAll(oldUser, recentUser, oldSession, recentSession);

        assertEquals("old@test.com", getDbUser(findSqlSession(100).get(0).getUser()).getEmail());
        assertEquals("recent@test.com", getDbUser(findSqlSession(101).get(0).getUser()).getEmail());

        executeGet("/expire-sql-sessions");

        assertEquals(0, findSqlSession(100).size());
        assertEquals("recent@test.com", getDbUser(findSqlSession(101).get(0).getUser()).getEmail());
    }

    @SuppressWarnings({"unchecked"})
    private List<SqlCloudSession> findSqlSession(long sessionId) {
        return (List<SqlCloudSession>) getPersistenceManager().newQuery(
                "select from " + persistenceHelper.getSqlCloudSessionClass().getName()
                + " where randomID == :val").execute(Long.toString(sessionId));
    }

    public void testUploadIssueWithoutAuthenticating() throws Exception {
		Issue issue = createProtoIssue("fad");
		UploadIssues issuesToUpload = UploadIssues.newBuilder().setSessionId(555).addNewIssues(issue).build();
		executePost("/upload-issues", issuesToUpload.toByteArray());
		checkResponse(403);
	}

	@SuppressWarnings("unchecked")
	public void testUploadIssue() throws Exception {
        // setup
    	createCloudSession(555);
		Issue issue = createProtoIssue("fad");

        // execute
		UploadIssues issuesToUpload = UploadIssues.newBuilder().setSessionId(555).addNewIssues(issue).build();
		executePost("/upload-issues", issuesToUpload.toByteArray());
		checkResponse(200, "");

        // verify
        List<DbIssue> dbIssues = getAllIssuesFromDb();
		assertEquals(1, dbIssues.size());

        DbIssue dbIssue = dbIssues.get(0);
        FlybushServletTestUtil.checkIssuesEqualExceptTimestamps(dbIssue, issue);
        assertEquals(issue.getFirstSeen(), dbIssue.getFirstSeen());
        assertEquals(issue.getFirstSeen(), dbIssue.getLastSeen()); // upon initial upload, should be identical
	}

	@SuppressWarnings("unchecked")
	public void testUploadMultipleIssues() throws Exception {
    	createCloudSession(555);
		Issue issue1 = createProtoIssue("fad1");
		Issue issue2 = createProtoIssue("fad2");
		UploadIssues issuesToUpload = UploadIssues.newBuilder()
				.setSessionId(555).addNewIssues(issue1).addNewIssues(issue2)
				.build();
		executePost("/upload-issues", issuesToUpload.toByteArray());
		checkResponse(200, "");
		List<DbIssue> dbIssues = (List<DbIssue>) getPersistenceManager()
				.newQuery("select from " + persistenceHelper.getDbIssueClass().getName()).execute();
		assertEquals(2, dbIssues.size());

        FlybushServletTestUtil.checkIssuesEqualExceptTimestamps(dbIssues.get(0), issue1);
        FlybushServletTestUtil.checkIssuesEqualExceptTimestamps(dbIssues.get(1), issue2);
    }

	@SuppressWarnings("unchecked")
	public void testUploadIssuesWhichAlreadyExist() throws Exception {
    	createCloudSession(555);
        DbIssue oldDbIssue = FlybushServletTestUtil.createDbIssue("fad1", persistenceHelper);
        getPersistenceManager().makePersistent(oldDbIssue);
		Issue oldIssue = createProtoIssue("fad1");
		Issue newIssue = createProtoIssue("fad2");
		UploadIssues issuesToUpload = UploadIssues.newBuilder()
				.setSessionId(555)
				.addNewIssues(oldIssue)
				.addNewIssues(newIssue)
				.build();
		executePost("/upload-issues", issuesToUpload.toByteArray());
		checkResponse(200, "");
		List<DbIssue> dbIssues = (List<DbIssue>) getPersistenceManager()
				.newQuery("select from " + persistenceHelper.getDbIssueClass().getName() + " order by hash ascending").execute();
		assertEquals(2, dbIssues.size());

		assertEquals("fad1", dbIssues.get(0).getHash());
		assertEquals("fad2", dbIssues.get(1).getHash());
	}

	public void testUploadEvaluationNoAuth() throws Exception {
		executePost("/upload-evaluation", UploadEvaluation.newBuilder()
				.setSessionId(555)
				.setHash(encodeHash("fad"))
				.setEvaluation(createProtoEvaluation())
				.build().toByteArray());
		checkResponse(403, "not authenticated");
	}

	public void testUploadEvaluationWithoutFindIssuesFirst() throws Exception {
		createCloudSession(555);

        DbIssue dbIssue = FlybushServletTestUtil.createDbIssue("fad", persistenceHelper);
        getPersistenceManager().makePersistent(dbIssue);
		Evaluation protoEval = createProtoEvaluation();
		executePost("/upload-evaluation", UploadEvaluation.newBuilder()
				.setSessionId(555)
				.setHash(encodeHash("fad"))
				.setEvaluation(protoEval)
				.build().toByteArray());
		checkResponse(200);
        getPersistenceManager().refresh(dbIssue);
		assertEquals(1, dbIssue.getEvaluations().size());
		DbEvaluation dbEval = dbIssue.getEvaluations().iterator().next();
		assertEquals(protoEval.getComment(), dbEval.getComment());
		assertEquals(protoEval.getDesignation(), dbEval.getDesignation());
		assertEquals(protoEval.getWhen(), dbEval.getWhen());
		assertEquals("my@email.com", getDbUser(dbEval.getWho()).getEmail());
		assertNull(dbEval.getInvocationKey());
	}

	public void testUploadEvaluationMoreThan500chars() throws Exception {
		createCloudSession(555);

        DbIssue dbIssue = FlybushServletTestUtil.createDbIssue("fad", persistenceHelper);
        getPersistenceManager().makePersistent(dbIssue);
        char[] array = new char[600];
        Arrays.fill(array, 'x');
        Evaluation protoEval = Evaluation.newBuilder()
                .setDesignation("MUST_FIX")
                .setComment(new String(array))
                .setWhen(100)
                .build();
		executePost("/upload-evaluation", UploadEvaluation.newBuilder()
				.setSessionId(555)
				.setHash(encodeHash("fad"))
				.setEvaluation(protoEval)
				.build().toByteArray());
		checkResponse(200);
        getPersistenceManager().refresh(dbIssue);
		assertEquals(1, dbIssue.getEvaluations().size());
		DbEvaluation dbEval = dbIssue.getEvaluations().iterator().next();
		assertEquals(protoEval.getComment(), dbEval.getComment());
		assertEquals(protoEval.getDesignation(), dbEval.getDesignation());
		assertEquals(protoEval.getWhen(), dbEval.getWhen());
		assertEquals("my@email.com", getDbUser(dbEval.getWho()).getEmail());
		assertNull(dbEval.getInvocationKey());
	}

	public void testUploadEvaluationWithFindIssuesFirst() throws Exception {
		createCloudSession(555);

		executePost(authServlet, "/log-in", LogIn.newBuilder()
				.setSessionId(555)
                .setAnalysisTimestamp(100)
				.build().toByteArray());
		checkResponse(200);
		initServletAndMocks();

        QueryServlet queryServlet = new QueryServlet();
        queryServlet.setPersistenceHelper(testHelper.createPersistenceHelper(getPersistenceManager()));
        initServletAndMocks();
        executePost(queryServlet, "/find-issues", FindIssues.newBuilder()
				.setSessionId(555)
				.addMyIssueHashes(encodeHash("abc"))
				.build().toByteArray());
		checkResponse(200);
		initServletAndMocks();

        DbIssue dbIssue = FlybushServletTestUtil.createDbIssue("fad", persistenceHelper);
        getPersistenceManager().makePersistent(dbIssue);
		Evaluation protoEval = createProtoEvaluation();
		executePost("/upload-evaluation", UploadEvaluation.newBuilder()
				.setSessionId(555)
				.setHash(encodeHash("fad"))
				.setEvaluation(protoEval)
				.build().toByteArray());
		checkResponse(200);
		assertEquals(1, dbIssue.getEvaluations().size());
		DbEvaluation dbEval = dbIssue.getEvaluations().iterator().next();
		assertEquals(protoEval.getComment(), dbEval.getComment());
		assertEquals(protoEval.getDesignation(), dbEval.getDesignation());
		assertEquals(protoEval.getWhen(), dbEval.getWhen());
		assertEquals("my@email.com", getDbUser(dbEval.getWho()).getEmail());
		Object invocationId = dbEval.getInvocationKey();
		assertNotNull(invocationId);
		DbInvocation invocation = persistenceHelper.getObjectById(getPersistenceManager(),
                                                                  persistenceHelper.getDbInvocationClass(),
                                                                  invocationId);
		assertEquals("my@email.com", getDbUser(invocation.getWho()).getEmail());
		assertEquals(100, invocation.getStartTime());
	}

	public void testUploadEvaluationNonexistentIssue() throws Exception {
		createCloudSession(555);

		Evaluation protoEval = createProtoEvaluation();
		executePost("/upload-evaluation", UploadEvaluation.newBuilder()
				.setSessionId(555)
				.setHash(encodeHash("faf"))
				.setEvaluation(protoEval)
				.build().toByteArray());
		checkResponse(404, "no such issue faf\n");
	}

    public void testSetBugLinkNotAuthenticated() throws Exception {
        setBugLinkExpectResponse(403, "fad", GOOGLE_CODE, "http://my.bug/123");
    }

    public void testSetBugLinkNonexistentBug() throws Exception {
        createCloudSession(555);
        setBugLinkExpectResponse(404, "fad", GOOGLE_CODE, "http://my.bug/123");
    }

    public void testSetBugLinkGoogleCode() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", GOOGLE_CODE, "http://my.bug/123");
        checkBugLinkInDb(GOOGLE_CODE, "http://my.bug/123");
    }

    public void testSetBugLinkJira() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", JIRA, "http://my.bug/123");
        checkBugLinkInDb(JIRA, "http://my.bug/123");
    }

    public void testSetBugLinkNullType() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", null, "http://my.bug/123");
        checkBugLinkInDb(null, "http://my.bug/123");
    }

    public void testSetBugLinkTrimsSpace() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", GOOGLE_CODE, "  http://my.bug/123   ");
        checkBugLinkInDb(GOOGLE_CODE, "http://my.bug/123");
    }

    public void testUpdateExistingBugLink() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", GOOGLE_CODE, "http://my.bug/123");
        checkBugLinkInDb(GOOGLE_CODE, "http://my.bug/123");
        setBugLink("fad", GOOGLE_CODE, "http://my.bug/456");
        checkBugLinkInDb(GOOGLE_CODE, "http://my.bug/456");
    }

    public void testClearBugLink() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", GOOGLE_CODE, "http://my.bug/123");
        checkBugLinkInDb(GOOGLE_CODE, "http://my.bug/123");
        setBugLink("fad", GOOGLE_CODE, null);
        checkBugLinkInDb(GOOGLE_CODE, null);
    }

    public void testClearBugLinkWithEmptyString() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", GOOGLE_CODE, "http://my.bug/123");
        checkBugLinkInDb(GOOGLE_CODE, "http://my.bug/123");
        setBugLink("fad", GOOGLE_CODE, "");
        checkBugLinkInDb(GOOGLE_CODE, null);
    }

    public void testClearBugLinkWithSpace() throws Exception {
        createCloudSession(555);
        uploadIssue("fad");
        setBugLink("fad", GOOGLE_CODE, "http://my.bug/123");
        checkBugLinkInDb(GOOGLE_CODE, "http://my.bug/123");
        setBugLink("fad", GOOGLE_CODE, "  ");
        checkBugLinkInDb(GOOGLE_CODE, null);
    }

    // TODO: I suspect this doesn't work due to DatastoreService and PersistenceManager sync issues
    @SuppressWarnings({"unchecked", "ConstantConditions", "UnusedDeclaration"})
    public void BROKEN_testClearAllData() throws Exception {
    	createCloudSession(555);

        DbIssue foundIssue = FlybushServletTestUtil.createDbIssue("fad1", persistenceHelper);
        DbEvaluation eval1 = createEvaluation(foundIssue, "first", 100);
        DbEvaluation eval2 = createEvaluation(foundIssue, "second", 200);
        DbEvaluation eval3 = createEvaluation(foundIssue, "first", 300);
		foundIssue.addEvaluation(eval1);
		foundIssue.addEvaluation(eval2);
		foundIssue.addEvaluation(eval3);

		// apparently the evaluation is automatically persisted. throws
		// exception when attempting to persist the eval with the issue.
        getPersistenceManager().makePersistent(foundIssue);

    	executePost("/clear-all-data", new byte[0]);
		checkResponse(200);


        getPersistenceManager().close();
        testHelper.initPersistenceManager();

        for (Class<?> cls : Arrays.asList(persistenceHelper.getDbIssueClass(),
                                          persistenceHelper.getDbIssueClass(),
                                          persistenceHelper.getDbInvocationClass(),
                                          persistenceHelper.getSqlCloudSessionClass())) {
            try {
                List objs = (List) getPersistenceManager().newQuery("select from " + cls.getName()).execute();
                fail("some entities still exist: " + cls.getSimpleName() + ": " + objs);
            } catch (Exception ignored) {
            }
        }
	}

	// ========================= end of tests ================================

    private void checkBugLinkInDb(DbIssue.DbBugLinkType expectedType, String expectedUrl) {
        List<DbIssue> dbIssues = getAllIssuesFromDb();
        assertEquals(1, dbIssues.size());
        DbIssue issue = dbIssues.get(0);
        String dbBugLink = issue.getBugLink();
        if (expectedUrl == null)
            assertNull(dbBugLink);
        else
            assertEquals(expectedUrl, dbBugLink);

        DbIssue.DbBugLinkType dbBugLinkType = issue.getBugLinkType();
        if (expectedType == null)
            assertNull("" + dbBugLinkType, dbBugLinkType);
        else
            assertEquals(expectedType, dbBugLinkType);
    }

    @SuppressWarnings("unchecked")
    private List<DbIssue> getAllIssuesFromDb() {
        Query query = getPersistenceManager().newQuery("select from " + persistenceHelper.getDbIssueClass().getName());
        return (List<DbIssue>) query.execute();
    }

    private void setBugLink(String hash, DbIssue.DbBugLinkType linkType, String link) throws IOException, ServletException {
        setBugLinkExpectResponse(200, hash, linkType, link);
    }

    private void setBugLinkExpectResponse(int responseCode, String hash, DbIssue.DbBugLinkType linkType, String link) throws IOException, ServletException {
        initServletAndMocks();
        Builder setBugLink = SetBugLink.newBuilder()
                .setSessionId(555)
                .setHash(encodeHash(hash));
        if (link != null)
            setBugLink.setUrl(link);
        if (linkType != null) {
            setBugLink.setBugLinkType(ProtoClasses.BugLinkType.valueOf(linkType.name()));
        }

        executePost("/set-bug-link", setBugLink.build().toByteArray());
        checkResponse(responseCode);
    }

    private void uploadIssue(String hash) throws IOException {
        Issue issue = createProtoIssue(hash);

        // create issue
        UploadIssues issuesToUpload = UploadIssues.newBuilder().setSessionId(555).addNewIssues(issue).build();
        executePost("/upload-issues", issuesToUpload.toByteArray());
        checkResponse(200);

        // verify initial upload
        List<DbIssue> dbIssues = getAllIssuesFromDb();
        assertEquals(1, dbIssues.size());
        assertNull(dbIssues.get(0).getBugLink());
    }

	private Evaluation createProtoEvaluation() {
        return Evaluation.newBuilder()
                .setDesignation("MUST_FIX")
                .setComment("my comment")
                .setWhen(100)
                .build();
	}

    private Issue createProtoIssue(String patternAndHash) {
		patternAndHash = AppEngineProtoUtil.normalizeHash(patternAndHash);
		Issue.Builder issueBuilder = Issue.newBuilder();
		issueBuilder.setHash(encodeHash(patternAndHash));
		issueBuilder.setBugPattern(patternAndHash);
		issueBuilder.setPriority(2);
		issueBuilder.setPrimaryClass("my.class");
		issueBuilder.setFirstSeen(100);
		issueBuilder.setLastSeen(200);

		return issueBuilder.build();
	}
}