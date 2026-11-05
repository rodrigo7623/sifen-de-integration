# Historial de pruebas y errores — SIFEN Manager

Registro único y cronológico de todo lo que se probó, lo que falló y cómo se corrigió, como insumo
para la documentación de implementación. Centraliza lo que antes estaba en
`SIFEN_Manager_Historial_Pruebas.docx` (Release 1 y 2) y todo lo detectado desde entonces.

**Reglas**

- Cada ejecución de pruebas relevante se agrega a la sección 1.
- Cada defecto se agrega en el momento en que se detecta y se actualiza cuando se corrige. Las
  entradas no se borran: si un defecto resulta no serlo, se marca como *Descartado* con el motivo.
- Se conservan los IDs ya usados en entregables anteriores (A, B, C). Los defectos nuevos usan
  `BUG-NNN` y los hallazgos operativos `H-NN`.

**Categorías**

- **Defectos de la aplicación** (sección 2): fallas del sistema que requieren un cambio en el
  código de producción.
- **Ajustes a la suite de pruebas** (sección 3): errores de las propias pruebas (Postman/Newman,
  JUnit). No son fallas del sistema, pero explican cómo quedó la suite.
- **Hallazgos operativos** (sección 4): problemas de entorno, infraestructura o configuración que
  no son defectos de código, pero bloquearon o condicionaron pruebas.

---

## 1. Ejecuciones de pruebas

