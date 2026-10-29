package com.arturmaximo.etiquetas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Test;

public class DocxGeneratorTest {
    @Test
    public void replacesAllLabelsAndKeepsOtherParts() throws Exception {
        byte[] template = makeTemplate();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        DocxGenerator.write(new ByteArrayInputStream(template), output, "Páprica");

        String xml = zipEntry(output.toByteArray(), "word/document.xml");
        assertEquals(120, occurrences(xml, "Páprica"));
        assertFalse(xml.contains("MINEIRO"));
        assertEquals("keep this part", zipEntry(output.toByteArray(), "word/styles.xml"));
    }

    @Test
    public void generatesFromTheRealPrintTemplate() throws Exception {
        String templatePath = System.getProperty("label.template.path");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream template = new FileInputStream(templatePath)) {
            DocxGenerator.write(template, output, "Manjericão");
        }

        assertEquals(120, occurrences(
                zipEntry(output.toByteArray(), "word/document.xml"), "Manjericão"));
        try (ZipFile original = new ZipFile(templatePath)) {
            java.util.Enumeration<? extends ZipEntry> entries = original.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!"word/document.xml".equals(entry.getName())) {
                    try (InputStream part = original.getInputStream(entry)) {
                        assertArrayEquals(part.readAllBytes(),
                                zipEntryBytes(output.toByteArray(), entry.getName()));
                    }
                }
            }
        }
    }

    @Test
    public void escapesXmlText() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DocxGenerator.write(new ByteArrayInputStream(makeTemplate()), output, "Sal & Pimenta <fina>");

        String xml = zipEntry(output.toByteArray(), "word/document.xml");
        assertEquals(120, occurrences(xml, "Sal &amp; Pimenta &lt;fina&gt;"));
    }

    @Test
    public void rejectsTemplateWithWrongLabelCount() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            DocxGenerator.write(
                    new ByteArrayInputStream(makeTemplate(119)), output, "Alecrim");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("120"));
            return;
        }
        throw new AssertionError("Expected an invalid template to be rejected.");
    }

    private byte[] makeTemplate() throws IOException {
        return makeTemplate(120);
    }

    private byte[] makeTemplate(int labelCount) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            StringBuilder xml = new StringBuilder("<root>");
            for (int i = 0; i < labelCount; i++) {
                xml.append("<w:t>MINEIRO</w:t>");
            }
            xml.append("</root>");
            zip.write(xml.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/styles.xml"));
            zip.write("keep this part".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private String zipEntry(byte[] archive, String name) throws IOException {
        return new String(zipEntryBytes(archive, name), StandardCharsets.UTF_8);
    }

    private byte[] zipEntryBytes(byte[] archive, String name) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (name.equals(entry.getName())) {
                    ByteArrayOutputStream contents = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = zip.read(buffer)) != -1) {
                        contents.write(buffer, 0, count);
                    }
                    return contents.toByteArray();
                }
            }
        }
        throw new IOException("Missing ZIP entry: " + name);
    }

    private int occurrences(String text, String value) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(value, offset)) >= 0) {
            count++;
            offset += value.length();
        }
        return count;
    }
}
