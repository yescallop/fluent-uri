package cn.yescallop.uriutils;

/**
 * Thrown if an input string could not be parsed as a
 * URI reference or component.
 *
 * @author Scallop Ye
 */
public class UriSyntaxException extends Exception {

    private final String input;
    private final String reason;
    private final int index;

    public UriSyntaxException(String input, String reason, int index) {
        super();
        if (input == null || reason == null)
            throw new NullPointerException();
        if (index < -1)
            throw new IllegalArgumentException("index");
        this.input = input;
        this.reason = reason;
        this.index = index;
    }

    public UriSyntaxException(String input, String reason) {
        this(input, reason, -1);
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
        StringBuilder sb = new StringBuilder();
        sb.append(reason);
        if (index >= 0) {
            sb.append(" at index ");
            sb.append(index);
        }
        sb.append(": ");
        sb.append(input);
        return sb.toString();
    }
}
