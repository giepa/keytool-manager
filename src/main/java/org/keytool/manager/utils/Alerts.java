package org.keytool.manager.utils;

import com.cathive.fonts.fontawesome.FontAwesomeIcon;
import com.cathive.fonts.fontawesome.FontAwesomeIconView;
import com.sun.jmx.remote.security.NotificationAccessController;
import impl.org.controlsfx.skin.DecorationPane;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Control;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.validation.ValidationSupport;

import java.util.Optional;

/**
 * @author Gideon Maree
 * @since 11 May 2017
 */
public class Alerts {

    public static void error(ValidationSupport validation){
        validation.getRegisteredControls()
                .forEach(control -> {
                    validation.getHighestMessage(control).ifPresent(msg -> {
                        switch(msg.getSeverity()){
                            case ERROR:
                                show(control, FontAwesomeIcon.ICON_EXCLAMATION_SIGN,msg.getText());
                                break;
                            case WARNING:
                                show(control, FontAwesomeIcon.ICON_WARNING_SIGN,msg.getText());
                                break;
                        }
                    });
                });

    }

    public static void info(Control control, String text){
        show(control, FontAwesomeIcon.ICON_INFO_SIGN, text);
    }

    public static void show(Control control, FontAwesomeIcon icon, String text){
        NotificationPane notification = (NotificationPane) control.getScene().lookup("#NotificationPane");
        if(notification != null){
            FontAwesomeIconView iconView = new FontAwesomeIconView();
            iconView.setIcon(icon);
            notification.show();
            notification.show(text, iconView);
        }else{
            System.out.println(control.getScene().getRoot().getClass().getName());
        }
    }

    public static void error(Control control, String msg){
        show(control, FontAwesomeIcon.ICON_EXCLAMATION_SIGN, msg);
    }

    public static void error(Control control, Throwable ex){
        show(control, FontAwesomeIcon.ICON_EXCLAMATION_SIGN, getMessage(ex));
    }

    private static String getMessage(Throwable ex){
        String msg = "Error " + ex.getClass().getSimpleName();
        if(ex.getMessage() != null){
            msg += ex.getMessage();
        }
        if(ex.getCause() != null){
            msg += " | " + getMessage(ex.getCause());
        }
        return msg;
    }

    public static Optional<String> showTextInput(String header, String prompt){
        TextInputDialog dialog = new TextInputDialog("walter");
        dialog.setTitle(header);
        dialog.setHeaderText(header);
        dialog.setContentText(prompt);
        return dialog.showAndWait();
    }
}
