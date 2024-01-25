package dev.webview.webview_java.example.jsbridge;

import dev.webview.webview_java.Webview;
import dev.webview.webview_java.bridge.WebviewBridge;

public class JsBridgeExample {

    public static void main(String[] args) {
        Webview wv = new Webview(true); // Can optionally be created with an AWT component to be painted on.

        wv.setTitle("My Webview App");
        wv.setSize(300, 200);

        WebviewBridge bridge = new WebviewBridge(wv);
        // await jpp.a("a"), await jpp.b("a", "b"),   await jpp.c(1024),
        // await jpp.count,  jpp.count=10
        bridge.defineObject("jpp", new RandStr());

        // load a URL
//        wv.loadURL("https://google.com");
        wv.loadURL("https://baidu.com");
        //wv.loadURL(null);

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