| Fecha | Release | Tipo | Alcance / herramienta | Resultado |
|-------|---------|------|-----------------------|-----------|
| Semana 3 (≤ 10/09/2026) | R1 | Unitarias | JUnit 5 + Mockito (`mvn test`) | 18 / 18 OK |
| Semana 3 (≤ 10/09/2026) | R1 | Integración | Postman + Newman contra backend real y PostgreSQL 18 | 25 requests / 36 aserciones, 0 fallos |
| Semana 3 (≤ 10/09/2026) | R1 | Funcionales | Recorrido manual y con Playwright de las 6 pantallas | 6 / 6 OK |
| ≤ 19/10/2026 | R2 | Unitarias | JUnit 5 + Mockito (`mvn test`) | 48 / 48 OK |
| ≤ 19/10/2026 | R2 | Integración | Postman + Newman | 39 requests / 62 aserciones, 0 fallos |
| ≤ 19/10/2026 | R2 | Despliegue | curl contra Render + Vercel (login, CORS, health check) | OK |
| ≤ 19/10/2026 | R2 | Conectividad SIFEN real | `EnviadorSifenSoapService` contra `sifen-test` con certificado real | Falló por interceptación TLS (ver H-01) |
| 06/10/2026 | R3 | Unitarias | `mvn test` con JDK 21.0.12 (máquina local) | 48 / 48 OK |
| 06/10/2026 | R3 | Estática | Frontend `tsc --noEmit` | Sin errores |
| 06/10/2026 | R3 | Revisión de código | Revisión completa backend + frontend | 12 defectos nuevos (BUG-001 a BUG-012) |
| 08/10/2026 | R3 | Arranque local | `mvn spring-boot:run` contra Postgres local | Falló (ver H-03) |
| 27/10/2026 | R3 | Conectividad SIFEN | Handshake TLS a `sifen-test.set.gov.py:443` desde la máquina local | OK, TLS 1.2, certificado real de la SET emitido por DigiCert, sin interceptación (ver H-04) |
| 27/10/2026 | R3 | Unitarias | `mvn test` tras agregar la consulta de RUC al SIFEN (`ConsultaRucSifenServiceTest`) | 53 / 53 OK |
| 27/10/2026 | R3 | Arranque local | Backend en `SIFEN_ENVIO_MODO=real` contra PostgreSQL 18.6, certificado `CN=BARTOLOME RAMIREZ MEDINA` (CI 4309652) | OK. Flyway avisa que PostgreSQL 18 es más nuevo que la última versión soportada (16) |
| 27/10/2026 | R3 | Conectividad SIFEN real | `GET /api/sifen/consulta-ruc/4309652-2` → `siConsRUC` en `sifen-test` | **OK**: `0502 RUC encontrado`, estado ACT, 775 ms. Primera respuesta real del SIFEN con TLS mutuo (ver H-05) |
| 27/10/2026 | R3 | Envío SIFEN real | Factura de prueba `a74f7383-…` confirmada → `SiRecepDE` en `sifen-test` | SIFEN respondió `0140 Rechazado — Firma difiere del estándar. [El documento XML no tiene firma]` en 120 ms. Esperado: falta la Fase 4 (ver H-05) |
| 29/10/2026 | R3 | Unitarias | `mvn test` tras implementar la generación real del DE (XML v150 + firma XMLDSig + QR + numeración): `GeneradorDteSifenTest` (10), `FacturaServiceTest` (11) | 63 / 63 OK. La firma se verifica con la clave pública del certificado y el hash del QR coincide con el ejemplo del manual (13.8.4) |
| 29/10/2026 | R3 | Envío SIFEN real | Factura `4bd9897b-…`, intento 1 (DE real firmado) | `0160 No se informó el schema en el XML`. La firma ya no es rechazada (ver H-06) |
| 29/10/2026 | R3 | Envío SIFEN real | Intento 2: `xmlns` declarado primero en `<rDE>` | `0160 No se informó el schema en el XML` |
| 29/10/2026 | R3 | Envío SIFEN real | Intento 3: `xsi:schemaLocation="http://ekuatia.set.gov.py/sifen/xsd siRecepDE_v150.xsd"` | `0160 XML malformado [Elemento esperado: dBasExe dentro de: gCamIVA]`. Pasa la validación del schema |
| 29/10/2026 | R3 | Envío SIFEN real | Intento 4: se agrega `dBasExe` | **`1101 TEST - Número de timbrado inválido`** en 1,4 s. El DE pasa firma, schema y XSD; el SIFEN rechaza el timbrado ficticio `12345678` (ver H-06) |
| 29/10/2026 | R3 | Unitarias | `mvn test` tras agregar Libro IVA (M4), estadísticas de rechazo e historial por cliente: `LibroIvaServiceTest` (5: separación por tasa y totales, mes en hora de Asunción, fecha local en el borde UTC, Excel, PDF paginado) y `ReportesServiceTest` (3) | 71 / 71 OK |
| 29/10/2026 | R3 | Integración local | Migración `V7__intentos_envio_sifen` sobre la base local; `GET /api/reportes/libro-iva`, `/excel`, `/pdf`, `/rechazos` y `/clientes/{ruc}` | **OK**. V7 aplicada en 0,24 s; volcó los 3 envíos existentes a `intento_envio_sifen` (1 aprobado, 0140 y 1101). Libro de 10/2026: 1 comprobante, IVA 4.545. Un período inválido (`periodo=octubre`) devolvía 500 (ver BUG-012) |
| 29/10/2026 | R3 | Verificación de corrección | BUG-012: `periodo=octubre`, sin `periodo`, `/facturas/abc`, `?estado=FOO`, JSON roto y enum desconocido en `POST /facturas` | Todos devuelven **400** con mensaje legible (antes 500) |
| 29/10/2026 | R3 | Build frontend | `npm run build` con las pantallas Libro IVA, Rechazos SIFEN e historial del cliente | OK (`tsc -b` sin errores) |
| 29/10/2026 | R3 | Unitarias | `mvn test` tras rehacer el PDF como KuDE (BUG-013): `FacturaPdfGeneratorTest` (3: vista previa con número/timbrado/emisor, rechazada sin validez, 70 ítems + caracteres fuera de WinAnsi) y `GeneradorDteSifenTest` (+1: KuDE armado desde el XML firmado, con CDC agrupado y QR) | 74 / 74 OK. Primer intento: 1 falla, el QR no se encontraba porque `gCamFuFD` es hermano del `DE` y no hijo; corregido |
| 29/10/2026 | R3 | Envío SIFEN real | Factura `4bd9897b-…`, timbrado `4309652` (RUC sin DV, según instrucción de la SET para TEST) | `0160 XML malformado ['4309652' is not a valid value of union type 'tdNumTim']`: el timbrado debe tener 8 dígitos (ver H-09) |
| 29/10/2026 | R3 | Envío SIFEN real | Timbrado `04309652` (completado con cero a la izquierda) | **`1107 TEST - Fecha de inicio de vigencia del timbrado incorrecta`**. El timbrado ya es aceptado (desaparece el 1101) |
| 29/10/2026 | R3 | Envío SIFEN real | Fecha de inicio de vigencia `2026-10-29` (fecha de la aceptación) | `1107` otra vez. No se siguen probando fechas a ciegas: hay que tomar la fecha real de Marangatu |
| 29/10/2026 | R3 | Conectividad SIFEN real | `siConsRUC` para 4309652 | Un intento devolvió `HTTP 400 / 0160 XML Mal Formado` sin cambios en el código; 12 minutos después, el mismo pedido respondió `0502 RUC encontrado` (`dRUCFactElec` sigue en N). Fallo transitorio del ambiente de test |
| 29/10/2026 | R3 | Integración local | Descarga de PDF y XML de la factura `4bd9897b-…` con el XML real firmado | **OK**. Archivos `factura-001-001-0000001.pdf/.xml`; el KuDE muestra número, timbrado, vigencia, CDC en grupos de 4, URL de consulta de test, liquidación de IVA y el aviso "no aprobado". La razón social de test se cortaba en el encabezado; se partió en dos líneas |
| 02/11/2026 | R3 | Unitarias | `mvn test` tras M6 (notas de crédito/débito). `GeneradorDteSifenTest` (+1): XML de la NC con CDC `05…`, `iTiDE` 5, `gCamNCDE`, `gCamDEAsoc` con el CDC de la factura y sin `gCamFE`/`gCamCond`/`iTipTra`, más el KuDE y el PDF de la NC. `FacturaServiceTest` (+8): la NC toma cliente/punto de la factura; saldo acreditable (2417) sin contar NC rechazadas; la ND no tiene límite; no se emiten notas sobre facturas no aprobadas ni con tipo FE; la NC por el total anula la factura y usa su propia numeración; la parcial no anula; `PUT /facturas` no edita notas. `LibroIvaServiceTest` (+1): la NC resta y la factura anulada sigue listada | 84 / 84 OK |
| 02/11/2026 | R3 | Build frontend | `npm run build` con la pantalla de notas (`/notas/nueva`, `/notas/:id`), el filtro por tipo en el listado y la sección "Notas de crédito / débito" del detalle de la factura | OK (`tsc -b` sin errores) |
| 02/11/2026 | R3 | Integración local (stub) | Migración `V8__notas_credito_debito` aplicada desde cero (V1–V8) en un schema aislado `m6_prueba` de `sifen_db`, con `ddl-auto: validate`, y flujo completo por API: factura de 241.000 → NC parcial 110.000 → NC de 131.001 (excede) → ND 22.000 → NC por el saldo 131.000 → factura anulada → nueva factura → Libro IVA, dashboard y PDF de la NC | **19 / 19 OK**. Numeraciones independientes (FE 001-001-0000002, NC …0000002, ND …0000001); la factura pasa a ANULADO; Libro IVA del mes neto 27.000 en 5 comprobantes (la NC con montos negativos); dashboard 27.000; PDF `nota-credito-001-001-0000001.pdf`. En la primera corrida el mensaje del saldo mostraba `131000.00` (escala de la columna): se formatea sin decimales. El script esperaba 400 en las reglas de negocio; el sistema devuelve 409 `BUSINESS_RULE_VIOLATION`, que es la convención del proyecto: se corrigió el script. Al terminar se borró el schema de prueba |
| 02/11/2026 | R3 | Unitarias | `mvn test` tras la búsqueda de RUC para alta de clientes y carga de facturas: `BusquedaRucServiceTest` (6: calcula el DV si se ingresa solo el número y trae razón social/estado del SIFEN; un cliente ya registrado no consulta al SIFEN; RUC inexistente (0500) sugiere consumidor final; sin respuesta del SIFEN sigue con la validación local; en modo stub no hay consulta; DV incorrecto y formato inválido → 409) | 90 / 90 OK |
| 02/11/2026 | R3 | Build frontend | `npm run build` con la consulta al SIFEN en el alta de clientes y el buscador por RUC con alta en el momento en la carga de factura | OK |
| 02/11/2026 | R3 | Integración local (real) | Arranque en modo real sobre la base local: aplica `V8` en `public` (0,08 s; las 3 facturas existentes quedan como `FE`). `GET /api/clientes/consulta-ruc` con `4309652`, `80000519`, `80016096-7`, `1234` y `4309652-5` | Cliente registrado → lo devuelve sin consultar al SIFEN; DV calculado (`4309652-2`, `80000519-8`); `1234` → "el SIFEN no consulta RUC de menos de 5 dígitos"; DV incorrecto → 409. **El SIFEN de test respondió HTTP 302 a `/vdesk/hangup.php3` en todas las consultas** (ver H-11); el sistema siguió con la validación local, como estaba previsto |
| 02/11/2026 | R3 | Integración local (real) | Reintento ~20:30 de `siConsRUC` (H-11): 15 consultas por `GET /api/clientes/consulta-ruc` y `GET /api/sifen/consulta-ruc` con `80000519`, `80016096`, `1234567`, `80000000`, `3456789` y `4309652-2` | **El 302 desapareció.** Respuestas reales: `80000519-8` → TELECEL SAE, facturador; `80016096-7` → RETAIL S.A., facturador; `1234567-9` → BRIGIDA GAUTO JARA; `80000000`/`3456789` → `0500` (sugiere consumidor final); `4309652-2` → BARTOLOME. Aprox. la mitad de los pedidos devolvió `HTTP 400 / 0160 XML Mal Formado` de forma intermitente: el mismo pedido, repetido sin cambios, alterna entre error y `0502` (ver H-11) |
| 02/11/2026 | R3 | Envío SIFEN real | Fecha de inicio del timbrado `2026-09-01` (dato real de Marangatu, cargado por `PUT /api/configuracion/emisor`); factura `4bd9897b-…` reabierta y reenviada (nuevo CDC `01043096522001001000000112026110217174845331`) | **Desaparece el 1107.** Nuevo rechazo: `1261 TEST - Código de actividad económica incorrecto`: `cActEco` (D131) `47521` era un valor provisorio y no coincide con la actividad declarada en el RUC (ver H-09) |
| 02/11/2026 | R3 | Envío SIFEN real | `cActEco` `47890` (dato del contribuyente) con una descripción tentativa ("COMERCIO AL POR MENOR DE OTROS PRODUCTOS EN PUESTOS DE VENTA Y MERCADOS"); factura `4bd9897b-…` reenviada (CDC `01043096522001001000000112026110218871615045`) | **Desaparece el 1261: el código es correcto.** Nuevo rechazo: `1262 TEST - Descripción de la actividad económica no corresponde al código`. El SIFEN valida también `dDesActEco` (D132) contra su catálogo: hace falta el texto exacto de Marangatu |
| 02/11/2026 | R3 | Envío SIFEN real | `dDesActEco` exacta de Marangatu: "COMERCIO AL POR MENOR DE OTROS ARTÍCULOS EN PUESTOS DE VENTA Y MERCADOS"; factura `4bd9897b-…` reenviada | **`0260 Aprobado — Autorización del DE satisfactoria`**. Primera factura electrónica aprobada por el SIFEN de test. CDC `01043096522001001000000112026110218908797044` |
| 02/11/2026 | R3 | Envío SIFEN real (M6) | Nota de crédito parcial (motivo Devolución, Gs. 5.500 de 11.000) sobre la factura aprobada: `POST /api/notas` + `confirmar` | **`0260 Aprobado` al primer intento.** CDC `05043096522001001000000112026110216858853890` (prefijo 05). La factura sigue `APROBADO` (saldo acreditable 5.500). KuDE de la NC verificado: título, motivo, documento asociado con el CDC de la factura, totales e IVA 500 |
| 02/11/2026 | R3 | Unitarias | `mvn test` con RNF-03: `CifradoSecretosTest` (8: ida y vuelta, IV distinto por cifrado, valor sin `ENC` pasa tal cual, otra clave maestra no descifra, valor alterado no descifra, clave de largo incorrecto, el post-processor publica el secreto descifrado y falla si falta la clave maestra) | 98 / 98 OK (tras corregir B5) |
| 02/11/2026 | R3 | Integración local (real) | Arranque en modo real con `SIFEN_FIRMA_KEYSTORE_PASSWORD=ENC(...)` y la clave maestra en la variable de entorno de usuario `SIFEN_MASTER_KEY` | 1.er intento: `keystore password was incorrect` (ver H-12). Corregido: "1 secreto descifrado en memoria", certificado cargado y `siConsRUC` → `0502` con TLS mutuo (6 de 6) |
| 02/11/2026 | R3 | Integración | Postman + Newman, colección ampliada con la carpeta *Release 3* (24 requests: M6 notas de crédito/débito, M4 Libro IVA y reportes, consulta de RUC), backend en modo stub sobre el esquema aislado `newman_r3` (V1–V8 desde cero) | 1.ª corrida: 2 fallos en casos de R1 (*Obtener producto por código* y *Obtener cliente por RUC*) que suponen datos precargados (`TLD-220V`, `80012345-0`) que un esquema nuevo no tiene. Cargados esos dos datos: **63 requests / 106 aserciones, 0 fallos** |
| 02/11/2026 | R3 | Despliegue real | Push de `ffc0286` a `main` → Render (backend, modo stub) + Vercel (frontend) + Neon (V8 aplicada en el arranque). Newman carpetas *Auth*, *Facturas* y *Release 3* contra `https://sifen-backend.onrender.com/api` | Versión nueva en línea ~8 min después del push. **38 requests / 70 aserciones, 0 fallos** (NC/ND, anulación por NC total, Libro IVA con NC negativa, Excel/PDF, consulta de RUC). Se cargó en Neon la configuración del emisor (estaba vacía) con los mismos datos que la local |
| 02/11/2026 | R3 | Unitarias | `mvn test` con el reintento de `siConsRUC` ante `0160` (`ConsultaRucSifenServiceTest`: solo el HTTP 400 con `0160` se considera transitorio; el 302, un error de conexión o un `0500` no se reintentan) | 99 / 99 OK |
| 02/11/2026 | R3 | Integración | Newman, colección completa contra un backend en modo stub en el puerto 8081 (esquema aislado `newman_r3`), corrida varias veces durante la grabación del video de pruebas | Una corrida con 4 fallos en *Establecimientos* (ver B6). Corregida la colección: **64 requests / 106 aserciones, 0 fallos** |
| 02/11/2026 | R3 | Envío SIFEN real (video) | Grabación del video de módulos: alta de cliente con `siConsRUC`, factura y nota de crédito enviadas en vivo desde la interfaz | `siConsRUC` devolvió `0160` y el reintento automático obtuvo `0502` en el mismo pedido. Facturas `001-001-0000002` a `0000005` y NC `0000002` a `0000004` **aprobadas (0260)** en las distintas tomas |
| 03/11/2026 | R3 | Unitarias | `mvn test` con las correcciones de BUG-001, 003, 004, 005 y 006: `FacturaServiceTest` (+7: una falla del correo no revierte la aprobación; la confirmación lee la factura con bloqueo de fila; sin respuesta del SIFEN el documento queda `EN_PROCESO` y no se puede reabrir; verificar aprueba sin reenviar si el SIFEN tiene el CDC, reenvía el mismo XML firmado si no lo tiene y deja `EN_PROCESO` si la consulta tampoco responde; no se verifica un documento que no está `EN_PROCESO`), `EnviadorSifenSoapServiceTest` (+6: un servidor local que acepta el envío y no contesta da "sin confirmar", un puerto cerrado da rechazo `ERROR_CONEXION`, sobre de `siConsDE`, códigos 0422/0420/otro), `ClienteServiceTest` (+2), `JwtAuthFilterTest` (3, nuevo) | **117 / 117 OK** |
| 03/11/2026 | R3 | Build frontend | `tsc -b` con el aviso "Envío sin confirmar" y el botón *Verificar en el SIFEN* (detalle de factura y de nota, y en el listado) | OK |
| 03/11/2026 | R3 | Regresión de defectos (stub) | Script HTTP (`verificar-bugs.js`) contra el backend real en modo stub y PostgreSQL (esquema aislado): 5 rondas de dos `confirmar` simultáneos sobre la misma factura, envío sin respuesta del SIFEN, cambio de RUC de un cliente, usuario desactivado con token vigente y carpeta de correo inutilizable. Para comprobar que el script detecta los defectos, se corrió también contra la versión anterior (`6253d7e`) | **Versión corregida: 13 / 13 OK**; cada documento llegó al SIFEN una sola vez (7 documentos, 7 envíos). **Versión anterior: 2 / 13**: los dos `confirmar` simultáneos llegaban al SIFEN y el segundo terminaba en 500 por `respuesta_sifen_factura_id_key`; cambiar el RUC creaba un cliente duplicado; el token del usuario desactivado seguía dando 200; la falla del correo devolvía 500 y la factura volvía a `BORRADOR` |
| 03/11/2026 | R3 | Consulta SIFEN real | `siConsDE` (`GET /api/sifen/consulta-de/{cdc}`) contra el SIFEN de test con la FE `001-001-0000005`, la NC `001-001-0000004` y un CDC que nunca se envió | FE y NC → **`0422 CDC encontrado`**; CDC inexistente → **`0420 Documento No Existe en SIFEN o ha sido Rechazado`**. Confirma los códigos que usa la verificación de BUG-006 |
| 03/11/2026 | R3 | Integración | Newman, colección completa con la nueva subcarpeta *Release 3 / Correcciones de defectos* (12 requests: BUG-006 sin respuesta → `EN_PROCESO` → no se reabre → *verificar* → aprobado con el mismo número; consulta por CDC; BUG-003; BUG-005 con un operario temporal) contra un backend en modo stub (esquema aislado) | 1.ª corrida: 4 fallos, 2 por los datos precargados que faltan en un esquema nuevo (ya conocido, ver la fila del 02/11) y 2 en la consulta por CDC porque en modo stub la factura no tiene CDC propio: el caso pasó a usar el CDC de una FE aprobada por el SIFEN de test. **76 requests / 125 aserciones, 0 fallos** |
| 05/11/2026 | R3 | Unitarias | `mvn test` con la tanda de ajustes pedida por el usuario (BUG-014 a BUG-022): `ConfiguracionEmisorServiceTest` (+5, actividades económicas: principal única, cambio de principal reutilizando filas, repetidas, compatibilidad con el par código/descripción), `GeneradorDteSifenTest` (+1, un `gActEco` por actividad con la principal primero), `FacturaServiceTest` (+6: 72 h desde la firma en *verificar*, cliente sin email, reenvío del correo), `EnviadorEmailSmtpTest` (5, nuevo: correo con número de comprobante, adjuntos PDF/XML, falla del SMTP, arranque sin servidor), `LibroIvaPdfExporterTest` (4, nuevo), `AuditoriaControllerTest` (rehecho para los filtros) | 1.ª corrida: 1 falla en `CifradoSecretosTest`, ajena a los cambios (ver B7). Después: **139 / 139 OK** |
| 05/11/2026 | R3 | Build frontend | `tsc -b` + `vite build` con el modal sin cierre por clic afuera, alertas, `MontoInput`, buscador de productos, botón de borrador según cambios, "Activo" en las ediciones, configuración con varias actividades, filtros de auditoría y estado del correo | OK |
| 05/11/2026 | R3 | Integración | Newman, colección completa contra un backend en modo stub (esquema aislado `mejoras_newman`, con el producto y el cliente semilla cargados por API) | **76 requests / 125 aserciones, 0 fallos** (la colección no necesitó cambios: los requests aceptan `activo` como opcional) |
| 05/11/2026 | R3 | Funcional por HTTP (stub) | Script `http-mejoras.js`: reactivar producto, cliente y establecimiento desde *Editar*; producto con código y descripción largos; establecimiento con códigos numéricos; factura de un cliente sin email → reenviar → cargar email → reenviar; un admin desactivándose a sí mismo; dos actividades económicas, cambio de principal, sin principal; configuración con timbrado y departamento no numéricos | 14 / 14 OK después de corregir BUG-022 (la última verificación devolvía el 400 genérico de la base en vez de los mensajes por campo) |
| 05/11/2026 | R3 | Funcional contra la app (modo real) | Libro IVA Ventas de noviembre en PDF; alta de establecimiento con `CENTRAL` como código de departamento y un email inválido; auditoría filtrada por `001-001-0000005` y con rango de fechas invertido; migración V9 sobre la base local | PDF con tipo, timbrado y documento asociado separados y "Página 1 de 1"; establecimiento → **400** con los tres mensajes (antes 500); auditoría → 6 registros con número, cliente y estado, y 400 "La fecha 'hasta' no puede ser anterior a 'desde'"; V9 aplicada: la actividad 47890 pasó a la tabla nueva como principal |
| 05/11/2026 | R3 | Unitarias | `mvn test` tras corregir BUG-010/011 y agregar Libro IVA Compras, liquidación y archivo RG 90: `UsuarioServiceTest` (+4), `ComprobanteRegistradoServiceTest` (9: IVA incluido, total = suma, ticket de compra solo total, todos los errores juntos, DV del RUC, autofactura/despacho, ventas electrónicas no se cargan, duplicado, fecha mínima 2021), `ExportadorRg90Test` (6: orden de columnas igual al ejemplo de la Especificación, NC de venta en positivo con asociado, tabuladores en la razón social, nombre del ZIP = TXT y exclusión de DE, lotes de 5.000, período vacío) y `LibroIvaServiceTest` (+3: libro compras solo IVA con NC que resta, ventas preimpresas + SIFEN, liquidación) | **161 / 161 OK**. Primer intento: 1 error del propio test (ver B8) |
| 05/11/2026 | R3 | Integración local (modo real) | Migración `V10__registro_comprobantes_rg90` y script `http-rg90.js` (23 verificaciones): BUG-010, BUG-011, catálogo RG 90, alta/duplicado/validaciones de compras, compra con CDC, venta con ticket sin nombre, Libro IVA Compras y Ventas, liquidación, Excel y PDF del libro de compras, ZIP RG 90 y borrado de lo creado | **23 / 23 OK**. V10 aplicada en 0,08 s. Primer intento 22/23 por una aserción mal planteada (ver B9). El ZIP `4309652_REG_112026_T0001.zip` contiene un TXT con 2 filas (venta de 19 campos, compra de 20) y deja fuera la compra con CDC |
| 05/11/2026 | R3 | Visual | PDF del Libro IVA Compras renderizado a PNG | OK: título, "RUC prov."/"Proveedor", siglas FAC, totales y "IVA crédito fiscal del período" |
| 05/11/2026 | R3 | Build frontend | `tsc -b` + `vite build` con la pantalla *Compras y otros comprobantes* y *Libros IVA* con pestañas Ventas / Compras / Liquidación y RG 90 | OK |

