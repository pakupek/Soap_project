package client;

import java.io.*;
import java.net.*;
import java.nio.file.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PdfGenerator {

    private static final String PDF_SERVICE_URL = "http://localhost:5000/generate-pdf";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static File generate(String invoiceId, String clientName, String device,
                                String actions, double laborCost, double partsCost) throws Exception {
        // Budujemy JSON
        ObjectNode body = mapper.createObjectNode();
        body.put("invoiceId", invoiceId);
        body.put("clientName", clientName);
        body.put("device", device);
        body.put("actions", actions);
        body.put("laborCost", laborCost);
        body.put("partsCost", partsCost);

        byte[] jsonBytes = mapper.writeValueAsBytes(body);

        // Wysyłamy HTTP POST
        URL url = new URL(PDF_SERVICE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBytes);
        }

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("PDF service zwrócił błąd: " + conn.getResponseCode());
        }

        byte[] pdfBytes;
        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
            pdfBytes = baos.toByteArray();
        }

        File tempFile = File.createTempFile("faktura_" + invoiceId + "_", ".pdf");
        Files.write(tempFile.toPath(), pdfBytes);
        return tempFile;
    }
}