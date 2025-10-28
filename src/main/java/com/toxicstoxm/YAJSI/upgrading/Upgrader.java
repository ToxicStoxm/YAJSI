package com.toxicstoxm.YAJSI.upgrading;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Upgrader {
    Class<? extends VersionFactory<? extends Version>> factory();
    String base();
}
