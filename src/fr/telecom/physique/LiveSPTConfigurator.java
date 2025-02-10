// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 10/02/2025
// Version : 0.4

package fr.telecom.physique;

import org.micromanager.data.ProcessorConfigurator;
import org.micromanager.PropertyMap;


// Cette classe ouvre une fenêtre graphique pour que l'utilisateur règle le plugin
// Elle renvoie les paramètres dans le PropertyMap à destination du ProcessorFactory
public class LiveSPTConfigurator implements ProcessorConfigurator {
    // Attributs
    public PropertyMap property_map;
    private ConfigWindow frame; // static ?

    
    // Constructeur(s)
    public LiveSPTConfigurator(PropertyMap pm) {
        // Ce constructeur est appelé à chaque fois qu'une instance du plugin est ajouté à la pipeline
        property_map = pm;
        System.out.println("Configurateur appelé");
        frame = new ConfigWindow(null, property_map);
        // attendre que la fenêtre soit fermée et récupérer les valeurs
        property_map = frame.getConfig();
        System.out.println("Taille de la map Configurator : " + property_map.size());
    }


    // Méthodes
    @Override
    public void cleanup() {
        return;
    }

    @Override
    public void showGUI() {
        frame.setVisible(true);
        return;
    }

    @Override
    public PropertyMap getSettings() {
        return property_map;
    }
}
