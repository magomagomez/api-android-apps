package com.magomez.androidapps.magicEmail;

import org.springframework.stereotype.Service;

import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.Properties;

@Service
public class EmailService {

        // --- Configuration Constants ---
        // IMAP (Incoming Mail) Settings
        private static final String IMAP_HOST = "imap.gmail.com"; // Example: imap.gmail.com, outlook.office365.com
        private static final String IMAP_PORT = "993"; // Standard port for IMAPS (SSL/TLS)
        private static final String IMAP_USERNAME = "unasenlamangashow@gmail.com"; // Your full email address
        private static final String IMAP_PASSWORD = "netgdbvvaekgrfge"; // Your IMAP app-specific password

        // SMTP (Outgoing Mail) Settings
        private static final String SMTP_HOST = "smtp.gmail.com"; // Example: smtp.gmail.com, smtp.office365.com
        private static final String SMTP_PORT = "587"; // Standard port for SMTP with STARTTLS (587) or SSL/TLS (465)
        private static final String SMTP_USERNAME = "unasenlamangashow@gmail.com"; // Your full email address
        private static final String SMTP_PASSWORD = "ekqdisgrmggjhxir"; // Your SMTP app-specific password
        private static final String REPLY_FROM_EMAIL = "unasenlamangashow@gmail.com"; // The email address replies will appear to come from
        private static final String REPLY_SUBJECT_PREFIX = "Recuerdo de un As en La Manga";

        // HTML Body Content for the reply email
        private static final LocalDate currentDate = LocalDate.now();
        private static final String HTML_BODY_CONTENT =
                "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                        "h2 { color: #5a2d82; }" +
                        "img { max-width: 100%; height: auto; display: block; margin: 15px 0; border: 1px solid #ddd; }" +
                        "p { margin-bottom: 10px; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<h2>Gracias por Asistir a un As en la Manga</h2>" +
                        "<p>A continuación, un recuerdo del Show:</p>" +
                        "<p><img src=\"https://magomagomez.com/prediccion/prediccion_"+currentDate+".jpg\"alt=\"Magician's Predition\"></p>" +
                        "<p>Espero que hayas disfrutado del show!</p>" +
                        "<p>un mágico saludo,<br/>Magomez</p>" +
                        "<hr>" +
                        "</body>" +
                        "</html>";

        public void mailing() {
            System.out.println("MagoEmailBot starting...");

            Store store = null;
            Folder inbox = null;

            try {
                // --- IMAP Configuration ---
                Properties imapProps = new Properties();
                imapProps.put("mail.store.protocol", "imaps"); // Use "imaps" for SSL/TLS
                imapProps.put("mail.imaps.host", IMAP_HOST);
                imapProps.put("mail.imaps.port", IMAP_PORT);
                imapProps.put("mail.imaps.auth", "true");
                imapProps.put("mail.imaps.ssl.enable", "true"); // Explicitly enable SSL/TLS for IMAPS
                imapProps.put("mail.imaps.ssl.protocols", "TLSv1.2"); // Recommended for stronger security

                // Create a mail session for IMAP
                Session imapSession = Session.getInstance(imapProps, null);
                imapSession.setDebug(false); // Set to true for verbose IMAP debug output

                System.out.println("Connecting to IMAP store at " + IMAP_HOST + "...");
                store = imapSession.getStore("imaps");
                store.connect(IMAP_HOST, IMAP_USERNAME, IMAP_PASSWORD);
                System.out.println("Successfully connected to IMAP store.");

                // Get the INBOX folder and open it
                inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_WRITE); // Open in READ_WRITE mode to allow marking messages as read or deleted

                System.out.println("Fetching messages from INBOX...");
                Message[] messages = inbox.getMessages();
                System.out.println("Total messages found in INBOX: " + messages.length);

                if (messages.length == 0) {
                    System.out.println("No messages to process in the INBOX.");
                } else {
                    for (int i = 0; i < messages.length; i++) {
                        Message message = messages[i];
                        System.out.println("\n--- Processing Message " + (i + 1) + " ---");

                        // Extract sender
                        Address[] fromAddresses = message.getFrom();
                        String senderEmail = "unknown@example.com"; // Default sender
                        if (fromAddresses != null && fromAddresses.length > 0) {
                            if (fromAddresses[0] instanceof InternetAddress) {
                                senderEmail = ((InternetAddress) fromAddresses[0]).getAddress();
                            } else {
                                senderEmail = fromAddresses[0].toString(); // Fallback for other address types
                            }
                            System.out.println("Sender: " + fromAddresses[0].toString() + " (" + senderEmail + ")");
                        } else {
                            System.out.println("Sender: Could not determine sender.");
                        }

                        // Extract subject
                        String originalSubject = message.getSubject();
                        System.out.println("Original Subject: " + (originalSubject != null ? originalSubject : "[No Subject]"));

                        // Prevent mail loops by not replying to self
                        if (senderEmail.equalsIgnoreCase(REPLY_FROM_EMAIL)) {
                            System.out.println("Skipping reply to self: " + senderEmail);
                            continue;
                        }

                        // Send the reply email
                        sendReplyEmail(message, senderEmail, originalSubject);

                        // Optionally, mark the message as seen/read after processing
                        // message.setFlag(Flags.Flag.SEEN, true);

                        // Optionally, move the message to another folder or delete it
                        // if (processedFolder == null) {
                        //     processedFolder = store.getFolder("Processed");
                        //     if (!processedFolder.exists()) {
                        //         processedFolder.create(Folder.HOLDS_MESSAGES);
                        //     }
                        // }
                        // inbox.copyMessages(new Message[]{message}, processedFolder);
                        // message.setFlag(Flags.Flag.DELETED, true); // Mark for deletion on close(true)
                    }
                }

            } catch (NoSuchProviderException e) {
                System.err.println("Error: No such mail provider. Check protocol name (e.g., 'imaps').");
                e.printStackTrace();
            } catch (AuthenticationFailedException e) {
                System.err.println("Error: IMAP authentication failed. Check IMAP_USERNAME and IMAP_PASSWORD.");
                e.printStackTrace();
            } catch (MessagingException e) {
                System.err.println("Error during IMAP operations (connecting, fetching messages, etc.).");
                e.printStackTrace();
            } finally {
                // Ensure resources are closed gracefully
                try {
                    if (inbox != null && inbox.isOpen()) {
                        inbox.close(false); // 'false' means don't expunge deleted messages
                        System.out.println("IMAP INBOX closed.");
                    }
                    if (store != null && store.isConnected()) {
                        store.close();
                        System.out.println("IMAP store closed.");
                    }
                } catch (MessagingException e) {
                    System.err.println("Error closing IMAP resources.");
                    e.printStackTrace();
                }
            }
            System.out.println("\nMagoEmailBot finished processing.");
        }