---

## 2. Defectos de la aplicación

### 2.1 Resumen

| ID | Release | Título | Severidad | Estado |
|----|---------|--------|-----------|--------|
| A1 | R1 | El backend no compilaba por colisión de nombre en `ResultadoValidacionRuc` | Alta | Corregido |
| A2 | R1 | 500 en `GET /facturas` por `LazyInitializationException` en `FacturaElectronica.cliente` | Alta | Corregido |
| A3 | R1 | Login de una cuenta desactivada devolvía 500 en vez de 401 | Media | Corregido |
| A4 | R2 | 500 al reconfirmar una factura rechazada (unique en `respuesta_sifen.factura_id`) | Alta | Corregido |
| C1 | R2 | 500 en `GET /api/auditoria` por `LazyInitializationException` en `LogAuditoria` | Alta | Corregido |
| BUG-001 | R3 | Una falla del correo revierte una factura ya aprobada por el SIFEN | Alta | Corregido |
| BUG-002 | R3 | `respuesta_sifen.descripcion` (250) se desborda en modo real | Alta | Corregido |
| BUG-003 | R3 | Editar un cliente cambiando el RUC produce error 500 | Alta | Corregido |
| BUG-004 | R3 | Doble confirmación concurrente envía la factura dos veces | Alta | Corregido |
| BUG-005 | R3 | Usuario desactivado conserva acceso con su JWT vigente | Alta | Corregido |
| BUG-006 | R3 | Timeout con el SIFEN se trata como rechazo y el reintento genera otro CDC | Media | Corregido |
| BUG-007 | R3 | El CDC ignora el establecimiento/punto de expedición de la factura | Media | Corregido |
| BUG-008 | R3 | `sifen.envio.timeout-segundos` no se aplica al tiempo de respuesta | Baja | Corregido |
| BUG-009 | R3 | XML del DTE armado sin escapar caracteres especiales | Media | Corregido |
| BUG-010 | R3 | Token vencido devuelve 403 y el frontend no cierra la sesión | Media | Corregido |
| BUG-011 | R3 | Un ADMIN puede quitarse su propio rol y dejar el sistema sin administradores | Baja | Corregido |
| BUG-012 | R3 | Body mal formado o UUID inválido en la URL devuelven 500 | Baja | Corregido |
| BUG-013 | R3 | El PDF de la factura no es un KuDE: le faltan número, timbrado, CDC y QR | Alta | Corregido |
| BUG-014 | R3 | Crear o editar un establecimiento con un nombre en el código de departamento/ciudad da 500 | Media | Corregido |
| BUG-015 | R3 | Los modales de los ABM se cierran al hacer clic afuera y se pierde lo cargado | Media | Corregido |
| BUG-016 | R3 | El campo de monto muestra un 0 que queda pegado al número tecleado | Baja | Corregido |
| BUG-017 | R3 | "Guardar borrador" sigue activo sin cambios y se puede enviar sin guardar lo último | Media | Corregido |
| BUG-018 | R3 | Las alertas son chicas y desaparecen a los 4 segundos | Baja | Corregido |
| BUG-019 | R3 | El correo al cliente muestra el UUID y se da por enviado aunque el cliente no tenga email | Media | Corregido |
| BUG-020 | R3 | PDF del Libro IVA: la condición de las NC pisa el monto y FE/NC con igual número no se distinguen | Media | Corregido |
| BUG-021 | R3 | *Verificar en el SIFEN* reenvía el DE aunque pasaron más de 72 h desde la firma | Media | Corregido |
| BUG-022 | R3 | `PUT /api/configuracion/emisor` no valida el request (`@Valid` faltante) | Baja | Corregido |

**Patrón recurrente:** A2 y C1 son el mismo defecto (relación `@ManyToOne` LAZY leída fuera de la
transacción con `open-in-view=false`). Revisarlo en todo DTO nuevo que lea una relación.

### 2.2 Defectos de Release 1 y Release 2 (migrados de `SIFEN_Manager_Historial_Pruebas.docx`)

#### A1 — El backend no compilaba por colisión de nombre en `ResultadoValidacionRuc`

- **Estado:** Corregido · **Release:** R1 · **Detectado mediante:** compilación (`mvn test`)
- **Síntoma:** el backend no compilaba.
- **Causa:** el record con componente `boolean valido` genera el accessor `valido()`, que
  colisionaba con una fábrica estática del mismo nombre.
- **Corrección:** renombrar la fábrica estática a `aceptado()`.

#### A2 — 500 en `GET /facturas` por `LazyInitializationException`

- **Estado:** Corregido · **Release:** R1 · **Detectado mediante:** prueba manual (Postman)
- **Síntoma:** 500 en `GET /facturas`, `/facturas/{id}` y `/clientes/{ruc}/facturas`.
- **Causa:** `FacturaElectronica.cliente` estaba mapeado `FetchType.LAZY`; con
  `open-in-view=false` el proxy de Hibernate fallaba al serializar fuera de la transacción.
- **Corrección:** mapear la relación como `FetchType.EAGER` (es `@ManyToOne`, sin riesgo de N+1).

#### A3 — Login de una cuenta desactivada devolvía 500 en vez de 401

- **Estado:** Corregido · **Release:** R1 · **Detectado mediante:** prueba manual (login con cuenta desactivada)
- **Síntoma:** 500 al intentar loguear una cuenta inactiva.
- **Causa:** Spring Security lanza `DisabledException` y no había un handler dedicado; caía en el
  handler genérico de 500.
- **Corrección:** `@ExceptionHandler(DisabledException.class)` que devuelve 401 `ACCOUNT_DISABLED`.

#### A4 — 500 al reconfirmar una factura rechazada

- **Estado:** Corregido · **Release:** R2 · **Detectado mediante:** prueba de integración en vivo (curl)
- **Síntoma:** 500 al confirmar de nuevo una factura rechazada y ya corregida.
- **Causa:** `respuesta_sifen.factura_id` es `unique` y el código insertaba una fila nueva en cada
  confirmación.
- **Corrección:** reutilizar la `RespuestaSifen` ya asociada a la factura.
- **Verificación:** prueba unitaria de regresión en `FacturaServiceTest`.

#### C1 — 500 en `GET /api/auditoria` por `LazyInitializationException`

