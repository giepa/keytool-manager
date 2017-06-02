package org.keytool.manager.main;

import com.cathive.fonts.fontawesome.FontAwesomeIcon;
import com.cathive.fonts.fontawesome.FontAwesomeIconView;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.fxmisc.easybind.EasyBind;
import org.keytool.manager.utils.Alerts;
import org.keytool.manager.utils.CertUtils;
import org.keytool.manager.utils.GuiceFXMLLoader;
import org.keytool.manager.utils.TableViewSelectedItem;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @author Gideon Maree
 * @since 11 May 2017
 */
@Singleton
public class MainController implements Initializable {

    @Inject
    Application application;
    @Inject
    FileChooser fileChooser;
    @Inject
    KeystoreManager keystoreManager;
    @Inject
    Stage stage;
    @Inject
    GuiceFXMLLoader fxmlLoader;
    @Inject
    EnterPasswordPopup passwordPopup;

    @FXML
    MenuButton newKeyStore;
    @FXML
    Button saveKeyStore;
    @FXML
    Button saveAs;
    @FXML
    Button openKeyStore;
    @FXML
    Button newKeyPair;
    @FXML
    Button generateCsr;
    @FXML
    Button importCert;
    @FXML
    Button viewDetails;
    @FXML
    TreeView entriesView;
    @FXML
    TreeItem rootItem;

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        saveKeyStore.disableProperty().bind(keystoreManager.isUnsavedProperty().not());
        saveAs.disableProperty().bind(keystoreManager.hasPath());
        newKeyPair.disableProperty().bind(keystoreManager.isLoaded().not());
        entriesView.disableProperty().bind(keystoreManager.isLoaded().not());
        entriesView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        generateCsr.disableProperty().bind(keystoreManager.selectedIsKey());
        importCert.disableProperty().bind(keystoreManager.selectedIsKey());
        viewDetails.disableProperty().bind(keystoreManager.selectedCertProperty().isNull());
        keystoreManager.entries().addListener((ListChangeListener<KeystoreManager.Entry>) c -> {
            rootItem.getChildren().clear();
            c.getList().stream()
                    .map(this::getItem)
                    .forEach(rootItem.getChildren()::add);
        });
        entriesView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        entriesView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null){
                keystoreManager.selectedEntryProperty().set(null);
                keystoreManager.selectedCertProperty().set(null);
                return;
            }
            TreeItem item = (TreeItem) newValue;
            if(item.getValue() instanceof KeystoreManager.Entry){
                keystoreManager.selectedEntryProperty().set((KeystoreManager.Entry) item.getValue());
                keystoreManager.selectedCertProperty().set(null);
            } else if(item.getValue() instanceof CertItem){
                keystoreManager.selectedCertProperty().set((X509Certificate) ((CertItem) item.getValue()).cert);
                TreeItem parent = item.getParent();
                while(! (parent.getValue() instanceof KeystoreManager.Entry)){
                    parent = parent.getParent();
                }
                keystoreManager.selectedEntryProperty().set((KeystoreManager.Entry) parent.getValue());
            }
        });
        application.getParameters()
                .getRaw().stream()
                .findFirst()
                .map(Paths::get)
                .filter(Files::exists)
                .ifPresent(this::openKeyStore);
    }

    private TreeItem getItem(KeystoreManager.Entry e) {
        try {
            if (e.isKey) {
                FontAwesomeIconView icon = new FontAwesomeIconView();
                TreeItem<KeystoreManager.Entry> item = new TreeItem<>();
                icon.setIcon(FontAwesomeIcon.ICON_KEY);
                item.setGraphic(icon);
                item.setValue(e);
                keystoreManager.getCertChain(e.alias)
                        .map(c -> (X509Certificate) c)
                        .map(this::getItem)
                        .forEachOrdered(i -> addItem(item, i));
                item.setExpanded(true);
                return item;
            } else if (e.isCert) {
                return getItem(keystoreManager.getCertificate(e.alias));
            }
            return null;
        }catch(Exception ex ){
            throw new IllegalStateException(ex);
        }
    }

    private TreeItem<CertItem> getItem(Certificate c){
        X509Certificate cert = (X509Certificate) c;
        TreeItem<CertItem> rt = new TreeItem(new CertItem(cert));
        FontAwesomeIconView icon = new FontAwesomeIconView();
        icon.setIcon(FontAwesomeIcon.ICON_CERTIFICATE);
        rt.setGraphic(icon);
        rt.setExpanded(true);
        return rt;
    }

    public void addItem(TreeItem root, TreeItem<CertItem> item){
        if(root.getChildren().isEmpty()){
            root.getChildren().add(item);
        } else {
            addItem((TreeItem) root.getChildren().get(0), item);
        }
    }

    public class CertItem {

        public final X509Certificate cert;

        public CertItem(X509Certificate cert) {
            this.cert = cert;
        }

        @Override
        public String toString() {
            return CertUtils.getRDN(cert, BCStyle.CN);
        }
    }

    @FXML
    public void openKeyStoreClicked(ActionEvent actionEvent) {
        showOpenDialog().ifPresent(path -> {
            openKeyStore(path);
        });
    }

    private void openKeyStore(Path path){
        passwordPopup.show("Please enter the keystore password")
                .ifPresent(pwd -> {
                    try {
                        keystoreManager.load(path, pwd);
                    } catch (Exception e) {
                        Alerts.error(openKeyStore, e);
                    }
                });
    }

    @FXML
    public void newJKSKeyStoreClicked(ActionEvent actionEvent) {
        try {
            keystoreManager.create("JKS");
        } catch (Exception e) {
            Alerts.error(newKeyStore, e);
        }
    }

    @FXML
    public void newKeyPairClicked(ActionEvent actionEvent) {
        fxmlLoader.popup(
                "/org/keytool/manager/main/GenKeyPair.fxml",
                "Generate new Key Pair",
                800,
                600
        ).showAndWait();
    }

    @FXML
    public void saveKeyStoreClicked(ActionEvent actionEvent) {
        Optional<Path> path;
        if(keystoreManager.hasPath().not().get()) {
            path = showSaveDialog();
        }else{
            path = Optional.of(keystoreManager.pathProperty().get());
        }
        path.ifPresent(p -> {
            passwordPopup.show("Please enter the keystore password")
                    .ifPresent(pwd -> {
                        try {
                            keystoreManager.save(p, pwd);
                        } catch (Exception e) {
                            Alerts.error(openKeyStore, e);
                        }
                    });
        });
    }

    @FXML
    public void saveAsClicked(ActionEvent actionEvent) {
        showSaveDialog().ifPresent(path -> {
            passwordPopup.show("Please enter the keystore password")
                    .ifPresent(pwd -> {
                        try {
                            keystoreManager.save(path, pwd);
                        } catch (Exception e) {
                            Alerts.error(openKeyStore, e);
                        }
                    });
        });
    }

    private Optional<Path> showSaveDialog(){
        return Optional.ofNullable(fileChooser.showSaveDialog(stage))
                .map(f -> f.toPath());
    }

    private Optional<Path> showOpenDialog(){
        return Optional.ofNullable(fileChooser.showOpenDialog(stage))
                .filter(f -> f.exists())
                .map(f -> f.toPath());
    }

    @FXML
    public void generateCsr(ActionEvent actionEvent) {
        fxmlLoader.popup(
                "/org/keytool/manager/main/GenCsr.fxml",
                "Generate CSR",
                800,
                600
        ).showAndWait();
    }

    @FXML
    public void viewDetails(ActionEvent actionEvent) {
        fxmlLoader.popup(
                "/org/keytool/manager/main/CertDetails.fxml",
                "View Certificate",
                800,
                600
        ).showAndWait();
    }

    public void importCert(ActionEvent actionEvent) {
        fxmlLoader.popup(
                "/org/keytool/manager/main/ImportCert.fxml",
                "Import Certificate Chain",
                800,
                600
        ).showAndWait();

    }


    public class IconCell extends TableCell<KeystoreManager.Entry, KeystoreManager.Entry> {

        private final HBox icon = new HBox();
        private final FontAwesomeIconView keyIcon = new FontAwesomeIconView();
        private final FontAwesomeIconView certIcon = new FontAwesomeIconView();

        public IconCell(){
            certIcon.setIcon(FontAwesomeIcon.ICON_CERTIFICATE);
            keyIcon.setIcon(FontAwesomeIcon.ICON_KEY);
            icon.getChildren().addAll(keyIcon,certIcon);
        }

        @Override
        protected void updateItem(KeystoreManager.Entry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (entry == null) {
                setGraphic(null);
                return;
            }
            certIcon.setVisible(entry.isCert);
            keyIcon.setVisible(entry.isKey);
            setGraphic(icon);
        }
    }
}
