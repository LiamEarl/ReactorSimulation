import org.jfree.chart.*;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.xml.crypto.Data;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class Simulation {

    static double totalTime;

    public static void main(String[] args) throws IOException, InterruptedException {

        NuclideList nuclideList = DataProcessor.nuclideDataToNuclideList("NuclideData.csv", "U235Data.txt", "U238Data.txt");

        JFrame frame = new JFrame("Reactor Simulation");

        frame.setSize(1100, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(0, 10, 20));
                g.fillRect(0, 0, getWidth(), getHeight());


                for (int i = 0; i < nuclideList.getPopulations().length; i++) {
                    int[] pn = nuclideList.getProtonsNeutrons()[i];
                    double[][] decayModes = nuclideList.getDecayModes()[i];
                    //double pop = nuclideList.getPopulations()[i];

                    //int actual = (int) (pop > 1e-3 ? 255: 0);
                    //if(actual < 0) actual = 0;
                    //if(actual > 255) actual = 255;
                    g.setColor(new Color(255, 255, 255));
                    g.fillRect(0+(pn[1]+pn[0])*7, 1080 - (pn[0] * 10), 3, 3);

                    for (int j = 0; j < 3; j++) {
                        if (decayModes[j][1] == 0) continue;
                        int[] pn2 = nuclideList.getProtonsNeutrons()[(int) decayModes[j][0]];
                        g.setColor(Color.RED);
                        if (pn2[0] > pn[0]) {
                            g.setColor(Color.BLUE);
                        }
                        if (pn2[0]+pn2[1] > pn[1]+pn[0]) {
                            g.setColor(Color.GREEN);
                        }
                        g.drawLine(0+((pn[1]+pn[0]) * 7) + 1, 1080 - (pn[0] * 10) + 1, 0+((pn2[1]+pn2[0]) * 7) + 1, 1080 - (pn2[0] * 10) + 1);
                    }

                }

                /*for(int i = 0; i < 37; i++) {
                    if(i % 5 == 0) {
                        g.setColor(new Color(0, 255, 0, 150));
                    }else {
                        g.setColor(new Color(255, 255, 255, 100));
                    }
                    if(i <= 24) {
                        g.drawString(String.valueOf(i * 5), 80, 610 - (i * 25));
                        g.drawLine(100, 605 - (i * 25), 1000, 605 - (i * 25));
                    }
                    g.drawString(String.valueOf(i*5),95 + (i * 25), 625);
                    g.drawLine(100 + (i * 25),0, 100 + (i * 25), 605);
                }
                g.setColor(Color.WHITE);
                g.drawString("Protons", 20, 300);
                g.drawString("Neutrons", 350, 650);
                //g.drawString("Time Elapsed: " + ((double)Math.round(totalTime*10)/10) + " Seconds, " + ((double)Math.round((totalTime/3600)*10)/10) + " Hours, " + ((double)Math.round((totalTime/86400)*10)/10) + " Days, " + ((double)Math.round((totalTime/3.154e7)*1000)/1000) + " Years.", 450, 650);

            }
                 */
            }
        };
        /*
        HashSet<Integer> toGraph = new HashSet<>();

        long start = System.currentTimeMillis();

        int framesBetweenObservation = 50000;
        int totalFrames = 8640000;
        double timeStep = 0.01;
        double[][] populationOverTime = new double[nuclideList.getPopulations().length][(totalFrames / framesBetweenObservation) + 1];

        for (int frameCount = 0; frameCount <= totalFrames; frameCount++) {
            if(frameCount % framesBetweenObservation == 0) {
                double dt = (System.currentTimeMillis() - start);
                if(frameCount != 0)
                    System.out.println("Frame: " + frameCount + ", Time Taken: " + Math.round(dt) + " milliseconds, Frames per second: " + (Math.round((1000/dt) * framesBetweenObservation)));

                double[] populations = nuclideList.getPopulations();
                for(int i = 0; i < populations.length; i++) {
                    double population = populations[i];
                    populationOverTime[i][frameCount / framesBetweenObservation] = population;
                    if(population > 1e-7) {
                        toGraph.add(i);
                    }
                }

                start = System.currentTimeMillis();
            }
            /*
            if(frameCount % 50000 == 0) {
                double sum = 0;
                Nuclide[] nuclides = nuclideList.getNuclides();
                for(int i = 0; i < nuclides.length; i++) {
                    int[] pn = nuclides[i].getProtonsNeutrons();
                    double pop = nuclides[i].getPopulation();

                    if(pop > 0) {
                        System.out.println("NONZERO POP P:" + pn[0] + " N:" + pn[1] + " Pop:" + pop);
                        sum += pop;
                    }
                }
                System.out.println(sum);
            }

            totalTime += timeStep;
            nuclideList.step(timeStep);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeriesCollection endProductsDataset = new XYSeriesCollection();
        XYSeries endProducts = new XYSeries("Products");
        double[] amnAmounts = new double[350];
        Arrays.fill(amnAmounts, 1e-9);
        Object[] toGraphArray = toGraph.toArray();

        double[] populations = nuclideList.getPopulations();
        int[][] protonsNeutrons = nuclideList.getProtonsNeutrons();
        String[] names = nuclideList.getNames();
        for(int i = 0; i < populationOverTime.length; i++) {//toGraphArray.length; i++) {
            //if(!(toGraphArray[i] instanceof Integer)) continue;
            int index = i;//(int) toGraphArray[i];

            XYSeries series = new XYSeries(names[i] + (protonsNeutrons[i][0] + protonsNeutrons[i][1]) + " " + Arrays.toString(protonsNeutrons[i]));
            for(int j = 0; j < populationOverTime[index].length; j++) {
                series.add((j * timeStep * framesBetweenObservation) / 86400, populationOverTime[index][j]);
            }

            amnAmounts[protonsNeutrons[i][0] + protonsNeutrons[i][1]] += populationOverTime[i][populationOverTime[i].length-1];

            dataset.addSeries(series);
        }
        for (int i = 0; i < amnAmounts.length; i++) {
            endProducts.add(i, amnAmounts[i]);
        }

        endProductsDataset.addSeries(endProducts);
        ChartPanel chartPanel = getPopulationOverAMN(endProductsDataset);//getPopulationOverTimeChartPanel(dataset);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        */
        frame.add(panel);
        panel.setVisible(true);
        frame.setVisible(true);
        System.out.println("U235 Population");
    }

    private static ChartPanel getPopulationOverTimeChartPanel(XYSeriesCollection dataset) {
        Color chartColor = new Color(0, 0, 20);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setDefaultStroke(new BasicStroke(3f));
        LogAxis logAxis = new LogAxis("Population");
        logAxis.setBase(2);
        logAxis.setRange(1e-10, 1e21);//1e21);
        logAxis.setMinorTickCount(0);
        XYPlot plot = new XYPlot(dataset, new NumberAxis("Time (days)"), logAxis, renderer);
        plot.setBackgroundPaint(chartColor);
        JFreeChart chart = new JFreeChart("Decay Simulation", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(chartColor);
        chart.setBorderPaint(Color.WHITE);
        chart.getTitle().setPaint(Color.WHITE);
        ValueAxis xAxis = chart.getXYPlot().getDomainAxis();
        ValueAxis yAxis = chart.getXYPlot().getRangeAxis();
        xAxis.setTickLabelPaint(Color.WHITE);
        yAxis.setTickLabelPaint(Color.WHITE);
        xAxis.setLabelPaint(Color.WHITE);
        yAxis.setLabelPaint(Color.WHITE);
        LegendTitle legend = chart.getLegend();
        legend.setBackgroundPaint(chartColor);
        legend.setItemPaint(Color.WHITE);
        return new ChartPanel(chart);
    }

    private static ChartPanel getPopulationOverAMN(XYSeriesCollection dataset) {
        Color chartColor = new Color(0, 0, 20);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setDefaultStroke(new BasicStroke(3f));
        LogAxis logAxis = new LogAxis("Population");
        logAxis.setBase(10);
        logAxis.setRange(1e-9, 1e21);//1e21);
        logAxis.setMinorTickCount(0);
        XYPlot plot = new XYPlot(dataset, new NumberAxis("AMU"), logAxis, renderer);
        plot.setBackgroundPaint(chartColor);
        JFreeChart chart = new JFreeChart("Decay Simulation", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(chartColor);
        chart.setBorderPaint(Color.WHITE);
        chart.getTitle().setPaint(Color.WHITE);
        ValueAxis xAxis = chart.getXYPlot().getDomainAxis();
        ValueAxis yAxis = chart.getXYPlot().getRangeAxis();
        xAxis.setTickLabelPaint(Color.WHITE);
        yAxis.setTickLabelPaint(Color.WHITE);
        xAxis.setLabelPaint(Color.WHITE);
        yAxis.setLabelPaint(Color.WHITE);

        return new ChartPanel(chart);
    }

    public static double mapRange(double value, double sourceMin, double sourceMax, double targetMin, double targetMax) {
        // Linear mapping formula
        return targetMin + (value - sourceMin) * (targetMax - targetMin) / (sourceMax - sourceMin);
    }


}
