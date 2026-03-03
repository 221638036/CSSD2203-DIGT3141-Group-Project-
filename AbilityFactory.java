package com.legends.model.ability;

public abstract class AbilityFactory {

    
    public enum Type {
        PROTECT, HEAL, FIREBALL, CHAIN_LIGHTNING, BERSERKER_ATTACK, REPLENISH
    }


    public abstract Ability factoryMethod();

    // ── Static dispatcher ─────────────────────────────────────────────────────

    /**
     * Selects the appropriate ConcreteCreator for the requested type and
     * invokes factoryMethod() — mirroring the lecture's client code pattern:
     *
     *   Creator creator = new ConcreteCreatorA();
     *   Product product = creator.factoryMethod();
     *
     * The caller never sees or names a concrete ability class.
     */
    public static Ability create(Type type) {
        AbilityFactory creator = switch (type) {
            case PROTECT          -> new ProtectCreator();
            case HEAL             -> new HealCreator();
            case FIREBALL         -> new FireballCreator();
            case CHAIN_LIGHTNING  -> new ChainLightningCreator();
            case BERSERKER_ATTACK -> new BerserkerAttackCreator();
            case REPLENISH        -> new ReplenishCreator();
        };
        return creator.factoryMethod();   // ← factory method call
    }
}
