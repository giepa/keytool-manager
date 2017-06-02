package org.keytool.manager.main;

import com.google.inject.Inject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.bouncycastle.operator.OperatorCreationException;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.controlsfx.validation.decoration.GraphicValidationDecoration;
import org.controlsfx.validation.decoration.StyleClassValidationDecoration;
import org.keytool.manager.utils.Alerts;
import org.keytool.manager.utils.CertUtils;
import org.keytool.manager.utils.DecimalTextFormatter;
import org.keytool.manager.utils.X509Builder;

import java.io.IOException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

/**
 * @author Gideon Maree
 * @since 12 May 2017
 */
public class GenKeyPairController implements Initializable {

    @Inject
    KeystoreManager keystoreManager;

    @FXML Button generateButton;
    @FXML ComboBox selectAlg;
    @FXML TextField keySize;
    @FXML TextField alias;
    @FXML PasswordField password;

    @FXML ComboBox selectSignAlg;
    @FXML TextField enterCN;
    @FXML TextField enterOU;
    @FXML TextField enterO;
    @FXML TextField enterL;
    @FXML TextField enterST;
    @FXML TextField enterC;
    @FXML TextField enterE;

    Validation validation;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        keySize.setTextFormatter(new DecimalTextFormatter(new DecimalFormat("#00")));
        validation = Validation.init()
                .notEmpty(alias, "Please enter an Alias", "#aliasMsg")
                .notEmpty(selectAlg, "Please select a Key Algorithm", "#selectAlgMsg")
                .notEmpty(password, "please enter a Key Password", "#passwordMsg")
                .between( keySize,"Invalid Key size", 16, 10000, "#keySizeMsg")
                .notEmpty(selectSignAlg,"Please enter a Signiture Algorithm", "#selectSignAlgMsg")
                .notEmpty(enterCN,"Please enter a Common Name", "#enterCNMsg")
                .equals(enterC,"Please enter a valid Country Code", CertUtils.getLocales(), "#enterCMsg");
    }

    @FXML
    public void cancelClicked(ActionEvent actionEvent) {
        generateButton.getScene().getWindow().hide();
    }

    @FXML
    public void generateClicked(ActionEvent actionEvent) {
        if(validation.validate()) {
            generateButton.setDisable(true);
            try {
                String name = X509Builder.init()
                        .appendPart( "CN", enterCN.getText())
                        .appendPart( ",OU", enterOU.getText())
                        .appendPart( ",O", enterO.getText())
                        .appendPart( ",ST", enterST.getText())
                        .appendPart( ",L", enterL.getText())
                        .appendPart( ",C", enterC.getText())
                        .appendPart( ",E", enterE.getText())
                        .toString();
                keystoreManager.generateKeyPair(
                        alias.getText(),
                        password.getText(),
                        name,
                        Integer.parseInt(keySize.getText()),
                        selectAlg.getSelectionModel().getSelectedItem().toString(),
                        selectSignAlg.getSelectionModel().getSelectedItem().toString()
                );
                generateButton.getScene().getWindow().hide();
            } catch (Exception e) {
                generateButton.setDisable(false);
                Alerts.error(generateButton, e);
                e.printStackTrace();
            }
        }
    }

}
