package org.keytool.manager.main;

import com.google.inject.Singleton;
import javafx.beans.binding.Binding;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.pkcs.ContentInfo;
import org.bouncycastle.asn1.pkcs.SignedData;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.*;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.PEMUtil;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.io.pem.PemObject;
import org.fxmisc.easybind.EasyBind;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Gideon Maree
 * @since 11 May 2017
 */
@Singleton
public class KeystoreManager {
    private final Provider bcProvider;

    private final ObjectProperty<KeyStore> keystore = new SimpleObjectProperty<>();
    private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
    private final BooleanProperty isUnsaved = new SimpleBooleanProperty(false);
    private final ObservableList<Entry> entries = FXCollections.observableArrayList();
    private final ObjectProperty<Entry> selectedEntry = new SimpleObjectProperty<>();
    private final ObjectProperty<X509Certificate> selectedCert = new SimpleObjectProperty<>();

    public KeystoreManager(){
        bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);
    }

    public void create(String type) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore ks = KeyStore.getInstance(type);
        ks.load(null);
        keystore.set(ks);
        path.set(null);
        loadEntries();
        isUnsaved.set(true);
    }

    public void load(Path p, String password) throws IOException, KeyStoreException {
        KeyStore ks = null;
        Exception error = null;
        try(InputStream in = Files.newInputStream(p)) {
            ks = load("JKS", in, password);
        }catch(Exception e){
            error = e;
        }
        if(error != null){
            throw new IllegalStateException(error);
        }
        path.set(p);
        keystore.set(ks);
        loadEntries();
        isUnsaved.set(false);
    }

    private KeyStore load(String type, InputStream in, String password) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance(type);
        ks.load(in, password.toCharArray());
        return ks;
    }

    public void saveAs(File file, String password) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        path.set(file.toPath());
        save(password);
    }

    public void save(String password) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        if(path.isNotNull().get()){
            save(path.get(),password);
        }
    }

    public void save(Path path, String password) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        keystore.get().store(Files.newOutputStream(path), password.toCharArray());
        this.path.set(path);
        isUnsaved.set(false);
        loadEntries();
    }

    public ObservableList<Entry> entries(){
        return entries;
    }

    private void loadEntries() throws KeyStoreException {
        entries.clear();
        entries.addAll(Collections
                .list(keystore.get().aliases())
                .stream()
                .map(alias -> new Entry(alias, keystore.get()))
                .collect(Collectors.toList())
        );
    }

    public PrivateKey getPrivateKey(String alias, String password) throws NoSuchAlgorithmException, InvalidKeySpecException, KeyStoreException, UnrecoverableKeyException {
        return (PrivateKey) keystore.get().getKey(alias, password.toCharArray());
    }

    private KeyPair getKeyPair(String alias, String password) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        Key key = keystore.get().getKey(alias, password.toCharArray());
        if (key instanceof PrivateKey) {
            PublicKey publicKey = keystore.get().getCertificate(alias).getPublicKey();
            return new KeyPair(publicKey, (PrivateKey) key);
        }else{
            throw new IllegalStateException("Unexpected key type"+ key.getClass().getName());
        }
    }

    public void generateKeyPair(String alias, String password, String subject, int size, String keyAlg, String sigAlg) throws NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException, KeyStoreException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyAlg);
        keyGen.initialize(size);
        KeyPair key = keyGen.generateKeyPair();
        X509Certificate cert = selfSign(key, subject, sigAlg);
        keystore.get().setKeyEntry(alias, key.getPrivate(), password.toCharArray(), new X509Certificate[]{cert});
        Entry entry = new Entry(alias, keystore.get());
        entries.add(entry);
        selectedEntry.set(entry);
        isUnsaved.setValue(true);
    }

    private X509Certificate selfSign(KeyPair keyPair, String subjectDN, String signatureAlgorithm) throws OperatorCreationException, CertificateException, IOException {
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);

        X500Name dnName = new X500Name(subjectDN);
        BigInteger certSerialNumber = new BigInteger(Long.toString(now)); // <-- Using the current timestamp as the certificate serial number

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.YEAR, 1); // <-- 1 Yr validity
        Date endDate = calendar.getTime();

        AlgorithmIdentifier algorithmIdentifier = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm);
        SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo(algorithmIdentifier, keyPair.getPublic().getEncoded());
        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
                dnName,
                certSerialNumber,
                startDate,
                endDate,
                dnName,
                subjectPublicKeyInfo
        );

        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).setProvider(bcProvider).build(keyPair.getPrivate());
        X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);
        X509Certificate selfSignedCert = new JcaX509CertificateConverter().getCertificate(certificateHolder);

        return selfSignedCert;
    }

    public String generateCsr(
            String alias,
            String password,
            String name,
            String sigAlg
    ) throws Exception {
        KeyPair pair = getKeyPair(alias, password);
        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                new X500Principal(name),
                pair.getPublic()
        );
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(sigAlg);
        ContentSigner signer = csBuilder.build(pair.getPrivate());

        PKCS10CertificationRequest csr = p10Builder.build(signer);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PEMWriter pem = new PEMWriter(new OutputStreamWriter(output));
        pem.writeObject(csr);
        pem.close();
        return new String(output.toByteArray());
    }

    public X509Certificate getCertificate(String alias) throws KeyStoreException {
        X509Certificate cert = (X509Certificate) keystore.get().getCertificate(alias);
        return cert;
    }

    public X500Name getX500Name(String alias) throws KeyStoreException {
        X509Certificate cert = getCertificate(alias);
        X500Principal principal = cert.getSubjectX500Principal();
        return new X500Name( principal.getName() );
    }

    public void importChain(String alias, String password, String chainText) throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, CMSException, IOException {
        X509Certificate[] certs;
        certs = getChain(chainText);
        PrivateKey key = this.getPrivateKey(alias, password);
        keystore.get().setKeyEntry(alias, key, password.toCharArray(), certs);
        isUnsaved.setValue(true);
        refresh(alias);
    }

    public X509Certificate[] getChain(String content) throws CertificateException, CMSException, IOException {
        List<X509Certificate> rt = new ArrayList<>();
        content = content.trim();
        PEMParser pemReader = new PEMParser(new StringReader(content));
        Object object = pemReader.readObject();
        while(object != null) {
            if (object instanceof org.bouncycastle.asn1.cms.ContentInfo) {
                rt.addAll(getChain((org.bouncycastle.asn1.cms.ContentInfo) object));
            } else if (object instanceof X509CertificateHolder) {
                rt.add(getCert((X509CertificateHolder) object));
            } else {
                System.out.println(object.getClass().getName());
            }
            object = pemReader.readObject();
        }
        return rt.toArray(new X509Certificate[rt.size()]);
    }

    public X509Certificate getCert(X509CertificateHolder holder){
        try {
            return new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(holder);
        } catch (CertificateException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<X509Certificate> getChain(org.bouncycastle.asn1.cms.ContentInfo object) throws CMSException, CertificateException, IOException {
        CMSSignedData data = new CMSSignedData(object);
        Store certStore = data.getCertificates();
        Collection<X509CertificateHolder>matches = certStore.getMatches(null);
        return matches.stream()
            .map(this::getCert)
            .collect(Collectors.toList());
    }

    public void refresh(String alias){
        for(int i = 0; i < entries.size(); i++){
            Entry e = entries.get(i);
            if(e.alias.equals(alias)){
                entries.set(i, new Entry(alias,keystore.get()));
                return;
            }
        }
        isUnsaved.setValue(true);
    }

    public Stream<Certificate> getCertChain(String alias) throws KeyStoreException {
        return Stream.of(keystore.get().getCertificateChain(alias));
    }

    public BooleanBinding hasPath(){
        return path.isNotNull();
    }

    public BooleanBinding isLoaded(){
        return keystore.isNotNull();
    }

    public ObjectProperty<KeyStore> keyStoreProperty(){
        return keystore;
    }

    public ObjectProperty<Path> pathProperty() {
        return path;
    }

    public BooleanProperty isUnsavedProperty() {
        return isUnsaved;
    }

    public ObjectProperty<Entry> selectedEntryProperty() {
        return selectedEntry;
    }

    public ObjectProperty<X509Certificate> selectedCertProperty() {
        return selectedCert;
    }

    public Binding<Boolean> selectedIsKey(){
        return EasyBind.map(selectedEntry, entry -> {
            System.out.println(entry == null);
            return ! (entry != null && entry.isKey);
        });
    }

    public Binding<String> fileNameProperty(){
        return EasyBind.map(path, p -> {
            if(p == null) return "";
            return p.getFileName().toString();
        });
    }

    public class Entry{

        public final String alias;
        public final Boolean isKey;
        public final Boolean isCert;
        public final Date created;

        public Entry(String alias, KeyStore ks){
            this.alias = alias;
            try {
                this.isKey = ks.isKeyEntry(alias);
                this.isCert = ks.isCertificateEntry(alias);
                this.created = ks.getCreationDate(alias);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        public Boolean getCert() {
            return isCert;
        }

        public Boolean getKey() {
            return isKey;
        }

        public Date getCreated() {
            return created;
        }

        public String getAlias() {
            return alias;
        }

        @Override
        public String toString() {
            return alias;
        }
    }
}
