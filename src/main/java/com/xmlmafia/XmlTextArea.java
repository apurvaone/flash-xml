package com.xmlmafia;

import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlTextArea extends StackPane {
    private final TextArea textArea;
    private final TextArea lineNumbers;
    private final XmlTokenizer tokenizer;
    private final TextFlow highlightingPane;

    public XmlTextArea(XmlTokenizer tokenizer) {
        this.tokenizer = tokenizer;

        // Create the main text area
        textArea = new TextArea();
        textArea.getStyleClass().add("xml-text-area");
        textArea.setWrapText(false);

        // Create line numbers text area
        lineNumbers = new TextArea();
        lineNumbers.getStyleClass().add("line-numbers");
        lineNumbers.setEditable(false);
        lineNumbers.setMouseTransparent(true);
        lineNumbers.setMaxWidth(60);
        lineNumbers.setWrapText(false);

        // Create highlighting pane
        highlightingPane = new TextFlow();
        highlightingPane.getStyleClass().add("highlighting-pane");
        highlightingPane.setMouseTransparent(true);

        // Layout
        VBox textContainer = new VBox();
        textContainer.setAlignment(Pos.TOP_LEFT);
        textContainer.getChildren().addAll(highlightingPane);

        // Stack the components
        getChildren().addAll(textArea, textContainer, lineNumbers);

        // Bind scroll positions
        textArea.scrollTopProperty().addListener((obs, old, newVal) -> {
            highlightingPane.setTranslateY(-newVal.doubleValue());
            lineNumbers.setScrollTop(newVal.doubleValue());
        });
        textArea.scrollLeftProperty().addListener((obs, old, newVal) -> {
            highlightingPane.setTranslateX(-newVal.doubleValue());
        });

        // Update highlighting when text changes
        textArea.textProperty().addListener((obs, old, newText) -> {
            updateHighlighting(newText);
            updateLineNumbers(newText);
        });

        // Set initial styles
        textArea.setStyle("-fx-font-family: 'Fira Code', 'JetBrains Mono', 'Cascadia Code', 'Source Code Pro', 'Consolas', monospace; -fx-font-size: 15px;");
        lineNumbers.setStyle("-fx-font-family: 'Fira Code', 'JetBrains Mono', 'Cascadia Code', 'Source Code Pro', 'Consolas', monospace; -fx-font-size: 13px;");
    }

    private void updateHighlighting(String text) {
        highlightingPane.getChildren().clear();
        String[] lines = text.split("\\R", -1);
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            List<XmlTokenizer.Token> tokens = tokenizer.tokenize(line);
            
            for (XmlTokenizer.Token token : tokens) {
                Text textNode = new Text(token.text);
                textNode.getStyleClass().add(token.styleClass);
                highlightingPane.getChildren().add(textNode);
            }
            
            if (i < lines.length - 1) {
                highlightingPane.getChildren().add(new Text("\\n"));
            }
        }
    }

    private void updateLineNumbers(String text) {
        int lineCount = text.split("\\R", -1).length;
        StringBuilder numbers = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            numbers.append(String.format("%4d%n", i));
        }
        lineNumbers.setText(numbers.toString());
    }

    public String getText() {
        return textArea.getText();
    }

    public void setText(String text) {
        textArea.setText(text);
    }

    public TextArea getTextArea() {
        return textArea;
    }
}
