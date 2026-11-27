import socket
import pytest
from app.safety import UnsafeUrl, validate_public_url

def test_rejects_non_http_scheme():
    with pytest.raises(UnsafeUrl):
        validate_public_url("file:///etc/passwd")

def test_rejects_localhost():
    with pytest.raises(UnsafeUrl):
        validate_public_url("http://localhost:8000")

def test_rejects_local_domain():
    with pytest.raises(UnsafeUrl):
        validate_public_url("http://printer.local")

def test_rejects_private_dns_result(monkeypatch):
    monkeypatch.setattr(socket, "getaddrinfo", lambda *a, **k: [
        (socket.AF_INET, socket.SOCK_STREAM, 6, "", ("192.168.1.10", 443))
    ])
    with pytest.raises(UnsafeUrl):
        validate_public_url("https://example.test")

def test_accepts_public_dns_result(monkeypatch):
    monkeypatch.setattr(socket, "getaddrinfo", lambda *a, **k: [
        (socket.AF_INET, socket.SOCK_STREAM, 6, "", ("93.184.216.34", 443))
    ])
    assert validate_public_url("https://example.test") == "https://example.test"
