package com.example.webview_java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.webview.webview_java.Webview;
import dev.webview.webview_java.jnr.WebviewJnr;

import java.util.Random;

public class ExampleJson {

    private static final Random random = new Random();

    public static void main(String[] args) {
//        Webview wv = new Webview(true); // Can optionally be created with an AWT component to be painted on.
        WebviewJnr wv = new WebviewJnr(true);

        // Calling `await echo(1,2,3)` will return `[1,2,3]`
        wv.bind("a", (arguments) -> {
            return arguments;
        });
        wv.bind("aa", (arguments) -> {
            return arguments;
        });
        wv.bind("b", (arguments) -> {
            System.out.println("arguments: " + arguments);
            return arguments;
        });

        // await b(200)
        wv.bind("c", (arguments) -> {
            System.out.println("arguments: " + arguments);
            try {
                ArrayNode argss = (ArrayNode) SimpleJackson.toJson(arguments);
                if(argss.get(0) == null) {
                    return "[\"single string test\"]";
                }

                // 测试字符返回上限 6442-6684
                int size = argss.get(0).asInt();
                byte[] bs = new byte[size];

                for(int i=0; i<bs.length; i++) {
                    bs[i] = (byte) (random.nextInt(26) + 'a');
                }
                String randomStr = new String(bs);
                return "[\""+randomStr+"\"]";
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        wv.setTitle("My Webview App");
        wv.setSize(300, 200);

        // load a URL
        //wv.loadURL("https://baidu.com");
        wv.loadURL(null);

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
