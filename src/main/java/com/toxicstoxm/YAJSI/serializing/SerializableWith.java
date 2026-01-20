package com.toxicstoxm.YAJSI.serializing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SerializableWith {
    Class<? extends ExternalYAMLSerializer<?>> serializer();
}
