package sample;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {
    private LineChart chart;
    private NumberAxis xAxis;
    private NumberAxis yAxis;

    public static double normalDistribution(double x, double u,double seta) {
        return 1.0/(Math.sqrt(2.0*Math.PI)*seta)*Math.exp(
                -0.5*Math.pow((x-u)/seta, 2));
    }
    public XYChart.Series<Double,Double> createSeries(){
        XYChart.Series series = new LineChart.Series<>();
        series.setName("X-Y(0,1)");
        for(double i=-4;i<=4;i+=0.1) {
            series.getData().add(new XYChart.Data(i,normalDistribution(i,0,1)));
        }
        return series;
    }
    public LineChart createContent() {
        xAxis = new NumberAxis();
        yAxis = new NumberAxis();
        xAxis.setLabel("x轴");
        yAxis.setLabel("y轴");
        chart = new LineChart(xAxis, yAxis);
        chart.getData().add(createSeries());
        chart.setTitle("折线图");
        chart.setCreateSymbols(false);
        return chart;
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        BorderPane root = new BorderPane();
        root.setCenter(new VfLineChart(createContent()));
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("仿echart-javafx-LineChart（qq：1761552314）");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
