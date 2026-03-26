package gui;

import factory.GameFactory;
import model.*;
import persistence.FileDataManager;
import state.GameStateManager;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class InnPanel extends JPanel {

    private final JLabel    goldLabel;
    private final JTextArea partyArea;
    private final JPanel    shopPanel;
    private final JPanel    recruitPanel;
    private final JPanel    inventoryPanel;

    public InnPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UI.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(UI.title("🏨  The Wanderer's Inn"), BorderLayout.NORTH);

        // ── Centre: four columns ─────────────────────────────────────────────
        JPanel centre = UI.bgPanel(new GridLayout(1, 4, 10, 0));

        // Left – party status
        partyArea = UI.logArea();
        partyArea.setBackground(UI.BG_PANEL);
        centre.add(UI.scrollPane(partyArea, "Party Status", UI.ACCENT_GOLD));

        // Shop
        shopPanel = UI.darkPanel(null);
        shopPanel.setLayout(new BoxLayout(shopPanel, BoxLayout.Y_AXIS));
        centre.add(UI.scrollPane(shopPanel, "Shop", UI.ACCENT_GOLD));

        // Recruit
        recruitPanel = UI.darkPanel(null);
        recruitPanel.setLayout(new BoxLayout(recruitPanel, BoxLayout.Y_AXIS));
        centre.add(UI.scrollPane(recruitPanel, "Recruit Heroes", UI.ACCENT_GOLD));

        // Inventory
        inventoryPanel = UI.darkPanel(null);
        inventoryPanel.setLayout(new BoxLayout(inventoryPanel, BoxLayout.Y_AXIS));
        centre.add(UI.scrollPane(inventoryPanel, "Inventory", UI.ACCENT_GOLD));

        add(centre, BorderLayout.CENTER);

        // ── South: gold + leave ───────────────────────────────────────────────
        JPanel south = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 20, 6));
        goldLabel = new JLabel("Gold: 0");
        goldLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        goldLabel.setForeground(UI.ACCENT_GOLD);

        JButton leaveBtn = UI.successButton("Leave Inn  →");
        leaveBtn.addActionListener(e -> {
            FileDataManager.getInstance().saveProfile(
                GameStateManager.getInstance().getLoggedInProfile());
            GameStateManager.getInstance().leaveInn();
        });

        south.add(goldLabel);
        south.add(leaveBtn);
        add(south, BorderLayout.SOUTH);
    }

    public void refresh() {
        CampaignState cs = GameStateManager.getInstance().getActiveCampaign();
        if (cs == null) return;
        Party party = cs.getParty();

        // Inn restores the whole party
        for (Hero h : party.getMembers()) h.fullHeal();

        // Party status text
        StringBuilder sb = new StringBuilder("The innkeeper tends to your wounds.\nAll heroes restored to full!\n\n");
        for (Hero h : party.getMembers()) {
            sb.append(h.isAlive() ? "✔ " : "✘ ").append(h.getName())
              .append("  [").append(h.getHeroClass()).append(" Lv").append(h.getLevel()).append("]\n")
              .append("  HP  ").append(h.getHp()).append("/").append(h.getMaxHp()).append("\n")
              .append("  MP  ").append(h.getMana()).append("/").append(h.getMaxMana()).append("\n\n");
        }
        partyArea.setText(sb.toString());
        goldLabel.setText("Gold:  " + party.getGold() + " g");

        // Shop
        shopPanel.removeAll();
        for (Item item : GameFactory.createInnShopItems()) {
            shopPanel.add(makeShopRow(item, party));
            shopPanel.add(Box.createVerticalStrut(4));
        }

        // Recruits
        recruitPanel.removeAll();
        List<Hero> recruits = GameFactory.createRecruitableHeroes(cs.getCurrentRoom());
        for (Hero recruit : recruits) {
            // Level 1 heroes are free, others cost 200g per level
            int cost = recruit.getLevel() == 1 ? 0 : recruit.getLevel() * 200;
            recruitPanel.add(makeRecruitRow(recruit, cost, party));
            recruitPanel.add(Box.createVerticalStrut(4));
        }

        // Inventory
        inventoryPanel.removeAll();
        if (party.getInventory().isEmpty()) {
            JLabel none = UI.label("  Inventory empty.", UI.TEXT_DIM);
            none.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            inventoryPanel.add(none);
        } else {
            for (Item item : party.getInventory()) {
                JPanel row = UI.darkPanel(new BorderLayout(6, 0));
                row.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

                JLabel lbl = UI.label(item.getName() + " (" + item.getValue() + ")", UI.TEXT_MAIN);
                JButton useBtn = UI.goldButton("Use");
                useBtn.addActionListener(e -> {
                    List<Hero> members = party.getAliveMembers();
                    if (members.isEmpty()) return;
                    String[] names = members.stream().map(Hero::getName).toArray(String[]::new);
                    String chosen = (String) JOptionPane.showInputDialog(this,
                        "Use " + item.getName() + " on:", "Inventory", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
                    if (chosen != null) {
                        members.stream().filter(h -> h.getName().equals(chosen)).findFirst().ifPresent(target -> {
                            item.use(target);
                            party.removeItem(item);
                            refresh();
                        });
                    }
                });

                row.add(lbl, BorderLayout.CENTER);
                row.add(useBtn, BorderLayout.EAST);
                inventoryPanel.add(row);
            }
        }

        revalidate();
        repaint();
    }

    private JPanel makeShopRow(Item item, Party party) {
        JPanel row = UI.darkPanel(new BorderLayout(6, 0));
        row.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lbl = UI.label(item.getName() + "  (+" + item.getValue() + ")  —  " + item.getCost() + "g", UI.TEXT_MAIN);
        JButton buyBtn = UI.goldButton("Buy");
        buyBtn.setEnabled(party.canAfford(item.getCost()));
        buyBtn.addActionListener(e -> {
            if (!party.canAfford(item.getCost())) return;
            party.spendGold(item.getCost());
            party.addItem(new Item(item.getName(), item.getType(), item.getValue(), item.getCost()));
            refresh();
        });
        row.add(lbl, BorderLayout.CENTER);
        row.add(buyBtn, BorderLayout.EAST);
        return row;
    }

    private JPanel makeRecruitRow(Hero recruit, int cost, Party party) {
        JPanel row = UI.darkPanel(new BorderLayout(6, 0));
        row.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lbl = UI.label(recruit.getName() + "  " + recruit.getHeroClass()
            + " Lv" + recruit.getLevel() + "  —  " + cost + "g", UI.TEXT_MAIN);
        JButton btn = UI.primaryButton("Recruit");
        btn.setEnabled(party.canAfford(cost) && !party.isFull());
        btn.addActionListener(e -> {
            if (!party.canAfford(cost) || party.isFull()) return;
            party.spendGold(cost);
            party.addHero(recruit);
            refresh();
        });
        row.add(lbl, BorderLayout.CENTER);
        row.add(btn, BorderLayout.EAST);
        return row;
    }
}
