package com.xmlmafia;

import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
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
        
        // Set cell height to 28px for better vertical spacing
        final int CELL_HEIGHT = 28;
        
        this.container = new HBox();
        this.container.setAlignment(Pos.CENTER_LEFT);
        this.container.setSpacing(0);
        
        // Configure line number with initial width
        this.lineNumber.setWrappingWidth(60);
        this.lineNumber.setTextOrigin(VPos.CENTER);
        this.lineNumber.setTranslateY(2); // Fine-tune vertical position
        
        // Configure TextFlow
        this.textFlow.setMaxHeight(CELL_HEIGHT);
        this.textFlow.setPrefHeight(CELL_HEIGHT);
        this.textFlow.setMinHeight(CELL_HEIGHT);
        this.textFlow.setLineSpacing(0);
        this.textFlow.setTranslateY(2); // Fine-tune vertical position
        
        // Add components to container with padding
        this.container.getChildren().addAll(lineNumber, textFlow);
        
        // Set container height with padding
        this.container.setMaxHeight(CELL_HEIGHT);
        this.container.setPrefHeight(CELL_HEIGHT);
        this.container.setMinHeight(CELL_HEIGHT);
        this.container.setPadding(new javafx.geometry.Insets(2, 0, 0, 0));
        
        // Set cell properties
        setPrefHeight(CELL_HEIGHT);
        setMaxHeight(CELL_HEIGHT);
        setMinHeight(CELL_HEIGHT);
        setPadding(new javafx.geometry.Insets(0));
        setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    protected void updateItem(String line, boolean empty) {
        super.updateItem(line, empty);

        if (empty || line == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        // Calculate line number width based on total lines
        int totalLines = getListView().getItems().size();
        int lineNumberWidth = Math.max(60, String.valueOf(totalLines).length() * 10 + 20);
        lineNumber.setWrappingWidth(lineNumberWidth);

        // Format line number with proper width
        int digitCount = String.valueOf(totalLines).length();
        String lineNumberFormat = String.format("%%%dd │", digitCount);
        lineNumber.setText(String.format(lineNumberFormat, getIndex() + 1));
        lineNumber.setStyle("-fx-font-family: 'monospace';");
        
        textFlow.getChildren().clear();
        List<XmlTokenizer.Token> tokens = tokenizer.tokenize(line);
        
        for (XmlTokenizer.Token token : tokens) {
            Text text = new Text(token.text);
            text.getStyleClass().add(token.styleClass);
            text.setTextOrigin(VPos.CENTER);
            text.setTranslateY(2); // Fine-tune vertical position
            textFlow.getChildren().add(text);
        }
        
        setGraphic(container);
    }
}
