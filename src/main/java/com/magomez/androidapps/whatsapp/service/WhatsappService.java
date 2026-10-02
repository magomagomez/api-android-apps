package com.magomez.androidapps.whatsapp.service;


import com.magomez.androidapps.whatsapp.dto.WhatsappMessageDTO;
import com.twilio.Twilio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.twilio.rest.api.v2010.account.Message;

@Service
public class WhatsappService {

    private final WebMailService mailService;
    private final String accountSid;
    private final String authToken;

    @Autowired
    public WhatsappService(WebMailService mailService,
                           @Value("${twilio.account-sid}") String accountSid,
                           @Value("${twilio.auth-token}") String authToken){
        this.mailService = mailService;
        this.accountSid = accountSid;
        this.authToken = authToken;
    }

    public void updateAttendants(WhatsappMessageDTO whatsappMessageDTO){
        String text = "Tienes un nuevo contacto web. \n\n" ;
        text = text + whatsappMessageDTO.getFrom() + ", con email " + whatsappMessageDTO.getEmail();
        text = text + " te envia el siguiente mensaje: \n\n";
        text = text + whatsappMessageDTO.getText();
        sendMessage(text);
    }

    private void sendMessage(String text) {
        Twilio.init(accountSid, authToken);
       Message.creator(
               new com.twilio.type.PhoneNumber("whatsapp:+34647152962"),
               new com.twilio.type.PhoneNumber("whatsapp:+14155238886"),
               text)
       .create();

       mailService.sendmail(text);
    }

}
