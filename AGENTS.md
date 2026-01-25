# Cursor Agents Konfiguration

## Gemini Agent

Verwende Gemini als primären Agent für dieses Projekt.

### Konfiguration

Um Gemini als Agent zu nutzen, hast du mehrere Optionen:

#### Option 1: Über Cursor Einstellungen (Empfohlen)

1. Öffne die Cursor Einstellungen (Strg+, oder Cmd+,)
2. Gehe zu "Features" → "AI" → "Model"
3. Wähle "Gemini" aus der Liste der verfügbaren Modelle
4. Falls nötig, füge deinen Google AI API Key hinzu

#### Option 2: Über MCP (Model Context Protocol)

1. Besuche [Google AI Studio](https://makersuite.google.com/app/apikey) und erstelle einen API Key
2. Füge die MCP-Konfiguration zu deinen Cursor Einstellungen hinzu:

```json
{
  "mcpServers": {
    "gemini": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "github:aliargun/mcp-server-gemini"],
      "env": {
        "GEMINI_API_KEY": "dein_api_key_hier"
      }
    }
  }
}
```

#### Option 3: Direkt in der Chat-Oberfläche

In Cursor kannst du auch direkt im Chat-Fenster das Modell wechseln:
- Klicke auf das Modell-Icon im Chat-Fenster
- Wähle "Gemini" aus der Liste

### Verfügbare Gemini Modelle

- gemini-2.5-pro
- gemini-2.5-flash
- gemini-2.5-flash-lite
- gemini-2.0-flash
- gemini-1.5-pro

### Hinweise

- Stelle sicher, dass du einen gültigen Google AI API Key hast
- Die API Keys können in den Cursor Einstellungen unter "Secrets" verwaltet werden
- Gemini unterstützt Vision, Embeddings und Token-Counting
