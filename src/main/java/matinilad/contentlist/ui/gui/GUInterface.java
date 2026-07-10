package matinilad.contentlist.ui.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.io.File;
import javax.swing.SwingUtilities;

/**
 *
 * @author Cien
 */
public class GUInterface {

    public static void run(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.installLafInfo();
            FlatDarculaLaf.installLafInfo();
            FlatLightLaf.installLafInfo();
            FlatIntelliJLaf.installLafInfo();
            FlatMacDarkLaf.installLafInfo();
            FlatMacLightLaf.installLafInfo();

            FlatDarkLaf.setup();
            
            MainWindow w = new MainWindow();
            w.setVisible(true);
            
            if (args.length > 0) {
                w.openFile(args[0]);
            }
        });
    }

    private GUInterface() {

    }
}
