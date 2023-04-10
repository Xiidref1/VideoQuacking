package org.duck.VideoQuacker.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.duck.VideoQuacker.enums.DownloadTypeEnum;
import org.duck.VideoQuacker.enums.PropertiesKeyEnum;
import org.duck.VideoQuacker.enums.QualityEnum;
import org.duck.VideoQuacker.utils.Callback;
import org.duck.VideoQuacker.utils.Properties;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JobCreationForm extends VBox {

    final Pattern urlPattern = Pattern.compile("((https?|ftp)://|(www|ftp)\\.)[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?");

    private Label titre;
    private GridPane boxFormulaire;
    private Callback<JobCreationForm, Job> callbackJoCreated;

    private File videoFolder;
    private File configFile;

    private TextField url;
    private ChoiceBox<String> typeChoice;
    private ChoiceBox<String> qualityChoice;
    private TextField name;
    private TextField defaultName;
    private ChoiceBox<String> defaultQuality;
    private TextField defaultSeason;

    private Button saveDefaults;
    private Button startJob;
    private Clipboard clipboard;

    public JobCreationForm(Callback<JobCreationForm, Job> callbackJobCreated, Scene scene) {
        super();

        this.callbackJoCreated = callbackJobCreated;
        this.titre = new Label("Aucune séléction");
        this.titre.setAlignment(Pos.CENTER);
        this.titre.setMinWidth(480);

        this.createForm();
        this.enableForm(false);
        this.boxFormulaire.setMinWidth(480);

        this.setSpacing(10);
        this.getChildren().addAll(this.titre, this.boxFormulaire);
    }

    public void initClipboard() {
        this.clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        this.clipboard.addFlavorListener(e -> this.handleClipBoardChange());
        getScene().setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.C) {
                this.handleClipBoardChange();
            }
        });
    }

    /**
     * Create all fields of the form
     */
    private void createForm() {
        this.boxFormulaire = new GridPane();
        this.url = new TextField();
        this.typeChoice = new ChoiceBox<>();
        this.qualityChoice = new ChoiceBox<>();
        this.name = new TextField();
        this.defaultName = new TextField();
        this.defaultQuality = new ChoiceBox<>();
        this.defaultSeason = new TextField();
        this.startJob = new Button("Start job");
        this.saveDefaults = new Button("Save defaults");

        this.url.setPromptText("URL à télécharger");
        this.defaultSeason.setPromptText("Numéro de la saison");
        this.defaultName.setPromptText("Pattern par défaut pour nommer les fichiers");
        this.defaultName.setTooltip(new Tooltip("Les pattern suivant sont disponible : \n" +
                " - {{snum}} : Le numéro de la saison\n" +
                " - {{enum}} : Le numéro de l'épisode"));

        this.boxFormulaire.setHgap(10);
        this.boxFormulaire.setVgap(10);


        this.boxFormulaire.add(new Label("URL"), 0, 0);
        this.boxFormulaire.add(this.url, 1, 0, 3, 1);

        this.boxFormulaire.add(new Label("Type"), 0, 1);
        this.boxFormulaire.add(this.typeChoice, 1, 1);
        this.boxFormulaire.add(new Label("Quality"), 2, 1);
        this.boxFormulaire.add(this.qualityChoice, 3, 1);
        this.boxFormulaire.add(new Label("Name"), 0, 2);
        this.boxFormulaire.add(this.name, 1, 2, 2, 1);
        this.boxFormulaire.add(new Label("Default name"), 0, 3);
        this.boxFormulaire.add(this.defaultName, 1, 3, 3, 1);
        this.boxFormulaire.add(new Label("Default quality"), 0, 4);
        this.boxFormulaire.add(this.defaultQuality, 1, 4);
        this.boxFormulaire.add(new Label("Default season number"), 2, 4);
        this.boxFormulaire.add(this.defaultSeason, 3, 4);

        this.boxFormulaire.add(this.saveDefaults, 0, 5, 2, 1);
        this.boxFormulaire.add(this.startJob, 3, 5, 2, 1);

        this.saveDefaults.setOnAction(actionEvent -> this.saveDefaults());
        this.startJob.setOnAction(actionEvent -> this.startJob());
    }


    /**
     * Enable or diable all fields in the form
     *
     * @param enabled Tell if fields should be enabled or not
     */
    private void enableForm(boolean enabled) {
        this.url.setDisable(!enabled);
        this.typeChoice.setDisable(!enabled);
        this.qualityChoice.setDisable(!enabled);
        this.name.setDisable(!enabled);
        this.defaultName.setDisable(!enabled);
        this.defaultQuality.setDisable(!enabled);
        this.defaultSeason.setDisable(!enabled);
        this.saveDefaults.setDisable(!enabled);
        this.startJob.setDisable(!enabled);

        if (!enabled) {
            this.url.setText("");
            this.name.setText("");
            this.defaultName.setText("");
            this.defaultSeason.setText("");
        }
    }


    /**
     * Fill the field with the coniguration of the gien directory
     *
     * @param videoFolder The folder tot download into
     */
    public void prepareFor(File videoFolder) {
        this.videoFolder = videoFolder;
        this.configFile = new File(videoFolder, ".config");

        if (!this.configFile.exists()) {
            Map<String, String> initialConfig = new HashMap<>();
            initialConfig.put(PropertiesKeyEnum.DEFAULT_NAME.name(), "Name S{{snum}}E{{enum}}.mp4");
            initialConfig.put(PropertiesKeyEnum.DEFAULT_QUALITY.name(), QualityEnum.QUALITY_720_LOW.name());
            initialConfig.put(PropertiesKeyEnum.SEASON.name(), "1");
            Properties.createOrUpdatePropFile(this.configFile, initialConfig);
        }

        this.titre.setText("Création job pour : " + videoFolder.getName());

        this.typeChoice.getItems().clear();
        for (DownloadTypeEnum dltype : DownloadTypeEnum.values()) this.typeChoice.getItems().add(dltype.name());
        this.typeChoice.setValue(DownloadTypeEnum.HLS.name());

        this.qualityChoice.getItems().clear();
        this.defaultQuality.getItems().clear();
        for (QualityEnum quality : QualityEnum.values()) {
            this.qualityChoice.getItems().add(quality.name());
            this.defaultQuality.getItems().add(quality.name());
        }
        this.qualityChoice.setValue(QualityEnum.valueOf(Properties.getFromCustom(this.configFile, PropertiesKeyEnum.DEFAULT_QUALITY.name())).name());
        this.defaultQuality.setValue(QualityEnum.valueOf(Properties.getFromCustom(this.configFile, PropertiesKeyEnum.DEFAULT_QUALITY.name())).name());

        String defaultName = Properties.getFromCustom(this.configFile, PropertiesKeyEnum.DEFAULT_NAME.name());
        String epNum = "00" + (Integer.parseInt(Properties.getFromCustom(this.configFile, PropertiesKeyEnum.LAST_EP_NUM.name(), "00")) + 1);
        String name = defaultName.replaceAll("\\{\\{snum\\}\\}", Properties.getFromCustom(this.configFile, PropertiesKeyEnum.SEASON.name(), "1"))
                .replaceAll("\\{\\{enum\\}\\}", epNum.substring(epNum.length() - 2));


        this.name.setText(name);
        this.defaultName.setText(defaultName);
        this.defaultSeason.setText(Properties.getFromCustom(this.configFile, PropertiesKeyEnum.SEASON.name(), "1"));

        this.enableForm(true);
    }


    /**
     * Save the default in the window to the config file
     */
    private void saveDefaults() {
        Map<String, String> props = new HashMap<>();

        String err = "";

        if (this.defaultSeason.getText().isBlank()) {
            err += "Le numéro de saison est manquant \n";
        }
        if (this.defaultQuality.getValue().isBlank()) {
            err += "La qualitée est manquante\n";
        }
        if (this.defaultName.getText().isBlank()) {
            err += "Le nom par défaut ne peut pas être vide\n";
        }

        if (!err.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText(err);
            alert.showAndWait();
            return;
        }

        props.put(PropertiesKeyEnum.SEASON.name(), this.defaultSeason.getText());
        props.put(PropertiesKeyEnum.DEFAULT_QUALITY.name(), QualityEnum.valueOf(this.defaultQuality.getValue()).name());
        props.put(PropertiesKeyEnum.DEFAULT_NAME.name(), this.defaultName.getText());

        Properties.createOrUpdatePropFile(this.configFile, props);

        String epNum = "00" + (Integer.parseInt(Properties.getFromCustom(this.configFile, PropertiesKeyEnum.LAST_EP_NUM.name(), "00")) + 1);
        String name = this.defaultName.getText().replaceAll("\\{\\{snum\\}\\}", this.defaultSeason.getText())
                .replaceAll("\\{\\{enum\\}\\}", epNum.substring(epNum.length() - 2));

        this.name.setText(name);
        this.qualityChoice.setValue(this.defaultQuality.getValue());
    }

    /**
     * Start a jo with the current form values
     */
    private void startJob() {
        if (this.url.getText().isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("URL is empty");
            alert.setHeaderText(null);
            alert.setContentText("The URL is empty aborting");
            alert.show();

            return;
        }

//        if (new UrlValidator().isValid(this.url.getText())) {
//            Alert alert = new Alert(Alert.AlertType.WARNING);
//            alert.setTitle("URL is not valid");
//            alert.setHeaderText(null);
//            alert.setContentText("The URL is not valid aborting");
//            alert.show();
//
//            return;
//        }

        if (new File(this.videoFolder, this.name.getText()).exists()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("File already exist");
            alert.setHeaderText("This file already exist are you sure you want to continue and replace it ?");
            alert.setContentText(new File(this.videoFolder, this.name.getText()).getAbsolutePath());
            alert.getButtonTypes().add(ButtonType.CANCEL);

            Optional<ButtonType> option = alert.showAndWait();
            if (option.get() == null || option.get() == ButtonType.CANCEL) {
                return;
            }
        }


        String tockens = this.defaultName.getText();
        String name = this.name.getText();

        Matcher matcher = Pattern.compile(tockens.replaceAll("\\{\\{enum\\}\\}", "(.*)").replaceAll("\\{\\{snum\\}\\}", ".*")).matcher(name);
        String epNum = null;
        if (matcher.matches()) {
            epNum = matcher.group(1);
            if (!StringUtils.isNumeric(epNum)) {
                epNum = null;
            }
        }


        callbackJoCreated.call(this,
                new Job(this.url.getText(),
                        new File(this.videoFolder, this.name.getText()),
                        this.configFile,
                        epNum,
                        DownloadTypeEnum.valueOf(this.typeChoice.getValue()),
                        QualityEnum.from(Integer.parseInt(this.qualityChoice.getValue()))
                )
        );
    }


    private void handleClipBoardChange() {
        if (this.clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
            String data = null;
            try {
                data = (String) this.clipboard.getData(DataFlavor.stringFlavor);

                Matcher matcher = urlPattern.matcher(data);
                if (matcher.find()) {
                    String url = matcher.group();
                    Platform.runLater(() -> this.url.setText(url));
                }
            } catch (UnsupportedFlavorException unsupportedFlavorException) {
                unsupportedFlavorException.printStackTrace();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
