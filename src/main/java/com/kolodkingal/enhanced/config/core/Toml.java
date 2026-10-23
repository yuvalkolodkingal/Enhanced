package com.kolodkingal.enhanced.config.core;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The mod's own TOML reader and writer (port P0, lane L7): the part of TOML the config files use, read and written
 * exactly as NightConfig 3.8 (NeoForge's config library, which the mod no longer uses) read and wrote them, so the
 * files players already have load with the same values and are written back with the same text.
 *
 * <p>Read: tables ({@code [a]}, {@code [a.b]}, arrays of tables {@code [[a]]}), {@code key = value} with bare, quoted
 * and dotted keys, basic, literal and multi-line strings with their escapes, integers (decimal with {@code _},
 * {@code 0x}, {@code 0o}, {@code 0b}, signs), floats (with exponents, {@code inf}, {@code nan}), booleans, arrays,
 * inline tables, and {@code #} comments. A comment belongs to the entry or table below it (the lines just above it,
 * then the one at the end of its own line); comments after the last entry of the file are dropped. Dates and times
 * are kept as their text ({@link Temporal}). Anything else is a {@link ParseException}.
 *
 * <p>Written: each table as {@code [name]} with its comment above it, its entries indented by a tab, each preceded by
 * its comment lines ({@code #} and the text), a blank line between tables, lines ending with the system's newline.
 */
public final class Toml {
    private Toml() {
    }

    // ---- the data ----

    /** A table: its entries in the order they were read or added, and their comments. */
    public static final class Table {
        private final Map<String, Object> values = new LinkedHashMap<>();
        private final Map<String, String> comments = new HashMap<>();
        private boolean inline;

        /** The entries (live): values are Boolean, Integer, Long, Double, String, Enum, List, Table or Temporal. */
        public Map<String, Object> values() {
            return values;
        }

        public boolean isEmpty() {
            return values.isEmpty();
        }

        @Nullable
        public Object get(String key) {
            return values.get(key);
        }

        /** The value at {@code path} (through sub-tables), or null. */
        @Nullable
        public Object get(List<String> path) {
            Table table = this;
            for (int i = 0; i < path.size() - 1; i++) {
                if (!(table.values.get(path.get(i)) instanceof Table sub)) {
                    return null;
                }
                table = sub;
            }
            return table.values.get(path.get(path.size() - 1));
        }

        /** Sets the value at {@code path}, making the tables on the way; returns what was there. */
        @Nullable
        public Object set(List<String> path, Object value) {
            Table table = this;
            for (int i = 0; i < path.size() - 1; i++) {
                Object sub = table.values.get(path.get(i));
                if (!(sub instanceof Table)) {
                    sub = new Table();
                    table.values.put(path.get(i), sub);
                }
                table = (Table) sub;
            }
            return table.values.put(path.get(path.size() - 1), value);
        }

        @Nullable
        public String comment(String key) {
            return comments.get(key);
        }

        /** Sets (or, with null, removes) the comment of {@code key}. */
        public void setComment(String key, @Nullable String comment) {
            if (comment == null) {
                comments.remove(key);
            } else {
                comments.put(key, comment);
            }
        }

        void setComment(List<String> path, @Nullable String comment) {
            Table table = this;
            for (int i = 0; i < path.size() - 1; i++) {
                if (!(table.values.get(path.get(i)) instanceof Table sub)) {
                    return;
                }
                table = sub;
            }
            table.setComment(path.get(path.size() - 1), comment);
        }

        /** A deep copy (lists and tables copied, other values shared: they are immutable). */
        public Table copy() {
            Table copy = new Table();
            values.forEach((key, value) -> copy.values.put(key, copyValue(value)));
            copy.comments.putAll(comments);
            copy.inline = inline;
            return copy;
        }

        private static Object copyValue(Object value) {
            if (value instanceof Table table) {
                return table.copy();
            }
            if (value instanceof List<?> list) {
                List<Object> copy = new ArrayList<>(list.size());
                list.forEach(element -> copy.add(copyValue(element)));
                return copy;
            }
            return value;
        }

        @Override
        public String toString() {
            return "Table" + values;
        }
    }

    /** A date or time, kept as it was written (no config option is one). */
    public record Temporal(String text) {
        @Override
        public String toString() {
            return text;
        }
    }

    /** A file that is not TOML (the message says where). */
    public static final class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
    }

    // ---- reading ----

    /** Reads a whole file. */
    public static Table parse(String text) throws ParseException {
        return new Parser(text).parseDocument();
    }

    private static final String KEY_END = "\t =.\n\r]:";
    private static final String END_OF_VALUE = "\t #\n\r,]}";
    private static final String END_OF_VALUE_DATE = "\t#\n\r,]}";

    private static final class Parser {
        private final String text;
        private int pos;
        @Nullable
        private String pendingComment;

        Parser(String text) {
            this.text = text;
        }

        private ParseException error(String message) {
            int line = 1;
            for (int i = 0; i < Math.min(pos, text.length()); i++) {
                if (text.charAt(i) == '\n') {
                    line++;
                }
            }
            return new ParseException(message + " (line " + line + ")");
        }

        private int peek() {
            return pos < text.length() ? text.charAt(pos) : -1;
        }

        private int peek(int ahead) {
            return pos + ahead < text.length() ? text.charAt(pos + ahead) : -1;
        }

        private int read() {
            return pos < text.length() ? text.charAt(pos++) : -1;
        }

        private char readChar() throws ParseException {
            if (pos >= text.length()) {
                throw error("Not enough data available");
            }
            return text.charAt(pos++);
        }

        /** The next char not in {@code skip} (consumed), or -1. */
        private int readAndSkip(String skip) {
            int c;
            do {
                c = read();
            } while (c != -1 && skip.indexOf(c) >= 0);
            return c;
        }

        private char readCharAndSkip(String skip) throws ParseException {
            int c = readAndSkip(skip);
            if (c == -1) {
                throw error("Not enough data available");
            }
            return (char) c;
        }

        /** The text up to (not with) the first char of {@code stops}, or to the end. */
        private String readUntil(String stops) {
            int start = pos;
            while (pos < text.length() && stops.indexOf(text.charAt(pos)) < 0) {
                pos++;
            }
            return text.substring(start, pos);
        }

        /** The rest of the line, its newline consumed and a carriage return before it dropped. */
        private String readLine() {
            String line = readUntil("\n");
            if (pos < text.length()) {
                pos++;
            }
            return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
        }

        /** Skips spaces, newlines and comment lines (gathered in {@code comments}); the next char, or -1. */
        private int readUseful(List<String> comments) throws ParseException {
            int next = readAndSkip("\t \n\r");
            while (next == '#') {
                comments.add(checkComment(readLine()));
                next = readAndSkip("\t \n\r");
            }
            return next;
        }

        /** The same inside an array, where comments are dropped. */
        private char readUsefulChar() throws ParseException {
            char next = readCharAndSkip("\t \n\r");
            while (next == '#') {
                readUntil("\n");
                next = readCharAndSkip("\t \n\r");
            }
            return next;
        }

        private String checkComment(String comment) throws ParseException {
            for (int c : comment.codePoints().toArray()) {
                if (c != '\t' && c != '\n' && (c <= 31 || c == 127)) {
                    throw error("Invalid control character in comment: " + comment);
                }
                if (c >= 0xD800 && c < 0xE000) {
                    throw error("Invalid unicode codepoint in comment: " + comment);
                }
            }
            return comment;
        }

        private void addPendingComment(List<String> comments) {
            if (!comments.isEmpty()) {
                addPendingComment(String.join("\n", comments));
            }
        }

        private void addPendingComment(String comment) {
            pendingComment = pendingComment == null ? comment : pendingComment + '\n' + comment;
        }

        @Nullable
        private String consumeComment() {
            String comment = pendingComment;
            pendingComment = null;
            return comment;
        }

        Table parseDocument() throws ParseException {
            Table root = new Table();
            parseNormal(root);
            while (peek() != -1) {
                // (parseNormal stopped after the '[' of a table name)
                boolean isArray = peek() == '[';
                if (isArray) {
                    pos++;
                }
                List<String> path = parseTableName(isArray);
                String last = path.get(path.size() - 1);
                Table parent = subTable(root, path.subList(0, path.size() - 1));
                if (parent == null) {
                    throw error("Cannot create entry " + path + " because of an invalid parent that isn't a table.");
                }
                if (pendingComment != null) {
                    parent.setComment(last, consumeComment());
                }
                if (isArray) {
                    Table table = new Table();
                    parseNormal(table);
                    Object existing = parent.values.get(last);
                    if (existing instanceof List<?> list) {
                        @SuppressWarnings("unchecked")
                        List<Object> tables = (List<Object>) list;
                        tables.add(table);
                    } else if (existing != null) {
                        throw error("Cannot create entry " + path + " because of an invalid parent that is not an "
                                + "array of tables");
                    } else {
                        List<Object> tables = new ArrayList<>();
                        tables.add(table);
                        parent.values.put(last, tables);
                    }
                } else {
                    Object existing = parent.values.get(last);
                    if (existing == null) {
                        Table table = new Table();
                        parseNormal(table);
                        parent.values.put(last, table);
                    } else if (existing instanceof Table table) {
                        for (Object value : table.values.values()) {
                            boolean tables = value instanceof List<?> list && !list.isEmpty()
                                    && list.get(0) instanceof Table;
                            if (!(value instanceof Table) && !tables) {
                                throw error("Table with path " + path + " has been declared twice.");
                            }
                        }
                        parseNormal(table);
                    } else {
                        throw error("Entry " + path + " has been defined twice.");
                    }
                }
            }
            return root;
        }

        /** The table at {@code path} under {@code root}, made if missing; null if a value is in the way. */
        @Nullable
        private Table subTable(Table root, List<String> path) throws ParseException {
            Table current = root;
            for (String key : path) {
                Object value = current.values.get(key);
                if (value == null) {
                    Table sub = new Table();
                    current.values.put(key, sub);
                    current = sub;
                } else if (value instanceof Table table) {
                    current = table;
                } else if (value instanceof List<?> list && !list.isEmpty()
                        && list.stream().allMatch(Table.class::isInstance)) {
                    current = (Table) list.get(list.size() - 1);
                } else {
                    return null;
                }
                if (current.inline) {
                    throw error("Cannot modify an inline table after its creation. Key path: " + path);
                }
            }
            return current;
        }

        /** The entries of a table, up to the next table name (its '[' consumed) or the end. */
        private void parseNormal(Table table) throws ParseException {
            boolean wasEmpty = table.isEmpty();
            while (true) {
                List<String> comments = new ArrayList<>(2);
                int keyFirst = readUseful(comments);
                if (keyFirst == -1 || keyFirst == '[') {
                    addPendingComment(comments);
                    return;
                }
                List<String> key = parseDottedKey((char) keyFirst);
                if (wasEmpty && key.size() > 1) {
                    Table current = table;
                    for (int i = 0; i < key.size() - 1; i++) {
                        Object sub = current.values.get(key.get(i));
                        if (sub == null) {
                            break;
                        }
                        if (!(sub instanceof Table next)) {
                            throw error("Wrong duplicate key " + key + ": intermediary level "
                                    + key.subList(0, i + 1) + " already exists");
                        }
                        current = next;
                    }
                }
                Object value = parseValue(readCharAndSkip("\t "));
                if (table.set(key, value) != null) {
                    throw error("Invalid TOML data: entry \"" + key + "\" defined twice in its table.");
                }
                int after = readAndSkip("\t ");
                if (after == -1) {
                    // (as NightConfig: the last entry of a file without a final newline keeps no comment)
                    return;
                }
                if (after == '#') {
                    comments.add(checkComment(readLine()));
                } else if (after != '\n' && after != '\r') {
                    throw error("Invalid character '" + (char) after + "' after table entry \"" + key + "\" = "
                            + value);
                }
                addPendingComment(comments);
                table.setComment(key, consumeComment());
            }
        }

        private List<String> parseTableName(boolean array) throws ParseException {
            List<String> names = new ArrayList<>();
            char separator;
            do {
                char first = readCharAndSkip("\t ");
                if (first == ']') {
                    throw error("Tables names must not be empty.");
                }
                names.add(parseNonDottedKey(first));
                separator = readCharAndSkip("\t ");
                if (separator == ']') {
                    if (array) {
                        char after = readChar();
                        if (after != ']') {
                            throw error("Invalid declaration of an element of an array of tables: it ends by ]"
                                    + after + " but should end by ]]");
                        }
                    }
                    char after = readCharAndSkip("\t ");
                    if (after == '#') {
                        addPendingComment(checkComment(readLine()));
                    } else if (after != '\n' && after != '\r') {
                        throw error("Invalid character '" + after + "' after a table declaration.");
                    }
                    return names;
                }
            } while (separator == '.');
            throw error("Invalid separator '" + separator + "' in table name.");
        }

        private List<String> parseDottedKey(char firstChar) throws ParseException {
            List<String> parts = new ArrayList<>();
            char first = firstChar;
            while (true) {
                parts.add(parseNonDottedKey(first));
                char separator = readCharAndSkip("\t ");
                if (separator == '=') {
                    return parts;
                }
                if (separator != '.') {
                    throw error("Invalid character '" + separator + "' after key " + parts);
                }
                first = readCharAndSkip("\t ");
            }
        }

        private String parseNonDottedKey(char first) throws ParseException {
            if (first == '"') {
                return parseBasic();
            }
            if (first == '\'') {
                return parseLiteral();
            }
            String key = first + readUntil(KEY_END);
            if (!isValidBareKey(key)) {
                throw error("Invalid bare key: '" + key + "'");
            }
            return key;
        }

        // -- values --

        private Object parseValue(char first) throws ParseException {
            switch (first) {
                case '"':
                    if (peek() == '"' && peek(1) == '"') {
                        pos += 2;
                        return parseMultiBasic();
                    }
                    return parseBasic();
                case '\'':
                    if (peek() == '\'' && peek(1) == '\'') {
                        pos += 2;
                        return parseMultiLiteral();
                    }
                    return parseLiteral();
                case '+':
                case '-':
                    pos--;
                    return parseNumber(readUntil(END_OF_VALUE));
                case '[':
                    return parseArray();
                case 'f': {
                    String rest = readUntil(END_OF_VALUE);
                    if (!rest.equals("alse")) {
                        throw error("Invalid value f" + rest + " - Expected the boolean value false.");
                    }
                    return Boolean.FALSE;
                }
                case 't': {
                    String rest = readUntil(END_OF_VALUE);
                    if (!rest.equals("rue")) {
                        throw error("Invalid value t" + rest + " - Expected the boolean value true.");
                    }
                    return Boolean.TRUE;
                }
                case '{':
                    return parseInlineTable();
                default: {
                    pos--;
                    String chars = readUntil(END_OF_VALUE_DATE);
                    if (chars.length() >= 8 && (chars.charAt(2) == ':'
                            || chars.charAt(4) == '-' && chars.charAt(7) == '-')) {
                        return new Temporal(chars.strip());
                    }
                    String trimmed = chars.strip();
                    if (trimmed.isEmpty()) {
                        throw error("Invalid value containing only whitespaces");
                    }
                    return parseNumber(trimmed);
                }
            }
        }

        private List<Object> parseArray() throws ParseException {
            List<Object> list = new ArrayList<>();
            boolean first = true;
            while (true) {
                char c = readUsefulChar();
                if (c == ']') {
                    return list;
                }
                if (c == ',') {
                    throw error(first ? "Invalid array: [,]" : "Invalid double comma in array.");
                }
                list.add(parseValue(c));
                char after = readUsefulChar();
                if (after == ']') {
                    return list;
                }
                if (after != ',') {
                    throw error("Invalid separator '" + after + "' in array.");
                }
                first = false;
            }
        }

        private Table parseInlineTable() throws ParseException {
            Table table = new Table();
            table.inline = true;
            boolean expectNext = false;
            while (true) {
                char keyFirst = readCharAndSkip("\t ");
                if (keyFirst == '}') {
                    if (expectNext) {
                        throw error("Invalid trailing comma in inline table");
                    }
                    return table;
                }
                List<String> key = parseDottedKey(keyFirst);
                Object value = parseValue(readCharAndSkip("\t "));
                if (table.set(key, value) != null) {
                    throw error("Invalid TOML data: entry \"" + key + "\" defined twice in its table.");
                }
                char after = readCharAndSkip("\t ");
                if (after == '}') {
                    return table;
                }
                if (after != ',') {
                    throw error("Invalid entry separator '" + after + "' in inline table.");
                }
                expectNext = true;
            }
        }

        private static boolean isDigit(int c, int base) {
            return switch (base) {
                case 2 -> c == '0' || c == '1';
                case 8 -> c >= '0' && c <= '7';
                case 16 -> c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
                default -> c >= '0' && c <= '9';
            };
        }

        private Number parseNumber(String chars) throws ParseException {
            char first = chars.charAt(0);
            String rest = first == '-' || first == '+' ? chars.substring(1) : chars;
            if (rest.equals("inf")) {
                return first == '-' ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            }
            if (rest.equals("nan")) {
                return Double.NaN;
            }
            String number = chars;
            int base = 10;
            if (chars.length() > 2) {
                switch (chars.substring(0, 2)) {
                    case "0x" -> base = 16;
                    case "0b" -> base = 2;
                    case "0o" -> base = 8;
                    default -> {
                    }
                }
                if (base != 10) {
                    number = chars.substring(2);
                }
            }
            number = simplifyNumber(number, base);
            if (base == 10 && (number.indexOf('.') >= 0 || number.indexOf('e') >= 0 || number.indexOf('E') >= 0)) {
                if (number.length() > 1) {
                    if (number.charAt(0) == '0' && isDigit(number.charAt(1), 10)) {
                        throw error("Invalid leading zero in floating-point number " + number);
                    }
                    if (number.length() > 2 && (number.startsWith("-0") || number.startsWith("+0"))
                            && isDigit(number.charAt(2), 10)) {
                        throw error("Invalid leading zero (after sign) in floating-point number " + number);
                    }
                }
                try {
                    return Double.parseDouble(number);
                } catch (NumberFormatException e) {
                    throw error("Invalid floating-point value: " + chars);
                }
            }
            if (base == 10) {
                if (number.length() > 1 && number.charAt(0) == '0') {
                    throw error("Invalid leading zero in base 10 integer " + number);
                }
                if (number.length() > 2 && (number.startsWith("-0") || number.startsWith("+0"))
                        && isDigit(number.charAt(2), 10)) {
                    throw error("Invalid leading zero (after sign) in base 10 integer " + number);
                }
            }
            long value;
            try {
                value = Long.parseLong(number, base);
            } catch (NumberFormatException e) {
                throw error("Invalid integer value: " + chars);
            }
            int small = (int) value;
            return small == value ? (Number) small : (Number) value;
        }

        private String simplifyNumber(String chars, int base) throws ParseException {
            StringBuilder builder = new StringBuilder(chars.length());
            int previous = -1;
            for (int i = 0; i < chars.length(); i++) {
                char c = chars.charAt(i);
                switch (c) {
                    case '.' -> {
                        if (previous == -1) {
                            throw error("Invalid leading decimal point in number " + chars);
                        }
                        if (!isDigit(previous, base)) {
                            throw error("Invalid decimal point in number (each decimal point must be surrounded by "
                                    + "digits): " + chars);
                        }
                        builder.append(c);
                    }
                    case '_' -> {
                        if (previous == -1) {
                            throw error("Invalid leading underscore in number " + chars);
                        }
                        if (!isDigit(previous, base)) {
                            throw error("Invalid underscore in number (each underscore must be surrounded by digits): "
                                    + chars);
                        }
                    }
                    case 'e' -> {
                        if (base != 16 && !isDigit(previous, base)) {
                            throw error("Invalid sequence " + previous + "e in number" + chars);
                        }
                        builder.append(c);
                    }
                    default -> builder.append(c);
                }
                previous = c;
            }
            if (previous == '_' || previous == '.' || previous == 'e' || previous == 'E' || previous == '-'
                    || previous == '+') {
                throw error("Invalid trailing '" + previous + "' in number: " + chars);
            }
            return builder.toString();
        }

        // -- strings --

        private String parseBasic() throws ParseException {
            StringBuilder builder = new StringBuilder();
            while (true) {
                char c = readChar();
                if (c == '"') {
                    return builder.toString();
                }
                if (c == '\\') {
                    builder.append(unescape(readChar()));
                } else if (c == '\n' || c == '\r') {
                    throw error("Invalid newline in basic string, you should use a multiline string or escape the "
                            + "newline by writing \\n. The string begins with: \"" + builder + "\"");
                } else if (c != '\t' && isControlChar(c)) {
                    throw error("Invalid control character in string, you should escape it");
                } else {
                    builder.append(c);
                }
            }
        }

        private String parseLiteral() throws ParseException {
            String str = readUntil("'\n\r");
            if (read() != '\'') {
                throw error("Invalid newline in literal string, you should use a multiline string. The string is '"
                        + str + "'");
            }
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c != '\t' && isControlChar(c)) {
                    throw error("Invalid control character in literal string '" + str + "'");
                }
            }
            return str;
        }

        private String parseMultiBasic() throws ParseException {
            StringBuilder builder = new StringBuilder();
            char c;
            while ((c = readChar()) != '"' || peek() != '"' || peek(1) != '"') {
                if (c == '\\') {
                    char next = readChar();
                    boolean lineEnd = next == '\n' || next == '\r' && peek() == '\n';
                    if (!lineEnd && (next == '\t' || next == ' ')) {
                        // a backslash and spaces up to the end of the line
                        int save = pos;
                        String line = readLine();
                        if (line.isBlank() && line.chars().allMatch(ch -> ch == ' ' || ch == '\t')) {
                            lineEnd = true;
                        } else {
                            pos = save;
                            throw error("Invalid escapement: \\" + next);
                        }
                    }
                    if (lineEnd) {
                        int nextNonSpace = readAndSkip("\t \n\r");
                        if (nextNonSpace != -1) {
                            pos--;
                        }
                    } else {
                        builder.append(unescape(next));
                    }
                } else {
                    if (c != '\n' && c != '\r' && c != '\t' && isControlChar(c)) {
                        throw error("Invalid control character in multiline string, you should escape it");
                    }
                    builder.append(c);
                }
            }
            pos += 2;
            if (peek() == '"') {
                pos++;
                builder.append('"');
            }
            if (peek() == '"') {
                pos++;
                builder.append('"');
            }
            return trimFirstNewline(builder);
        }

        private String parseMultiLiteral() throws ParseException {
            StringBuilder builder = new StringBuilder();
            char c;
            while ((c = readChar()) != '\'' || peek() != '\'' || peek(1) != '\'') {
                if (c != '\n' && c != '\r' && c != '\t' && isControlChar(c)) {
                    throw error("Invalid control character in multiline literal string, you should escape it");
                }
                builder.append(c);
            }
            pos += 2;
            if (peek() == '\'') {
                pos++;
                builder.append('\'');
            }
            if (peek() == '\'') {
                pos++;
                builder.append('\'');
            }
            return trimFirstNewline(builder);
        }

        private static String trimFirstNewline(StringBuilder builder) {
            if (!builder.isEmpty() && builder.charAt(0) == '\n') {
                return builder.substring(1);
            }
            if (builder.length() > 1 && builder.charAt(0) == '\r' && builder.charAt(1) == '\n') {
                return builder.substring(2);
            }
            return builder.toString();
        }

        private String unescape(char c) throws ParseException {
            return switch (c) {
                case '"', '\\' -> String.valueOf(c);
                case 'b' -> "\b";
                case 'f' -> "\f";
                case 'n' -> "\n";
                case 'r' -> "\r";
                case 't' -> "\t";
                case 'u' -> codePoint(4);
                case 'U' -> codePoint(8);
                default -> throw error("Invalid escapement: \\" + c);
            };
        }

        private String codePoint(int digits) throws ParseException {
            if (pos + digits > text.length()) {
                throw error("Not enough data available");
            }
            String hex = text.substring(pos, pos + digits);
            pos += digits;
            int codePoint;
            try {
                codePoint = Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                throw error("Invalid unicode codepoint: " + hex);
            }
            if (codePoint < 0 || codePoint > 0xD7FF && codePoint < 0xE000 || codePoint > 0x10FFFF) {
                throw error("Invalid unicode codepoint: " + hex);
            }
            return new String(Character.toChars(codePoint));
        }
    }

    private static boolean isControlChar(int c) {
        return c <= 31 || c == 127;
    }

    static boolean isValidBareKey(CharSequence key) {
        if (key.isEmpty()) {
            return false;
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_')) {
                return false;
            }
        }
        return true;
    }

    // ---- writing ----

    /** The text of a file, with the system's line ends. */
    public static String write(Table table) {
        return write(table, System.lineSeparator());
    }

    /** The text of a file, with {@code newline} as line end. */
    public static String write(Table table, String newline) {
        Writer writer = new Writer(newline);
        writer.writeWithHeader(table, null, false, false, new ArrayList<>());
        return writer.out.toString();
    }

    private static final class Writer {
        private final StringBuilder out = new StringBuilder();
        private final String newline;
        private int indent = -1;

        Writer(String newline) {
            this.newline = newline;
        }

        private void writeIndent() {
            for (int i = 0; i < indent; i++) {
                out.append('\t');
            }
        }

        private void writeComment(@Nullable String comment) {
            for (String line : splitLines(comment)) {
                writeIndent();
                out.append('#').append(line).append(newline);
            }
        }

        private void writeKey(String key) {
            if (isValidBareKey(key)) {
                out.append(key);
            } else {
                writeBasic(key);
            }
        }

        private static boolean isTables(Object value) {
            return value instanceof List<?> list && !list.isEmpty() && list.stream().allMatch(Table.class::isInstance);
        }

        void writeWithHeader(Table table, @Nullable String tableComment, boolean inArrayOfTables, boolean header,
                             List<String> path) {
            String comment = tableComment == null ? "" : tableComment;
            List<Map.Entry<String, Object>> simples = new ArrayList<>();
            List<Map.Entry<String, Object>> subTables = new ArrayList<>();
            List<Map.Entry<String, Object>> arrays = new ArrayList<>();
            for (Map.Entry<String, Object> entry : table.values.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Table sub) {
                    (sub.isEmpty() ? simples : subTables).add(entry);
                } else if (isTables(value)) {
                    arrays.add(entry);
                } else {
                    simples.add(entry);
                }
            }
            boolean skip = (comment.isEmpty() || !arrays.isEmpty()) && simples.isEmpty()
                    && (!subTables.isEmpty() || !arrays.isEmpty());
            if (skip) {
                indent++;
                if (inArrayOfTables) {
                    writeTableName(path, "[[", "]]");
                    out.append(newline);
                    indent++;
                }
                writeSubTables(table, subTables, !arrays.isEmpty(), path);
                if (inArrayOfTables) {
                    indent--;
                }
                if (!comment.isEmpty()) {
                    writeComment(tableComment);
                }
                writeArraysOfTables(table, arrays, path);
                indent--;
            } else {
                if (!comment.isEmpty()) {
                    writeComment(tableComment);
                }
                if (inArrayOfTables) {
                    writeTableName(path, "[[", "]]");
                    out.append(newline);
                } else if (header) {
                    writeTableName(path, "[", "]");
                    out.append(newline);
                }
                indent++;
                for (Map.Entry<String, Object> entry : simples) {
                    writeComment(table.comment(entry.getKey()));
                    writeIndent();
                    writeKey(entry.getKey());
                    out.append(" = ");
                    writeValue(entry.getValue());
                    out.append(newline);
                }
                if (!subTables.isEmpty()) {
                    out.append(newline);
                }
                writeSubTables(table, subTables, !arrays.isEmpty(), path);
                writeArraysOfTables(table, arrays, path);
                indent--;
            }
        }

        private void writeSubTables(Table table, List<Map.Entry<String, Object>> subTables, boolean arraysFollow,
                                    List<String> path) {
            Iterator<Map.Entry<String, Object>> it = subTables.iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                path.add(entry.getKey());
                writeWithHeader((Table) entry.getValue(), table.comment(entry.getKey()), false, true, path);
                path.remove(path.size() - 1);
                if (arraysFollow || it.hasNext()) {
                    out.append(newline);
                }
            }
        }

        private void writeArraysOfTables(Table table, List<Map.Entry<String, Object>> arrays, List<String> path) {
            Iterator<Map.Entry<String, Object>> it = arrays.iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                path.add(entry.getKey());
                for (Object sub : (List<?>) entry.getValue()) {
                    writeWithHeader((Table) sub, table.comment(entry.getKey()), true, true, path);
                }
                path.remove(path.size() - 1);
                if (it.hasNext()) {
                    out.append(newline);
                }
            }
        }

        private void writeTableName(List<String> path, String begin, String end) {
            writeIndent();
            out.append(begin);
            for (int i = 0; i < path.size(); i++) {
                if (i > 0) {
                    out.append('.');
                }
                writeKey(path.get(i));
            }
            out.append(end);
        }

        private void writeValue(Object value) {
            if (value instanceof Table table) {
                out.append('{');
                Iterator<Map.Entry<String, Object>> it = table.values.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Object> entry = it.next();
                    writeKey(entry.getKey());
                    out.append(" = ");
                    writeValue(entry.getValue());
                    if (it.hasNext()) {
                        out.append(", ");
                    }
                }
                out.append('}');
            } else if (value instanceof List<?> list) {
                out.append('[');
                Iterator<?> it = list.iterator();
                while (it.hasNext()) {
                    writeValue(it.next());
                    if (it.hasNext()) {
                        out.append(", ");
                    }
                }
                out.append(']');
            } else if (value instanceof CharSequence string) {
                writeString(string.toString());
            } else if (value instanceof Enum<?> constant) {
                writeString(constant.name());
            } else if (value instanceof Temporal temporal) {
                out.append(temporal.text());
            } else if (value instanceof Float || value instanceof Double) {
                double d = ((Number) value).doubleValue();
                if (Double.isNaN(d)) {
                    out.append("nan");
                } else if (d == Double.POSITIVE_INFINITY) {
                    out.append("+inf");
                } else if (d == Double.NEGATIVE_INFINITY) {
                    out.append("-inf");
                } else {
                    out.append(value);
                }
            } else if (value instanceof Number || value instanceof Boolean) {
                out.append(value);
            } else if (value == null) {
                throw new IllegalArgumentException("TOML doesn't support null values");
            } else {
                throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
            }
        }

        private void writeString(String str) {
            int i0 = str.indexOf('\n');
            int i1 = str.indexOf('\n', i0 + 1);
            int i2 = str.indexOf('\n', i1 + 1);
            int cr = str.indexOf('\r');
            boolean multiline = cr == -1 && (i0 >= 0 && i1 > 0 && i2 > 0 || i0 > 0 && i0 < str.length() - 1);
            if (multiline) {
                writeBasicMultiline(str);
            } else {
                writeBasic(str);
            }
        }

        private void writeBasic(String str) {
            out.append('"');
            str.codePoints().forEach(c -> {
                switch (c) {
                    case '\b' -> out.append("\\b");
                    case '\t' -> out.append("\\t");
                    case '\n' -> out.append("\\n");
                    case '\f' -> out.append("\\f");
                    case '\r' -> out.append("\\r");
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    default -> {
                        if (isControlChar(c)) {
                            out.append(escapeUnicode(c));
                        } else {
                            out.appendCodePoint(c);
                        }
                    }
                }
            });
            out.append('"');
        }

        private void writeBasicMultiline(String str) {
            out.append("\"\"\"");
            List<String> lines = splitLines(str);
            for (int l = 0; l < lines.size(); l++) {
                boolean more = l + 1 < lines.size();
                out.append(newline);
                char[] chars = lines.get(l).toCharArray();
                for (int i = 0; i < chars.length; i++) {
                    char c = chars[i];
                    switch (c) {
                        case '\b' -> out.append("\\b");
                        case '\f' -> out.append("\\f");
                        case '"' -> {
                            boolean quoteEnd = (i + 1 == chars.length && !more)
                                    || (i + 1 < chars.length && chars[i + 1] == '"' && i + 2 < chars.length
                                    && chars[i + 2] == '"' && i + 3 < chars.length);
                            out.append(quoteEnd ? "\\\"" : "\"");
                        }
                        case '\\' -> out.append("\\\\");
                        default -> {
                            if (c != '\t' && c != '\n' && c != '\r' && isControlChar(c)) {
                                out.append(escapeUnicode(c));
                            } else {
                                out.append(c);
                            }
                        }
                    }
                }
            }
            out.append("\"\"\"");
        }

        private static String escapeUnicode(int codePoint) {
            StringBuilder hex = new StringBuilder(Integer.toHexString(codePoint).toUpperCase(java.util.Locale.ROOT));
            int width = hex.length() < 4 ? 4 : 8;
            while (hex.length() < width) {
                hex.insert(0, '0');
            }
            return "\\u" + hex;
        }
    }

    /** The lines of a text ({@code \n}, {@code \r\n} or {@code \r}); none for null or empty. */
    static List<String> splitLines(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                lines.add(text.substring(start, i));
                start = i + 1;
            } else if (c == '\r') {
                lines.add(text.substring(start, i));
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                start = i + 1;
            }
        }
        lines.add(text.substring(start));
        return lines;
    }
}
