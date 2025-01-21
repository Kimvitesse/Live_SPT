// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 21/01/2025
// Version : 0.3

package fr.telecom.physique;

import org.micromanager.data.ProcessorFactory;
import org.micromanager.data.Processor;
import org.micromanager.PropertyMap;
import org.micromanager.Studio;


// J'ai pas trop compris à quoi elle sert celle là mais elle doit être là
public class LiveSPTFactory implements ProcessorFactory {
    // Attributs
    private PropertyMap property_map;
    private Studio app;


    // Constructeur(s)
    public LiveSPTFactory(PropertyMap pm, Studio app) {
        super();
        property_map = pm;
        this.app = app;
        System.out.println("Usine appelée");
    }


    // Méthodes
    @Override
    public Processor createProcessor() {
        System.out.println("Processeur créé");
        return new LiveSPTProcessor(property_map, app);
    }
}
