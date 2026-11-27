import ipaddress
import socket
from urllib.parse import urlparse

class UnsafeUrl(ValueError):
    pass

def validate_public_url(raw: str) -> str:
    parsed = urlparse(raw)
    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        raise UnsafeUrl("Only absolute HTTP(S) URLs are allowed.")
    host = parsed.hostname.lower()
    if host == "localhost" or host.endswith((".local", ".localhost")):
        raise UnsafeUrl("Local network destinations are blocked.")
    try:
        addresses = socket.getaddrinfo(host, parsed.port or (443 if parsed.scheme == "https" else 80))
    except socket.gaierror as exc:
        raise UnsafeUrl("Host could not be resolved.") from exc
    for item in addresses:
        ip = ipaddress.ip_address(item[4][0].split("%")[0])
        if not ip.is_global:
            raise UnsafeUrl("Private, local, link-local and reserved destinations are blocked.")
    return raw

SENSITIVE = ("submit", "send", "purchase", "buy", "checkout", "pay", "delete",
             "remove", "publish", "post", "transfer", "invite", "save changes",
             "confirm order", "change password")

def needs_approval(description: str) -> bool:
    value = description.lower()
    return any(word in value for word in SENSITIVE)
