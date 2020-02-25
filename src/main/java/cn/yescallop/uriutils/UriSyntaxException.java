package cn.yescallop.uriutils;

/**
 * Thrown if an input string could not be parsed as a
 * URI reference or component.
 *
 * @author Scallop Ye
 */
public class UriSyntaxException extends IllegalArgumentException {

    private final String input;
    private final String reason;
    private final int index;

    UriSyntaxException(String input, String reason, int index) {
        super();
        if (input == null || reason == null)
            throw new NullPointerException();
        if (index < 0)
            throw new IllegalArgumentException("index");
        this.input = input;
        this.reason = reason;
        this.index = index;
    }

    public String input() {
        return input;
    }

    public String reason() {
        return reason;
    }

    public int index() {
        return index;
    }

    @Override
    public String getMessage() {
        return reason + " at index " + index + ": " + input;
    }
}
