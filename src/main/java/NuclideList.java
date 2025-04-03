import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;

public class NuclideList {
    @Getter
    private final double[] populations;
    @Getter
    private final String[] names;
    @Getter
    private final double[][][] decayModes;
    private final double[] u235FissionAmounts;
    private final double[] u238FissionAmounts;
    private final double[] decayConstants;
    private final double[] halfLives;
    @Getter
    private final int[][] protonsNeutrons;
    private final double[] deltaPopulation;
    private final double[][][] decayData;
    private HashMap<Integer, Integer> indexFromProtonsNeutrons = new HashMap<>();

    public NuclideList(double[][][] decayData, double[] halfLives, int[][] protonsNeutrons, double[][] u235FissionData, double[][] u238FissionData, String[] names) {
        this.decayData = decayData;
        this.halfLives = halfLives;
        this.protonsNeutrons = protonsNeutrons;
        this.names = names;
        this.populations = new double[halfLives.length];
        this.u235FissionAmounts = new double[halfLives.length];
        this.u238FissionAmounts = new double[halfLives.length];
        this.decayConstants = new double[halfLives.length];
        this.decayModes = new double[halfLives.length][3][2];
        this.deltaPopulation = new double[halfLives.length];
        // Each element of the fission dataset must be matched with the nuclide dataset by protons and neutrons vs index.
        HashMap<Integer, Double> u235Fissions = new HashMap<>();
        HashMap<Integer, Double> u238Fissions = new HashMap<>();
        for(int i = 0; i < u235FissionData.length; i++) {
            int keyU235 = hashProtonsNeutrons(new int[] {(int) u235FissionData[i][0], (int) u235FissionData[i][1]});
            int keyU238 = hashProtonsNeutrons(new int[] {(int) u238FissionData[i][0], (int) u238FissionData[i][1]});
            u235Fissions.put(keyU235, u235FissionData[i][2]);
            u238Fissions.put(keyU238, u238FissionData[i][2]);
        }

        for(int i = 0; i < halfLives.length; i++) {
            int fissionDataKey = hashProtonsNeutrons(protonsNeutrons[i]);
            double u235FissionAmount = u235Fissions.getOrDefault(fissionDataKey, 0d);
            double u238FissionAmount = u238Fissions.getOrDefault(fissionDataKey, 0d);
            this.u235FissionAmounts[i] = u235FissionAmount;
            this.u238FissionAmounts[i] = u238FissionAmount;
            this.decayConstants[i] = Math.log(2) / halfLives[i];
            indexFromProtonsNeutrons.put(hashProtonsNeutrons((new int[] {protonsNeutrons[i][0], protonsNeutrons[i][1]})), i);
        }

        for(int i = 0; i < decayData.length; i++) {
            for(int j = 0; j < 3; j++) {
                if(decayData[i][j][2] == 0) continue;
                int[] decayPN = protonsNeutrons[i].clone();
                decayPN[0] += (int) decayData[i][j][0];
                decayPN[1] += (int) decayData[i][j][1];
                int hashedDecayPn = hashProtonsNeutrons(decayPN);
                if(!indexFromProtonsNeutrons.containsKey(hashedDecayPn)) {
                    //System.out.println("Failed: " + (i+1) + " Decay Path " + (j+1) + " original: " + Arrays.toString(protonsNeutrons[i]) + " After Decay" + Arrays.toString(decayPN));
                    continue;
                }
                //System.out.println("Successful: " + (i+1) + " Decay Path " + (j+1));
                int afterDecayIndex = indexFromProtonsNeutrons.getOrDefault(hashedDecayPn, -1);

                decayModes[i][j][0] = afterDecayIndex;
                decayModes[i][j][1] = decayData[i][j][2];
            }
        }

        double numFe56BillionAtoms = 2.153e19; // 2000kgs worth of Iron 56, at a 1 to 1,000,000,000 ratio
        double numU235BillionAtoms = 3.843e17; // 150 kgs worth of Ur. 235, at a 1 to 1,000,000,000 ratio
        double numU238BillionAtoms = 2.150e18; // 850 kgs worth of Ur. 238, at a 1 to 1,000,000,000 ratio
        int Fe56Index = indexFromProtonsNeutrons.get(hashProtonsNeutrons(new int[] {26, 30}));//{86, 136}));
        int U235Index = indexFromProtonsNeutrons.get(hashProtonsNeutrons(new int[] {92, 143}));
        int U238Index = indexFromProtonsNeutrons.get(hashProtonsNeutrons(new int[] {92, 146}));
        if(U238Index == -1 || U235Index == -1 || Fe56Index == -1) {
            System.out.println("Failed to determine the index of Uranium or Iron");
            System.exit(1);
        }
        this.populations[Fe56Index] = numFe56BillionAtoms;
        this.populations[U235Index] = numU235BillionAtoms;
        this.populations[U238Index] = numU238BillionAtoms;
        //printNuclides();
    }

