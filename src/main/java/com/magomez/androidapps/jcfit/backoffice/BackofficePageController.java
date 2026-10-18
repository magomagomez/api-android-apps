package com.magomez.androidapps.jcfit.backoffice;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The JCFit backoffice is a static page in {@code static/jcfit-admin/}; Spring only serves it
 * by its full name, so the short addresses redirect there.
 */
@Controller
public class BackofficePageController {

    private static final String PAGE = "redirect:/jcfit-admin/index.html";

    @GetMapping({"/jcfit-admin", "/jcfit-admin/"})
    public String page() {
        return PAGE;
    }
}
