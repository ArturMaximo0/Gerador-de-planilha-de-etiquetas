package com.arturmaximo.etiquetas;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DocxGenerator {
    private static final String DOCUMENT_XML = "word/document.xml";
    private static final int EXPECTED_LABELS = 120;
    private static final Pattern LABEL_PATTERN =
            Pattern.compile("(<w:t(?:\\s[^>]*)?>)MINEIRO(</w:t>)");

    private DocxGenerator() {}

    static void write(InputStream template, OutputStream destination, String text) throws IOException {
        byte[] replacement = escapeXml(text).getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream document = new ByteArrayOutputStream();
        int replacements = 0;
        boolean foundDocument = false;

        try (ZipInputStream input = new ZipInputStream(template);
             ZipOutputStream output = new ZipOutputStream(destination)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                ZipEntry outputEntry = new ZipEntry(entry.getName());
                if (entry.getTime() >= 0) {
                    outputEntry.setTime(entry.getTime());
                }
                output.putNextEntry(outputEntry);

                if (DOCUMENT_XML.equals(entry.getName())) {
                    foundDocument = true;
                    document.reset();
                    copy(input, document);
                    byte[] updated = replaceLabels(document.toByteArray(), replacement);
                    replacements = countLabels(document.toByteArray());
                    output.write(updated);
                } else {
                    copy(input, output);
                }

                output.closeEntry();
                input.closeEntry();
            }

            if (!foundDocument) {
                throw new IOException("O modelo não contém o documento Word esperado.");
            }
            if (replacements != EXPECTED_LABELS) {
                throw new IOException("O modelo precisa conter exatamente 120 etiquetas MINEIRO.");
            }
        }
    }

    private static byte[] replaceLabels(byte[] xml, byte[] replacement) {
        String source = new String(xml, StandardCharsets.UTF_8);
        Matcher matcher = LABEL_PATTERN.matcher(source);
        StringBuffer result = new StringBuffer(source.length());
        while (matcher.find()) {
            String newText = matcher.group(1)
                    + new String(replacement, StandardCharsets.UTF_8)
                    + matcher.group(2);
            matcher.appendReplacement(result, Matcher.quoteReplacement(newText));
        }
        matcher.appendTail(result);
        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static int countLabels(byte[] xml) {
        Matcher matcher = LABEL_PATTERN.matcher(new String(xml, StandardCharsets.UTF_8));
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int length;
        while ((length = input.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }
    }
}
