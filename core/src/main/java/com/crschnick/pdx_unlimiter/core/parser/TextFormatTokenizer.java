package com.crschnick.pdx_unlimiter.core.parser;

import java.util.Stack;

public class TextFormatTokenizer {

    public static final byte STRING_UNQUOTED = 1;
    public static final byte STRING_QUOTED = 2;
    public static final byte OPEN_GROUP = 3;
    public static final byte CLOSE_GROUP = 4;
    public static final byte EQUALS = 5;

    private static final byte DOUBLE_QUOTE_CHAR = 34;

    private final byte[] bytes;
    private final byte[] tokenTypes;
    private final int[] scalarsStart;
    private final short[] scalarsLength;
    private final Stack<Integer> arraySizeStack;
    private final int[] arraySizes;
    private boolean isInQuotes;
    private boolean isInComment;
    private int prev;
    private int i;
    private int tokenCounter;
    private int scalarCounter;
    private int arraySizesCounter;

    public TextFormatTokenizer(byte[] bytes) {
        this.bytes = bytes;
        this.prev = 0;
        this.tokenCounter = 0;

        int maxTokenCount;
        int maxNodeCount;
        if (bytes.length < 300) {
            // Special case for small files

            // Add 2 to include open and close group tokens that are always added
            maxTokenCount = bytes.length + 2;

            // Add 1 in case bytes.length is 0. We then still have one empty array node
            maxNodeCount = bytes.length + 1;
        } else {
            // Pessimistic assumptions, should always hold!

            maxTokenCount = bytes.length / 2;
            maxNodeCount = bytes.length / 5;
        }

        this.tokenTypes = new byte[maxTokenCount];
        this.scalarsStart = new int[maxNodeCount];
        this.scalarsLength = new short[maxNodeCount];
        this.arraySizes = new int[maxNodeCount];

        this.arraySizeStack = new Stack<>();
        this.arraySizesCounter = 0;
    }

    public void tokenize() {
        tokenTypes[0] = OPEN_GROUP;
        arraySizes[0] = 0;
        arraySizeStack.add(0);
        arraySizesCounter++;
        tokenCounter = 1;
        for (i = 0; i <= bytes.length; i++) {
            tokenizeIteration();
        }
        tokenTypes[tokenCounter] = CLOSE_GROUP;
    }

    private void tokenizeIteration() {
        // Add extra new line at the end to simulate end of token
        char c = i == bytes.length ? '\n' : (char) bytes[i];

        if (isInComment) {
            if (c == '\n') {
                isInComment = false;
            }
            prev = i + 1;
            return;
        }

        byte t = 0;
        if (isInQuotes && c != '"') {
            return;
        } else if (c == '"') {
            isInQuotes = !isInQuotes;
        } else if (c == '{') {
            t = OPEN_GROUP;
        } else if (c == '}') {
            t = CLOSE_GROUP;
        } else if (c == '=') {
            t = EQUALS;
        }

        boolean isWhitespace = !isInQuotes && (c == '\n' || c == '\r' || c == ' ' || c == '\t');
        boolean isComment = c == '#';
        boolean marksEndOfPreviousToken =
                (t != 0 && prev < i)                // New token finishes old token
                        || (isWhitespace && prev < i)          // Whitespace finishes old token
                        || (isComment && prev < i);            // New comment finishes old token
        if (marksEndOfPreviousToken) {
            int offset = prev;
            short length = (short) ((i - 1) - prev + 1);
            if (bytes[prev] == DOUBLE_QUOTE_CHAR && bytes[i - 1] == DOUBLE_QUOTE_CHAR) {
                tokenTypes[tokenCounter++] = STRING_QUOTED;
            } else {
                tokenTypes[tokenCounter++] = STRING_UNQUOTED;
            }
            scalarsStart[scalarCounter] = offset;
            scalarsLength[scalarCounter] = length;
            scalarCounter++;

            assert arraySizeStack.size() > 0: "Encountered unexpectedly large array at index " + i;
            arraySizes[arraySizeStack.peek()]++;
        }

        if (isWhitespace) {
            prev = i + 1;
        } else if (t != 0) {
            if (t == CLOSE_GROUP) {
                assert arraySizeStack.size() > 0 : "Encountered an additional close group token at " + i;
                arraySizeStack.pop();
            } else if (t == EQUALS) {
                arraySizes[arraySizeStack.peek()]--;
            } else if (t == OPEN_GROUP) {
                arraySizes[arraySizeStack.peek()]++;
                arraySizeStack.add(arraySizesCounter++);
            }

            tokenTypes[tokenCounter++] = t;
            prev = i + 1;
        } else if (isComment) {
            isInComment = true;
        }
    }

    public byte[] getTokenTypes() {
        return tokenTypes;
    }

    public int[] getArraySizes() {
        return arraySizes;
    }

    public int[] getScalarsStart() {
        return scalarsStart;
    }

    public short[] getScalarsLength() {
        return scalarsLength;
    }

    public int getScalarCount() {
        return scalarCounter;
    }
}
