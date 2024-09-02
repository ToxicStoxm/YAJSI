module YAJSI {
    requires static lombok;
    requires org.jetbrains.annotations;
    requires org.yaml.snakeyaml;
    requires java.logging;
    exports com.toxicstoxm.YAJSI.api.file;
    exports com.toxicstoxm.YAJSI.api.settings;
}