    public void step(double deltaTime) {
        int u235Index = indexFromProtonsNeutrons.get(hashProtonsNeutrons(new int[] {92, 143}));
        int u238Index = indexFromProtonsNeutrons.get(hashProtonsNeutrons(new int[] {92, 146}));
        double u235Population = this.populations[u235Index];
        double u238Population = this.populations[u238Index];
        double u235PopRatio = u235Population / (u235Population + u238Population);
        double u238PopRatio = u238Population / (u235Population + u238Population);
        double U238FRatio = 0.3064 / (0.3064 + 1.218);
        double U235FRatio = 1.2180 / (0.3064 + 1.218);

        //The number of fissions must be divided by 1 billion because the population of atoms is at a 1 to 1 billion ratio
        double totalFissions = ((10000000 / 3.24e-11) * deltaTime) / 1e9; //10 million joules / joules released from single fission
        double numU235Fissions = totalFissions * ((U235FRatio * u235PopRatio) / ((U235FRatio * u235PopRatio) + (U238FRatio * u238PopRatio)));
        double numU238Fissions = totalFissions * ((U238FRatio * u238PopRatio) / ((U235FRatio * u235PopRatio) + (U238FRatio * u238PopRatio)));

        deltaPopulation[u235Index] -= numU235Fissions;
        deltaPopulation[u238Index] -= numU238Fissions;

        for(int i = 0; i < this.populations.length; i++) {
            double population = this.populations[i];

            deltaPopulation[i] += this.u235FissionAmounts[i] * numU235Fissions;
            deltaPopulation[i] += this.u238FissionAmounts[i] * numU238Fissions;
            double decayConstant = this.decayConstants[i];
            if(population == 0 || decayConstant < 1e-30) continue;
            double totalDecayAmount = ((population * Math.pow(Math.E, -decayConstant * deltaTime)) - population);
            deltaPopulation[i] += totalDecayAmount;

            for(int j = 0; j < 3; j++) {
                int newIndex = (int) decayModes[i][j][0];
                double change = -totalDecayAmount * decayModes[i][j][1];
                if(change <= 0) continue;
                deltaPopulation[newIndex] += change;
            }
        }

        for(int i = 0; i < this.populations.length; i++) {
            this.populations[i] += deltaPopulation[i];
            this.deltaPopulation[i] = 0;
        }
    }

    private void printNuclides() {
        for (int i = 0; i < this.populations.length; i++) {
            StringBuilder nuclideOutput = new StringBuilder();
            int[] pn = this.protonsNeutrons[i];
            nuclideOutput.append("#").append(i + 1).append(" Name: ").append(this.names[i]).append((pn[0] + pn[1]))
            .append(", PN: ").append(Arrays.toString(pn)).append("__________________\nHalf-Life: ").append(this.halfLives[i]).append(" Seconds");
            double[][] decayModes = this.decayModes[i];
            for (int j = 0; j < 3; j++) {
                int[] dpn = protonsNeutrons[(int) decayModes[j][0]];
                nuclideOutput.append("\nDecay Path ").append(j + 1).append(":\n");
                nuclideOutput.append("Name: ").append((int) decayModes[j][0] == 0 ? "NA" : this.names[(int) decayModes[j][0]]).append((int) decayModes[j][0] == 0 ? "" : (dpn[0] + dpn[1]));
                nuclideOutput.append(" PN: ").append((int) decayModes[j][0] == 0 ? "NA" : Arrays.toString(dpn));
                nuclideOutput.append(", ΔP: ").append(this.decayData[i][j][0]);
                nuclideOutput.append(", ΔN: ").append(this.decayData[i][j][1]);
                nuclideOutput.append(", %: ").append(this.decayData[i][j][2] * 100);
            }
            nuclideOutput.append("\n");
            System.out.println(nuclideOutput);
        }
    }
    private int hashProtonsNeutrons(int[] protonsNeutrons) {
        int p = protonsNeutrons[0];
        int n = protonsNeutrons[1];
        int scaledP = p * 200;
        return scaledP + n;
    }
}
