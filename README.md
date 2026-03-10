# StockSubscriptionService – Quarkus / REST / Vue.js / Docker

Dieses Repository implementiert ein verteiltes System zur Aktienüberwachung basierend auf einer Microservice-Architektur.

Folgende Kernprozesse sind abgebildet:

- **Aktie abonnieren** 
- **Aktienkursabfrage mit Währungsumrechnung** 
- **Automatische Kursüberwachung** 

---

## Architektur & Services (Ports)

Das System besteht aus einem Orchestrator, mehreren Domänen-Services, einer Datenbank und einem Frontend.

- **Frontend (Vue.js):** `5173`
- **stock-subscription-service** (Orchestrator): `8080`
- **subscription-service** (Abo-Verwaltung): `8081`
- **alert-service** (Schwellenwert-Prüfung): `8082`
- **stock-service** (AlphaVantage-Wrapper): `8083`
- **currency-service** (USD → Zielwährung): `8084`
- **PostgreSQL Datenbank** (Docker-Container): `5432`

---

## Voraussetzungen

- min. Java 17
- Maven
- Node.js & npm (für das Frontend)
- Docker + Docker Compose
- **AlphaVantage API Key** (für Live-Kursdaten)
  - Kostenlos beziehbar über die Alpha-Vantage-Website.
  - Im Projektroot befindet sich eine Datei `.env.example`. Diese Datei kopieren `.env.example` zu `.env` und eigenen Key eintragen:
  `ALPHAVANTAGE_API_KEY=HIER_KEY_EINTRAGEN`. 

---

## Build & Start

Da die Docker-Container auf den kompilierten Java-Dateien basieren, muss der Code zunächst gebaut werden.

### 1) Backend bauen & starten
Öffnen Sie ein Terminal im Projektroot und führen Sie nacheinander aus:

```bash
# 1. Java-Code kompilieren (erstellt die target-Ordner)
mvn clean package

# 2. Docker-Container (Services + Postgres DB) bauen und starten
docker compose up --build
```

Hinweis zur Datenbank: Die PostgreSQL-Datenbank wird automatisch über Docker hochgefahren. Dank Hibernate ORM / Panache werden die benötigten Tabellen (`Subscription`, `Alert`) beim Start automatisch generiert.

### 2) Frontend starten

Ein neues Terminal öffnen, in den Frontend-Ordner wechseln und den Entwicklungsserver starten:

```bash
cd frontend
npm install
npm run dev
```

Das Dashboard ist anschließend unter `http://localhost:5173` erreichbar.


---
