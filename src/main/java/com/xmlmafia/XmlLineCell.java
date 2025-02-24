package com.xmlmafia;

import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.geometry.Pos;
import java.util.List;

public class XmlLineCell extends ListCell<String> {
    private final XmlTokenizer tokenizer;
    private final TextFlow textFlow;
    private final Text lineNumber;
    private final HBox container;

    public XmlLineCell(XmlTokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.textFlow = new TextFlow();
        this.lineNumber = new Text();
        this.lineNumber.getStyleClass().add("line-number");
        
        this.container = new HBox();
        this.container.setAlignment(Pos.CENTER_LEFT);
        this.container.getChildren().addAll(lineNumber, textFlow);
        
        setGraphic(container);
    }

    @Override
    protected void updateItem(String line, boolean empty) {
        super.updateItem(line, empty);

        if (empty || line == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        // Set line number (1-based index)
        lineNumber.setText(String.format("%4d │ ", getIndex() + 1));
        
        textFlow.getChildren().clear();
        List<XmlTokenizer.Token> tokens = tokenizer.tokenize(line);
        
        for (XmlTokenizer.Token token : tokens) {
            Text text = new Text(token.text);
            text.getStyleClass().add(token.styleClass);
            textFlow.getChildren().add(text);
        }
        
        setGraphic(container);
    }
}
