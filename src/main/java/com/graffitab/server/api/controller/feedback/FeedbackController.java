package com.graffitab.server.api.controller.feedback;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graffitab.server.api.dto.ActionCompletedResult;
import com.graffitab.server.api.dto.feedback.FeedbackDto;
import com.graffitab.server.service.FeedbackService;

import java.util.Locale;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

	@Resource
	private FeedbackService feedbackService;

	@RequestMapping(value = "", method = RequestMethod.POST)
	public ActionCompletedResult submitFeedback(@JsonProperty("feedback") FeedbackDto feedbackDto, Locale locale) {
		feedbackService.sendFeedback(feedbackDto.getName(), feedbackDto.getEmail(), feedbackDto.getText(), locale);
		return new ActionCompletedResult();
	}
}
