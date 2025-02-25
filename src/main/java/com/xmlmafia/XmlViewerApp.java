package com.xmlmafia;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.text.Text;
import java.io.File;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

public class XmlViewerApp extends Application {
    private XmlViewerController controller;
    private ListView<String> xmlListView;
    private BorderPane root;
    private BooleanProperty isDarkTheme = new SimpleBooleanProperty(false);

    @Override
    public void start(Stage primaryStage) {
        initializeUI(primaryStage);
    }

    private void initializeUI(Stage primaryStage) {
        root = new BorderPane();
        
        // Create menu bar container
        HBox menuContainer = new HBox();
        menuContainer.setAlignment(Pos.CENTER_LEFT);
        menuContainer.getStyleClass().add("menu-container");
        
        // Create menu bar
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(false);
        
        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open XML File");
        fileMenu.getItems().add(openMenuItem);
        menuBar.getMenus().add(fileMenu);
        
        // Create theme toggle button
        ToggleButton themeToggle = new ToggleButton();
        themeToggle.setTooltip(new Tooltip("Toggle Dark/Light Theme"));
        themeToggle.getStyleClass().add("theme-toggle");
        
        // Create spacer to push theme toggle to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Set up theme toggle with text symbols
        Label sunIcon = new Label("☀");
        Label moonIcon = new Label("☾");
        sunIcon.getStyleClass().add("theme-icon");
        moonIcon.getStyleClass().add("theme-icon");
        
        themeToggle.setGraphic(sunIcon);
        themeToggle.selectedProperty().bindBidirectional(isDarkTheme);
        
        themeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            themeToggle.setGraphic(newVal ? moonIcon : sunIcon);
            root.getStyleClass().removeAll("light-theme", "dark-theme");
            root.getStyleClass().add(newVal ? "dark-theme" : "light-theme");
        });
        
        // Add all components to menu container
        menuContainer.getChildren().addAll(menuBar, spacer, themeToggle);
        
        // Set menu container as the top container
        VBox topContainer = new VBox();
        topContainer.getChildren().add(menuContainer);
        
        xmlListView = new ListView<>();
        controller = new XmlViewerController(xmlListView);
        
        // Create scroll buttons
        Button scrollUpButton = new Button("↑");
        Button scrollDownButton = new Button("↓");
        scrollUpButton.getStyleClass().add("scroll-button");
        scrollDownButton.getStyleClass().add("scroll-button");
        
        HBox scrollButtons = new HBox(10);
        scrollButtons.getChildren().addAll(scrollUpButton, scrollDownButton);
        scrollButtons.setAlignment(Pos.BOTTOM_RIGHT);
        scrollButtons.setPadding(new Insets(0, 20, 20, 0));
        scrollButtons.setMouseTransparent(false);
        scrollButtons.setPickOnBounds(false);
        
        // Create a stack pane to overlay the scroll buttons on the list view
        StackPane centerPane = new StackPane();
        centerPane.getChildren().addAll(xmlListView, scrollButtons);
        StackPane.setAlignment(scrollButtons, Pos.BOTTOM_RIGHT);
        
        // Ensure the ListView receives keyboard events
        xmlListView.setFocusTraversable(true);
        
        // Make sure mouse events pass through to the ListView when not on buttons
        scrollButtons.setPickOnBounds(false);
        centerPane.setPickOnBounds(false);
        
        root.setTop(topContainer);
        root.setCenter(centerPane);
        
        Scene scene = new Scene(root, 1200, 800);
        root.getStyleClass().add("light-theme");
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        // Setup file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open XML File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("XML Files", "*.xml", "*.cxml"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // Add open file action to menu item
        openMenuItem.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                controller.loadFile(file);
                primaryStage.setTitle("XML Mafia - " + file.getName());
            }
        });
        
        // Add scroll button actions
        scrollUpButton.setOnAction(e -> xmlListView.scrollTo(0));
        scrollDownButton.setOnAction(e -> xmlListView.scrollTo(xmlListView.getItems().size() - 1));
        
        primaryStage.setTitle("XML Mafia - High Performance XML Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
