// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 14/03/2025
// Version : 0.7

package fr.telecom.physique;


import java.awt.Color;
import java.awt.Polygon;
import java.util.concurrent.ConcurrentSkipListMap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.micromanager.data.Processor;
import org.micromanager.data.Image;
import org.micromanager.data.ProcessorContext;
import org.micromanager.PropertyMap;
import org.micromanager.Studio;

import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.EllipseFitter;
import ij.gui.Roi;
import ij.gui.Line;
import ch.epfl.leb.alica.acpack.analyzers.spotcounter.FindLocalMaxima;


/**
 * 
 * Implementation of the tracking algorithm. It relies on the FindLocalMaxima method from ALICA
 * to find the spots and uses a nearest neighbour algorithm to link the spots found between frames.
 * 
 */
public class LiveSPTProcessor implements Processor {
    // Attributs
    private Polygon spots = new Polygon();
    private long processingTime;
    private ConcurrentSkipListMap< Integer, Spot > trajectories = new ConcurrentSkipListMap<>();
    private int nbIter;
    private int roiSize;
    private int reducedRoiSize = 20;
    private int initialRoiX;
    private int initialRoiY;
    private int minDistToOtherMax;
    private int detectionThreshold;
    private int ellipseThreshold;
    private int height;
    private int width;
    private Roi zone;
    private int trajectoryFrame;
    private Studio app;
    private PropertyMap settings;
    private boolean trajectoryHasNotBegun = true;
    private double A;
    private double B;
    private double d; // 80
    private double cx; // 200
    private double cy; // -200
    private double w0; // 1500
    private String stageName;
    public static double conversionRatio; // 82 µm pour 512 px
    private double microscopeZPosition;
    public static int UNSIGNED_SHORT_MAX_VALUE = 65535;


    // Constructeur(s)
    public LiveSPTProcessor(PropertyMap pm, Studio app) {
        super();
        settings = pm;
        this.app = app;
        nbIter = -1;
        trajectoryFrame = 0;
        roiSize = settings.getInteger("RoiSize", 80); // Valeur à récupérer, valeur par défaut
        initialRoiX = settings.getInteger("InitialRoiX", -1);
        initialRoiY = settings.getInteger("InitialRoiY", -1);
        minDistToOtherMax = settings.getInteger("MinDistToOtherMax", 2);
        detectionThreshold = settings.getInteger("DetectionThreshold", 150);
        ellipseThreshold = settings.getInteger("EllipseThreshold", 10000);
        reducedRoiSize = settings.getInteger("ReducedRoiSize", 20);
        A = settings.getDouble("A", 0.0001);
        B = settings.getDouble("B", 0.0001);
        d = settings.getDouble("d", 320);
        cx = settings.getDouble("cx", 200);
        cy = settings.getDouble("cy", -200);
        w0 = settings.getDouble("w0", 300); // nm
        conversionRatio = settings.getInteger("RealSize", 82);
        stageName = settings.getString("StageName", "PIZstage");
        try
        {
            microscopeZPosition = app.core().getPosition(stageName);
        }
        catch (Exception e)
        {
            app.getLogManager().logError("Failed to get initial position");
        }
    }

    
    // Méthodes
    
