package org.keytool.manager.utils;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.stream.Stream;

import com.google.inject.Module;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.StatusBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gideon Maree
 * @since 11 May 2017
 */
public class GuiceFXMLLoader {
    private static final Logger LOG = LoggerFactory.getLogger(GuiceFXMLLoader.class);
    private final Injector injector;
    private final Callback<Class<?>, Object> loader;
    private final Stage primaryStage;

    @Inject
    public GuiceFXMLLoader(Injector injector, Stage primaryStage) {
        this.injector = injector;
        this.primaryStage = primaryStage;
        loader = c -> {
            Object instance = injector.getInstance(c);
            Stream.of(instance.getClass().getFields())
                    .filter(f -> f.isAnnotationPresent(FXML.class))
                    .forEach(f -> {
                        try {
                            f.set(instance, null);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    });
            return instance;
        };
    }

    // Load some FXML file, using the supplied Controller, and return the
    // instance of the initialized controller...?
    public Object load(String url) {
        InputStream in = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setControllerFactory(loader);
            in = GuiceFXMLLoader.class.getResourceAsStream(url);
            Parent content = fxmlLoader.load(in);
            BorderPane pane = new BorderPane();
            pane.setCenter(content);
            StatusBar bottom = new StatusBar();
            bottom.setPrefHeight(40);
            bottom.setText("");
            pane.setBottom(bottom);
            NotificationPane rt = new NotificationPane(pane);
            rt.setId("NotificationPane");
            rt.setShowFromTop(false);
            rt.showingProperty().addListener(showing -> {
                if(rt.isShowing()){
                    Timeline timeline = new Timeline(new KeyFrame(
                            Duration.millis(5000),
                            ae -> rt.hide()));
                    timeline.play();
                }
            });
            return rt;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            if (in != null) try { in.close(); } catch (Exception ee) { }
        }
    }

    public Stage popup(String url, String title, int width, int height){
        final Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(primaryStage);
        load(stage, url, title, width, height);
        return stage;
    }

    public Stage load(Stage stage, String url, String title, int width, int height){
        final Scene scene = new Scene((Parent) load(url), width, height);
        scene.getStylesheets().add(GuiceFXMLLoader.class.getResource("/org/keytool/manager/main/bootstrap3.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle(title);
        return stage;
    }

    public Stage load(String url, String title, int width, int height){
        return load(primaryStage, url, title, width, height);
    }

    public static GuiceFXMLLoader init(Stage stage, Module... modules){
        Injector injector = Guice.createInjector(modules);
        return new GuiceFXMLLoader(injector, stage);
    }
}

