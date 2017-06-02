package org.keytool.manager.main;

import com.google.inject.Inject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeView;
import org.keytool.manager.utils.Alerts;

import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ResourceBundle;

/**
 * @author Gideon Maree
 * @since 19 May 2017
 */
public class ImportCertController implements Initializable {

    @Inject
    KeystoreManager keystoreManager;
    @Inject
    EnterPasswordPopup passwordPopup;

    @FXML TextArea importText;

    String alias;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        alias = keystoreManager.selectedEntryProperty().get().alias;
    }

    public void cancelClicked(ActionEvent actionEvent) {
        importText.getScene().getWindow().hide();
    }

    public void importClicked(ActionEvent actionEvent) {
        passwordPopup.show("Please enter the password for alias "+alias).ifPresent(pwd -> {
            try {
                keystoreManager.importChain(alias, pwd, importText.getText());
                importText.getScene().getWindow().hide();
            } catch (Exception e) {
                e.printStackTrace();
                Alerts.error(importText, e);
            }
        });
    }
}
