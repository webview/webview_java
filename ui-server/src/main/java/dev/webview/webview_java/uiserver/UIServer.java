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
package dev.webview.webview_java.uiserver;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.server.HttpListener;
import co.casterlabs.rhs.server.HttpResponse;
import co.casterlabs.rhs.server.HttpServer;
import co.casterlabs.rhs.server.HttpServerBuilder;
import co.casterlabs.rhs.session.HttpSession;
import co.casterlabs.rhs.session.WebsocketListener;
import co.casterlabs.rhs.session.WebsocketSession;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

/**
 * Serves files to the local machine, useful for bundling your own UI with your
 * Webview application.
 */
@Accessors(chain = true)
public class UIServer implements Closeable {
    private @Setter Function<HttpSession, HttpResponse> handler;

    private HttpServer server;

    private @Getter int port;
    private @Getter String localAddress;

    @SneakyThrows
    public UIServer() {
        String hostname = "localhost";

        // Find a random port.
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(hostname, 0), 1);
            this.port = serverSocket.getLocalPort();
        }

        this.localAddress = String.format("http://%s:%d", hostname, this.port);

        this.server = new HttpServerBuilder()
            .withHostname(hostname)
            .withPort(this.port)
            .build(new HttpListener() {
                @Override
                public @Nullable HttpResponse serveHttpSession(@NonNull HttpSession session) {
                    if (handler == null) {
                        return null;
                    } else {
                        return handler.apply(session);
                    }
                }

                @Override
                public @Nullable WebsocketListener serveWebsocketSession(@NonNull WebsocketSession session) {
                    return null; // Drop
                }
            });
    }

    public UIServer start() throws IOException {
        this.server.start();
        return this;
    }

    @Override
    public void close() throws IOException {
        this.server.stop();
    }

}
