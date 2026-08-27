# Sistema de Facturación Electrónica SIFEN — Tottal Store

Monorepo del sistema desarrollado para el Proyecto II (LCIK, Facultad Politécnica – UNA). Ver la
documentación de planificación y diseño en `../Proyecto1_SIFEN_Version_Final.docx-1.pdf` y
`../Proyecto2_Planificacion_SIFEN-1.pdf`.

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
> todavía. Cuando se disponga de esos accesos, se agregan nuevas implementaciones sin tocar el resto
> del sistema (RNF-09).

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

## Solución de problemas

**Maven falla al descargar dependencias con `PKIX path building failed`.** Ocurre si tu antivirus
(por ejemplo Kaspersky Endpoint Security) o el proxy de la red intercepta el tráfico HTTPS con un
certificado propio: Windows y el navegador confían en él, pero el almacén de confianza (`cacerts`)
del JDK es independiente y no lo conoce. Solución (no requiere permisos de administrador):

```powershell
# 1. Exportar el certificado raíz que intercepta el tráfico (ajustar host si hace falta)
$hostName = "repo.maven.apache.org"
$tcp = New-Object System.Net.Sockets.TcpClient($hostName, 443)
$sslStream = New-Object System.Net.Security.SslStream($tcp.GetStream(), $false, ({$true}))
$sslStream.AuthenticateAsClient($hostName)
$chain = New-Object System.Security.Cryptography.X509Certificates.X509Chain
$chain.Build($sslStream.RemoteCertificate) | Out-Null
$root = $chain.ChainElements[$chain.ChainElements.Count - 1].Certificate
[System.IO.File]::WriteAllBytes("$env:TEMP\root-ca.cer", $root.Export("Cert"))

# 2. Copiar el cacerts del JDK a una carpeta donde sí tengas permiso de escritura
$cacerts = "$env:USERPROFILE\.m2\jdk21-cacerts"
Copy-Item "C:\Program Files\Java\jdk-21\lib\security\cacerts" $cacerts

# 3. Importar el certificado a esa copia (password por defecto: changeit)
& "C:\Program Files\Java\jdk-21\bin\keytool.exe" -importcert -trustcacerts -noprompt `
  -alias root-ca -file "$env:TEMP\root-ca.cer" -keystore $cacerts -storepass changeit

# 4. Usar esa copia al invocar Maven
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStore=$cacerts -Djavax.net.ssl.trustStorePassword=changeit"
```

Repetir el paso 4 (`$env:MAVEN_OPTS = ...`) en cada terminal nueva, o agregarlo como variable de
entorno de usuario para que quede permanente.

## Convenciones

- Backend: paquetes organizados por feature (`auth`, `catalogo`, `clientes`, `facturacion`, `firma`,
  `sifen`, `auditoria`), no por capa técnica.
- Esquema de base de datos versionado con Flyway (`backend/src/main/resources/db/migration`); Hibernate
  corre en modo `validate`, nunca genera DDL automáticamente.
- El ambiente de comunicación con SIFEN (`TEST` / `PRODUCCION`) es configurable — Release 1 y 2 operan
  en `TEST` (homologación), Release 3 pasa a `PRODUCCION`.
