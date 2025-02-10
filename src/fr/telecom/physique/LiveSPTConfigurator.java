// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 10/02/2025
// Version : 0.3

package fr.telecom.physique;

import org.micromanager.data.ProcessorConfigurator;
import org.micromanager.PropertyMap;


// Cette classe ouvre une fenêtre graphique pour que l'utilisateur règle le plugin
// Elle renvoie les paramètres dans le PropertyMap à destination du ProcessorFactory
public class LiveSPTConfigurator implements ProcessorConfigurator {
    // Attributs
    private PropertyMap property_map;
    private ConfigWindow frame;

    
    // Constructeur(s)
    public LiveSPTConfigurator(PropertyMap pm) {
        // Ce constructeur est appelé à chaque fois qu'une instance du plugin est ajouté à la pipeline
        
        System.out.println("Configurateur appelé");
        frame = new ConfigWindow();     
        // attendre que la fenêtre soit fermée et récupérer les valeurs
        PropertyMap property_map = frame.getConfig();

        // Pas besoin normalement
        // Cherche la valeur associée à la clé dans la PropertyMap
        // Si cette clé n'existe pas, la valeur "default_value" est retournée à la place (pour éviter une erreur)
        // int RoiSize = property_map.getInteger("RoiSize", 80);
        // int minDistance = property_map.getInteger("minDistToOtherMax", 2);
        // int threshold = property_map.getInteger("threshold", 40000);
        
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
