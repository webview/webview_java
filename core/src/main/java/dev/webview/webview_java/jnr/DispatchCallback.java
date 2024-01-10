package dev.webview.webview_java.jnr;

import jnr.ffi.annotations.Delegate;

public interface DispatchCallback {

    /**
     * @param $pointer The pointer of the webview
     * @param arg      Unused
     */
    @Delegate
    void callback(long $pointer, long arg);

}