package com.quack.videoquacker.controllers;

import com.quack.videoquacker.MainApplication;
import com.quack.videoquacker.models.CopiedParameters;
import com.quack.videoquacker.models.JobParameters;
import com.quack.videoquacker.utils.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
                ButtonType btnTestFolder = new ButtonType("Create in test folder", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnFolder = new ButtonType("Create a normal folder", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnCancel = new ButtonType("Cancel and do nothing", ButtonBar.ButtonData.CANCEL_CLOSE);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to create a test folder for it ?", btnTestFolder, btnFolder, btnCancel);
                alert.setHeaderText("New serie detected '" + parameters.getSname().trim() + "'");
                alert.setTitle("New serie detected");
                alert.showAndWait();

                if (alert.getResult() == btnFolder || alert.getResult() == btnTestFolder) {
                    String folderName = (alert.getResult() == btnTestFolder) ? "test" : parameters.getSname().trim();
                    this.tfSerieName.setText(folderName);

                    File destFolder = new File(this.seriesPath, folderName);
                    if (!destFolder.exists()) {
                        destFolder.mkdirs();
                    }

                    this.refreshList(this.tiRoot, this.seriesPath, LIST_REFRESH_TYPE.TYPE_FOLDER);

                    if ("test".equals(folderName)) {
                        File props = new File(destFolder, "current.properties");
                        new PropertiesManager(props.getAbsolutePath()).setProperty(
                                PropertiesManager.PropertiesKeys.name_pattern,
                                parameters.getSname() + " S1E{{epnum}}.mp4"
                        );
                    }

                    this.tvArbo.getSelectionModel().select(this.tvArbo.getRow(this.seriesMapByName.get(folderName)));
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
    private TreeItem<String> currentSelection = null;
    private final FileNameMap fileNameMap = URLConnection.getFileNameMap();
    private int disableNextSelectionEvent = 0;


    @FXML
    public void initialize() {
        this.seriesPath = new File(PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.series_path));
        this.tvArbo.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            SeriesSelectorController.this.onItemSelected((TreeItem<String>) newValue);
        });
        this.tiRoot.setGraphic(new ImageView(new Image(RessourceLocator.getResString("icons/goku_icon.jpg"), 20, 20, true, true)));
        this.tvArbo.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleRightClick);
        this.refreshList(this.tiRoot, this.seriesPath, LIST_REFRESH_TYPE.TYPE_FOLDER);
        MainApplication.getClipboard().registerListener(LISTENER_KEY_CLIPBOARD, this);
    }

    private void handleRightClick(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY && this.currentSelection != null) {
            if (this.currentSelection.getParent() == this.tiRoot) {
                //If a series folder is selected
                ContextMenu contextMenu = new ContextMenu();
                MenuItem showFolder = new MenuItem("Open Folder");
                showFolder.setOnAction((e) -> {
                    try {
                        new ProcessBuilder("explorer.exe", new File(this.seriesPath, this.currentSelection.getValue()).getAbsolutePath()).start();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });

                MenuItem deleteFolder = new MenuItem("Delete Folder");
                deleteFolder.setOnAction((e) -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, String.format("Do you want to delete the '%s' folder ?", this.currentSelection.getValue()), ButtonType.YES, ButtonType.NO);
                    alert.showAndWait();
                    if (alert.getResult() == ButtonType.YES) {
                        try {
                            FileUtils.deleteDirectory(new File(this.seriesPath, this.currentSelection.getValue()));
                            this.refreshList(this.tiRoot, this.seriesPath, LIST_REFRESH_TYPE.TYPE_FOLDER);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });
                contextMenu.getItems().add(showFolder);
                contextMenu.getItems().add(deleteFolder);
                this.tvArbo.setContextMenu(contextMenu);
            }

            if (this.currentSelection.getParent() == null) {
                MenuItem deleteAllFolders = new MenuItem("Delete All");
                deleteAllFolders.setOnAction((e) -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to delete the ALL folders ?", ButtonType.YES, ButtonType.NO);
                    alert.showAndWait();
                    if (alert.getResult() == ButtonType.YES) {
                        Alert alert2 = new Alert(Alert.AlertType.CONFIRMATION, "Sure of sure ? Like for real ?", ButtonType.YES, ButtonType.NO);
                        alert2.showAndWait();
                        if (alert2.getResult() == ButtonType.YES) {
                            for (File folder : this.seriesPath.listFiles()) {
                                if (folder.isDirectory()) {
                                    try {
                                        FileUtils.deleteDirectory(new File(this.seriesPath, this.currentSelection.getValue()));
                                        this.refreshList(this.tiRoot, this.seriesPath, LIST_REFRESH_TYPE.TYPE_FOLDER);
                                    } catch (IOException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                }
                            }
                            this.refreshList(this.tiRoot, this.seriesPath, LIST_REFRESH_TYPE.TYPE_FOLDER);
                        }
                    }
                });

                ContextMenu contextMenu = new ContextMenu();
                contextMenu.getItems().add(deleteAllFolders);
                this.tvArbo.setContextMenu(contextMenu);
            }

            if (this.currentSelection.getParent() != null && this.currentSelection.getParent() != this.tiRoot) {
                ContextMenu contextMenu = new ContextMenu();

                MenuItem showInFolder = new MenuItem("Show video in folder");
                showInFolder.setOnAction(actionEvent -> {
                    try {
                        new ProcessBuilder("explorer.exe",  "/select,", new File(new File(this.seriesPath, this.currentSelection.getParent().getValue()), this.currentSelection.getValue()).getAbsolutePath()).start();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                contextMenu.getItems().add(showInFolder);

                this.tvArbo.setContextMenu(contextMenu);
            }
        }
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

                    if (!(f.isFile() && f.getName().contains(this.tfSerieName.getText()) && !f.getName().equals("current.properties") && Optional.ofNullable(this.fileNameMap.getContentTypeFor(f.getName())).orElse("").startsWith("video"))) {
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
        this.currentSelection = item;
        if (this.disableNextSelectionEvent > 0) {
            this.disableNextSelectionEvent--;
            return;
        }

        if (item != null && item.getParent() == this.tiRoot) {
            // A series folder
            File selectedSeriePath = new File(this.seriesPath, item.getValue());
            this.refreshList(item, selectedSeriePath, LIST_REFRESH_TYPE.TYPE_VIDEO_FILES);
            MainWindowController.instance.downloadFormController.onSerieSelected(selectedSeriePath);
        } else {
            MainWindowController.instance.downloadFormController.onSerieSelected(null);
        }
    }


    public void onJobSelected(JobParameters jobParameters) {

        //Check if the series name contain the copied data
        for (Map.Entry<String, TreeItem<String>> series : this.seriesMapByName.entrySet()) {
            if (series.getKey().equals(jobParameters.getSeriesSelected().getName())) {
                this.disableNextSelectionEvent++;
                this.tvArbo.getSelectionModel().select(this.tvArbo.getRow(series.getValue()));
            }
        }
    }
}
