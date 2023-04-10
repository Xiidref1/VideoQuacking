package org.duck.VideoQuacker.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.duck.VideoQuacker.utils.Callback;
import org.duck.VideoQuacker.utils.Properties;

import java.io.File;

public class Selector extends VBox {

    private ListView<Label> animeList;
    private TextField newFolder;

    public Selector(Callback<Selector, File> onItemSelectedCallback) {
        super();
        this.animeList = new ListView<>();
        this.newFolder = new TextField();
        this.newFolder.setPromptText("Create a new folder");
        Button createBtn = new Button("+");
        createBtn.setOnAction(actionEvent -> this.createNewFolder());

        HBox header = new HBox();
        header.getChildren().addAll(this.newFolder, createBtn);

        this.animeList.getSelectionModel().selectedItemProperty().addListener((observableValue, old, newVal) -> {
            onItemSelectedCallback.call(Selector.this, new File(Properties.get("baseFolderToScan"), newVal.getText()));
        });
        this.populateAnimeList();

        this.getChildren().addAll(header, this.animeList);
    }

    public void populateAnimeList() {
        File folderToScan = new File(Properties.get("baseFolderToScan"));

        for (File animeFolder : folderToScan.listFiles()) {
            if (animeFolder.isDirectory() && !animeFolder.getName().startsWith("_")) {
                this.animeList.getItems().add(new Label(animeFolder.getName()));
            }
        }
    }

    public void createNewFolder() {
        File folder = new File(Properties.get("baseFolderToScan"), this.newFolder.getText());
        if (this.newFolder.getText().isBlank() || folder.exists()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Folder name is empty or folder exist");
            alert.setHeaderText(null);
            alert.setContentText("Folder name is empty or folder exist");
            alert.show();
            return;
        }
        if(folder.mkdir()){
            this.animeList.getItems().add(new Label(this.newFolder.getText()));
        }
    }
}
