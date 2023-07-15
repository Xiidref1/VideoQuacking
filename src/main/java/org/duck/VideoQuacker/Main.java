package org.duck.VideoQuacker;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.duck.VideoQuacker.ui.*;

import java.io.File;

public class Main extends Application {

    private HBox rootView;
    private Selector selector;
    private JobCreationForm jobCreationForm;
    private JobManager jobManager;

    private Scene scene;


    @Override
    public void start(Stage stage) {
        this.createRoot();

        this.scene = new Scene(this.rootView);
        stage.setScene(this.scene);
        this.jobCreationForm.initClipboard();
        stage.show();
    }


    private void createRoot() {
        this.selector = new Selector(Main.this::call);
        this.jobCreationForm = new JobCreationForm(Main.this::call ,this.scene);
        this.jobManager = new JobManager();

        this.rootView = new HBox();
        this.rootView.getChildren().addAll(this.selector, new Separator(Orientation.VERTICAL), this.jobCreationForm, new Separator(Orientation.VERTICAL), this.jobManager);
    }





    public static void main(String[] args) {
        launch();
    }


    public void call(Selector caller, File result) {
        this.jobCreationForm.prepareFor(result);
    }

    public void call(JobCreationForm caller, Job result) {
        this.jobManager.append(result);
    }
}