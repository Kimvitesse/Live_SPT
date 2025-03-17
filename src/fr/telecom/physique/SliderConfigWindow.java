package fr.telecom.physique;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.micromanager.Studio;

public class SliderConfigWindow extends JDialog {
    public int detectionThreshold;
    public int ellipseThreshold;
    private JSlider detectionThresholdSlider;
    private JSlider ellipseThresholdSlider;
    private JLabel detectionThresholdLabel;
    private JLabel ellipseThresholdLabel;
    private Studio app;

    public SliderConfigWindow(JFrame parent, int initialDetectionThreshold, int initialEllipseThreshold) {
        super(parent, true);
        this.detectionThreshold = initialDetectionThreshold;
        this.ellipseThreshold = initialEllipseThreshold;
        setTitle("Configuration avec Sliders");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(3, 2));

        // Slider pour le seuil de détection
        add(new JLabel("Seuil de détection :"));
        detectionThresholdSlider = new JSlider(0, 65535, detectionThreshold);
        detectionThresholdSlider.setMajorTickSpacing(10000);
        detectionThresholdSlider.setPaintTicks(true);
        detectionThresholdSlider.setPaintLabels(true);
        detectionThresholdLabel = new JLabel(String.valueOf(detectionThresholdSlider.getValue()));
        add(detectionThresholdSlider);
        add(detectionThresholdLabel);
        
        // Slider pour le seuil de l’ellipse
        add(new JLabel("Seuil de l’ellipse :"));
        ellipseThresholdSlider = new JSlider(0, 65535, ellipseThreshold);
        ellipseThresholdSlider.setMajorTickSpacing(10000);
        ellipseThresholdSlider.setPaintTicks(true);
        ellipseThresholdSlider.setPaintLabels(true);
        ellipseThresholdLabel = new JLabel(String.valueOf(ellipseThresholdSlider.getValue()));
        add(ellipseThresholdSlider);
        add(ellipseThresholdLabel);
        
        // Listener pour mettre à jour les labels en temps réel
        detectionThresholdSlider.addChangeListener(e -> {
            detectionThreshold = detectionThresholdSlider.getValue();
            detectionThresholdLabel.setText(String.valueOf(detectionThreshold));
        });
        ellipseThresholdSlider.addChangeListener(e -> {
            ellipseThreshold = ellipseThresholdSlider.getValue();
            ellipseThresholdLabel.setText(String.valueOf(ellipseThreshold));
        });

        // Bouton de validation
        JButton validateButton = new JButton("OK");
        validateButton.addActionListener(e -> dispose());
        add(validateButton);
        
        setLocationRelativeTo(parent);
    }
}
