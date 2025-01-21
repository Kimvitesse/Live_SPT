package fr.telecom.physique;


// Classe utilisée pour réunir les informations d'un spot
public class Spot {
    // Attributs
    private int x;
    private int y;
    private int radius = 0;

    // Constructeurs
    public Spot(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Spot(int x, int y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    // Méthodes
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getRadius() {
        return radius;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }
}
