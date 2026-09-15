package com.initialneko.qualityanalysis.cli;

import com.initialneko.qualityanalysis.QualityAnalyzer;
import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.model.TableRef;
import com.initialneko.qualityanalysis.run.ProfileRunService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public final class QualityAnalysisCli {
    private QualityAnalysisCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            printUsage();
            System.exit(2);
        }

        String driverClass = args[0];
        String url = args[1];
        String user = args[2];
        String password = args[3];
        Class.forName(driverClass);
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, user, password);
            if (args.length >= 8) {
                runReportMode(connection, args);
            } else {
                runLegacyConsoleMode(connection, args);
            }
        } finally {
            if (connection != null) connection.close();
        }
    }

    private static void runReportMode(Connection connection, String[] args) throws Exception {
        String databaseLabel = args[4];
        String schema = args[5];
        String[] tableNames = args[6].split(",");
        Path outputRoot = Paths.get(args[7]);
        int fetchSize = args.length >= 9 ? Integer.parseInt(args[8]) : 10_000;
        List<TableRef> tables = new ArrayList<TableRef>();
        for (String name : tableNames) {
            String table = name.trim();
            if (table.length() > 0) tables.add(new TableRef(schema, table));
        }
        ProfileOptions options = ProfileOptions.builder().fetchSize(fetchSize).build();
        Path runDirectory = new ProfileRunService().run(connection, databaseLabel, tables, options, outputRoot);
        System.out.println("Profile run saved to: " + runDirectory.toAbsolutePath());
        System.out.println("Excel: " + runDirectory.resolve("quality-profile.xlsx").toAbsolutePath());
        System.out.println("HTML : " + runDirectory.resolve("quality-profile.html").toAbsolutePath());
    }

    private static void runLegacyConsoleMode(Connection connection, String[] args) throws Exception {
        String schema = args[4];
        String table = args[5];
        int fetchSize = args.length >= 7 ? Integer.parseInt(args[6]) : 10_000;
        ProfileOptions options = ProfileOptions.builder().fetchSize(fetchSize).build();
        TableProfile profile = new QualityAnalyzer().profileTable(connection, schema, table, options);
        ConsoleReportPrinter.print(profile, System.out);
    }

    private static void printUsage() {
        System.err.println("Report mode:");
        System.err.println("  <driver-class> <jdbc-url> <user> <password> <database-label> <schema> <table1,table2,...> <output-root> [fetchSize]");
        System.err.println("Legacy single-table console mode:");
        System.err.println("  <driver-class> <jdbc-url> <user> <password> <schema> <table> [fetchSize]");
    }
}
