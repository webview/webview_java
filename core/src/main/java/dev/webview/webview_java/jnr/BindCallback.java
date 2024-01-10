package dev.webview.webview_java.jnr;

import jnr.ffi.annotations.Delegate;

public interface BindCallback {

    /**
     * @param seq The request id, used in {@link webview_return}
     * @param req The javascript arguments converted to a json array (string)
     * @param arg Unused
     */
    @Delegate
    void callback(long seq, String req, long arg);
}
