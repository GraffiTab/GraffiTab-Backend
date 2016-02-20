package com.graffitab.server.service.email;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import com.amazonaws.util.IOUtils;

@Getter @Setter @Log4j2
public class Email {

	private EmailType emailType;
	private String subject;
	private String htmlBody;
	private String textBody;
	private String sender;
	private String[] recipients;
	private String fromAddress;
	private String fromName;

	private static String WELCOME_TEMPLATE_CONTENTS;
	private static String PASSWORD_RESET_TEMPLATE_CONTENTS;
	private static String FEEDBACK_TEMPLATE_CONTENTS;

	private static String FROM_NAME = "GraffiTab";
	private static String FROM_ADDRESS = "no_reply@graffitab.com";

	static {
		try {
			WELCOME_TEMPLATE_CONTENTS = readTemplate(EmailType.ACTIVATION.getTemplateName());
			PASSWORD_RESET_TEMPLATE_CONTENTS = readTemplate(EmailType.RESET_PASSWORD.getTemplateName());
			FEEDBACK_TEMPLATE_CONTENTS = readTemplate(EmailType.FEEDBACK.getTemplateName());
		} catch (IOException e) {
			log.error("Error reading email templates", e);
		}
	}

	public static Email welcome(String[] recipients, Map<String, String> placeHolders) {
		Email email = new Email();
		EmailType emailType = EmailType.ACTIVATION;
		email.setSubject("Welcome to GraffiTab");
		email.setFromAddress(FROM_ADDRESS);
		email.setFromName(FROM_NAME);
		email.setEmailType(emailType);
		String emailBody = replacePlaceholders(placeHolders, WELCOME_TEMPLATE_CONTENTS);
		email.setHtmlBody(emailBody);
		email.setRecipients(recipients);
		return email;
	}

	public static Email resetPassword(String[] recipients, Map<String, String> placeHolders) {
		Email email = new Email();
		EmailType emailType = EmailType.RESET_PASSWORD;
		email.setSubject("Reset your password in GraffiTab");
		email.setFromAddress(FROM_ADDRESS);
		email.setFromName(FROM_NAME);
		email.setEmailType(emailType);
		String emailBody = replacePlaceholders(placeHolders, PASSWORD_RESET_TEMPLATE_CONTENTS);
		email.setHtmlBody(emailBody);
		email.setRecipients(recipients);
		return email;
	}

	private static String replacePlaceholders(Map<String,String> placeholders, String baseText) {
		for (String key : placeholders.keySet()) {
			String value = placeholders.get(key);
			baseText = baseText.replaceAll(key, value);
		}
		return baseText;
	}

	private static String readTemplate(String templateName) throws IOException {
		InputStream is = Email.class.getClassLoader().getResourceAsStream("emailTemplates/" + templateName);
		String templateString = IOUtils.toString(is);
		return templateString;
	}


}
