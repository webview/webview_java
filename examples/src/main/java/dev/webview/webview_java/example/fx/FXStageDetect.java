package dev.webview.webview_java.example.fx;

import com.sun.javafx.tk.TKStage;
import com.sun.jna.Pointer;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FXStageDetect {

    /**
     * Get stage's HWND handler<br/>
     * jdk17 (jdk11+ ?)
     */
    public static Pointer getWindowPointer(Stage stage) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        // stage inherit from window
        Method getPeer = Window.class.getDeclaredMethod("getPeer");
        getPeer.setAccessible(true);
        final TKStage tkStage = (TKStage) getPeer.invoke(stage);

        Method getRawHandle = tkStage.getClass().getDeclaredMethod("getRawHandle");
        getRawHandle.setAccessible(true);
        Long platformWindow = (Long) getRawHandle.invoke(tkStage);

        return new Pointer(platformWindow);
    }

}
