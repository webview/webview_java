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

import static dev.webview.webview_java.WebviewNative.N;
import static dev.webview.webview_java.WebviewNative.WV_HINT_FIXED;
import static dev.webview.webview_java.WebviewNative.WV_HINT_MAX;
import static dev.webview.webview_java.WebviewNative.WV_HINT_MIN;
import static dev.webview.webview_java.WebviewNative.WV_HINT_NONE;

import java.awt.Component;
import java.io.Closeable;

import org.jetbrains.annotations.Nullable;

import com.sun.jna.Native;
import com.sun.jna.ptr.PointerByReference;

import co.casterlabs.commons.platform.Platform;
import dev.webview.webview_java.WebviewNative.BindCallback;
import lombok.NonNull;

public class Webview implements Closeable, Runnable {

    @Deprecated
    public long $pointer;

    private String initScript = "";

    /**
     * Creates a new Webview.
     * 
     * @param debug Enables devtools/inspect element if true.
     */
    public Webview(boolean debug) {
        this(debug, (PointerByReference) null);
    }
    /**
     * Creates a new Webview.
     *
     * @param debug Enables devtools/inspect element if true.
     * @param width preset - width
     * @param height preset - height
     */
    public Webview(boolean debug, int width, int height) {
        this(debug, NULL_PTR, width, height);
    }

    /**
     * Creates a new Webview.
     * 
     * @param debug  Enables devtools/inspect element if true.
     * 
     * @param target The target awt component, such as a {@link java.awt.JFrame} or
     *               {@link java.awt.Canvas}. Must be "drawable".
     */
    public Webview(boolean debug, @NonNull Component target) {
        this(debug, new PointerByReference(Native.getComponentPointer(target)));
    }

    /**
     * @deprecated Use this only if you absolutely know what you're doing.
     */
    @Deprecated
    public Webview(boolean debug, @Nullable PointerByReference windowPointer) {
        this(debug, windowPointer, 800, 600);
    }

    /**
     * @deprecated Use this only if you absolutely know what you're doing.
     */
    @Deprecated
    public Webview(boolean debug, @Nullable PointerByReference windowPointer, int width, int height) {
        $pointer = N.webview_create(debug, windowPointer);

        this.loadURL(null);
        this.setSize(width, height);
    }

    /**
     * @deprecated Use this only if you absolutely know what you're doing.
     */
    @Deprecated
    public long getNativeWindowPointer() {
        return N.webview_get_window($pointer);
    }

    public void setHTML(@Nullable String html) {
        N.webview_set_html($pointer, html);
        this.eval(this.initScript);
    }

    public void loadURL(@Nullable String url) {
        if (url == null) {
            url = "about:blank";
        }

        N.webview_navigate($pointer, url);
        this.eval(this.initScript);
    }

    public void setTitle(@NonNull String title) {
        N.webview_set_title($pointer, title);
    }

    public void setMinSize(int width, int height) {
        N.webview_set_size($pointer, width, height, WV_HINT_MIN);
    }

    public void setMaxSize(int width, int height) {
        N.webview_set_size($pointer, width, height, WV_HINT_MAX);
    }

    public void setSize(int width, int height) {
        N.webview_set_size($pointer, width, height, WV_HINT_NONE);
    }

    public void setFixedSize(int width, int height) {
        N.webview_set_size($pointer, width, height, WV_HINT_FIXED);
    }

    /**
     * Sets the script to be run on page load.
     * 
     * @implNote        This get's called AFTER window.load.
     * 
     * @param    script
     */
    public void setInitScript(@NonNull String script) {
        script = String.format(
            "(async () => {"
                + "try {"
                + "%s"
                + "} catch (e) {"
                + "console.error('[Webview]', 'An error occurred whilst evaluating init script:', %s, e);"
                + "}"
                + "})();",
            script,
            '"' + _WebviewUtil.jsonEscape(script) + '"'
        );

        N.webview_init($pointer, script);
        this.initScript = script;
    }

    /**
     * Executes the given script NOW.
     * 
     * @param script
     */
    public void eval(@NonNull String script) {
        this.dispatch(() -> {
            N.webview_eval(
                $pointer,
                String.format(
                    "(async () => {"
                        + "try {"
                        + "%s"
                        + "} catch (e) {"
                        + "console.error('[Webview]', 'An error occurred whilst evaluating script:', %s, e);"
                        + "}"
                        + "})();",
                    script,
                    '"' + _WebviewUtil.jsonEscape(script) + '"'
                )
            );
        });
    }

    /**
     * Binds a function to the JavaScript environment on page load.
     * 
     * @implNote         This get's called AFTER window.load.
     * 
     * @implSpec         After calling the function in JavaScript you will get a
     *                   Promise instead of the value. This is to prevent you from
     *                   locking up the browser while waiting on your Java code to
     *                   execute and generate a return value.
     * 
     * @param    name    The name to be used for the function, e.g "foo" to get
     *                   foo().
     * @param    handler The callback handler, accepts a JsonArray (which are all
     *                   arguments passed to the function()) and returns a value
     *                   which is of type JsonElement (can be null). Exceptions are
     *                   automatically passed back to JavaScript.
     */
    public void bind(@NonNull String name, @NonNull WebviewBindCallback handler) {
        N.webview_bind($pointer, name, new BindCallback() {
            @Override
            public void callback(long seq, String req, long arg) {
                try {
                    String result = handler.apply(req);
                    if (result == null) {
                        result = "null";
                    }

                    N.webview_return($pointer, seq, false, result);
                } catch (Throwable e) {
                    e.printStackTrace();

                    String exceptionJson = '"' + _WebviewUtil.jsonEscape(_WebviewUtil.getExceptionStack(e)) + '"';

                    N.webview_return($pointer, seq, true, exceptionJson);
                }
            }
        }, 0);
    }

    /**
     * Unbinds a function, removing it from future pages.
     * 
     * @param name The name of the function.
     */
    public void unbind(@NonNull String name) {
        N.webview_unbind($pointer, name);
    }

    /**
     * Executes an event on the event thread.
     * 
     * @deprecated Use this only if you absolutely know what you're doing.
     */
    @Deprecated
    public void dispatch(@NonNull Runnable handler) {
        N.webview_dispatch($pointer, ($pointer, arg) -> {
            handler.run();
        }, 0);
    }

    /**
     * Executes the webview event loop until the user presses "X" on the window.
     * 
     * @see #close()
     */
    @Override
    public void run() {
        N.webview_run($pointer);
        N.webview_destroy($pointer);
    }

    /**
     * Executes the webview event loop asynchronously until the user presses "X" on
     * the window.
     * 
     * @see #close()
     */
    public void runAsync() {
        Thread t = new Thread(this);
        t.setDaemon(false);
        t.setName("Webview RunAsync Thread - #" + this.hashCode());
        t.start();
    }

    /**
     * Closes the webview, call this to end the event loop and free up resources.
     */
    @Override
    public void close() {
        N.webview_terminate($pointer);
    }

    public void setDarkAppearance(boolean shouldAppearDark) {
        switch (Platform.osFamily) {
            case WINDOWS:
                _WindowsHelper.setWindowAppearance(this, shouldAppearDark);
                break;

            default: // NOOP
                break;
        }
    }

    public static String getVersion() {
        byte[] bytes = N.webview_version().version_number;
        int length = 0;
        for (byte b : bytes) {
            if (b == 0) {
                break;
            }
            length++;
        }
        return new String(bytes, 0, length);
    }

}
