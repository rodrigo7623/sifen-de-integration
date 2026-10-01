-- Integración real con el SIFEN, Fase 1 (conectividad): actualiza la URL del web service de
-- recepción síncrona (SiRecepDE) en ambiente de prueba, tomada del Manual Técnico v150, pág. 41.
-- La URL de invocación exacta (si coincide con el WSDL o requiere una variante sin ".wsdl") se
-- termina de confirmar al probar con el certificado real -- ver EnviadorSifenSoapService.

update enviador_sifen
set url_endpoint = 'https://sifen-test.set.gov.py/de/ws/sync/recibe.wsdl'
where url_endpoint = 'https://sifen-test.set.gov.py (pendiente de configurar)';
