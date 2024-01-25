package dev.webview.webview_java.example.swing;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.Closeable;
import java.util.function.Consumer;

import co.casterlabs.commons.platform.OSDistribution;
import co.casterlabs.commons.platform.Platform;
import dev.webview.webview_java.Webview;
import lombok.Getter;
import lombok.Setter;

/**
 * An AWT component a which will automatically initialize the webview when it's
 * considered "drawable".
 * 
 */
public class AWTWebview extends Canvas implements Closeable {
    private static final long serialVersionUID = 5199512256429931156L;

    private @Getter Webview webview;
    private final boolean debug;

    private Dimension lastSize = null;

    /**
     * The callback handler for when the Webview gets created.
     */
    private @Setter Consumer<Webview> onInitialized;

    private @Getter boolean initialized = false;

    public AWTWebview() {
        this(false);
    }

    /**
     * @param debug Whether or not to allow the opening of inspect element/devtools.
     */
    public AWTWebview(boolean debug) {
        this.debug = debug;
    }

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

            // We need to create the webview off of the swing thread.

            Thread t = new Thread(() -> {
                this.webview = new Webview(this.debug, this);

                this.updateSize();

                if (this.onInitialized != null) {
                    this.onInitialized.accept(this.webview);
                }

                this.webview.run();
            });
            t.setDaemon(false);
            t.setName("AWTWebview RunAsync Thread - #" + this.hashCode());
            t.start();
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

        //this.webview.setFixedSize(width, height);
        this.webview.setMinSize(width, height);
        this.webview.setMaxSize(width, height);
    }

    @Override
    public void close() {
        this.webview.close();
        this.initialized = false;
        this.webview = null;
    }

}
