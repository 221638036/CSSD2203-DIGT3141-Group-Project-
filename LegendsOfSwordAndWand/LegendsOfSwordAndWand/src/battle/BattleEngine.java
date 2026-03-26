package battle;

import model.*;
import java.util.*;
import java.util.stream.Collectors;

public class BattleEngine {
    private Party playerParty;       // the challenger (person who clicked Challenge)
    private List<Enemy> enemies;     // used in PvE only
    private Party opponentParty;     // used in PvP only

    private List<BattleObserver> observers = new ArrayList<>();
    private int currentHeroIndex = 0;
    private boolean battleOver  = false;
    private boolean playerWon   = false;
    private boolean pvpMode     = false;

    /**
     * PvP turn tracking.
     * false = player's turn (challenger acts, opponent is the target).
     * true  = opponent's turn (opponent acts, challenger is the target).
     */
    private boolean isOpponentTurn = false;
    private String opponentUsername = null;  // for win/loss tracking

    private Set<String> defendingHeroes = new HashSet<>();

    // ── PvE constructor ───────────────────────────────────────────────────────
    public BattleEngine(Party playerParty, List<Enemy> enemies) {
        this.playerParty  = playerParty;
        this.enemies      = enemies;
        this.pvpMode      = false;
        this.opponentParty = null;
    }

    // ── PvP constructor ───────────────────────────────────────────────────────
    public BattleEngine(Party playerParty, Party opponentParty, boolean pvpMode) {
        this(playerParty, opponentParty, pvpMode, null);
    }

    public BattleEngine(Party playerParty, Party opponentParty, boolean pvpMode, String opponentUsername) {
        this.playerParty       = playerParty;
        this.opponentParty     = opponentParty;
        this.enemies           = new ArrayList<>();
        this.pvpMode           = pvpMode;
        this.isOpponentTurn    = false;
        this.opponentUsername  = opponentUsername;
    }

    // ── Observers ─────────────────────────────────────────────────────────────
    public void addObserver(BattleObserver obs)    { observers.add(obs); }
    public void removeObserver(BattleObserver obs) { observers.remove(obs); }

    private void notifyEvent(String msg)      { for (BattleObserver o : observers) o.onBattleEvent(msg); }
    private void notifyBattleEnd(boolean won) { for (BattleObserver o : observers) o.onBattleEnd(won); }
    private void notifyTurnChanged(int idx)   { for (BattleObserver o : observers) o.onTurnChanged(idx); }

    // ── PvP perspective helpers ───────────────────────────────────────────────

    /**
     * The party whose heroes are ACTING this turn.
     * On the challenger's turn  → playerParty.
     * On the opponent's turn    → opponentParty.
     */
    public Party getActingParty() {
        if (!pvpMode) return playerParty;
        return isOpponentTurn ? opponentParty : playerParty;
    }

    /**
     * The party that is being TARGETED this turn.
     * On the challenger's turn  → opponentParty.
     * On the opponent's turn    → playerParty.
     */
    public Party getTargetParty() {
        if (!pvpMode) return null;
        return isOpponentTurn ? playerParty : opponentParty;
    }

    /** Which hero is currently acting */
    public Hero getCurrentHero() {
        List<Hero> alive = getActingParty().getAliveMembers();
        if (alive.isEmpty()) return null;
        return alive.get(currentHeroIndex % alive.size());
    }

    /**
     * In PvE: alive enemies from the enemy list.
     * In PvP: alive heroes from the TARGET (non-acting) party, wrapped as Enemy proxies
     *         so the action system can damage them.
     *
     * We keep PvP targets as Hero objects and damage them directly via EnemyHeroProxy.
     */
    public List<Enemy> getAliveEnemies() {
        if (!pvpMode) {
            return enemies.stream().filter(Enemy::isAlive).collect(Collectors.toList());
        }
        // In PvP the "enemies" are the heroes of the target party
        return getTargetParty().getMembers().stream()
            .filter(Hero::isAlive)
            .map(EnemyHeroProxy::new)
            .collect(Collectors.toList());
    }

