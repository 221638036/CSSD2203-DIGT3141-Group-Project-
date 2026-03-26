package gui;

import battle.*;
import model.*;
import state.GameStateManager;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BattlePanel extends JPanel implements BattleObserver {

    private JLabel    turnLabel;
    private JPanel    enemyPanel;
    private JPanel    heroPanel;
    private JPanel    actionPanel;
    private JTextArea battleLog;
    private BattleEngine engine;
    private int currentTargetIndex = 0;

    public BattlePanel() {
        setLayout(new BorderLayout(6, 6));
        setBackground(UI.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        turnLabel = UI.centeredLabel("Battle!", UI.ACCENT_RED, Font.BOLD, 20);
        add(turnLabel, BorderLayout.NORTH);

        JPanel centre = UI.bgPanel(new GridLayout(2, 1, 4, 4));

        enemyPanel = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        enemyPanel.setBackground(new Color(35, 10, 10));

        heroPanel = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        heroPanel.setBackground(new Color(10, 18, 40));

        centre.add(enemyPanel);
        centre.add(heroPanel);
        add(centre, BorderLayout.CENTER);

        battleLog = UI.logArea();
        JScrollPane logScroll = UI.scrollPane(battleLog, "Battle Log", Color.GRAY);
        logScroll.setPreferredSize(new Dimension(240, 0));
        add(logScroll, BorderLayout.EAST);

        actionPanel = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        actionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Actions",
            0, 0, UI.FONT_SMALL, Color.GRAY));
        add(actionPanel, BorderLayout.SOUTH);
    }

    public void refresh() {
        engine = GameStateManager.getInstance().getBattleEngine();
        if (engine == null) return;
        engine.addObserver(this);
        currentTargetIndex = 0;
        battleLog.setText("⚔  Battle begins!\n\n");
        updateDisplay();
    }

    // ── Core display update ───────────────────────────────────────────────────

    private void updateDisplay() {
        if (engine == null) return;
        updatePanelLabels();
        rebuildEnemyPanel();
        rebuildHeroPanel();
        rebuildActionPanel();
        revalidate();
        repaint();
    }

    /**
     * Set the titles on the two panels.
     *
     * PvE:  top = "Enemies",      bottom = "Your Party"
     * PvP (your turn):   top = "Opponent Party (Target)",  bottom = "Your Party (Acting)"
     * PvP (their turn):  top = "Your Party (Target)",      bottom = "Opponent Party (Acting)"
     */
    private void updatePanelLabels() {
        if (!engine.isPvpMode()) {
            enemyPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UI.ACCENT_RED), "Enemies — click to select target",
                0, 0, UI.FONT_SMALL, UI.ACCENT_RED));
            heroPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UI.ACCENT_BLUE), "Your Party",
                0, 0, UI.FONT_SMALL, UI.ACCENT_BLUE));
        } else if (!engine.isOpponentTurn()) {
            // Challenger's turn: you act, opponent is target
            enemyPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UI.ACCENT_RED), "Opponent Party  (target) — click to select",
                0, 0, UI.FONT_SMALL, UI.ACCENT_RED));
            heroPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UI.ACCENT_BLUE), "Your Party  (acting)",
                0, 0, UI.FONT_SMALL, UI.ACCENT_BLUE));
        } else {
            // Opponent's turn: opponent acts, you are the target
            enemyPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UI.ACCENT_RED), "Your Party  (target)",
                0, 0, UI.FONT_SMALL, UI.ACCENT_RED));
            heroPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UI.ACCENT_BLUE), "Opponent Party  (acting)",
                0, 0, UI.FONT_SMALL, UI.ACCENT_BLUE));
        }
    }

    // ── Panel rebuilds ────────────────────────────────────────────────────────

    /**
     * Top panel always shows the TARGET side.
     *
     * PvE:              enemies (Enemy objects).
     * PvP your turn:    opponent's heroes.
     * PvP their turn:   your heroes (you are the target).
     */
    private void rebuildEnemyPanel() {
        enemyPanel.removeAll();

        if (!engine.isPvpMode()) {
            // PvE — show enemy cards with click-to-select
            List<Enemy> alive = engine.getAliveEnemies();
            if (currentTargetIndex >= alive.size()) currentTargetIndex = 0;
            for (int i = 0; i < alive.size(); i++) {
                final int idx = i;
                JPanel card = makeEnemyCard(alive.get(i), i == currentTargetIndex);
                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                card.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                        currentTargetIndex = idx;
                        updateDisplay();
                    }
                });
                enemyPanel.add(card);
            }
            for (Enemy e : engine.getEnemies()) {
                if (!e.isAlive()) enemyPanel.add(makeDefeatedCard(e.getName()));
            }
        } else {
            // PvP — show target party's heroes
            Party targetParty = engine.getTargetParty();
            List<Hero> targetMembers = targetParty.getMembers();
            List<Hero> aliveTargets  = targetParty.getAliveMembers();
            if (currentTargetIndex >= aliveTargets.size()) currentTargetIndex = 0;

            // Alive heroes — clickable to select target
            for (int i = 0; i < aliveTargets.size(); i++) {
                final int idx = i;
                Hero h = aliveTargets.get(i);
                JPanel card = makePvpTargetCard(h, i == currentTargetIndex);
                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                card.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                        currentTargetIndex = idx;
                        updateDisplay();
                    }
                });
                enemyPanel.add(card);
            }
            // Defeated heroes greyed out
            for (Hero h : targetMembers) {
                if (!h.isAlive()) enemyPanel.add(makeDefeatedCard(h.getName()));
            }
        }
    }

    /**
     * Bottom panel always shows the ACTING side.
     *
     * PvE:              playerParty heroes.
     * PvP your turn:    playerParty heroes (you act).
     * PvP their turn:   opponentParty heroes (they act).
     */
    private void rebuildHeroPanel() {
        heroPanel.removeAll();
        Hero current = engine.getCurrentHero();
        Party actingParty = engine.getActingParty();
        for (Hero h : actingParty.getMembers()) {
            heroPanel.add(makeHeroCard(h, h == current));
        }
    }

    /** Builds action buttons for whoever is currently acting (both players in PvP get full actions). */
    private void rebuildActionPanel() {
        actionPanel.removeAll();
        Hero current = engine.getCurrentHero();
        boolean canAct = current != null && current.isAlive() && !engine.isBattleOver();

        if (!canAct) return;

        // Update turn label
        if (!engine.isPvpMode()) {
            turnLabel.setText("⚔  " + current.getName() + "'s turn  [" + current.getHeroClass() + "]");
        } else if (!engine.isOpponentTurn()) {
            turnLabel.setText("⚔  Your turn — " + current.getName() + "  [" + current.getHeroClass() + "]");
        } else {
            turnLabel.setText("⚔  Opponent's turn — " + current.getName() + "  [" + current.getHeroClass() + "]");
        }

        // Normal actions
        List<Enemy> alive = engine.getAliveEnemies();
        if (!alive.isEmpty()) {
            currentTargetIndex = Math.min(currentTargetIndex, alive.size() - 1);
            // Target navigator
            JPanel targetNav = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            JButton prev = UI.goldButton("◀");
            prev.addActionListener(e -> { currentTargetIndex = (currentTargetIndex - 1 + alive.size()) % alive.size(); updateDisplay(); });
            String targetName = alive.get(currentTargetIndex).getName();
            JLabel targetLbl = UI.centeredLabel("🎯 " + targetName, UI.ACCENT_GOLD, Font.BOLD, 11);
            JButton next = UI.goldButton("▶");
            next.addActionListener(e -> { currentTargetIndex = (currentTargetIndex + 1) % alive.size(); updateDisplay(); });
            targetNav.add(prev); targetNav.add(targetLbl); targetNav.add(next);
            actionPanel.add(targetNav);
        }

        // Ability buttons
        for (BattleAction action : BattleActions.getActionsForHero(current)) {
            JButton btn = UI.button(action.getName(),
                action.canUse(current) ? new Color(50, 65, 130) : new Color(35, 35, 55));
            btn.setEnabled(action.canUse(current) && !alive.isEmpty());
            btn.addActionListener(e -> {
                engine.playerAction(action, currentTargetIndex);
                currentTargetIndex = 0;
                if (!engine.isBattleOver()) updateDisplay();
            });
            actionPanel.add(btn);
        }

        // Item button
        Party actingParty = engine.getActingParty();
        JButton itemBtn = UI.button("🎒 Use Item", new Color(100, 80, 140));
        itemBtn.setEnabled(!actingParty.getInventory().isEmpty());
        itemBtn.addActionListener(e -> showItemMenu(current, actingParty));
        actionPanel.add(itemBtn);
    }

    private void showItemMenu(Hero user, Party party) {
        List<Item> items = party.getInventory();
        if (items.isEmpty()) { JOptionPane.showMessageDialog(this, "No items!"); return; }

        String[] itemNames = items.stream().map(i -> i.getName() + " (+" + i.getValue() + ")").toArray(String[]::new);
        int chosen = JOptionPane.showOptionDialog(this, "Choose item:", "Use Item",
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, itemNames, itemNames[0]);
        if (chosen < 0 || chosen >= items.size()) return;

        Item item = items.get(chosen);
        List<Hero> heroes = party.getAliveMembers();
        String[] heroNames = heroes.stream().map(Hero::getName).toArray(String[]::new);
        String targetName = (String) JOptionPane.showInputDialog(this,
            "Use " + item.getName() + " on:", "Choose Target",
            JOptionPane.PLAIN_MESSAGE, null, heroNames, heroNames[0]);
        if (targetName == null) return;

        heroes.stream().filter(h -> h.getName().equals(targetName)).findFirst().ifPresent(target -> {
            engine.useItem(user, item, target, party);
            if (!engine.isBattleOver()) updateDisplay();
        });
    }

    // ── Card builders ─────────────────────────────────────────────────────────

    private JPanel makeEnemyCard(Enemy e, boolean selected) {
        JPanel card = new JPanel(new GridLayout(4, 1, 2, 2));
        card.setBackground(selected ? new Color(80, 20, 20) : new Color(55, 15, 15));
        card.setBorder(BorderFactory.createLineBorder(selected ? Color.YELLOW : UI.ACCENT_RED, selected ? 2 : 1));
        card.setPreferredSize(new Dimension(145, 105));
        card.add(UI.centeredLabel(e.getName(), selected ? Color.YELLOW : new Color(255, 140, 140), Font.BOLD, 12));
        card.add(UI.centeredLabel("Lv " + e.getLevel(), UI.TEXT_DIM, Font.PLAIN, 11));
        card.add(UI.enemyHpBar(e.getHp(), e.getMaxHp()));
        card.add(UI.centeredLabel("ATK " + e.getAttack() + "  DEF " + e.getDefense(), UI.TEXT_DIM, Font.PLAIN, 10));
        return card;
    }

    /** Hero shown in the TARGET (top) panel during PvP */
    private JPanel makePvpTargetCard(Hero h, boolean selected) {
        JPanel card = new JPanel(new GridLayout(4, 1, 2, 2));
        card.setBackground(selected ? new Color(80, 20, 20) : new Color(55, 15, 15));
        card.setBorder(BorderFactory.createLineBorder(selected ? Color.YELLOW : UI.ACCENT_RED, selected ? 2 : 1));
        card.setPreferredSize(new Dimension(155, 120));
        card.add(UI.centeredLabel(h.getName(), selected ? Color.YELLOW : new Color(255, 140, 140), Font.BOLD, 12));
        card.add(UI.centeredLabel(h.getHeroClass() + "  Lv" + h.getLevel(), UI.TEXT_DIM, Font.PLAIN, 11));
        card.add(UI.enemyHpBar(h.getHp(), h.getMaxHp()));
        card.add(UI.centeredLabel("ATK " + h.getAttack() + "  DEF " + h.getDefense(), UI.TEXT_DIM, Font.PLAIN, 10));
        return card;
    }

    /** Hero shown in the ACTING (bottom) panel */
    private JPanel makeHeroCard(Hero h, boolean active) {
        JPanel card = new JPanel(new GridLayout(5, 1, 2, 2));
        Color bg     = active ? new Color(22, 42, 85) : new Color(18, 25, 52);
        Color border = active ? UI.ACCENT_BLUE         : new Color(45, 65, 110);
        card.setBackground(bg);
        card.setBorder(BorderFactory.createLineBorder(border, active ? 2 : 1));
        card.setPreferredSize(new Dimension(155, 125));
        Color nameColor = !h.isAlive() ? UI.TEXT_DIM : active ? new Color(140, 195, 255) : UI.TEXT_MAIN;
        card.add(UI.centeredLabel(h.getName(), nameColor, Font.BOLD, 13));
        card.add(UI.centeredLabel(h.getHeroClass() + "  Lv" + h.getLevel(), UI.TEXT_DIM, Font.PLAIN, 11));
        if (h.isAlive()) {
            card.add(UI.hpBar(h.getHp(), h.getMaxHp()));
            card.add(UI.mpBar(h.getMana(), h.getMaxMana()));
            String status = h.isStunned() ? "⚡ STUNNED" : active ? "▶ Acting" : "";
            card.add(UI.centeredLabel(status, h.isStunned() ? UI.ACCENT_GOLD : UI.ACCENT_BLUE, Font.PLAIN, 10));
        } else {
            card.add(UI.centeredLabel("☠ Defeated", UI.ACCENT_RED, Font.ITALIC, 12));
            card.add(new JLabel()); card.add(new JLabel());
        }
        return card;
    }

    private JPanel makeDefeatedCard(String name) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(new Color(25, 10, 10));
        card.setBorder(BorderFactory.createLineBorder(new Color(60, 30, 30)));
        card.setPreferredSize(new Dimension(145, 55));
        card.add(UI.centeredLabel(name, new Color(80, 50, 50), Font.BOLD, 11));
        card.add(UI.centeredLabel("☠ Defeated", new Color(80, 50, 50), Font.ITALIC, 10));
        return card;
    }

    // ── BattleObserver ────────────────────────────────────────────────────────

    @Override
    public void onBattleEvent(String message) {
        SwingUtilities.invokeLater(() -> {
            battleLog.append(message + "\n");
            battleLog.setCaretPosition(battleLog.getDocument().getLength());
            updateDisplay();
        });
    }

    @Override
    public void onBattleEnd(boolean playerWon) {
        SwingUtilities.invokeLater(() -> {
            turnLabel.setText(playerWon ? "🏆  Victory!" : "💀  Defeat...");
            actionPanel.removeAll();
            JButton continueBtn = playerWon ? UI.successButton("Continue →") : UI.dangerButton("Continue →");
            continueBtn.addActionListener(e -> GameStateManager.getInstance().battleEnded(playerWon));
            actionPanel.add(continueBtn);
            revalidate(); repaint();
        });
    }

    @Override
    public void onTurnChanged(int heroIndex) {
        SwingUtilities.invokeLater(this::updateDisplay);
    }
}
