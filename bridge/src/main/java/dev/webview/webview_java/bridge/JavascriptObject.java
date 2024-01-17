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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonString;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import dev.webview.webview_java.bridge.util.ReflectionFieldMutationListener;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * Allows you to expose a Java object to Javascript
 * 
 * @see JavascriptFunction
 * @see JavascriptGetter
 * @see JavascriptSetter
 * @see JavascriptValue
 */
public abstract class JavascriptObject {
    private @Getter String id = UUID.randomUUID().toString();

    private Map<String, FieldMapping> properties = new HashMap<>();
    private Map<String, MethodMapping> functions = new HashMap<>();
    private Map<String, Field> subObjects = new HashMap<>();

    private WebviewBridge bridge = null;
    @SuppressWarnings("unused")
    private String name;

    @SneakyThrows
    public JavascriptObject() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            if (JavascriptObject.class.isAssignableFrom(field.getType())) {
                this.subObjects.put(
                    field.getName(),
                    field
                );
            } else if (field.isAnnotationPresent(JavascriptValue.class)) {
                JavascriptValue annotation = field.getAnnotation(JavascriptValue.class);
                String name = annotation.value().isEmpty() ? field.getName() : annotation.value();

                FieldMapping mapping = new FieldMapping(this, name);

                mapping.value = field;
                mapping.valueAnnotation = annotation;

                this.properties.put(name, mapping);

                if (annotation.watchForMutate()) {
                    new ReflectionFieldMutationListener(field, this)
                        .onMutate((value) -> {
                            if (this.bridge == null) return;
                            this.bridge.emit(
                                String.format("__internal:mut:%s:%s", this.id, name),
                                Rson.DEFAULT.toJson(value)
                            );
                        });
                }
            }
        }

        for (Method method : this.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) continue;

            if (method.isAnnotationPresent(JavascriptFunction.class)) {
                JavascriptFunction annotation = method.getAnnotation(JavascriptFunction.class);
                String name = annotation.value().isEmpty() ? method.getName() : annotation.value();

                this.functions.put(name, new MethodMapping(this, method));
            } else if (method.isAnnotationPresent(JavascriptGetter.class)) {
                JavascriptGetter annotation = method.getAnnotation(JavascriptGetter.class);
                String name = annotation.value().isEmpty() ? method.getName() : annotation.value();

                FieldMapping mapping = this.properties.get(name);

                if (mapping == null) {
                    mapping = new FieldMapping(this, name);
                    this.properties.put(name, mapping);
                }

                mapping.getter = method;
            } else if (method.isAnnotationPresent(JavascriptSetter.class)) {
                JavascriptSetter annotation = method.getAnnotation(JavascriptSetter.class);
                String name = annotation.value().isEmpty() ? method.getName() : annotation.value();

                FieldMapping mapping = this.properties.get(name);

                if (mapping == null) {
                    mapping = new FieldMapping(this, name);
                    this.properties.put(name, mapping);
                }

                mapping.setter = method;
            }
        }
    }

    List<String> getInitLines(String name, WebviewBridge bridge) {
        return this.getInitLines(name, bridge, null);
    }

    @SneakyThrows
    private List<String> getInitLines(String name, WebviewBridge bridge, @Nullable JavascriptObject parent) {
        this.bridge = bridge;
        this.name = name;

        bridge.objects.put(name, this);

        List<String> linesToExecute = new LinkedList<>();

        linesToExecute.add(
            String.format("window.Bridge.__internal.defineObject(%s,%s);", new JsonString(name), new JsonString(this.id))
        );

        for (String functionName : this.functions.keySet()) {
            linesToExecute.add(
                // We directly access the property without `[]` for subobject support.
                String.format("window.%s.__internal.defineFunction(%s,%s);", name, new JsonString(functionName), new JsonString(this.id))
            );
        }

        for (String propertyName : this.properties.keySet()) {
            linesToExecute.add(
                // We directly access the property without `[]` for subobject support.
                String.format("window.%s.__internal.defineProperty(%s);", name, new JsonString(propertyName))
            );
        }

        for (Map.Entry<String, Field> entry : this.subObjects.entrySet()) {
            JavascriptObject value = (JavascriptObject) entry.getValue().get(this);

            if ((value != null) && (value != parent)) {
                linesToExecute.addAll(
                    value.getInitLines(name + "." + entry.getKey(), bridge, this)
                );
            }
        }

        return linesToExecute;
    }

    @Nullable
    JsonElement get(@NonNull String property, @NonNull WebviewBridge bridge) throws Throwable {
        try {
            FieldMapping mapping = this.properties.get(property);
            assert mapping != null : "Could not find property: " + property;

            return mapping.get();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    void set(@NonNull String property, JsonElement value, @NonNull WebviewBridge bridge) throws Throwable {
        try {
            FieldMapping mapping = this.properties.get(property);
            assert mapping != null : "Could not find property: " + property;

            mapping.set(value, bridge);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Nullable
    JsonElement invoke(@NonNull String function, @NonNull JsonArray arguments, @NonNull WebviewBridge bridge) throws Throwable {
        try {
            MethodMapping mapping = this.functions.get(function);
            assert mapping != null : "Could not find function: " + function;

            return mapping.invoke(arguments, bridge);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static class MethodMapping {
        private Object $i;

        private Method method;

        public MethodMapping(Object i, Method method) {
            this.$i = i;
            this.method = method;
        }

        public @Nullable JsonElement invoke(@NonNull JsonArray arguments, @NonNull WebviewBridge bridge) throws Exception {
            Class<?>[] argTypes = method.getParameterTypes();
            assert argTypes.length == arguments.size() : "The invoking arguments do not match the expected length: " + argTypes.length;

            Object[] args = new Object[argTypes.length];

            for (int i = 0; i < args.length; i++) {
                try {
                    args[i] = Rson.DEFAULT.fromJson(arguments.get(i), argTypes[i]);
                } catch (JsonParseException e) {
                    throw new IllegalArgumentException("The provided argument " + arguments.get(i) + " could not be converted to " + argTypes[i].getCanonicalName());
                }
            }

            Object result = this.method.invoke($i, args);

            return Rson.DEFAULT.toJson(result);
        }

    }

    private static class FieldMapping {
        private @NonNull Object $i;
        private @NonNull String $name;

        private Method getter;
        private Method setter;
        private Field value;
        private JavascriptValue valueAnnotation;

        public FieldMapping(Object i, String name) {
            this.$i = i;
            this.$name = name;
        }

        public void set(@NonNull JsonElement v, @NonNull WebviewBridge bridge) throws Exception {
            if (this.setter != null) {
                Object o = null;

                if (!v.isJsonNull()) {
                    Class<?> type = this.setter.getParameterTypes()[0];
                    o = Rson.DEFAULT.fromJson(v, type);
                }

                this.setter.invoke($i, o);
            } else {
                if (this.valueAnnotation.allowSet()) {
                    Object o = null;

                    if (!v.isJsonNull()) {
                        Class<?> type = this.value.getType();
                        o = Rson.DEFAULT.fromJson(v, type);
                    }

                    this.value.set($i, o);
                } else {
                    throw new UnsupportedOperationException("SET is not allowed for the field: " + $name);
                }
            }
        }

        public @Nullable JsonElement get() throws Exception {
            Object result;

            if (this.getter != null) {
                result = this.getter.invoke($i);
            } else {
                if (this.valueAnnotation.allowGet()) {

                    result = this.value.get($i);
                } else {
                    throw new UnsupportedOperationException("GET is not allowed for the field: " + $name);
                }
            }

            return Rson.DEFAULT.toJson(result);
        }

    }

}
