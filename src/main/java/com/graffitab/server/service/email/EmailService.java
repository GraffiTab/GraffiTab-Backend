package com.graffitab.server.service.email;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import lombok.extern.log4j.Log4j;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@Log4j
public class EmailService {

	@Resource
	private EmailSenderService emailSenderService;

	@Resource
	private MessageSource messageSource;

	private ExecutorService emailExecutor = Executors.newFixedThreadPool(2);

	public void sendWelcomeEmail(String username, String email, String activationLink, Locale locale) {
		Map<String,String> data = new HashMap<>();
		data.put("@username", username);
		data.put("@activation_link", activationLink);
		String welcomeSubject = messageSource.getMessage("email.subject.welcome", null, locale);
		Email welcomeEmail = Email.welcome(new String[] {email}, data, welcomeSubject, locale.getLanguage());
		sendEmailAsync(welcomeEmail);
	}

	public void sendWelcomeExternalEmail(String username, String email, Locale locale) {
		Map<String,String> data = new HashMap<>();
		data.put("@username", username);

		String welcomeSubject = messageSource.getMessage("email.subject.welcome", null, locale);
		Email welcomeEmail = Email.welcomeExternal(new String[] {email}, data, welcomeSubject, locale.getLanguage());
		sendEmailAsync(welcomeEmail);
	}

	public void sendResetPasswordEmail(String email, String resetPasswordLink, Locale locale) {
		Map<String,String> data = new HashMap<>();
		data.put("@reset_link", resetPasswordLink);

		String resetPasswordSubject = messageSource.getMessage("email.subject.resetPassword", null, locale);
		Email resetPasswordEmail = Email.resetPassword(new String[] {email}, data, resetPasswordSubject, locale.getLanguage());
		sendEmailAsync(resetPasswordEmail);
	}

	public void sendFeedbackEmail(String name, String email, String text, Locale locale) {
		Map<String,String> data = new HashMap<>();
		data.put("@username", name);
		data.put("@email", email);
		data.put("@feedback", text);

		String feedbackSubject = messageSource.getMessage("email.subject.feedback", null, locale);

		Email feedbackEmail = Email.feedback(data, feedbackSubject, locale.getLanguage());
		sendEmailAsync(feedbackEmail);
	}

	public void sendFlagEmail(Long streamableId, String streamableLink, Locale locale) {
		Map<String,String> data = new HashMap<>();
		data.put("@streamable_id", streamableId + "");
		data.put("@streamable_link", streamableLink);

		String flagSubject = messageSource.getMessage("email.subject.flag", null, locale);

		Email feedbackEmail = Email.flag(data, flagSubject, locale.getLanguage());
		sendEmailAsync(feedbackEmail);
	}

	private void sendEmailAsync(Email email) {
		emailExecutor.submit(() -> {
			log.debug("About to send email " + email);
			try {
				emailSenderService.sendEmail(email);
			} catch (Throwable t) {
				log.error("Error sending email", t);
			}
		});
	}
}
