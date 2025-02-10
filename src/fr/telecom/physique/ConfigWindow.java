// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 10/02/2025
// Date de modification : 10/02/2025
// Version : 0.3

package fr.telecom.physique;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.micromanager.PropertyMap;

public class ConfigWindow extends JDialog {
    private JTextField roiField;
    private JTextField minDistanceField;
    private JTextField thresholdField;
    private PropertyMap config;

    public ConfigWindow(JFrame parent, PropertyMap conf) {
        super(parent, true);
        config = conf;
        setTitle("Configuration");
        setSize(550, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(4, 2));

        add(new JLabel("Taille ROI (pix) :"));
        roiField = new JTextField();
        add(roiField);

        add(new JLabel("Distance min entre particules (pix) :"));
        minDistanceField = new JTextField();
        add(minDistanceField);

        add(new JLabel("Seuil de luminosité (/65 535) :"));
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

        setLocationRelativeTo(parent); // Centre la fenêtre de réglage
    }

    private void saveConfig() {
        try {
            config = config.copyBuilder()
                .putInteger("RoiSize", Integer.parseInt(roiField.getText()))
                .putInteger("MinDistToOtherMax", Integer.parseInt(minDistanceField.getText()))
                .putInteger("Threshold", Integer.parseInt(thresholdField.getText()))
                .build();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Veuillez entrer des entiers", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
        System.out.println("Taille de la map config : " + config.size());
    }

    public PropertyMap getConfig() {
        return config;
    }
}
