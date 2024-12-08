// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 08/12/2024
// Version : 0.1

package fr.telecom.physique;

import org.micromanager.data.Processor;
import org.micromanager.data.Image;
import org.micromanager.data.ProcessorContext;


// Cette classe implémente le traitement d'image
public class LiveSPTProcessor implements Processor {
    // Attributs


    // Constructeur(s)


    // Méthodes
    
    // Cette méthode implémente le traitement d'image
    // L'image en paramètre est l'image à traiter
    // Le ProcessorContext est là où l'on envoie l'image traitée
    @Override
    public void processImage(Image image, ProcessorContext context) {
        // Mettre notre algorithme ici
        // context.outputImage(treated_image)
    }
}