package eapli.aisafe.reporting.infrastructure;

import eapli.aisafe.reporting.application.MonthlyReportGenerator;
import eapli.aisafe.reporting.dto.CompanyFlightStat;
import eapli.aisafe.reporting.dto.MonthlyEvolutionPoint;
import eapli.aisafe.reporting.dto.MonthlyReportData;
import eapli.aisafe.reporting.dto.RouteUsageStat;
import eapli.aisafe.reporting.dto.ViolationAreaStat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HtmlMonthlyReportGenerator implements MonthlyReportGenerator {

    private static final String CHARTJS_CDN =
            "https://cdn.jsdelivr.net/npm/chart.js@4.4.2/dist/chart.umd.min.js";

    private static final String[] PALETTE = {
        "#3b82f6","#10b981","#f59e0b","#ef4444","#8b5cf6",
        "#06b6d4","#f97316","#84cc16","#ec4899","#14b8a6"
    };

    @Override
    public String fileExtension() {
        return "html";
    }

    @Override
    public void generate(MonthlyReportData data, Path outputPath) throws IOException {
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.writeString(outputPath, buildHtml(data), StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // HTML builder
    // -------------------------------------------------------------------------

    private String buildHtml(MonthlyReportData d) {
        final var sb = new StringBuilder(32_768);
        final String generatedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        sb.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>AISafe — Monthly Statistics Report</title>
                    <script src=\"""").append(CHARTJS_CDN).append("\"></script>\n").append("""
                    <style>
                        :root {
                            --bg:      #0d1117;
                            --surface: #161b22;
                            --border:  #30363d;
                            --text:    #c9d1d9;
                            --accent:  #58a6ff;
                        }
                        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
                        body {
                            background: var(--bg);
                            color: var(--text);
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                            min-height: 100vh;
                        }
                        header {
                            background: var(--surface);
                            border-bottom: 1px solid var(--border);
                            padding: 1.5rem 2rem;
                        }
                        header h1 { font-size: 1.6rem; color: var(--accent); }
                        header p  { font-size: 0.9rem; color: #8b949e; margin-top: 0.25rem; }
                        main { padding: 1.5rem 2rem; }
                        .kpi-bar {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
                            gap: 1rem;
                            margin-bottom: 1.5rem;
                        }
                        .kpi-card {
                            background: var(--surface);
                            border: 1px solid var(--border);
                            border-radius: 8px;
                            padding: 1rem 1.25rem;
                        }
                        .kpi-card .value {
                            font-size: 2rem;
                            font-weight: 700;
                            line-height: 1;
                        }
                        .kpi-card .label {
                            font-size: 0.8rem;
                            color: #8b949e;
                            margin-top: 0.25rem;
                        }
                        .kpi-green  .value { color: #10b981; }
                        .kpi-red    .value { color: #ef4444; }
                        .kpi-blue   .value { color: #3b82f6; }
                        .kpi-yellow .value { color: #f59e0b; }
                        .kpi-purple .value { color: #8b5cf6; }
                        .charts-grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(480px, 1fr));
                            gap: 1.5rem;
                        }
                        .chart-card {
                            background: var(--surface);
                            border: 1px solid var(--border);
                            border-radius: 8px;
                            padding: 1.25rem;
                        }
                        .chart-card h2 {
                            font-size: 0.95rem;
                            color: var(--accent);
                            margin-bottom: 1rem;
                            border-bottom: 1px solid var(--border);
                            padding-bottom: 0.5rem;
                        }
                        footer {
                            background: var(--surface);
                            border-top: 1px solid var(--border);
                            padding: 1rem 2rem;
                            font-size: 0.8rem;
                            color: #8b949e;
                            margin-top: 2rem;
                            text-align: center;
                        }
                    </style>
                </head>
                <body>
                """);

        // ---- header ----
        sb.append("<header>\n");
        sb.append("    <h1>AISafe — Monthly Statistics Report</h1>\n");
        sb.append("    <p>Period: <strong>").append(esc(d.period().label())).append("</strong></p>\n");
        sb.append("    <p>Generated: ").append(esc(generatedAt)).append("</p>\n");
        sb.append("</header>\n<main>\n");

        // ---- KPI bar ----
        sb.append("<div class=\"kpi-bar\">\n");
        kpiCard(sb, "kpi-green",  d.totalFlights(),       "Total Flights");
        kpiCard(sb, "kpi-red",    d.totalViolations(),    "Safety Violations");
        kpiCard(sb, "kpi-blue",   d.totalSimulations(),   "Simulations Run");
        kpiCard(sb, "kpi-yellow", d.topRoutes().size(),   "Routes Active");
        kpiCard(sb, "kpi-purple", d.topCompanies().size(),"Companies");
        sb.append("</div>\n");

        // ---- charts grid ----
        sb.append("<div class=\"charts-grid\">\n");
        chartRoutes(sb, d.topRoutes());
        chartCompanies(sb, d.topCompanies());
        chartViolations(sb, d.violationsByArea());
        chartEvolution(sb, d.evolution());
        sb.append("</div>\n");

        sb.append("</main>\n");
        sb.append("<footer>Generated by AISafe &mdash; Flight Control Operator Portal</footer>\n");
        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // KPI helper
    // -------------------------------------------------------------------------

    private static void kpiCard(StringBuilder sb, String cssClass, int value, String label) {
        sb.append("    <div class=\"kpi-card ").append(cssClass).append("\">\n");
        sb.append("        <div class=\"value\">").append(value).append("</div>\n");
        sb.append("        <div class=\"label\">").append(esc(label)).append("</div>\n");
        sb.append("    </div>\n");
    }

    // -------------------------------------------------------------------------
    // Chart 1 — Most-Used Routes (horizontal bar)
    // -------------------------------------------------------------------------

    private void chartRoutes(StringBuilder sb, List<RouteUsageStat> routes) {
        sb.append("<div class=\"chart-card\">\n");
        sb.append("    <h2>Most-Used Routes</h2>\n");
        sb.append("    <canvas id=\"chartRoutes\"></canvas>\n");
        sb.append("    <script>\n(function(){\n");

        sb.append("const labels=[");
        for (int i = 0; i < routes.size(); i++) {
            final RouteUsageStat r = routes.get(i);
            if (i > 0) sb.append(",");
            sb.append("\"").append(jsStr(r.routeName() + " (" + r.companyName() + ")")).append("\"");
        }
        sb.append("];\n");

        sb.append("const data=[");
        for (int i = 0; i < routes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(routes.get(i).flightCount());
        }
        sb.append("];\n");

        sb.append("const colors=[");
        for (int i = 0; i < routes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(PALETTE[i % PALETTE.length]).append("\"");
        }
        sb.append("];\n");

        sb.append("""
                new Chart(document.getElementById('chartRoutes'),{
                    type:'bar',
                    data:{labels,datasets:[{label:'Flights',data,backgroundColor:colors,borderRadius:6}]},
                    options:{
                        indexAxis:'y',
                        plugins:{legend:{display:false}},
                        scales:{
                            x:{ticks:{color:'#8b949e'},grid:{color:'#30363d'}},
                            y:{ticks:{color:'#8b949e'},grid:{color:'#30363d'}}
                        }
                    }
                });
                })();
                </script>
                </div>
                """);
    }

    // -------------------------------------------------------------------------
    // Chart 2 — Companies with Most Flights (doughnut)
    // -------------------------------------------------------------------------

    private void chartCompanies(StringBuilder sb, List<CompanyFlightStat> companies) {
        sb.append("<div class=\"chart-card\">\n");
        sb.append("    <h2>Companies with Most Flights</h2>\n");
        sb.append("    <canvas id=\"chartCompanies\"></canvas>\n");
        sb.append("    <script>\n(function(){\n");

        sb.append("const labels=[");
        for (int i = 0; i < companies.size(); i++) {
            final CompanyFlightStat c = companies.get(i);
            if (i > 0) sb.append(",");
            sb.append("\"").append(jsStr(c.companyName() + " (" + c.companyIata() + ")")).append("\"");
        }
        sb.append("];\n");

        sb.append("const data=[");
        for (int i = 0; i < companies.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(companies.get(i).flightCount());
        }
        sb.append("];\n");

        sb.append("const colors=[");
        for (int i = 0; i < companies.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(PALETTE[i % PALETTE.length]).append("\"");
        }
        sb.append("];\n");

        sb.append("""
                new Chart(document.getElementById('chartCompanies'),{
                    type:'doughnut',
                    data:{labels,datasets:[{data,backgroundColor:colors}]},
                    options:{
                        cutout:'55%',
                        plugins:{legend:{position:'right',labels:{color:'#c9d1d9'}}}
                    }
                });
                })();
                </script>
                </div>
                """);
    }

    // -------------------------------------------------------------------------
    // Chart 3 — Safety Violations by Area (vertical bar)
    // -------------------------------------------------------------------------

    private void chartViolations(StringBuilder sb, List<ViolationAreaStat> areas) {
        sb.append("<div class=\"chart-card\">\n");
        sb.append("    <h2>Safety Violations by Area</h2>\n");
        sb.append("    <canvas id=\"chartViolations\"></canvas>\n");
        sb.append("    <script>\n(function(){\n");

        sb.append("const labels=[");
        for (int i = 0; i < areas.size(); i++) {
            final ViolationAreaStat a = areas.get(i);
            if (i > 0) sb.append(",");
            sb.append("\"").append(jsStr(a.areaName() + " (" + a.areaCode() + ")")).append("\"");
        }
        sb.append("];\n");

        sb.append("const data=[");
        for (int i = 0; i < areas.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(areas.get(i).violationCount());
        }
        sb.append("];\n");

        sb.append("""
                new Chart(document.getElementById('chartViolations'),{
                    type:'bar',
                    data:{labels,datasets:[{
                        label:'Violations',data,
                        backgroundColor:'#ef444499',borderColor:'#ef4444',
                        borderWidth:1,borderRadius:6
                    }]},
                    options:{
                        plugins:{legend:{display:false}},
                        scales:{
                            x:{ticks:{color:'#8b949e'},grid:{color:'#30363d'}},
                            y:{beginAtZero:true,ticks:{color:'#8b949e'},grid:{color:'#30363d'}}
                        }
                    }
                });
                })();
                </script>
                </div>
                """);
    }

    // -------------------------------------------------------------------------
    // Chart 4 — Monthly Evolution (dual-axis line)
    // -------------------------------------------------------------------------

    private void chartEvolution(StringBuilder sb, List<MonthlyEvolutionPoint> evolution) {
        sb.append("<div class=\"chart-card\">\n");
        sb.append("    <h2>Monthly Evolution (Last 6 Months)</h2>\n");
        sb.append("    <canvas id=\"chartEvolution\"></canvas>\n");
        sb.append("    <script>\n(function(){\n");

        sb.append("const labels=[");
        for (int i = 0; i < evolution.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(jsStr(evolution.get(i).label())).append("\"");
        }
        sb.append("];\n");

        sb.append("const flights=[");
        for (int i = 0; i < evolution.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(evolution.get(i).totalFlights());
        }
        sb.append("];\n");

        sb.append("const violations=[");
        for (int i = 0; i < evolution.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(evolution.get(i).totalViolations());
        }
        sb.append("];\n");

        sb.append("""
                new Chart(document.getElementById('chartEvolution'),{
                    type:'line',
                    data:{
                        labels,
                        datasets:[
                            {label:'Flights',data:flights,borderColor:'#3b82f6',
                             backgroundColor:'#3b82f622',fill:true,tension:0.4,yAxisID:'yFlights'},
                            {label:'Violations',data:violations,borderColor:'#ef4444',
                             fill:false,borderDash:[5,5],tension:0.4,yAxisID:'yViolations'}
                        ]
                    },
                    options:{
                        interaction:{mode:'index',intersect:false},
                        plugins:{legend:{labels:{color:'#c9d1d9'}}},
                        scales:{
                            x:{ticks:{color:'#8b949e'},grid:{color:'#30363d'}},
                            yFlights:{type:'linear',position:'left',
                                ticks:{color:'#3b82f6'},grid:{color:'#30363d'},
                                title:{display:true,text:'Flights',color:'#3b82f6'}},
                            yViolations:{type:'linear',position:'right',
                                ticks:{color:'#ef4444'},grid:{drawOnChartArea:false},
                                title:{display:true,text:'Violations',color:'#ef4444'}}
                        }
                    }
                });
                })();
                </script>
                </div>
                """);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String jsStr(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
