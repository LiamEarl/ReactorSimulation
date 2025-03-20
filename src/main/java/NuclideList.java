import java.util.Arrays;
import java.util.HashMap;

public class NuclideList {

    private final Nuclide[] nuclides;
    private final double[][][] decayData;

    public NuclideList(double[][][] decayData, double[] halfLives, int[][] protonsNeutrons) {
        this.decayData = decayData;
        HashMap<Integer, Integer> indexFromProtonsNeutrons = new HashMap<>();
        this.nuclides = new Nuclide[decayData.length];

        for(int i = 0; i < this.nuclides.length; i++) {
            this.nuclides[i] = new Nuclide(protonsNeutrons[i], halfLives[i]);
            indexFromProtonsNeutrons.put(Arrays.hashCode((new int[] {protonsNeutrons[i][0], protonsNeutrons[i][1]})), i);
        }

        for(int i = 0; i < decayData.length; i++) {
            for(int j = 0; j < 3; j++) {
                if(decayData[i][j][2] == 0) continue;

                int[] decayPN = protonsNeutrons[i].clone();
                decayPN[0] += (int) decayData[i][j][0];
                decayPN[1] += (int) decayData[i][j][1];
                int hashedDecayPn = Arrays.hashCode(decayPN);
                if(!indexFromProtonsNeutrons.containsKey(hashedDecayPn)) {
                    //System.out.println("Failed: " + (i+1) + " Decay Path " + (j+1) + " original: " + Arrays.toString(protonsNeutrons[i]) + " After Decay" + Arrays.toString(decayPN));
                    continue;
                }
                //System.out.println("Successful: " + (i+1) + " Decay Path " + (j+1));
                int afterDecayIndex = indexFromProtonsNeutrons.getOrDefault(hashedDecayPn, -1);
                this.nuclides[i].addDecayMode(afterDecayIndex, decayData[i][j][2]);
            }
        }

        double numFe56BillionAtoms = 2.15e20; // 2000kgs worth of Iron 56, at a 1 to 1,000,000,000 ratio
        double numU235BillionAtoms = 3.84e17; // 150 kgs worth of Ur. 235, at a 1 to 1,000,000,000 ratio
        double numU238BillionAtoms = 2.15e18; // 850 kgs worth of Ur. 238, at a 1 to 1,000,000,000 ratio
        int Fe56Index = indexFromProtonsNeutrons.get(Arrays.hashCode(new int[] {26, 30}));
        int U235Index = indexFromProtonsNeutrons.get(Arrays.hashCode(new int[] {92, 143}));
        int U238Index = indexFromProtonsNeutrons.get(Arrays.hashCode(new int[] {92, 146}));
        if(U238Index == -1 || U235Index == -1 || Fe56Index == -1) {
            System.out.println("Failed to determine the index of Uranium or Iron");
            System.exit(1);
        }
        this.nuclides[Fe56Index].setPopulation(numFe56BillionAtoms);
        this.nuclides[U235Index].setPopulation(numU235BillionAtoms);
        this.nuclides[U238Index].setPopulation(numU238BillionAtoms);
        //printNuclides();
    }

    public void step(double deltaTime) {
        double[] deltaPopulation = new double[this.nuclides.length];

        for(int i = 0; i < this.nuclides.length; i++) {
            deltaPopulation[i] = 0;
        }

        for(int i = 0; i < this.nuclides.length; i++) {
            Nuclide nuclide = this.nuclides[i];
            double population = nuclide.getPopulation();
            if(population == 0) continue;
            double[][] decayModes = nuclide.getDecayModes().clone();
            double totalDecayAmount = ((population * Math.pow(Math.E, -nuclide.getDecayConstant() * deltaTime)) - population);
            if(population - totalDecayAmount < 0) {
                double amountScalar = population / totalDecayAmount;
                decayModes[0][1] = decayModes[0][1] * amountScalar;
                decayModes[1][1] = decayModes[1][1] * amountScalar;
                decayModes[2][1] = decayModes[2][1] * amountScalar;
                totalDecayAmount *= amountScalar;
            }
            deltaPopulation[i] = deltaPopulation[i] + totalDecayAmount;
            for(int j = 0; j < 3; j++) {
                double change = totalDecayAmount * decayModes[j][1];
                deltaPopulation[(int) decayModes[j][0]] = deltaPopulation[(int) decayModes[j][0]] + ((change < 0) ? -change : 0);
            }
        }

        for(int i = 0; i < this.nuclides.length; i++) {
            this.nuclides[i].addPopulation(deltaPopulation[i]);
        }
    }

    private void printNuclides() {
        for (int i = 0; i < this.nuclides.length; i++) {
            StringBuilder nuclideOutput = new StringBuilder();
            nuclideOutput.append("#").append(i + 1).append("__________________\nHalf-Life: ").append(nuclides[i].getHalfLife()).append(" Seconds");
            for (int j = 0; j < 3; j++) {
                nuclideOutput.append("\nDecay Path ").append(j + 1).append(":\n");
                nuclideOutput.append("ΔP: ").append(this.decayData[i][j][0]);
                nuclideOutput.append(", ΔN: ").append(this.decayData[i][j][1]);
                nuclideOutput.append(", %: ").append(this.decayData[i][j][2] * 100);
            }
            System.out.println(nuclideOutput);
        }
    }

    public Nuclide[] getNuclides() {
        return this.nuclides;
    }
}
