import java.io.*;
import java.net.URL;
import java.util.*;

public class DataProcessor {
    private static final int numNuclides = 9; //3386
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
    private static final double[][][] decayData = new double[numNuclides][3][3];
    private static final double[] halfLivesSec = new double[numNuclides];
    private static final HashMap<Integer, Integer> indexFromProtonsNeutrons = new HashMap<>();
    private static final HashMap<Integer, int[]> protonsNeutronsFromIndex = new HashMap<>();

    public static void getDecayMatrix() {
        // Temp array to make changing the decay values easy, once it's converted into a matrix that will be much harder.
        double[][] decayMatrixArray = new double[numNuclides][numNuclides];
        double[] decayConstants = new double[numNuclides];
        double ln2 = Math.log(2);
        for(int i = 0; i < decayConstants.length; i++) {
            decayConstants[i] = ln2 / halfLivesSec[i]; // Get decay constants based on the half life λ = ln(2) / T½
        }
        List<String> lines = new ArrayList<>();
        for(int i = 0; i < decayData.length; i++) {

            Arrays.fill(decayMatrixArray[i], 0);
            decayMatrixArray[i][i] = -1;
            for(int j = 0; j < 3; j++) {
                if(decayData[i][j][2] == 0) continue;

                int[] decayPN = protonsNeutronsFromIndex.get(i).clone();
                decayPN[0] += (int) decayData[i][j][0];
                decayPN[1] += (int) decayData[i][j][1];
                int hashedDecayPn = Arrays.hashCode(decayPN);
                if(!indexFromProtonsNeutrons.containsKey(hashedDecayPn)) {
                    //System.out.println("Failed: " + (i+1) + " Decay Path " + (j+1));
                    continue;
                }
                //System.out.println("Successful: " + (i+1) + " Decay Path " + (j+1));
                int afterDecayIndex = indexFromProtonsNeutrons.get(hashedDecayPn);
                decayMatrixArray[afterDecayIndex][i] = (double) Math.round(decayData[i][j][2] * 100) /100;
                //System.out.println("Writing " + decayData[i][j][2] + " to i:" + afterDecayIndex + " j" + i);
                //int[] testDecayPN = protonsNeutronsFromIndex.get(i).clone();
                //System.out.println("Index: " + i + " Before: P" + testDecayPN[0] + ", N" + testDecayPN[1] + " Expected Change: P" + (int) decayData[i][j][0] + ", N" + (int) decayData[i][j][1] + " => After: P" + decayPN[0] + ", N" + decayPN[1] + " After Index: " + afterDecayIndex);
            }


        }
        try {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < decayMatrixArray.length; i++) {
                //System.out.println(Arrays.toString(decayMatrixArray[i]));
                for (int j = 0; j < decayMatrixArray[i].length; j++) {
                    line.append(decayMatrixArray[i][j]).append(",");
                }
                lines.add(line.toString());
            }
            writeToCSV("DecayMatrix.csv", lines);
        }catch(IOException e) {
            System.out.println("Failed to write to DecayMatrix.csv");
        }
    }

    public static void writeToCSV(String csvName, List<String> dataLines) throws IOException {
        File csvOutputFile = new File(csvName);
        //csvOutputFile.getParentFile().mkdirs();
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            dataLines.stream().forEach(pw::println);
        }
    }


    public static void processDecayDataCSVFile(String fileLoc) throws IOException {
        InputStream inputStream = DataProcessor.class.getClassLoader().getResourceAsStream(fileLoc);
        if (inputStream == null) {
            throw new FileNotFoundException("File not found in resources");
        }
        BufferedReader csvReader = new BufferedReader(new InputStreamReader(inputStream));

        int lineNum = 0;
        String line;
        while ((line = csvReader.readLine()) != null) {
            String[] rowData = line.split(",", -1);
            halfLivesSec[lineNum] = rowData[2].isEmpty() ? 100000000000000000d : Double.parseDouble(rowData[2]);

            int protons = Integer.parseInt(rowData[0]);
            int neutrons = Integer.parseInt(rowData[1]);
            indexFromProtonsNeutrons.put(Arrays.hashCode((new int[]{protons, neutrons})), lineNum);
            protonsNeutronsFromIndex.put(lineNum, new int[] {protons, neutrons});

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
                rowData[4].isEmpty() ? 0f : Double.parseDouble(rowData[4]) / 100,
                rowData[6].isEmpty() ? 0f : Double.parseDouble(rowData[6]) / 100,
                rowData[8].isEmpty() ? 0f : Double.parseDouble(rowData[8]) / 100
            };

            /*
                If the dataset has a cumulative probability less than or greater than 100, we need to fix that.
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
                halfLivesSec[lineNum] = 100000000000000000d;

            lineNum++;
        }
        //printNuclides();
    }

    private static void printNuclides() {
        for (int i = 0; i < DataProcessor.halfLivesSec.length; i++) {
            StringBuilder nuclideOutput = new StringBuilder();
            nuclideOutput.append("#").append(i + 1).append("__________________\nHalf-Life: ").append(DataProcessor.halfLivesSec[i]).append(" Seconds");
            for (int j = 0; j < 3; j++) {
                nuclideOutput.append("\nDecay Path ").append(j + 1).append(":\n");
                nuclideOutput.append("ΔP: ").append(DataProcessor.decayData[i][j][0]);
                nuclideOutput.append(", ΔN: ").append(DataProcessor.decayData[i][j][1]);
                nuclideOutput.append(", %: ").append(DataProcessor.decayData[i][j][2] * 100);
            }
            System.out.println(nuclideOutput);
        }
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
