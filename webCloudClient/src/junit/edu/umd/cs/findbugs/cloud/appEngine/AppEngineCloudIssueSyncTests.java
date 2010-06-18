package edu.umd.cs.findbugs.cloud.appEngine;

import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static edu.umd.cs.findbugs.cloud.Cloud.SigninState.UNAUTHENTICATED;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AppEngineCloudIssueSyncTests extends AbstractAppEngineCloudTest {

	public void testFindIssuesAllFound() throws IOException, InterruptedException {
		// set up mocks
		final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream()).thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        ByteArrayOutputStream findIssuesOutput = setupResponseCodeAndOutputStream(findIssuesConnection);

		// execution
		MockAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConnection);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
		cloud.bugsPopulated();
		cloud.initiateCommunication();
		cloud.waitUntilIssueDataDownloaded();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());

        // verify find-issues
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
		verify(findIssuesConnection).connect();
		FindIssues hashes = FindIssues.parseFrom(findIssuesOutput.toByteArray());
		assertEquals(1, hashes.getMyIssueHashesCount());
		List<String> hashesFromFindIssues = AppEngineProtoUtil.decodeHashes(hashes.getMyIssueHashesList());
		assertTrue(hashesFromFindIssues.contains(foundIssue.getInstanceHash()));

		// verify processing of found issues
		assertEquals(SAMPLE_DATE+100, cloud.getFirstSeen(foundIssue));
		assertEquals(SAMPLE_DATE+500, cloud.getUserTimestamp(foundIssue));
		assertEquals("latest comment", cloud.getUserEvaluation(foundIssue));
		assertEquals(UserDesignation.MUST_FIX, cloud.getUserDesignation(foundIssue));

		BugDesignation primaryDesignation = cloud.getPrimaryDesignation(foundIssue);
		assertNotNull(primaryDesignation);
		assertEquals("latest comment", primaryDesignation.getAnnotationText());
		assertEquals(SAMPLE_DATE+500, primaryDesignation.getTimestamp());
		assertEquals("MUST_FIX", primaryDesignation.getDesignationKey());
		assertEquals("test@example.com", primaryDesignation.getUser());
	}

	public void testFindIssuesNetworkFailure() throws IOException, InterruptedException {
		final HttpURLConnection findIssuesConn = mock(HttpURLConnection.class);
        when(findIssuesConn.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(findIssuesConn.getResponseCode()).thenReturn(500);
        when(findIssuesConn.getOutputStream()).thenReturn(outputStream);

        // execution
		final MockAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConn);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        cloud.waitUntilIssueDataDownloaded();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());

        assertEquals(1, cloud.urlsRequested.size());
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
	}

	public void testLogInAndUploadIssues() throws IOException, InterruptedException {
        addMissingIssue = true;

		// set up mocks
		final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream()).thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        ByteArrayOutputStream findIssuesOutput = setupResponseCodeAndOutputStream(findIssuesConnection);

		final HttpURLConnection logInConnection = mock(HttpURLConnection.class);
		ByteArrayOutputStream logInOutput = setupResponseCodeAndOutputStream(logInConnection);

		HttpURLConnection uploadConnection = mock(HttpURLConnection.class);
		ByteArrayOutputStream uploadIssuesBuffer = setupResponseCodeAndOutputStream(uploadConnection);

		// execution
		MockAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConnection, logInConnection, uploadConnection);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        cloud.waitUntilIssuesUploaded(5, TimeUnit.SECONDS);

        assertEquals(Cloud.SigninState.SIGNED_IN, cloud.getSigninState());

        // verify find-issues
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
		verify(findIssuesConnection).connect();
		FindIssues hashes = FindIssues.parseFrom(findIssuesOutput.toByteArray());
		assertEquals(2, hashes.getMyIssueHashesCount());
		List<String> hashesFromFindIssues = AppEngineProtoUtil.decodeHashes(hashes.getMyIssueHashesList());
		assertTrue(hashesFromFindIssues.contains(foundIssue.getInstanceHash()));
		assertTrue(hashesFromFindIssues.contains(missingIssue.getInstanceHash()));

		// verify log-in
        assertEquals("/log-in", cloud.urlsRequested.get(1));
		verify(logInConnection).connect();
		LogIn logIn = LogIn.parseFrom(logInOutput.toByteArray());
        assertEquals(cloud.getBugCollection().getAnalysisTimestamp(), logIn.getAnalysisTimestamp());

		// verify processing of found issues
		assertEquals(SAMPLE_DATE+100, cloud.getFirstSeen(foundIssue));
		assertEquals(SAMPLE_DATE+500, cloud.getUserTimestamp(foundIssue));
		assertEquals("latest comment", cloud.getUserEvaluation(foundIssue));
		assertEquals(UserDesignation.MUST_FIX, cloud.getUserDesignation(foundIssue));

		BugDesignation primaryDesignation = cloud.getPrimaryDesignation(foundIssue);
		assertNotNull(primaryDesignation);
		assertEquals("latest comment", primaryDesignation.getAnnotationText());
		assertEquals(SAMPLE_DATE+500, primaryDesignation.getTimestamp());
		assertEquals("MUST_FIX", primaryDesignation.getDesignationKey());
		assertEquals("test@example.com", primaryDesignation.getUser());

		// verify uploaded issues

        assertEquals("/upload-issues", cloud.urlsRequested.get(2));
        UploadIssues uploadedIssues = UploadIssues.parseFrom(uploadIssuesBuffer.toByteArray());
		assertEquals(1, uploadedIssues.getNewIssuesCount());
		checkIssuesEqual(missingIssue, uploadedIssues.getNewIssues(0));
	}

	public void testUserCancelsLogInAndUploadIssues() throws IOException, InterruptedException {
        addMissingIssue = true;

		// set up mocks
		final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream()).thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        setupResponseCodeAndOutputStream(findIssuesConnection);

		// execution
		MockAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConnection);
        when(cloud.mockGuiCallback.showConfirmDialog(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(-1);
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        cloud.waitUntilIssuesUploaded(5, TimeUnit.SECONDS);

        assertEquals(UNAUTHENTICATED, cloud.getSigninState());

        // verify
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
	}

	public void testDontUploadInTextMode() throws IOException, InterruptedException {
        addMissingIssue = true;

		// set up mocks
		final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream()).thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        setupResponseCodeAndOutputStream(findIssuesConnection);

		// execution
		MockAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConnection);
        when(cloud.mockGuiCallback.isHeadless()).thenReturn(true);
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        cloud.waitUntilIssuesUploaded(5, TimeUnit.SECONDS);

        assertEquals(UNAUTHENTICATED, cloud.getSigninState());

        // verify
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
	}

	@SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void testLogInAndUploadIssuesFailsDuringSignIn() throws IOException, InterruptedException {
        addMissingIssue = true;

		// set up mocks
		final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream()).thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        setupResponseCodeAndOutputStream(findIssuesConnection);

		// execution
		MockAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConnection);
        AppEngineCloudNetworkClient spyNetworkClient = cloud.createSpyNetworkClient();
        Mockito.doThrow(new IOException()).when(spyNetworkClient).signIn(Mockito.anyBoolean());
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        cloud.waitUntilIssuesUploaded(5, TimeUnit.SECONDS);

        assertEquals(Cloud.SigninState.SIGNIN_FAILED, cloud.getSigninState());

        // verify
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
	}

    // =================================== end of tests ===========================================

	private void checkIssuesEqual(BugInstance issue, Issue uploadedIssue) {
		assertEquals(issue.getInstanceHash(), AppEngineProtoUtil.decodeHash(uploadedIssue.getHash()));
		assertEquals(issue.getType(), uploadedIssue.getBugPattern());
		assertEquals(issue.getPriority(), uploadedIssue.getPriority());
		assertEquals(0, uploadedIssue.getLastSeen());
		assertEquals(issue.getPrimaryClass().getClassName(), uploadedIssue.getPrimaryClass());
	}
}