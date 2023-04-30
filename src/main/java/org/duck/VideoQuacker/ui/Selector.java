package org.duck.VideoQuacker.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.checkerframework.checker.units.qual.C;
import org.duck.VideoQuacker.enums.PropertiesKeyEnum;
import org.duck.VideoQuacker.utils.Callback;
import org.duck.VideoQuacker.utils.Properties;

import java.io.File;
import java.util.Optional;

public class Selector extends VBox {

    private TreeView<File> animeList;
    private TreeItem<File> root = new TreeItem<>(new File("Video Folders"));
    private TextField newFolder;
    private final File folderToScan = new File(Properties.get("baseFolderToScan"));

    public Selector(Callback<Selector, File> onItemSelectedCallback) {
        super();
        this.animeList = new TreeView<>();
        this.animeList.setCellFactory(new javafx.util.Callback<>() {
            @Override
            public TreeCell<File> call(TreeView<File> stringTreeView) {


                TreeCell<File> cell = new TreeCell<>() {
                    @Override
                    protected void updateItem(File f, boolean b) {
                        super.updateItem(f, b);


                        if (f != null) {
                            setText(f.getName());
                            setTextFill(Color.BLACK);
                            if (f.isDirectory()) {

                                File confFile = new File(f, ".config");
                                if (confFile.exists()) {
                                    switch (Properties.getFromCustom(confFile, PropertiesKeyEnum.STATUS.name(), "")) {
                                        case "":
                                            setTextFill(Color.BLACK);
                                            break;
                                        case "archived":
                                            setTextFill(Color.YELLOW);
                                            break;
                                    }
                                }
                            }
                        }
                    }
                };

                return cell;
            }
        });
        this.animeList.setRoot(this.root);
        this.root.setExpanded(true);
        this.newFolder = new TextField();
        this.newFolder.setPromptText("Create a new folder");
        Button createBtn = new Button("+");
        createBtn.setOnAction(actionEvent -> this.createNewFolder());

        Button reloadBtn = new Button("reload");
        reloadBtn.setOnAction(actionEvent -> this.populateAnimeList());
        Button deleteBtn = new Button("delete");
        deleteBtn.setOnAction(actionEvent -> this.deleteSelection());

        HBox header = new HBox();
        header.getChildren().addAll(this.newFolder, createBtn);
        HBox footer = new HBox();
        footer.getChildren().addAll(reloadBtn, deleteBtn);

        this.animeList.getSelectionModel().selectedItemProperty().addListener((observableValue, old, newVal) -> {
            File selected = newVal.getValue();
            if (selected.exists() && selected.isDirectory()) {
                onItemSelectedCallback.call(Selector.this, selected);
            }
        });
        this.populateAnimeList();

        this.getChildren().addAll(header, this.animeList, footer);
    }

    private void deleteSelection() {
        TreeItem<File> item = this.animeList.getSelectionModel().getSelectedItem();
        if (item != null) {
            if (!item.getValue().isDirectory()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Confirm deletion");
                alert.setContentText("Are you sure to delete " + item.getValue().getName() + " ?");
                alert.setHeaderText(item.getValue().getAbsolutePath());
                alert.getButtonTypes().add(ButtonType.CANCEL);

                Optional<ButtonType> option = alert.showAndWait();
                if (option.get() == null || option.get() == ButtonType.CANCEL) {
                    return;
                }

                item.getValue().delete();
                item.getParent().getChildren().remove(item);
            }
        }
    }

    public void populateAnimeList() {
        this.root.getChildren().clear();
        File folderToScan = new File(Properties.get("baseFolderToScan"));

        for (File animeFolder : folderToScan.listFiles()) {
            if (animeFolder.isDirectory() && !animeFolder.getName().startsWith("_")) {
                TreeItem<File> item = new TreeItem<>(animeFolder);
                item.setExpanded(false);
                this.addEpisodes(animeFolder, item);
                this.root.getChildren().add(item);
            }
        }
    }

    public void addEpisodes(File folder, TreeItem<File> parent) {
        parent.getChildren().clear();
        for (File episode : folder.listFiles()) {
            if (!episode.getName().equals(".config")) {
                TreeItem<File> item = new TreeItem<>(episode);
                parent.getChildren().add(item);
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
        if (folder.mkdir()) {
            this.root.getChildren().add(new TreeItem<>(folder));
        }
    }
}
