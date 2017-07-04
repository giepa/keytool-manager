package org.keytool.manager.utils;

import com.cathive.fonts.fontawesome.FontAwesomeIcon;
import com.cathive.fonts.fontawesome.FontAwesomeIconView;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import org.keytool.manager.main.KeystoreManager;

/**
 * @author Gideon Maree
 * @since 03 Jul 2017
 */
public class Icons {

    public static Node icon(FontAwesomeIcon i){
        FontAwesomeIconView icon = new FontAwesomeIconView();
        icon.setIcon(i);
        return icon;
    }
}
