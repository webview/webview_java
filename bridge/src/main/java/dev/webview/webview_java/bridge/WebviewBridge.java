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
package dev.webview.webview_java.bridge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.io.streams.StreamUtil;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.element.JsonString;
import dev.webview.webview_java.Webview;
import lombok.NonNull;

public class WebviewBridge {
    private static String bridgeScript = "";
    static {
        try {
            bridgeScript = StreamUtil.toString(WebviewBridge.class.getResourceAsStream("/dev/webview/webview_java/bridge/BridgeScript.js"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Map<String, JavascriptObject> objects = new HashMap<>();
    Webview webview;

    public WebviewBridge(@NonNull Webview webview) {
        this.webview = webview;

        this.webview.bind("__bridgeInternal", (rawArgs) -> {
            JsonArray args = Rson.DEFAULT.fromJson(rawArgs, JsonArray.class);

            String type = args.getString(0);
            JsonObject data = args.getObject(1);

            switch (type) {
                case "INIT": {
                    this.webview.eval("console.log('[Webview-Bridge]', 'Bridge init completed.');");
                    // todo rebind objects
                    //this.rebuildInitScript();
                    //this.webview.executeOnloadAndInitScript();
                    return null;
                }

                case "GET": {
                    String id = data.getString("id");
                    String property = data.getString("property");
                    return this.processGet(id, property).toString();
                }

                case "SET": {
                    String id = data.getString("id");
                    String property = data.getString("property");
                    JsonElement newValue = data.get("newValue");
                    this.processSet(id, property, newValue);
                    return null;
                }

                case "INVOKE": {
                    String id = data.getString("id");
                    String function = data.getString("function");
                    JsonArray arguments = data.getArray("arguments");
                    return this.processInvoke(id, function, arguments).toString();
                }

                default:
                    throw new IllegalArgumentException("Unknown IPC message: " + rawArgs);
            }
        });

        this.rebuildInitScript();
    }

    private void rebuildInitScript() {
        List<String> linesToExecute = new LinkedList<>();
        linesToExecute.add(bridgeScript);

        for (Map.Entry<String, JavascriptObject> entry : new ArrayList<>(this.objects.entrySet())) {
            if (!entry.getKey().contains(".")) {
                linesToExecute.addAll(
                    entry.getValue().getInitLines(entry.getKey(), this)
                );
            }
        }

        this.webview.setInitScript(String.join("\n", linesToExecute));
    }

    public void defineObject(@NonNull String name, @NonNull JavascriptObject obj) {
        this.webview.eval(String.join("\n", obj.getInitLines(name, this)));
        this.rebuildInitScript();
    }

    public void emit(@NonNull String type, @NonNull JsonElement data) {
        this.webview.eval(
            String.format(
                "window.Bridge.__internal.broadcast(%s,%s);",
                new JsonString(type), data
            )
        );
    }

    private @Nullable JsonElement processGet(String id, String property) throws Throwable {
        for (JavascriptObject obj : this.objects.values()) {
            if (obj.getId().equals(id)) {
                return obj.get(property, this);
            }
        }
        return null;
    }

    private void processSet(String id, String property, JsonElement value) throws Throwable {
        for (JavascriptObject obj : this.objects.values()) {
            if (obj.getId().equals(id)) {
                obj.set(property, value, this);
                break;
            }
        }
    }

    private @Nullable JsonElement processInvoke(String id, String function, JsonArray args) throws Throwable {
        for (JavascriptObject obj : this.objects.values()) {
            if (obj.getId().equals(id)) {
                return obj.invoke(function, args, this);
            }
        }
        return null;
    }

}
