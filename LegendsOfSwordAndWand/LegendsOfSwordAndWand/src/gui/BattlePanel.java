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

        // ── North: turn label ─────────────────────────────────────────────────
        turnLabel = UI.centeredLabel("Battle!", UI.ACCENT_RED, Font.BOLD, 20);
        add(turnLabel, BorderLayout.NORTH);

        // ── Centre: enemy row / hero row ──────────────────────────────────────
        JPanel centre = UI.bgPanel(new GridLayout(2, 1, 4, 4));

        enemyPanel = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        enemyPanel.setBackground(new Color(35, 10, 10));
        enemyPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UI.ACCENT_RED), "Enemies",
            0, 0, UI.FONT_SMALL, UI.ACCENT_RED));

        heroPanel = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        heroPanel.setBackground(new Color(10, 18, 40));
        heroPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UI.ACCENT_BLUE), "Your Party",
            0, 0, UI.FONT_SMALL, UI.ACCENT_BLUE));

        centre.add(enemyPanel);
        centre.add(heroPanel);
        add(centre, BorderLayout.CENTER);

        // ── East: battle log ──────────────────────────────────────────────────
        battleLog = UI.logArea();
        JScrollPane logScroll = UI.scrollPane(battleLog, "Battle Log", Color.GRAY);
        logScroll.setPreferredSize(new Dimension(240, 0));
        add(logScroll, BorderLayout.EAST);

        // ── South: action buttons ─────────────────────────────────────────────
        actionPanel = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        actionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Actions",
            0, 0, UI.FONT_SMALL, Color.GRAY));
        add(actionPanel, BorderLayout.SOUTH);
    }

    /** Called by MainWindow every time BATTLE state is entered */
    public void refresh() {
        engine = GameStateManager.getInstance().getBattleEngine();
        if (engine == null) return;
        engine.addObserver(this);
        battleLog.setText("⚔  Battle begins!\n\n");
        updateDisplay();
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    private void updateDisplay() {
        if (engine == null) return;

        if (engine.isPvpMode()) {
            if (engine.isOpponentTurn()) {
                enemyPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(UI.ACCENT_RED), "Your Party (Target)",
                    0, 0, UI.FONT_SMALL, UI.ACCENT_RED));
                heroPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(UI.ACCENT_BLUE), "Opponent Party (Acting)",
                    0, 0, UI.FONT_SMALL, UI.ACCENT_BLUE));
            } else {
                enemyPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(UI.ACCENT_RED), "Opponent Party (Target)",
                    0, 0, UI.FONT_SMALL, UI.ACCENT_RED));
                heroPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(UI.ACCENT_BLUE), "Your Party (Acting)",
                    0, 0, UI.FONT_SMALL, UI.ACCENT_BLUE));
            }

            turnLabel.setText(engine.isOpponentTurn() ? "Opponent's Turn" : "Your Turn");
        } else {
            enemyPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UI.ACCENT_RED), "Enemies",
                0, 0, UI.FONT_SMALL, UI.ACCENT_RED));
            heroPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UI.ACCENT_BLUE), "Your Party",
                0, 0, UI.FONT_SMALL, UI.ACCENT_BLUE));
            turnLabel.setText("Battle!");
        }

        rebuildEnemyPanel();
        rebuildHeroPanel();
        rebuildActionPanel();
        revalidate();
        repaint();
    }

    private void rebuildEnemyPanel() {
        enemyPanel.removeAll();
        if (engine.isPvpMode()) {
            Party enemyParty = engine.getOtherParty();
            Hero current = engine.getCurrentHero();
            for (Hero h : enemyParty.getMembers()) {
                enemyPanel.add(makeHeroCard(h, false));
            }
            for (Hero h : enemyParty.getMembers()) {
                if (!h.isAlive()) enemyPanel.add(makeDefeatedHeroCard(h));
            }
        } else {
            for (Enemy e : engine.getAliveEnemies()) {
                enemyPanel.add(makeEnemyCard(e));
            }
            // Show defeated enemies greyed out
            for (Enemy e : engine.getEnemies()) {
                if (!e.isAlive()) enemyPanel.add(makeDefeatedEnemyCard(e));
            }
        }
    }

    private void rebuildHeroPanel() {
        heroPanel.removeAll();
        if (engine.isPvpMode()) {
            Party playerParty = engine.getCurrentParty();
            Hero current = engine.getCurrentHero();
            for (Hero h : playerParty.getMembers()) {
                heroPanel.add(makeHeroCard(h, h == current));
            }
        } else {
            Hero current = engine.getCurrentHero();
            for (Hero h : engine.getCurrentParty().getMembers()) {
                heroPanel.add(makeHeroCard(h, h == current));
            }
        }
    }

    private void rebuildActionPanel() {
        actionPanel.removeAll();
        Hero current = engine.getCurrentHero();
        boolean canAct = current != null && current.isAlive() && !engine.isBattleOver();

        if (!engine.isPvpMode() && canAct) {
            turnLabel.setText("⚔  " + current.getName() + "'s turn  [" + current.getHeroClass() + "]");
        }

        List<Enemy> alive = engine.getAliveEnemies();
        if (canAct) {
            List<BattleAction> actions = BattleActions.getActionsForHero(current);

            // Add target display and navigation for enemies
            if (!alive.isEmpty()) {
                currentTargetIndex = Math.min(currentTargetIndex, alive.size() - 1);
                JPanel targetNav = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
                JButton prevBtn = UI.goldButton("◀ Prev");
                prevBtn.addActionListener(e -> {
                    currentTargetIndex = (currentTargetIndex - 1 + alive.size()) % alive.size();
                    updateDisplay();
                });
                String targetName = alive.get(currentTargetIndex).getName();
                JLabel targetLabel = UI.centeredLabel("Target: " + targetName, UI.ACCENT_RED, Font.BOLD, 11);
                JButton nextBtn = UI.goldButton("Next ▶");
                nextBtn.addActionListener(e -> {
                    currentTargetIndex = (currentTargetIndex + 1) % alive.size();
                    updateDisplay();
                });
                targetNav.add(prevBtn);
                targetNav.add(targetLabel);
                targetNav.add(nextBtn);
                actionPanel.add(targetNav);
            }

            // Action buttons
            for (BattleAction action : actions) {
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
        }

        // Item usage always in panel as long as inventory exists
        Party party = engine.getCurrentParty();
        JButton itemBtn = UI.button("Use Item", new Color(100, 80, 140));
        itemBtn.setEnabled(!party.getInventory().isEmpty() && canAct);
        itemBtn.addActionListener(e -> showItemMenu(current, party));
        actionPanel.add(itemBtn);
    }

    private void showItemMenu(Hero user, Party party) {
        List<Item> items = party.getInventory();
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items in inventory!", "Inventory Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] itemNames = items.stream()
            .map(i -> i.getName() + " (+" + i.getValue() + ")")
            .toArray(String[]::new);
        int chosen = JOptionPane.showOptionDialog(this, "Choose an item to use:", "Use Item",
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, itemNames, itemNames[0]);

        if (chosen >= 0 && chosen < items.size()) {
            Item item = items.get(chosen);
            // Choose target (self or other alive hero)
            List<Hero> heroes = engine.getCurrentParty().getAliveMembers();
            String[] heroNames = heroes.stream().map(Hero::getName).toArray(String[]::new);
            String targetName = (String) JOptionPane.showInputDialog(this,
                "Use " + item.getName() + " on:", "Choose Target",
                JOptionPane.PLAIN_MESSAGE, null, heroNames, heroNames[0]);

            if (targetName != null) {
                for (Hero h : heroes) {
                    if (h.getName().equals(targetName)) {
                        engine.useItem(user, item, h, party);
                        if (!engine.isBattleOver()) updateDisplay();
                        break;
                    }
                }
            }
        }
    }

    // ── Card builders ─────────────────────────────────────────────────────────

    private JPanel makeEnemyCard(Enemy e) {
        JPanel card = new JPanel(new GridLayout(4, 1, 2, 2));
        card.setBackground(new Color(55, 15, 15));
        card.setBorder(BorderFactory.createLineBorder(UI.ACCENT_RED));
        card.setPreferredSize(new Dimension(145, 105));

        card.add(UI.centeredLabel(e.getName(), new Color(255, 140, 140), Font.BOLD, 12));
        card.add(UI.centeredLabel("Lv " + e.getLevel(), UI.TEXT_DIM, Font.PLAIN, 11));
        card.add(UI.enemyHpBar(e.getHp(), e.getMaxHp()));
        card.add(UI.centeredLabel("ATK " + e.getAttack() + "  DEF " + e.getDefense(), UI.TEXT_DIM, Font.PLAIN, 10));
        return card;
    }

    private JPanel makeDefeatedEnemyCard(Enemy e) {
        JPanel card = new JPanel(new GridLayout(2, 1, 2, 2));
        card.setBackground(new Color(25, 10, 10));
        card.setBorder(BorderFactory.createLineBorder(new Color(60, 30, 30)));
        card.setPreferredSize(new Dimension(145, 60));
        card.add(UI.centeredLabel(e.getName(), new Color(90, 60, 60), Font.BOLD, 12));
        card.add(UI.centeredLabel("☠ Defeated", new Color(90, 60, 60), Font.ITALIC, 11));
        return card;
    }

    private JPanel makeDefeatedHeroCard(Hero h) {
        JPanel card = new JPanel(new GridLayout(2, 1, 2, 2));
        card.setBackground(new Color(18, 15, 25));
        card.setBorder(BorderFactory.createLineBorder(new Color(45, 40, 60)));
        card.setPreferredSize(new Dimension(155, 60));
        card.add(UI.centeredLabel(h.getName(), new Color(70, 65, 90), Font.BOLD, 12));
        card.add(UI.centeredLabel("☠ Defeated", new Color(70, 65, 90), Font.ITALIC, 11));
        return card;
    }

    private JPanel makeHeroCard(Hero h, boolean active) {
        JPanel card = new JPanel(new GridLayout(5, 1, 2, 2));
        Color bg     = active ? new Color(22, 42, 85) : new Color(18, 25, 52);
        Color border = active ? UI.ACCENT_BLUE        : new Color(45, 65, 110);
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
            card.add(new JLabel());
            card.add(new JLabel());
        }
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
            revalidate();
            repaint();
        });
    }

    @Override
    public void onTurnChanged(int heroIndex) {
        SwingUtilities.invokeLater(this::updateDisplay);
    }
}
