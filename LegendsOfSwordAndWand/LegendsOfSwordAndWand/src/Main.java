import gui.MainWindow;
import javax.swing.*;

/**
 * ENTRY POINT — Right-click this file in IntelliJ and select "Run Main.main()"
 * or click the green play button next to the main() method below.
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
