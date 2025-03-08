import org.jfree.chart.*;
import org.jfree.chart.plot.PiePlot;

import javax.xml.crypto.Data;
import java.io.IOException;

public class Simulation {
    public static void main(String[] args) throws IOException {


        DataProcessor.processDecayDataCSVFile("TestNuclideData.csv");
        DataProcessor.getDecayMatrix();
        JFreeChart chart = new JFreeChart("Reactor Data", new PiePlot<Integer>());
        ChartFrame frame = new ChartFrame("Visualization", chart);
        frame.setSize(500, 500);
        frame.setVisible(true);
    }
}
