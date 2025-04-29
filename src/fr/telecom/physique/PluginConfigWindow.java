// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 10/02/2025
// Date de modification : 17/03/2025
// Version : 0.9

package fr.telecom.physique;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.micromanager.PropertyMap;
import org.micromanager.Studio;

public class PluginConfigWindow extends JDialog {
    private JTextField roiField;
    private JTextField minDistanceField;
    private JTextField detectionThresholdField;
    private JTextField reducedRoiField;
    private JTextField initialRoiXField;
    private JTextField initialRoiYField;
    private JTextField axField;
    private JTextField ayField;
    private JTextField bField;
    private JTextField dField;
    private JTextField cxField;
    private JTextField cyField;
    private JTextField w0Field;
    private JTextField realSizeField;
    private JTextField stageNameField;
    private Studio app;
    private PropertyMap config;

    public PluginConfigWindow(JFrame parent, PropertyMap conf) {
        super(parent, true);
        config = conf;
        setTitle("Configuration");
        setSize(550, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(16, 2));

        add(new JLabel("Taille ROI (pix) :"));
        roiField = new JTextField();
        add(roiField);

        add(new JLabel("Coordonnée initiale en x de la ROI (pix) :"));
        initialRoiXField = new JTextField();
        add(initialRoiXField);

        add(new JLabel("Coordonnée initiale en y de la ROI (pix) :"));
        initialRoiYField = new JTextField();
        add(initialRoiYField);

        add(new JLabel("Taille de la ROI réduite (pix) :"));
        reducedRoiField = new JTextField();
        add(reducedRoiField);

        add(new JLabel("Distance min entre particules (pix) :"));
        minDistanceField = new JTextField();
        add(minDistanceField);

        add(new JLabel("Seuil de luminosité pour la détection (/65 535) :"));
        detectionThresholdField = new JTextField();
        add(detectionThresholdField);

        add(new JLabel("Coefficient de calibration Ax :"));
        axField = new JTextField();
        add(axField);
        
        add(new JLabel("Coefficient de calibration Ay :"));
        ayField = new JTextField();
        add(ayField);

        add(new JLabel("Coefficient de calibration B :"));
        bField = new JTextField();
        add(bField);

        add(new JLabel("Coefficient de calibration d :"));
        dField = new JTextField();
        add(dField);

        add(new JLabel("Coefficient de calibration w0 :"));
        w0Field = new JTextField();
        add(w0Field);

        add(new JLabel("Coefficient de calibration cx :"));
        cxField = new JTextField();
        add(cxField);

        add(new JLabel("Coefficient de calibration cy :"));
        cyField = new JTextField();
        add(cyField);

        add(new JLabel("Taille réelle de l'échantillon (µm) :"));
        realSizeField = new JTextField();
        add(realSizeField);

        add(new JLabel("Nom de la platine piezoélectrique :"));
        stageNameField = new JTextField();
        add(stageNameField);

        JButton validateButton = new JButton("OK");
        add(validateButton);

        validateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkSettings();
                saveConfig();
                dispose(); // Ferme la fenêtre après validation
            }
        });

        setLocationRelativeTo(parent); // Centre la fenêtre de réglage
    }

    /**
     * Checks whether the inputted settings are in the correct format
     */
    private void checkSettings() {
        try
        {
            Integer.parseInt(roiField.getText());
            Integer.parseInt(reducedRoiField.getText());
            Integer.parseInt(initialRoiXField.getText());
            Integer.parseInt(initialRoiYField.getText());
            Integer.parseInt(minDistanceField.getText());
            Integer.parseInt(detectionThresholdField.getText());
            Double.parseDouble(axField.getText());
            Double.parseDouble(ayField.getText());
            Double.parseDouble(bField.getText());
            Double.parseDouble(dField.getText());
            Double.parseDouble(w0Field.getText());
            Double.parseDouble(cxField.getText());
            Double.parseDouble(cyField.getText());
            Integer.parseInt(realSizeField.getText());
        }
        catch (NumberFormatException e)
        {
            app.logs().showError("Wrong input format.");
        }
    }

    /**
     * Updates the PropertyMap with the new settings
     */
    private void saveConfig() {
        try {
            config = config.copyBuilder()
                .putInteger("RoiSize", Integer.parseInt(roiField.getText()))
                .putInteger("ReducedRoiSize", Integer.parseInt(reducedRoiField.getText()))
                .putInteger("InitialRoiX", Integer.parseInt(initialRoiXField.getText()))
                .putInteger("InitialRoiY", Integer.parseInt(initialRoiYField.getText()))
                .putInteger("MinDistToOtherMax", Integer.parseInt(minDistanceField.getText()))
                .putInteger("DetectionThreshold", Integer.parseInt(detectionThresholdField.getText()))
                .putDouble("Ax", Double.parseDouble(axField.getText()))
                .putDouble("Ay", Double.parseDouble(ayField.getText()))
                .putDouble("B", Double.parseDouble(bField.getText()))
                .putDouble("d", Double.parseDouble(dField.getText()))
                .putDouble("w0", Double.parseDouble(w0Field.getText()))
                .putDouble("cx", Double.parseDouble(cxField.getText()))
                .putDouble("cy", Double.parseDouble(cyField.getText()))
                .putInteger("RealSize", Integer.parseInt(realSizeField.getText()))
                .putString("StageName", stageNameField.getText())
                .build();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Veuillez entrer des valeurs", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public PropertyMap getConfig() {
        return config;
    }
}
