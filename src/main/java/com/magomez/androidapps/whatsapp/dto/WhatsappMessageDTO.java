package com.magomez.androidapps.whatsapp.dto;

import java.io.Serial;
import java.io.Serializable;

public class WhatsappMessageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String from;
    private String email;
    private String text;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
