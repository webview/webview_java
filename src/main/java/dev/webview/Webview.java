package dev.webview;

import static dev.webview.WebviewNative.NULL_PTR;
import static dev.webview.WebviewNative.WV_HINT_FIXED;
import static dev.webview.WebviewNative.WV_HINT_MAX;
import static dev.webview.WebviewNative.WV_HINT_MIN;
import static dev.webview.WebviewNative.WV_HINT_NONE;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.Closeable;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.sun.jna.Native;
import com.sun.jna.ptr.PointerByReference;

import co.casterlabs.commons.platform.OSDistribution;
import co.casterlabs.commons.platform.Platform;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import dev.webview.WebviewNative.BindCallback;
import lombok.NonNull;

public class Webview implements Closeable, Runnable {
    private static final WebviewNative N;

    @Deprecated
    public long $pointer;

    static {
        // Extract & load the natives.
        WebviewNative.runSetup();
        N = Native.load("webview", WebviewNative.class);
    }

    /**
     * Creates a Swing which will automatically initialize the webview when it's
     * considered "drawable".
     * 
     * @param  debug    Whether or not to allow the opening of inspect
     *                  element/devtools.
     * @param  onCreate The callback handler for when the webview gets created.
     * 
     * @return          A component for direct embedding in Swing.
     */
    public static Component createAWT(boolean debug, @NonNull Consumer<Webview> onCreate) {
        return new Canvas() {
            private static final long serialVersionUID = 5199512256429931156L;

            private boolean initialized = false;

            private Webview webview;
            private Dimension lastSize = null;

            @Override
            public void paint(Graphics g) {
                Dimension size = this.getSize();

                if (!size.equals(this.lastSize)) {
                    this.lastSize = size;

                    if (this.webview != null) {
                        this.updateSize();
                    }
                }

                if (!this.initialized) {
                    this.initialized = true;

                    new Thread(() -> {
                        this.webview = new Webview(debug, this);

                        this.updateSize();

                        onCreate.accept(this.webview);
                    }).start();
                }
            }

            private void updateSize() {
                int width = this.lastSize.width;
                int height = this.lastSize.height;

                // There is a random margin on Windows that isn't visible, so we must
                // compensate.
                // TODO figure out why this is caused.
                if (Platform.osDistribution == OSDistribution.WINDOWS_NT) {
                    width -= 16;
                    height -= 39;
                }

                this.webview.setFixedSize(width, height);
            }

        };
    }

    /**
     * Creates a new Webview.
     * 
     * @param debug Enables devtools/inspect element if true.
     */
    public Webview(boolean debug) {
        this(debug, NULL_PTR);
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
        $pointer = N.webview_create(debug, windowPointer);

        this.loadURL(null);
        this.setSize(800, 600);
    }

    /**
     * @deprecated Use this only if you absolutely know what you're doing.
     */
    @Deprecated
    public long getNativeWindowPointer() {
        return N.webview_get_window($pointer);
    }

    public void loadURL(@Nullable String url) {
        if (url == null) {
            url = "about:blank";
        }

        N.webview_navigate($pointer, url);
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
        N.webview_init($pointer, script);
    }

    /**
     * Executes the given script NOW.
     * 
     * @param script
     */
    public void eval(@NonNull String script) {
        N.webview_eval($pointer, script);
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
    public void bind(@NonNull String name, @NonNull Function<JsonArray, JsonElement> handler) {
        N.webview_bind($pointer, name, new BindCallback() {
            @Override
            public void callback(long seq, String req, long arg) {
                try {
                    JsonArray arguments = Rson.DEFAULT.fromJson(req, JsonArray.class);

                    try {
                        @Nullable
                        JsonElement result = handler.apply(arguments);

                        N.webview_return($pointer, seq, false, Rson.DEFAULT.toJsonString(result));
                    } catch (Exception e) {
                        N.webview_return($pointer, seq, true, e.getMessage());
                    }
                } catch (JsonParseException e) {
                    e.printStackTrace();
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
     * Closes the webview, call this to end the event loop and free up resources.
     */
    @Override
    public void close() {
        N.webview_terminate($pointer);
    }

}
