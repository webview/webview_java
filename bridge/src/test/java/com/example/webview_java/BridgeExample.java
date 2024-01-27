package com.example.webview_java;

import java.awt.Toolkit;

import co.casterlabs.commons.async.AsyncTask;
import dev.webview.webview_java.Webview;
import dev.webview.webview_java.bridge.JavascriptFunction;
import dev.webview.webview_java.bridge.JavascriptObject;
import dev.webview.webview_java.bridge.JavascriptValue;
import dev.webview.webview_java.bridge.WebviewBridge;

public class BridgeExample {

    public static void main(String[] args) {
        Webview wv = new Webview(true); // Can optionally be created with an AWT component to be painted on.
        WebviewBridge bridge = new WebviewBridge(wv);

        bridge.defineObject("Test", new TestObject());

        wv.bind("test", (_unused) -> {
            return "junk";
        });

        // Calling `await echo(1,2,3)` will return `[1,2,3]`
        wv.bind("setDarkAppearance", (arguments) -> {
            wv.setDarkAppearance(arguments.contains("true")); // Use an actual Json parser. This is just a dirty example.
            return null;
        });

        wv.setTitle("My Webview App");
        wv.setSize(800, 600);
        wv.setHTML(
            "<!DOCTYPE html>"
                + "<html>"
                + "<p>Nano Time: <span id='nano-time'></span></p>"
                + "<button onclick='Test.ringBell();'>Ring Bell</button>"
                + "<button onclick='setDarkAppearance(true);'>Set Dark</button>"
                + "<button onclick='setDarkAppearance(false);'>Set Light</button>"
                + "<script>"
                + "Test.__stores.svelte('nanoTime')"
                + ".subscribe((time) => {"
                + "document.querySelector('#nano-time').innerText = time;"
                + "});"
                + "</script>"
                + "</html>"
        );

        wv.run(); // Run the webview event loop, the webview is fully disposed when this returns.
        wv.close(); // Free any resources allocated.
    }

    public static class TestObject extends JavascriptObject {

        public final NestedTestObject nested = new NestedTestObject();

        @JavascriptValue(allowSet = false, watchForMutate = true)
        public long nanoTime = -1;
        {
            AsyncTask.create(() -> {
                try {
                    while (true) {
                        this.nanoTime = System.nanoTime();
                        Thread.sleep(100);
                    }
                } catch (InterruptedException ignored) {}
            });
        }

        @JavascriptFunction
        public void ringBell() {
            Toolkit.getDefaultToolkit().beep();
        }

    }

    public static class NestedTestObject extends JavascriptObject {

        @JavascriptFunction
        public void test() {
            System.out.println("Test");
        }

    }

}
