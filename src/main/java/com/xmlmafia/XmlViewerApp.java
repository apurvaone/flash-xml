package com.xmlmafia;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;

public class XmlViewerApp extends Application {
    private XmlViewerController controller;
    private ListView<String> xmlListView;
    private BorderPane root;

    @Override
    public void start(Stage primaryStage) {
        initializeUI(primaryStage);
    }

    private void initializeUI(Stage primaryStage) {
        root = new BorderPane();
        
        // Create menu bar
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open");
        fileMenu.getItems().add(openMenuItem);
        menuBar.getMenus().add(fileMenu);
        
        // Create toolbar
        ToolBar toolBar = new ToolBar();
        Button openButton = new Button("Open XML File");
        openButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 15;");
        toolBar.getItems().add(openButton);
        
        // Create top container for menu and toolbar
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(menuBar, toolBar);
        
        xmlListView = new ListView<>();
        controller = new XmlViewerController(xmlListView);
        
        root.setTop(topContainer);
        root.setCenter(xmlListView);
        
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        // Setup file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open XML File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("XML Files", "*.xml", "*.cxml"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // Add open file action to both menu item and button
        Runnable openFileAction = () -> {
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                controller.loadFile(file);
                primaryStage.setTitle("XML Mafia - " + file.getName());
            }
        };
        
        openMenuItem.setOnAction(e -> openFileAction.run());
        openButton.setOnAction(e -> openFileAction.run());
        
        primaryStage.setTitle("XML Mafia - High Performance XML Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