    /**
     * Tracks a single particle and adjusts the microscope to keep that particle in focus
     * The algorithm is divided in three parts :
     * - part 1 : initialization
     * - part 2 : spot detection
     * - part 3 : trajectory building
     */
    @Override
    public void processImage(Image image, ProcessorContext context) {
        

        // ---------------------------------------------
        // | PARTIE 1 : INITIALISATION DE L'ALGORITHME |
        // ---------------------------------------------
        
        
        System.out.println("A : " + A);

        // Nombre d'itérations
        nbIter++;

        // Tant qu'on ne trouve pas de spots dans la ROI initiale
        if (trajectoryHasNotBegun && spots.npoints == 0)
        {
            if (nbIter == 0) // Première itération, on initialise les attributs
            {
                height = image.getHeight();
                width = image.getWidth();
                conversionRatio = conversionRatio/height;
                if (initialRoiX == -1)
                {
                    initialRoiX = (width - roiSize)/2;
                }
                if (initialRoiY == -1)
                {
                    initialRoiY = (height - roiSize)/2;
                }

                // Initialisation de la ROI
                zone = new Roi(initialRoiX, initialRoiY, roiSize, roiSize);
            }
            
            // Création de l'objet adapté pour la traitement des images
            ImageProcessor img = app.data().ij().createProcessor(image);
            
            // Détection des spots
            spots = FindLocalMaxima.FindMax(img, zone, minDistToOtherMax, detectionThreshold, FindLocalMaxima.FilterType.NONE);
            app.getLogManager().logMessage("Number of spots found : " + spots.npoints);
            
            // Initialisation de trajectories
            if (spots.npoints != 0)
            {
                // Mise à jour de la ROI
                roiSize = reducedRoiSize;
                zone = new Roi(spots.xpoints[0] - roiSize/2, spots.ypoints[0] - roiSize/2, roiSize, roiSize);
                
                /*ImageProcessor img2 = img.duplicate();
                img2.setThreshold((double)ellipseThreshold, (double)UNSIGNED_SHORT_MAX_VALUE, ImageProcessor.NO_LUT_UPDATE);
                ByteProcessor mask = img2.createMask();
                mask.fillOutside(zone);
                image = app.data().ij().createImage(mask, image.getCoords(), image.getMetadata());*/

                // Calcul de la coordonée en z
                double z = getZCoordinate(img);
                app.getLogManager().logMessage("Coordonnée en z : " + z);
                
                // Mise à jour de la trajectoire
                trajectories.put(trajectoryFrame, new Spot(spots.xpoints[0], spots.ypoints[0], z + microscopeZPosition));
                app.getLogManager().logMessage("First spot found !");

                // Correction du focus
                try
                {
                    app.core().setPosition(stageName, z + microscopeZPosition);
                    microscopeZPosition += z;
                }
                catch (Exception e)
                {
                    app.getLogManager().logError("Microscope focus correction failed");
                }
                
                trajectoryHasNotBegun = false;
            }

            image = app.data().ij().createImage(img, image.getCoords(), image.getMetadata());
            
            context.outputImage(image);
            return;
        }
        

        // ----------------------------------
        // | PARTIE 2 : DÉTECTION DES SPOTS |
        // ----------------------------------


        app.getLogManager().logMessage("Appel n°"+ nbIter);

        // Mesure du temps de calcul
        long start = System.currentTimeMillis();
        long end;

        // Création de l'objet pour la traitement de l'image
        ImageProcessor img = app.data().ij().createProcessor(image);

        // Détection des spots avec ALICA
        Polygon newSpots = FindLocalMaxima.FindMax(img, zone, minDistToOtherMax, detectionThreshold, FindLocalMaxima.FilterType.NONE);

        app.getLogManager().logMessage("Number of spots found : " + newSpots.npoints);

        
        // -----------------------------------------
        // | PARTIE 3 : CRÉATION DE LA TRAJECTOIRE |
        // -----------------------------------------

        
        if (newSpots.npoints == 0) // Si on a pas trouvé de spots
        {
            // Réinitialisation de la trajectoire
            trajectoryHasNotBegun = true;

            // Réinitialisation de la ROI
            zone = new Roi(initialRoiX, initialRoiY, roiSize, roiSize);
            
            // Réinitialisation des spots
            spots = new Polygon();

            // Enregistrement de la trajectoire obtenue
            File f = new File("Trajectory.txt"); // Nommer mieux les fichiers
            try
            {
                if (f.createNewFile())
                {
                    FileWriter fw = new FileWriter(f);
                    for (int i = 0; i < trajectories.size(); i++)
                    {
                        Spot s = trajectories.get(i);
                        fw.write(s.getX() + ", " + s.getY() + ", " + s.getZ());
                    }
                    fw.close();
                }
            }
            catch (IOException e)
            {
                app.getLogManager().logError(e, "An error occured trying to create the file for the trajectory.");
            }

            // Vidage de la trajectoir obtenue jusque là
            trajectories.clear();

            // Mesure du temps de calcul
            end = System.currentTimeMillis();
            processingTime = end - start;

            context.outputImage(image);
            return;
        }
        if (newSpots.npoints == 1) // Si on a trouvé qu'un seul spot dans la ROI
        {
            
            // Tracé de la trajectoire
            for (int i = 1; i <= trajectoryFrame; i++)
            {
                Line line = new Line(trajectories.get(i - 1).getXIndex(), trajectories.get(i - 1).getYIndex(), trajectories.get(i).getXIndex(), trajectories.get(i).getYIndex());
                line.drawPixels(img);
            }
            Spot previous = trajectories.get(trajectoryFrame);
            Line line = new Line(previous.getXIndex(), previous.getYIndex(), newSpots.xpoints[0], newSpots.ypoints[0]);
            line.drawPixels(img);
            
            // Mise à jour de la région d'intérêt
            zone = new Roi(newSpots.xpoints[0] - roiSize/2, newSpots.ypoints[0] - roiSize/2, roiSize, roiSize);
            
            // Calcul de la coordonnée en z
            double z = getZCoordinate(img);
            app.getLogManager().logMessage("Coordonnée en z : " + z);

            // Mise à jour de la trajectoire
            trajectoryFrame++;
            trajectories.put(trajectoryFrame, new Spot(newSpots.xpoints[0], newSpots.ypoints[0], z));

            // Correction du focus
            try
            {
                app.core().setPosition(stageName, z + microscopeZPosition);
                microscopeZPosition += z;
            }
            catch (Exception e)
            {
                app.getLogManager().logError("Microscope focus correction failed");
            }
            
            // Stockage pour l'itération suivante
            spots = newSpots;

            // Génération de l'image pour MicroManager
            image = app.data().ij().createImage(img, image.getCoords(), image.getMetadata());
            
            // Mesure du temps de calcul
            end = System.currentTimeMillis();
            processingTime = end - start;
            app.getLogManager().logMessage("Total processing time : " + processingTime + " ms");
            
            // Rendu de l'image à la pipeline
            context.outputImage(image);
            return;
        }

        // Si on a trouvé plusieurs spots dans la ROI

        // Création d'un vecteur de distances
        double distances[] = new double[newSpots.npoints];
        
        // Stockage du spot trouvé à l'itération précédente pour limiter les appels de méthodes pour gagner du temps
        Spot previous = trajectories.get(trajectoryFrame);
        int x = previous.getXIndex();
        int y = previous.getYIndex();
        
        // Calcul de chaque distance entre le spot de l'itération précédente et les nouveaux trouvés
        for (int i = 0; i < newSpots.npoints; i++)
        {
            double dist = Math.sqrt((x - newSpots.xpoints[i])*(x - newSpots.xpoints[i]) + (y - newSpots.ypoints[i])*(y - newSpots.ypoints[i]));
            distances[i] = dist;
        }

        // Affichage pour débugger
        //System.out.println("Vecteur des distances :");
        //printVector(distances);
        int min = argMin(distances); // Indice du plus proche voisin
        //System.out.println(min);
        
        // Tracé de la trajectoire
        for (int i = 1; i <= trajectoryFrame; i++)
        {
            Line line = new Line(trajectories.get(i - 1).getXIndex(), trajectories.get(i - 1).getYIndex(), trajectories.get(i).getXIndex(), trajectories.get(i).getYIndex());
            line.drawPixels(img);
        }
        Line line = new Line(x, y, newSpots.xpoints[min], newSpots.ypoints[min]);
        line.drawPixels(img);
        
        // Mise à jour de la ROI
        zone = new Roi(newSpots.xpoints[min] - roiSize/2, newSpots.ypoints[min] - roiSize/2, roiSize, roiSize);
        
        // Calcul de la coordonnée en z
        double z = getZCoordinate(img);
        app.getLogManager().logMessage("Coordonnée en z : " + z);

        // Mise à jour de la trajectoire
        trajectoryFrame++;
        trajectories.put(trajectoryFrame, new Spot(newSpots.xpoints[min], newSpots.ypoints[min], z));

        // Correction du focus
        try
        {
            app.core().setPosition(stageName, z + microscopeZPosition);
            microscopeZPosition += z;
        }
        catch (Exception e)
        {
            app.getLogManager().logError("Microscope focus correction failed");
        }

        // Génération de l'image pour MicroManager
        image = app.data().ij().createImage(img, image.getCoords(), image.getMetadata());

        // Stockage
        spots = newSpots;
        
        // Mesure du temps de calcul
        end = System.currentTimeMillis();
        processingTime = end - start;
        app.getLogManager().logMessage("Total processing time : " + processingTime + " ms");

        // Rendu de l'image à la pipeline
        context.outputImage(image);
    }

