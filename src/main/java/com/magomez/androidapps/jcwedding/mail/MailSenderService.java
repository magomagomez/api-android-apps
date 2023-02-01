package com.magomez.androidapps.jcwedding.mail;

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
public class MailSenderService {

    private static final String PWD = "lduvcmpguxukedid";
    private static final String JC_WEDDING = "JC Wedding";
    private static final String TEXT_HTML_CHARSET_UTF_8 = "text/html; charset=utf-8";
    private static final String SMTP = "smtp";

    final Logger logger = Logger.getLogger("com.magomez.escapersthings.mail.MailService");


    private void sendMail(MailSenderDTO mailSenderDTO){

        String from = mailSenderDTO.getFrom();
        String pass = mailSenderDTO.getPassword();
        // Recipient's email ID needs to be mentioned.
        String to = mailSenderDTO.getTo();
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
            message.setFrom(new InternetAddress(from, JC_WEDDING));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(to));

            // Set Subject: header field
            message.setSubject(mailSenderDTO.getSub());

            // Now set the actual message
            message.setContent(mailSenderDTO.getMsg(), TEXT_HTML_CHARSET_UTF_8);

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

    public void sendMail(MailDTO mailDTO){
        MailSenderDTO mailSenderDTO = new MailSenderDTO();
        mailSenderDTO.setFrom(UsersEnum.DEFAULT.getMail());
        mailSenderDTO.setPassword(PWD);
        mailSenderDTO.setTo(UsersEnum.BARTOMEU.getMail());
        if (mailDTO.getType() == 1) {
            mailSenderDTO.setSub("Cançó Boda");
            mailSenderDTO.setMsg(" El " + mailDTO.getName() + " Vol la cançó " + mailDTO.getSong());
        }
        if (mailDTO.getType() == 2) {
            mailSenderDTO.setSub("Apadrina un Friki");
            mailSenderDTO.setMsg(" El " + mailDTO.getEmail() + " Vol apadrinar");
        }
        sendMail(mailSenderDTO);
    }

}
