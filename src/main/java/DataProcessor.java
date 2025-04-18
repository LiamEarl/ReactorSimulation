import java.io.*;
import java.util.*;
import org.ejml.simple.SimpleMatrix;

public class DataProcessor {
    private static int numNuclides = 3386; //3386

    public static double[][] processFissionDataTXT(String fissionDataFileLoc) throws IOException {

        /*
            fissionData = {
                {Protons, Neutrons, Amount}
                {Protons, Neutrons, Amount}
                {Protons, Neutrons, Amount}
                ...
            }
         */
        double[][] fissionData = new double[3577][3]; // 3577 is the # of nuclides in the fission data files

        InputStream inputStream = DataProcessor.class.getClassLoader().getResourceAsStream(fissionDataFileLoc);
        if (inputStream == null) {
            throw new FileNotFoundException("File not found in resources");
        }
        double sum = 0;
        int lineNum = 0;
        Scanner txtReader = new Scanner(new InputStreamReader(inputStream));
        while (txtReader.hasNextLine()) {
            String line = txtReader.nextLine();
            line = line.substring(2);
            line = line.replace("  ", ",");
            line = line.replace("   ", ",");
            //System.out.println(line);
            String[] lineData = line.split(",");
            if(lineData.length != 3) continue;
            if(lineData[0].isEmpty() || lineData[1].isEmpty() || lineData[2].isEmpty()) continue;
            fissionData[lineNum][0] = Double.parseDouble(lineData[0]);
            fissionData[lineNum][1] = Double.parseDouble(lineData[1]);
            double amount = Double.parseDouble(lineData[2]);
            fissionData[lineNum][2] = amount > 1e-29 ? amount : 0;
            sum += fissionData[lineNum][2];
            lineNum++;
        }
        //System.out.println("SUM: " + sum);
        return fissionData;
    }

