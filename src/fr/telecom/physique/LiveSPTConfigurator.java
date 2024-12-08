// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 08/12/2024
// Version : 0.1

package fr.telecom.physique;

import org.micromanager.data.ProcessorConfigurator;
import org.micromanager.PropertyMap;


// Cette classe ouvre une fenêtre graphique pour que l'utilisateur règle le plugin
// Elle renvoie les paramètres dans le PropertyMap à destination du ProcessorFactory
public class LiveSPTConfigurator implements ProcessorConfigurator {
    // Attributs
    private PropertyMap property_map;

    
    // Constructeur(s)
    public LiveSPTConfigurator(PropertyMap pm) {
        // À modifier
        property_map = pm;
    }


    // Méthodes
    @Override
    public void cleanup() {
        return;
    }

    @Override
    public void showGUI() {
        // À modifier
        return;
    }

    @Override
    public PropertyMap getSettings() {
        return property_map;
    }
}
