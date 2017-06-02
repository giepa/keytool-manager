package org.keytool.manager.utils;

import javafx.scene.control.TextField;

/**
 * @author Gideon Maree
 * @since 26 May 2017
 */
public class X509Builder {

    StringBuilder b = new StringBuilder();

    public X509Builder appendPart(String part, String val){
        if(!val.trim().isEmpty()){
            b.append(part+"="+val.trim());
        }
        return this;
    }

    @Override
    public String toString() {
        return b.toString();
    }

    public static X509Builder init(){
        return new X509Builder();
    }
}