    /** Full enemy list (including defeated) — used by BattlePanel for display */
    public List<Enemy> getEnemies() {
        if (!pvpMode) return enemies;
        return getTargetParty().getMembers().stream()
            .map(EnemyHeroProxy::new)
            .collect(Collectors.toList());
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void playerAction(BattleAction action, int targetIndex) {
        if (battleOver) return;
        Hero hero = getCurrentHero();
        if (hero == null || !hero.isAlive()) { advanceTurn(); return; }

        if (hero.isStunned()) {
            notifyEvent(hero.getName() + " is stunned and loses their turn!");
            hero.setStunned(false);
            if (!pvpMode) enemyTurn();
            advanceTurn();
            return;
        }

        List<Enemy> aliveEnemies = getAliveEnemies();
        List<Enemy> targetList = new ArrayList<>();
        if (!aliveEnemies.isEmpty()) {
            int idx = Math.min(targetIndex, aliveEnemies.size() - 1);
            targetList.add(aliveEnemies.get(idx));
            for (int i = 0; i < aliveEnemies.size(); i++) if (i != idx) targetList.add(aliveEnemies.get(i));
        }

        if (action instanceof BattleActions.Defend) defendingHeroes.add(hero.getName());

        String result = action.execute(hero, targetList, getActingParty());
        notifyEvent(result);
        checkBattleEnd();
        if (!battleOver) {
            if (!pvpMode) enemyTurn();
            advanceTurn();
        }
    }

    public void useItem(Hero user, Item item, Hero target, Party party) {
        if (battleOver) return;
        if (!party.getInventory().contains(item)) return;
        item.use(target);
        party.removeItem(item);
        notifyEvent(user.getName() + " used " + item.getName() + " on " + target.getName() + "!");
        checkBattleEnd();
        if (!battleOver) {
            if (!pvpMode) enemyTurn();
            advanceTurn();
        }
    }

    private void enemyTurn() {
        // PvE only
        List<Hero> alive = playerParty.getAliveMembers();
        if (alive.isEmpty()) return;
        Random rand = new Random();
        for (Enemy enemy : enemies.stream().filter(Enemy::isAlive).collect(Collectors.toList())) {
            if (enemy.isStunned()) {
                notifyEvent(enemy.getName() + " is stunned and skips their turn!");
                enemy.setStunned(false);
                continue;
            }
            Hero target = alive.get(rand.nextInt(alive.size()));
            int dmg = enemy.getAttack();
            if (defendingHeroes.contains(target.getName())) dmg /= 2;
            target.takeDamage(dmg);
            int effective = Math.max(0, dmg - target.getDefense());
            notifyEvent(enemy.getName() + " attacks " + target.getName() + " for " + effective + " damage!"
                + (!target.isAlive() ? "  " + target.getName() + " is defeated!" : ""));
        }
        defendingHeroes.clear();
        checkBattleEnd();
    }

    private void advanceTurn() {
        if (battleOver) return;
        List<Hero> alive = getActingParty().getAliveMembers();
        if (alive.isEmpty()) return;
        currentHeroIndex = (currentHeroIndex + 1) % alive.size();

        // When we've cycled through all of the acting party's heroes, switch sides (PvP)
        if (pvpMode && currentHeroIndex == 0) {
            isOpponentTurn = !isOpponentTurn;
            notifyEvent(isOpponentTurn
                ? "--- Opponent's turn ---"
                : "--- Your turn ---");
        }
        notifyTurnChanged(currentHeroIndex);
    }

    private void checkBattleEnd() {
        if (pvpMode) {
            boolean opponentDefeated = opponentParty.isDefeated();
            boolean playerDefeated   = playerParty.isDefeated();
            if (opponentDefeated || playerDefeated) {
                battleOver = true;
                // playerWon = true means the challenger (playerParty) won
                playerWon  = opponentDefeated && !playerDefeated;
                String msg = playerWon ? "🏆 Your party wins the PvP battle!" : "💀 Opponent's party wins the PvP battle!";
                notifyEvent(msg);
                notifyBattleEnd(playerWon);
            }
        } else {
            List<Enemy> aliveEnemies = enemies.stream().filter(Enemy::isAlive).collect(Collectors.toList());
            if (aliveEnemies.isEmpty()) {
                battleOver = true; playerWon = true;
                int totalGold = 0, totalXp = 0;
                for (Enemy e : enemies) {
                    totalGold += e.getGoldReward() * e.getLevel();
                    totalXp  += e.getXpReward()   * e.getLevel();
                }
                playerParty.earnGold(totalGold);
                List<Hero> survivors = playerParty.getAliveMembers();
                if (!survivors.isEmpty()) {
                    int xpEach = totalXp / survivors.size();
                    for (Hero h : survivors) h.addXp(xpEach);
                }
                notifyEvent("Victory! Earned " + totalGold + " gold and " + totalXp + " XP split among " + survivors.size() + " survivors!");
                notifyBattleEnd(true);
            } else if (playerParty.isDefeated()) {
                battleOver = true; playerWon = false;
                notifyEvent("Your party has been defeated...");
                notifyBattleEnd(false);
            }
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public boolean isBattleOver()    { return battleOver; }
    public String getOpponentUsername() { return opponentUsername; }
    public boolean isPlayerWon()     { return playerWon; }
    public Party getPlayerParty()    { return playerParty; }
    public Party getOpponentParty()  { return opponentParty; }
    public boolean isPvpMode()       { return pvpMode; }
    public boolean isOpponentTurn()  { return isOpponentTurn; }
    public void setOpponentTurn(boolean b) { this.isOpponentTurn = b; }

    // ── Inner class: wraps a Hero as an Enemy for the action system ───────────
    /**
     * In PvP, the "enemy" targets are actually Hero objects from the opposing party.
     * This proxy lets BattleActions damage them via the normal Enemy API while
     * keeping the Hero's real HP updated.
     */
    public static class EnemyHeroProxy extends Enemy {
        private final Hero hero;

        public EnemyHeroProxy(Hero hero) {
            super(hero.getName(), hero.getHp(), hero.getAttack(), hero.getDefense(), 0, 0);
            this.hero = hero;
        }

        @Override
        public void takeDamage(int dmg) {
            // Damage goes to the real Hero object
            hero.takeDamage(dmg);
            // Keep proxy HP in sync so isAlive() works correctly
            syncHp();
        }

        @Override
        public boolean isAlive() { return hero.isAlive(); }

        @Override
        public int getHp()    { return hero.getHp(); }
        @Override
        public int getMaxHp() { return hero.getMaxHp(); }

        private void syncHp() { /* hero manages its own hp */ }

        public Hero getHero() { return hero; }
    }
}
