package dev.webview.webview_java.example.raw;

import dev.webview.webview_java.Webview;

public class Example {

    public static void main(String[] args) {
        Webview wv = new Webview(true); // Can optionally be created with an AWT component to be painted on.

        // Calling `await echo(1,2,3)` will return `[1,2,3]`
        wv.bind("echo", (arguments) -> {
            return arguments;
        });

        wv.setTitle("My Webview App");
        //wv.setSize(800, 600);

        // load a URL
        wv.loadURL("https://google.com");

        /*

        Or, load raw html from a file with:
        wv.setHTML("<h1>This is a test!<h1>");

        String htmlContent = loadContentFromFile("index.html");
        wv.setHTML(htmlContent);

         */

        wv.run(); // Run the webview event loop, the webview is fully disposed when this returns.
        wv.close(); // Free any resources allocated.
    }

}
