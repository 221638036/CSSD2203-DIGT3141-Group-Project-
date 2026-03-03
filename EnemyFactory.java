package com.legends.enemy;

import com.legends.model.EnemyTemplate;
import com.legends.model.Hero;
import com.legends.model.Party;

import java.util.Random;

/**
 * Abstract factory for generating enemy parties and templates.
 *
 * Concrete subclasses supply scaling details for different difficulty tiers.
 */
public abstract class EnemyFactory {

    protected static final Random RNG = new Random();

    // ── Factory Method definition ────────────────────────────────────────────

    /**
     * Factory Method — parameterless, as per the GoF lecture definition.
     * Each ConcreteCreator receives its context (level) via its constructor
     * and uses it inside this method, so the signature stays clean.
     */
    public abstract EnemyTemplate factoryMethod();

    // ── Template Method skeleton ───────────────────────────────────────────────

    /**
     * Generate a complete enemy party (1–5 units) scaled to the player's
     * cumulative party level. The actual template creation is delegated to a
     * subclass-specific factory.
     */
    public Party generateParty(int playerCumulativeLevel) {
        Party enemies = new Party("Enemies");
        int size    = rollSize();
        int bracket = Math.max(1, playerCumulativeLevel / size);

        for (int i = 0; i < size; i++) {
            int level = rollLevel(bracket);
            // Instantiate a ConcreteCreator with the level context,
            // then call the parameterless factoryMethod() to obtain a template.
            EnemyTemplate template = createForLevel(level).factoryMethod();
            Hero enemy = template.toHero("Enemy_" + (i + 1) + "_Lv" + template.getLevel());
            enemies.addHero(enemy);
        }
        return enemies;
    }

    /**
     * Hook: return a ConcreteCreator configured for the given level.
     * This is how context is passed to ConcreteCreators without giving
     * factoryMethod() a parameter.
     * Subclasses may override to return a different creator type.
     */
    protected abstract EnemyFactory createForLevel(int level);

    // ── Shared helpers ────────────────────────────────────────────────────────

    protected int rollSize() {
        return RNG.nextInt(5) + 1;
    }

    protected int rollLevel(int bracket) {
        int variance = Math.max(1, bracket / 2);
        return Math.max(1, Math.min(10, bracket - RNG.nextInt(variance)));
    }

    // ── Static factory convenience ────────────────────────────────────────────

    /** Returns the standard (default) enemy factory for a campaign session. */
    public static EnemyFactory standard() {
        return new StandardEnemyFactory(1); // level set per-call in generateParty
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ConcreteCreator A: Standard enemies
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Produces normally scaled enemies.
     * Stats: +2 atk/def, +15 hp per level above 1.
     */
    public static final class StandardEnemyFactory extends EnemyFactory {

        private final int level;

        public StandardEnemyFactory(int level) {
            this.level = level;
        }

        /** Factory Method — creates a standard EnemyTemplate (ConcreteProduct). */
        @Override
        public EnemyTemplate factoryMethod() {
            int atk = 5  + (level - 1) * 2;
            int def = 5  + (level - 1) * 2;
            int hp  = 100 + (level - 1) * 15;
            return new EnemyTemplate(level, atk, def, hp);
        }

        /**
         * Returns a StandardEnemyFactory configured for the given level.
         * The parent generateParty() calls this to get a fresh ConcreteCreator.
         */
        @Override
        protected EnemyFactory createForLevel(int lvl) {
            return new StandardEnemyFactory(lvl);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ConcreteCreator B: Elite enemies
    // OCP: added without any modification to StandardEnemyFactory or callers.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Produces elite enemies with higher stats and smaller group sizes.
     * Demonstrates OCP — zero changes to any existing class were needed.
     */
    public static final class EliteEnemyFactory extends EnemyFactory {

        private final int level;

        public EliteEnemyFactory(int level) {
            this.level = level;
        }

        /** Factory Method — creates an elite EnemyTemplate (ConcreteProduct). */
        @Override
        public EnemyTemplate factoryMethod() {
            int atk = 8  + (level - 1) * 3;
            int def = 8  + (level - 1) * 3;
            int hp  = 150 + (level - 1) * 25;
            return new EnemyTemplate(level, atk, def, hp);
        }

        @Override
        protected EnemyFactory createForLevel(int lvl) {
            return new EliteEnemyFactory(lvl);
        }

        @Override
        protected int rollSize() {
            return RNG.nextInt(2) + 1; // elites come in smaller groups
        }
    }
}
