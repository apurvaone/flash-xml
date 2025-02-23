package com.xmlmafia;

import java.util.ArrayList;
import java.util.List;

public class XmlTokenizer {
    private enum State {
        TEXT, TAG, ATTRIBUTE, COMMENT, CDATA
    }

    public static class Token {
        public final String text;
        public final String styleClass;

        public Token(String text, String styleClass) {
            this.text = text;
            this.styleClass = styleClass;
        }
    }

    public List<Token> tokenize(String line) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        State state = State.TEXT;
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            switch (state) {
                case TEXT:
                    if (c == '<') {
                        if (currentToken.length() > 0) {
                            tokens.add(new Token(currentToken.toString(), "text"));
                            currentToken.setLength(0);
                        }
                        currentToken.append(c);
                        
                        if (i + 3 < line.length() && line.substring(i, i + 4).equals("<!--")) {
                            state = State.COMMENT;
                            i += 3;
                            currentToken.append("!--");
                        } else if (i + 8 < line.length() && line.substring(i, i + 9).equals("<![CDATA[")) {
                            state = State.CDATA;
                            i += 8;
                            currentToken.append("![CDATA[");
                        } else {
                            state = State.TAG;
                        }
                    } else {
                        currentToken.append(c);
                    }
                    break;
                    
                case TAG:
                    currentToken.append(c);
                    if (Character.isWhitespace(c)) {
                        tokens.add(new Token(currentToken.toString(), "tag"));
                        currentToken.setLength(0);
                        state = State.ATTRIBUTE;
                    } else if (c == '>') {
                        tokens.add(new Token(currentToken.toString(), "tag"));
                        currentToken.setLength(0);
                        state = State.TEXT;
                    }
                    break;
                    
                case ATTRIBUTE:
                    if (c == '"') {
                        currentToken.append(c);
                        inQuotes = !inQuotes;
                        if (!inQuotes) {
                            tokens.add(new Token(currentToken.toString(), "attribute"));
                            currentToken.setLength(0);
                        }
                    } else if (c == '>' && !inQuotes) {
                        if (currentToken.length() > 0) {
                            tokens.add(new Token(currentToken.toString(), "attribute"));
                            currentToken.setLength(0);
                        }
                        tokens.add(new Token(String.valueOf(c), "tag"));
                        state = State.TEXT;
                    } else {
                        currentToken.append(c);
                    }
                    break;
                    
                case COMMENT:
                    currentToken.append(c);
                    if (i >= 2 && line.substring(i - 2, i + 1).equals("-->")) {
                        tokens.add(new Token(currentToken.toString(), "comment"));
                        currentToken.setLength(0);
                        state = State.TEXT;
                    }
                    break;
                    
                case CDATA:
                    currentToken.append(c);
                    if (i >= 2 && line.substring(i - 2, i + 1).equals("]]>")) {
                        tokens.add(new Token(currentToken.toString(), "cdata"));
                        currentToken.setLength(0);
                        state = State.TEXT;
                    }
                    break;
            }
        }
        
        if (currentToken.length() > 0) {
            tokens.add(new Token(currentToken.toString(), 
                state == State.TEXT ? "text" :
                state == State.TAG ? "tag" :
                state == State.ATTRIBUTE ? "attribute" :
                state == State.COMMENT ? "comment" : "cdata"));
        }
        
        return tokens;
    }
}
