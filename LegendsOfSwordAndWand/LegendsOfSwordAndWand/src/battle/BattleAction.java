package battle;

import model.Enemy;
import model.Hero;
import model.Party;
import java.util.List;

/** DESIGN PATTERN: Strategy — all combat actions implement this interface */
public interface BattleAction {
    String execute(Hero actor, List<Enemy> targets, Party allies);
    String getName();
    int getManaCost();
    boolean canUse(Hero actor);
}
