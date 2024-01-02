package dev.webview.webview_java;

import java.io.PrintWriter;
import java.io.StringWriter;

import lombok.NonNull;

class WebviewUtil {

    static String getExceptionStack(@NonNull Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        e.printStackTrace(pw);

        String out = sw.toString();

        pw.flush();
        pw.close();
        sw.flush();

        return out
            .substring(0, out.length() - 2)
            .replace("\r", "");
    }

    static String jsonEscape(@NonNull String input) {
        char[] chars = input.toCharArray();

        StringBuilder output = new StringBuilder();

        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];

            switch (ch) {
                case 0: {
                    output.append("\\u0000");
                    break;
                }

                case '\n': {
                    output.append("\\n");
                    break;
                }

                case '\t': {
                    output.append("\\t");
                    break;
                }

                case '\r': {
                    output.append("\\r");
                    break;
                }

                case '\\': {
                    output.append("\\\\");
                    break;
                }

                case '"': {
                    output.append("\\\"");
                    break;
                }

                case '\b': {
                    output.append("\\b");
                    break;
                }

                case '\f': {
                    output.append("\\f");
                    break;
                }

                default: {
                    if (ch > 127) {
                        output.append("\\u").append(String.format("%04x", (int) ch));
                    } else {
                        output.append(ch);
                    }

                    break;
                }
            }
        }

        return output.toString();
    }

}
