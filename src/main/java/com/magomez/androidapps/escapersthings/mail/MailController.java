package com.magomez.androidapps.escapersthings.mail;

import com.magomez.androidapps.escapersthings.config.ApiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(ApiConfig.BASE_URL + "/mail")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class MailController {

    private final MailService mailService;

    @Autowired
    public MailController(final MailService mailService){
        this.mailService = mailService;
    }

    @PostMapping
    public void sendEmail(@RequestBody MailDTO mailDTO){
        mailService.sendMail(mailDTO);
    }

}
