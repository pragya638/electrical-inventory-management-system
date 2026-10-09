package com.wonder.electricals.inventory.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CSV export formatting, including the quoting rules Excel needs. */
class CsvExporterTest {

    private final CsvExporter exporter = new CsvExporter();

    @Test
    @DisplayName("writes a header row followed by data rows")
    void writesHeaderAndRows() {
        String csv = exporter.build(
                List.of("Code", "Name"),
                List.of(List.of("CBL-001", "Copper Cable"), List.of("SWT-001", "Switch")));

        assertThat(csv).isEqualTo("\"Code\",\"Name\"\r\n"
                + "\"CBL-001\",\"Copper Cable\"\r\n"
                + "\"SWT-001\",\"Switch\"\r\n");
    }

    @Test
    @DisplayName("escapes embedded quotes by doubling them")
    void escapesEmbeddedQuotes() {
        String csv = exporter.build(List.of("Remarks"),
                List.of(List.of("He said \"in stock\"")));

        assertThat(csv).contains("\"He said \"\"in stock\"\"\"");
    }

    @Test
    @DisplayName("renders a null cell as an empty value rather than the text 'null'")
    void rendersNullAsEmpty() {
        String csv = exporter.build(List.of("Remarks"), List.of(java.util.Arrays.asList((String) null)));

        assertThat(csv).doesNotContain("null");
        assertThat(csv).isEqualTo("\"Remarks\"\r\n\r\n");
    }

    @Test
    @DisplayName("handles values that contain a comma without breaking the column count")
    void handlesEmbeddedCommas() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("SUP-001", "Demo Supplier, Kashipur"));

        String csv = exporter.build(List.of("Code", "Name"), rows);

        assertThat(csv.split("\r\n")[1]).isEqualTo("\"SUP-001\",\"Demo Supplier, Kashipur\"");
    }

    @Test
    @DisplayName("produces only the header when there are no data rows")
    void handlesEmptyData() {
        String csv = exporter.build(List.of("Code", "Name"), List.of());

        assertThat(csv).isEqualTo("\"Code\",\"Name\"\r\n");
    }
}