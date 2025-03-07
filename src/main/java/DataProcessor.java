import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class DataProcessor {
    public static void processDecayDataCSVFile(String fileLoc) throws IOException {
        int numNuclides = 3386;

        /*
        DecayData Structure:
        { // Holds all nuclides
            { // Holds 3 individual nuclide decay modes for each nuclide
                { // Individual nuclide decay mode
                    deltaProtons,
                    deltaNeutrons,
                    Probability
                }
                { // Individual nuclide decay mode
                    deltaProtons,
                    deltaNeutrons,
                    Probability
                }
                { // Individual nuclide decay mode
                    deltaProtons,
                    deltaNeutrons,
                    Probability
                }
            }
        }
         */
        float[][][] decayData = new float[numNuclides][3][3];
        double[] halfLivesSec = new double[numNuclides];

        URL resource = DataProcessor.class.getClassLoader().getResource(fileLoc);
        if (resource == null) {
            throw new IllegalArgumentException("File not found!");
        }

        InputStream inputStream = DataProcessor.class.getClassLoader().getResourceAsStream("NuclideData.csv");
        if (inputStream == null) {
            throw new FileNotFoundException("File not found in resources");
        }
        BufferedReader csvReader = new BufferedReader(new InputStreamReader(inputStream));

        int lineNum = 0;
        String line;
        while ((line = csvReader.readLine()) != null) {
            String[] rowData = line.split(",", -1);
            halfLivesSec[lineNum] = rowData[2].isEmpty() ? 100000000000000000d : Double.parseDouble(rowData[2]);

            for (int i = 0; i < 3; i++) {
                String decayType = rowData[3 + (i * 2)];
                // No probability for decayData yet, we will add that next.
                float[] decayPathData = parseDecayValue(decayType);
                decayData[lineNum][i][0] = decayPathData[0];
                decayData[lineNum][i][1] = decayPathData[1];
                if (decayPathData[2] == -1)
                    rowData[4 + (i * 2)] = ""; // Wipe any probability value for an invalid decay mode
            }

            float[] decayProbabilities = new float[]{
                rowData[4].isEmpty() ? 0f : Float.parseFloat(rowData[4]) / 100,
                rowData[6].isEmpty() ? 0f : Float.parseFloat(rowData[6]) / 100,
                rowData[8].isEmpty() ? 0f : Float.parseFloat(rowData[8]) / 100
            };

            /*
                If the dataset has a cumulative probability less than or greater than 100, we need to fix that.
                I treat the probabilities like a linear equation, where each probability is multiplied by a scale factor.
                That scale factor = 1 / (prob1 + prob2 + prob3)
                Initial faulty probability example: prob1 = 1, prob2 = 1, prob3 = .26 Note: prob. should add up to 1.
                1 / (1 + 1 + .26) = 0.44 Then, multiply each probability by that scale factor and you get the new values.
                (1 * 0.44 + 1 * 0.44 + .26 * 0.44) = 1, so roughly, prob1 = 0.44, prob2 = 0.44, prob3 = 0.11
            */
            float decayProbabilitySum = decayProbabilities[0] + decayProbabilities[1] + decayProbabilities[2];
            float scaleFactor = decayProbabilitySum != 0 ? 1 / decayProbabilitySum : 1; // Avoid divide by 0 errors

            boolean allZero = true;
            for (int i = 0; i < 3; i++) {
                float prob = decayProbabilities[i] * scaleFactor;
                decayData[lineNum][i][2] = prob; // Add the probability to the decayData
                if(prob > 0) allZero = false;
            }
            if(allZero) // If there are no decay pathways set the half-life very high.
                halfLivesSec[lineNum] = 100000000000000000d;

            lineNum++;
        }
        printNuclides(decayData, halfLivesSec);
    }

    private static void printNuclides(float[][][] decayData, double[] halfLivesSec) {
        for (int i = 0; i < halfLivesSec.length; i++) {
            StringBuilder nuclideOutput = new StringBuilder();
            nuclideOutput.append("#" + (i + 1) + "__________________\nHalf-Life: " + halfLivesSec[i] + " Seconds");
            for (int j = 0; j < 3; j++) {
                nuclideOutput.append("\nDecay Path " + (j + 1) + ":\n");
                nuclideOutput.append("ΔP: " + decayData[i][j][0]);
                nuclideOutput.append(", ΔN: " + decayData[i][j][1]);
                nuclideOutput.append(", %: " + decayData[i][j][2] * 100);
            }
            System.out.println(nuclideOutput);
        }
    }

    private static float[] parseDecayValue(String decayType) {
        Map<String, float[]> decayValue = Map.ofEntries(// In the form in Delta Proton, Delta Neutron
            Map.entry("B-", new float[] {1f, -1f}),
            Map.entry("N", new float[] {0f, -1f}),
            Map.entry("2N", new float[] {0f, -2f}),
            Map.entry("P", new float[] {-1f, 0f}),
            Map.entry("B-A", new float[] {-3f, -1f}),
            Map.entry("A", new float[] {-2f, -2f}),
            Map.entry("EC", new float[] {-1f, 1f}),
            Map.entry("EC+B+", new float[] {-1f, 1f}),
            Map.entry("2P", new float[] {-2f, 0f}),
            Map.entry("B+P", new float[] {-2f, 1f}),
            Map.entry("B+", new float[] {-1f, 1f}),
            Map.entry("B-N", new float[] {1f, -2f}),
            Map.entry("B-2N", new float[] {1f, -3f}),
            Map.entry("2B-", new float[] {2f, -2f}),
            Map.entry("ECP", new float[] {-2f, 1f}),
            Map.entry("2EC", new float[] {-2f, 2f}),
            Map.entry("2B+", new float[] {-2f, 2f}),
            Map.entry("IT", new float[] {0f, 0f})
            //Map.entry("SF", new float[] {0f, 0f}), // Ignore SF events
            //Map.entry("ECSF", new float[] {-1f, 1f}) // Ignore SF events
        );
        if(!decayValue.containsKey(decayType)) return new float[] {0, 0, -1};
        float[] result = decayValue.get(decayType);
        return new float[] {result[0], result[1], 1};
    }
}
