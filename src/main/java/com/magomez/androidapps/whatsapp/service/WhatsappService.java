package com.magomez.androidapps.whatsapp.service;


import com.magomez.androidapps.whatsapp.dto.WhatsappMessageDTO;
import com.twilio.Twilio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.twilio.rest.api.v2010.account.Message;

@Service
public class WhatsappService {

    private static final String ACCOUNT_SID = "AC9b482f7491c6fec1fe89120965fb4325";
    private static final String AUTH_TOKEN = "7f291c764095b4a6bcc1d3dd70784f57";

    private final WebMailService mailService;

    @Autowired
    public WhatsappService(WebMailService mailService){
        this.mailService = mailService;
    }

    public void updateAttendants(WhatsappMessageDTO whatsappMessageDTO){
        String text = "Tienes un nuevo contacto web. \n\n" ;
        text = text + whatsappMessageDTO.getFrom() + ", con email " + whatsappMessageDTO.getEmail();
        text = text + " te envia el siguiente mensaje: \n\n";
        text = text + whatsappMessageDTO.getText();
        sendMessage(text);
    }

    private void sendMessage(String text) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
       Message.creator(
               new com.twilio.type.PhoneNumber("whatsapp:+34647152962"),
               new com.twilio.type.PhoneNumber("whatsapp:+14155238886"),
               text)
       .create();

       mailService.sendmail(text);
    }

}
