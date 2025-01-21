// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 21/01/2025
// Version : 0.5

package fr.telecom.physique;


import java.awt.Polygon;
import java.util.concurrent.ConcurrentSkipListMap;

import org.micromanager.data.Processor;
import org.micromanager.data.Image;
import org.micromanager.data.ProcessorContext;
import org.micromanager.PropertyMap;
import org.micromanager.Studio;

import ij.process.ShortProcessor;
import ij.gui.Roi;
import ij.gui.Line;


import ch.epfl.leb.alica.acpack.analyzers.spotcounter.FindLocalMaxima;


// Cette classe implémente le traitement d'image
public class LiveSPTProcessor implements Processor {
    // Attributs
    private Polygon spots = new Polygon();
    private long processingTime;
    private ConcurrentSkipListMap< Integer, Spot > trajectories = new ConcurrentSkipListMap<>();
    private int nbIter = -1;
    private int RoiSize = 80; // à régler avec settings
    private int minDistToOtherMax = 2; // à régler avec settings
    private int threshold = 40000; // à régler avec settings
    private int height;
    private int width;
    private Roi zone;
    private int trajectoryFrame = 0;
    private Studio app;
    private PropertyMap settings;
    private boolean trajectoryHasNotBegun = true;


    // Constructeur(s)
    public LiveSPTProcessor(PropertyMap pm, Studio app) {
        super();
        settings = pm;
        this.app = app;
    }

    // Méthodes
    
