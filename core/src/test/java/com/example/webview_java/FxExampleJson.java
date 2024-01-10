package com.example.webview_java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.webview.webview_java.jnr.WebviewJnr;
import dev.webview.webview_java.jnrfx.FXStageDetect;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.Random;

public class FxExampleJson extends Application {

    StackPane root;
    WebviewJnr wv;
    Stage stage;
    private static final Random random = new Random();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        root = new StackPane();

        Scene s = new Scene(root, 200,100);
        stage.setScene(s);
        stage.setTitle("TestWindow");

        stage.show();
        this.stage = stage;

        this.initWv();
    }

    private void initWv() {
        wv = new WebviewJnr(true, FXStageDetect.getWindowPointer(stage));
        wv.setTitle("TestWindow");

        bindJs();

        // 绑定尺寸
        Scene s = stage.getScene();
        s.widthProperty().addListener((observable, oldValue, newValue) -> {
            wv.setSize(newValue.intValue(), (int)s.getHeight());
        });
        s.heightProperty().addListener((observable, oldValue, newValue) -> {
            wv.setSize((int)s.getWidth(), newValue.intValue());
        });

        // 关闭
        stage.setOnCloseRequest(e -> {
            this.beforeClose();
            if (wv!=null) {
                wv.close();
                System.exit(0);
            }
        });

//        new Thread(() -> {
        System.out.println("before run");
        wv.run(); // It locks here (event if `wv.close()` has been invoked ). The code bellow won't be run, why?
        System.out.println("after run");
//        }).start();
    }

    private void bindJs() {
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
    }

    private void beforeClose() {
        System.out.println("before close");
    }

}
