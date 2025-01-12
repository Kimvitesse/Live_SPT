// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 12/01/2025
// Version : 0.2

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
        // Ce constructeur est appelé à chaque fois qu'une instance du plugin est ajouté à la pipeline
        property_map = pm;
        System.out.println("Configurateur appelé");
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