    /**
     * Finds the index of the minimum in the given array
     * @param vector
     * @return
     */
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

    /**
     * Prints a double array in the console
     * @param vector
     */
    private void printVector(double vector[]) {
        for (int i = 0; i < vector.length; i++)
        {
            System.out.printf("%06.2f", vector[i]);
            System.out.print(" ");
        }
        System.out.println();
    }

    /**
     * Calculates z coordinate relative to the focal plane from input image using an elliptical fit
     * @param
     * @return Double
     */
    private double getZCoordinate(ImageProcessor image) throws NumberFormatException {
        // Creation of a mask to select only the tracked particle
        ImageProcessor img2 = image.duplicate();
        img2.setThreshold((double)ellipseThreshold, (double)UNSIGNED_SHORT_MAX_VALUE, ImageProcessor.NO_LUT_UPDATE);
        ByteProcessor mask = img2.createMask();
        mask.fillOutside(zone);
        img2.setMask(mask);

        // Fitting the ellipse to the mask
        image.setColor(Color.BLUE);
        EllipseFitter ellipseFitter = new EllipseFitter();
        ellipseFitter.fit(img2, null);
        ellipseFitter.drawEllipse(image);
        
        // Decide which waist is which depending on the orientation of the ellipse
        double wx, wy;
        if (ellipseFitter.angle <  45 && ellipseFitter.angle > -45)
        {
            wx = ellipseFitter.minor;
            wy = ellipseFitter.major;
        }
        else
        {
            wx = ellipseFitter.major;
            wy = ellipseFitter.minor;
        }
        
        // Calculation of the z coordinate with the waists and optical parameters
        if (A == 0.0 && B == 0.0) // Linear case
        {
            return -(d*d*(cx*cx - cy*cy) - d*d*d*d*(wx*wx - wy*wy)/(w0*w0))/(2*d*d*(cx-cy));
        }
        if (B == 0.0) // Quadratic case
        {
            double a = -3*A*d*(cx-cy);
            double b = 3*A*d*(cx*cx - cy*cy) - 2*d*d*(cx - cy);
            double c = d*d*(cx*cx - cy*cy) - A*d*(cx*cx*cx - cy*cy*cy) - d*d*d*d/(w0*w0)*(wx*wx - wy*wy);
            double delta = b*b - 4*a*c;
            if (delta < 0)
            {
                throw new NumberFormatException("No real solutions");
            }
            if (delta == 0)
            {
                return -b/(2*a);
            }
            if (delta > 0) // Trouver méthode de sélection
            {
                double z1 = (-b + Math.sqrt(delta))/(2*a);
                double z2 = (-b - Math.sqrt(delta))/(2*a);
                return z1;
            }
        }

        // Cubic case
        // cas delta < 0 => 1 seule solution
        double a1 = -4*B*(cx-cy); // = 0
        double a2 = 6*B*(cx*cx - cy*cy) - 3*A*d*(cx - cy); // = 0
        double a3 = 3*A*d*(cx*cx - cy*cy) - 4*B*(cx*cx*cx - cy*cy*cy) - 2*d*d*(cx - cy); // != 0
        double a4 = d*d*(cx*cx - cy*cy) + B*(cx*cx*cx*cx - cy*cy*cy*cy) - A*d*(cx*cx*cx - cy*cy*cy) - d*d*d*d*(wx*wx - wy*wy)/(w0*w0); // != 0

        double p = (3*a1*a3 - a2*a2)/(3*a1*a1);
        double q = (2*a2*a2*a2 - 9*a1*a2*a3 + 27*a1*a1*a4)/(27*a1*a1*a1);
        double delta = -(q*q/4 + p*p*p/27);
        System.out.println("Delta : " + delta);
        double u1 = -0.5*q + Math.sqrt(q*q/4 + p*p*p/27);
        double u2 = -0.5*q - Math.sqrt(q*q/4 + p*p*p/27);
        return Math.cbrt(u1) + Math.cbrt(u2) - a2/(3*a1);
    }
}
