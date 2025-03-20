import org.jfree.chart.*;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Simulation extends JPanel {

    public static void main(String[] args) throws IOException, InterruptedException {
        NuclideList nuclideList = DataProcessor.processDecayDataCSVFile("NuclideData.csv");

        JFrame frame = new JFrame("Reactor Simulation");

        frame.setSize(1920, 1080);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                Nuclide[] nuclides = nuclideList.getNuclides();
                for(int i = 0; i < nuclides.length; i++) {
                    int[] pn = nuclides[i].getProtonsNeutrons();
                    double pop = nuclides[i].getPopulation();

                    int actual = 0;
                    if(pop > 255) actual = 255;
                    else if(pop > 0) actual = (int) pop;

                    g.setColor(new Color(actual, 0, 0));
                    g.fillRect(300+ pn[0] * 5, 1000 - (pn[1] * 5), 5, 5);
                }
            }
        };

        frame.add(panel);
        // Make the frame visible
        frame.setVisible(true);
        long start = System.currentTimeMillis();
        int numFrames = 1000;
        for (int frameCount = 0; frameCount <= 10000; frameCount++) {

            if(frameCount % numFrames == 0 && frameCount != 0) {
                System.out.println("Frame: " + frameCount + " time: " + (System.currentTimeMillis() - start) + " Frames per second " + ((1000/(System.currentTimeMillis() - start)) * numFrames));
                System.out.println(frameCount);
                start = System.currentTimeMillis();
            }
            nuclideList.step(2.2195e12);
        }

        Nuclide[] nuclides = nuclideList.getNuclides();
        for(int i = 0; i < nuclides.length; i++) {
            int[] pn = nuclides[i].getProtonsNeutrons();
            double pop = nuclides[i].getPopulation();

            if(pop > 0) {
                System.out.println("NONZERO POP P:" + pn[0] + " N:" + pn[1] + " Pop:" + pop);
            }
        }


    }
    public static double mapRange(double value, double sourceMin, double sourceMax, double targetMin, double targetMax) {
        // Linear mapping formula
        return targetMin + (value - sourceMin) * (targetMax - targetMin) / (sourceMax - sourceMin);
    }

    private static void updateChart(JFreeChart chart) {

    }


}
