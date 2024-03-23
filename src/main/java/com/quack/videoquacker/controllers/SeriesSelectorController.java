package com.quack.videoquacker.controllers;

import com.quack.videoquacker.MainApplication;
import com.quack.videoquacker.models.CopiedParameters;
import com.quack.videoquacker.utils.DataManager;
import com.quack.videoquacker.utils.IObservableListener;
import com.quack.videoquacker.utils.PropertiesManager;
import com.quack.videoquacker.utils.RessourceLocator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SeriesSelectorController implements IObservableListener<String> {
    private static final String LISTENER_KEY_CLIPBOARD = "clip";

    @Override
    public void onObservableChange(String key, String value) {
        if (!key.equals(LISTENER_KEY_CLIPBOARD)) return;
        if (DataManager.isValidJSON(value)) {
            CopiedParameters parameters = CopiedParameters.fromJsonString(value);
            int matchCount = 0;
            int tvRow = -1;
            boolean perfectMatch = false;

            //Check if the series name contain the copied data
            for (Map.Entry<String, TreeItem<String>> series : this.seriesMapByName.entrySet()) {
                if (series.getKey().trim().toUpperCase().contains(parameters.getSname().trim().toUpperCase())) {
                    matchCount++;
                    tvRow = this.tvArbo.getRow(series.getValue());
                }
            }

            if (matchCount == 1) {
                this.tvArbo.getSelectionModel().select(tvRow);
                return;
            }

            //Check if there is a perfect match between copied data and series or if not check if there is one where the copied data contains the name of the series
            matchCount = 0;
            for (Map.Entry<String, TreeItem<String>> series : this.seriesMapByName.entrySet()) {
                if (parameters.getSname().trim().toUpperCase().contains(series.getKey().trim().toUpperCase())) {
                    matchCount++;
                    tvRow = this.tvArbo.getRow(series.getValue());
                    if (parameters.getSname().trim().toUpperCase().equals(series.getKey().trim().toUpperCase())) {
                        perfectMatch = true;
                        break;
                    }
                }
            }
            if (matchCount == 1 || perfectMatch) {
                this.tvArbo.getSelectionModel().select(tvRow);
                return;
            }

            //Else the serie is unknown, should ask if need to create a folder for it
            Platform.runLater(() -> {
                this.tfSerieName.setText(parameters.getSname());
                this.refreshList(this.tiRoot, this.seriesPath, LIST_REFRESH_TYPE.TYPE_FOLDER);
                ButtonType btnTestFolder = new ButtonType("Create a test folder", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnFolder = new ButtonType("Create a normal folder", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnCancel = new ButtonType("Cancel and do nothing", ButtonBar.ButtonData.CANCEL_CLOSE);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to create a test folder for it ?", btnTestFolder, btnFolder, btnCancel);
                alert.setHeaderText("New serie detected '" + parameters.getSname().trim() + "'");
                alert.setTitle("New serie detected");
                alert.showAndWait();

                if (alert.getResult() == btnFolder || alert.getResult() == btnTestFolder) {
                    new File(this.seriesPath, parameters.getSname().trim()).mkdirs();
                    if (alert.getResult() == btnTestFolder) {
                        //TODO tag the folder as a test one
                    }
                    this.refreshList(this.tiRoot, this.seriesPath, LIST_REFRESH_TYPE.TYPE_FOLDER);
                    this.tvArbo.getSelectionModel().select(this.tvArbo.getRow(this.seriesMapByName.get(parameters.getSname().trim())));
                }
            });
        }
    }

    private enum LIST_REFRESH_TYPE {
        TYPE_FOLDER,
        TYPE_VIDEO_FILES
    }

    @FXML
    public TextField tfSerieName;
    @FXML
    public Button btnNewSerie;
    @FXML
    public TreeItem<String> tiRoot;
    @FXML
    public TreeView tvArbo;

    private File seriesPath;
    private Map<String, TreeItem<String>> seriesMapByName = new HashMap<>();
    private final FileNameMap fileNameMap = URLConnection.getFileNameMap();


    @FXML
    public void initialize() {
        this.seriesPath = new File(PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.series_path));
        this.tvArbo.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            SeriesSelectorController.this.onItemSelected((TreeItem<String>) newValue);
        });
        this.tiRoot.setGraphic(new ImageView(new Image(RessourceLocator.getResString("icons/goku_icon.jpg"), 20, 20, true, true)));
        this.refreshList(this.tiRoot, this.seriesPath, LIST_REFRESH_TYPE.TYPE_FOLDER);
        MainApplication.getClipboard().registerListener(LISTENER_KEY_CLIPBOARD, this);
    }

    @FXML
    public void onCreateSerie() {
        File path = this.seriesPath;
        new File(path, this.tfSerieName.getText().trim()).mkdirs();
        this.tfSerieName.setText("");
        refreshList(this.tiRoot, this.seriesPath, LIST_REFRESH_TYPE.TYPE_FOLDER);
    }

    public void onSearchChanged() {
        this.refreshList(this.tiRoot, this.seriesPath, LIST_REFRESH_TYPE.TYPE_FOLDER);
    }


    public void refreshList(TreeItem<String> source, File pathToScan, LIST_REFRESH_TYPE type) {
        source.getChildren().clear();
        if (type == LIST_REFRESH_TYPE.TYPE_FOLDER) this.seriesMapByName.clear();
        for (File f : pathToScan.listFiles((dir, name) -> name.toUpperCase().trim().contains(SeriesSelectorController.this.tfSerieName.getText().toUpperCase().trim()))) {
            switch (type) {
                case TYPE_FOLDER:
                    if (!(f.isDirectory() && f.getName().contains(this.tfSerieName.getText()))) {
                        continue;
                    }
                    break;
                case TYPE_VIDEO_FILES:

                    if (!(f.isFile() && f.getName().contains(this.tfSerieName.getText()) && !f.getName().equals("current.properties") && (this.fileNameMap.getContentTypeFor(f.getName())).startsWith("video"))) {
                        continue;
                    }
                    break;

            }
            TreeItem<String> item = new TreeItem<>();
            item.setValue(f.getName());
            item.setGraphic(new ImageView(new Image(RessourceLocator.getResString("icons/folder_icon_" + ThreadLocalRandom.current().nextInt(1, 61) + ".png"), 25, 25, true, true)));
            source.getChildren().add(item);
            if (type == LIST_REFRESH_TYPE.TYPE_FOLDER) this.seriesMapByName.put(f.getName(), item);
        }
    }

    public void onItemSelected(TreeItem<String> item) {
        if (item != null && item.getParent() == this.tiRoot) {
            // A series folder
            File selectedSeriePath = new File(this.seriesPath, item.getValue());
            this.refreshList(item, selectedSeriePath, LIST_REFRESH_TYPE.TYPE_VIDEO_FILES);
            MainWindowController.instance.downloadFormController.onSerieSelected(selectedSeriePath);
        } else {
            MainWindowController.instance.downloadFormController.onSerieSelected(null);
        }
    }


}
