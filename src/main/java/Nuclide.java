import java.math.BigDecimal;

public class Nuclide {
    private double population;
    private final int[] protonsNeutrons;
    private final double[][] decayModes;
    private final double decayConstant;
    private final double halfLife;
    Nuclide(int[] protonsNeutrons, double halfLife) {
        this.decayConstant = Math.log(2) / halfLife;
        this.halfLife = halfLife;
        this.protonsNeutrons = protonsNeutrons;
        this.population = 0;
        this.decayModes = new double[3][2];
        for(int i = 0; i < decayModes.length; i++) {
            this.decayModes[i][0] = 0;
            this.decayModes[i][1] = 0;
        }
    }
    public int[] getProtonsNeutrons() {
        return this.protonsNeutrons;
    }
    public double getPopulation() {
        return this.population;
    }
    public void setPopulation(double newPopulation) {
        this.population = newPopulation;
    }
    void addPopulation(double toAdd) {
        this.population += toAdd;
    }
    public double[][] getDecayModes() {
        return this.decayModes;
    }
    public double getHalfLife() {
        return this.halfLife;
    }
    public double getDecayConstant() {
        return this.decayConstant;
    }
    public void addDecayMode(int decayInto, double amount) {
        for(int i = 0; i < this.decayModes.length; i++) {
            if(this.decayModes[i] == null) continue;
            this.decayModes[i][0] = decayInto;
            this.decayModes[i][1] = amount;
            return;
        }
        System.out.println("Failed to add a nuclide decay mode.");
    }
}