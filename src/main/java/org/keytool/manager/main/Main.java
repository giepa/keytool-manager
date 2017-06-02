package org.keytool.manager.main;

import com.cathive.fonts.fontawesome.FontAwesomeIcon;
import com.cathive.fonts.fontawesome.FontAwesomeIconView;
import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.keytool.manager.utils.GuiceFXMLLoader;

/**
 * @author Gideon Maree
 * @since 11 May 2017
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        stage.getIcons().clear();
        stage.getIcons().add(getIcon());
        GuiceFXMLLoader.init(stage, new MainModule(this, stage))
                .load("/org/keytool/manager/main/Main.fxml","Keytool Manager",1024, 768)
                .show();

    }

    public Image getIcon(){
        return new Image(getClass().getResourceAsStream("icon.png"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}


