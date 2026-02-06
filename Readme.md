i18n-service

Microservice zur Verwaltung von Sprachen und Übersetzungs-Bundles (Sprachdateien) pro Projekt (projectKey).
Stellt REST-Endpunkte bereit, um Sprachen zu verwalten, Bundles hochzuladen/auszuliefern und Übersetzungen zur Laufzeit abzufragen (inkl. Fallback).

Tech-Stack

Java 21, Spring Boot

MariaDB

Flyway Migrations

OpenAPI/Swagger (springdoc)

Docker / Docker Compose

Start (Docker)
1) .env anlegen
   cp .env.example .env


Passe danach die Werte in .env an (Passwörter etc.).

2) Starten
   docker compose up --build

3) Service-URLs

API: http://localhost:${APP_PORT}

Swagger UI: http://localhost:${APP_PORT}/swagger-ui.html

Health: http://localhost:${APP_PORT}/actuator/health

Persistente Daten (Volumes)

MariaDB Daten: ./docker-data/mariadb

Bundle Storage: ./docker-data/i18n (im Container: /data/i18n)

Quickstart (Beispiel-Flow)
1) Projekt anlegen
   curl -X POST "http://localhost:${APP_PORT}/api/v1/projects" \
   -H "Content-Type: application/json" \
   -d '{"projectKey":"portal","displayName":"Portal"}'

2) Sprache anlegen
   curl -X POST "http://localhost:${APP_PORT}/api/v1/portal/languages" \
   -H "Content-Type: application/json" \
   -d '{"name":"Deutsch","languageCode":"de-DE"}'

3) Default/Fallback setzen (optional)

Zuerst aktuelle Settings holen (wegen expectedVersion):

curl "http://localhost:${APP_PORT}/api/v1/portal/language-settings"


Dann setzen:

curl -X PUT "http://localhost:${APP_PORT}/api/v1/portal/language-settings" \
-H "Content-Type: application/json" \
-d '{"defaultLanguageCode":"de-DE","fallbackLanguageCode":null,"expectedVersion":0}'

4) Bundle erstellen & hochladen (JSON)

Beispiel-Datei de-DE.json:

{
"A": "Anmelden",
"B": "Abbrechen"
}


Upload:

curl -X POST "http://localhost:${APP_PORT}/api/v1/portal/languages/de-DE/bundle" \
-F "file=@de-DE.json"

5) Übersetzungen abfragen

Keys (Bulk):

curl "http://localhost:${APP_PORT}/api/v1/portal/translations/de-DE?keys=A,B"

Wichtige Umgebungsvariablen

Die wichtigsten Werte kommen aus .env (siehe .env.example), typischerweise:

APP_PORT (z. B. 8080)

DB_PORT (z. B. 3306)

MARIADB_ROOT_PASSWORD

MARIADB_DATABASE

MARIADB_USER

MARIADB_PASSWORD

APP_BUNDLE_STORAGE_BASE_PATH (im Container z. B. /data/i18n)

Troubleshooting
DB startet nicht / Healthcheck failt
docker compose ps
docker compose logs mariadb

Service startet nicht / keine DB-Verbindung
docker compose logs i18n-service

Bundles werden nicht persistiert

Prüfe, ob ./docker-data/i18n auf dem Host erstellt wird

Prüfe das Compose-Volume-Mapping auf /data/i18n