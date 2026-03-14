package com.hamradio.data;

import java.io.*;
import java.util.*;

public class SigMFMetadata {

    private String datatype = "cf32_le";
    private double sampleRate;
    private double frequency;
    private String description = "";
    private String author = "";
    private final List<Map<String, Object>> captures = new ArrayList<>();
    private final List<Map<String, Object>> annotations = new ArrayList<>();

    public SigMFMetadata(double sampleRate, double frequency) {
        this.sampleRate = sampleRate;
        this.frequency = frequency;
    }

    public void addCapture(long sampleStart, double frequency) {
        Map<String, Object> capture = new LinkedHashMap<>();
        capture.put("core:sample_start", sampleStart);
        capture.put("core:frequency", frequency);
        capture.put("core:datetime", new Date().toString());
        captures.add(capture);
    }

    public void addAnnotation(long sampleStart, long sampleCount, String label) {
        Map<String, Object> ann = new LinkedHashMap<>();
        ann.put("core:sample_start", sampleStart);
        ann.put("core:sample_count", sampleCount);
        ann.put("core:label", label);
        annotations.add(ann);
    }

    public void save(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"global\": {\n");
        sb.append("    \"core:datatype\": \"").append(datatype).append("\",\n");
        sb.append("    \"core:sample_rate\": ").append(sampleRate).append(",\n");
        sb.append("    \"core:version\": \"1.0.0\",\n");
        sb.append("    \"core:description\": \"").append(escape(description)).append("\",\n");
        sb.append("    \"core:author\": \"").append(escape(author)).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"captures\": ").append(toJsonArray(captures)).append(",\n");
        sb.append("  \"annotations\": ").append(toJsonArray(annotations)).append("\n");
        sb.append("}\n");

        try (Writer w = new FileWriter(filePath)) {
            w.write(sb.toString());
        }
    }

    private String toJsonArray(List<Map<String, Object>> list) {
        if (list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < list.size(); i++) {
            sb.append("    {");
            Map<String, Object> m = list.get(i);
            int j = 0;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                if (j > 0) sb.append(", ");
                sb.append("\"").append(e.getKey()).append("\": ");
                Object v = e.getValue();
                if (v instanceof String) sb.append("\"").append(escape(v.toString())).append("\"");
                else sb.append(v);
                j++;
            }
            sb.append("}");
            if (i < list.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]");
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public void setDatatype(String dt) { this.datatype = dt; }
    public void setDescription(String desc) { this.description = desc; }
    public void setAuthor(String author) { this.author = author; }
    public double getSampleRate() { return sampleRate; }
    public double getFrequency() { return frequency; }
}
