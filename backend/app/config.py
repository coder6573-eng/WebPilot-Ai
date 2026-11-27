from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    ai_base_url: str = "https://api.groq.com/openai/v1"
    ai_api_key: str = ""
    ai_model: str = "llama-3.3-70b-versatile"
    max_agent_steps: int = 10
    allowed_origins: str = "*"
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

settings = Settings()
