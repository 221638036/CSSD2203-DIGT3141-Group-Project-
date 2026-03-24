package battle;

/** DESIGN PATTERN: Observer — GUI panels implement this to receive battle updates */
public interface BattleObserver {
    void onBattleEvent(String message);
    void onBattleEnd(boolean playerWon);
    void onTurnChanged(int heroIndex);
}
