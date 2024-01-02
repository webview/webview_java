package dev.webview.webview_java.binding;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(FIELD)
public @interface JavascriptValue {

    String value() default "";

    boolean allowGet() default true;

    boolean allowSet() default true;

    boolean watchForMutate() default false;

}
