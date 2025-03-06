package dev.by1337.bc.addon;

import org.by1337.blib.text.MessageFormatter;

public class InvalidAddonException extends Exception {
    public InvalidAddonException() {
    }

    public InvalidAddonException(String message) {
        super(message);
    }

    public InvalidAddonException(String message, Object... objects) {
        super(MessageFormatter.apply(message, objects));
    }

    public InvalidAddonException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidAddonException(String message, Throwable cause, Object... objects) {
        super(MessageFormatter.apply(message, objects), cause);
    }


    public InvalidAddonException(Throwable cause) {
        super(cause);
    }

}
