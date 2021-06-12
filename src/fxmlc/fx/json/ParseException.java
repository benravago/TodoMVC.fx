package fx.json;

public class ParseException extends RuntimeException {

  public ParseException(String message) {
    super(message);
  }

  public ParseException(String message, Throwable cause) {
    super(message, cause, true, false);
  }

  private static final long serialVersionUID = 1L;
}
