package org.keytool.manager.utils;

import javafx.scene.control.TextFormatter;

import java.text.DecimalFormat;
import java.text.ParsePosition;

/**
 * @author Gideon Maree
 * @since 17 May 2017
 */
public class DecimalTextFormatter extends TextFormatter<String > {

    public DecimalTextFormatter(DecimalFormat format){
        super(c -> {
            if ( c.getControlNewText().isEmpty() )
            {
                return c;
            }

            ParsePosition parsePosition = new ParsePosition( 0 );
            Object object = format.parse( c.getControlNewText(), parsePosition );

            if ( object == null || parsePosition.getIndex() < c.getControlNewText().length() )
            {
                return null;
            }
            else
            {
                return c;
            }
        });
    }
}
