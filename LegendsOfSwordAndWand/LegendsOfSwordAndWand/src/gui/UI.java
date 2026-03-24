package gui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/** Shared colours, fonts and factory methods so every panel looks consistent */
public class UI {

    // Palette
    public static final Color BG_DARK       = new Color(18, 18, 35);
    public static final Color BG_PANEL      = new Color(28, 28, 52);
    public static final Color BG_CARD       = new Color(38, 38, 65);
    public static final Color ACCENT_GOLD   = new Color(220, 175, 60);
    public static final Color ACCENT_BLUE   = new Color(80, 140, 230);
    public static final Color ACCENT_RED    = new Color(210, 60, 60);
    public static final Color ACCENT_GREEN  = new Color(60, 190, 90);
    public static final Color ACCENT_PURPLE = new Color(160, 80, 220);
    public static final Color TEXT_MAIN     = new Color(220, 220, 230);
    public static final Color TEXT_DIM      = new Color(140, 140, 160);

    // Fonts
    public static final Font FONT_TITLE  = new Font("Serif",      Font.BOLD,  26);
    public static final Font FONT_SUB    = new Font("Serif",      Font.BOLD,  18);
    public static final Font FONT_BODY   = new Font("SansSerif",  Font.PLAIN, 13);
    public static final Font FONT_BOLD   = new Font("SansSerif",  Font.BOLD,  13);
    public static final Font FONT_MONO   = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font FONT_SMALL  = new Font("SansSerif",  Font.PLAIN, 11);

    public static JLabel title(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(FONT_TITLE);
        l.setForeground(ACCENT_GOLD);
        return l;
    }

    public static JLabel label(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        l.setForeground(color);
        return l;
    }

    public static JLabel centeredLabel(String text, Color color, int style, int size) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    public static JButton button(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(FONT_BOLD);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return b;
    }

    public static JButton primaryButton(String text)  { return button(text, new Color(55, 75, 140)); }
    public static JButton dangerButton(String text)   { return button(text, new Color(130, 30, 30)); }
    public static JButton successButton(String text)  { return button(text, new Color(30, 110, 55)); }
    public static JButton goldButton(String text)     { return button(text, new Color(120, 90, 20)); }
    public static JButton purpleButton(String text)   { return button(text, new Color(90, 40, 130)); }

    public static JTextField textField(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(new Color(45, 45, 75));
        f.setForeground(TEXT_MAIN);
        f.setCaretColor(TEXT_MAIN);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 110)),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        f.setFont(FONT_BODY);
        return f;
    }

    public static JPasswordField passwordField(int cols) {
        JPasswordField f = new JPasswordField(cols);
        f.setBackground(new Color(45, 45, 75));
        f.setForeground(TEXT_MAIN);
        f.setCaretColor(TEXT_MAIN);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 110)),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        f.setFont(FONT_BODY);
        return f;
    }

    public static JTextArea logArea() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setBackground(new Color(12, 12, 22));
        a.setForeground(TEXT_MAIN);
        a.setFont(FONT_MONO);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return a;
    }

    public static JScrollPane scrollPane(JComponent c, String title, Color borderColor) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBackground(BG_PANEL);
        sp.getViewport().setBackground(BG_PANEL);
        sp.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(borderColor), title,
            0, 0, FONT_SMALL, borderColor));
        return sp;
    }

    public static JProgressBar hpBar(int value, int max) {
        JProgressBar b = new JProgressBar(0, max);
        b.setValue(value);
        b.setStringPainted(true);
        b.setString("HP " + value + "/" + max);
        b.setForeground(ACCENT_GREEN);
        b.setBackground(new Color(20, 40, 20));
        b.setFont(FONT_SMALL);
        return b;
    }

    public static JProgressBar mpBar(int value, int max) {
        JProgressBar b = new JProgressBar(0, max);
        b.setValue(value);
        b.setStringPainted(true);
        b.setString("MP " + value + "/" + max);
        b.setForeground(ACCENT_BLUE);
        b.setBackground(new Color(15, 20, 45));
        b.setFont(FONT_SMALL);
        return b;
    }

    public static JProgressBar enemyHpBar(int value, int max) {
        JProgressBar b = new JProgressBar(0, max);
        b.setValue(value);
        b.setStringPainted(true);
        b.setString("HP " + value + "/" + max);
        b.setForeground(ACCENT_RED);
        b.setBackground(new Color(40, 10, 10));
        b.setFont(FONT_SMALL);
        return b;
    }

    /** Dark background panel with BG_PANEL colour */
    public static JPanel darkPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(BG_PANEL);
        return p;
    }

    public static JPanel bgPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(BG_DARK);
        return p;
    }

    public static Border cardBorder(Color color) {
        return BorderFactory.createLineBorder(color, 1);
    }
}
