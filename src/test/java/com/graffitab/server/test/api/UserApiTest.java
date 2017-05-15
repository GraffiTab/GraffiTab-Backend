package com.graffitab.server.test.api;

import com.graffitab.server.GraffitabApplication;
import com.graffitab.server.api.controller.user.MeApiController;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.ActivityService;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.email.Email;
import com.graffitab.server.service.email.EmailSenderService;
import com.graffitab.server.service.email.EmailService;
import com.graffitab.server.service.image.ImageUtilsService;
import com.graffitab.server.service.store.AmazonS3DatastoreService;
import com.graffitab.server.service.store.DatastoreService;
import com.graffitab.server.service.user.RunAsUser;
import com.graffitab.server.service.user.UserService;
import com.graffitab.server.util.GuidGenerator;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import javax.annotation.Resource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.Filter;
import javax.transaction.Transactional;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//https://github.com/arpitaggarwal/spring-boot-redis

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes={TestDatabaseConfig.class, GraffitabApplication.class})
@ActiveProfiles("unit-test")
public class UserApiTest {

	    @Resource
	    private WebApplicationContext ctx;

		@Mock
		private MessageSource messageSource;

		@Resource
		@InjectMocks
		private EmailService emailService;

	    @Resource
	    private UserService userService;

	    @Resource
	    private MeApiController meApiController;

		@Resource
		private HibernateDaoImpl<User, Long> userDao;

		@Resource
		private TransactionUtils transactionUtils;

		@Resource
		private ActivityService activityService;

	    @Autowired
	    private Filter springSecurityFilterChain;

	    @Autowired
	    private ImageUtilsService imageUtilsService;

		@Autowired
		private AmazonS3DatastoreService datastoreService;

	    private Wiser wiser;

	    private MockMvc mockMvc;

	    private static Integer currentSmtpPort;

	    private static User user1;

	    private static User user2;

		@Before
		public void setUp() throws Exception {
			this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
					.addFilters(springSecurityFilterChain).build();

			wiser = startWiser();
			MockitoAnnotations.initMocks(this);
			given(this.messageSource.getMessage("email.subject.welcome", null, Locale.ENGLISH))
					.willReturn("Welcome to GraffiTab");

			replaceEmailSenderService();
			replaceDatastoreService();
		}

		// Runs after every test
		@After
		public void nukeDb() {
			transactionUtils.executeInTransaction(() -> {
				userDao.createSQLQuery("TRUNCATE SCHEMA public AND COMMIT");
			});
		}

	    @Test
	    public void getUserByIdTest() throws Exception {
	    	User loggedInUser = createUser();
	    	User testUser = createUser2();
	        mockMvc.perform(get("/api/users/" + testUser.getId())
	        		.with(user(loggedInUser))
	        		.contentType(MediaType.APPLICATION_JSON))
	                .andExpect(status().isOk())
	                .andExpect(content().contentType("application/json;charset=UTF-8"))
	                .andExpect(jsonPath("$.user.id").value(testUser.getId().intValue()));
	    }

	    @Test
	    public void createUserTest() throws Exception {
	    	fillTestUser();
	    	InputStream in = this.getClass().getResourceAsStream("/api/user.json");
	    	String json = IOUtils.toString(in);

	    	mockMvc.perform(post("/api/users")
	                .contentType("application/json;charset=UTF-8")
	                .content(json))
	                .andExpect(status().is(201))
	                .andExpect(content().contentType("application/json;charset=UTF-8"))
	                .andExpect(jsonPath("$.result").value("OK"));

	    	pollForEmail(wiser);

	    	List<WiserMessage> wiserMessages = wiser.getMessages();
	    	assertEquals(wiserMessages.size(), 1);
	    	WiserMessage message = wiserMessages.get(0);
	    	assertEquals("Welcome to GraffiTab", message.getMimeMessage().getSubject());
	    }