    public static NuclideList nuclideDataToNuclideList(String decayDataLoc, String u235FileLoc, String u238FileLoc) throws IOException {
        /*
        DecayData Structure:
        { // Holds all nuclides
            { // Holds 3 individual nuclide decay modes for each nuclide
                { // Individual nuclide decay mode 1
                    deltaProtons,
                    deltaNeutrons,
                    Probability
                }
                { // Individual nuclide decay mode 2
                    deltaProtons,
                    deltaNeutrons,
                    Probability
                }
                { // Individual nuclide decay mode 3
                    deltaProtons,
                    deltaNeutrons,
                    Probability
                }
            }
        }
        */
        String[] names = new String[numNuclides];
        double[][][] decayData = new double[numNuclides][3][3];
        double[] halfLivesSec = new double[numNuclides];
        int[][] protonsNeutrons = new int[numNuclides][2];

        InputStream inputStream = DataProcessor.class.getClassLoader().getResourceAsStream(decayDataLoc);
        if (inputStream == null) {
            throw new FileNotFoundException("File not found in resources");
        }
        BufferedReader csvReader = new BufferedReader(new InputStreamReader(inputStream));

        int lineNum = 0;
        String line;
        while ((line = csvReader.readLine()) != null) {
            String[] rowData = line.split(",", -1);
            halfLivesSec[lineNum] = rowData[2].isEmpty() ? 1e30 : Double.parseDouble(rowData[2]);
            protonsNeutrons[lineNum] = new int[] {Integer.parseInt(rowData[0]), Integer.parseInt(rowData[1])};

            names[lineNum] = rowData[9].isEmpty() ? "Unknown" : rowData[9];

            for (int i = 0; i < 3; i++) {
                String decayType = rowData[3 + (i * 2)];
                // No probability for decayData yet, we will add that next.
                double[] decayPathData = parseDecayValue(decayType);
                decayData[lineNum][i][0] = decayPathData[0];
                decayData[lineNum][i][1] = decayPathData[1];
                if (decayPathData[2] == -1)
                    rowData[4 + (i * 2)] = ""; // Wipe any probability value for an invalid decay mode
            }

            double[] decayProbabilities = new double[]{
                rowData[4].isEmpty() ? 0f : (Double.parseDouble(rowData[4]) > 0.5 ? Double.parseDouble(rowData[4]) / 100 : 0),
                rowData[6].isEmpty() ? 0f : (Double.parseDouble(rowData[6]) > 0.5 ? Double.parseDouble(rowData[6]) / 100 : 0),
                rowData[8].isEmpty() ? 0f : (Double.parseDouble(rowData[8]) > 0.5 ? Double.parseDouble(rowData[8]) / 100 : 0)
            };

            /*
                If the data point has a cumulative probability less than or greater than 100, we need to fix that.
                I treat the probabilities like a linear equation, where each probability is multiplied by a scale factor.
                That scale factor = 1 / (prob1 + prob2 + prob3)
                Initial faulty probability example: prob1 = 1, prob2 = 1, prob3 = .26 Note: prob. should add up to 1.
                1 / (1 + 1 + .26) = 0.44 Then, multiply each probability by that scale factor and you get the new values.
                (1 * 0.44 + 1 * 0.44 + .26 * 0.44) = 1, so roughly, prob1 = 0.44, prob2 = 0.44, prob3 = 0.11
            */
            double decayProbabilitySum = decayProbabilities[0] + decayProbabilities[1] + decayProbabilities[2];
            double scaleFactor = decayProbabilitySum != 0 ? 1 / decayProbabilitySum : 1; // Avoid divide by 0 errors

            boolean allZero = true;
            for (int i = 0; i < 3; i++) {
                double prob = decayProbabilities[i] * scaleFactor;
                decayData[lineNum][i][2] = prob; // Add the probability to the decayData
                if(prob > 0) {
                    allZero = false;
                } else {
                    decayData[lineNum][i][0] = 0;
                    decayData[lineNum][i][1] = 0;
                }
            }
            if(allZero) // If there are no decay pathways set the half-life very high.
                halfLivesSec[lineNum] = 1e30;

            lineNum++;
        }

        double[][] u235FissionData = processFissionDataTXT(u235FileLoc);
        double[][] u238FissionData = processFissionDataTXT(u238FileLoc);

        return new NuclideList(decayData, halfLivesSec, protonsNeutrons, u235FissionData, u238FissionData, names);
    }

    private static double[] parseDecayValue(String decayType) {
        Map<String, double[]> decayValue = Map.ofEntries(// In the form in Delta Proton, Delta Neutron
            Map.entry("B-", new double[] {1f, -1f}),
            Map.entry("N", new double[] {0f, -1f}),
            Map.entry("2N", new double[] {0f, -2f}),
            Map.entry("P", new double[] {-1f, 0f}),
            Map.entry("B-A", new double[] {-3f, -1f}),
            Map.entry("A", new double[] {-2f, -2f}),
            Map.entry("EC", new double[] {-1f, 1f}),
            Map.entry("EC+B+", new double[] {-1f, 1f}),
            Map.entry("2P", new double[] {-2f, 0f}),
            Map.entry("B+P", new double[] {-2f, 1f}),
            Map.entry("B+", new double[] {-1f, 1f}),
            Map.entry("B-N", new double[] {1f, -2f}),
            Map.entry("B-2N", new double[] {1f, -3f}),
            Map.entry("2B-", new double[] {2f, -2f}),
            Map.entry("ECP", new double[] {-2f, 1f}),
            Map.entry("2EC", new double[] {-2f, 2f}),
            Map.entry("2B+", new double[] {-2f, 2f}),
            Map.entry("IT", new double[] {0f, 0f})
            //Map.entry("SF", new double[] {0f, 0f}), // Ignore SF events
            //Map.entry("ECSF", new double[] {-1f, 1f}) // Ignore SF events
        );
        if(!decayValue.containsKey(decayType)) return new double[] {0, 0, -1};
        double[] result = decayValue.get(decayType);
        return new double[] {result[0], result[1], 1};
    }
}