- **Estado:** Corregido · **Release:** R2 · **Detectado mediante:** prueba manual (curl) al implementar el visor de auditoría
- **Síntoma:** 500 (`INTERNAL_ERROR`) en `GET /api/auditoria`.
- **Causa:** `LogAuditoria.usuario` y `LogAuditoria.factura` eran `@ManyToOne(LAZY)` y
  `AuditoriaResponse.from()` los lee fuera de la transacción (mismo patrón que A2).
- **Corrección:** ambos campos a `FetchType.EAGER`.

### 2.3 Defectos detectados en Release 3

Formato: **Estado** (Abierto / Corregido / Descartado), **Severidad**, **Detectado** (fecha y
cómo), **Ubicación**, **Síntoma**, **Causa**, **Corrección** (archivos y commit), **Verificación**
(test o prueba) y **Corregido** (fecha).

#### BUG-001 — Una falla del correo revierte una factura ya aprobada por el SIFEN

- **Estado:** Corregido
- **Severidad:** Alta
- **Detectado:** 2026-10-06, revisión de código
- **Ubicación:** `backend/.../facturacion/FacturaService.java:122-162` (`confirmarEnvio`),
  `backend/.../email/EnviadorEmailStub.java:50`
- **Síntoma:** si el envío por correo falla después de que el SIFEN aprobó la factura, la API
  responde 500 y la factura vuelve a quedar en `BORRADOR`. Al confirmar de nuevo se reenvía al
  SIFEN un documento que ya estaba aprobado.
- **Causa:** `confirmarEnvio` es una única transacción (`@Transactional`) que incluye la llamada
  al SIFEN, la generación del PDF y el envío por correo. `EnviadorEmailStub` lanza
  `UncheckedIOException` ante un error de disco, lo que revierte toda la transacción, incluido el
  estado `APROBADO` y la `RespuestaSifen`.
- **Corrección:** `FacturaService.enviarPorCorreo` atrapa la falla del PDF/correo: el documento queda `APROBADO` con `emailEnviado=false`, se registra `EMAIL_FALLIDO` en la auditoría y se loguea el error. El correo ya no puede revertir la transacción.
- **Verificación:** `FacturaServiceTest.unaFallaDelCorreoNoRevierteLaFacturaAprobada`; por HTTP, con la carpeta de salida del correo reemplazada por un archivo: antes 500 y la factura volvía a `BORRADOR`, ahora 200, `APROBADO`, `emailEnviado=false`.
- **Corregido:** 03/11/2026

#### BUG-002 — `respuesta_sifen.descripcion` (250) se desborda en modo real

- **Estado:** Corregido
- **Severidad:** Alta
- **Detectado:** 2026-10-06, revisión de código
- **Ubicación:** `backend/.../sifen/EnviadorSifenSoapService.java:94-95` y `:116`;
  `V1__init_schema.sql` (`respuesta_sifen.descripcion varchar(250)`)
- **Síntoma:** en `sifen.envio.modo=real`, una respuesta HTTP distinta de 200 o un error de
  conexión termina en un 500 y no queda registrado el rechazo en la factura.
- **Causa:** la descripción se arma como `"HTTP xxx del SIFEN: " + body` (truncado a 500
  caracteres) o con `e.getMessage()` completo; Postgres rechaza el insert por superar los 250
  caracteres de la columna y se revierte la transacción.
- **Corrección:** migración `V6__numeracion_cdc_y_descripcion_respuesta.sql`: `respuesta_sifen.descripcion` pasa a `text`.
- **Verificación:** envíos reales al SIFEN de test del 29/10/2026 guardaron los motivos de rechazo completos, sin error 500.
- **Corregido:** 29/10/2026

#### BUG-003 — Editar un cliente cambiando el RUC produce error 500

- **Estado:** Corregido
- **Severidad:** Alta
- **Detectado:** 2026-10-06, revisión de código
- **Ubicación:** `backend/.../clientes/ClienteService.java:51-55` y `:71-77` (`aplicar`)
- **Síntoma:** `PUT /api/clientes/{ruc}` con un `ruc` distinto en el body responde 500.
- **Síntoma observado (03/11/2026):** no da 500 como se había previsto: `PUT /api/clientes/{ruc}` con otro RUC responde 200 y **crea un cliente nuevo** con el RUC del body (el original queda igual). `editar` no es transaccional, así que la entidad está desacoplada y `save` hace un `merge` contra un id que no existe.
- **Causa:** `aplicar()` hace `cliente.setRuc(request.ruc())` sobre la entidad cuyo `@Id` es el
  RUC. En la revisión se supuso que Hibernate rechazaría el cambio del identificador; en la
  práctica la entidad está desacoplada y se inserta otro cliente (ver el síntoma observado).
  Además, la edición no valida el RUC nuevo.
- **Corrección:** `ClienteService.editar` rechaza con 409 un RUC distinto del de la URL ("El RUC/CI de un cliente no se puede modificar. Registrá un cliente nuevo… y desactivá el actual"); `aplicar` ya no toca el RUC (solo lo asigna `crear`). El RUC es la clave del cliente y lo referencian sus facturas emitidas. El frontend ya tenía el campo deshabilitado al editar.
- **Verificación:** `ClienteServiceTest` (+2); por HTTP y en Newman (*Correcciones de defectos / BUG-003*): 409 con el mensaje, y editar el resto de los datos sigue funcionando.
- **Corregido:** 03/11/2026

#### BUG-004 — Doble confirmación concurrente envía la factura dos veces

- **Estado:** Corregido
- **Severidad:** Alta
- **Detectado:** 2026-10-06, revisión de código
- **Ubicación:** `backend/.../facturacion/FacturaService.java:122-125`,
  `FacturaElectronica.java` (sin `@Version`)
- **Síntoma:** dos clics seguidos en "Confirmar" (o dos pestañas) pueden mandar dos DTE al SIFEN
  para la misma factura.
- **Causa:** el chequeo `exigirEstado(BORRADOR)` no está protegido por bloqueo optimista ni
  pesimista; ambas requests leen `BORRADOR` antes de que la otra escriba.
- **Corrección:** `confirmarEnvio` lee la factura con `FacturaRepository.findByIdParaActualizar` (`PESSIMISTIC_WRITE`, `SELECT … FOR UPDATE`): la segunda confirmación espera a que termine la primera y ya no encuentra el `BORRADOR` (409). Se eligió bloqueo pesimista y no optimista (`@Version`) porque el optimista detecta el conflicto recién al hacer commit, cuando el DE ya fue enviado dos veces.
- **Verificación:** `FacturaServiceTest.confirmarLeeLaFacturaConBloqueoDeFila`; por HTTP contra PostgreSQL, 5 rondas de dos `confirmar` simultáneos: siempre 200/409 y un solo envío al SIFEN por documento. La versión anterior daba 200/500 con los dos envíos hechos.
- **Corregido:** 03/11/2026

#### BUG-005 — Usuario desactivado conserva acceso con su JWT vigente

- **Estado:** Corregido
- **Severidad:** Alta
- **Detectado:** 2026-10-06, revisión de código
- **Ubicación:** `backend/.../auth/JwtAuthFilter.java:40-48`
- **Síntoma:** un usuario desactivado sigue operando hasta que vence su token (8 h). Si el
  usuario se borra de la base, sus requests devuelven 500.
- **Causa:** el filtro carga el `UserDetails` pero no verifica `isEnabled()`; la
  `UsernameNotFoundException` se lanza dentro del filtro, fuera del `GlobalExceptionHandler`.
- **Corrección:** `JwtAuthFilter` solo autentica si el usuario sigue habilitado (`isEnabled()`); si fue desactivado o borrado, la request sigue sin autenticar y Spring Security la rechaza (403), en vez de operar con el token o terminar en 500.
- **Verificación:** `JwtAuthFilterTest` (3); por HTTP y en Newman (*BUG-005*): el token de un operario recién desactivado pasa de 200 a 403. El código 403 (y no 401) es el de BUG-010, que sigue abierto.
- **Corregido:** 03/11/2026

#### BUG-006 — Timeout con el SIFEN se trata como rechazo y el reintento genera otro CDC

- **Estado:** Corregido
- **Severidad:** Media
- **Detectado:** 2026-10-06, revisión de código
- **Ubicación:** `backend/.../sifen/EnviadorSifenSoapService.java:111-119` y `generarCdc`
- **Síntoma:** ante un timeout la factura queda `RECHAZADO`, aunque el SIFEN pudo haberla
  recibido. Al reabrir y reenviar se genera un número y CDC nuevos, con riesgo de duplicar el
  documento en el SIFEN.
- **Causa:** cualquier excepción se mapea a rechazo; el CDC y el número de documento no se
  persisten en la factura, se recalculan en cada envío.
- **Corrección:** Se distingue "no se pudo conectar" de "no se sabe si llegó". Si no hubo conexión (`ConnectException`, timeout de conexión, handshake TLS) sigue siendo un rechazo. Si el SIFEN no respondió después de recibir el envío (timeout de respuesta, conexión cortada, respuesta ilegible), el documento queda `EN_PROCESO` con su número, CDC y XML firmado, y no se puede reabrir. Nuevo `POST /api/facturas/{id}/verificar` consulta el DE por CDC (`siConsDE`): `0422` → `APROBADO` (sin reenviar); `0420` → reenvía el mismo XML firmado (mismo número y CDC); otra respuesta → sigue `EN_PROCESO`. En el frontend, aviso "Envío sin confirmar" con el botón *Verificar en el SIFEN*. También `GET /api/sifen/consulta-de/{cdc}` para consultar cualquier documento. El stub simula la falta de respuesta con la marca `SIN_RESPUESTA`.
- **Verificación:** `FacturaServiceTest` (+5) y `EnviadorSifenSoapServiceTest` (+6, uno con un servidor HTTP local que no contesta); `siConsDE` real contra el SIFEN de test (`0422` para FE y NC aprobadas, `0420` para un CDC inexistente); por HTTP y en Newman: `EN_PROCESO` → no se reabre → *verificar* → `APROBADO` con el mismo número.
- **Corregido:** 03/11/2026

#### BUG-007 — El CDC ignora el establecimiento/punto de expedición de la factura

- **Estado:** Corregido
- **Severidad:** Media
- **Detectado:** 2026-10-06, revisión de código
- **Ubicación:** `backend/.../sifen/EnviadorSifenSoapService.java:137-152`, `EmisorConfig.java`
- **Síntoma:** en modo real, todas las facturas se envían con el establecimiento/punto de las
  variables de entorno (`001-001` por defecto), sin importar el elegido en el formulario.
- **Causa:** `generarCdc` usa `EmisorConfig` (variables de entorno) en lugar de
  `factura.getEstablecimiento()`/`getPuntoExpedicion()`. RUC y tipo de contribuyente también
  están duplicados entre `EmisorConfig` y la tabla `configuracion_emisor`.
- **Corrección:** el CDC y el grupo C (timbrado) se arman en `GeneradorDteSifen` con el establecimiento/punto de expedición de la factura y su número de documento propio (migración V6: `punto_expedicion.ultimo_numero_documento`, `factura_electronica.numero_documento/cdc/codigo_seguridad`). Los datos del emisor salen solo de `configuracion_emisor`; se eliminó `EmisorConfig` y las variables `SIFEN_EMISOR_*`.
- **Verificación:** `GeneradorDteSifenTest.generaUnRDeConLaEstructuraDelManualYElCdcComoIdDelDe` (establecimiento 002, punto 003, número 42 dentro del CDC) y `FacturaServiceTest.confirmarAsignaElSiguienteNumeroDelPuntoDeExpedicionYGuardaElCdc`.
- **Corregido:** 29/10/2026

