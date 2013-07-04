package teammates.test.cases.ui.browsertests;

import static org.testng.AssertJUnit.*;

import java.text.ParseException;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Text;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.util.Const;
import teammates.common.util.Url;
import teammates.test.driver.BackDoor;
import teammates.test.pageobjects.Browser;
import teammates.test.pageobjects.BrowserPool;
import teammates.test.pageobjects.InstructorFeedbackEditPage;
import teammates.test.pageobjects.InstructorFeedbackPage;

/**
 * Covers the 'Edit Feedback Session' page for instructors. 
 * SUT is {@link InstructorFeedbackEditPage}.
 */
public class InstructorFeedbackEditPageUiTest extends BaseUiTestCase {
	private static Browser browser;
	private static InstructorFeedbackEditPage feedbackEditPage;
	private static DataBundle testData;
	private static String instructorId;
	private static String courseId;
	private static String feedbackSessionName;
	/** This contains data for the feedback session to be edited during testing */
	private static FeedbackSessionAttributes editedSession;
	
	@BeforeClass
	public static void classSetup() throws Exception {
		printTestClassHeader();
		testData = loadDataBundle("/InstructorFeedbackEditPageUiTest.json");
		restoreTestDataOnServer(testData);
		
		editedSession = testData.feedbackSessions.get("openSession");
		editedSession.gracePeriod = 30;
		editedSession.sessionVisibleFromTime = Const.TIME_REPRESENTS_FOLLOW_OPENING;
		editedSession.resultsVisibleFromTime = Const.TIME_REPRESENTS_FOLLOW_VISIBLE;
		editedSession.instructions = new Text("Please fill in the edited feedback session.");
		
		instructorId = testData.accounts.get("instructorWithSessions").googleId;
		courseId = testData.courses.get("course").id;
		feedbackSessionName = testData.feedbackSessions.get("openSession").feedbackSessionName;
		
		browser = BrowserPool.getBrowser();
		
	}
	
	@Test
	public void allTests() throws Exception{
		testContent();
		
		testEditSessionLink();
		testInputValidationForSession();		
		testEditSessionAction();
		
		testNewQuestionLink();
		testInputValidationForQuestion();
		testAddQuestionAction();
		
		testEditQuestionLink();
		testEditQuestionAction();
		
		testDeleteQuestionAction();
		
		testDeleteSessionAction();
	}

	private void testContent() throws Exception{
		
		______TS("no questions");
		
		feedbackEditPage = getFeedbackEditPage();
		feedbackEditPage.verifyHtml("/instructorFeedbackEditEmpty.html");
	}
	
	private void testEditSessionLink(){
		______TS("edit session link");
		assertEquals(true, feedbackEditPage.clickEditSessionButton());		
	}

	private void testInputValidationForSession() throws ParseException {
		
		______TS("client-side input validation");
		
		// They are to be removed after confirming coverage by JS tests.
		
		// Empty instructions
		feedbackEditPage.fillInstructionsBox("");
		feedbackEditPage.clickSaveSessionButton();
		assertEquals(Const.StatusMessages.FIELDS_EMPTY, feedbackEditPage.getStatus());

		// Empty custom publishTime	
		feedbackEditPage.fillInstructionsBox("instructions filled.");
		feedbackEditPage.clearField(Const.ParamsNames.FEEDBACK_SESSION_PUBLISHDATE);
		feedbackEditPage.clickSaveSessionButton();
		assertEquals(Const.StatusMessages.FIELDS_EMPTY, feedbackEditPage.getStatus());

		// Empty custom visibleTime
		feedbackEditPage.clickDefaultPublishTimeButton();
		feedbackEditPage.clearField(Const.ParamsNames.FEEDBACK_SESSION_VISIBLEDATE);
		feedbackEditPage.clickSaveSessionButton();
		assertEquals(Const.StatusMessages.FIELDS_EMPTY, feedbackEditPage.getStatus());


	}

