package org.keytool.manager.utils;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableView;

/**
 * @author Gideon Maree
 * @since 17 May 2017
 */
public class TableViewSelectedItem<T> extends SimpleObjectProperty<T>{

    public TableViewSelectedItem(TableView.TableViewSelectionModel<T> model){
        super();
        model.selectedItemProperty().addListener((observable, oldValue, newValue) -> set(newValue));
        this.addListener((observable, oldValue, newValue) -> model.select(newValue));
    }

    public static <TT> ObjectProperty<TT> bind(TableView<TT> table){
        return new TableViewSelectedItem(table.getSelectionModel());
    }
}
