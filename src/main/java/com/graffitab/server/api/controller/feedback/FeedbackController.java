package com.graffitab.server.api.controller.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graffitab.server.api.dto.ActionCompletedResult;
import com.graffitab.server.api.dto.feedback.FeedbackDto;
import com.graffitab.server.service.FeedbackService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

	@Resource
	private FeedbackService feedbackService;

	@CrossOrigin(origins = {"https://graffitab.com", "https://dev.graffitab.com"})
	@RequestMapping(value = "", method = RequestMethod.POST)
	public ActionCompletedResult submitFeedback(@JsonProperty("feedback") FeedbackDto feedbackDto, Locale locale) {
		feedbackService.sendFeedback(feedbackDto.getName(),
                feedbackDto.getEmail(),
                feedbackDto.getText(),
                feedbackDto.getSubject(),
                locale);
		return new ActionCompletedResult();
	}
}
