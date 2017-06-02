package org.keytool.manager.main;

import com.google.inject.AbstractModule;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * @author Gideon Maree
 * @since 11 May 2017
 */
public class MainModule extends AbstractModule {

    private final Application app;
    private final Stage stage;

    public MainModule(Application app, Stage stage) {
        this.app = app;
        this.stage = stage;
    }

    @Override
    protected void configure() {
        bind(Application.class).toInstance(app);
        bind(Stage.class).toInstance(stage);
    }
}
