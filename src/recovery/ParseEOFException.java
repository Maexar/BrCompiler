package recovery;

import parser.ParseException;

public class ParseEOFException extends ParseException {
    public ParseEOFException(String x) {
        super(x);
    }
}
