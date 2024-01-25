package dev.webview.webview_java.example.fx;

import com.sun.jna.ptr.PointerByReference;
import dev.webview.webview_java.Webview;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.lang.reflect.InvocationTargetException;

/**
 * do not support resize!
 * */
public class FxExample extends Application {

    private StackPane root;
    private Webview wv;
    private Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        root = new StackPane();

        Scene s = new Scene(root, 400,300);
        stage.setScene(s);
        stage.setTitle("TestWindow");

        stage.show();
        this.stage = stage;
        stage.setResizable(false);  // not allow to resize

        this.initWv();
    }

    private void initWv() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Scene s = stage.getScene();

        wv = new Webview(true, new PointerByReference(FXStageDetect.getWindowPointer(stage)),
                (int)s.getWidth(), (int)s.getHeight());
        wv.setTitle("TestWindow");

        bindJs();

        // bind resize (not working)
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
                //System.exit(0);
            }
        });

        wv.loadURL("https://google.com");
        //wv.loadURL("https://baidu.com");
        //wv.loadURL(null);

//        new Thread(() -> {
        wv.run(); // It locks here (event if `wv.close()` has been invoked ). The code bellow won't be run, why?
//        }).start();
    }

    private void bindJs() {

    }

    private void beforeClose() {
        System.out.println("before close");
    }

}
