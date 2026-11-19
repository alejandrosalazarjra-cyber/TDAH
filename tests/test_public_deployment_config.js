'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const docker=fs.readFileSync('backend/Dockerfile','utf8');
const render=fs.readFileSync('render.staging.yaml','utf8');
const workflow=fs.readFileSync('.github/workflows/public-backend-check.yml','utf8');
const compose=fs.readFileSync('backend/compose.yaml','utf8');
const migrator=fs.readFileSync('backend/migrate.js','utf8');
const readme=fs.readFileSync('docs/DESPLIEGUE_PUBLICO_STAGING.md','utf8');
assert(docker.includes('USER node'),'El proceso Node debe ejecutarse sin root');
assert(docker.includes('migrate.js')&&docker.includes('schema_public_v1.sql'),
 'La imagen debe contener el esquema inicial y el migrador');
assert(render.includes('autoDeployTrigger: off'),'Nunca desplegar automáticamente la infraestructura pública');
assert(render.includes('plan: free')&&render.includes('PRUEBAS TEMPORALES'),
 'La configuración gratuita es solo un piloto desechable');
assert(render.includes('ipAllowList: []'),'La base de datos no debe tener acceso externo');
assert(render.includes('fromDatabase:')&&render.includes('property: connectionString'),
 'La API obtiene DATABASE_URL mediante la red privada de PostgreSQL');
assert(!render.includes('key: GOOGLE_CLIENT_ID'),
 'GOOGLE_CLIENT_ID se gestiona en variables privadas del servicio Render');
assert(render.includes('healthCheckPath: /health'),'El despliegue requiere una comprobación de salud');
assert(compose.includes('127.0.0.1:8081:8081'),'El entorno local no debe publicar el puerto en la red');
assert(!compose.includes('5432:5432'),'La base de desarrollo no debe exponerse a la red');
assert(migrator.includes("NEUROSYNC_PUBLIC_DB_CONFIRM!=='yes'"),
 'No ejecutar migraciones sin confirmación explícita');
assert(workflow.includes('postgres:16-alpine')&&
 workflow.includes('node tests/test_public_backend_database.js'),
 'Las pruebas independientes requieren PostgreSQL real');
assert(readme.includes('exclusivamente de pruebas')&&readme.includes('loginEnabled'),
 'La guía documenta staging activo y registro bloqueado, nunca producción');
console.log('Despliegue de pruebas OK: configuración segura y CI independiente');
