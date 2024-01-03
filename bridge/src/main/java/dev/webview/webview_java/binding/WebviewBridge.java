package dev.webview.webview_java.binding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
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
            bridgeScript = StreamUtil.toString(WebviewBridge.class.getResourceAsStream("/dev/webview/webview_java/binding/BridgeScript.js"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Map<String, JavascriptObject> objects = new HashMap<>();
    Webview webview;

    public WebviewBridge(@NonNull Webview webview) {
        this.webview = webview;

        // TODO rework emission to be more direct.
        // The return values here WORK.

        this.webview.bind("__bridgeInternal", (rawArgs) -> {
            JsonArray args = Rson.DEFAULT.fromJson(rawArgs, JsonArray.class);

            String type = args.getString(0);
            JsonObject data = args.getObject(1);

            switch (type) {
                case "INIT": {
                    for (Map.Entry<String, JavascriptObject> entry : new ArrayList<>(this.objects.entrySet())) {
                        if (!entry.getKey().contains(".")) {
                            entry.getValue().init(entry.getKey(), this);
                        }
                    }

                    this.webview.eval("console.log('[Webview-Bridge]', 'Bridge init completed.');");
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

        this.webview.setInitScript(bridgeScript);
    }

    public void defineObject(@NonNull String name, @NonNull JavascriptObject obj) {
        obj.init(name, this);
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
