package com.graffitab.server.service;

import com.graffitab.server.service.email.EmailService;

import org.springframework.stereotype.Service;

import java.util.Locale;

import javax.annotation.Resource;

@Service
public class FeedbackService {

	@Resource
	private EmailService emailService;

	public void sendFeedback(String name,
							 String email,
							 String text,
							 String subject,
							 Locale locale) {
		emailService.sendFeedbackEmail(name, email, text, subject, locale);
	}
}
