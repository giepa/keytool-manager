package org.keytool.manager.main;

import com.cathive.fonts.fontawesome.FontAwesomeIcon;
import com.cathive.fonts.fontawesome.FontAwesomeIconView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import org.controlsfx.validation.*;
import org.controlsfx.validation.decoration.StyleClassValidationDecoration;
import org.controlsfx.validation.decoration.ValidationDecoration;
import org.keytool.manager.utils.Alerts;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author Gideon Maree
 * @since 15 May 2017
 */
public class Validation {

    private final Map<Control,Function<Control,Labeled>> lookup = new HashMap<>();
    private final ValidationSupport validationSupport = new ValidationSupport();
    private ChangeListener<? super ValidationResult> listener;

    public static Validation init(){
        return init(new StyleClassValidationDecoration());
    }

    public static Validation init(ValidationDecoration decor){
        Validation v = new Validation();
        v.getValidationSupport().setValidationDecorator(decor);
        return v;
    }

    public Validation notEmpty(Control c, String msg, String labelId){
        return register(c, Validator.createEmptyValidator(msg), labelId);
    }

    public Validation notEmpty(Control c, String msg, Labeled label){
        return register(c, Validator.createEmptyValidator(msg), label);
    }

    public Validation equals(Control c, String msg, Collection vals, String labelId){
        return register(c, Validator.createEqualsValidator(msg, vals), labelId);
    }

    public Validation equals(Control c, String msg, Collection vals, Labeled label){
        return register(c, Validator.createEqualsValidator(msg, vals), label);
    }

    public Validation predicate(Control c, String msg, Predicate pre, String labelId){
        return register(c, Validator.createPredicateValidator(pre, msg), labelId);
    }

    public Validation predicate(Control c, String msg, Predicate pre, Labeled label){
        return register(c, Validator.createPredicateValidator(pre, msg), label);
    }

    public Validation regex(Control c, String msg, Pattern regex, String labelId){
        return register(c, Validator.createRegexValidator(msg, regex, Severity.ERROR), labelId);
    }

    public Validation regex(Control c, String msg, Pattern regex, Labeled label){
        return register(c, Validator.createRegexValidator(msg, regex, Severity.ERROR), label);
    }

    public Validation between(Control c, String msg, Number c1, Number c2, String labelId){
        return predicate(c, msg, between(c1, c2), labelId);
    }

    public Validation between(Control c, String msg, Number c1, Number c2, Labeled label){
        return predicate(c, msg, between(c1, c2), label);
    }

    private Predicate between(Number c1, Number  c2){
        return v -> {
            Number val;
            if(v instanceof Number ){
                val = (Number) v;
            }else{
                val = Double.parseDouble(v.toString());
            }
            boolean rt = val.doubleValue() >= c1.doubleValue() && val.doubleValue() <= c2.doubleValue();
            return rt;
        };
    }

    public <T> Validation register(Control c, Validator<T> validator, String labelId) {
        if(validationSupport.registerValidator(c, validator)){
            lookup.put(c,control -> {
                Node node = control.getScene().lookup(labelId);
                if(node instanceof Labeled){
                    return (Labeled)node;
                } else {
                    return null;
                }
            });

        }
        return this;
    }

    public <T> Validation register(Control c, Validator<T> validator, Labeled label) {
        if(validationSupport.registerValidator(c, validator)){
            lookup.put(c,control -> label);
        }
        return this;
    }

    public boolean validate(){
        if(listener == null){
            listener = (observable, oldValue, newValue) -> showMessages();
            validationSupport.validationResultProperty().addListener(listener);
            showMessages();
        }
        return ! validationSupport.isInvalid();
    }

    private void showMessages(){
        lookup.keySet().forEach(control -> {
            showMessage(control, validationSupport.getHighestMessage(control));
        });
        Alerts.error(validationSupport);
    }

    private void showMessage(Control control, Optional<ValidationMessage> msg){
        Labeled label = lookup.get(control).apply(control);
        if(label == null){
            Alerts.error(control, "Could not find label for control");
            return;
        }
        label.setVisible(msg.isPresent());
        label.setManaged(msg.isPresent());
        msg.ifPresent(m -> {
            label.setGraphic(getIcon(m));
            label.setText(m.getText());
        });
    }

    private FontAwesomeIconView getIcon(ValidationMessage msg){
        FontAwesomeIconView icon = new FontAwesomeIconView();
        switch(msg.getSeverity()){
            case ERROR:
                icon.setIcon(FontAwesomeIcon.ICON_EXCLAMATION_SIGN);
                break;
            case WARNING:
                icon.setIcon(FontAwesomeIcon.ICON_WARNING_SIGN);
                break;
            default:
                return null;
        }
        return icon;
    }

    public ValidationSupport getValidationSupport() {
        return validationSupport;
    }
}
