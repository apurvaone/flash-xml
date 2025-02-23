package com.xmlmafia;

import javafx.scene.control.ListCell;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import java.util.List;

public class XmlLineCell extends ListCell<String> {
    private final XmlTokenizer tokenizer;
    private final TextFlow textFlow;

    public XmlLineCell(XmlTokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.textFlow = new TextFlow();
        setGraphic(textFlow);
    }

    @Override
    protected void updateItem(String line, boolean empty) {
        super.updateItem(line, empty);

        if (empty || line == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        textFlow.getChildren().clear();
        List<XmlTokenizer.Token> tokens = tokenizer.tokenize(line);
        
        for (XmlTokenizer.Token token : tokens) {
            Text text = new Text(token.text);
            text.getStyleClass().add(token.styleClass);
            textFlow.getChildren().add(text);
        }
        
        setGraphic(textFlow);
    }
}
