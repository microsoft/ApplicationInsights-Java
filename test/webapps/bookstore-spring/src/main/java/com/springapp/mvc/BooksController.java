package com.springapp.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;

@Controller
@RequestMapping("/books")
public final class BooksController {
    @RequestMapping(method = RequestMethod.GET, params = {"id"})
    public ModelAndView showBooksByCategory(String id) {
        BooksDB.INSTANCE.initialize();

        ModelAndView model = new ModelAndView("books");

        ArrayList<BookData> bookList = BooksDB.INSTANCE.getBooks(id);

        model.addObject("books", bookList);
        model.addObject("subject", id);

        return model;
    }

    @RequestMapping(method = RequestMethod.GET, params = {"id", "runId", "requestId"})
    public ModelAndView showBooksByCategory(String id, String runId, String requestId) {
        return showBooksByCategory(id);
    }
}
