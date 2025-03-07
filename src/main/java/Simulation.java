import org.jfree.chart.*;
import org.jfree.chart.plot.PiePlot;
import java.io.IOException;

public class Simulation {
    public static void main(String[] args) throws IOException {


        DataProcessor.processDecayDataCSVFile("NuclideData.csv");

        JFreeChart chart = new JFreeChart("Reactor Data", new PiePlot<Integer>());
        ChartFrame frame = new ChartFrame("Visualization", chart);
        frame.setSize(500, 500);
        frame.setVisible(true);
    }
}
