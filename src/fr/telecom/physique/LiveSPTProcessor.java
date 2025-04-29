// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 17/03/2025
// Version : 0.8

package fr.telecom.physique;


import java.awt.Color;
import java.awt.Polygon;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.List;
import java.util.Date;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.micromanager.data.Processor;
import org.micromanager.data.ProcessorConfigurator;
import org.micromanager.data.ProcessorContext;
import org.micromanager.data.Image;
import org.micromanager.PropertyMap;
import org.micromanager.Studio;
import org.micromanager.acquisition.AcquisitionEndedEvent;

import com.google.common.eventbus.Subscribe;

import ij.process.ImageProcessor;
import ij.gui.Roi;
import ij.gui.Line;
import ij.plugin.filter.MaximumFinder;

import cz.cuni.lf1.lge.ThunderSTORM.estimators.CylindricalLensZEstimator;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.FullImageFitting;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.LSQFitter;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.EllipticGaussianPSF;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.Molecule;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.MoleculeDescriptor;
import cz.cuni.lf1.lge.ThunderSTORM.detectors.CentroidOfConnectedComponentsDetector;

import ch.epfl.leb.alica.acpack.analyzers.spotcounter.FindLocalMaxima;




/**
 * 
 * Implementation of the tracking algorithm. It relies on the FindLocalMaxima method from ALICA
 * to find the spots and uses a nearest neighbour algorithm to link the spots found between frames.
 * 
 */
public class LiveSPTProcessor implements Processor {
    // Attributs
    private Polygon spots;
    private long processingTime;
    private ConcurrentSkipListMap< Integer, Spot > trajectories;
    private int nbIter;
    private int roiSize;
    private int reducedRoiSize;
    private int initialRoiX;
    private int initialRoiY;
    private int minDistToOtherMax;
    private int detectionThreshold;
    private int height;
    private int width;
    private Roi zone;
    private int trajectoryFrame;
    private Studio app;
    private PropertyMap settings;
    private boolean trajectoryHasNotBegun = true;
    private double Ax;
    private double Ay;
    private double B;
    private double d;
    private double cx;
    private double cy;
    private double w0;
    private String stageName;
    public static double conversionRatio;
    private double microscopeZPosition;
    public static int UNSIGNED_SHORT_MAX_VALUE = 65535;
    private LiveSPTConfigurator configurator;


    // Constructeur(s)
    public LiveSPTProcessor(PropertyMap pm, Studio app) {
        super();
        trajectories = new ConcurrentSkipListMap<>();
        settings = pm;
        this.app = app;
        nbIter = -1;
        trajectoryFrame = 0;
        spots = new Polygon();
        setSettings();
        System.out.println("Ax : " + Ax);
        System.out.println("Ay : " + Ay);
        try
        {
            microscopeZPosition = app.core().getPosition(stageName);
        }
        catch (Exception e)
        {
            app.getLogManager().logError("Failed to get initial position");
        }
        app.events().registerForEvents(this);
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
        

        // Nombre d'itérations
        nbIter++;

        // Tant qu'on ne trouve pas de spots dans la ROI initiale
        if (trajectoryHasNotBegun && spots.npoints == 0)
        {
            if (nbIter == 0) // Première itération, on initialise les attributs
            {
                // Height and width of the image
                height = image.getHeight();
                width = image.getWidth();
                
                // µm per pixels to convert to real coordinates
                conversionRatio = (double)conversionRatio/height;
                
                // Placing the ROI in the center
                if (initialRoiX == -1)
                {
                    initialRoiX = (width - roiSize)/2;
                }
                if (initialRoiY == -1)
                {
                    initialRoiY = (height - roiSize)/2;
                }

                // Retrieving configurator
                List<ProcessorConfigurator> configuratorList = app.data().getApplicationPipelineConfigurators(false);
                for (int i = 0; i < configuratorList.size(); i++)
                {
                    if (configuratorList.get(i) instanceof LiveSPTConfigurator)
                    {
                        configurator = (LiveSPTConfigurator)configuratorList.get(i);
                    }
                }

                // Initialisation de la ROI
                zone = new Roi(initialRoiX, initialRoiY, roiSize, roiSize);
            }
            
            // Création de l'objet adapté pour la traitement des images
            ImageProcessor img = app.data().ij().createProcessor(image);

            // Tracé de la ROI
            img.setColor(Color.BLUE);
            zone.drawPixels(img);
            
            // Détection des spots
            spots = FindLocalMaxima.FindMax(img, zone, minDistToOtherMax, detectionThreshold, FindLocalMaxima.FilterType.NONE);
            app.getLogManager().logMessage("Number of spots found with ALICA: " + spots.npoints);

            //MaximumFinder maximumFinder = new MaximumFinder();
            img.setRoi(zone);
            //spots = maximumFinder.getMaxima(img, detectionThreshold, true);
            //app.getLogManager().logMessage("Number of spots found with ImageJ : " + spots.npoints);

            
            // Initialisation de trajectories
            if (spots.npoints != 0)
            {
                // Mise à jour de la ROI
                zone = new Roi(spots.xpoints[0] - reducedRoiSize/2, spots.ypoints[0] - reducedRoiSize/2, reducedRoiSize, reducedRoiSize);

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
                    app.getLogManager().logError("Microscope focus correction failed.");
                }
                
                trajectoryHasNotBegun = false;
            }

            // Génération de l'image pour Micro-Manager
            image = app.data().ij().createImage(img, image.getCoords(), image.getMetadata());
            
            // Rendu de l'image à la pipeline
            context.outputImage(image);
            return;
        }

