package com.initialneko.qualityanalysis.report;

import com.initialneko.qualityanalysis.report.descriptor.ReportDescriptor;
import com.initialneko.qualityanalysis.report.descriptor.ReportDescriptorBuilder;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Facade over the annotation-driven renderers. Scans each entity class once,
 * then renders HTML tables and Excel sheets from the same descriptor so the
 * two outputs can never drift apart.
 */
public final class ReportEngine {
    private static final ConcurrentMap<Class<?>, ReportDescriptor> DESCRIPTORS =
            new ConcurrentHashMap<Class<?>, ReportDescriptor>();

    private ReportEngine() {}

    public static ReportDescriptor descriptor(Class<?> entityType) {
        ReportDescriptor cached = DESCRIPTORS.get(entityType);
        if (cached != null) return cached;
        ReportDescriptor built = ReportDescriptorBuilder.scan(entityType);
        ReportDescriptor existing = DESCRIPTORS.putIfAbsent(entityType, built);
        return existing == null ? built : existing;
    }

    public static String htmlTable(List<?> rows, Class<?> entityType) {
        return HtmlRenderer.renderTable(rows, descriptor(entityType));
    }

    public static void excelSheet(org.apache.poi.ss.usermodel.Workbook workbook, String sheetName,
                                  List<?> rows, Class<?> entityType, ExcelRenderer.Styles styles) {
        ExcelRenderer.renderSheet(workbook, sheetName, rows, descriptor(entityType), styles);
    }
}
