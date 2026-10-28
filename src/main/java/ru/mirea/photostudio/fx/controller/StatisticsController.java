package ru.mirea.photostudio.fx.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.SessionType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Вкладка «Статистика»: показатели в таблице, диаграммы и подсчёт бронирований за выбранный период. */
public class StatisticsController extends TabController {
    private final AppContext ctx = AppContext.get();

    @FXML
    private TableView<Map.Entry<String, String>> statsTable;
    @FXML
    private TableColumn<Map.Entry<String, String>, String> nameColumn;
    @FXML
    private TableColumn<Map.Entry<String, String>, String> valueColumn;
    @FXML
    private PieChart statusChart;
    @FXML
    private BarChart<String, Number> typeChart;
    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private Label periodResult;

    @Override
    protected Node anchor() {
        return statsTable;
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getKey()));
        valueColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getValue()));
        LocalDate today = LocalDate.now();
        fromPicker.setValue(today.withDayOfMonth(1));
        toPicker.setValue(today.withDayOfMonth(today.lengthOfMonth()));
        statusChart.setAnimated(false);
        typeChart.setAnimated(false);
        typeChart.setLegendVisible(false);
        refresh();
    }

    @Override
    public void refresh() {
        try {
            Map<String, String> stats = ctx.statisticsService.collect();
            statsTable.setItems(FXCollections.observableArrayList(stats.entrySet()));

            List<PieChart.Data> pieData = new ArrayList<>();
            for (Map.Entry<BookingStatus, Long> e : ctx.statisticsService.countByStatus().entrySet()) {
                if (e.getValue() > 0) {
                    pieData.add(new PieChart.Data(e.getKey().getTitle() + " (" + e.getValue() + ")", e.getValue()));
                }
            }
            statusChart.setData(FXCollections.observableArrayList(pieData));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (Map.Entry<SessionType, Long> e : ctx.statisticsService.countBySessionType().entrySet()) {
                series.getData().add(new XYChart.Data<String, Number>(e.getKey().getTitle(), e.getValue()));
            }
            typeChart.getData().clear();
            typeChart.getData().add(series);
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onCountPeriod() {
        try {
            long count = ctx.statisticsService.countBookingsBetween(fromPicker.getValue(), toPicker.getValue());
            periodResult.setText("Бронирований за выбранный период: " + count);
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }
}
