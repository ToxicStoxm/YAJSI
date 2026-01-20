package com.toxicstoxm.YAJSI.upgrading;

public interface Version {
    Version getVersion();
    int compareTo(Version version);
    String toString();
    Version fromString(String versionString);
}
