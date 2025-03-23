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
import javafx.stage.Modality;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.scene.layout.GridPane;

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
        
        // Edit menu with Find option
        Menu editMenu = new Menu("Edit");
        MenuItem findMenuItem = new MenuItem("Find");
        findMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
        editMenu.getItems().add(findMenuItem);
        menuBar.getMenus().add(editMenu);
        
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
        
        // Add find action to menu item
        findMenuItem.setOnAction(e -> showFindDialog(primaryStage));
        
        // Add scroll button actions
        scrollUpButton.setOnAction(e -> xmlListView.scrollTo(0));
        scrollDownButton.setOnAction(e -> xmlListView.scrollTo(xmlListView.getItems().size() - 1));
        
        primaryStage.setTitle("XML Mafia - High Performance XML Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private void showFindDialog(Stage parentStage) {
        // Create dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Find");
        dialog.setHeaderText(null);
        dialog.initModality(Modality.NONE); // Allow interaction with main window
        dialog.initOwner(parentStage);
        
        // Set buttons
        ButtonType findButtonType = new ButtonType("Find", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().addAll(findButtonType, cancelButtonType);
        
        // Create the search field and buttons layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search text");
        searchField.setPrefWidth(300);
        
        Button findNextButton = new Button("▼");
        findNextButton.setTooltip(new Tooltip("Find Next"));
        
        Button findPrevButton = new Button("▲");
        findPrevButton.setTooltip(new Tooltip("Find Previous"));
        
        Label statusLabel = new Label("");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        
        CheckBox caseSensitiveCheck = new CheckBox("Case sensitive");
        
        grid.add(new Label("Find:"), 0, 0);
        grid.add(searchField, 1, 0);
        
        HBox navigationButtons = new HBox(5);
        navigationButtons.getChildren().addAll(findPrevButton, findNextButton);
        grid.add(navigationButtons, 2, 0);
        
        grid.add(caseSensitiveCheck, 1, 1);
        grid.add(statusLabel, 1, 2, 2, 1);
        
        // Enable/Disable find button depending on whether text was entered
        Node findButton = dialog.getDialogPane().lookupButton(findButtonType);
        findButton.setDisable(true);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            findButton.setDisable(newValue.trim().isEmpty());
        });
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on the search field by default
        Platform.runLater(searchField::requestFocus);
        
        // Prevent dialog from closing when Find button is clicked
        Button findDialogButton = (Button) findButton;
        findDialogButton.setOnAction(event -> {
            event.consume(); // Prevent default button action (closing dialog)
            String searchText = searchField.getText();
            if (!searchText.isEmpty()) {
                int count = controller.findNext(searchText, caseSensitiveCheck.isSelected());
                updateSearchStatus(statusLabel, count);
            }
        });
        
        // Handle the search for navigation buttons
        findNextButton.setOnAction(e -> {
            String searchText = searchField.getText();
            if (!searchText.isEmpty()) {
                int count = controller.findNext(searchText, caseSensitiveCheck.isSelected());
                updateSearchStatus(statusLabel, count);
            }
        });
        
        findPrevButton.setOnAction(e -> {
            String searchText = searchField.getText();
            if (!searchText.isEmpty()) {
                int count = controller.findPrevious(searchText, caseSensitiveCheck.isSelected());
                updateSearchStatus(statusLabel, count);
            }
        });
        
        // Add keyboard support for Enter key to search
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String searchText = searchField.getText();
                if (!searchText.isEmpty()) {
                    int count = controller.findNext(searchText, caseSensitiveCheck.isSelected());
                    updateSearchStatus(statusLabel, count);
                }
            }
        });
        
        // Show the dialog (non-blocking)
        dialog.show();
    }
    
    private void updateSearchStatus(Label statusLabel, int count) {
        if (count == 0) {
            statusLabel.setText("No matches found");
            statusLabel.getStyleClass().add("search-no-results");
        } else {
            statusLabel.setText(count + " matches found");
            statusLabel.getStyleClass().removeAll("search-no-results");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
