package com.magomez.androidapps.whatsapp.controller;


import com.magomez.androidapps.whatsapp.dto.WhatsappMessageDTO;
import com.magomez.androidapps.whatsapp.service.WhatsappService;
import com.magomez.androidapps.whatsapp.config.ApiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL+"/whatsapp")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class WhatsappController {

    private final WhatsappService whatsappService;

    @Autowired
    public WhatsappController(WhatsappService whatsappService){
        this.whatsappService = whatsappService;
    }

    @PostMapping
    public void sendWhatsapp(@RequestBody WhatsappMessageDTO whatsappMessageDTO){
        whatsappService.updateAttendants(whatsappMessageDTO);
    }

}
