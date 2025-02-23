# XML Mafia - High Performance XML Viewer

A blazing fast XML viewer designed to handle gigabyte-scale XML files with ease. Built with JavaFX and optimized for performance using memory-mapped I/O and virtualized display.

## Features

- Memory-mapped file handling for efficient gigabyte-scale XML file viewing
- Virtualized display showing only visible content
- Real-time syntax highlighting
- Lazy loading and background processing
- Smooth scrolling and navigation
- Support for large XML and CXML files

## Requirements

- Java 17 or higher
- Maven 3.6 or higher

## Building the Project

```bash
mvn clean package
```

## Running the Application

```bash
mvn javafx:run
```

## Usage

1. Launch the application
2. Click anywhere in the window to open the file chooser
3. Select your XML or CXML file
4. The file will be loaded instantly and you can scroll through it with syntax highlighting

## Performance Features

- Memory-mapped I/O for efficient file access
- Virtualized ListView showing only visible content
- Background thread for file processing
- Efficient tokenizer for syntax highlighting
- Pre-fetching for smooth scrolling
- Minimal memory footprint

## Contributing

Feel free to submit issues and enhancement requests!
