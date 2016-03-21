package com.graffitab.server.test.api;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.annotation.Resource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.Filter;
import javax.transaction.Transactional;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import com.graffitab.server.config.spring.MainConfig;
import com.graffitab.server.config.web.WebConfig;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.asset.Asset.AssetType;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.model.user.User.AccountStatus;
import com.graffitab.server.service.email.Email;
import com.graffitab.server.service.email.EmailSenderService;
import com.graffitab.server.service.email.EmailService;
import com.graffitab.server.service.store.DatastoreService;
import com.graffitab.server.service.user.UserService;
import com.graffitab.server.util.GuidGenerator;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes={MainConfig.class, TestDatabaseConfig.class, WebConfig.class})
@Rollback(value = true)
@Transactional
@ActiveProfiles("unit-test")
public class UserApiTest {

	    @Resource
	    private WebApplicationContext ctx;

	    @Resource
	    private UserService userService;

		@Resource
		private HibernateDaoImpl<User, Long> userDao;

	    @Autowired
	    private Filter springSecurityFilterChain;

	    private Wiser wiser;

	    private MockMvc mockMvc;

	    private static Integer currentSmtpPort;

	    @Before
	    public void setUp() throws Exception {
	        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
	        		                      .addFilters(springSecurityFilterChain)
	        		                      .build();

	         wiser = startWiser();
	         replaceEmailSenderService();
	         replaceDatastoreService();
	    }

	    @After
	    public void clear() {
	    	// Nothing to do
	    }

	    @Test
	    public void getUserByIdTest() throws Exception {
	    	User loggedInUser = createUser();
	    	User testUser = createUser2();

	        mockMvc.perform(get("/api/users/{id}",testUser.getId()).
	        		with(user(loggedInUser))
	        		.accept(MediaType.APPLICATION_JSON))
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
	                .andExpect(jsonPath("$.token").isNotEmpty());

	    	pollForEmail(wiser);

	    	List<WiserMessage> wiserMessages = wiser.getMessages();
	    	assertEquals(wiserMessages.size(), 1);
	    	WiserMessage message = wiserMessages.get(0);
	    	assertEquals("Welcome to GraffiTab", message.getMimeMessage().getSubject());
	    }

	    @Test
	    @Transactional
	    @Rollback(value = true)
	    public void followUserTest() throws Exception {
	    	User currentUser = createUser();
	    	User userToFollow = createUser2();

	    	mockMvc.perform(post("/api/users/" + userToFollow.getId() + "/followers")
	    			.with(user(currentUser)))
	                .andExpect(status().is(200))
	                .andExpect(content().contentType("application/json;charset=UTF-8"))
	                .andExpect(jsonPath("$.user.guid").isNotEmpty())
	                .andExpect(jsonPath("$.user.email").isNotEmpty())
	                .andExpect(jsonPath("$.user.followersCount").isNotEmpty())
	                .andExpect(jsonPath("$.user.followingCount").isNotEmpty())
	                .andExpect(jsonPath("$.user.streamablesCount").isNotEmpty());

	    	//TODO: complete test when possible to query following and followers
	    }

	    @Test
	    public void unFollowUserTest() throws Exception {
	    	User currentUser = createUser();
	    	User userToFollow = createUser2();

	    	// Follow first
	    	mockMvc.perform(post("/api/users/" + userToFollow.getId() + "/followers")
	    			.with(user(currentUser)))
	                .andExpect(status().is(200));

	    	// Unfollow afterwards
	    	mockMvc.perform(delete("/api/users/" + userToFollow.getId() + "/followers")
	    			.with(user(currentUser)))
	                .andExpect(status().is(200));

	    	//TODO: Complete test when possible to query following and followers
	    }

	    @Test
	    public void addAssetTest() throws IOException, Exception {
	    	User loggedInUser = createUser();
	    	InputStream in = this.getClass().getResourceAsStream("/api/test-asset.jpg");
	    	mockMvc.perform(post("/api/users/me/avatar")
	    			.with(user(loggedInUser))
	                .contentType("application/octet-stream")
	                .content(IOUtils.toByteArray(in)))
	                .andExpect(status().is(200))
	                .andExpect(content().contentType("application/json;charset=UTF-8"))
	                .andExpect(jsonPath("$.asset.guid").isNotEmpty())
	                .andExpect(jsonPath("$.asset.type").value(AssetType.IMAGE.name()))
	                .andExpect(jsonPath("$.asset.link").isNotEmpty());
	    }

	    private User fillTestUser() {
	    	User testUser = new User();
	    	testUser.setFirstName("John");
	    	testUser.setLastName("Doe");
	    	testUser.setEmail("john.doe@mailinator.com");
	    	testUser.setUsername("johnd");
	    	testUser.setPassword("password");
	    	testUser.setAccountStatus(AccountStatus.ACTIVE);
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
	    	testUser2.setAccountStatus(AccountStatus.ACTIVE);
	    	testUser2.setGuid(GuidGenerator.generate());
	    	return testUser2;
	    }

	    private User createUser() {
	    	User testUser = fillTestUser();
	    	userDao.persist(testUser);
	    	return testUser;
	    }

	    private User createUser2() {
	    	User user = fillTestUser2();
	    	userDao.persist(user);
	    	return user;
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
	    	ReflectionTestUtils.setField(unwrapped, "datastoreService", testDatastoreService);
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

		@Override
		public void saveAsset(InputStream inputStream, long contentLength, String assetGuid) {
			// TODO Auto-generated method stub

		}

		@Override
		public void updateAsset(InputStream inputStream, long contentLength, String assetGuid) {
			// TODO Auto-generated method stub

		}

		@Override
		public void deleteAsset(String assetGuid) {
			// TODO Auto-generated method stub

		}

		@Override
		public String generateDownloadLink(String assetGuid) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}