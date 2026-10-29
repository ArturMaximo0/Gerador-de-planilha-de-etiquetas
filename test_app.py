import io
import unittest
from zipfile import ZipFile

from app import DOCUMENT_XML_PATH, EXPECTED_LABELS, create_document, safe_filename


class DocumentGenerationTests(unittest.TestCase):
    def test_replaces_all_labels_and_preserves_other_docx_parts(self):
        generated = create_document("Páprica")

        with ZipFile("6.docx") as original, ZipFile(io.BytesIO(generated)) as result:
            self.assertEqual(set(original.namelist()), set(result.namelist()))
            for name in original.namelist():
                if name != DOCUMENT_XML_PATH:
                    self.assertEqual(original.read(name), result.read(name), name)

            xml = result.read(DOCUMENT_XML_PATH)
            self.assertEqual(xml.count("Páprica".encode()), EXPECTED_LABELS)
            self.assertNotIn(b"MINEIRO", xml)

    def test_escapes_xml_characters(self):
        generated = create_document("Sal & Pimenta <fina>")
        with ZipFile(io.BytesIO(generated)) as result:
            xml = result.read(DOCUMENT_XML_PATH)
        self.assertEqual(xml.count(b"Sal &amp; Pimenta &lt;fina&gt;"), EXPECTED_LABELS)

    def test_filename_is_safe_for_download_headers(self):
        self.assertEqual(safe_filename("Páprica Doce"), "etiquetas-paprica-doce.docx")
        self.assertEqual(safe_filename("../../"), "etiquetas-personalizadas.docx")


if __name__ == "__main__":
    unittest.main()