	    @Test
	    public void followUserTest() throws Exception {

	    	User currentUser = createUser();
	    	User userToFollow = createUser2();

	    	RunAsUser.set(currentUser);

	    	try {

				// Follow someone
		    	ResultActions followResultActions = mockMvc.perform(post("/api/users/" + userToFollow.getId() + "/followers")
		    			.with(user(currentUser)))
		                .andExpect(status().is(200))
		                .andExpect(content().contentType("application/json;charset=UTF-8"));

		    	assertFollowEndpointResponse(followResultActions);

				//TODO: assertFollowActivityIsCorrect();

				// Check the followers
				ResultActions getFollowersResultActions = mockMvc.perform(get("/api/users/" + userToFollow.getId() + "/followers" )
						.with(user(currentUser)));

				assertGetFollowersResponse(getFollowersResultActions, currentUser, 1, 0);

	    	} finally {
	    		RunAsUser.clear();
	    	}
	    }

		@Test
		public void followYourselfTest() throws Exception {

			User currentUser = createUser();
			RunAsUser.set(currentUser);
			mockMvc.perform(post("/api/users/" + currentUser.getId() + "/followers")
					.with(user(currentUser)))
					.andExpect(status().is(400))
					.andExpect(content().contentType("application/json;charset=UTF-8"))
					.andExpect(jsonPath("$.resultMessage").value("You cannot follow yourself"));
			RunAsUser.clear();
		}

	    @Test
	    public void unFollowUserTest() throws Exception {
	    	User currentUser = createUser();
	    	User userToFollow = createUser2();

	    	RunAsUser.set(currentUser);

	    	try {

		    	// Follow first
		    	mockMvc.perform(post("/api/users/" + userToFollow.getId() + "/followers")
		    			.with(user(currentUser)))
		                .andExpect(status().is(200));

		    	// Unfollow afterwards
		    	mockMvc.perform(delete("/api/users/" + userToFollow.getId() + "/followers")
		    			.with(user(currentUser)))
		                .andExpect(status().is(200));

	    	} finally {
	    		RunAsUser.clear();
	    	}
	    }

	    @Test
	    @Transactional
	    public void addAvatarAssetTest() throws Exception {
	    	User loggedInUser = createUser();
	    	InputStream in = this.getClass().getResourceAsStream("/api/test-asset.jpg");
			MockMultipartFile assetFile = new MockMultipartFile("file", "test-asset.jpg", "image/jpeg", in);
			HashMap<String, String> contentTypeParams = new HashMap<>();
			contentTypeParams.put("boundary", "265001916915724");
			MediaType mediaType = new MediaType("multipart", "form-data", contentTypeParams);
			mockMvc.perform(MockMvcRequestBuilders.fileUpload("/api/users/me/avatar")
					.file(assetFile)
	    			.with(user(loggedInUser))
					.contentType(mediaType))
	                .andExpect(status().is(200))
	                .andExpect(content().contentType("application/json;charset=UTF-8"))
	                .andExpect(jsonPath("$.asset.guid").isNotEmpty())
	                .andExpect(jsonPath("$.asset.type").value(Asset.AssetType.IMAGE.name()))
					.andExpect(jsonPath("$.asset.state").value(Asset.AssetState.PROCESSING.name()));

	    }

	@Test
	@Transactional
	public void addCoverAssetTest() throws Exception {
		User loggedInUser = createUser();
		InputStream in = this.getClass().getResourceAsStream("/api/test-asset.jpg");
		MockMultipartFile assetFile = new MockMultipartFile("file", "test-asset.jpg", "image/jpeg", in);
		HashMap<String, String> contentTypeParams = new HashMap<>();
		contentTypeParams.put("boundary", "265001916915724");
		MediaType mediaType = new MediaType("multipart", "form-data", contentTypeParams);
		MvcResult result  = mockMvc.perform(MockMvcRequestBuilders.fileUpload("/api/users/me/cover")
				.file(assetFile)
				.with(user(loggedInUser))
				.contentType(mediaType))
				.andExpect(status().is(200))
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(jsonPath("$.asset.guid").isNotEmpty())
				.andExpect(jsonPath("$.asset.type").value(Asset.AssetType.IMAGE.name()))
				.andExpect(jsonPath("$.asset.state").value(Asset.AssetState.PROCESSING.name()))
				.andReturn();
		String content = result.getResponse().getContentAsString();
	}

