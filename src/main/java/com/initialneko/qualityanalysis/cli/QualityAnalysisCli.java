package com.initialneko.qualityanalysis.cli;

import com.initialneko.qualityanalysis.QualityAnalyzer;
import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.TableProfile;

import java.sql.Connection;
import java.sql.DriverManager;

public final class QualityAnalysisCli {
    private QualityAnalysisCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.err.println("Usage: java -cp <quality-jar;driver-jar> com.initialneko.qualityanalysis.cli.QualityAnalysisCli <driver-class> <jdbc-url> <user> <password> <schema> <table> [fetchSize]");
            System.exit(2);
        }
        String driverClass = args[0];
        String url = args[1];
        String user = args[2];
        String password = args[3];
        String schema = args[4];
        String table = args[5];
        int fetchSize = args.length >= 7 ? Integer.parseInt(args[6]) : 10_000;
        Class.forName(driverClass);
        ProfileOptions options = ProfileOptions.builder().fetchSize(fetchSize).build();
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, user, password);
            TableProfile profile = new QualityAnalyzer().profileTable(connection, schema, table, options);
            ConsoleReportPrinter.print(profile, System.out);
        } finally {
            if (connection != null) connection.close();
        }
    }
}
