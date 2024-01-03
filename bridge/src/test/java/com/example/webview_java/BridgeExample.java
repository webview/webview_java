package com.example.webview_java;

import dev.webview.webview_java.Webview;
import dev.webview.webview_java.binding.JavascriptFunction;
import dev.webview.webview_java.binding.JavascriptObject;
import dev.webview.webview_java.binding.JavascriptValue;
import dev.webview.webview_java.binding.WebviewBridge;

public class BridgeExample {

    public static void main(String[] args) {
        Webview wv = new Webview(true); // Can optionally be created with an AWT component to be painted on.
        WebviewBridge bridge = new WebviewBridge(wv);

        bridge.defineObject("Test", new TestObject());

        wv.setTitle("My Webview App");
        wv.setSize(800, 600);
        wv.loadURL("https://example.com");

        wv.run(); // Run the webview event loop, the webview is fully disposed when this returns.
        wv.close(); // Free any resources allocated.
    }

    public static class TestObject extends JavascriptObject {

        @JavascriptValue(watchForMutate = true)
        public int variable = 123;

        @JavascriptFunction
        public void doSomething(int number) {
            System.out.println(number);
        }

    }

}