#### BUG-008 — `sifen.envio.timeout-segundos` no se aplica al tiempo de respuesta

- **Estado:** Corregido
- **Severidad:** Baja
- **Detectado:** 2026-10-06, revisión de código
- **Ubicación:** `backend/.../sifen/EnviadorSifenSoapService.java:47` y `:85`
- **Síntoma:** cambiar `SIFEN_ENVIO_TIMEOUT_SEGUNDOS` no modifica cuánto se espera la respuesta.
- **Causa:** el timeout de la request usa la constante `TIMEOUT` (30 s); la propiedad solo se usa
  como `connectTimeout`.
- **Corrección:** `EnviadorSifenSoapService` usa `sifen.envio.timeout-segundos` tanto para la conexión como para la respuesta (se eliminó la constante `TIMEOUT`).
- **Verificación:** revisión de código; envíos reales del 29/10/2026 sin regresiones.
- **Corregido:** 29/10/2026

#### BUG-009 — XML del DTE armado sin escapar caracteres especiales

- **Estado:** Corregido (en el envío real; el stub también escapa)
- **Severidad:** Media
- **Detectado:** 2026-10-06, revisión de código
- **Ubicación:** `backend/.../facturacion/FacturaService.java:280-293`
  (`construirXmlSimplificado`); también `EnviadorSifenSoapService.java:191` (parser sin
  protección XXE)
- **Síntoma:** una descripción de ítem con `&` o `<` genera un XML inválido que el SIFEN rechaza.
- **Causa:** el XML se construye por concatenación de strings. El parser de la respuesta no
  deshabilita entidades externas (riesgo bajo por TLS mutuo).
- **Corrección:** el XML real del DE se arma con DOM en `sifen/dte/DeXmlBuilder.java` (el contenido se escapa al serializar) y el parser de respuestas desactiva DOCTYPE/entidades externas (`EnviadorSifenSoapService`, `ConsultaRucSifenService`). `GeneradorDteStub` escapa `& < >` en el XML simplificado.
- **Verificación:** `GeneradorDteSifenTest.escapaCaracteresEspecialesEnLasDescripciones`, `ConsultaRucSifenServiceTest.rechazaRespuestasConDoctype`.
- **Corregido:** 29/10/2026

#### BUG-010 — Token vencido devuelve 403 y el frontend no cierra la sesión

- **Estado:** Corregido
- **Severidad:** Media
- **Detectado:** 2026-10-06, revisión de código
- **Ubicación:** `backend/.../config/SecurityConfig.java:81` (sin `authenticationEntryPoint`),
  `frontend/src/api/client.ts` (sin interceptor de respuesta)
- **Síntoma:** al vencer la sesión, todas las pantallas muestran errores y el usuario no vuelve
  al login.
- **Causa:** sin entry point configurado, Spring responde 403 a requests no autenticadas; el
  frontend no maneja 401/403 para limpiar la sesión.
- **Corrección:** backend: `config/RestAuthenticationEntryPoint` responde **401** con el formato de
  `ApiError` ("Tu sesión venció o no es válida…") a toda request sin token válido (vencido, adulterado
  o de un usuario desactivado); el 403 queda solo para "autenticado sin el rol". Frontend: interceptor
  de respuesta en `api/client.ts`: ante un 401 fuera de `/auth/` guarda el aviso y dispara el evento
  `sifen:sesion-vencida`; `AuthProvider` limpia la sesión y `ProtectedRoute` lleva al login, que
  muestra el motivo. El 401 del login con clave incorrecta no cierra nada (lo muestra el formulario).
- **Verificación:** por HTTP (`http-rg90.js`): sin token → 401 `UNAUTHORIZED`; token adulterado → 401;
  clave incorrecta → sigue 401 `INVALID_CREDENTIALS`; operario contra `/api/usuarios` → sigue 403.
  La aserción de Newman de *BUG-005* ya aceptaba 401/403. Build del frontend OK.
- **Corregido:** 05/11/2026

#### BUG-011 — Un ADMIN puede quitarse su propio rol y dejar el sistema sin administradores

- **Estado:** Corregido
- **Severidad:** Baja
- **Detectado:** 2026-10-06, revisión de código
- **Ubicación:** `backend/.../auth/UsuarioService.java:53-61`
- **Síntoma:** si el único ADMIN se edita a sí mismo como OPERARIO, nadie puede volver a
  administrar usuarios desde la aplicación.
- **Causa:** `editar` no valida el cambio de rol propio ni que quede al menos un ADMIN activo.
- **Corrección:** `UsuarioService`: un ADMIN no puede quitarse su propio rol, y no se puede pasar a
  OPERARIO ni desactivar (por `editar` o `desactivar`) a un ADMIN si no queda otro ADMIN activo
  (`UsuarioRepository.countByRolAndActivoTrueAndIdNot`). Responde 409 con el motivo.
- **Verificación:** `UsuarioServiceTest` (+4: quitarse el rol, pasar a OPERARIO al único admin activo,
  cambiarlo si hay otro admin, desactivar al único admin activo). Por HTTP: el admin que se edita como
  OPERARIO recibe 409 "No podés quitarte tu propio rol de administrador" y sigue siendo ADMIN.
- **Corregido:** 05/11/2026

#### BUG-012 — Body mal formado o UUID inválido en la URL devuelven 500

- **Estado:** Corregido
- **Severidad:** Baja
- **Detectado:** 2026-10-06, revisión de código. Reproducido el 29/10/2026 al probar
  `GET /api/reportes/libro-iva?periodo=octubre`.
- **Ubicación:** `backend/.../common/GlobalExceptionHandler.java`
- **Síntoma:** errores del cliente (JSON inválido, enum desconocido, `/api/facturas/abc`) se
  reportan como "Ocurrió un error inesperado" (500).
- **Causa:** no hay handlers para `HttpMessageNotReadableException` ni
  `MethodArgumentTypeMismatchException`; caen en el handler genérico.
- **Corrección:** handlers que devuelven 400 `INVALID_REQUEST` para
  `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException` (con el nombre y el
  valor del parámetro) y `MissingServletRequestParameterException`.
- **Verificación:** prueba manual contra el backend local, 6 casos, todos 400 (ver sección 1).
- **Corregido:** 29/10/2026

#### BUG-013 — El PDF de la factura no es un KuDE: le faltan número, timbrado, CDC y QR

- **Estado:** Corregido
- **Severidad:** Alta
- **Detectado:** 2026-10-29, reportado por el usuario ("creo que el nro de factura no está ahí")
- **Ubicación:** `backend/.../facturacion/FacturaPdfGenerator.java`; `FacturaResponse`;
  `FacturaController` (nombres de archivo); frontend `FacturasPage` / `FacturaFormPage`
- **Síntoma:** el PDF mostraba cliente, ítems y totales, pero no el número de factura
  (001-001-0000001), los datos del emisor, el timbrado y su vigencia, la condición de venta, el valor
  de venta por tasa, el CDC en grupos de 4, la URL de consulta ni el código QR que exige la sección 13
  del Manual Técnico. La API y la pantalla tampoco mostraban el número, y los archivos se descargaban
  como `factura-<uuid>`.
- **Causa:** el generador era de R2, anterior a la numeración (V6) y al XML real del DE.
- **Corrección:** el PDF se rehízo como KuDE. Si la factura tiene el XML firmado del SIFEN, todo sale
  de ese XML (`KudeDatosFactory.desdeXml`), porque el manual prohíbe mostrar en el KuDE datos que no
  estén en el DE firmado. Si no lo tiene (borrador o modo stub), se arma una vista previa con los
  datos de la factura y la configuración, marcada "sin validez fiscal". El QR (ZXing) se dibuja como
  vector, de 32 mm (el mínimo es 25). Cuando los ítems no entran en una página se agregan páginas, y
  los caracteres fuera de WinAnsi se reemplazan para que el PDF no falle. La API devuelve `numero` y
  `cdc`; el listado muestra la columna Número, el detalle muestra "Factura N° …" y el CDC, y los
  archivos se llaman `factura-001-001-0000001.pdf/.xml`. El XML del modo stub ahora incluye número y
  fecha.
- **Verificación:** 4 pruebas unitarias nuevas o rehechas (74/74) y la descarga real de la factura
  `4bd9897b-…` (sección 1).
- **Corregido:** 29/10/2026

#### BUG-014 — Crear o editar un establecimiento con un nombre en el código de departamento/ciudad da 500

- **Estado:** Corregido
- **Severidad:** Media
- **Detectado:** 03/11/2026 en el log del backend (19:31, dos veces); reportado por el usuario el 05/11 ("al crear un nuevo establecimiento Ocurrió un error inesperado")
- **Ubicación:** `EstablecimientoRequest` y los demás DTO de alta/edición; `GlobalExceptionHandler`; formularios del frontend
- **Síntoma:** la pantalla mostraba "Ocurrió un error inesperado" sin decir qué campo estaba mal.
- **Causa:** `departamento_codigo`, `distrito_codigo` y `ciudad_codigo` son `varchar(5)` (V5) y llevan el código numérico del SIFEN, pero el request no validaba nada. Un nombre como "CENTRAL" llegaba a la base, PostgreSQL respondía "el valor es demasiado largo para el tipo character varying(5)" y eso caía en el handler genérico de 500. Pasaba lo mismo en todos los formularios: ningún request validaba el largo de las columnas.
- **Corrección:** validaciones según el tipo de cada columna en todos los requests: `@Size` con el largo de la tabla, `@Pattern` numérico para los códigos geográficos, el RUC base y el timbrado, `@Email`, `@Digits` para los montos y rangos para cantidad, plazo, cuotas, DV y tipo de contribuyente. Los mensajes nombran el campo ("El código de ciudad es numérico, de hasta 5 dígitos (el nombre va en la descripción)"). La descripción de producto e ítem se limita a 120, el máximo del SIFEN (E708). El 400 de validación muestra solo los mensajes, sin el nombre técnico del atributo. Como red de seguridad, `DataIntegrityViolationException` responde 400/409 legible (largo excedido con el máximo, duplicado, referencia) en vez de 500. En el frontend: `maxLength`, `pattern` e `inputMode` por campo, y una ayuda que explica que en departamento, distrito y ciudad va el código y el nombre en el campo de al lado.
- **Verificación:** por HTTP contra la app: el mismo alta con `CENTRAL` y un email inválido devuelve 400 con los tres mensajes; Newman sin cambios (76/125).
- **Corregido:** 05/11/2026

#### BUG-015 — Los modales de los ABM se cierran al hacer clic afuera y se pierde lo cargado

