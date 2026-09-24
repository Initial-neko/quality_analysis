package com.initialneko.qualityanalysis.report.descriptor;

import com.initialneko.qualityanalysis.report.annotation.CellType;
import com.initialneko.qualityanalysis.report.annotation.DerivedColumn;
import com.initialneko.qualityanalysis.report.annotation.Flatten;
import com.initialneko.qualityanalysis.report.annotation.ReportColumn;
import com.initialneko.qualityanalysis.report.annotation.ReportTable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Scans a reportable entity class and builds the {@link ReportDescriptor}.
 *
 * Recognized members (declared on the class or any superclass below Object):
 * <ul>
 *   <li>fields annotated with {@link ReportColumn}</li>
 *   <li>no-arg methods annotated with {@link ReportColumn} or {@link DerivedColumn}</li>
 *   <li>fields annotated with {@link Flatten} whose declared type is scanned one
 *       level deep; flattened columns join the outer table (optionally prefixed)</li>
 * </ul>
 * Columns are sorted by {@code order}, ties keep declaration order.
 */
public final class ReportDescriptorBuilder {
    private ReportDescriptorBuilder() {}

    public static ReportDescriptor scan(Class<?> type) {
        List<ColumnDescriptor> columns = new ArrayList<ColumnDescriptor>();
        collect(type, columns, new ArrayList<ColumnDescriptor.Accessor>(), true);
        Collections.sort(columns, new Comparator<ColumnDescriptor>() {
            @Override public int compare(ColumnDescriptor a, ColumnDescriptor b) {
                return Integer.compare(a.getOrder(), b.getOrder());
            }
        });
        ReportTable table = type.getAnnotation(ReportTable.class);
        String title = table == null ? "" : table.title();
        String sheetName = table == null || table.sheet().isEmpty() ? type.getSimpleName() : table.sheet();
        return new ReportDescriptor(type, title, sheetName, columns);
    }

    private static void collect(Class<?> type, List<ColumnDescriptor> out,
                                List<ColumnDescriptor.Accessor> prefix, boolean allowFlatten) {
        int declarationIndex = 0;
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                int sequence = declarationIndex++;
                if (allowFlatten && field.isAnnotationPresent(Flatten.class)) {
                    Flatten flatten = field.getAnnotation(Flatten.class);
                    List<ColumnDescriptor.Accessor> innerPrefix = new ArrayList<ColumnDescriptor.Accessor>(prefix);
                    innerPrefix.add(fieldAccessor(field));
                    List<ColumnDescriptor> inner = new ArrayList<ColumnDescriptor>();
                    collect(field.getType(), inner, innerPrefix, false);
                    for (ColumnDescriptor column : inner) {
                        out.add(new ColumnDescriptor(column.getOrder(),
                                flatten.prefix().isEmpty() ? column.getHeader() : flatten.prefix() + column.getHeader(),
                                column.getWidth(), column.getType(), column.getAccessor()));
                    }
                    continue;
                }
                ReportColumn column = field.getAnnotation(ReportColumn.class);
                if (column == null) continue;
                out.add(new ColumnDescriptor(orderOr(sequence, column.order()),
                        headerOr(column.header(), field.getName()), column.width(), column.type(),
                        chain(prefix, fieldAccessor(field))));
            }
            for (Method method : c.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers())) continue;
                if (method.getParameterTypes().length != 0) continue;
                if (method.getReturnType() == void.class) continue;
                int sequence = declarationIndex++;
                DerivedColumn derived = method.getAnnotation(DerivedColumn.class);
                ReportColumn column = method.getAnnotation(ReportColumn.class);
                if (derived == null && column == null) continue;
                int order = derived != null ? derived.order() : column.order();
                String header = derived != null ? derived.header() : column.header();
                int width = derived != null ? derived.width() : column.width();
                CellType cellType = derived != null ? derived.type() : column.type();
                out.add(new ColumnDescriptor(orderOr(sequence, order),
                        headerOr(header, method.getName()), width, cellType,
                        chain(prefix, methodAccessor(method))));
            }
        }
    }

    private static ColumnDescriptor.Accessor chain(final List<ColumnDescriptor.Accessor> prefix,
                                                   final ColumnDescriptor.Accessor last) {
        if (prefix.isEmpty()) return last;
        return new ColumnDescriptor.Accessor() {
            @Override public Object get(Object row) {
                Object current = row;
                for (ColumnDescriptor.Accessor step : prefix) {
                    if (current == null) return null;
                    current = step.get(current);
                }
                return current == null ? null : last.get(current);
            }
        };
    }

    private static ColumnDescriptor.Accessor fieldAccessor(final Field field) {
        field.setAccessible(true);
        return new ColumnDescriptor.Accessor() {
            @Override public Object get(Object row) {
                try {
                    return field.get(row);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("cannot read field " + field.getName(), e);
                }
            }
        };
    }

    private static ColumnDescriptor.Accessor methodAccessor(final Method method) {
        method.setAccessible(true);
        return new ColumnDescriptor.Accessor() {
            @Override public Object get(Object row) {
                try {
                    return method.invoke(row);
                } catch (Exception e) {
                    throw new IllegalStateException("cannot invoke method " + method.getName(), e);
                }
            }
        };
    }

    private static int orderOr(int declarationSequence, int order) {
        return order != 0 ? order : declarationSequence + 1;
    }

    private static String headerOr(String header, String fallback) {
        return header == null || header.isEmpty() ? fallback : header;
    }
}
