import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;

public class Main extends JFrame {
    static Map<Integer, Double> clustersToWcValues = new HashMap<>();
    static Map<Integer, Double> clustersToBcValues = new HashMap<>();

    static List<List<String>> groups = new ArrayList<>();

    public Main() {
        initUI();
    }

    private void initUI() {

        XYDataset dataset = createDataset();
        JFreeChart chart = createChart(dataset);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);
        add(chartPanel);

        pack();
        setTitle("Line chart");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private XYDataset createDataset() {

        XYSeries series = new XYSeries("");
        for (Integer key : clustersToWcValues.keySet()) {
            series.add(key, clustersToWcValues.get(key));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        return dataset;
    }

    private JFreeChart createChart(XYDataset dataset) {

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Liczba segmentow do wskaznika WC",
                "K",
                "WC",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        chart.getLegend().setFrame(BlockBorder.NONE);

        chart.setTitle(new TextTitle("Liczba segmentow do wskaznika WC",
                        new Font("Serif", java.awt.Font.BOLD, 18)
                )
        );

        return chart;
    }

    public static void main(String[] args) {
        List<String> elements = new ArrayList<>();
        if (!isDataSuccessfullyPrepared(elements)) {
            return;
        }

        for (int clusters = 1; clusters < 100; clusters++) {
            int k = clusters;
            int elementsSize = elements.size();
            groups.clear();
            for (int i = 0; i < k; i++) {
                groups.add(new ArrayList<>());
            }

            int k_index[] = new int[k];

            Random rand = new Random();

            while (!uniqueKpointsPicked(k_index)) {
                for (int i = 0; i < k_index.length; i++) {
                    k_index[i] = rand.nextInt(elementsSize - 1);
                }
            }

            String[][] groupCenterAttributes = new String[k][5];
            for (int i = 0; i < k; i++) {
                groupCenterAttributes[i] = elements.get(k_index[i]).split(",");
            }

            boolean pointsDistributionFinished = false;

            while (!pointsDistributionFinished) {
                pointsDistributionFinished = true;
                for (String point : elements) {
                    String[] ns = point.split(",");

                    double[] distances = new double[k];
                    for (int i = 0; i < k; i++) {
                        distances[i] = calculateEuclideanDistance(groupCenterAttributes[i], ns);
                    }

                    for (int i = 0; i < k; i++) {
                        List<String> group = groups.get(i);
                        if (!group.contains(point) && isShortestDistance(i, distances)) {
                            pointsDistributionFinished = false;
                            group.add(point);
                            removePointFromOtherGroups(i, point);
                        }
                    }
                }

                for (int i = 0; i < k; i++) {
                    calculateNewGroupCenters(groups.get(i), groupCenterAttributes[i]);
                }
            }

            double wc = 0;
            double bc = 0;

            for (int i = 0; i < k; i++) {
                List<String> group = groups.get(i);
                for (int j = 0; j < group.size(); j++) {
                    wc += calculateEuclideanDistance(groupCenterAttributes[i], group.get(j).split(","));
                }
            }

            for (int i = 0; i < k; i++) {
                for (int j = 1; j < k; j++) {
                    if (i >= j) {
                        continue;
                    }
                    bc += calculateEuclideanDistance(groupCenterAttributes[i], groupCenterAttributes[j]);
                }
            }

            clustersToWcValues.put(k, wc);
            clustersToBcValues.put(k, bc);
        }

        Main chart = new Main();
        chart.setVisible(true);
        System.out.println("");
    }

    private static void removePointFromOtherGroups(int index, String point) {
        for (int i = 0; i < groups.size(); i++) {
            if (i == index) {
                continue;
            }
            groups.get(i).remove(point);
        }
    }

    private static boolean isShortestDistance(int index, double[] distances) {
        double distance = distances[index];
        for (int i = 0; i < distances.length; i++) {
            if (i == index) {
                continue;
            }
            if (distance > distances[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean uniqueKpointsPicked(int[] points) {
        for (int i = 0; i < points.length; i++) {
            for (int j = 0; j < points.length; j++) {
                if (i == j) {
                    continue;
                }
                if (points[i] == points[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void calculateNewGroupCenters(List<String> group, String[] groupCenterAttributes) {
        double[] sum3 = new double[groupCenterAttributes.length];
        for (String point : group) {
            String[] pointAttributesAsStrings = point.split(",");
            for (int j = 0; j < pointAttributesAsStrings.length; j++) {
                sum3[j] += Double.parseDouble(pointAttributesAsStrings[j]);
            }
        }
        int thirdGroupSize = group.size();
        for (int i = 0; i < sum3.length; i++) {
            groupCenterAttributes[i] = Double.toString(sum3[i] / thirdGroupSize);
        }
    }

    private static double calculateEuclideanDistance(String[] nGroupCenterAttributes, String[] ns) {
        double sum = 0.0;
        for (int i = 0; i < ns.length; i++) {
            double x = Double.parseDouble(ns[i]);
            double y = Double.parseDouble(nGroupCenterAttributes[i]);
            sum += Math.pow(x - y, 2);
        }
        return Math.sqrt(sum);
    }


    private static boolean isDataSuccessfullyPrepared(List<String> elements) {
        File file = new File("segmentacja.txt");
        FileReader fileReader;
        try {
            fileReader = new FileReader(file);
        } catch (FileNotFoundException e) {
            return false;
        }

        BufferedReader reader = new BufferedReader(fileReader);

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String s = line.replaceAll("[\\[\\] ]", "");
                elements.add(s);
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
