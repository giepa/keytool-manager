package org.keytool.manager.main;

import com.google.inject.Inject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.keytool.manager.utils.Alerts;
import org.keytool.manager.utils.CertUtils;
import org.keytool.manager.utils.X509Builder;

import javax.security.auth.x500.X500Principal;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keytool.manager.utils.CertUtils.getRDN;

/**
 * @author Gideon Maree
 * @since 17 May 2017
 */
public class GenCsrController implements Initializable {

    @Inject
    KeystoreManager keystoreManager;
    @Inject
    EnterPasswordPopup passwordPopup;

    @FXML ComboBox selectAlg;
    @FXML TextField enterCN;
    @FXML TextField enterOU;
    @FXML TextField enterO;
    @FXML TextField enterL;
    @FXML TextField enterST;
    @FXML TextField enterC;
    @FXML TextField enterE;
    @FXML TextArea csrText;

    private String alias;
    private Validation validation;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            alias = keystoreManager.selectedEntryProperty().get().getAlias();
            X500Name x500name = keystoreManager.getX500Name(alias);
            enterCN.setText(CertUtils.getRDN(x500name, BCStyle.CN));
            enterOU.setText(CertUtils.getRDN(x500name, BCStyle.OU));
            enterO.setText(CertUtils.getRDN(x500name, BCStyle.O));
            enterL.setText(CertUtils.getRDN(x500name, BCStyle.L));
            enterST.setText(CertUtils.getRDN(x500name, BCStyle.ST));
            enterC.setText(CertUtils.getRDN(x500name, BCStyle.C));
            enterE.setText(CertUtils.getRDN(x500name, BCStyle.E));
        } catch (Exception e) {
            //Alerts.error(csrText, e);
            e.printStackTrace();
        }
        validation = Validation.init()
                .notEmpty(selectAlg,"Please enter an Algorithm", "#selectAlgMsg")
                .notEmpty(enterCN,"Please enter a Common Name", "#enterCNMsg")
                .equals(enterC,"Please enter a valid country code", CertUtils.getLocales(), "#enterCMsg");
    }

    @FXML
    public void cancelClicked(ActionEvent actionEvent) {
        csrText.getScene().getWindow().hide();
    }

    @FXML
    public void generateClicked(ActionEvent actionEvent) {
        if(validation.validate()){
            passwordPopup.show("Please enter the password for alias "+alias).ifPresent(pwd -> {
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
                    String csr = keystoreManager.generateCsr(
                            alias,
                            pwd,
                            name,
                            selectAlg.getSelectionModel().getSelectedItem().toString()
                    );
                    csrText.setText(new String(csr));
                    csrText.requestFocus();
                    csrText.selectAll();
                } catch (Exception e) {
                    e.printStackTrace();
                    Alerts.error(enterCN,e);
                }
            });
        }
    }



}
