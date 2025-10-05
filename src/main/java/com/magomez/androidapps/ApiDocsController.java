package com.magomez.androidapps;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@CrossOrigin(origins = "*", methods= {RequestMethod.GET})
public class ApiDocsController {

    @GetMapping("/api-docs/frikistanteria")
    public String getDocs() {
        return "redirect:/swagger-ui/index.html?urls.primaryName=frikistanteria";
    }

}
