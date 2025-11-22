module com.brb.fatdown {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;
    requires static lombok;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    opens com.brb.fatdown to javafx.fxml;
    exports com.brb.fatdown;
    exports com.brb.fatdown.controller;
    opens com.brb.fatdown.controller to javafx.fxml;
    exports com.brb.fatdown.service;
    opens com.brb.fatdown.model to com.fasterxml.jackson.databind;
}