        if (updateSettings())
        {
            // Mise à jour du rapport de conversion
            conversionRatio = (double)conversionRatio/height;
            
            // Rendu de l'image à la pipeline
            context.outputImage(image);
            return;
        }
        

        // ----------------------------------
        // | PARTIE 2 : DÉTECTION DES SPOTS |
        // ----------------------------------


        app.getLogManager().logMessage("Appel n°"+ nbIter);

        System.out.println("ratio : " + conversionRatio);

        // Mesure du temps de calcul
        long start = System.currentTimeMillis();
        long end;

        // Création de l'objet pour la traitement de l'image
        ImageProcessor img = app.data().ij().createProcessor(image);

        // Détection des spots
        Polygon newSpots = FindLocalMaxima.FindMax(img, zone, minDistToOtherMax, detectionThreshold, FindLocalMaxima.FilterType.NONE);
        app.getLogManager().logMessage("Number of spots found with ALICA: " + newSpots.npoints);
        //MaximumFinder maximumFinder = new MaximumFinder();
        img.setRoi(zone);
        //newSpots = maximumFinder.getMaxima(img, detectionThreshold, true);
        //app.getLogManager().logMessage("Number of spots found with ImageJ : " + newSpots.npoints);

        
        // -----------------------------------------
        // | PARTIE 3 : CRÉATION DE LA TRAJECTOIRE |
        // -----------------------------------------

        
        if (newSpots.npoints == 0) // Si on a pas trouvé de spots
        {
            resetTracking();

            // Mesure du temps de calcul
            end = System.currentTimeMillis();
            processingTime = end - start;

            // Rendu de l'image à la pipeline
            context.outputImage(image);
            return;
        }
        if (newSpots.npoints == 1) // Si on a trouvé qu'un seul spot dans la ROI
        {    
            // Mise à jour de la région d'intérêt
            zone = new Roi(newSpots.xpoints[0] - reducedRoiSize/2, newSpots.ypoints[0] - reducedRoiSize/2, reducedRoiSize, reducedRoiSize);
            img.setColor(Color.BLUE);
            zone.drawPixels(img);
            
            // Calcul de la coordonnée en z
            double z = getZCoordinate(img);
            app.getLogManager().logMessage("Coordonnée en z : " + z);

            // Mise à jour de la trajectoire
            trajectoryFrame++;
            trajectories.put(trajectoryFrame, new Spot(newSpots.xpoints[0], newSpots.ypoints[0], z));

            // Tracé de la trajectoire
            drawTrajectory(img);

            // Correction du focus
            try
            {
                app.core().setPosition(stageName, z + microscopeZPosition);
                microscopeZPosition += z;
            }
            catch (Exception e)
            {
                app.getLogManager().logError("Microscope focus correction failed.");
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

        // Indice du plus proche voisin
        int min = argMin(distances);
        
        // Mise à jour de la ROI
        zone = new Roi(newSpots.xpoints[min] - reducedRoiSize/2, newSpots.ypoints[min] - reducedRoiSize/2, reducedRoiSize, reducedRoiSize);
        
        // Calcul de la coordonnée en z
        double z = getZCoordinate(img);
        app.getLogManager().logMessage("Coordonnée en z : " + z);

        // Mise à jour de la trajectoire
        trajectoryFrame++;
        trajectories.put(trajectoryFrame, new Spot(newSpots.xpoints[min], newSpots.ypoints[min], z));

        // Tracé de la trajectoire
        drawTrajectory(img);

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
     * Draws the detected trajectory on the image
     * @param image
     */
    private void drawTrajectory(ImageProcessor image) {
        for (int i = 1; i < trajectories.size(); i++)
        {
            Line line = new Line(trajectories.get(i - 1).getXIndex(), trajectories.get(i - 1).getYIndex(), trajectories.get(i).getXIndex(), trajectories.get(i).getYIndex());
            line.drawPixels(image);
        }
    }

    /**
     * Calculates z coordinate relative to the focal plane from input image using an elliptical fit
     * @param image
     * @return Double
     */
    private double getZCoordinate(ImageProcessor image) throws NumberFormatException {
        double wx, wy;

        // Detection of the waists of the ellipses
        image.setRoi(zone);
        ImageProcessor croppedImage = image.crop();

        CylindricalLensZEstimator estimator = new CylindricalLensZEstimator(new FullImageFitting(new LSQFitter(new EllipticGaussianPSF(1.6, 0), false))); // Mettre derniers réglages trouvés
        CentroidOfConnectedComponentsDetector detector = new CentroidOfConnectedComponentsDetector(Integer.toString(detectionThreshold), false);
        List<Molecule> params = estimator.estimateParameters(croppedImage.convertToFloatProcessor(), detector.detectMoleculeCandidates(croppedImage.convertToFloatProcessor()));
        if (params.size() <= 0)
        {
            throw new NumberFormatException("No detection with ThunderSTORM");
        }
        Molecule m = params.get(0);
        MoleculeDescriptor mDesc = m.descriptor;
        if (mDesc.hasParam("sigma1"))
        {
            wx = 2*m.values[mDesc.getParamIndex("sigma1")];
        }
        else
        {
            wx = 0;
        }
        if (mDesc.hasParam("sigma2"))
        {
            wy = 2*m.values[mDesc.getParamIndex("sigma2")];
        }
        else
        {
            wy = 0;
        }

        System.out.println("wx : " + wx);
        System.out.println("wy : " + wy);
        
        // Calculation of the z coordinate with the waists and optical parameters
        if (Ax == 0.0 && Ay == 0.0 && B == 0.0) // Linear case
        {
            return -(d*d*(cx*cx - cy*cy) - d*d*d*d*(wx*wx - wy*wy)/(w0*w0))/(2*d*d*(cx-cy))*conversionRatio;
        }
        if (B == 0.0) // Quadratic case
        {
            double a = -3*d*(Ax*cx-Ay*cy);
            double b = 3*d*(Ax*cx*cx - Ay*cy*cy) - 2*d*d*(cx - cy);
            double c = d*d*(cx*cx - cy*cy) - d*(Ax*cx*cx*cx - Ay*cy*cy*cy) - d*d*d*d/(w0*w0)*(wx*wx - wy*wy);
            double delta = b*b - 4*a*c;
            if (delta < 0)
            {
                throw new NumberFormatException("No real solutions");
            }
            if (delta == 0)
            {
                return -b/(2*a)*conversionRatio;
            }
            if (delta > 0)
            {
                double z1 = (-b + Math.sqrt(delta))/(2*a);
                double z2 = (-b - Math.sqrt(delta))/(2*a);

                // z1 et z2 dans le volume de recherche
                if (Math.abs(z1) < reducedRoiSize/2 && Math.abs(z2) < reducedRoiSize/2)
                {
                    double wx1 = w0*Math.sqrt(1 + ((z1-cx)/d)*((z1-cx)/d) + Ax*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d) + B*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d));
                    double wy1 = w0*Math.sqrt(1 + ((z1-cy)/d)*((z1-cy)/d) + Ay*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d) + B*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d));
                    double wx2 = w0*Math.sqrt(1 + ((z2-cx)/d)*((z2-cx)/d) + Ax*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d) + B*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d));
                    double wy2 = w0*Math.sqrt(1 + ((z2-cy)/d)*((z2-cy)/d) + Ay*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d) + B*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d));
                    double[] distances = new double[2];
                    distances[0] = Math.sqrt((wx-wx1)*(wx-wx1) + (wy-wy1)*(wy-wy1));
                    distances[1] = Math.sqrt((wx-wx2)*(wx-wx2) + (wy-wy2)*(wy-wy2));
                    int min = argMin(distances);
                    if (min == 0)
                    {
                        return z1*conversionRatio;
                    }
                    return z2*conversionRatio;
                }
                
                // z1 en dehors du volume mais z2 dans le volume
                if (Math.abs(z1) >= reducedRoiSize/2 && Math.abs(z2) < reducedRoiSize/2)
                {
                    return z2*conversionRatio;
                }
                
                // z2 en dehors mais z1 dans le volume
                if (Math.abs(z2) >= reducedRoiSize/2 && Math.abs(z1) < reducedRoiSize/2)
                {
                    return z1*conversionRatio;
                }

                // z1 et z2 en dehors du volume
                if (Math.abs(z1) < Math.abs(z2))
                {
                    return z1*conversionRatio;
                }
                return z2*conversionRatio;
            }
        }

        // Cubic case
        double a2 = (6*B*(cx*cx - cy*cy)-3*d*(Ax*cx - Ay*cy))/((Ax - Ay)*d - 4*B*(cx - cy));
        double a1 = (3*d*(Ax*cx*cx - Ay*cy*cy) - 4*B*(cx*cx*cx - cy*cy*cy) - 2*d*d*(cx - cy))/((Ax - Ay)*d - 4*B*(cx - cy));
        double a0 = (d*d*(cx*cx - cy*cy) + B*(cx*cx*cx*cx - cy*cy*cy*cy) - d*(Ax*cx*cx*cx - Ay*cy*cy*cy) - d*d*d*d*(wx*wx - wy*wy)/(w0*w0))/((Ax - Ay)*d - 4*B*(cx - cy));
        double q = (3*a1 - a2*a2)/9;
        double r = (9*a1*a2 - 27*a0 - 2*a2*a2*a2)/54;
        double delta = q*q*q + r*r;
        if (delta > 0.0)
        {
            double z = -a2/3 + Math.cbrt(r + Math.sqrt(delta)) + Math.cbrt(r - Math.sqrt(delta));
            System.out.println(" z : " + z);
            return z*conversionRatio;
        }
        if (delta == 0.0)
        {
            double z1 = -a2/3 + 2*Math.cbrt(r);
            double z2 = -a2/3 - Math.cbrt(r);

            System.out.println("z1 : " + z1);
            System.out.println("z2 : " + z2);

            // z1 et z2 dans le volume de recherche
            if (Math.abs(z1) < reducedRoiSize/2 && Math.abs(z2) < reducedRoiSize/2)
            {
                double wx1 = w0*Math.sqrt(1 + ((z1-cx)/d)*((z1-cx)/d) + Ax*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d) + B*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d));
                double wy1 = w0*Math.sqrt(1 + ((z1-cy)/d)*((z1-cy)/d) + Ay*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d) + B*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d));
                double wx2 = w0*Math.sqrt(1 + ((z2-cx)/d)*((z2-cx)/d) + Ax*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d) + B*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d));
                double wy2 = w0*Math.sqrt(1 + ((z2-cy)/d)*((z2-cy)/d) + Ay*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d) + B*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d));
                double[] distances = new double[2];
                distances[0] = Math.sqrt((wx-wx1)*(wx-wx1) + (wy-wy1)*(wy-wy1));
                distances[1] = Math.sqrt((wx-wx2)*(wx-wx2) + (wy-wy2)*(wy-wy2));
                int min = argMin(distances);
                if (min == 0)
                {
                    return z1*conversionRatio;
                }
                return z2*conversionRatio;
            }

            // z1 en dehors du volume mais z2 dans le volume
            if (Math.abs(z1) >= reducedRoiSize/2 && Math.abs(z2) < reducedRoiSize/2)
            {
                return z2*conversionRatio;
            }

            // z2 en dehors mais z1 dans le volume
            if (Math.abs(z2) >= reducedRoiSize/2 && Math.abs(z1) < reducedRoiSize/2)
            {
                return z1*conversionRatio;
            }

            // z1 et z2 en dehors du volume
            if (Math.abs(z1) < Math.abs(z2))
            {
                return z1*conversionRatio;
            }
            return z2*conversionRatio;
        }

        double theta = Math.acos(r/Math.sqrt(-q*q*q));
        double z1 = 2*Math.sqrt(-q)*Math.cos(theta/3) - a2/3;
        double z2 = 2*Math.sqrt(-q)*Math.cos((theta + 2*Math.PI)/3) - a2/3;
        double z3 = 2*Math.sqrt(-q)*Math.cos((theta + 4*Math.PI)/3) - a2/3;

        System.out.println("z1 : " + z1);
        System.out.println("z2 : " + z2);
        System.out.println("z3 : " + z3);

        // z1, z2, z3 dans le volume
        if (Math.abs(z1) < reducedRoiSize/2 && Math.abs(z2) < reducedRoiSize/2 && Math.abs(z3) < reducedRoiSize/2)
        {
            double wx1 = w0*Math.sqrt(1 + ((z1-cx)/d)*((z1-cx)/d) + Ax*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d) + B*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d));
            double wy1 = w0*Math.sqrt(1 + ((z1-cy)/d)*((z1-cy)/d) + Ay*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d) + B*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d));
            double wx2 = w0*Math.sqrt(1 + ((z2-cx)/d)*((z2-cx)/d) + Ax*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d) + B*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d));
            double wy2 = w0*Math.sqrt(1 + ((z2-cy)/d)*((z2-cy)/d) + Ay*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d) + B*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d));
            double wx3 = w0*Math.sqrt(1 + ((z3-cx)/d)*((z3-cx)/d) + Ax*((z3-cx)/d)*((z3-cx)/d)*((z3-cx)/d) + B*((z3-cx)/d)*((z3-cx)/d)*((z3-cx)/d)*((z3-cx)/d));
            double wy3 = w0*Math.sqrt(1 + ((z3-cy)/d)*((z3-cy)/d) + Ay*((z3-cy)/d)*((z3-cy)/d)*((z3-cy)/d) + B*((z3-cy)/d)*((z3-cy)/d)*((z3-cy)/d)*((z3-cy)/d));
            double[] distances = new double[3];
            distances[0] = Math.sqrt((wx-wx1)*(wx-wx1) + (wy-wy1)*(wy-wy1));
            distances[1] = Math.sqrt((wx-wx2)*(wx-wx2) + (wy-wy2)*(wy-wy2));
            distances[2] = Math.sqrt((wx-wx3)*(wx-wx3) + (wy-wy3)*(wy-wy3));
            int min = argMin(distances);
            if (min == 0)
            {
                return z1*conversionRatio;
            }
            if (min == 1)
            {
                return z2*conversionRatio;
            }
            return z3*conversionRatio;
        }

        // z1 en dehors, z2 et z3 dans le volume
        if (Math.abs(z1) >= reducedRoiSize/2 && Math.abs(z2) < reducedRoiSize/2 && Math.abs(z3) < reducedRoiSize/2)
        {
            double wx1 = w0*Math.sqrt(1 + ((z2-cx)/d)*((z2-cx)/d) + Ax*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d) + B*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d));
            double wy1 = w0*Math.sqrt(1 + ((z2-cy)/d)*((z2-cy)/d) + Ay*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d) + B*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d));
            double wx2 = w0*Math.sqrt(1 + ((z3-cx)/d)*((z3-cx)/d) + Ax*((z3-cx)/d)*((z3-cx)/d)*((z3-cx)/d) + B*((z3-cx)/d)*((z3-cx)/d)*((z3-cx)/d)*((z3-cx)/d));
            double wy2 = w0*Math.sqrt(1 + ((z3-cy)/d)*((z3-cy)/d) + Ay*((z3-cy)/d)*((z3-cy)/d)*((z3-cy)/d) + B*((z3-cy)/d)*((z3-cy)/d)*((z3-cy)/d)*((z3-cy)/d));
            double[] distances = new double[2];
            distances[0] = Math.sqrt((wx-wx1)*(wx-wx1) + (wy-wy1)*(wy-wy1));
            distances[1] = Math.sqrt((wx-wx2)*(wx-wx2) + (wy-wy2)*(wy-wy2));
            printVector(distances);
            int min = argMin(distances);
            if (min == 0)
            {
                return z2*conversionRatio;
            }
            return z3*conversionRatio;
        }
        
        // z2 en dehors, z1 et z3 dans le volume
        if (Math.abs(z2) >= reducedRoiSize/2 && Math.abs(z1) < reducedRoiSize/2 && Math.abs(z3) < reducedRoiSize/2)
        {
            double wx1 = w0*Math.sqrt(1 + ((z1-cx)/d)*((z1-cx)/d) + Ax*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d) + B*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d));
            double wy1 = w0*Math.sqrt(1 + ((z1-cy)/d)*((z1-cy)/d) + Ay*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d) + B*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d));
            double wx2 = w0*Math.sqrt(1 + ((z3-cx)/d)*((z3-cx)/d) + Ax*((z3-cx)/d)*((z3-cx)/d)*((z3-cx)/d) + B*((z3-cx)/d)*((z3-cx)/d)*((z3-cx)/d)*((z3-cx)/d));
            double wy2 = w0*Math.sqrt(1 + ((z3-cy)/d)*((z3-cy)/d) + Ay*((z3-cy)/d)*((z3-cy)/d)*((z3-cy)/d) + B*((z3-cy)/d)*((z3-cy)/d)*((z3-cy)/d)*((z3-cy)/d));
            double[] distances = new double[2];
            distances[0] = Math.sqrt((wx-wx1)*(wx-wx1) + (wy-wy1)*(wy-wy1));
            distances[1] = Math.sqrt((wx-wx2)*(wx-wx2) + (wy-wy2)*(wy-wy2));
            int min = argMin(distances);
            if (min == 0)
            {
                return z1*conversionRatio;
            }
            return z3*conversionRatio;
        }

        // z3 en dehors, z1 et z2 dans le volume
        if (Math.abs(z3) >= reducedRoiSize/2 && Math.abs(z1) < reducedRoiSize/2 && Math.abs(z2) < reducedRoiSize/2)
        {
            double wx1 = w0*Math.sqrt(1 + ((z1-cx)/d)*((z1-cx)/d) + Ax*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d) + B*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d)*((z1-cx)/d));
            double wy1 = w0*Math.sqrt(1 + ((z1-cy)/d)*((z1-cy)/d) + Ay*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d) + B*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d)*((z1-cy)/d));
            double wx2 = w0*Math.sqrt(1 + ((z2-cx)/d)*((z2-cx)/d) + Ax*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d) + B*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d)*((z2-cx)/d));
            double wy2 = w0*Math.sqrt(1 + ((z2-cy)/d)*((z2-cy)/d) + Ay*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d) + B*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d)*((z2-cy)/d));
            double[] distances = new double[2];
            distances[0] = Math.sqrt((wx-wx1)*(wx-wx1) + (wy-wy1)*(wy-wy1));
            distances[1] = Math.sqrt((wx-wx2)*(wx-wx2) + (wy-wy2)*(wy-wy2));
            int min = argMin(distances);
            if (min == 0)
            {
                return z1*conversionRatio;
            }
            return z2*conversionRatio;
        }

        // z1, z2 en dehors, z3 dans le volume
        if (Math.abs(z1) >= reducedRoiSize/2 && Math.abs(z2) >= reducedRoiSize/2 && Math.abs(z3) < reducedRoiSize/2)
        {
            return z3*conversionRatio;
        }

        // z1, z3 en dehors, z2 dans le volume
        if (Math.abs(z1) >= reducedRoiSize/2 && Math.abs(z3) >= reducedRoiSize/2 && Math.abs(z2) < reducedRoiSize/2)
        {
            return z2*conversionRatio;
        }

        // z2, z3 en dehors, z1 dans le volume
        if (Math.abs(z2) >= reducedRoiSize/2 && Math.abs(z3) >= reducedRoiSize/2 && Math.abs(z1) < reducedRoiSize/2)
        {
            return z1*conversionRatio;
        }

        // z1, z2, z3 en dehors du volume
        if (Math.abs(z1) < Math.abs(z2) && Math.abs(z1) < Math.abs(z3))
        {
            return z1*conversionRatio;
        }

        if (Math.abs(z2) < Math.abs(z1) && Math.abs(z2) < Math.abs(z3))
        {
            return z2*conversionRatio;
        }
        return z3*conversionRatio;
    }


    /**
     * Saves the trajectory at the end of an acquisition
     * @param e
     */
    @Subscribe
    public void saveTrajectory(AcquisitionEndedEvent e) {
        saveTrajectoryInFile();
    }


    /**
     * Saves the current trajectory in a .txt file
     * TODO : add directory choice
     */
    private void saveTrajectoryInFile() {
        Date d = new Date();
        File f = new File("LiveSPT-Trajectory-" + d + ".txt");
        try
        {
            if (f.createNewFile())
            {
                FileWriter fw = new FileWriter(f);
                for (int i = 0; i < trajectories.size(); i++)
                {
                    Spot s = trajectories.get(i);
                    fw.write(s.getX() + ", " + s.getY() + ", " + s.getZ() + "\n");
                }
                fw.close();
            }
        }
        catch (IOException e)
        {
            app.getLogManager().logError(e, "An error occured trying to create the file for the trajectory.");
        }
    }


    /**
     * Updates the settings of the plugin and resets tracking
     */
    public boolean updateSettings() {
        if (configurator.property_map.equals(settings))
        {
            return false;
        }
        settings = configurator.getSettings();
        setSettings();
        try
        {
            microscopeZPosition = app.core().getPosition(stageName);
        }
        catch (Exception exception)
        {
            app.getLogManager().logError("Failed to get initial position.");
        }
        resetTracking();
        return true;
    }


    /**
     * Resets the tracking algorithm
     */
    private void resetTracking() {
        // Réinitialisation de la trajectoire
        trajectoryHasNotBegun = true;

        // Réinitialisation de la ROI
        zone = new Roi(initialRoiX, initialRoiY, roiSize, roiSize);
        
        // Réinitialisation des spots
        spots = new Polygon();

        // Enregistrement de la trajectoire obtenue
        saveTrajectoryInFile();

        // Vidage de la trajectoire obtenue jusque là
        trajectories.clear();
        trajectoryFrame = 0;
    }


    /**
     * Sets the attributes of the plugin according to the PropertyMap
     */
    private void setSettings() {
        roiSize = settings.getInteger("RoiSize", 80); // Valeur à récupérer, valeur par défaut
        initialRoiX = settings.getInteger("InitialRoiX", -1);
        initialRoiY = settings.getInteger("InitialRoiY", -1);
        minDistToOtherMax = settings.getInteger("MinDistToOtherMax", 2);
        detectionThreshold = settings.getInteger("DetectionThreshold", 150);
        reducedRoiSize = settings.getInteger("ReducedRoiSize", 20);
        Ax = settings.getDouble("Ax", -0.01494);
        Ay = settings.getDouble("Ay", 0.167);
        B = settings.getDouble("B", -0.1299);
        d = settings.getDouble("d", 4.141);
        cx = settings.getDouble("cx", -1.755);
        cy = settings.getDouble("cy", 0.1064);
        w0 = settings.getDouble("w0", 1.093);
        conversionRatio = settings.getInteger("RealSize", 82);
        stageName = settings.getString("StageName", "PIZstage");
    }

    /*private void setThresholds() {
        SliderConfigWindow frame = new SliderConfigWindow(null, detectionThreshold, ellipseThreshold);
        frame.setVisible(true);
        
        
        // Image for ellipse threshold
        ImageProcessor img = app.data().ij().createProcessor(image);
        img.setThreshold((double)frame.ellipseThreshold, (double)UNSIGNED_SHORT_MAX_VALUE, ImageProcessor.NO_LUT_UPDATE);
        ByteProcessor mask = img.createMask();
        mask.fillOutside(zone);
        image = app.data().ij().createImage(mask, image.getCoords(), image.getMetadata());
        app.live().displayImage(image);
    } */
}
