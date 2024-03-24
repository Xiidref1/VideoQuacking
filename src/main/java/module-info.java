module com.quack.videoquacker {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.desktop;
    requires org.apache.commons.validator;
    requires org.apache.commons.lang3;
    requires org.apache.commons.io;
    requires org.json;
    requires static lombok;
    requires com.google.gson;

    opens com.quack.videoquacker to javafx.fxml;
    exports com.quack.videoquacker;
    exports com.quack.videoquacker.controllers;
    exports com.quack.videoquacker.controllers.jobs;
    opens com.quack.videoquacker.controllers to javafx.fxml;
    opens com.quack.videoquacker.models to com.google.gson;
}