- **Estado:** Corregido
- **Severidad:** Media
- **Detectado:** 05/11/2026, reportado por el usuario
- **Ubicación:** `frontend/src/components/ui/Modal.tsx` (lo usan todos los ABM)
- **Síntoma:** un clic fuera del formulario cerraba el modal y se perdía lo cargado.
- **Causa:** el fondo del modal tenía `onClick={onClose}`.
- **Corrección:** el fondo ya no cierra: se cierra con la **X** de la esquina, con *Cancelar* o con Escape. Se agregó `role="dialog"`.
- **Verificación:** build del frontend; aplica a productos, clientes, usuarios, establecimientos y puntos de expedición.
- **Corregido:** 05/11/2026

#### BUG-016 — El campo de monto muestra un 0 que queda pegado al número tecleado

- **Estado:** Corregido
- **Severidad:** Baja
- **Detectado:** 05/11/2026, reportado por el usuario ("el campo de monto al cargar un producto tiene un cero medio raro")
- **Ubicación:** `ProductosPage` (precio base) e ítems de `FacturaFormPage` y `NotaFormPage`
- **Síntoma:** el precio arrancaba en `0` y al escribir quedaba "0280000"; además aceptaba decimales, que el guaraní no tiene.
- **Causa:** `input type=number` con valor inicial `0`: React no reformatea el texto de un input numérico.
- **Corrección:** componente `MontoInput`: vacío con placeholder en vez de 0, solo dígitos y separador de miles mientras se escribe ("280.000").
- **Verificación:** build del frontend.
- **Corregido:** 05/11/2026

#### BUG-017 — "Guardar borrador" sigue activo sin cambios y se puede enviar sin guardar lo último

- **Estado:** Corregido
- **Severidad:** Media
- **Detectado:** 05/11/2026, reportado por el usuario
- **Ubicación:** `FacturaFormPage`, `NotaFormPage`
- **Síntoma:** después de guardar, el botón seguía activo. Y si se editaba algo sin guardar, *Confirmar y enviar* mandaba al SIFEN la versión guardada, no la que se veía en pantalla.
- **Causa:** la pantalla no comparaba el formulario con lo último guardado.
- **Corrección:** se guarda una firma (JSON) de lo último guardado o cargado y se compara con el formulario. Sin cambios, el botón queda inactivo con el texto "Borrador guardado". Con cambios sin guardar, *Confirmar y enviar* se inactiva con la ayuda "Guardá el borrador antes de enviarlo".
- **Verificación:** build del frontend.
- **Corregido:** 05/11/2026

#### BUG-018 — Las alertas son chicas y desaparecen a los 4 segundos

- **Estado:** Corregido
- **Severidad:** Baja
- **Detectado:** 05/11/2026, reportado por el usuario
- **Ubicación:** `frontend/src/components/ui/ToastProvider.tsx`
- **Síntoma:** los avisos (por ejemplo, el motivo de un rechazo) no llegaban a leerse.
- **Causa:** texto de 14 px y una duración fija de 4 s.
- **Corrección:** texto de 16 px, ícono más grande y un borde de color por tipo. Duran 6 s (éxito), 8 s (información) y 12 s (error), y tienen una X para cerrarlos antes.
- **Verificación:** build del frontend.
- **Corregido:** 05/11/2026

#### BUG-019 — El correo al cliente muestra el UUID y se da por enviado aunque el cliente no tenga email

- **Estado:** Corregido
- **Severidad:** Media
- **Detectado:** 03/11/2026 al revisar el correo simulado; el usuario pidió "ver envío de correo" el 05/11
- **Ubicación:** `email/EnviadorEmailStub.java`, `FacturaService.enviarPorCorreo`
- **Síntoma:** el cuerpo decía "factura N.° 4bd9897b-…" (el UUID), con el total sin formato. Con un cliente sin email, el "Para:" quedaba vacío y la factura figuraba como *enviada por correo*.
- **Causa:** el stub armaba el texto con `factura.getId()` y no controlaba el destinatario.
- **Corrección:** `CorreoDocumento` arma el asunto y el cuerpo con el tipo y el número del comprobante, el total en guaraníes, el CDC y el enlace de consulta de e-Kuatia; los adjuntos se llaman `factura-001-001-0000005.pdf/.xml`. Sin email no se intenta el envío: queda `emailEnviado=false` y se audita `EMAIL_SIN_DESTINATARIO`. Nuevo `POST /api/facturas/{id}/reenviar-correo` y, en el detalle, a quién se envió o por qué no, con el botón para (re)enviar. Además, envío real por SMTP (`EnviadorEmailSmtp`, `sifen.email.modo=smtp` con `SPRING_MAIL_*` y `SIFEN_EMAIL_REMITENTE`). El modo por defecto sigue siendo el stub hasta que se defina la cuenta. El health de Actuator no depende del SMTP.
- **Verificación:** `EnviadorEmailSmtpTest` (5) y `FacturaServiceTest` (+4). Por HTTP en modo stub: cliente sin email → aprobada sin error → reenviar 409 "no tiene email registrado" → cargar el email → reenviar 200. Correo simulado revisado. **El envío SMTP real todavía no se probó contra un servidor**: falta la cuenta.
- **Corregido:** 05/11/2026

#### BUG-020 — PDF del Libro IVA: la condición de las NC pisa el monto y FE/NC con igual número no se distinguen

- **Estado:** Corregido
- **Severidad:** Media
- **Detectado:** 05/11/2026, reportado por el usuario ("corregir la salida del pdf Libro IVA Ventas")
- **Ubicación:** `reportes/LibroIvaPdfExporter.java`
- **Síntoma:** en las NC se leía "Ajusta 001-001-00000015.500" (el documento asociado encima del gravado 10%). La FE 001-001-0000001 y la NC 001-001-0000001 aparecían iguales, faltaba el timbrado y el pie decía solo "Página 1".
- **Causa:** columnas con posiciones x fijas sin recortar el texto, y sin columna de tipo de comprobante.
- **Corrección:** columnas de ancho fijo con el texto recortado con "…". Columnas nuevas: *Tipo* (FE/NC/ND), *Timbrado* y *Doc. asociado* (en las notas, la condición pasa a "-"). Además: filas alternadas sombreadas, el período como "Noviembre 2026", pie con "Generado el …" y "Página X de N", y un mensaje cuando no hay comprobantes. Los caracteres fuera de WinAnsi se reemplazan para que el reporte no falle.
- **Verificación:** `LibroIvaPdfExporterTest` (4) y el PDF real de noviembre revisado visualmente.
- **Corregido:** 05/11/2026

#### BUG-021 — *Verificar en el SIFEN* reenvía el DE aunque pasaron más de 72 h desde la firma

- **Estado:** Corregido
- **Severidad:** Media
- **Detectado:** 03/11/2026, al responder la consulta del usuario sobre los plazos de 48/72 h; pedido el 05/11 ("validar tiempos de envío y anulación")
- **Ubicación:** `FacturaService.verificarEnvio`, `VerificarEnvioAviso.tsx`
- **Síntoma:** un documento `EN_PROCESO` que el SIFEN no tiene (0420) se reenviaba con el mismo XML firmado, sin importar cuándo se había firmado.
- **Causa:** no se controlaba el plazo de transmisión del Manual Técnico v150 (6.2: hasta 72 h después de la firma).
- **Corrección:** si el SIFEN no tiene el documento y pasaron más de 72 h desde la firma, no se reenvía: queda `RECHAZADO` con el código `PLAZO_VENCIDO` y una explicación, para reabrirlo y volver a firmarlo. La firma se toma de `fechaEmision`, que se fija junto con ella al confirmar. Si el SIFEN sí tiene el documento, queda aprobado igual. El aviso de la pantalla muestra hasta cuándo se puede transmitir.
- **Verificación:** `FacturaServiceTest` (+2): con 73 h y 0420 queda rechazado, sin reenviar y sin contar intento; con 100 h y 0422 queda aprobado.
- **Corregido:** 05/11/2026

#### BUG-022 — `PUT /api/configuracion/emisor` no valida el request (`@Valid` faltante)

- **Estado:** Corregido
- **Severidad:** Baja
- **Detectado:** 05/11/2026, en la prueba por HTTP de BUG-014
- **Ubicación:** `configuracion/ConfiguracionEmisorController.java`
- **Síntoma:** con un timbrado "ABC" y "CAPITAL" como código de departamento, la respuesta era el 400 genérico de la red de seguridad de la base ("supera el largo permitido"), no los mensajes por campo.
- **Causa:** era el único `@RequestBody` sin `@Valid`, así que las anotaciones nuevas del DTO no se evaluaban.
- **Corrección:** `@Valid` en el controller. Se revisaron todos los controllers: no queda ningún `@RequestBody` sin validar.
- **Verificación:** por HTTP: 400 con "El número de timbrado es numérico…", "El dígito verificador va de 0 a 9" y "El código de departamento es numérico…".
- **Corregido:** 05/11/2026

---

## 3. Ajustes a la suite de pruebas

Todos detectados en Release 1, al automatizar la colección de Postman con Newman (migrados de
`SIFEN_Manager_Historial_Pruebas.docx`). Ninguno reflejaba una falla real del sistema.

| ID | Síntoma | Causa | Corrección |
|----|---------|-------|------------|
| B1 | Todas las requests posteriores al login fallaban con 403 | El script de *Login* declaraba `const data`, que colisionaba en el sandbox de Postman y abortaba antes de guardar el token | Renombrar la variable a `loginData` |
| B2 | *Crear cliente* fallaba con 409 (RUC inválido) | Los RUC de prueba tenían un dígito verificador arbitrario | Calcular el DV (módulo 11) en el pre-request script de la colección |
| B3 | 409 "El cliente … está desactivado" en la carpeta Facturas | Facturas reusaba el cliente/producto que las carpetas Clientes/Productos ya habían desactivado | Requests de *Setup* propias dentro de Facturas |
| B4 | Aserción fallaba: `expected undefined to deeply equal BORRADOR` | Se comparaba contra el campo `estado`, que no existe; el real es `estadoDte` | Corregir el nombre del campo |
| B5 | (R3, 02/11/2026) `CifradoSecretosTest.unValorAlteradoNoDescifra` fallaba 1 de cada ~5 corridas | El test alteraba un carácter del base64 que a veces caía en los bits de relleno y no cambiaba los bytes cifrados | Alterar un byte del texto cifrado decodificado (posición 14, después del IV) |
| B6 | (R3, 02/11/2026) *Establecimientos / Crear* devolvió 409 "Ya existe un establecimiento con el código 716" en la 4.ª corrida contra la misma base | El código se tomaba de los últimos 3 dígitos del `runId` (milisegundos): al repetir la colección contra la misma base, tarde o temprano choca | El pre-request lista los establecimientos (`pm.sendRequest`) y elige un código de 3 dígitos libre. La colección pasa a 64 requests (cuenta la consulta) |
| B8 | (R3, 05/11/2026) `LibroIvaServiceTest.laLiquidacion…` falló con `PotentialStubbingProblem` | La liquidación arma también el libro de ventas, que consulta `delPeriodo(VENTA, …)`, y el test solo había simulado la consulta de compras | Simular también la consulta de ventas (lista vacía) |
| B9 | (R3, 05/11/2026) `http-rg90.js`: "formato inválido → 400 con mensajes" esperaba 2 detalles y llegó 1 | El timbrado "1" cumple el formato del DTO (`\d{0,8}`); que tenga exactamente 8 dígitos lo controla el servicio (409), no Bean Validation | La aserción pasa a verificar el mensaje del número de comprobante |
| B7 | (R3, 05/11/2026) `CifradoSecretosTest.elPostProcessorFallaSiHayUnSecretoCifradoYNoHayClaveMaestra` fallaba en una terminal nueva | `StandardEnvironment` incluye las variables de entorno reales, y la máquina ya tiene definida `SIFEN_MASTER_KEY` (RNF-03): el test, que espera que *no* haya clave maestra, la encontraba | El test quita las fuentes del entorno del sistema antes de ejecutar el post-processor |