        /**
         * Sends an HTML-formatted reply email.
         *
         * @param originalMessage The original message being replied to (used for threading headers).
         * @param recipientEmail The email address of the original sender.
         * @param originalSubject The subject of the original message.
         */
        private static void sendReplyEmail(Message originalMessage, String recipientEmail, String originalSubject) {
            Transport transport = null;
            try {
                // --- SMTP Configuration ---
                Properties smtpProps = new Properties();
                smtpProps.put("mail.smtp.host", SMTP_HOST);
                smtpProps.put("mail.smtp.port", SMTP_PORT);
                smtpProps.put("mail.smtp.auth", "true");
                smtpProps.put("mail.smtp.starttls.enable", "true"); // Enable STARTTLS for port 587
                smtpProps.put("mail.smtp.ssl.protocols", "TLSv1.2"); // Recommended TLS version
                // smtpProps.put("mail.debug", "true"); // Uncomment for verbose SMTP debug output

                // Create a mail session for SMTP with an Authenticator
                Session smtpSession = Session.getInstance(smtpProps, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
                    }
                });
                smtpSession.setDebug(false); // Set to true for verbose SMTP debug output

                // Create the new reply message
                MimeMessage replyMessage = new MimeMessage(smtpSession);

                // Set the sender of the reply
                replyMessage.setFrom(new InternetAddress(REPLY_FROM_EMAIL, "Mago Magomez"));

                // Set the recipient of the reply (the original sender)
                replyMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));

                // Set the subject of the reply, prepending "Re:"
                String replySubject = REPLY_SUBJECT_PREFIX;
                replyMessage.setSubject(replySubject);

                // --- Create HTML Content ---
                // Use MimeMultipart to encapsulate the HTML body. "alternative" allows for plain text fallback.
                MimeMultipart multipart = new MimeMultipart("alternative");

                // Create the HTML body part
                MimeBodyPart htmlBodyPart = new MimeBodyPart();
                htmlBodyPart.setContent(HTML_BODY_CONTENT, "text/html; charset=utf-8");
                multipart.addBodyPart(htmlBodyPart);

                // Set the content of the message to the multipart
                replyMessage.setContent(multipart);

                // Set the sent date
                replyMessage.setSentDate(new java.util.Date());

                // --- Optional: Set threading headers for better email client display ---

                // --- Send the email ---
                System.out.println("Attempting to send reply to: " + recipientEmail + " with subject: " + replySubject);
                transport = smtpSession.getTransport("smtp");
                transport.connect(SMTP_HOST, SMTP_USERNAME, SMTP_PASSWORD); // Explicitly connect the transport
                transport.sendMessage(replyMessage, replyMessage.getAllRecipients());
                System.out.println("Reply email sent successfully to " + recipientEmail);

            } catch (AuthenticationFailedException e) {
                System.err.println("Error: SMTP authentication failed. Check SMTP_USERNAME and SMTP_PASSWORD.");
                e.printStackTrace();
            } catch (MessagingException e) {
                System.err.println("Error sending reply email to " + recipientEmail + ".");
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            } finally {
                // Ensure transport is closed
                if (transport != null && transport.isConnected()) {
                    try {
                        transport.close();
                    } catch (MessagingException e) {
                        System.err.println("Error closing SMTP transport.");
                        e.printStackTrace();
                    }
                }
            }
        }
}
