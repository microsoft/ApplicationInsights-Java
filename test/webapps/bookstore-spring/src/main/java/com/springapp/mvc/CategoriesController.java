package com.springapp.mvc;

import java.util.ArrayList;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/categories")
public class CategoriesController {
  @RequestMapping(
    method = RequestMethod.GET,
    params = {}
  )
  public ModelAndView listCategories() {
    ApplicationInsights.INSTANCE.getTelemetryClient().trackEvent("User entered");

    ModelAndView model = new ModelAndView("categories");
    ArrayList<String> categories = new ArrayList<String>();
    categories.add("Thriller");
    categories.add("Romance");
    categories.add("Science Fiction");
    categories.add("Drama");
    categories.add("Comedy");
    model.addObject("categories", categories);

    return model;
  }

  @RequestMapping(
    method = RequestMethod.GET,
    params = {"runId", "requestId"}
  )
  public ModelAndView listCategories(String runId, String requestId) {
    return listCategories();
  }
}
