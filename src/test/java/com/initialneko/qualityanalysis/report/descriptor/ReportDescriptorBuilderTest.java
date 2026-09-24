package com.initialneko.qualityanalysis.report.descriptor;

import com.initialneko.qualityanalysis.report.annotation.CellType;
import com.initialneko.qualityanalysis.report.annotation.DerivedColumn;
import com.initialneko.qualityanalysis.report.annotation.Flatten;
import com.initialneko.qualityanalysis.report.annotation.ReportColumn;
import com.initialneko.qualityanalysis.report.annotation.ReportTable;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportDescriptorBuilderTest {

    public static class Inner {
        @ReportColumn(order = 1, header = "内嵌A", width = 8)
        public String a;
    }

    @ReportTable(title = "测试实体", sheet = "测试Sheet")
    public static class Fixture {
        @Flatten
        public Inner inner;

        @ReportColumn(order = 3, header = "名称", width = 10)
        public String name;

        @DerivedColumn(order = 2, header = "内嵌B", width = 12)
        public String innerB() { return inner == null ? null : "B-" + inner.a; }

        @DerivedColumn(order = 4, header = "派生数值", type = CellType.NUMBER)
        public Integer doubled() { return 42; }

        public Fixture(Inner inner, String name) {
            this.inner = inner;
            this.name = name;
        }
    }

    @Test
    public void scanResolvesOrderHeadersTypesAndExtractors() {
        ReportDescriptor descriptor = ReportDescriptorBuilder.scan(Fixture.class);
        assertEquals("测试实体", descriptor.getTitle());
        assertEquals("测试Sheet", descriptor.getSheetName());
        assertEquals(4, descriptor.getColumns().size());

        assertEquals("内嵌A", descriptor.getColumns().get(0).getHeader());
        assertEquals("内嵌B", descriptor.getColumns().get(1).getHeader());
        assertEquals("名称", descriptor.getColumns().get(2).getHeader());
        assertEquals("派生数值", descriptor.getColumns().get(3).getHeader());

        Fixture fixture = new Fixture(new Inner(), "n1");
        fixture.inner.a = "A1";
        assertEquals("A1", descriptor.getColumns().get(0).extract(fixture));
        assertEquals("B-A1", descriptor.getColumns().get(1).extract(fixture));
        assertEquals("n1", descriptor.getColumns().get(2).extract(fixture));
        assertEquals(Integer.valueOf(42), descriptor.getColumns().get(3).extract(fixture));

        assertEquals(CellType.TEXT, descriptor.getColumns().get(0).getType());
        assertEquals(CellType.NUMBER, descriptor.getColumns().get(3).getType());
        assertEquals(8, descriptor.getColumns().get(0).getWidth());
    }

    @Test
    public void flattenExtractionToleratesNullNestedObject() {
        ReportDescriptor descriptor = ReportDescriptorBuilder.scan(Fixture.class);
        Fixture fixture = new Fixture(null, "n2");
        assertNull(descriptor.getColumns().get(0).extract(fixture));
        assertNull(descriptor.getColumns().get(1).extract(fixture));
        assertEquals("n2", descriptor.getColumns().get(2).extract(fixture));
    }
}