	//@Test
	public void logoutEverywhereTest() throws Exception {
		User loggedInUser = createUser();
		String json = "{\"username\":\"johnd\",\"password\":\"password\"}";
		//1. Log in
		mockMvc.perform(post("/api/login")
				.contentType("application/json;charset=UTF-8")
				.content(json))
				.andExpect(status().is(200));
	}

	private User fillTestUser() {
	    	User testUser = new User();
	    	testUser.setFirstName("John");
	    	testUser.setLastName("Doe");
	    	testUser.setEmail("john.doe@mailinator.com");
	    	testUser.setUsername("johnd");
	    	testUser.setPassword("password");
	    	testUser.setAccountStatus(User.AccountStatus.ACTIVE);
	    	testUser.setCreatedOn(new DateTime());
	    	testUser.setGuid(GuidGenerator.generate());
	    	return testUser;
	    }

		private User fillTestUser2() {
	    	User testUser2 = new User();
	    	testUser2.setFirstName("Jane");
	    	testUser2.setLastName("Doe");
	    	testUser2.setEmail("janedoe@mailinator.com");
	    	testUser2.setUsername("janed");
	    	testUser2.setPassword("password2");
	    	testUser2.setCreatedOn(new DateTime());
	    	testUser2.setAccountStatus(User.AccountStatus.ACTIVE);
	    	testUser2.setGuid(GuidGenerator.generate());
	    	return testUser2;
	    }

	    public User createUser() {
			User u = transactionUtils.executeInTransactionWithResult(() -> {
				User testUser = fillTestUser();
		    	userDao.persist(testUser);
		    	user1 = testUser;
		    	return testUser;
			});

			return u;

	    }

		public void deleteUser(User user) {
			transactionUtils.executeInTransaction(() -> {
				User inner = userService.findUserById(user.getId());
				inner.getFollowers().clear();
				userDao.flush();
				inner.getFollowing().clear();
				userDao.remove(inner);
			});
		}

	    public User createUser2() {

			User u = transactionUtils.executeInTransactionWithResult(() -> {
				User user = fillTestUser2();
		    	userDao.persist(user);
		    	user2 = user;
		    	return user;
			});

			return u;

	    }

	    @SuppressWarnings("boxing")
		protected synchronized Wiser startWiser() throws Exception {
			if (wiser == null) {
				currentSmtpPort = getSmtpPort();
				wiser = new Wiser();
				wiser.setPort(currentSmtpPort);
				wiser.start();
			}
			// Clear any stored messages.
			wiser.getMessages().clear();
			return wiser;
		}


	    private static int getSmtpPort() {
	    	Random r = new Random();
	    	int port = new Double(r.nextDouble()*(3121 - 2121) + 3000).intValue();
	    	return port;
	    }

	    private void replaceEmailSenderService() throws Exception {
	    	UserService unwrapped = (UserService) unwrapSpringProxy(userService);
	    	EmailService emailService = (EmailService) ReflectionTestUtils.getField(unwrapped, "emailService");
	    	EmailSenderService testEmailSender = new TestEmailSenderService(currentSmtpPort);
	    	ReflectionTestUtils.setField(emailService, "emailSenderService", testEmailSender);
	    	ReflectionTestUtils.setField(userService, "emailService", emailService);
	    }

	    private void replaceDatastoreService() throws Exception {
	    	DatastoreService testDatastoreService = new TestDatastoreService();
	    	UserService unwrapped = (UserService) unwrapSpringProxy(userService);
	    	ImageUtilsService imageUtilsUnwrapped = (ImageUtilsService) unwrapSpringProxy(imageUtilsService);
	    	ReflectionTestUtils.setField(unwrapped, "datastoreService", testDatastoreService);
	    	ReflectionTestUtils.setField(imageUtilsUnwrapped, "datastoreService", testDatastoreService);
	    }

	    /**
		 * Unwrap the given spring bean, if it's proxied.
		 * @throws Exception
		 */
		protected <T> Object unwrapSpringProxy(T mayBeProxied) throws Exception {
			Object unwrapped = mayBeProxied;
			if (AopUtils.isAopProxy(mayBeProxied)
					&& mayBeProxied instanceof Advised) {
				unwrapped = ((Advised) mayBeProxied).getTargetSource().getTarget();
			}
			return unwrapped;
		}

