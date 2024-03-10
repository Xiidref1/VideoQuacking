package com.quack.videoquacker.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;

public class MainWindowController {
    public static MainWindowController instance;

    @FXML
    public SeriesSelectorController seriesSelectorController;
    @FXML
    public DownloadFormController downloadFormController;
    @FXML
    public CurrentJobsController currentJobsController;
    @FXML
    public BorderPane rootBox;

    @FXML
    public void initialize() {
        BorderPane.setMargin(rootBox.getCenter(), new Insets(50));
        MainWindowController.instance = this;
    }
}
