package com.magomez.androidapps.friki.config;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConfig.BASE_URL)
@CrossOrigin(origins = "*", methods= {RequestMethod.GET})
public class FrikiStatusController {

    @GetMapping("/status")
    public String getStatus() {
        return "ALIVE";
    }

}
