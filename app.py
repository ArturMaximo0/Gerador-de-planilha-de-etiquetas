import html
import io
import json
import re
import socket
import unicodedata
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import quote
from zipfile import ZIP_DEFLATED, ZipFile


ROOT = Path(__file__).resolve().parent
TEMPLATE_PATH = ROOT / "6.docx"
DOCUMENT_XML_PATH = "word/document.xml"
EXPECTED_LABELS = 120
MAX_REQUEST_SIZE = 8_192
TEXT_NODE = re.compile(rb"(<w:t(?:\s[^>]*)?>)MINEIRO(</w:t>)")


def create_document(text: str) -> bytes:
    """Replace each template label without changing the rest of the DOCX."""
    replacement = html.escape(text, quote=False).encode("utf-8")
    output = io.BytesIO()

    with ZipFile(TEMPLATE_PATH) as source:
        try:
            document_xml = source.read(DOCUMENT_XML_PATH)
        except KeyError as error:
            raise ValueError("O arquivo modelo não contém a tabela de etiquetas.") from error

        updated_xml, replacements = TEXT_NODE.subn(
            lambda match: match.group(1) + replacement + match.group(2),
            document_xml,
        )
        if replacements != EXPECTED_LABELS:
            raise ValueError(
                "O modelo precisa conter exatamente 120 etiquetas com o texto MINEIRO."
            )

        with ZipFile(output, "w", ZIP_DEFLATED) as result:
            for item in source.infolist():
                contents = updated_xml if item.filename == DOCUMENT_XML_PATH else source.read(item.filename)
                result.writestr(item, contents)

    return output.getvalue()


def safe_filename(text: str) -> str:
    normalized = unicodedata.normalize("NFKD", text)
    ascii_text = normalized.encode("ascii", "ignore").decode("ascii").lower()
    slug = re.sub(r"[^a-z0-9]+", "-", ascii_text).strip("-")[:40].strip("-")
    return f"etiquetas-{slug or 'personalizadas'}.docx"


def get_lan_ip() -> str | None:
    """Find the address the local network can use to reach this computer."""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as connection:
            connection.connect(("192.0.2.1", 80))
            return connection.getsockname()[0]
    except OSError:
        return None


class LabelsHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path not in ("/", "/index.html"):
            self.send_error(404)
            return

        try:
            page = (ROOT / "index.html").read_bytes()
        except OSError:
            self.send_error(500, "Não foi possível carregar a página do aplicativo.")
            return

        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(page)))
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(page)

    def do_POST(self):
        if self.path != "/gerar":
            self.send_error(404)
            return

        try:
            content_length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            self.send_json_error(400, "Requisição inválida.")
            return
        if content_length <= 0 or content_length > MAX_REQUEST_SIZE:
            self.send_json_error(413, "O texto enviado é inválido ou muito grande.")
            return

        try:
            payload = json.loads(self.rfile.read(content_length))
        except (json.JSONDecodeError, UnicodeDecodeError):
            self.send_json_error(400, "Não foi possível ler o texto enviado.")
            return

        text = payload.get("texto") if isinstance(payload, dict) else None
        if not isinstance(text, str) or not text.strip():
            self.send_json_error(400, "Digite um texto para criar as etiquetas.")
            return
        if len(text) > 500:
            self.send_json_error(400, "O texto deve ter no máximo 500 caracteres.")
            return

        try:
            document = create_document(text.strip())
        except (OSError, ValueError) as error:
            self.send_json_error(500, str(error))
            return

        filename = safe_filename(text.strip())
        self.send_response(200)
        self.send_header(
            "Content-Type",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        )
        self.send_header(
            "Content-Disposition",
            f"attachment; filename=\"{filename}\"; filename*=UTF-8''{quote(filename)}",
        )
        self.send_header("Content-Length", str(len(document)))
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(document)

    def send_json_error(self, status: int, message: str):
        response = json.dumps({"erro": message}, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(response)))
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(response)


if __name__ == "__main__":
    if not TEMPLATE_PATH.is_file():
        raise SystemExit(f"Arquivo modelo não encontrado: {TEMPLATE_PATH}")
    server = ThreadingHTTPServer(("0.0.0.0", 8000), LabelsHandler)
    print("Aplicativo disponível neste computador: http://127.0.0.1:8000")
    lan_ip = get_lan_ip()
    if lan_ip:
        print(f"Na mesma rede Wi-Fi, abra no celular: http://{lan_ip}:8000")
    else:
        print("Não consegui detectar o endereço da rede. Confira o IP deste computador.")
    print("Use somente em uma rede local confiável; não exponha a porta 8000 à internet.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nAplicativo encerrado.")
    finally:
        server.server_close()
