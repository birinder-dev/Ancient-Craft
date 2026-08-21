package dev.birinder.ac.client.speech;

import java.util.ArrayList;
import java.util.List;

public class SpeechBubble {

    private final int entityId;
    private final List<String> lines;
    private int remainingTicks;

    public SpeechBubble(int entityId, String message, int durationTicks) {
        this.entityId = entityId;
        this.remainingTicks = durationTicks;
        this.lines = wrapText(message, 30);
    }

    public int getEntityId() {
        return entityId;
    }

    public List<String> getLines() {
        return lines;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    public boolean isExpired() {
        return remainingTicks <= 0;
    }

    private static List<String> wrapText(String text, int maxLineLength) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxLineLength) {
                if (currentLine.length() > 0) {
                    result.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }
            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        if (currentLine.length() > 0) {
            result.add(currentLine.toString());
        }

        return result;
    }
}
