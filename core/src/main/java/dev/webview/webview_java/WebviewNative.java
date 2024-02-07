/**
 * MIT LICENSE
 *
 * Copyright (c) 2024 Alex Bowles @ Casterlabs
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.webview.webview_java;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import co.casterlabs.commons.io.streams.StreamUtil;
import co.casterlabs.commons.platform.LinuxLibC;
import co.casterlabs.commons.platform.Platform;
import lombok.NonNull;
import lombok.SneakyThrows;

interface WebviewNative extends Library {
    static final WebviewNative N = runSetup();

    @SneakyThrows
    private static WebviewNative runSetup() {
        String[] libraries = null;

        switch (Platform.osDistribution) {
            case LINUX: {
                if (LinuxLibC.isGNU()) {
                    libraries = new String[] {
                            "/dev/webview/webview_java/natives/" + Platform.archTarget + "/linux/gnu/libwebview.so"
                    };
                } else {
                    libraries = new String[] {
                            "/dev/webview/webview_java/natives/" + Platform.archTarget + "/linux/musl/libwebview.so"
                    };
                }
                break;
            }

            case MACOS: {
                libraries = new String[] {
                        "/dev/webview/webview_java/natives/" + Platform.archTarget + "/macos/libwebview.dylib"
                };
                break;
            }

            case WINDOWS_NT: {
                libraries = new String[] {
//                        "/dev/webview/webview_java/natives/" + Platform.archTarget + "/windows_nt/WebView2Loader.dll",
                        "/dev/webview/webview_java/natives/" + Platform.archTarget + "/windows_nt/webview.dll"
                };
                break;
            }

            default: {
                throw new IllegalStateException("Unsupported platform: " + Platform.osDistribution + ":" + Platform.archTarget);
            }
        }

        // Extract all of the libs.
        for (String lib : libraries) {
            File target = new File(new File(lib).getName());
            if (target.exists()) {
                target.delete();
            }
            target.deleteOnExit();

            // Copy it to a file.
            try (InputStream in = WebviewNative.class.getResourceAsStream(lib.toLowerCase())) {
                byte[] bytes = StreamUtil.toBytes(in);
                Files.write(target.toPath(), bytes);
            } catch (Exception e) {
                if (e.getMessage().contains("used by another")) continue; // Ignore.

                System.err.println("Unable to extract native: " + lib);
                throw e;
            }

            System.load(target.getAbsolutePath()); // Load it. This is so Native will be able to link it.
        }

        return Native.load("webview", WebviewNative.class);
    }

    static final int WV_HINT_NONE = 0;
    static final int WV_HINT_MIN = 1;
    static final int WV_HINT_MAX = 2;
    static final int WV_HINT_FIXED = 3;

    /**
     * Used in {@link webview_bind}
     */
    static interface BindCallback extends Callback {

        /**
         * @param seq The request id, used in {@link webview_return}
         * @param req The javascript arguments converted to a json array (string)
         * @param arg Unused
         */
        void callback(long seq, String req, long arg);

    }

    /**
     * Used in {@link webview_dispatch}
     */
    static interface DispatchCallback extends Callback {

        /**
         * @param $pointer The pointer of the webview
         * @param arg      Unused
         */
        void callback(long $pointer, long arg);

    }

    /**
     * Creates a new webview instance. If debug is true - developer tools will be
     * enabled (if the platform supports them). Window parameter can be a pointer to
     * the native window handle. If it's non-null - then child WebView is embedded
     * into the given parent window. Otherwise a new window is created. Depending on
     * the platform, a GtkWindow, NSWindow or HWND pointer can be passed here.
     * 
     * @param debug   Enables developer tools if true (if supported)
     * @param $window A pointer to a native window handle, for embedding the webview
     *                in a window. (Either a GtkWindow, NSWindow, or HWND pointer)
     */
    long webview_create(boolean debug, PointerByReference window);

    /**
     * @return            a native window handle pointer.
     * 
     * @param    $pointer The instance pointer of the webview
     * 
     * @implNote          This is either a pointer to a GtkWindow, NSWindow, or
     *                    HWND.
     */
    long webview_get_window(long $pointer);

    /**
     * Load raw HTML content onto the window.
     *
     * @param $pointer The instance pointer of the webview
     * @param html     The raw HTML string.
     */
    void webview_set_html(long $pointer, String html);

    /**
     * Navigates to the given URL.
     * 
     * @param $pointer The instance pointer of the webview
     * @param url      The target url, can be a data uri.
     */
    void webview_navigate(long $pointer, String url);

    /**
     * Sets the title of the webview window.
     * 
     * @param $pointer The instance pointer of the webview
     * @param title
     */
    void webview_set_title(long $pointer, String title);

    /**
     * Updates the webview's window size, see {@link WV_HINT_NONE},
     * {@link WV_HINT_MIN}, {@link WV_HINT_MAX}, and {@link WV_HINT_FIXED}
     * 
     * @param $pointer The instance pointer of the webview
     * @param width
     * @param height
     * @param hint
     */
    void webview_set_size(long $pointer, int width, int height, int hint);

    /**
     * Runs the main loop until it's terminated. You must destroy the webview after
     * this method returns.
     * 
     * @param $pointer The instance pointer of the webview
     */
    void webview_run(long $pointer);

    /**
     * Destroys a webview and closes the native window.
     * 
     * @param $pointer The instance pointer of the webview
     */
    void webview_destroy(long $pointer);

    /**
     * Stops the webview loop, which causes {@link #webview_run(long)} to return.
     * 
     * @param $pointer The instance pointer of the webview
     */
    void webview_terminate(long $pointer);

    /**
     * Evaluates arbitrary JavaScript code asynchronously.
     * 
     * @param $pointer The instance pointer of the webview
     * @param js       The script to execute
     */
    void webview_eval(long $pointer, @NonNull String js);

    /**
     * Injects JavaScript code at the initialization of the new page.
     * 
     * @implSpec          It is guaranteed to be called before window.onload.
     * 
     * @param    $pointer The instance pointer of the webview
     * @param    js       The script to execute
     */
    void webview_init(long $pointer, @NonNull String js);

    /**
     * Binds a native callback so that it will appear under the given name as a
     * global JavaScript function. Internally it uses webview_init().
     * 
     * @param $pointer The instance pointer of the webview
     * @param name     The name of the function to be exposed in Javascript
     * @param callback The callback to be called
     * @param arg      Unused
     */
    void webview_bind(long $pointer, @NonNull String name, @NonNull BindCallback callback, long arg);

    /**
     * Remove the native callback specified.
     * 
     * @param $pointer The instance pointer of the webview
     * @param name     The name of the callback
     */
    void webview_unbind(long $pointer, @NonNull String name);

    /**
     * Allows to return a value from the native binding. Original request pointer
     * must be provided to help internal RPC engine match requests with responses.
     * 
     * @param $pointer The instance pointer of the webview
     * @param name     The name of the callback
     * @param isError  Whether or not `result` should be thrown as an exception
     * @param result   The result (in json)
     */
    void webview_return(long $pointer, long seq, boolean isError, String result);

    /**
     * Dispatches the callback on the UI thread, only effective while
     * {@link #webview_run(long)} is blocking.
     * 
     * @param $pointer The instance pointer of the webview
     * @param callback The callback to be called
     * @param arg      Unused
     */
    void webview_dispatch(long $pointer, @NonNull DispatchCallback callback, long arg);

    /**
     * Returns the version info.
     */
    VersionInfoStruct webview_version();

    static class VersionInfoStruct extends Structure {
        public int major; // This is technically in a sub-struct.
        public int minor; // This is technically in a sub-struct.
        public int patch; // This is technically in a sub-struct.
        public byte[] version_number = new byte[32];
        public byte[] pre_release = new byte[48];
        public byte[] build_metadata = new byte[48];

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("major", "minor", "patch", "version_number", "pre_release", "build_metadata");
        }
    }

}
