/**
 * @ Author     ：ValleyFalcon
 * @ Date       ：Created in 16:22 2020/9/17
 * @ Description：可进行局部数据预览、数据视图切换、图片导出的线形图控件。可使用数据视图按钮切换为tableview，tablewview界面可进行数据导出excel和界面关闭；
 * 图片下载按钮可将当前曲线进行保存为图片，需手动选择保存位置；数据预览工具条，可通过滑块进行选择预览区域
 * @ QQ：1761552314
 */
package sample;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import javax.crypto.spec.PSource;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class VfLineChart extends BorderPane {
    private LineChart lineChart;
    public enum OPERATION {
        LEFT_RESIZE,MOVE,RIGHT_RESIZE;

        public AtomicReference<OPERATION> get() {
            return new AtomicReference<>();
        }
    }
    double leftBound = 0;
    double rightBound = 100;

    public VfLineChart(LineChart lineChart) {
        //曲线图，在外部构造好传入即可
        this.lineChart = lineChart;
        //设置个背景，挡住TableView
        this.lineChart.setBackground(new Background(new BackgroundFill(Paint.valueOf("#F3F3F3"),null,null)));
        //曲线数据
        ObservableList<XYChart.Data<Double,Double>> seriesData = ((XYChart.Series<Double,Double>)lineChart.getData().get(0)).getData();
        //承载曲线图、曲线工具按钮
        StackPane chartArea = new StackPane();
        //滚动条到参考系原点的距离
        AtomicReference<Double> distance = new AtomicReference<>((double) 0);
        //创建子曲线，用于快速预览。将原始数据归一化到0-1区间
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart subLineChart = new LineChart(xAxis,yAxis);
        //构造预览工具
        StackPane previewArea = new StackPane();
        previewArea.getChildren().add(subLineChart);
        /*设置子曲线 */
        subLineChart.setMinHeight(USE_PREF_SIZE);
        subLineChart.setPrefHeight(80);
        subLineChart.setLegendVisible(false);
        subLineChart.setCreateSymbols(false);
        //禁用Y轴自适应，用于设置Y轴上界
        yAxis.setAutoRanging(false);
        //设置上界
        yAxis.setUpperBound(1.2);
        //隐藏x、y轴
        xAxis.setOpacity(0);
        yAxis.setOpacity(0);
        //归一化
        double maxY = 0;
        ObservableList<XYData> xyDataObservableList = FXCollections.observableArrayList();
        for (int i=0;i<seriesData.size();i++){
            maxY=(seriesData.get(i).getYValue()>maxY)?seriesData.get(i).getYValue():maxY;
            xyDataObservableList.add(new XYData(seriesData.get(i).getXValue(),seriesData.get(i).getYValue()));
        }
        XYChart.Series subSeries = new LineChart.Series<>();

        for (int i=0;i<seriesData.size();i++){
            subSeries.getData().add(new XYChart.Data((double)i,seriesData.get(i).getYValue()/maxY));
        }
        subLineChart.getData().add(subSeries);
        //构造数据视图按钮和下载按钮
        //构造数据视图
        TableView<XYData> tableView = new TableView<>();
        String xLabel = (lineChart.getXAxis().getLabel()==null)?"x":lineChart.getXAxis().getLabel();
        String yLabel = (lineChart.getYAxis().getLabel()==null)?"y":lineChart.getYAxis().getLabel();
        TableColumn xCol = new TableColumn(xLabel);
        TableColumn yCol = new TableColumn(yLabel);

        xCol.setCellValueFactory(
                new PropertyValueFactory<XYData,Double>("x")
        );
        yCol.setCellValueFactory(
                new PropertyValueFactory<XYData,Double>("y")
        );
        tableView.getColumns().addAll(xCol, yCol);
        tableView.getItems().addAll(xyDataObservableList);
        StackPane tableStackPane = new StackPane();
        tableStackPane.getChildren().add(tableView);

        //导出excel功能
        ImageView exportButton = new ImageView(new Image(getClass().getResourceAsStream("/sample/images/export to excel.png")));
        exportButton.setFitWidth(24);
        exportButton.setFitHeight(24);
        exportButton.setCursor(Cursor.HAND);
        tableStackPane.setAlignment(Pos.TOP_RIGHT);
        StackPane.setMargin(exportButton,new Insets(1,80,0,0));

        exportButton.setOnMousePressed(e->{
            WritableImage image = lineChart.snapshot(new SnapshotParameters(), null);
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("导出Excel");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Excel", "*.xls"));
            File file = fileChooser.showSaveDialog(new Stage());
            //避免空指针异常
            if(file == null){
                return;
            }
            //创建Excel文件薄
            HSSFWorkbook workbook=new HSSFWorkbook();
            //创建工作表sheeet
            HSSFSheet sheet=workbook.createSheet();
            //创建第一行
            HSSFRow row=sheet.createRow(0);
            String[] title={"id",xLabel,yLabel};
            HSSFCell cell=null;
            for (int i=0;i<title.length;i++){
                cell=row.createCell(i);
                cell.setCellValue(title[i]);
            }
            //追加数据
            for (int i=0;i<xyDataObservableList.size();i++){
                HSSFRow nextrow=sheet.createRow(i+1);
                HSSFCell cell2=nextrow.createCell(0);
                cell2.setCellValue(i+1);
                cell2=nextrow.createCell(1);
                cell2.setCellValue(xyDataObservableList.get(i).getX());
                cell2=nextrow.createCell(2);
                cell2.setCellValue(xyDataObservableList.get(i).getY());
            }
            try {
                file.createNewFile();
                FileOutputStream stream= FileUtils.openOutputStream(file);
                workbook.write(stream);
                stream.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        //关闭数据视图功能
        ImageView closeButton = new ImageView(new Image(getClass().getResourceAsStream("/sample/images/closeBtn.png")));
        closeButton.setFitWidth(17);
        closeButton.setFitHeight(17);
        closeButton.setCursor(Cursor.HAND);
        StackPane.setMargin(closeButton,new Insets(4,40,0,0));

        tableStackPane.getChildren().add(exportButton);
        tableStackPane.getChildren().add(closeButton);

        chartArea.getChildren().add(tableStackPane);
        chartArea.getChildren().add(lineChart);
        //切换数据视图功能
        ImageView dataPreviewButton = new ImageView(new Image(getClass().getResourceAsStream("/sample/images/data.png")));
        dataPreviewButton.setFitWidth(30);
        dataPreviewButton.setFitHeight(30);
        dataPreviewButton.setCursor(Cursor.HAND);
        chartArea.setAlignment(Pos.TOP_RIGHT);
        StackPane.setMargin(dataPreviewButton,new Insets(25,80,0,0));

        dataPreviewButton.setOnMousePressed(e->{
            tableStackPane.toFront();
            previewArea.setVisible(false);

        });
        closeButton.setOnMousePressed(e->{
            tableStackPane.toBack();
            previewArea.setVisible(true);
        });
        //保存图片功能
        ImageView downloadButton = new ImageView(new Image(getClass().getResourceAsStream("/sample/images/download.png")));
        downloadButton.setFitWidth(30);
        downloadButton.setFitHeight(30);
        downloadButton.setCursor(Cursor.HAND);
        StackPane.setMargin(downloadButton,new Insets(25,40,0,0));

        downloadButton.setOnMousePressed(e->{
            WritableImage image = lineChart.snapshot(new SnapshotParameters(), null);
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("图片另存为");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("图片", "*.png"),
                    new FileChooser.ExtensionFilter("All Files", "*.*"));
            File file = fileChooser.showSaveDialog(new Stage());
            //避免空指针异常
            if(file == null){
                return;
            }
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png",file);
            } catch (IOException ex) {
            }
        });

        chartArea.getChildren().add(dataPreviewButton);
        chartArea.getChildren().add(downloadButton);
        //数据预览滚动滑块
        Rectangle rectangle = new Rectangle();
        rectangle.setOpacity(0.2);
        Pane pane = new Pane();
        pane.setMaxHeight(50);
        previewArea.getChildren().add(pane);
        pane.getChildren().add(rectangle);
        rectangle.setWidth(500);
        rectangle.setHeight(26);
        rectangle.setX(35);
        rectangle.setFill(Paint.valueOf("#2B2B2B"));
        AtomicReference<OPERATION> operation = OPERATION.LEFT_RESIZE.get();
       //鼠标移动
        rectangle.setOnMouseMoved(e->{
            if((e.getX()-rectangle.getX())>0.9*rectangle.getWidth()){
                rectangle.setCursor(Cursor.E_RESIZE);
            }
            else if((e.getX()-rectangle.getX())<0.1*rectangle.getWidth()){
                rectangle.setCursor(Cursor.E_RESIZE);
            }
            else{
                rectangle.setCursor(Cursor.MOVE);
            }

        });

       //鼠标按下
        rectangle.setOnMousePressed(e->{
            if((e.getX()-rectangle.getX())>0.9*rectangle.getWidth()){
                operation.set(OPERATION.RIGHT_RESIZE);
            }else if((e.getX()-rectangle.getX())<0.1*rectangle.getWidth()){
                operation.set(OPERATION.LEFT_RESIZE);
            }
            else {
                operation.set(OPERATION.MOVE);
                distance.set(e.getX() - rectangle.getX());
            }
        });
        //鼠标拖动
        rectangle.setOnMouseDragged(e->{
            if(operation.get()== OPERATION.RIGHT_RESIZE){
                rectangle.setWidth(e.getX()-rectangle.getX());
                rightBound = rectangle.getX()+rectangle.getWidth();
            }else if(operation.get()== OPERATION.MOVE){
                rectangle.setX(e.getX()-distance.get());
                rightBound = rectangle.getX()+rectangle.getWidth();
            }else{
                rectangle.setX(e.getX());
                rectangle.setWidth(rightBound-e.getX());
            }
        });
        //曲线图放BorderPane中间
        super.setCenter(chartArea);
        super.setBottom(previewArea);

        ChangeListener<Number> chartListener = new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

                if(oldValue.doubleValue()>0.0){
                rectangle.setWidth(newValue.doubleValue()/oldValue.doubleValue()*rectangle.getWidth());
                }
                if(distance.get()>0){
                    rectangle.setX(newValue.doubleValue()/oldValue.doubleValue()*rectangle.getX());
                }

            }
        };
        lineChart.widthProperty().addListener(chartListener);

    }
}