---

## 4. Hallazgos operativos

### H-01 — Interceptación TLS (MITM) de Kaspersky en la máquina de desarrollo original

- **Fecha:** Release 2 · **Contexto:** primera conexión real de `EnviadorSifenSoapService` a `sifen-test.set.gov.py`
- **Hallazgo:** el certificado recibido era de *Kaspersky Endpoint Security Personal Certification
  Authority*, no el de la SET. Se diagnosticó inspeccionando la cadena (`X509Chain`) con un script
  de PowerShell.
- **Impacto:** la conexión falla con `PKIX path building failed`. El manejo de errores respondió
  como se diseñó: la factura quedó RECHAZADO con ese motivo, sin un 500.
- **Resolución:** repetir la prueba desde un equipo sin interceptación (ver H-04).

### H-02 — Verificación independiente del DV del RUC del emisor

- **Fecha:** Release 2
- **Hallazgo:** antes de cargar el RUC real se recalculó su dígito verificador con un cálculo
  independiente y se detectó una discrepancia (confirmada luego como prueba deliberada), antes de
  que se usara para generar CDC reales.

### H-03 — El backend no arrancaba: base creada en PostgreSQL 9.3

- **Fecha:** 08/10/2026 · **Contexto:** arranque local con `mvn spring-boot:run`
- **Síntoma:** `BeanCreationException ... Cannot resolve reference to bean
  'jpaSharedEM_entityManagerFactory'`.
- **Causa:** en la máquina conviven PostgreSQL 9.3 (puerto 5432), 10 (5433) y 18 (5434). La base
  `sifen_db` se había creado en el 9.3, al que apunta el default `localhost:5432`. Flyway 10 no
  soporta esa versión y, al fallar Flyway, no se crea el `entityManagerFactory`. Además, las
  migraciones usan `gen_random_uuid()`, nativa recién desde PostgreSQL 13.
- **Resolución:** crear rol y base en PostgreSQL 18 (puerto 5434) y apuntar con
  `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/sifen_db` en `backend/.env`.
  Requisito mínimo real: PostgreSQL 13.
- **Hallazgo asociado:** `CREATE DATABASE` falló con `42501 permiso denegado` por ejecutarse con un
  usuario sin `CREATEDB`; se debe ejecutar como `postgres`.

### H-04 — Conectividad TLS con el SIFEN de test confirmada desde la máquina local

- **Fecha:** 27/10/2026
- **Prueba:** handshake TLS a `sifen-test.set.gov.py:443` (IP 201.131.51.6).
- **Resultado:** TLS 1.2; certificado `CN=*.set.gov.py` emitido por *DigiCert Global G2 TLS RSA
  SHA256 2020 CA1*, válido hasta el 26/03/2027, sin errores de validación. No hay interceptación:
  el truststore por defecto del JDK alcanza para validar al servidor.
- **Siguiente paso:** prueba de TLS mutuo + SOAP con la consulta de RUC (`siConsRUC`,
  `GET /api/sifen/consulta-ruc/{ruc}`), que no depende del XML real ni de la firma (Fases 3 y 4).
  Realizada con éxito, ver H-05.
- **Actualización (27/10/2026):** el PostgreSQL 18 quedó reconfigurado en el puerto 5432, así que
  `SPRING_DATASOURCE_URL` apunta a `localhost:5432` (ya no 5434).

### H-05 — Primera comunicación exitosa con el SIFEN de test (TLS mutuo + SOAP)

- **Fecha:** 27/10/2026 · **Máquina:** local, sin interceptación TLS (ver H-04)
- **Configuración:** `SIFEN_ENVIO_MODO=real`, certificado cualificado tributario de persona física
  `CN=BARTOLOME RAMIREZ MEDINA, SERIALNUMBER=CI4309652`, emisor `4309652-2`.
- **Prueba 1, consulta de RUC (`siConsRUC`):** el SIFEN aceptó el certificado y el sobre SOAP y
  respondió `0502 RUC encontrado` (estado `ACT`, 775 ms). Queda confirmado que el TLS mutuo, el
  certificado y el armado/parseo SOAP funcionan contra el SIFEN real.
- **Prueba 2, recepción de DE (`SiRecepDE`):** una factura de prueba fue procesada por el SIFEN y
  rechazada con `0140 — Firma difiere del estándar. [El documento XML no tiene firma]`. El SIFEN ya
  recibe y valida el DE enviado. El rechazo corresponde a lo pendiente: firma XMLDSig real (Fase 4)
  y XML completo del DE (Fase 3).
- **Hallazgo:** la consulta devolvió `dRUCFactElec = N` para el RUC 4309652: **el RUC no figura
  como facturador electrónico** en el ambiente de test. Hay que verificar la habilitación en
  Marangatu antes de esperar una aprobación, aunque el XML y la firma estén correctos.
  Verificado sobre la respuesta cruda del SIFEN (consulta directa desde PowerShell con el mismo
  certificado, 27/10/2026): `<ns2:dRUCFactElec>N</ns2:dRUCFactElec>`.
- **Observación:** `dRazCons` devolvió solo "BARTOLOME" como razón social (dato del ambiente de test).
- **Evidencia:** `backend/sifen-envios-historial.jsonl`, entradas con `dId` 1793131024237
  (consulta RUC) y 1793131051472 (recepción, factura `a74f7383-c045-406c-ac1a-5d2f7e284a8a`).

### H-06 — DE real firmado: el SIFEN de test lo valida hasta el timbrado

- **Fecha:** 29/10/2026 · **Contexto:** primera versión de la generación real del DE (Fases 3 y 4):
  XML v150 armado con DOM, firma XMLDSig enveloped (RSA-SHA256, exc-c14n) con el certificado
  `CN=BARTOLOME RAMIREZ MEDINA` y QR con el CSC de ejemplo del manual (`IdCSC=0001`).
- **Datos del emisor usados:** RUC real 4309652-2, persona física. **Provisorios:** dirección
  "CALLE PROVISORIA", departamento 1 CAPITAL, ciudad 1 ASUNCION (DISTRITO), teléfono 021000000,
  actividad económica 47521, timbrado ficticio 12345678 desde 2026-10-01.
- **Secuencia de respuestas del SIFEN** (factura `4bd9897b-5947-4ff2-bd1c-0070cc410f00`, número 1
  del punto 001-001, reenviada tras cada corrección):
  1. `0160 No se informó el schema en el XML`: la firma ya fue aceptada (antes: `0140`).
  2. Igual, tras declarar `xmlns` como primer atributo de `<rDE>`.
  3. `0160 XML malformado [Elemento esperado: dBasExe dentro de: gCamIVA]`, tras usar
     `xsi:schemaLocation="http://ekuatia.set.gov.py/sifen/xsd siRecepDE_v150.xsd"` (forma de la
     sección 7.2.2 del manual; el ejemplo `…/xsd/siRecepDE_v150.xsd` de otras secciones no
     funciona).
  4. **`1101 TEST - Número de timbrado inválido`**, tras agregar `dBasExe` (campo E737 de una nota
     técnica posterior al manual v150; vale 0 salvo ítems "gravado parcial").
- **Conclusión:** del lado del sistema, el DE ya cumple el formato, el schema y la firma que exige el
  SIFEN. El bloqueo actual es el timbrado: hace falta un timbrado electrónico real asignado al RUC
  4309652 en Marangatu (validaciones 1101–1106).
- **Todavía no validado por el SIFEN** (se evalúa después del timbrado): datos del emisor contra el
  RUC, QR con el CSC de ejemplo, datos del receptor y totales.
- **Lecciones para el documento de implementación:** el manual v150 publicado no alcanza por sí
  solo. El XSD vigente exige campos de notas técnicas posteriores (`dBasExe`) y la forma del
  `schemaLocation` varía entre secciones del propio manual. Los mensajes de rechazo del SIFEN de
  test fueron suficientemente precisos para corregir cada punto en minutos.

### H-07 — Habilitación como facturador electrónico aceptada (timbrado de test creado)

- **Fecha:** 29/10/2026 · **Origen:** correo de la SET al contribuyente.
- **Contenido:** la solicitud de habilitación como facturador electrónico N° 364010071258 está en
  estado ACEPTADO. Con ella se creó el **timbrado de Documentos Electrónicos** del RUC 4309652 para
  pruebas y homologación en el ambiente de TEST. La SET entrega CSC genéricos: `IdCSC 1 =
  ABCD0000000000000000000000000000` e `IdCSC 2 = EFGH0000000000000000000000000000`.
- **Implicancias:**
  - Explica el rechazo `1101` de H-06: el SIFEN valida el número de timbrado aunque sea test. El
    timbrado ficticio 12345678 se tiene que reemplazar por el número real (8 dígitos) y su fecha de
    inicio de vigencia, tal como figuran en Marangatu. El número de solicitud (12 dígitos) no es el
    timbrado.
  - El CSC configurado por defecto (`0001` / `ABCD…`) coincide con el IdCSC 1 genérico, así que no
    hay que cambiarlo para test.
  - Hay que volver a consultar `siConsRUC`: en H-05 el RUC figuraba con `dRUCFactElec=N`.

### H-08 — `respuesta_sifen` no conserva el historial de rechazos

- **Fecha:** 29/10/2026 · **Contexto:** diseño de las estadísticas de rechazo del R3.
- **Hallazgo:** `respuesta_sifen` tiene `factura_id unique` y se sobrescribe en cada reenvío. Una
  factura rechazada dos veces y aprobada a la tercera solo muestra la aprobación, así que esa tabla no
  sirve para medir rechazos. El historial JSONL (`sifen-envios-historial.jsonl`) sí guarda cada
  intento, pero es un archivo local: en Render se pierde con cada despliegue.
- **Decisión:** nueva tabla append-only `intento_envio_sifen` (migración V7), escrita en
  `FacturaService.confirmarEnvio` en cada envío. La migración carga, para las facturas ya enviadas,
  su última respuesta como único intento conocido. Los intentos intermedios anteriores a V7 se
  perdieron.

### H-09 — Datos del emisor para TEST indicados por la SET, y el formato del timbrado

- **Fecha:** 29/10/2026 · **Origen:** instrucción de la SET para la prueba de transmisión en TEST.
- **Contenido:**
  - Timbrado = RUC sin dígito verificador (ejemplo de la SET: 80000000).
  - Establecimiento 001; puntos de expedición 001, 002 y 003.
  - Tipos de documento habilitados: factura, nota de crédito, nota de débito, autofactura y nota de
    remisión electrónicas.
