package org.keytool.manager.main;

import com.cathive.fonts.fontawesome.FontAwesomeIcon;
import com.cathive.fonts.fontawesome.FontAwesomeIconView;
import com.google.inject.Inject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.keytool.manager.utils.Alerts;
import org.keytool.manager.utils.CertUtils;

import javax.security.auth.x500.X500Principal;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ResourceBundle;

/**
 * @author Gideon Maree
 * @since 19 May 2017
 */
public class CertDetailsController implements Initializable{

    @FXML TextField showKeySize;
    @FXML TextField showSigAlg;
    @FXML TextField showAlg;
    @FXML TextField showVersion;
    @FXML TextField showIssuer;
    @FXML TextField showSerial;
    @FXML TextField showValidFrom;
    @FXML TextField showValidUntil;
    @FXML TextField showCN;
    @FXML TextField showOU;
    @FXML TextField showO;
    @FXML TextField showL;
    @FXML TextField showST;
    @FXML TextField showC;
    @FXML TextField showE;

    @Inject
    KeystoreManager keystoreManager;
    @Inject
    EnterPasswordPopup passwordPopup;

    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        init(keystoreManager.selectedCertProperty().get());
    }

    private void init(X509Certificate cert){
        showKeySize.setText(CertUtils.getKeySize(cert.getPublicKey()));
        showSigAlg.setText(cert.getSigAlgName());
        showAlg.setText(cert.getPublicKey().getAlgorithm());
        showVersion.setText(Integer.toString(cert.getVersion()));
        showIssuer.setText(cert.getIssuerDN().toString());
        showSerial.setText(cert.getSerialNumber().toString());
        showValidFrom.setText(cert.getNotBefore().toString());
        showValidUntil.setText(cert.getNotAfter().toString());

        X500Principal principal = cert.getSubjectX500Principal();
        X500Name x500Name = new X500Name( principal.getName() );
        showCN.setText(CertUtils.getRDN(x500Name, BCStyle.CN));
        showOU.setText(CertUtils.getRDN(x500Name, BCStyle.OU));
        showO.setText(CertUtils.getRDN(x500Name, BCStyle.O));
        showL.setText(CertUtils.getRDN(x500Name, BCStyle.L));
        showST.setText(CertUtils.getRDN(x500Name, BCStyle.ST));
        showC.setText(CertUtils.getRDN(x500Name, BCStyle.C));
        showE.setText(CertUtils.getRDN(x500Name, BCStyle.E));
    }

    @FXML void cancelClicked(ActionEvent actionEvent) {
        showKeySize.getScene().getWindow().hide();
    }


}
