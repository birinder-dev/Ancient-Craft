package dev.birinder.ac.client.gambling;

/**
 * Manages the client-side gambling state.
 * When active:
 *  - Standard item textures (swords, tools, blocks) are suppressed from the hotbar and first-person hand.
 *  - Playing cards are rendered in an arc / circular fan at the bottom of the screen.
 */
public class GamblingClientState {

    private static boolean active = false;
    private static int cardCount = 5;

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean isActive) {
        active = isActive;
    }

    public static int getCardCount() {
        return cardCount;
    }

    public static void setCardCount(int count) {
        cardCount = Math.max(1, Math.min(9, count));
    }

    /**
     * Toggles gambling mode. If activating without a specific count, defaults to 5.
     */
    public static boolean toggle(int requestedCount) {
        if (active && (requestedCount <= 0 || requestedCount == cardCount)) {
            active = false;
        } else {
            active = true;
            if (requestedCount > 0) {
                setCardCount(requestedCount);
            }
        }
        return active;
    }
}
