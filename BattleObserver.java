package com.legends.battle;

import com.legends.model.Hero;

/**
 * Observer interface for battle events.
 *
 * Implementors register with BattleEngine (the subject) to receive updates
 * on state changes such as turn end or battle over.
 */
public interface BattleObserver {

    /**
     * Called by BattleEngine (Subject) whenever battle state changes:
     * after every turn, and when the battle ends.
     *
     * @param event  A description of the event (e.g. "TURN_END", "BATTLE_OVER").
     * @param actor  The hero whose turn just ended (null on battle start/end).
     * @param result The outcome string for the most recent action.
     */
    void onBattleEvent(String event, Hero actor, String result);
}
