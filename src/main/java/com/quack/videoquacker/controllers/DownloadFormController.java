package com.quack.videoquacker.controllers;

import com.quack.videoquacker.MainApplication;
import com.quack.videoquacker.models.DownloadFormInstance;
import com.quack.videoquacker.models.DownloadModesEnum;
import com.quack.videoquacker.models.QualityEnum;
import com.quack.videoquacker.utils.DataMatcher;
import com.quack.videoquacker.utils.PropertiesManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.File;
import java.io.IOException;

public class DownloadFormController {
    private File currentSeriesSelected;
    private PropertiesManager currentSeriesProperties;

    @FXML
    public TextField tfURL;
    @FXML
    public TextField tfTargetEpname;
    @FXML
    public ChoiceBox cbTargetQuality;
    @FXML
    public ChoiceBox cbTargetDlmode;
    @FXML
    public TextField tfDefaultNamepattern;
    @FXML
    public ChoiceBox cbDefaultQuality;

    @FXML
    public Button btnStartDownload;
    @FXML
    public Button btnSaveDefaults;

    public void startDownloadJob() {
        if (!DataMatcher.isValidUrl(this.tfURL.getText())) {
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

        DownloadFormInstance jobInstance = new DownloadFormInstance(this.tfURL.getText(),
                this.tfTargetEpname.getText(),
                QualityEnum.getFromDisplayText((String) this.cbTargetQuality.getSelectionModel().getSelectedItem()),
                DownloadModesEnum.getFromDisplayText((String) this.cbTargetDlmode.getSelectionModel().getSelectedItem()),
                this.currentSeriesSelected,
                this.currentSeriesProperties
        );

        MainWindowController.instance.currentJobsController.startJob(jobInstance);
    }

    public void saveDefaults() {
        this.currentSeriesProperties.setProperty(PropertiesManager.PropertiesKeys.name_pattern, this.tfDefaultNamepattern.getText())
                .setProperty(PropertiesManager.PropertiesKeys.default_quality, QualityEnum.getFromDisplayText((String) this.cbDefaultQuality.getValue()).name());
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
            if (!propertiesManager.isValid()) {
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

        //Default settings section
        this.tfDefaultNamepattern.setText(propertiesManager.getProperty(PropertiesManager.PropertiesKeys.name_pattern));
        this.cbDefaultQuality.getSelectionModel().select(QualityEnum.valueOf(propertiesManager.getProperty(PropertiesManager.PropertiesKeys.default_quality)).displayText);

        //JobLaucher settings section
        String targetEpNum = "000000000" + (Integer.parseInt(propertiesManager.getProperty(PropertiesManager.PropertiesKeys.max_ep)) + 1);
        targetEpNum = targetEpNum.substring(targetEpNum.length() - Math.max(propertiesManager.getProperty(PropertiesManager.PropertiesKeys.max_ep).length(), 2));
        this.tfTargetEpname.setText(propertiesManager.getProperty(PropertiesManager.PropertiesKeys.name_pattern).replace("{{epnum}}", targetEpNum));
        this.cbTargetQuality.getSelectionModel().select(QualityEnum.valueOf(propertiesManager.getProperty(PropertiesManager.PropertiesKeys.default_quality)).displayText);

        this.currentSeriesProperties = propertiesManager;
        this.toogleEnabled(true);
    }
}
