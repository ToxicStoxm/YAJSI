module YAJSI {
    requires lombok;
    requires org.jetbrains.annotations;
    requires org.yaml.snakeyaml;
    requires io.github.classgraph;
    requires java.logging;
    requires jdk.compiler;

    exports com.toxicstoxm.YAJSI.api.file;
    exports com.toxicstoxm.YAJSI.api.settings;
    exports com.toxicstoxm.YAJSI.api.yaml.util;
    exports com.toxicstoxm.YAJSI.api.yaml.serialization;
    exports com.toxicstoxm.YAJSI.api.yaml;
    exports com.toxicstoxm.YAJSI.api.logging;
}