module com.jvmart {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.bson;
    requires org.mongodb.driver.core;
    requires jbcrypt;
    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires java.prefs;
    requires mysql.connector.j;

    opens com.jvmart to javafx.fxml;
    opens com.jvmart.controllers to javafx.fxml;
    opens com.jvmart.models to javafx.base;
    
    exports com.jvmart;
    exports com.jvmart.controllers;
    exports com.jvmart.models;
    exports com.jvmart.services;
    exports com.jvmart.dao.sql;
}