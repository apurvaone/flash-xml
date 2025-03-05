# XML Mafia - The Ultimate High-Performance XML Viewer
**Because XML Deserves Respect.**


![xw5zZiXWbMxD8ang-generated_image (2)](https://github.com/user-attachments/assets/9ecad2a0-297b-4d80-b7f3-b10f1f27134a)



---

## 🚀 Blazing Fast. Uncompromisingly Smooth.

Tired of sluggish XML editors choking on large files? Meet **XML Mafia**—a high-performance XML viewer built to handle **gigabyte-scale XML files** with unmatched speed and efficiency. Whether you’re dealing with **massive datasets, complex CXML files, or deeply nested structures**, XML Mafia loads them **instantly**, without sacrificing syntax highlighting or smooth scrolling.

---

## 🎯 Key Features

XML Mafia achieves high performance through several key optimizations:

🔹 **Memory-Mapped I/O** – Loads gigabyte-scale files instantly, minimizing disk access overhead.  
🔹 **Virtualized Display** – Renders only the visible portion, reducing memory usage.  
🔹 **Real-Time Syntax Highlighting** – Ensures clear and readable XML content.  
🔹 **Lazy Loading & Background Processing** – Keeps the UI responsive while loading large files.  
🔹 **Pre-Fetching for Smooth Scrolling** – Eliminates stutters for a seamless browsing experience.  
🔹 **CXML Support** – Fully compatible with Commerce XML (CXML) formats.  
🔹 **Theme Support** – Dark and light themes for comfortable viewing.  
🔹 **Line Numbers** – Clear line numbering for easy reference.  

---

## 🛠️ System Requirements

🔹 Java 17 or higher  
🔹 Maven 3.6 or higher  
🔹 JavaFX runtime  

---

## ⚡ Quick Start

### 📦 Build the Project

```bash
mvn clean package
```

### ▶️ Run the Application

Using Maven:
```bash
mvn javafx:run
```

Using the shell script:
```bash
./src/main/resources/xml-mafia.sh
```

Directly using Java:
```bash
java --module-path /usr/share/openjfx/lib --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.graphics -jar target/xml-mafia.jar
```

---

## 🏗 Usage

1. Launch **XML Mafia** using any of the methods above.  
2. Click anywhere in the window to open the file chooser.  
3. Select an **XML** or **CXML** file.  
4. Instantly browse even the largest files with **syntax highlighting** and **smooth scrolling**.  
5. Use the theme toggle button to switch between **dark and light themes**.  
6. Scroll through the file smoothly with automatic content loading.  

---

## 🏎 Technical Features

### Performance Optimizations

- **Memory-Mapped I/O** for efficient file access  
- **Virtualized ListView** showing only visible content  
- **Background Thread** for file processing  
- **Efficient Tokenizer** for syntax highlighting  
- **Pre-Fetching** for smooth scrolling  
- **Minimal Memory Footprint** for handling large files  

### Key Components

- **XmlViewerApp**: Main application class that sets up the JavaFX UI  
- **XmlViewerController**: Handles file loading and viewing logic  
- **XmlTokenizer**: Provides XML syntax highlighting functionality  
- **XmlLineCell**: Custom cell implementation for efficient line rendering  

---

## 🛠 Contributing

XML Mafia is built for speed and efficiency, making it easier than ever to work with large XML files. But every great tool needs a great community to push it to the next level. Whether you're a developer, designer, tester, or just a power user with brilliant ideas, we want you on board!

### How You Can Help:

🔹 **Report Issues** – Found a bug? Let us know, and we’ll squash it like a true mafia family.  
🔹 **Suggest Features** – Have a killer idea to make XML Mafia even better? We’re all ears!  
🔹 **Contribute Code** – Fork the repo, add your magic, and submit a pull request.  
🔹 **Spread the Word** – Share XML Mafia with fellow developers, because no one should suffer slow XML viewers ever again!  

Join us in making XML Mafia the **fastest, smoothest, and most powerful XML editor in the world**. The family is growing—be a part of it! 💥

