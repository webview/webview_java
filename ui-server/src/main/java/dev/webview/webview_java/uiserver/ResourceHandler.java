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

import java.io.InputStream;
import java.util.function.Function;

import co.casterlabs.rhs.protocol.StandardHttpStatus;
import co.casterlabs.rhs.server.HttpResponse;
import co.casterlabs.rhs.session.HttpSession;
import co.casterlabs.rhs.util.MimeTypes;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * A handler that serves UI files out of your jar.
 */
@AllArgsConstructor
public class ResourceHandler implements Function<HttpSession, HttpResponse> {
    private final @NonNull String resourceBase;

    public ResourceHandler() {
        this("");
    }

    @Override
    public HttpResponse apply(HttpSession session) {
        String path = this.resourceBase + session.getUri();
        String mimeType = MimeTypes.getMimeForType(path.substring(path.lastIndexOf('.') + 1));

        InputStream in = ResourceHandler.class.getResourceAsStream(path);

        if (in == null) {
            return HttpResponse
                .newFixedLengthResponse(StandardHttpStatus.NOT_FOUND);
        } else {
            return HttpResponse
                .newChunkedResponse(StandardHttpStatus.OK, in)
                .setMimeType(mimeType);
        }
    }

}