	private void testEditSessionAction() throws Exception{
		
		______TS("typical success case");
		
		feedbackEditPage.clickDefaultPublishTimeButton();
		feedbackEditPage.clickDefaultVisibleTimeButton();
		feedbackEditPage.editFeedbackSession(editedSession.startTime, editedSession.endTime,
				editedSession.instructions,
				editedSession.gracePeriod);
		feedbackEditPage.verifyStatus(Const.StatusMessages.FEEDBACK_SESSION_EDITED);
		FeedbackSessionAttributes savedSession = 
				BackDoor.getFeedbackSession(editedSession.courseId, editedSession.feedbackSessionName);
		assertEquals(editedSession.toString(), savedSession.toString());
		feedbackEditPage.verifyHtml("/instructorFeedbackEditSuccess.html");
	}

	
	private void testNewQuestionLink() {
		
		______TS("new question (frame) link");
		
		assertEquals(true, feedbackEditPage.clickNewQuestionButton());		
	}
	
	private void testInputValidationForQuestion() {
		
		______TS("empty question text");
		
		feedbackEditPage.clickAddQuestionButton();
		assertEquals(Const.StatusMessages.FEEDBACK_QUESTION_TEXTINVALID, feedbackEditPage.getStatus());
		
		______TS("empty number of max respondants field");
		
		feedbackEditPage.fillQuestionBox("filled qn");
		feedbackEditPage.selectRecipientsToBeStudents();
		feedbackEditPage.fillNumOfEntitiesToGiveFeedbackToBox("");
		feedbackEditPage.clickAddQuestionButton();
		assertEquals(Const.StatusMessages.FEEDBACK_QUESTION_NUMBEROFENTITIESINVALID, feedbackEditPage.getStatus());

	}

	private void testAddQuestionAction() {
		
		______TS("add question action success");

		feedbackEditPage.clickMaxNumberOfRecipientsButton();
		feedbackEditPage.clickAddQuestionButton();
		assertEquals(Const.StatusMessages.FEEDBACK_QUESTION_ADDED, feedbackEditPage.getStatus());
		assertNotNull(BackDoor.getFeedbackQuestion(courseId, feedbackSessionName, 1));
		feedbackEditPage.verifyHtml("/instructorFeedbackQuestionAddSuccess.html");
	}
	
	private void testEditQuestionLink() {
		
		______TS("edit question link");
		
		assertEquals(true, feedbackEditPage.clickEditQuestionButton());	
	}
	
	private void testEditQuestionAction() {
		
		______TS("edit question success");
		
		feedbackEditPage.fillEditQuestionBox("edited qn text");
		feedbackEditPage.clickSaveExistingQuestionButton();
		assertEquals(Const.StatusMessages.FEEDBACK_QUESTION_EDITED, feedbackEditPage.getStatus());
		feedbackEditPage.verifyHtml("/instructorFeedbackQuestionEditSuccess.html");
	}

	private void testDeleteQuestionAction() {
		
		______TS("qn delete then cancel");
		
		feedbackEditPage.clickAndCancel(feedbackEditPage.getDeleteQuestionLink());		
		assertNotNull(BackDoor.getFeedbackQuestion(courseId, feedbackSessionName, 1));
		
		______TS("qn delete then accept");
		
		feedbackEditPage.clickAndConfirm(feedbackEditPage.getDeleteQuestionLink());
		assertEquals(Const.StatusMessages.FEEDBACK_QUESTION_DELETED, feedbackEditPage.getStatus());
		assertNull(BackDoor.getFeedbackQuestion(courseId, feedbackSessionName, 1));
	}
	
	private void testDeleteSessionAction() {
		
		______TS("session delete then cancel");
		
		feedbackEditPage.clickAndCancel(feedbackEditPage.getDeleteSessionLink());		
		assertNotNull(BackDoor.getFeedbackSession(courseId, feedbackSessionName));
		
		______TS("session delete then accept");
		
		// check redirect to main feedback page
		InstructorFeedbackPage feedbackPage = feedbackEditPage.deleteSession();
		assertContains(Const.StatusMessages.FEEDBACK_SESSION_DELETED, feedbackPage.getStatus());
		assertNull(BackDoor.getFeedbackSession(courseId, feedbackSessionName));
		
	}


	@AfterClass
	public static void classTearDown() throws Exception {
		BrowserPool.release(browser);
	}

	private InstructorFeedbackEditPage getFeedbackEditPage() {		
		Url feedbackPageLink = new Url(Const.ActionURIs.INSTRUCTOR_FEEDBACK_EDIT_PAGE).
				withUserId(instructorId).withCourseId(courseId).withSessionName(feedbackSessionName);
		return loginAdminToPage(browser, feedbackPageLink, InstructorFeedbackEditPage.class);
	}

}