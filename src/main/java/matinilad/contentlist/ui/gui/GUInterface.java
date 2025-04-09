package matinilad.contentlist.ui.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

/**
 *
 * @author Cien
 */
public class GUInterface {
    
    public static void run() {
        FlatDarkLaf.installLafInfo();
        FlatDarculaLaf.installLafInfo();
        FlatLightLaf.installLafInfo();
        FlatIntelliJLaf.installLafInfo();
        FlatMacDarkLaf.installLafInfo();
        FlatMacLightLaf.installLafInfo();
        
        FlatDarkLaf.setup();
        
        //new MainMenu().setVisible(true);
        new MainWindow().setVisible(true);
    }
    
    private GUInterface() {
        
    }
}
