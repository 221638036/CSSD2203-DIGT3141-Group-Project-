package com.legends.model.ability;
// ── Creator: Abstract factory for abilities ───────────────────────────────────
abstract class AbilityFactory {
    public abstract Ability factoryMethod();
}

// ── ConcreteCreator: Protect ability (ORDER class) ────────────────────────────
class ProtectCreator extends AbilityFactory {
    @Override public Ability factoryMethod() { return new ProtectAbility(); }
}

// ── ConcreteCreator: Heal ability (ORDER class) ───────────────────────────────
class HealCreator extends AbilityFactory {
    @Override public Ability factoryMethod() { return new HealAbility(); }
}

// ── ConcreteCreator: Fireball ability (CHAOS class) ───────────────────────────
class FireballCreator extends AbilityFactory {
    @Override public Ability factoryMethod() { return new FireballAbility(); }
}

// ── ConcreteCreator: Chain Lightning ability (CHAOS class) ────────────────────
class ChainLightningCreator extends AbilityFactory {
    @Override public Ability factoryMethod() { return new ChainLightningAbility(); }
}

// ── ConcreteCreator: Berserker Attack ability (WARRIOR class) ─────────────────
class BerserkerAttackCreator extends AbilityFactory {
    @Override public Ability factoryMethod() { return new BerserkerAttackAbility(); }
}

// ── ConcreteCreator: Replenish ability (MAGE class) ───────────────────────────
class ReplenishCreator extends AbilityFactory {
    @Override public Ability factoryMethod() { return new ReplenishAbility(); }
}
