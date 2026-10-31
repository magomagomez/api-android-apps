package com.magomez.androidapps.regalos.linkpreview;

import java.io.Serial;

/** A link that points somewhere a preview may not go (a private address, an odd port). */
public class BlockedLinkException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BlockedLinkException() {
        super("A link preview may not reach that address");
    }
}
