from app.main import app

def test_core_api_routes_are_registered():
    paths = {route.path for route in app.routes}
    assert "/health" in paths
    assert "/api/sessions" in paths
    assert "/api/sessions/{sid}/tasks" in paths
    assert "/api/sessions/{sid}/tasks/stop" in paths
    assert "/api/sessions/{sid}/stream" in paths
