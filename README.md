# Sistema de Facturación Electrónica SIFEN — Tottal Store

Repo del sistema desarrollado para el Proyecto II (LCIK, Facultad Politécnica – UNA).

## Estado del desarrollo

Seguimos el plan de releases definido en la planificación (3 releases de 3 semanas). Estado actual:

- **Release 1 — Núcleo de facturación manual (Semanas 1–3, Homologación)**
  - [x] Infraestructura y base de datos (Postgres + Flyway)
  - [x] Autenticación y control de acceso por roles (JWT)
  - [x] M5 — Catálogo de productos y gestión de clientes (validación de RUC)
  - [x] M1 — Modelo de factura manual, cálculo de IVA y borrador (backend)
  - [ ] M1 — Formulario de factura manual (frontend)
  - [ ] Firma digital X.509 real y envío SIFEN real (por ahora **stubs**, ver abajo)

> Las integraciones con la SET/SIFEN (validación de RUC, firma digital XAdES, envío del DTE) están
> implementadas como **stubs** detrás de interfaces (`ValidadorRuc`, `FirmaDigitalService`,
> `EnviadorSifenService`) para poder avanzar sin certificado ni acceso al ambiente de homologación
> todavía. (RNF-09).

## Estructura

```
sifen-de-integration/
├── backend/     # Spring Boot 3 (Java 21) — API REST
├── frontend/    # React + TypeScript + Vite — panel de control
└── docker-compose.yml   # Postgres 16 para desarrollo local
```

## Cómo levantar el entorno de desarrollo

### 1. Base de datos

```bash
docker compose up -d
```

Levanta Postgres 16 en `localhost:5432` (db `sifen_db`, usuario `sifen`, password `sifen`).

### 2. Backend

```bash
cd backend
mvn spring-boot:run
```

Flyway aplica las migraciones automáticamente al iniciar. La API queda disponible en
`http://localhost:8080`. Al primer arranque se crea un usuario administrador de prueba:

- **Usuario:** `admin@tottalstore.com`
- **Contraseña:** `admin123`

Para correr los tests unitarios:

```bash
mvn test
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

SPA disponible en `http://localhost:5173`, apuntando por defecto a `http://localhost:8080/api`
(configurable en `frontend/.env`, ver `.env.example`).

## Convenciones

- Backend: paquetes organizados por feature (`auth`, `catalogo`, `clientes`, `facturacion`, `firma`,
  `sifen`, `auditoria`), no por capa técnica.
- Esquema de base de datos versionado con Flyway (`backend/src/main/resources/db/migration`); Hibernate
  corre en modo `validate`, nunca genera DDL automáticamente.
- El ambiente de comunicación con SIFEN (`TEST` / `PRODUCCION`) es configurable — Release 1 y 2 operan
  en `TEST` (homologación), Release 3 pasa a `PRODUCCION`.
