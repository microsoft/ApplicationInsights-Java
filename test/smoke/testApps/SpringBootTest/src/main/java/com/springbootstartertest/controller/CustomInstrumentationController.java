package com.springbootstartertest.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomInstrumentationController {

	@GetMapping("/customInstrumentationOne")
	public String customInstrumentationOne() {
		return new TargetObject().one();
	}

	@GetMapping("/customInstrumentationTwo")
	public String customInstrumentationTwo() {
		return new TargetObject().two("Two");
	}

	@GetMapping("/customInstrumentationThree")
	public String customInstrumentationThree() {
		try {
			return new TargetObject().three();
		} catch (Exception e) {
			return "Three";
		}
	}

	@GetMapping("/customInstrumentationFour")
	public String customInstrumentationFour() {
		new TargetObject.NestedObject().four(false, null, null);
		return "Four";
	}

	@GetMapping("/customInstrumentationFive")
	public String customInstrumentationFive() {
		return new TargetObject().five();
	}

	@GetMapping("/customInstrumentationSeven")
	public String customInstrumentationSeven() {
		return new TargetObject().seven("Seven");
	}

	@GetMapping("/customInstrumentationEight")
	public String customInstrumentationEight() {
		return new TargetObject().eight("Eight");
	}

	@GetMapping("/customInstrumentationNine")
	public String customInstrumentationNine() throws IOException {
		return new TargetObject().nine();
	}
}
