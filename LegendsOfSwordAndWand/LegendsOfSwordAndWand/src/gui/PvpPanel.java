package gui;

import model.*;
import persistence.FileDataManager;
import state.GameStateManager;
import javax.swing.*;
import java.awt.*;

public class PvpPanel extends JPanel {

    private final JTextField  opponentField;
    private final JComboBox<String> myPartyBox;
    private final JTextArea   opponentInfo;
    private final JLabel      statusLabel;

    public PvpPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UI.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = UI.title("🏆  PvP Battle");
        add(title, BorderLayout.NORTH);

        // ── Form ─────────────────────────────────────────────────────────────
        JPanel form = UI.darkPanel(new GridBagLayout());
        form.setBackground(UI.BG_PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 40, 120)),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 8, 7, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        opponentField = UI.textField(16);
        JButton lookupBtn = UI.purpleButton("Look Up");

        g.gridx = 0; g.gridy = 0; form.add(UI.label("Opponent Username:", UI.TEXT_DIM), g);
        g.gridx = 1;               form.add(opponentField, g);
        g.gridx = 2;               form.add(lookupBtn, g);

        opponentInfo = UI.logArea();
        opponentInfo.setBackground(UI.BG_CARD);
        opponentInfo.setRows(5);
        opponentInfo.setText("Enter an opponent's username and click Look Up.");
        g.gridx = 0; g.gridy = 1; g.gridwidth = 3;
        form.add(UI.scrollPane(opponentInfo, "Opponent Info", new Color(100, 50, 150)), g);

        g.gridy = 2; g.gridwidth = 1;
        form.add(UI.label("My Party:", UI.TEXT_DIM), g);
        myPartyBox = new JComboBox<>();
        myPartyBox.setBackground(UI.BG_CARD);
        myPartyBox.setForeground(UI.TEXT_MAIN);
        g.gridx = 1; g.gridwidth = 2;
        form.add(myPartyBox, g);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(UI.ACCENT_RED);
        statusLabel.setFont(UI.FONT_SMALL);
        g.gridx = 0; g.gridy = 3; g.gridwidth = 3;
        form.add(statusLabel, g);

        add(form, BorderLayout.CENTER);

        // ── Buttons ───────────────────────────────────────────────────────────
        JPanel south = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 15, 6));
        JButton challengeBtn = UI.purpleButton("⚔  Challenge!");
        JButton backBtn      = UI.primaryButton("← Back");
        south.add(challengeBtn);
        south.add(backBtn);
        add(south, BorderLayout.SOUTH);

        // Listeners
        lookupBtn.addActionListener(e -> lookupOpponent());
        backBtn.addActionListener(e -> GameStateManager.getInstance().showMainMenu());
        challengeBtn.addActionListener(e -> startPvp());
    }

    private void lookupOpponent() {
        String username = opponentField.getText().trim();
        if (username.isEmpty()) { setStatus("Enter a username.", true); return; }

        Profile opp = FileDataManager.getInstance().getAllProfiles()
            .stream().filter(p -> p.getUsername().equals(username)).findFirst().orElse(null);

        if (opp == null) { opponentInfo.setText("No profile found for: " + username); setStatus("User not found.", true); return; }
        if (opp.getSavedParties().isEmpty()) { opponentInfo.setText(username + " has no saved parties."); setStatus("Opponent has no parties.", true); return; }

        StringBuilder sb = new StringBuilder(username + "'s saved parties:\n\n");
        for (int i = 0; i < opp.getSavedParties().size(); i++) {
            Party p = opp.getSavedParties().get(i);
            sb.append(i + 1).append(".  ").append(p.getName())
              .append("  (").append(p.getSize()).append(" heroes,  ").append(p.getGold()).append("g)\n");
            for (Hero h : p.getMembers())
                sb.append("     ").append(h.getName()).append("  ").append(h.getHeroClass()).append("  Lv").append(h.getLevel()).append("\n");
            sb.append("\n");
        }
        opponentInfo.setText(sb.toString());
        setStatus(" ", false);
    }

    private void startPvp() {
        String username = opponentField.getText().trim();
        if (username.isEmpty())         { setStatus("Enter an opponent username.", true); return; }
        if (myPartyBox.getItemCount() == 0) { setStatus("You need a saved party to do PvP.", true); return; }

        int myIdx = myPartyBox.getSelectedIndex();
        if (!GameStateManager.getInstance().initiatePvp(username, myIdx)) {
            setStatus("Could not start PvP — check opponent username and parties.", true);
        }
    }

    public void refresh() {
        Profile profile = GameStateManager.getInstance().getLoggedInProfile();
        if (profile == null) return;
        myPartyBox.removeAllItems();
        for (Party p : profile.getSavedParties()) myPartyBox.addItem(p.getName());
        if (profile.getSavedParties().isEmpty())
            setStatus("You need at least one saved party to challenge someone.", true);
        else
            setStatus(" ", false);
    }

    private void setStatus(String msg, boolean error) {
        statusLabel.setForeground(error ? UI.ACCENT_RED : UI.ACCENT_GREEN);
        statusLabel.setText(msg);
    }
}
