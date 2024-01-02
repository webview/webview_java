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

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.Closeable;
import java.util.function.Consumer;

import co.casterlabs.commons.platform.OSDistribution;
import co.casterlabs.commons.platform.Platform;
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

        this.webview.setFixedSize(width, height);
    }

    @Override
    public void close() {
        this.webview.close();
        this.initialized = false;
        this.webview = null;
    }

}
