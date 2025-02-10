// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 10/02/2025
// Version : 0.6

package fr.telecom.physique;


import java.awt.Polygon;
import java.util.concurrent.ConcurrentSkipListMap;

import org.micromanager.data.Processor;
import org.micromanager.data.Image;
import org.micromanager.data.ProcessorContext;
import org.micromanager.PropertyMap;
import org.micromanager.Studio;
import org.micromanager.display.overlay.Overlay;
import org.micromanager.display.overlay.AbstractOverlay;

import ij.process.ImageProcessor;
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
    private int roiSize;
    private int minDistToOtherMax;
    private int threshold;
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
        roiSize = settings.getInteger("RoiSize", 80); // Valeur à récupérer, valeur par défaut
        minDistToOtherMax = settings.getInteger("MinDistToOtherMax", 2);
        threshold = settings.getInteger("Threshold", 35000);
        System.out.println("Taille de la map : " + pm.size());
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
        if (trajectoryHasNotBegun && spots.npoints == 0)
        {
            if (nbIter == 0) // Première itération, on initialise les attributs
            {
                height = image.getHeight();
                width = image.getWidth();
                
                // Initialisation de la ROI
                zone = new Roi((width - roiSize)/2, (height - roiSize)/2, roiSize, roiSize);
            }
            
            // Création de l'objet adapté pour la traitement des images
            ImageProcessor img = app.data().ij().createProcessor(image);
            
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
                roiSize = 20;
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
        ImageProcessor img = app.data().ij().createProcessor(image);


        // Détection des spots avec ALICA
        Polygon newSpots = FindLocalMaxima.FindMax(img, zone, minDistToOtherMax, threshold, FindLocalMaxima.FilterType.NONE); // Tester filtres

        System.out.println("Number of spots found : " + newSpots.npoints);

        
        // -----------------------------------------
        // | PARTIE 2 : CRÉATION DE LA TRAJECTOIRE |
        // -----------------------------------------

        
        // Plus proches voisins (multithreading possible mais pas très intéressant)
        if (newSpots.npoints == 0) // Si on a pas trouvé de spots
        {
            spots = newSpots;

            end = System.currentTimeMillis();
            processingTime = end - start;

            context.outputImage(image);
            return;
        }
        if (newSpots.npoints == 1) // Si on a trouvé qu'un seul spot dans la ROI
        {
            // Tracé de la trajectoire (multithreading possible et intéressant pour les trajectoires longues)
            for (int i = 1; i <= trajectoryFrame; i++)
            {
                Line line = new Line(trajectories.get(i - 1).getX(), trajectories.get(i - 1).getY(), trajectories.get(i).getX(), trajectories.get(i).getY());
                line.drawPixels(img);
            }
            Spot previous = trajectories.get(trajectoryFrame);
            Line line = new Line(previous.getX(), previous.getY(), newSpots.xpoints[0], newSpots.ypoints[0]);
            line.drawPixels(img);
            
            //Overlay overlay = new AbstractOverlay();
            //app.live().getDisplay().addOverlay();

            // Mise à jour de la trajectoire
            trajectoryFrame++;
            trajectories.put(trajectoryFrame, new Spot(newSpots.xpoints[0], newSpots.ypoints[0]));
            
            // Mise à jour de la région d'intérêt
            zone = new Roi(newSpots.xpoints[0] - roiSize/2, newSpots.ypoints[0] - roiSize/2, roiSize, roiSize);
            zone.drawPixels(img);
            image = app.data().ij().createImage(img, image.getCoords(), image.getMetadata());
            
            // Stockage pour l'itération suivante
            spots = newSpots;
            
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
        double distances[] = new double[newSpots.npoints];
        
        // Stockage du spot trouvé à l'itération précédente pour limiter les appels de méthodes pour gagner du temps
        Spot previous = trajectories.get(trajectoryFrame);
        int x = previous.getX();
        int y = previous.getY();
        
        // Calcul de chaque distance entre le spot de l'itération précédente et les nouveaux trouvés
        for (int i = 0; i < newSpots.npoints; i++)
        {
            double dist = Math.sqrt((x - newSpots.xpoints[i])*(x - newSpots.xpoints[i]) + (y - newSpots.ypoints[i])*(y - newSpots.ypoints[i]));
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
        Line line = new Line(x, y, newSpots.xpoints[min], newSpots.ypoints[min]);
        line.drawPixels(img);

        // Mise à jour de la trajectoire
        trajectoryFrame++;
        trajectories.put(trajectoryFrame, new Spot(newSpots.xpoints[min], newSpots.ypoints[min]));
        
        // Mise à jour et tracé de la ROI
        zone = new Roi(newSpots.xpoints[min] - roiSize/2, newSpots.ypoints[min] - roiSize/2, roiSize, roiSize);
        zone.drawPixels(img);
        image = app.data().ij().createImage(img, image.getCoords(), image.getMetadata());

        // Mesure du temps de calcul
        end = System.currentTimeMillis();
        processingTime = end - start;
        System.out.println("Total processing time : " + processingTime + " ms");

        // Stockage
        spots = newSpots;

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
