package gui;

import model.*;
import state.GameStateManager;
import javax.swing.*;
import java.awt.*;

public class CampaignMapPanel extends JPanel {

    private final JLabel    roomLabel;
    private final JTextArea partyArea;

    public CampaignMapPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UI.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        add(UI.title("⚔  Campaign Map"), BorderLayout.NORTH);

        // Centre
        JPanel centre = UI.bgPanel(new BorderLayout(10, 10));

        roomLabel = UI.centeredLabel("Room 0 / 30", UI.ACCENT_GOLD, Font.BOLD, 16);
        centre.add(roomLabel, BorderLayout.NORTH);

        partyArea = UI.logArea();
        partyArea.setBackground(UI.BG_PANEL);
        centre.add(UI.scrollPane(partyArea, "Party Status", UI.ACCENT_BLUE), BorderLayout.CENTER);

        add(centre, BorderLayout.CENTER);

        // Buttons
        JPanel south = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 14, 6));
        JButton nextRoomBtn = UI.successButton("Enter Next Room  →");
        JButton exitBtn     = UI.goldButton("💾  Save & Exit");
        JButton menuBtn     = UI.primaryButton("Main Menu");

        south.add(nextRoomBtn);
        south.add(exitBtn);
        south.add(menuBtn);
        add(south, BorderLayout.SOUTH);

        nextRoomBtn.addActionListener(e -> GameStateManager.getInstance().enterNextRoom());
        exitBtn.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this, "Save and exit the campaign?",
                "Exit", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) GameStateManager.getInstance().exitCampaign();
        });
        menuBtn.addActionListener(e -> GameStateManager.getInstance().showMainMenu());
    }

    public void refresh() {
        CampaignState cs = GameStateManager.getInstance().getActiveCampaign();
        if (cs == null) return;
        roomLabel.setText("Room  " + cs.getCurrentRoom() + " / 30");

        StringBuilder sb = new StringBuilder();
        for (Hero h : cs.getParty().getMembers()) {
            sb.append(h.isAlive() ? "✔ " : "✘ ").append(h.toString()).append("\n");
        }
        sb.append("\nGold:  ").append(cs.getParty().getGold()).append(" g");
        partyArea.setText(sb.toString());
    }
}
