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

    /**
     * Calculates the starting hotbar slot index (0-indexed) for the given card count,
     * centered around the 5th slot (index 4), filling left first, then right.
     *
     * 1 card  -> Slot 5 (index 4)
     * 2 cards -> Slots 4, 5 (indices 3, 4)
     * 3 cards -> Slots 4, 5, 6 (indices 3, 4, 5)
     * 4 cards -> Slots 3, 4, 5, 6 (indices 2, 3, 4, 5)
     * 5 cards -> Slots 3, 4, 5, 6, 7 (indices 2, 3, 4, 5, 6)
     * ...
     * 9 cards -> Slots 1 to 9 (indices 0 to 8)
     */
    public static int getStartSlotIndex(int count) {
        return 4 - count / 2;
    }

    /**
     * Returns the card index (0 to count - 1) mapped to the given hotbar slot (0-8),
     * or -1 if the slot is outside the active cards.
     */
    public static int getCardIndexForSlot(int slot, int count) {
        int start = getStartSlotIndex(count);
        int cardIndex = slot - start;
        if (cardIndex >= 0 && cardIndex < count) {
            return cardIndex;
        }
        return -1;
    }

    /**
     * Returns the hotbar slot index (0-indexed, 0-8) corresponding to a card index.
     */
    public static int getSlotForCardIndex(int cardIndex, int count) {
        return getStartSlotIndex(count) + cardIndex;
    }
}

