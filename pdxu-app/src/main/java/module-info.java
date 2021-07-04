import com.crschnick.pdxu.app.editor.adapter.*;

module com.crschnick.pdxu.app {
    uses com.crschnick.pdxu.app.editor.adapter.EditorSavegameAdapter;
    provides EditorSavegameAdapter with Eu4SavegameAdapter, Ck3SavegameAdapter, Hoi4SavegameAdapter, StellarisSavegameAdapter, Ck2SavegameAdapter, Vic2SavegameAdapter;
    requires com.realityinteractive.imageio.tga;
    requires org.apache.commons.lang;
    requires org.apache.commons.io;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires net.nikr.dds;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.media;
    requires org.jnativehook;
    requires org.kordamp.iconli.core;
    requires org.kordamp.ikonli.javafx;
    requires com.jfoenix;
    requires com.google.gson;
    requires org.slf4j;
    requires org.slf4j.simple;
    requires io.sentry;
    requires org.apache.commons.collections4;
    requires java.desktop;
    requires java.net.http;
    requires com.crschnick.pdxu.model;
    requires com.crschnick.pdxu.io;

    exports com.crschnick.pdxu.app to javafx.graphics;
}