package com.graffitab.server.api.controller;

import com.graffitab.server.api.dto.StatusDto;
import com.graffitab.server.service.user.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;

@Log4j2
@Controller
public class DefaultController {

	@Autowired
	private UserService userService;

	@RequestMapping(value = "/**")
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public @ResponseBody StatusDto getDefault(HttpServletRequest request) {
		StatusDto statusDto = new StatusDto();
		statusDto.setStatus("NOT FOUND");
		log.warn("Uri not found " + request.getRequestURI());
		return statusDto;
	}

	@RequestMapping(value = {"/","/home"})
	@ResponseStatus(HttpStatus.OK)
	public String getBasePage(HttpServletRequest request, Model model) {
		log.warn("Testing the furrula activado arehrhhrhora!");
		return "index";
	}
}
