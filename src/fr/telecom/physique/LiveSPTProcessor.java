// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 12/01/2025
// Version : 0.2

package fr.telecom.physique;

import org.micromanager.data.Processor;
import org.micromanager.data.Image;
import org.micromanager.data.ProcessorContext;


// Cette classe implémente le traitement d'image
public class LiveSPTProcessor implements Processor {
    // Attributs
    // private List<Spot> spots // attribut qui permet de stocker les spots des images
    


    // Constructeur(s)


    // Méthodes
    
    // Cette méthode implémente le traitement d'image
    // Elle est appelée à chaque image capturée (en snap, live ou acquisition) sur la même instance du LiveSPTProcessor
    // L'image en paramètre est l'image à traiter
    // Le ProcessorContext est là où l'on envoie l'image traitée
    @Override
    public void processImage(Image image, ProcessorContext context) {
        // Détection de points
        System.out.println("Méthode processImage appelée");


        context.outputImage(image); // rend l'image pour les processeurs suivants
    }

    public void FindSpot() {
    }

    public void Tracker() {
    }
}
