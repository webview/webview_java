package dev.webview.webview_java.jnrfx;

import com.sun.javafx.tk.TKStage;
import javafx.stage.Stage;
import javafx.stage.Window;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.provider.IntPointer;

import java.lang.reflect.Method;

public class FXStageDetect {

    /** Get stage's HWND handler */
    // https://stackoverflow.com/questions/15034407/how-can-i-get-the-window-handle-hwnd-for-a-stage-in-javafx
    public static Pointer getWindowPointer(Stage stage) {
        try {
            // jdk17 (jdk11+ ?)  stage 继承自 window
            Method getPeer = Window.class.getDeclaredMethod("getPeer");
            getPeer.setAccessible(true);
            final TKStage tkStage = (TKStage)getPeer.invoke(stage);

            Method getRawHandle = tkStage.getClass().getDeclaredMethod("getRawHandle" );
            getRawHandle.setAccessible(true);
            Long platformWindow = (Long) getRawHandle.invoke(tkStage);

            System.out.println("platform-peer: " + platformWindow);
            // int ?
            return new IntPointer(Runtime.getSystemRuntime(), platformWindow);
        } catch (Throwable e) {
            System.err.println("Error getting Window Pointer");
            e.printStackTrace();
            return null;
        }
    }

}
