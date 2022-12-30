package com.magomez.androidapps.escapersthings.mail;

import com.magomez.androidapps.escapersthings.escaperooms.dto.UsersEnum;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Logger;

@Service
public class MailService {

    private static final String PWD = "lduvcmpguxukedid";
    private static final String APP_ESCAPERS = "App Escapers";
    private static final String TEXT_HTML_CHARSET_UTF_8 = "text/html; charset=utf-8";
    private static final String SMTP = "smtp";

    final Logger logger = Logger.getLogger("com.magomez.escapersthings.mail.MailService");


    public void sendMail(MailDTO mailDTO){

        String from = mailDTO.getFrom();
        String pass = mailDTO.getPassword();
        // Recipient's email ID needs to be mentioned.
        String to = mailDTO.getTo();
        String host = "smtp.mail.yahoo.com";

        // Get system properties
        Properties properties = System.getProperties();
        // Setup mail server
        properties.put("mail.smtp.starttls.enable", "true");
        System.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.user", from);
        properties.put("mail.smtp.password", pass);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");

        // Get the default Session object.
        Session session = Session.getDefaultInstance(properties);

        try{
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from, APP_ESCAPERS));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(to));

            // Set Subject: header field
            message.setSubject(mailDTO.getSub());

            // Now set the actual message
            message.setContent(mailDTO.getMsg(), TEXT_HTML_CHARSET_UTF_8);

            // Send message
            Transport transport = session.getTransport(SMTP);
            transport.connect(host, from, pass);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            logger.info("Sent message successfully....");
        }catch (MessagingException | UnsupportedEncodingException mex) {
            mex.printStackTrace();
        }

    }

    public void sendMail(String email, String name, Integer id, String image, String userName){
        MailDTO mailDTO = new MailDTO();
        mailDTO.setFrom(UsersEnum.DEFAULT.getMail());
        mailDTO.setPassword(PWD);
        mailDTO.setTo(email);
        mailDTO.setSub("Vota " + name);



        String bodyMail = "<html><head></head> <body> <div class='logo' style='text-align: center; background-color: black'>"+
		    "<img src='http://escapersthings.magomagomez.com/web/images/logo.png' style='width: 150px'>"+
	        "</div> <div class='texto' style='background-color: black; color:#3333cc; padding-top:10px; padding-bottom:10px; text-align: center;font-size: 20px;'>"+
		    "<span><span style='color:orange'>¡¡¡¡¡ </span>"+name+" Finalizado<span style='color:orange'> !!!!!</span></span>"+
	        "</div><div class='foto' style='background-color:black; text-align: center'> <img style='width:250px' src='http://escapersthings.magomagomez.com/web/images/escapes/"+image+"'></div>"+
            "<div class='boton' style='background-color:black; text-align: center'> <a href='http://escapersthings.magomagomez.com/web/mail.html?escapeName="+name+"&escapeImage="+image+"&name="+userName+"&id="+id+"'>"+
   			"<input style='color: black;background-color: green;border-color: black;width: 65%;margin-top: 15px;margin-bottom: 15px;border-radius: 25px; font-size: 40px; height: 7%' type='submit' value='Puntuar'>"+
   		    "</a></div></body></html>";

        mailDTO.setMsg(bodyMail);
        sendMail(mailDTO);
    }

}
