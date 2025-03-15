// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 08/12/2024
// Date de modification : 14/03/2025
// Version : 0.3

package fr.telecom.physique;


// Classe utilisée pour réunir les informations d'un spot
public class Spot {
    // Attributs
    private double x;
    private double y;
    private double z;
    private int xIndex;
    private int yIndex;

    // Constructeurs
    public Spot(int x, int y, double z) {
        this.x = x*LiveSPTProcessor.conversionRatio;
        this.y = y*LiveSPTProcessor.conversionRatio;
        this.z = z;
        xIndex = x;
        yIndex = y;
    }

    // Méthodes
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
    
    public int getXIndex() {
        return xIndex;
    }

    public int getYIndex() {
        return yIndex;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setXIndex(int x) {
        xIndex = x;
    }

    public void setYIndex(int y) {
        yIndex = y;
    }
}
