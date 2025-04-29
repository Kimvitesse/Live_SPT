// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 17/03/2025
// Version : 0.5

package fr.telecom.physique;

import org.micromanager.data.ProcessorConfigurator;
import org.micromanager.PropertyMap;


// Cette classe ouvre une fenêtre graphique pour que l'utilisateur règle le plugin
// Elle renvoie les paramètres dans le PropertyMap à destination du ProcessorFactory
/**
 * Configures the plugin by opening a GUI and returning the settings to the Factory
 */
public class LiveSPTConfigurator implements ProcessorConfigurator {
    // Attributs
    public PropertyMap property_map;
    private PluginConfigWindow frame;

    
    // Constructeur
    public LiveSPTConfigurator(PropertyMap pm) {
        // Ce constructeur est appelé à chaque fois qu'une instance du plugin est ajouté à la pipeline
        property_map = pm;
        frame = new PluginConfigWindow(null, property_map);
    }


    // Méthodes
    @Override
    public void cleanup() {
        return;
    }

    @Override
    public void showGUI() {
        frame.setVisible(true);
        property_map = frame.getConfig();
        return;
    }

    @Override
    public PropertyMap getSettings() {
        return property_map;
    }
}
