package org.duck.VideoQuacker.ui;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.duck.VideoQuacker.utils.Callback;
import org.duck.VideoQuacker.utils.Properties;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class Selector extends VBox {

    public Image rootIcon = new Image(getClass().getResourceAsStream("/animesIcon.png"));
    public Image folderIcon = new Image(getClass().getResourceAsStream("/animeIcon.jpg"));
    public Image videoIcon = new Image(getClass().getResourceAsStream("/videoIcon.png"));

    public ImageView reloadIcon = new ImageView(new Image(getClass().getResourceAsStream("/reloadIcon.png")));
    public ImageView deleteIcon = new ImageView(new Image(getClass().getResourceAsStream("/deleteIcon.png")));


    public Callback<Selector, File> callback;
    public TreeView<CustomTreeItem> treeView;
    public TreeItem<CustomTreeItem> rootItem;

    private TextField newFolder;

    public Selector(Callback<Selector, File> callback) {
        this.callback = callback;
        this.treeView = new TreeView<>();
        this.treeView.setCellFactory(param -> new CustomTreeItemCellFactory());
        this.rootItem = new TreeItem<>(new CustomTreeItem("Anime", new File(Properties.get("baseFolderToScan")), CustomTreeItem.CustomTreeItemTypes.ROOT_ITEM));
        this.rootItem.setExpanded(true);
        this.treeView.setRoot(this.rootItem);

        Button createBtn = new Button("+");
        createBtn.setOnAction(actionEvent -> this.createNewFolder());

        this.reloadIcon.setFitHeight(16);
        this.deleteIcon.setFitHeight(16);
        this.reloadIcon.setFitWidth(16);
        this.deleteIcon.setFitWidth(16);
        Button reloadBtn = new Button("", this.reloadIcon);
        reloadBtn.setOnAction(actionEvent -> this.reloadItems());
        Button deleteBtn = new Button("", this.deleteIcon);
        deleteBtn.setOnAction(actionEvent -> this.deleteSelection());
        this.newFolder = new TextField();
        this.newFolder.setPromptText("Create a new folder");

        HBox header = new HBox();
        header.getChildren().addAll(this.newFolder, createBtn);
        header.setHgrow(this.newFolder, Priority.ALWAYS);
        HBox footer = new HBox();
        footer.getChildren().addAll(reloadBtn, deleteBtn);

        this.reloadItems();
        this.setCallbackHandler();

        this.getChildren().addAll(header,this.treeView, footer);
    }

    private void setCallbackHandler() {
        this.treeView.getSelectionModel().selectedItemProperty().addListener((observableValue, old, newVal) -> {
            CustomTreeItem selected = newVal.getValue();
            if (selected.type == CustomTreeItem.CustomTreeItemTypes.EPISODES) {
                selected = newVal.getParent().getValue();
            }
            if (selected.path.exists() && selected.path.isDirectory()) {
                callback.call(Selector.this, selected.path);
            }
        });
    }

    public void reloadItems() {
        this.rootItem.getChildren().clear();

        for (File f : this.rootItem.getValue().path.listFiles()) {
            if (f.isDirectory()) {
                TreeItem<CustomTreeItem> currentItem = new TreeItem<>(new CustomTreeItem(f.getName(), f, CustomTreeItem.CustomTreeItemTypes.FOLDER));
                for (File video: f.listFiles()) {
                    if (!video.getName().startsWith(".") && video.isFile()) {
                        currentItem.getChildren().add(new TreeItem<>(new CustomTreeItem(video.getName(), video, CustomTreeItem.CustomTreeItemTypes.EPISODES)));
                    }
                }
                this.rootItem.getChildren().add(currentItem);
            }
        }
    }

    private void createNewFolder() {
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
            this.rootItem.getChildren().add(new TreeItem<>(new CustomTreeItem(folder.getName(), folder, CustomTreeItem.CustomTreeItemTypes.FOLDER)));
        }
    }

    private void deleteSelection() {
        TreeItem<CustomTreeItem> item = this.treeView.getSelectionModel().getSelectedItem();
        if (item != null) {
            if (item.getValue().type == CustomTreeItem.CustomTreeItemTypes.FOLDER || item.getValue().type == CustomTreeItem.CustomTreeItemTypes.EPISODES) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Confirm deletion");
                alert.setContentText("Are you sure to delete " + item.getValue().label + " ?");
                alert.setHeaderText(item.getValue().path.getAbsolutePath());
                alert.getButtonTypes().add(ButtonType.CANCEL);

                Optional<ButtonType> option = alert.showAndWait();
                if (option.get() == null || option.get() == ButtonType.CANCEL) {
                    return;
                }

                item.getValue().path.delete();
                item.getParent().getChildren().remove(item);
            }
        }
    }

    static class CustomTreeItem {
        enum CustomTreeItemTypes {
            ROOT_ITEM,
            FOLDER,
            EPISODES;
        }

        public String label;
        public File path;
        private CustomTreeItemTypes type;

        public CustomTreeItem(String label, File path, CustomTreeItemTypes type) {
            this.label = label;
            this.path = path;
            this.type = type;
        }
    }

    class CustomTreeItemCellFactory extends TreeCell<CustomTreeItem> {

        private ImageView iconImageView;
        private ContextMenu openFolderMenu = null;

        public CustomTreeItemCellFactory() {
            iconImageView = new ImageView();
            iconImageView.setFitWidth(16);
            iconImageView.setFitHeight(16);
        }

        @Override
        protected void updateItem(CustomTreeItem customTreeItem, boolean empty) {
            super.updateItem(customTreeItem, empty);
            if (empty || customTreeItem == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(customTreeItem.label);
                switch (customTreeItem.type) {
                    case ROOT_ITEM -> this.iconImageView.setImage(Selector.this.rootIcon);
                    case FOLDER -> this.iconImageView.setImage(Selector.this.folderIcon);
                    case EPISODES -> this.iconImageView.setImage(Selector.this.videoIcon);
                }
                if (customTreeItem.type != CustomTreeItem.CustomTreeItemTypes.ROOT_ITEM && this.openFolderMenu == null) {
                    this.openFolderMenu = new ContextMenu();
                    MenuItem addMenuItem = new MenuItem("Open in folder");
                    openFolderMenu.getItems().add(addMenuItem);
                    addMenuItem.setOnAction((ActionEvent t) -> {
                        try {
                            Desktop.getDesktop().open(customTreeItem.path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    setContextMenu(openFolderMenu);
                }
                setGraphic(iconImageView);
            }
        }
    }
}
