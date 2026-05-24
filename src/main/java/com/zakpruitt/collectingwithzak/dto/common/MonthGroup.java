package com.zakpruitt.collectingwithzak.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthGroup<T> {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private String label;
    private LocalDate firstDay;
    private String monthClass;
    private List<T> items;
    private double subtotal;

    public static <T> List<MonthGroup<T>> groupByMonth(List<T> items,
                                                       Function<T, LocalDate> dateExtractor,
                                                       ToDoubleFunction<T> valueExtractor) {
        List<MonthGroup<T>> groups = groupByMonth(items, dateExtractor);
        for (MonthGroup<T> group : groups) {
            group.subtotal = group.items.stream().mapToDouble(valueExtractor).sum();
        }
        return groups;
    }

    public static <T> List<MonthGroup<T>> groupByMonth(List<T> items, Function<T, LocalDate> dateExtractor) {
        List<MonthGroup<T>> groups = new ArrayList<>();
        String currentLabel = null;

        for (T item : items) {
            LocalDate date = dateExtractor.apply(item);
            String label = date.format(MONTH_FMT);

            if (!label.equals(currentLabel)) {
                groups.add(startMonth(label, date));
                currentLabel = label;
            }

            groups.getLast().items.add(item);
        }

        return groups;
    }

    private static <T> MonthGroup<T> startMonth(String label, LocalDate date) {
        MonthGroup<T> group = new MonthGroup<>();
        group.label = label;
        group.firstDay = date;
        group.monthClass = "month-" + date.getMonth()
                .name()
                .toLowerCase();
        group.items = new ArrayList<>();
        return group;
    }
}
