package com.springapp.mvc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Controller
@RequestMapping("/")
public class HelloController {
	private static final Logger logger = LogManager.getLogger(HelloController.class);

	@Value("${user.name}")
	private String userName;

	@RequestMapping(method = RequestMethod.GET, params = {})
	public ModelAndView printWelcome() {
		logger.debug("Entering the server....");

		ModelAndView model = new ModelAndView("hello");
		model.addObject("name", userName);

		return model;
	}

	@RequestMapping(method = RequestMethod.GET, params = {"runId", "requestId"})
	public ModelAndView printWelcome(String runId, String requestId) {
		return printWelcome();
	}
}