package com.graffitab.server.api.controller;

import com.graffitab.server.service.user.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Controller
public class DefaultController {

	@Autowired
	private UserService userService;

	// Comment this out for now as it blocks Swagger.
//	@RequestMapping(value = "/**")
//	@ResponseStatus(HttpStatus.NOT_FOUND)
//	public @ResponseBody
//	StatusDto getDefault(HttpServletRequest request) {
//		StatusDto statusDto = new StatusDto();
//		statusDto.setStatus("NOT FOUND");
//		log.warn("Uri not found " + request.getRequestURI());
//		return statusDto;
//	}

	@RequestMapping(value = {"/", "/index.html"})
	public String getBasePage(HttpServletRequest request, Model model) {
		return "redirect:/docs";
	}
}
