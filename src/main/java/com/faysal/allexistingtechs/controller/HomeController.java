package com.faysal.allexistingtechs.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
	private static final Logger LOGGER = LoggerFactory.getLogger("apiLogger");

	@GetMapping("/home")
	public String getHome() {
		LOGGER.info("Accessing /home endpoint");
		return "Welcome Home";
	}
}
