package semanticWeather;

import javafx.fxml.FXML;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Pair;

import java.util.ArrayList;

public class Controller {

    @FXML
    private TableView table;
    @FXML
    private TextField sensorId;
    @FXML
    private TextField place;
    @FXML
    private CheckBox observationCheck;
    @FXML
    private TextField elementId;
    @FXML
    private ComboBox element;
    @FXML
    private DatePicker fromDate;
    @FXML
    private DatePicker toDate;
    @FXML
    private Button getData;


    private ModelManager modelManager;


    public Controller(){
        modelManager = ModelManager.getInstance();
    }

    @FXML
    public void initialize(){
        TableColumn<String, Record> column1 = new TableColumn<>("Sensor Id");
        column1.setCellValueFactory(new PropertyValueFactory<>("sensorId"));

        TableColumn<String, Record> column2 = new TableColumn<>("Name");
        column2.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<String, Record> column3 = new TableColumn<>("Municipality");
        column3.setCellValueFactory(new PropertyValueFactory<>("municipality"));

        TableColumn<String, Record> column4 = new TableColumn<>("County");
        column4.setCellValueFactory(new PropertyValueFactory<>("county"));

        TableColumn<String, Record> column5 = new TableColumn<>("Measurement");
        column5.setCellValueFactory(new PropertyValueFactory<>("element"));

        TableColumn<String, Record> column6 = new TableColumn<>("Value");
        column6.setCellValueFactory(new PropertyValueFactory<>("value"));

        TableColumn<String, Record> column7 = new TableColumn<>("Time");
        column7.setCellValueFactory(new PropertyValueFactory<>("date"));

        table.getColumns().add(column1);
        table.getColumns().add(column2);
        table.getColumns().add(column3);
        table.getColumns().add(column4);
        table.getColumns().add(column5);
        table.getColumns().add(column6);
        table.getColumns().add(column7);

        element.getItems().addAll("air_temperature", "best_estimate_mean(air_temperature P1D)", "sum(precipitation_amount P1D)");
    }


    @FXML
    public void getChartData(){

        ArrayList<Pair<String,Float>> data = new ArrayList<>(); //= modelManager.query(placePicker.getText(), fromDate.getValue().toString(), toDate.getValue().toString());


        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1.setName("Series 1");
        ArrayList<String> labels = new ArrayList<>();


        for(Pair<String, Float> entry : data) {
            String key = entry.getKey();
            Float value = entry.getValue();

            labels.add(key);

            series1.getData().add(new XYChart.Data<>(key, value));
        }
        //xAxis.setCategories(FXCollections.observableArrayList(labels));

        //chart1.getData().add(series1);
    }

    /**
     * Populates the tableView based on selected search criteria
     */
    @FXML
    public void getTableData(){
        table.getItems().clear();

        ArrayList<Record> data = queryModel();

        if(data.size() < 1){
            System.out.println("No local data found. \nRequesting data from api.");

            if(observationCheck.isSelected()){
                ArrayList<String> temp = new ArrayList<>();

                if (sensorId.getText().length() < 1 || sensorId.getText() != null){
                    temp.add("sources=" + sensorId.getText());
                }
                if (fromDate.getValue() != null || toDate.getValue() != null){
                    temp.add("referencetime=" + fromDate.getValue().toString() + "%2F" + toDate.getValue().toString());
                }
                if (element.getValue().toString().length() < 1 || element.getValue().toString() != null){
                    temp.add("elements=" + element.getValue().toString().replace(" ", "%20"));
                }
                String[] arguments = new String[1];
                arguments = temp.toArray(arguments);
                modelManager.apiGetObservations(arguments);
            } else {
                modelManager.apiGetSources();
            }

            data = queryModel();
        }

        for (Record r : data) {
            table.getItems().add(r);
        }
    }

    public ArrayList<Record> queryModel(){
        ArrayList<Record> data;
        if(observationCheck.isSelected()){
            System.out.println("1");
            data = modelManager.query(sensorId.getText(), place.getText(), element.getValue().toString(), fromDate.getValue(), toDate.getValue());
        } else {
            System.out.println("2");
            data = modelManager.query(sensorId.getText(), place.getText());
        }
        return data;
    }

    public TextField getPlace() {
        return place;
    }

    public DatePicker getFromDate() {
        return fromDate;
    }

    public DatePicker getToDate() {
        return toDate;
    }
}
