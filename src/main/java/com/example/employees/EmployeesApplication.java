package com.example.employees;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
public class EmployeesApplication {

    public static void main(String[] args) {
        SpringApplication.run( EmployeesApplication.class, args);
    }

    @RestController
    public static class AnalyzeController {

        @PostMapping(value = "/api/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResultDto analyze(@RequestParam("file") MultipartFile file) throws Exception {
            List<Assignment> data = loadCsv( file);
            Result best = findBestPair( data);
            List<RowDto> rows = best.perProjectDays.entrySet().stream()
                    .sorted( Map.Entry.comparingByKey())
                    .map(e -> new RowDto( best.emp1, best.emp2, e.getKey(), e.getValue()))
                    .toList();
            return new ResultDto( best.emp1, best.emp2, best.totalDays, rows, "OK");
        }

        private static List<Assignment> loadCsv(MultipartFile file) throws Exception {
            FlexibleDateParser parser = new FlexibleDateParser();
            List<Assignment> out = new ArrayList<>();
            try (BufferedReader br = new BufferedReader( new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineNo = 0;
                while ((line = br.readLine()) != null) {
                    lineNo++;
                    String t = line.trim();
                    if (t.isEmpty() || (lineNo == 1 && t.toLowerCase().startsWith("empid"))) continue;
                    String[] parts = t.split("\\s*,\\s*");
                    if (parts.length != 4) {
                        throw new IllegalArgumentException("Invalid CSV at line " + lineNo + ": " + line);
                    }
                    int empId = Integer.parseInt(parts[0]), projectId = Integer.parseInt(parts[1]);
                    LocalDate from = parser.parse(parts[2]);
                    String dateToRaw = (parts[3]==null ? "" : parts[3].trim());
                    LocalDate to = (dateToRaw.isEmpty() || dateToRaw.equalsIgnoreCase("NULL")) ? LocalDate.now() : parser.parse(dateToRaw);
                    out.add( new Assignment(empId, projectId, from, to));
                }
            }
            return out;
        }

        private static Result findBestPair(List<Assignment> assignments) {
            if (assignments == null || assignments.isEmpty()) return null;
            Map<Integer, List<Assignment>> byProject = assignments.stream().collect( Collectors.groupingBy(a -> a.projectId));
            Map<PairEmployers, Map<Integer, Integer>> pairProjectDays = new HashMap<>();
            Map<PairEmployers, Integer> pairTotalDays = new HashMap<>();
            for( var entry : byProject.entrySet()) {
                int projectId = entry.getKey();
                List<Assignment> list = entry.getValue();
                for( int i=0; i<list.size(); i++) {
                    for( int j=i+1; j<list.size(); j++) {
                        Assignment a = list.get(i),  b = list.get(j);
                        if (a.empId == b.empId) continue;
                        int overlap = overlapDays( a.dateFrom, a.dateTo, b.dateFrom, b.dateTo);
                        PairEmployers pair = new PairEmployers( a.empId, b.empId);
                        pairProjectDays.computeIfAbsent( pair, k -> new HashMap<>()).merge( projectId, overlap, Integer::sum);
                        pairTotalDays.merge( pair, overlap, Integer::sum);
                    }
                }
            }
            var bestEntry = pairTotalDays.entrySet().stream()
                    .max( Comparator.comparingInt( Map.Entry::getValue))
                    .orElse(null);
            PairEmployers bestPair = bestEntry.getKey();
            int total = bestEntry.getValue();
            Map<Integer, Integer> perProject = pairProjectDays.getOrDefault( bestPair, Map.of());
            return new Result( bestPair.a, bestPair.b, total, perProject);
        }

        private static int overlapDays( LocalDate aFrom, LocalDate aTo, LocalDate bFrom, LocalDate bTo) {
            LocalDate start = (aFrom.isAfter(bFrom) ? aFrom : bFrom),  end = (aTo.isBefore(bTo) ? aTo : bTo);
            return (int)ChronoUnit.DAYS.between(start, end); //+ 1;
        }
    }

    static record Assignment( int empId, int projectId, LocalDate dateFrom, LocalDate dateTo) {}
    static final class PairEmployers {
        final int a;
        final int b;

        PairEmployers( int x, int y) {
            a = Math.min(x,y);   b = Math.max(x,y);
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PairEmployers other)) return false;
            return a == other.a && b == other.b;
        }
        @Override public int hashCode() { return Objects.hash(a, b); }
    }

    record Result(int emp1, int emp2, int totalDays, Map<Integer, Integer> perProjectDays) {}

    public record RowDto( int emp1, int emp2, int projectId, long daysWorked) {}
    public record ResultDto( Integer emp1, Integer emp2, long totalDays, List<RowDto> rows, String message) {}

    static final class FlexibleDateParser {
        private static final List<DateTimeFormatter> FORMATTERS = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,           // 2013-11-01
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),  // 2013/11/01
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),  // 01-11-2013
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),  // 01/11/2013
                DateTimeFormatter.ofPattern("MM/dd/yyyy")   // 11/01/2013
        );

        LocalDate parse(String s) {
            String t = s.trim();
            for( DateTimeFormatter f : FORMATTERS) {
                try { return LocalDate.parse(t, f); }  catch (DateTimeParseException ignored) { }
            }
            throw new IllegalArgumentException("Unsupported date format: " + s);
        }
    }
}
