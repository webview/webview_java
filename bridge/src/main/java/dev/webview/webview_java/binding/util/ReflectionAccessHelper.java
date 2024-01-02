package dev.webview.webview_java.binding.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import lombok.SneakyThrows;

public class ReflectionAccessHelper {
    private static Field modifiersField;

    static {
        try {
            modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                getDeclaredFields0.setAccessible(true);

                Field[] allFields = (Field[]) getDeclaredFields0.invoke(Field.class, false);

                for (Field field : allFields) {
                    if (field.getName().equals("modifiers")) {
                        modifiersField = field;
                        modifiersField.setAccessible(true);
                        break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (modifiersField == null) {
            System.out.println("No modifiers field");
        }
    }

    @SneakyThrows
    public static void removeFinal(Field field) {
        int mods = field.getModifiers();

        if (Modifier.isFinal(mods)) {
            modifiersField.set(field, mods & ~Modifier.FINAL);
        }
    }

    public static void makeAccessible(Field field) {
        removeFinal(field);
        field.setAccessible(true);
    }

    public static void makeAccessible(Method method) {
        method.setAccessible(true);
    }

}
