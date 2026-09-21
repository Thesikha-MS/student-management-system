import java.util.HashMap;
import java.util.Map;

/**
 * Minimal JSON utility used so the project needs zero external
 * dependencies (no Gson/Jackson, no Spring, no Maven). It only supports
 * flat JSON objects with string/number values, which is all this
 * application needs.
 */
public final class Json {

    private Json() { }

    /** Escapes and wraps a string in double quotes for JSON output. */
    public static String quote(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    /**
     * Parses a flat JSON object (e.g. {"name":"A","marks":88.5}) into a
     * Map of String -> String. Callers convert values to the type they
     * expect (Double.parseDouble, etc). Not a general purpose parser -
     * it deliberately only handles what this app sends.
     */
    public static Map<String, String> parseObject(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null) return map;
        String s = body.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);

        int i = 0;
        int n = s.length();
        while (i < n) {
            // skip whitespace / commas
            while (i < n && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) i++;
            if (i >= n) break;

            // read key
            if (s.charAt(i) != '"') break;
            i++;
            StringBuilder key = new StringBuilder();
            while (i < n && s.charAt(i) != '"') {
                if (s.charAt(i) == '\\' && i + 1 < n) { key.append(s.charAt(i + 1)); i += 2; }
                else { key.append(s.charAt(i)); i++; }
            }
            i++; // closing quote

            while (i < n && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ':')) i++;

            // read value
            String value;
            if (i < n && s.charAt(i) == '"') {
                i++;
                StringBuilder val = new StringBuilder();
                while (i < n && s.charAt(i) != '"') {
                    if (s.charAt(i) == '\\' && i + 1 < n) { val.append(s.charAt(i + 1)); i += 2; }
                    else { val.append(s.charAt(i)); i++; }
                }
                i++; // closing quote
                value = val.toString();
            } else {
                int valStart = i;
                while (i < n && s.charAt(i) != ',' && s.charAt(i) != '}') i++;
                value = s.substring(valStart, i).trim();
            }
            map.put(key.toString(), value);
        }
        return map;
    }
}