- **Hallazgo:** `dNumTim` (C004) es numérico de 8 dígitos exactos (`tdNumTim` en el XSD). El RUC
  4309652 tiene 7, así que hay que completarlo con un cero a la izquierda: `04309652`. Con ese valor
  el SIFEN acepta el timbrado.
- **Pendiente:** la fecha de inicio de vigencia (C008). El SIFEN rechaza `2026-10-01` y `2026-10-29`
  con 1107. Hay que tomarla de Marangatu (detalle del timbrado o de la solicitud 364010071258).
- **Actualización 02/11/2026:** la fecha real de inicio es `2026-09-01` (Marangatu); con ella el SIFEN
  ya no devuelve 1107. El siguiente rechazo es `1261` (actividad económica): `47521` no es la
  actividad declarada en el RUC. Hay que tomar el código (y su descripción) de la constancia de RUC
  en Marangatu.
- **Cierre 02/11/2026:** con la actividad `47890` y su descripción exacta, el SIFEN aprueba la factura
  (`0260`). Las tres validaciones del emisor que bloqueaban (1107, 1261, 1262) se resuelven con datos
  reales de Marangatu, no adivinando valores.
- **Configuración local al cierre:** timbrado `04309652`, inicio `2026-09-01` (desde el 02/11; antes `2026-10-29` provisorio);
  establecimiento 001, punto 001.

### H-10 — Notas de crédito/débito (M6): reglas del SIFEN y decisión de modelo

- **Fecha:** 02/11/2026 · **Origen:** implementación de M6 contra el Manual Técnico v150.
- **Reglas del manual que condicionan el diseño:**
  - C002 = 5 (NC) / 6 (ND); el CDC empieza con `05` / `06`.
  - E5 `gCamNCDE` (motivo E401/E402) es obligatorio (1450) y la descripción debe coincidir con el
    código (1452).
  - Las notas **no** llevan E7 condición de la operación (1501), D011 tipo de transacción ni E1
    `gCamFE`.
  - H `gCamDEAsoc` es obligatorio (2400), una sola vez (2415), y debe referir a una factura
    electrónica (2414) por su CDC.
  - La suma de las NC no puede superar el total de la factura (2417). Se valida antes de enviar,
    contando también las notas pendientes; las rechazadas no cuentan.
- **Decisión de modelo:** la planificación preveía una entidad `NOTA_CREDITO_DEBITO` (id_nota, tipo,
  motivo, fecha_emision, estado_dte, id_factura_referenciada) con relación 1:N a la factura. Se
  implementó como filas de `factura_electronica` con `tipo_doc` (FE/NC/ND), `motivo_emision` y
  `factura_asociada_id` (la misma relación 1:N). Así la nota reutiliza sin duplicar el ciclo de
  vida del DTE: ítems, IVA, firma, envío, reintentos, KuDE, correo e historial de intentos. Los
  campos de la entidad planificada quedan cubiertos uno a uno.
- **Numeración:** cada tipo de documento tiene la suya por punto de expedición
  (`punto_expedicion.ultimo_numero_nc/nd`; índice único `(punto, tipo_doc, número)`).
- **Anulación:** cuando las NC aprobadas suman el total de la factura, esta pasa a ANULADO. En el
  Libro IVA la factura anulada se sigue listando y las NC restan, en el mes en que se emiten.
- **Pendiente:** probar una NC contra el SIFEN de test. Necesita primero una factura aprobada allí,
  y eso está bloqueado por el 1107 (ver H-09).

### H-11 — El SIFEN de test redirige a `/vdesk/hangup.php3` (HTTP 302)

- **Fecha:** 02/11/2026 ~10:45 · **Origen:** primera prueba de la búsqueda de RUC conectada al alta
  de clientes.
- **Síntoma:** `siConsRUC` (`https://sifen-test.set.gov.py/de/ws/consultas/consulta-ruc.wsdl`)
  responde HTTP 302 con `Location: /vdesk/hangup.php3` y cuerpo vacío, en 5 consultas seguidas y en
  un reintento posterior. El 29/10 el mismo código, con la misma URL y el mismo certificado, recibía
  `0502`/`0500`.
- **Análisis:** `/vdesk/hangup.php3` es la URL de cierre de sesión del F5 BIG-IP APM que la SET
  tiene delante del SIFEN: el gateway corta la conexión antes de llegar al servicio. El certificado
  (CODE100, CI 4309652) está vigente hasta el 18/09/2027, así que no es un vencimiento. Posibles
  causas: mantenimiento del ambiente de test, una política de acceso del gateway (p. ej. por IP de
  origen) o un cambio del lado de la SET.
- **Acción:** el mensaje de error ahora incluye el destino de la redirección, para diagnosticarlo. La
  búsqueda de RUC degrada como se diseñó: valida el DV localmente y permite cargar los datos a mano.
- **Actualización 02/11/2026 ~20:30:** el 302 ya no aparece; el SIFEN responde `0502`/`0500` con
  datos reales. Queda un `HTTP 400 / 0160 XML Mal Formado` **intermitente** (aprox. la mitad de los
  pedidos): el mismo sobre SOAP, reenviado sin cambios, alterna entre `0160` y `0502`. Ya se había
  visto una vez el 29/10. Es del ambiente de test, no del XML.
- **Pendiente:** evaluar un reintento automático de `siConsRUC` ante `0160` (la consulta es de solo
  lectura, así que reintentar no tiene efectos colaterales). Seguir observando si el `0160` también
  afecta a `siRecepDE`.

### H-12 — RNF-03: secretos de configuración cifrados (AES-256-GCM)

- **Fecha:** 02/11/2026 · **Origen:** cierre de R3 (prueba de seguridad planificada: certificado y
  credenciales cifrados).
- **Diseño:** cualquier propiedad puede valer `ENC(base64(iv || cifrado+tag))`. Al arrancar,
  `DescifradoSecretosPostProcessor` (un `EnvironmentPostProcessor`) la descifra con la clave maestra
  `SIFEN_MASTER_KEY` y la publica en memoria; los consumidores (`SifenKeystoreConfig`, CSC del QR,
  datasource) no cambian. La clave maestra está en una variable de entorno de usuario de Windows, no
  en el `.env` ni en el repo. El `.pfx` ya está cifrado por PKCS#12 con su contraseña, que ahora
  también está cifrada. Las contraseñas de usuarios ya usaban BCrypt (R1).
- **Herramienta:** `java -cp target/classes com.tottalstore.sifen.seguridad.CifradoSecretos
  generar-clave | cifrar` (`cifrar` lee el valor sin eco). En modo real, una contraseña en texto plano
  sigue funcionando pero deja un `WARN` de RNF-03.
- **Hallazgo (1.er arranque):** el secreto se descifraba bien (abría el `.pfx` fuera de Spring) pero
  el certificado fallaba con `keystore password was incorrect`. Causa: se publicaba en una
  `MapPropertySource`; la resolución de Spring Boot pasa por `ConfigurationPropertySources`, que no
  mapea nombres estilo variable de entorno (`SIFEN_FIRMA_KEYSTORE_PASSWORD`) en una fuente común, así
  que ganaba el `ENC(...)` original. Corrección: publicarla como `SystemEnvironmentPropertySource`
  con nombre `*-systemEnvironment`. El test ahora adjunta `ConfigurationPropertySources` para
  reproducir el caso real.
- **Estado local:** `.env` con la contraseña del certificado en `ENC(...)`; `iniciar-backend.ps1`
  toma `SIFEN_MASTER_KEY` del ámbito de usuario si la terminal es anterior.

### H-13 — Plazos de envío y de anulación (Manual Técnico v150)

- **Fecha:** 05/11/2026, a partir del pedido del usuario "validar tiempos de envío y anulación".
- **Envío:** el DE se transmite dentro de las 72 h siguientes a su firma (6.2). En este sistema la
  firma y el envío ocurren en el mismo *confirmar*, así que el plazo solo se puede pasar al reenviar
  un documento `EN_PROCESO`. Ese caso se controla desde BUG-021.
- **Anulación:** el plazo de 48 h (FE) o 168 h (otros DTE) desde la aprobación es el del **evento de
  cancelación** (`siRecepEvento`), que el sistema todavía no implementa. La anulación que tiene el
  sistema es por **nota de crédito** (M6), que no tiene ese plazo, así que hoy no hay dónde aplicar
  la regla de 48 h. Si se implementa el evento de cancelación, el control de 48/168 h va ahí, y
  vencido ese plazo la pantalla debería ofrecer solo la NC.

### H-14 — Libros IVA y Registro de Comprobantes (RG 90/2021)

- **Fecha:** 05/11/2026, a partir del pedido "implementemos los módulos necesarios para cumplir con
  los libros de IVA".
- **Fuente:** *Especificación Técnica para Importación — Registro de Comprobantes de Ventas, Compras,
  Ingresos y/o Egresos* de la SET (v1, junio 2021), publicada por la DNIT.
- **Qué exige:** los contribuyentes registran mensualmente sus comprobantes en Marangatu (obligación
  955), a mano o importando un archivo. Puntos clave de la especificación:
  - el archivo **no debe incluir comprobantes del SIFEN (e-Kuatia) ni virtuales**, que Marangatu
    toma directamente;
  - es CSV o TXT con tabuladores, en UTF-8 y sin fila de títulos, comprimido en un ZIP con el mismo
    nombre `<RUC sin DV>_REG_MMAAAA_XXXXX`;
  - lleva hasta 5.000 filas por archivo;
  - los montos van enteros y positivos;
  - la SET valida el archivo en hasta 72 h, y las correcciones se hacen a mano.
- **Consecuencia para Tottal Store:** las ventas electrónicas ya están en la SET. Lo que faltaba para
  cumplir era:
  1. registrar las **compras**, que dan el crédito fiscal;
  2. registrar las ventas que no pasan por el SIFEN (preimpresas o tickets);
  3. tener el **Libro IVA Compras**;
  4. tener una **liquidación** débito − crédito del mes;
  5. generar el **archivo de importación**.
- **Decisiones:**
  - Formato TXT con tabuladores, para que una coma en la razón social no corra las columnas (el
    ejemplo CSV del anexo usa ";", así que el CSV quedaba ambiguo).
  - Una compra con CDC (DE recibido) entra al Libro IVA Compras pero no al archivo.
  - Al Libro Compras solo va lo imputado al IVA.
  - En compras, autofactura, boleta resimple, loterías y ticket solo informan el total, como manda
    la especificación, y no dan crédito fiscal.
  - La liquidación es referencial para el Formulario 120: no incluye saldos anteriores, retenciones
    ni percepciones.
  - Los registros de ingresos y egresos (tipos 2xx) quedan fuera: son del IRP-RSP, que no aplica a
    Tottal Store.
- **Implementación:**
  - Base: migración V10.
  - Backend: paquete `registro/` y `ReportesController` (`/libro-iva?libro=COMPRAS`,
    `/liquidacion-iva`, `/registro-rg90`).
  - Pantallas: *Compras y otros comprobantes*, y *Libros IVA* con pestañas.
- **Pendiente de validar:** importar un archivo real en Marangatu con el RUC de Tottal Store. Eso
  solo se puede hacer con su clave de acceso, y la SET puede tardar hasta 72 h en validarlo.
