package com.graffitab.server.service.user;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import lombok.extern.log4j.Log4j2;

import org.hibernate.criterion.Restrictions;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.User;
import com.graffitab.server.persistence.model.UserSession;
import com.graffitab.server.service.ProxyUtilities;
import com.graffitab.server.service.TransactionUtils;

@Service
@Log4j2
public class UserSessionService {

	@Resource
	private HibernateDaoImpl<UserSession, Long> userSessionDao;

	@Resource
	private UserService userService;

	@Resource
	private TransactionUtils transactionUtils;

	private ExecutorService sessionOperationsExecutor = Executors.newFixedThreadPool(2);

	@Transactional
	public UserSession findBySessionIdAndInitialize(String sessionId) {

		UserSession userSession = (UserSession) userSessionDao.getBaseCriteria()
				                  .add(Restrictions.eq("sessionId", sessionId)).uniqueResult();

		if (userSession != null) {
			ProxyUtilities.unwrapProxy(userSession.getUser());
		}
		return userSession;
	}

	@Transactional
	public UserSession findBySessionId(String sessionId) {
		UserSession userSession = (UserSession) userSessionDao.getBaseCriteria()
                .add(Restrictions.eq("sessionId", sessionId)).uniqueResult();
		return userSession;
	}

	@Transactional
	public boolean exists(String sessionId) {
		return findBySessionId(sessionId) != null;
	}

	public void saveOrUpdateSessionData(HttpSession session) {

		Map<String, Object> sessionAttributeMap = getSessionAttributeMap(session);
		byte[] sessionData = SerializationUtils.serialize(sessionAttributeMap);

		transactionUtils.executeInTransaction(() -> {
			UserSession currentUserSession = findBySessionIdAndInitialize(session.getId());
			if (currentUserSession == null) {
				User currentUser = userService.getCurrentUser();
				UserSession userSession = new UserSession();
				userSession.setSessionId(session.getId());
				userSession.setContent(sessionData);
				userSession.setUser(currentUser);
				userSessionDao.persist(userSession);
			} else {
				currentUserSession.setContent(sessionData);
			}
		});
	}

	public void saveSessionDataInBackground(HttpSession session) {
	  sessionOperationsExecutor.submit(() -> {
		  saveOrUpdateSessionData(session);
	  });
	}

	@Transactional
	public void deleteSession(String sessionId) {
		Integer deletedSessionsCount = userSessionDao.createQuery("delete from UserSession us " +
												 "where sessionId = :sessionId")
												 .setParameter("sessionId", sessionId)
												 .executeUpdate();
		if (log.isDebugEnabled()) {
			log.debug("Deleted " + deletedSessionsCount + " sessions");
		}
	}

	public void pingSession(String sessionId) {
	 sessionOperationsExecutor.submit(() -> {
		  // TODO: update pingTime for sessionId
	  });
	}

	private Map<String, Object> getSessionAttributeMap(HttpSession session) {
		Map<String, Object> sessionAttributeMap = new HashMap<>();
		if (session != null) {
			Enumeration<String> sessionAttributeNames = session.getAttributeNames();
			while (sessionAttributeNames.hasMoreElements()) {
				String attributeName = sessionAttributeNames.nextElement();
				// Don't copy the spring security context, we'll rebuild it using Session Registry
				if (!attributeName.equals(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)) {
					Object attributeValue = session.getAttribute(attributeName);
					sessionAttributeMap.put(attributeName, attributeValue);
				}
			}
		}
		return sessionAttributeMap;
	}

	//TODO: cleaner thread to expire sessions in the DB
}
