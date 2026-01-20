package com.toxicstoxm.YAJSI.upgrading;

public interface VersionFactory<T extends Version> {
    T fromString(String versionString);
}
