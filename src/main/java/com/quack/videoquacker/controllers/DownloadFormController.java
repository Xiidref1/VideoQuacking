package com.quack.videoquacker.controllers;

import com.quack.videoquacker.models.QualityEnum;
import com.quack.videoquacker.utils.PropertiesManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.IOException;

public class DownloadFormController {
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

    }

    public void saveDefaults() {

    }

    @FXML
    public void initialize() {
        toogleEnabled(false);
        for (QualityEnum qualityEnum:QualityEnum.values()) {
            this.cbTargetQuality.getItems().add(qualityEnum.displayText);
            this.cbDefaultQuality.getItems().add(qualityEnum.displayText);
        }
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
        String targetEpNum = "000000000"+ (Integer.parseInt(propertiesManager.getProperty(PropertiesManager.PropertiesKeys.max_ep))+1);
        targetEpNum = targetEpNum.substring(targetEpNum.length() - Math.max(propertiesManager.getProperty(PropertiesManager.PropertiesKeys.max_ep).length(), 2));
        this.tfTargetEpname.setText(propertiesManager.getProperty(PropertiesManager.PropertiesKeys.name_pattern).replace("{{epnum}}", targetEpNum));
        this.cbTargetQuality.getSelectionModel().select(QualityEnum.valueOf(propertiesManager.getProperty(PropertiesManager.PropertiesKeys.default_quality)).displayText);

        this.toogleEnabled(true);
    }
}
