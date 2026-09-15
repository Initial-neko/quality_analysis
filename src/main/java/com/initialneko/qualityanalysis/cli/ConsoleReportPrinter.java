package com.initialneko.qualityanalysis.cli;

import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.PatternFrequency;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.model.ValueFrequency;

import java.io.PrintStream;

final class ConsoleReportPrinter {
    private ConsoleReportPrinter() {}

    static void print(TableProfile profile, PrintStream out) {
        out.println("Table: " + profile.getMetadata().getSchema() + "." + profile.getMetadata().getTable());
        out.println("Rows : " + profile.getRowCount());
        out.println();
        for (ColumnProfile c : profile.getColumns()) {
            out.println("[" + c.getMetadata().getName() + "] " + c.getMetadata().getDatabaseTypeName()
                    + (c.getMetadata().isDeclaredPrimaryKey() ? " PK" : ""));
            out.printf("  null=%d (%.4f%%), blank=%d, semanticNull=%d, distinct=%d, uniqueness=%.6f%n",
                    c.getNullCount(), c.getNullRate() * 100.0d, c.getBlankCount(), c.getSemanticNullCount(),
                    c.getDistinctCount(), c.getUniqueness());
            if (c.getMinValue() != null || c.getMaxValue() != null) out.println("  range=" + c.getMinValue() + " .. " + c.getMaxValue());
            if (c.getMinLength() != null) out.printf("  length=%d .. %d, avg=%.2f%n", c.getMinLength(), c.getMaxLength(), c.getAvgLength());
            if (c.isLobContentSkipped()) out.println("  LOB content skipped (length only; optional CLOB preview does not affect distinct)");
            if (c.isCandidatePrimaryKey()) out.println("  candidate-key=true");
            if (c.isPotentialEnum()) out.println("  potential-enum=true");
            if (c.isConstant()) out.println("  constant=true");
            if (c.isQuasiConstant()) out.println("  quasi-constant=true");
            if (c.getTrimChangedCount() > 0 || c.getCaseVariantGroupCount() > 0) {
                out.println("  normalization: trimChanged=" + c.getTrimChangedCount() + ", caseVariantGroups=" + c.getCaseVariantGroupCount());
            }
            if (!c.getValues().isEmpty()) {
                out.println("  values:");
                for (ValueFrequency v : c.getValues()) out.printf("    %s -> %d (%.4f%%)%n", v.getValue(), v.getCount(), v.getRatio() * 100.0d);
            }
            if (!c.getTopPatterns().isEmpty()) {
                out.print("  patterns: ");
                int i = 0;
                for (PatternFrequency p : c.getTopPatterns()) {
                    if (i++ > 0) out.print(", ");
                    out.print(p.getPattern() + "=" + p.getCount());
                }
                out.println();
            }
            out.println();
        }
    }
}
