package org.keytool.manager.utils;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.IETFUtils;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Gideon Maree
 * @since 19 May 2017
 */
public class CertUtils {

    public static String getRDN(X500Name x500name, ASN1ObjectIdentifier id){
        RDN[] rdn = x500name.getRDNs(id);
        if(rdn == null) return "";
        if(rdn.length == 0) return "";
        return StringUtils.defaultIfEmpty(IETFUtils.valueToString(rdn[0].getFirst().getValue()), "");
    }

    public static String getRDN(X509Certificate cert, ASN1ObjectIdentifier id){
        return getRDN(getX500Name(cert), id);
    }

    public static  X500Name getX500Name(X509Certificate cert){
        X500Principal principal = cert.getSubjectX500Principal();
        return new X500Name( principal.getName() );
    }

    public static List<String> getLocales(){
        List<String> locales = Stream.of(Locale.getISOCountries())
                .collect(Collectors.toList());
        List<String> locales2 = Stream.of(Locale.getISOCountries())
                .map(s -> s.toLowerCase())
                .collect(Collectors.toList());
        locales.addAll(locales2);
        return locales;
    }

    public static String getKeySize(PublicKey k){
        if(k.getAlgorithm().equalsIgnoreCase("RSA")){
            RSAPublicKey rsaPk = (RSAPublicKey) k;
           return Integer.toString(rsaPk.getModulus().bitLength());
        }
        return "";
    }
}