    // Cette méthode implémente le traitement d'image
    // Elle est appelée à chaque image capturée (en snap, live ou acquisition) sur la même instance du LiveSPTProcessor
    // L'image en paramètre est l'image à traiter
    // Le ProcessorContext est là où l'on envoie l'image traitée
    @Override
    public void processImage(Image image, ProcessorContext context) {
        // Nombre d'itérations
        nbIter++;

        // Tant qu'on ne trouve pas de spots dans la ROI initiale
        if (spots.npoints == 0 && trajectoryHasNotBegun)
        {
            if (nbIter == 0) // Première itération, on initialise les attributs
            {
                height = image.getHeight();
                width = image.getWidth();
                
                // Initialisation de la ROI
                //zone = new Roi((width - RoiSize)/2, (height - RoiSize)/2, RoiSize, RoiSize); // initialisation officielle
                zone = new Roi(246, 246, RoiSize, RoiSize); // sélection manuelle
            }
            
            // Création de l'objet adapté pour la traitement des images
            ShortProcessor img = new ShortProcessor(width, height);
            img.setPixels(image.getRawPixels());
            
            // Tracé de la ROI
            zone.drawPixels(img);
            image = app.data().ij().createImage(img, image.getCoords(), image.getMetadata());
            
            // Détection des spots
            spots = FindLocalMaxima.FindMax(img, zone, minDistToOtherMax, threshold, FindLocalMaxima.FilterType.NONE);
            
            // Initialisation de trajectories
            if (spots.npoints != 0)
            {
                trajectories.put(trajectoryFrame, new Spot(spots.xpoints[0], spots.ypoints[0]));
                System.out.println("First spot found !");
                RoiSize = 20;
                trajectoryHasNotBegun = false;
            }

            context.outputImage(image);
            return;
        }
        

        // ----------------------------------
        // | PARTIE 1 : DÉTECTION DES SPOTS |
        // ----------------------------------

        System.out.println("Appel n°"+ nbIter);

        // Mesure du temps de calcul
        long start = System.currentTimeMillis();
        long end;

        // Création de l'objet pour la traitement de l'image
        ShortProcessor img = new ShortProcessor(512, 512);
        img.setPixels(image.getRawPixels());
        
        // Détection des spots avec ALICA
        Polygon newSpots = FindLocalMaxima.FindMax(img, zone, minDistToOtherMax, threshold, FindLocalMaxima.FilterType.NONE);


        // Filtrage des doublons créés par la détection
        Polygon realSpots = new Polygon();
        if (newSpots.npoints > 1) // uniquement si + de 1 spots sont détectés
        {
            realSpots.addPoint(newSpots.xpoints[0], newSpots.ypoints[0]);
            for (int i = 1; i < newSpots.npoints; i++)
            {
                if (newSpots.xpoints[i] <= newSpots.xpoints[i - 1] + 1 && newSpots.xpoints[i] >= newSpots.xpoints[i - 1] - 1)
                {
                    if (newSpots.ypoints[i] <= newSpots.ypoints[i - 1] + 1 && newSpots.ypoints[i] >= newSpots.ypoints[i - 1] - 1)
                    {
                        continue;
                    }
                }
                realSpots.addPoint(newSpots.xpoints[i], newSpots.ypoints[i]);
            }
        }
        else 
        {
            realSpots = newSpots;
        }

        System.out.println("Number of spots found : " + realSpots.npoints);

        
        // -----------------------------------------
        // | PARTIE 2 : CRÉATION DE LA TRAJECTOIRE |
        // -----------------------------------------

        
        // Plus proches voisins (multithreading possible mais pas très intéressant)
        if (realSpots.npoints == 0) // Si on a pas trouvé de spots
        {
            spots = realSpots;

            end = System.currentTimeMillis();
            processingTime = end - start;

            context.outputImage(image);
            return;
        }
        if (realSpots.npoints == 1) // Si on a trouvé qu'un seul spot dans la ROI
        {
            // Tracé de la trajectoire (multithreading possible et intéressant pour les trajectoires longues)
            for (int i = 1; i <= trajectoryFrame; i++)
            {
                Line line = new Line(trajectories.get(i - 1).getX(), trajectories.get(i - 1).getY(), trajectories.get(i).getX(), trajectories.get(i).getY());
                line.drawPixels(img);
            }
            Spot previous = trajectories.get(trajectoryFrame);
            Line line = new Line(previous.getX(), previous.getY(), realSpots.xpoints[0], realSpots.ypoints[0]);
            line.drawPixels(img);

            // Mise à jour de la trajectoire
            trajectoryFrame++;
            trajectories.put(trajectoryFrame, new Spot(realSpots.xpoints[0], realSpots.ypoints[0]));
            
            // Mise à jour de la région d'intérêt
            zone = new Roi(realSpots.xpoints[0] - RoiSize/2, realSpots.ypoints[0] - RoiSize/2, RoiSize, RoiSize);
            zone.drawPixels(img);
            image = app.data().ij().createImage(img, image.getCoords(), image.getMetadata());
            
            // Stockage pour l'itération suivante
            spots = realSpots;
            
            // Mesure du temps de calcul
            end = System.currentTimeMillis();
            processingTime = end - start;
            System.out.println("Total processing time : " + processingTime + " ms");
            
            // Rendu de l'image à la pipeline
            context.outputImage(image);
            return;
        }

        // Si on a trouvé plusieurs spots dans la ROI

        // Création d'un vecteur de distances
        double distances[] = new double[realSpots.npoints];
        
        // Stockage du spot trouvé à l'itération précédente pour limiter les appels de méthodes pour gagner du temps
        Spot previous = trajectories.get(trajectoryFrame);
        int x = previous.getX();
        int y = previous.getY();
        
        // Calcul de chaque distance entre le spot de l'itération précédente et les nouveaux trouvés
        for (int i = 0; i < realSpots.npoints; i++)
        {
            double dist = Math.sqrt((x - realSpots.xpoints[i])*(x - realSpots.xpoints[i]) + (y - realSpots.ypoints[i])*(y - realSpots.ypoints[i]));
            distances[i] = dist;
        }

        // Affichage pour débugger
        System.out.println("Vecteur des distances :");
        printVector(distances);
        int min = argMin(distances); // Indice du plus proche voisin
        System.out.println(min);
        
        // Tracé de la trajectoire
        for (int i = 1; i <= trajectoryFrame; i++)
        {
            Line line = new Line(trajectories.get(i - 1).getX(), trajectories.get(i - 1).getY(), trajectories.get(i).getX(), trajectories.get(i).getY());
            line.drawPixels(img);
        }
        Line line = new Line(x, y, realSpots.xpoints[min], realSpots.ypoints[min]);
        line.drawPixels(img);

        // Mise à jour de la trajectoire
        trajectoryFrame++;
        trajectories.put(trajectoryFrame, new Spot(realSpots.xpoints[min], realSpots.ypoints[min]));
        
        // Mise à jour et tracé de la ROI
        zone = new Roi(realSpots.xpoints[min] - RoiSize/2, realSpots.ypoints[min] - RoiSize/2, RoiSize, RoiSize);
        zone.drawPixels(img);
        image = app.data().ij().createImage(img, image.getCoords(), image.getMetadata());

        // Mesure du temps de calcul
        end = System.currentTimeMillis();
        processingTime = end - start;
        System.out.println("Total processing time : " + processingTime + " ms");

        // Stockage
        spots = realSpots;

        // Rendu de l'image à la pipeline
        context.outputImage(image);
    }

    // Finds the index of the minimum in a given double array
    // Input : double array, array in which to look for the minimum
    // Output : int, index of the minimum
    private int argMin(double vector[]) {
        double min = Double.MAX_VALUE;
        int argmini = 0;
        for (int k = 0; k < vector.length; k++)
        {
            if (vector[k] < min)
            {
                min = vector[k];
                argmini = k;
            }
        }
        return argmini;
    }

    // Prints a double array in the console
    // Input : double array, array to print
    // Output : console print
    private void printVector(double vector[]) {
        for (int i = 0; i < vector.length; i++)
        {
            System.out.printf("%06.2f", vector[i]);
            System.out.print(" ");
        }
        System.out.println();
    }
}
