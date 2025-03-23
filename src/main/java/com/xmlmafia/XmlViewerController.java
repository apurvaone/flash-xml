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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    // Search related fields
    private final List<SearchResult> searchResults = new ArrayList<>();
    private final AtomicInteger currentSearchIndex = new AtomicInteger(-1);
    private String currentSearchText = "";
    private boolean currentCaseSensitive = false;
    private Future<?> searchTask;
    
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
                    .ifPresent(this::setupScrollBar);
            }
        });
    }
    
    private void setupScrollBar(ScrollBar scrollBar) {
        // Update scroll bar to represent full file size
        scrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isLoading) {
                double scrollPosition = newVal.doubleValue();
                handleScrollChange(scrollPosition);
            }
        });
    }
    
    private void handleScrollChange(double scrollPosition) {
        // Calculate the target line based on total lines and scroll position
        int targetLine = (int) (scrollPosition * totalLines.get());
        
        // Ensure we stay within bounds
        targetLine = Math.max(0, Math.min(targetLine, (int)totalLines.get() - 1));
        
        // Load content around the target line
        int windowSize = 100; // Number of lines to load above and below
        int startLine = Math.max(0, targetLine - windowSize);
        int endLine = Math.min((int)totalLines.get() - 1, targetLine + windowSize);
        
        loadLinesRange(startLine, endLine);
    }
    
    private void loadLinesRange(int startLine, int endLine) {
        executor.submit(() -> {
            try {
                for (int i = startLine; i <= endLine; i++) {
                    if (!lineCache.containsKey(i)) {
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
            } catch (Exception e) {
                logger.error("Error loading lines range: " + startLine + " to " + endLine, e);
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
        
        // Clear search results when loading a new file
        clearSearchResults();
        
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
                
                // Create placeholder items for the entire file
                Platform.runLater(() -> {
                    for (int i = 0; i < totalLines.get(); i++) {
                        items.add(null);
                    }
                    xmlListView.setItems(items);
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
    
    /**
     * Search class to represent search results
     */
    private static class SearchResult {
        final int lineNumber;
        final int position;
        final String lineText;
        
        SearchResult(int lineNumber, int position, String lineText) {
            this.lineNumber = lineNumber;
            this.position = position;
            this.lineText = lineText;
        }
    }
    
    /**
     * Performs a search for the next occurrence of text
     * @param searchText The text to search for
     * @param caseSensitive Whether the search is case sensitive
     * @return The number of matches found
     */
    public int findNext(String searchText, boolean caseSensitive) {
        // If search parameters changed, restart search
        if (!searchText.equals(currentSearchText) || caseSensitive != currentCaseSensitive) {
            return startNewSearch(searchText, caseSensitive);
        }
        
        // If we have results, move to next one
        if (!searchResults.isEmpty()) {
            int nextIndex = currentSearchIndex.get() + 1;
            if (nextIndex >= searchResults.size()) {
                nextIndex = 0; // Wrap around
            }
            currentSearchIndex.set(nextIndex);
            navigateToSearchResult(searchResults.get(nextIndex));
            return searchResults.size();
        }
        
        return 0;
    }
    
    /**
     * Performs a search for the previous occurrence of text
     * @param searchText The text to search for
     * @param caseSensitive Whether the search is case sensitive
     * @return The number of matches found
     */
    public int findPrevious(String searchText, boolean caseSensitive) {
        // If search parameters changed, restart search
        if (!searchText.equals(currentSearchText) || caseSensitive != currentCaseSensitive) {
            return startNewSearch(searchText, caseSensitive);
        }
        
        // If we have results, move to previous one
        if (!searchResults.isEmpty()) {
            int prevIndex = currentSearchIndex.get() - 1;
            if (prevIndex < 0) {
                prevIndex = searchResults.size() - 1; // Wrap around
            }
            currentSearchIndex.set(prevIndex);
            navigateToSearchResult(searchResults.get(prevIndex));
            return searchResults.size();
        }
        
        return 0;
    }
    
    /**
     * Starts a new search operation
     * @param searchText The text to search for
     * @param caseSensitive Whether the search is case sensitive
     * @return Currently found results count (may increase as background search completes)
     */
    private int startNewSearch(String searchText, boolean caseSensitive) {
        // Cancel any existing search task
        if (searchTask != null && !searchTask.isDone()) {
            searchTask.cancel(true);
        }
        
        clearSearchResults();
        
        currentSearchText = searchText;
        currentCaseSensitive = caseSensitive;
        
        // First search in visible/cached content immediately
        searchInVisibleContent(searchText, caseSensitive);
        
        // Then start background search for the entire file
        startBackgroundSearch(searchText, caseSensitive);
        
        // If we found any results, navigate to the first one
        if (!searchResults.isEmpty()) {
            currentSearchIndex.set(0);
            navigateToSearchResult(searchResults.get(0));
            return searchResults.size();
        }
        
        return 0;
    }
    
    /**
     * Clears all search results
     */
    private void clearSearchResults() {
        searchResults.clear();
        currentSearchIndex.set(-1);
        currentSearchText = "";
    }
    
    /**
     * Search in the currently visible/cached content for immediate results
     * @param searchText The text to search for
     * @param caseSensitive Whether the search is case sensitive
     */
    private void searchInVisibleContent(String searchText, boolean caseSensitive) {
        // Create pattern with case sensitivity option
        Pattern pattern = Pattern.compile(Pattern.quote(searchText), 
                                         caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        
        // Search in cached lines first for immediate results
        synchronized(lineCache) {
            for (Map.Entry<Integer, String> entry : lineCache.entrySet()) {
                int lineNumber = entry.getKey();
                String lineText = entry.getValue();
                
                Matcher matcher = pattern.matcher(lineText);
                while (matcher.find()) {
                    searchResults.add(new SearchResult(lineNumber, matcher.start(), lineText));
                }
            }
        }
        
        // Sort results by line number
        searchResults.sort(Comparator.comparingInt(r -> r.lineNumber));
    }
    
    /**
     * Start a background task to search the entire file
     * @param searchText The text to search for
     * @param caseSensitive Whether the search is case sensitive
     */
    private void startBackgroundSearch(String searchText, boolean caseSensitive) {
        // Create a set of lines we've already searched in the visible content
        Set<Integer> searchedLines = new HashSet<>();
        for (SearchResult result : searchResults) {
            searchedLines.add(result.lineNumber);
        }
        
        // Start background search task for the rest of the file
        searchTask = executor.submit(() -> {
            try {
                Pattern pattern = Pattern.compile(Pattern.quote(searchText), 
                                                 caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                List<SearchResult> newResults = new ArrayList<>();
                
                // Search remaining lines (not already searched in visible content)
                for (int i = 0; i < totalLines.get(); i++) {
                    // Skip already searched lines
                    if (searchedLines.contains(i)) {
                        continue;
                    }
                    
                    // Read the line (uses our efficient line reading approach)
                    String line = readLine(i);
                    if (line != null) {
                        Matcher matcher = pattern.matcher(line);
                        while (matcher.find()) {
                            newResults.add(new SearchResult(i, matcher.start(), line));
                        }
                    }
                    
                    // Check if search was cancelled
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                }
                
                // Add new results to the list
                if (!newResults.isEmpty()) {
                    Platform.runLater(() -> {
                        searchResults.addAll(newResults);
                        // Sort results by line number
                        searchResults.sort(Comparator.comparingInt(r -> r.lineNumber));
                        
                        // If this is the first result, navigate to it
                        if (searchResults.size() == newResults.size() && !searchResults.isEmpty()) {
                            currentSearchIndex.set(0);
                            navigateToSearchResult(searchResults.get(0));
                        }
                    });
                }
            } catch (Exception e) {
                logger.error("Error in background search", e);
            }
        });
    }
    
    /**
     * Navigate to a specific search result
     * @param result The search result to navigate to
     */
    private void navigateToSearchResult(SearchResult result) {
        // Scroll to the line
        xmlListView.scrollTo(result.lineNumber);
        
        // Ensure line is loaded and visible
        prefetchLines(result.lineNumber);
        
        // Select the item in the list
        xmlListView.getSelectionModel().select(result.lineNumber);
        xmlListView.getFocusModel().focus(result.lineNumber);
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
