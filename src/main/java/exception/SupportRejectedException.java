package exception;

public class SupportRejectedException extends RuntimeException {
    public SupportRejectedException(String message) {
        super(message);
    }
}