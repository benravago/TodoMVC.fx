package fx.json;

import java.io.Reader;
import java.io.InputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.UncheckedIOException;

public class ParseInput {

  private final char[] cbuf;

  public ParseInput(char[] cbuf) {
    this.cbuf = cbuf;
  }

  public char[] getChars() {
    return cbuf;
  }

  @Override
  public String toString() {
    return new String(cbuf);
  }

  public ParseInput(InputStream in) {
    this(readAllBytes(in));
  }

  private static char[] readAllBytes(InputStream in) {
    try {
      var b = in.readAllBytes();
      return new String(b).toCharArray();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public ParseInput(Reader in) {
    this(readAllChars(in));
  }

  private static char[] readAllChars(Reader in) {
    try (var out = new CharArrayWriter()) {
      in.transferTo(out);
      return out.toCharArray();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

}
