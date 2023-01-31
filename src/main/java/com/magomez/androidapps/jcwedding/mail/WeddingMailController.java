package com.magomez.androidapps.jcwedding.mail;

import com.magomez.androidapps.jcwedding.config.ApiConfig;
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
public class WeddingMailController {

    private final MailSenderService mailService;

    @Autowired
    public WeddingMailController(final MailSenderService mailService){
        this.mailService = mailService;
    }

    @PostMapping
    public void sendEmail(@RequestBody MailDTO mailDTO){
        mailService.sendMail(mailDTO);
    }

}