		private List<WiserMessage> pollForEmail(Wiser wiser) {
			long timeout = 30 * 1000; // Wait 30 seconds for email;
			long startTime = System.currentTimeMillis();
			while(wiser.getMessages().size() == 0 && (System.currentTimeMillis() - startTime) < timeout) {
				System.out.println("Polling for email...");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}

			return wiser.getMessages();
		}


	public static class TestEmailSenderService implements EmailSenderService {

		private Integer smtpPort;

		public TestEmailSenderService(Integer smtpPort) {
			this.smtpPort = smtpPort;
		}

		private void sendUsingJavaMail(Email email) {
			System.out.println("Sending email -- SMTP port is " + smtpPort);
			// Get the session object
			Properties properties = System.getProperties();
			properties.setProperty("mail.smtp.host", "localhost");
			properties.put("mail.smtp.port", smtpPort);
			Session session = Session.getDefaultInstance(properties);

			// Compose the message
			try {
				MimeMessage message = new MimeMessage(session);
				message.setFrom(new InternetAddress(email.getFromAddress()));
				message.addRecipient(Message.RecipientType.TO,
						new InternetAddress(email.getRecipients()[0]));
				message.setSubject(email.getSubject());
				message.setContent(email.getHtmlBody(),
						"text/html; charset=utf-8");
				// Send message
				Transport.send(message);
				System.out.println("message sent successfully....");

			} catch (MessagingException mex) {
				mex.printStackTrace();
			}
		}

		@Override
		public void sendEmail(Email email) {
			sendUsingJavaMail(email);
		}
	}


	public static class TestDatastoreService implements DatastoreService {

		private ConcurrentHashMap<String, Object> mockStore = new ConcurrentHashMap<String, Object>();

		@Override
		public void saveAsset(InputStream inputStream, long contentLength, String assetGuid) {
			mockStore.put(assetGuid, inputStream);

		}

		@Override
		public void updateAsset(InputStream inputStream, long contentLength, String assetGuid) {
			mockStore.put(assetGuid, inputStream);
		}

		@Override
		public void deleteAsset(String assetGuid) {
			mockStore.remove(assetGuid);
		}

		@Override
		public String generateDownloadLink(String assetGuid) {
			return "http://" +  "graffitab-eu1" + ".s3.amazonaws.com/" + generateKey(assetGuid);
		}

		private static String generateKey(String assetGuid) {
			return AmazonS3DatastoreService.ASSETS_ROOT_KEY + "/" + assetGuid;
		}

		@Override
		public String generateThumbnailLink(String assetGuid) {
			return "http://bucket/" + assetGuid + "_thumb";
		}
	}

	private ResultActions assertFollowEndpointResponse(ResultActions ra) throws Exception {

		return ra.andExpect(jsonPath("$.user.username").isNotEmpty())
				.andExpect(jsonPath("$.user.firstName").isNotEmpty())
				.andExpect(jsonPath("$.user.lastName").isNotEmpty())
				.andExpect(jsonPath("$.user.email").isNotEmpty())
				.andExpect(jsonPath("$.user.followedByCurrentUser").value(true))
				.andExpect(jsonPath("$.user.email").isNotEmpty())
				.andExpect(jsonPath("$.user.createdOn").isNotEmpty())
				.andExpect(jsonPath("$.user.followersCount").isNotEmpty())
				.andExpect(jsonPath("$.user.followingCount").isNotEmpty())
				.andExpect(jsonPath("$.user.streamablesCount").isNotEmpty());
	}

	private ResultActions assertGetFollowersResponse(ResultActions resultActions, User currentUser, Integer totalCount, Integer offset) throws Exception {
		return resultActions.andExpect(status().is(200))
				.andExpect(jsonPath("$.items[0]").exists())
				.andExpect(jsonPath("$.items[1]").doesNotExist())
				.andExpect(jsonPath("$.items[0].id").value(currentUser.getId().intValue()))
				.andExpect(jsonPath("$.resultsCount").value(totalCount))
				.andExpect(jsonPath("$.offset").value(offset));
	}

	private void assertFollowActivityIsCorrect(User currentUser, User followedUser) {
		//TODO: create helper method in activity to check correctness
	}

}