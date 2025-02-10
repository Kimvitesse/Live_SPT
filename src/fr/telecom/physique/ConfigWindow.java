// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 10/02/2025
// Date de modification : 10/02/2025
// Version : 0.2

package fr.telecom.physique;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.micromanager.PropertyMap;
import org.micromanager.internal.propertymap.DefaultPropertyMap;

public class ConfigWindow extends JFrame {
    private JTextField roiField;
    private JTextField minDistanceField;
    private JTextField thresholdField;
    private PropertyMap config;

    public ConfigWindow() {
        setTitle("Configuration");
        setSize(600, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(4, 2));

        add(new JLabel("Taille ROI :"));
        roiField = new JTextField();
        add(roiField);

        add(new JLabel("Distance min entre particules :"));
        minDistanceField = new JTextField();
        add(minDistanceField);

        add(new JLabel("Seuil de luminosité :"));
        thresholdField = new JTextField();
        add(thresholdField);

        JButton validateButton = new JButton("Valider");
        add(validateButton);

        validateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveConfig();
                dispose(); // Ferme la fenêtre après validation
            }
        });

        setVisible(true);
    }

    private void saveConfig() {
        config = config.copyBuilder()
                .putInteger("RoiSize", Integer.parseInt(roiField.getText()))
                .putInteger("MinDistToOtherMax", Integer.parseInt(minDistanceField.getText()))
                .putInteger("Threshold", Integer.parseInt(thresholdField.getText()))
                .build();
    }

    public PropertyMap getConfig() {
        return config;
    }
}
