package dev.webview;

import java.util.function.Function;

public interface WebviewBindCallback extends Function<String, String> {

    /**
     * @param  jsonArgs A JSON string containing an array of arguments.
     * 
     * @return          A JSON string to be deserialized in the Webview.
     */
    @Override
    public String apply(String jsonArgs);

}
