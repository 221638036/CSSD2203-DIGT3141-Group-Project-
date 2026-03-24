package gui;

import state.GameStateManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginPanel extends JPanel {

    private final JTextField     userField;
    private final JPasswordField passField;
    private final JLabel         statusLabel;

    public LoginPanel() {
        setLayout(new GridBagLayout());
        setBackground(UI.BG_DARK);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 10, 8, 10);
        g.fill   = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel title = UI.title("⚔  Legends of Sword and Wand  ⚔");
        title.setFont(new Font("Serif", Font.BOLD, 28));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        add(title, g);

        JLabel sub = UI.centeredLabel("Enter the realm", UI.TEXT_DIM, Font.ITALIC, 14);
        g.gridy = 1;
        add(sub, g);

        // Form card
        JPanel card = UI.darkPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 100), 1),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)));
        card.setBackground(UI.BG_PANEL);

        GridBagConstraints cg = new GridBagConstraints();
        cg.insets = new Insets(6, 8, 6, 8);
        cg.fill   = GridBagConstraints.HORIZONTAL;

        userField = UI.textField(18);
        passField = UI.passwordField(18);
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(UI.FONT_SMALL);
        statusLabel.setForeground(UI.ACCENT_RED);

        cg.gridx = 0; cg.gridy = 0; card.add(UI.label("Username", UI.TEXT_DIM), cg);
        cg.gridx = 1;               card.add(userField, cg);
        cg.gridx = 0; cg.gridy = 1; card.add(UI.label("Password", UI.TEXT_DIM), cg);
        cg.gridx = 1;               card.add(passField, cg);

        JButton loginBtn    = UI.primaryButton("Login");
        JButton registerBtn = UI.goldButton("Register");

        JPanel btnRow = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setBackground(UI.BG_PANEL);
        btnRow.add(loginBtn);
        btnRow.add(registerBtn);

        cg.gridx = 0; cg.gridy = 2; cg.gridwidth = 2;
        card.add(btnRow, cg);
        cg.gridy = 3;
        card.add(statusLabel, cg);

        g.gridy = 2; g.gridwidth = 2;
        add(card, g);

        // Actions
        loginBtn.addActionListener(this::doLogin);
        registerBtn.addActionListener(this::doRegister);

        // Allow Enter key to log in
        passField.addActionListener(this::doLogin);
        userField.addActionListener(e -> passField.requestFocus());
    }

    private void doLogin(ActionEvent e) {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) { setStatus("Please enter username and password.", true); return; }
        if (!GameStateManager.getInstance().login(user, pass)) {
            setStatus("Incorrect username or password.", true);
        }
    }

    private void doRegister(ActionEvent e) {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) { setStatus("Username and password required.", true); return; }
        if (GameStateManager.getInstance().register(user, pass)) {
            setStatus("Account created! You can now log in.", false);
        } else {
            setStatus("Username already taken.", true);
        }
    }

    private void setStatus(String msg, boolean error) {
        statusLabel.setForeground(error ? UI.ACCENT_RED : UI.ACCENT_GREEN);
        statusLabel.setText(msg);
    }
}
