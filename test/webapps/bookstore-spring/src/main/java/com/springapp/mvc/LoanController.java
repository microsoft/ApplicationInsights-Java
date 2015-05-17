package com.springapp.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/loan")
public class LoanController {
    @RequestMapping(method = RequestMethod.GET, params = {})
    public ModelAndView loanHandler(String title, String id, String subject) {
        BooksDB.INSTANCE.initialize();

        try {
            BooksDB.INSTANCE.loanBook(id);
            ModelAndView model = new ModelAndView("success");

            model.addObject("title", title);
            model.addObject("book", id);
            model.addObject("subject", subject);
            return model;
        } catch (Exception e) {
            ModelAndView model = new ModelAndView("failed");
            model.addObject("title", title);
            model.addObject("book", id);
            model.addObject("subject", subject);
            return model;
        }
    }

    @RequestMapping(method = RequestMethod.GET, params = {"title", "id", "subject", "runId", "requestId"})
    public ModelAndView loanHandler(String title, String id, String subject, String runId, String requestId) {
        return loanHandler(title, id, subject);
    }
}