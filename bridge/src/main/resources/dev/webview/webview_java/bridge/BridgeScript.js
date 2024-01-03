if (window.Bridge) return;

const __bridgeInternal = window.__bridgeInternal;
delete window.__bridgeInternal; // HIDE.

function randomId() {
    return Math.random().toString(28).replace(".", "");
}

let listeners = {};
const Bridge = {
    on(type, callback) {
        if (typeof type != "string" || !(callback instanceof Function)) {
            throw "`type` must be a string and `callback` must be a function.";
        }

        const callbackId = randomId();
        type = type.toLowerCase();

        const callbacks = listeners[type] || {};
        callbacks[callbackId] = callback;
        listeners[type] = callbacks;

        return () => {
            delete listeners[type][callbackId];
        };
    },

    once(type, callback) {
        const unregisterFunction = Bridge.on(type, (v) => {
            unregisterFunction();
            callback(v);
        });
        return unregisterFunction;
    },

    __internal: {
        listeners: listeners,

        sendMessageToJava(type, data) {
            return __bridgeInternal(type, data);
        },

        defineObject(path, id) {
            path = path.split(".");
            const propertyName = path.pop();

            let proxy;

            const object = {
                __stores: {
                    svelte(field) {
                        return {
                            subscribe(callback) {
                                return Bridge.__internal.mutate(id, field, callback);
                            },
                            set(val) {
                                proxy[field] = val;
                            }
                        };
                    }
                },

                __internal: {
                    id: id,

                    defineFunction(name) {
                        Object.defineProperty(object, name, {
                            value: function () {
                                return Bridge.__internal.invoke(id, name, Array.from(arguments));
                            }
                        });
                    },

                    defineProperty(name) {
                        Object.defineProperty(object, name, {
                            value: null,
                            writable: true,
                            configurable: true
                        });
                    }
                }
            };

            const handler = {
                get(obj, property) {
                    if (typeof obj[property] != "undefined" && obj[property] != null) {
                        return obj[property];
                    }

                    return Bridge.__internal.get(id, property);
                },
                set(obj, property, value) {
                    Bridge.__internal.set(id, property, value);
                    return value;
                }
            }

            Object.freeze(object.__stores);
            Object.freeze(object.__internal);
            proxy = new Proxy(object, handler);

            // Resolve the root object.
            let root = window;
            for (const part of path) {
                root = root[part];
            }

            root[propertyName] = proxy;
        },

        get(id, property) {
            return Bridge.__internal.sendMessageToJava("GET", { id, property });
        },

        set(id, property, newValue) {
            return Bridge.__internal.sendMessageToJava("SET", { id, property, newValue });
        },

        mutate(id, property, callback) {
            const listenerType = `__internal:mut:${id}:${property}`;
            const unregisterFunction = Bridge.on(listenerType, callback);
            Bridge.__internal.get(id, property).then(callback); // Initial set.
            return () => unregisterFunction;
        },

        invoke(id, func, arguments) {
            return Bridge.__internal.sendMessageToJava("INVOKE", { id, "function": func, arguments });
        },

        broadcast(type, data) {
            // Broadcast under a wildcard.
            const wildCardCallbacks = listeners["*"];
            if (wildCardCallbacks) {
                Object.values(wildCardCallbacks).forEach((callback) => {
                    try {
                        callback(type.toLowerCase(), data);
                    } catch (e) {
                        console.error("[Webview-Bridge]", "A listener produced an exception: ");
                        console.error(e);
                    }
                });
            }

            // Broadcast under type.
            const callbacks = listeners[type.toLowerCase()];
            if (callbacks) {
                Object.values(callbacks).forEach((callback) => {
                    try {
                        callback(data);
                    } catch (e) {
                        console.error("[Webview-Bridge]", "A listener produced an exception: ");
                        console.error(e);
                    }
                });
            }
        }
    }
};


Object.freeze(Bridge);
Object.freeze(Bridge.__internal);
Object.defineProperty(window, "Bridge", {
    value: Bridge,
    writable: false,
    configurable: false
});

console.log("[Webview-Bridge]", "Injected bridge script.");
Bridge.__internal.sendMessageToJava("INIT", {});
