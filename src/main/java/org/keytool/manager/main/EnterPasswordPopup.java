package org.keytool.manager.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.keytool.manager.utils.GuiceFXMLLoader;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * @author Gideon Maree
 * @since 18 May 2017
 */
@Singleton
public class EnterPasswordPopup implements Initializable{

    @Inject
    GuiceFXMLLoader fxmlLoader;

    @FXML PasswordField password;
    @FXML Label promptLabel;

    Optional<String> result;
    Stage stage;
    String prompt;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        promptLabel.setText(prompt);
    }

    @FXML
    public void cancelClicked(ActionEvent actionEvent) {
        result = Optional.empty();
        stage.hide();
    }

    @FXML
    public void okClicked(ActionEvent actionEvent) {
        result = Optional.of(password.getText());
        stage.hide();
    }

    public Optional<String> show(String prompt){
        this.prompt = prompt;
        stage = fxmlLoader.popup(
                "/org/keytool/manager/main/EnterPasswordPopup.fxml",
                "Enter Password",
                300,
                150
        );
        stage.showAndWait();
        return result;
    }
}
