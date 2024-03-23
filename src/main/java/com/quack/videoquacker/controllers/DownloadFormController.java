package com.quack.videoquacker.controllers;

import com.quack.videoquacker.MainApplication;
import com.quack.videoquacker.models.CopiedParameters;
import com.quack.videoquacker.models.DownloadModesEnum;
import com.quack.videoquacker.models.JobParameters;
import com.quack.videoquacker.models.QualityEnum;
import com.quack.videoquacker.utils.DataManager;
import com.quack.videoquacker.utils.IObservableListener;
import com.quack.videoquacker.utils.NotificationManager;
import com.quack.videoquacker.utils.PropertiesManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadFormController implements IObservableListener<String> {
    private static final String LISTENER_KEY_CLIPBOARD = "clip";
    private File currentSeriesSelected;
    private PropertiesManager currentSeriesProperties;
    private CopiedParameters copiedParameters = null;
    @FXML
    public TextField tfURL;
    @FXML
    public TextField tfTargetEpname;
    @FXML
    public ChoiceBox<String> cbTargetQuality;
    @FXML
    public ChoiceBox<String> cbTargetDlmode;
    @FXML
    public TextField tfDefaultNamepattern;
    @FXML
    public ChoiceBox<String> cbDefaultQuality;
    @FXML
    public Button btnStartDownload;
    @FXML
    public Button btnSaveDefaults;


    public void startDownloadJob() {
        if (!DataManager.isValidUrl(this.tfURL.getText())) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid url aborting");
            alert.setTitle("Invalid URL");
            alert.showAndWait();
            return;
        }

        if (MainWindowController.instance.currentJobsController.jobWithUrlExist(this.tfURL.getText())) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "URL already used by the job " + MainWindowController.instance.currentJobsController.getJobNameFromUrl(this.tfURL.getText()) + ".\nAre you sure you want to start another job with the same URL ?", ButtonType.YES, ButtonType.CANCEL);
            alert.setTitle("Invalid URL");
            alert.setHeaderText("URL already in a job");
            alert.showAndWait();

            if (alert.getResult() == ButtonType.CANCEL) {
                return;
            }
        }

        Matcher epNumMatcher = Pattern.compile(this.currentSeriesProperties.getProperty(PropertiesManager.PropertiesKeys.name_pattern)
                .replace("{{epnum}}", "(?<epnum>\\d+)")
        ).matcher(this.tfTargetEpname.getText());

        int epNum = -1;
        if (epNumMatcher.matches()) {
            epNum = Integer.parseInt(epNumMatcher.group("epnum"));
        }

        JobParameters jobInstance = new JobParameters(DataManager.getUrl(this.tfURL.getText()),
                this.tfTargetEpname.getText(),
                epNum,
                QualityEnum.getFromDisplayText(this.cbTargetQuality.getSelectionModel().getSelectedItem()),
                DownloadModesEnum.getFromDisplayText(this.cbTargetDlmode.getSelectionModel().getSelectedItem()),
                this.currentSeriesSelected,
                this.currentSeriesProperties,
                this.copiedParameters == null ? null : this.copiedParameters.getHeaders()
        );

        MainWindowController.instance.currentJobsController.startJob(jobInstance);
    }

    public void saveDefaults() {
        this.currentSeriesProperties.setProperty(PropertiesManager.PropertiesKeys.name_pattern, this.tfDefaultNamepattern.getText())
                .setProperty(PropertiesManager.PropertiesKeys.default_quality, QualityEnum.getFromDisplayText(this.cbDefaultQuality.getValue()).name());
        this.onSerieSelected(this.currentSeriesSelected);
    }

    @FXML
    public void initialize() {
        toogleEnabled(false);
        for (QualityEnum qualityEnum : QualityEnum.values()) {
            this.cbTargetQuality.getItems().add(qualityEnum.displayText);
            this.cbDefaultQuality.getItems().add(qualityEnum.displayText);
        }
        for (DownloadModesEnum mode : DownloadModesEnum.values()) {
            this.cbTargetDlmode.getItems().add(mode.displayText);
        }
        this.cbTargetDlmode.getSelectionModel().select(0);
        MainApplication.getClipboard().registerListener(LISTENER_KEY_CLIPBOARD, this);
    }

    private void toogleEnabled(boolean enabled) {
        tfURL.setDisable(!enabled);
        tfTargetEpname.setDisable(!enabled);
        cbTargetQuality.setDisable(!enabled);
        cbTargetDlmode.setDisable(!enabled);
        tfDefaultNamepattern.setDisable(!enabled);
        cbDefaultQuality.setDisable(!enabled);
        btnStartDownload.setDisable(!enabled);
        btnSaveDefaults.setDisable(!enabled);
    }


    public void onSerieSelected(File path) {
        this.currentSeriesSelected = path;
        if (path == null) {
            this.toogleEnabled(false);
            return;
        }
        String name = path.getName();
        path = new File(path, "current.properties");
        PropertiesManager propertiesManager = null;
        boolean needInit = false;
        try {
            propertiesManager = new PropertiesManager(path.toString());
            if (propertiesManager.isInvalid()) {
                needInit = true;
                path.delete();
            }
        } catch (ExceptionInInitializerError error) {
            needInit = true;
        }

        if (needInit) {
            try {
                path.createNewFile();
                propertiesManager = new PropertiesManager(path.toString());
                propertiesManager.setProperty(PropertiesManager.PropertiesKeys.name_pattern, name.trim() + " S1E{{epnum}}.mp4");
                propertiesManager.setProperty(PropertiesManager.PropertiesKeys.default_quality, QualityEnum.MAX_250_MB.name());
                propertiesManager.setProperty(PropertiesManager.PropertiesKeys.max_ep, "0");
                propertiesManager.setProperty(PropertiesManager.PropertiesKeys.alternatives_names, "");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        this.currentSeriesProperties = propertiesManager;

        Platform.runLater(() -> {
            //Default settings section
            this.tfDefaultNamepattern.setText(this.currentSeriesProperties.getProperty(PropertiesManager.PropertiesKeys.name_pattern));
            this.cbDefaultQuality.getSelectionModel().select(QualityEnum.valueOf(this.currentSeriesProperties.getProperty(PropertiesManager.PropertiesKeys.default_quality)).displayText);

            //JobLaucher settings section
            String targetEpNum = "000000000" + (Integer.parseInt(this.currentSeriesProperties.getProperty(PropertiesManager.PropertiesKeys.max_ep)) + 1);
            targetEpNum = targetEpNum.substring(targetEpNum.length() - Math.max(this.currentSeriesProperties.getProperty(PropertiesManager.PropertiesKeys.max_ep).length(), 2));
            this.tfTargetEpname.setText(this.currentSeriesProperties.getProperty(PropertiesManager.PropertiesKeys.name_pattern).replace("{{epnum}}", targetEpNum));
            this.cbTargetQuality.getSelectionModel().select(QualityEnum.valueOf(this.currentSeriesProperties.getProperty(PropertiesManager.PropertiesKeys.default_quality)).displayText);

            this.toogleEnabled(true);
        });
    }

    @Override
    public void onObservableChange(String key, String value) {
        if (key.equals(LISTENER_KEY_CLIPBOARD)) {
            if (DataManager.isValidJSON(value)) {
                this.copiedParameters = CopiedParameters.fromJsonString(value);
                Platform.runLater(() -> {
                    this.tfURL.setText(this.copiedParameters.getUrl());
                });
                NotificationManager.notify("Data retrieved for " + this.copiedParameters.getSname() + ", episode " + this.copiedParameters.getEpnum());
            } else {
                if (DataManager.isValidUrl(value)) {
                    this.copiedParameters = null;
                    Platform.runLater(() -> {
                        this.tfURL.setText(value);
                    });
                    NotificationManager.notify("Url retrieved : " + value);
                }
            }
        }
    }
}
