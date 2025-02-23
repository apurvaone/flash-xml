package com.xmlmafia;

import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.geometry.Orientation;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;

public class XmlViewerController {
    private static final Logger logger = LoggerFactory.getLogger(XmlViewerController.class);
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    private static final int BUFFER_SIZE = 8192;
    private static final int INITIAL_VISIBLE_LINES = 100;
    private static final int INDEX_INTERVAL = 1000; // Store position every 1000 lines
    
    private final ListView<String> xmlListView;
    private final ExecutorService executor;
    private final XmlTokenizer tokenizer;
    private final AtomicLong totalLines;
    private final ObservableList<String> items;
    private final Map<Integer, Long> linePositionIndex;
    private final ConcurrentHashMap<Integer, String> lineCache;
    private final LinkedHashMap<Integer, String> recentLines;
    private static final int MAX_CACHE_SIZE = 10000;
    
    private MappedByteBuffer mappedBuffer;
    private FileChannel fileChannel;
    private volatile boolean isLoading;
    private long fileSize;
    
    public XmlViewerController(ListView<String> xmlListView) {
        this.xmlListView = xmlListView;
        this.executor = Executors.newFixedThreadPool(2);
        this.tokenizer = new XmlTokenizer();
        this.totalLines = new AtomicLong(0);
        this.items = FXCollections.observableArrayList();
        this.linePositionIndex = new HashMap<>();
        this.lineCache = new ConcurrentHashMap<>();
        this.recentLines = new LinkedHashMap<Integer, String>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
        this.xmlListView.setItems(items);
        
        setupListView();
    }
    
    private void setupListView() {
        xmlListView.setCellFactory(list -> new XmlLineCell(tokenizer));
        
        xmlListView.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                xmlListView.lookupAll(".scroll-bar").stream()
                    .filter(node -> node instanceof ScrollBar)
                    .map(node -> (ScrollBar) node)
                    .filter(scrollBar -> scrollBar.getOrientation() == Orientation.VERTICAL)
                    .findFirst()
                    .ifPresent(scrollBar -> {
                        scrollBar.valueProperty().addListener((obs2, oldVal, newVal) -> {
                            if (!isLoading) {
                                int index = (int) (newVal.doubleValue() * items.size());
                                prefetchLines(index);
                            }
                        });
                    });
            }
        });
    }
    
    public void loadFile(File file) {
        isLoading = true;
        Platform.runLater(() -> {
            items.clear();
            xmlListView.setItems(null); // Temporarily detach items
        });
        
        lineCache.clear();
        linePositionIndex.clear();
        
        executor.submit(() -> {
            try {
                if (fileChannel != null) {
                    fileChannel.close();
                }
                
                fileChannel = new RandomAccessFile(file, "r").getChannel();
                fileSize = fileChannel.size();
                mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
                
                // Build line index and count lines
                buildLineIndex();
                
                // Create placeholder items
                Platform.runLater(() -> {
                    for (int i = 0; i < totalLines.get(); i++) {
                        items.add(null);
                    }
                    xmlListView.setItems(items); // Reattach items
                });
                
                // Load initial viewport content
                loadInitialView();
                isLoading = false;
                
            } catch (Exception e) {
                logger.error("Error loading file: " + file.getName(), e);
                isLoading = false;
            }
        });
    }
    
    private void loadInitialView() {
        int initialLines = Math.min(INITIAL_VISIBLE_LINES, (int) totalLines.get());
        
        for (int i = 0; i < initialLines; i++) {
            final int index = i;
            String line = readLine(index);
            if (line != null) {
                lineCache.put(index, line);
                Platform.runLater(() -> {
                    if (index < items.size()) {
                        items.set(index, line);
                    }
                });
            }
        }
    }
    
    private void buildLineIndex() {
        long position = 0;
        long lineCount = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        mappedBuffer.position(0);
        
        // Store the start position
        linePositionIndex.put(0, 0L);
        
        while (mappedBuffer.hasRemaining()) {
            int bytesRead = Math.min(mappedBuffer.remaining(), BUFFER_SIZE);
            mappedBuffer.get(buffer, 0, bytesRead);
            
            for (int i = 0; i < bytesRead; i++) {
                if (buffer[i] == '\n') {
                    lineCount++;
                    if (lineCount % INDEX_INTERVAL == 0) {
                        linePositionIndex.put((int)lineCount, position + i + 1);
                    }
                }
            }
            position += bytesRead;
        }
        
        totalLines.set(lineCount + 1);
        mappedBuffer.position(0);
    }
    
    private void prefetchLines(int index) {
        if (isLoading || index < 0 || index >= items.size()) {
            return;
        }
        
        executor.submit(() -> {
            try {
                int start = Math.max(0, index - 50);
                int end = Math.min(items.size(), index + 150);
                
                for (int i = start; i < end; i++) {
                    final int lineNum = i;
                    String currentLine = items.get(lineNum);
                    if (currentLine == null) {
                        String line = readLine(lineNum);
                        if (line != null) {
                            lineCache.put(lineNum, line);
                            Platform.runLater(() -> {
                                if (lineNum < items.size()) {
                                    items.set(lineNum, line);
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error in prefetch", e);
            }
        });
    }
    
    private String readLine(int lineNumber) {
        // Check cache first
        String cachedLine = lineCache.get(lineNumber);
        if (cachedLine != null) {
            return cachedLine;
        }
        
        try {
            // Find nearest indexed position
            int indexedLine = lineNumber - (lineNumber % INDEX_INTERVAL);
            long startPosition = linePositionIndex.getOrDefault(indexedLine, 0L);
            
            // Read from the nearest indexed position
            mappedBuffer.position((int)startPosition);
            StringBuilder line = new StringBuilder();
            int currentLine = indexedLine;
            
            while (mappedBuffer.hasRemaining() && currentLine <= lineNumber) {
                char c = (char) mappedBuffer.get();
                if (c == '\n') {
                    if (currentLine == lineNumber) {
                        String result = line.toString();
                        lineCache.put(lineNumber, result);
                        synchronized(recentLines) {
                            recentLines.put(lineNumber, result);
                        }
                        return result;
                    }
                    currentLine++;
                    line.setLength(0);
                } else if (currentLine == lineNumber) {
                    line.append(c);
                }
            }
            
            // Handle last line if it doesn't end with newline
            if (line.length() > 0 && currentLine == lineNumber) {
                String result = line.toString();
                lineCache.put(lineNumber, result);
                synchronized(recentLines) {
                    recentLines.put(lineNumber, result);
                }
                return result;
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error reading line: " + lineNumber, e);
            return null;
        }
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (fileChannel != null) {
                fileChannel.close();
            }
        } catch (Exception e) {
            logger.error("Error closing file channel", e);
        }
    }
}
