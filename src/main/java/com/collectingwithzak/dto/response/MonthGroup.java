package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

@Data
@AllArgsConstructor
public class MonthGroup<T> {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private String label;
    private LocalDate firstDay;
    private String monthClass;
    private List<T> items;
    private double subtotal;

    public static <T> List<MonthGroup<T>> groupByMonth(List<T> items, Function<T, LocalDate> dateExtractor) {
        List<MonthGroup<T>> groups = new ArrayList<>();
        String current = null;

        for (T item : items) {
            LocalDate date = dateExtractor.apply(item);
            String label = date.format(MONTH_FMT);
            if (!label.equals(current)) {
                String mc = "month-" + date.getMonth().name().toLowerCase();
                groups.add(new MonthGroup<>(label, date, mc, new ArrayList<>(), 0));
                current = label;
            }
            groups.getLast().getItems().add(item);
        }
        return groups;
    }

    public static <T> void computeSubtotals(List<MonthGroup<T>> groups, ToDoubleFunction<T> valueExtractor) {
        for (MonthGroup<T> group : groups) {
            group.setSubtotal(group.getItems().stream().mapToDouble(valueExtractor).sum());
        }
    }
}
