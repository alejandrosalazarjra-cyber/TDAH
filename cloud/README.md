# NeuroSync en internet, sin Tailscale ni Netlify

Este servicio es opcional y necesita un proyecto **Cloudflare Workers + D1**. Cuando esté publicado, cada teléfono solo necesitará la APK e internet.

1. Registra una cuenta de Cloudflare y abre Workers & Pages / D1.
2. Desde un entorno autorizado con Node.js, ejecuta `npx wrangler login`.
3. En `cloud/`, crea la base: `npx wrangler d1 create neurosync-family`. Copia su `database_id` al archivo `wrangler.toml`.
4. Aplica migraciones: `npx wrangler d1 migrations apply DB --remote`.
5. Genera una clave aleatoria larga; guárdala solo en Cloudflare como secreto: `npx wrangler secret put BOOTSTRAP_SECRET`. No uses admin1234 en internet.
6. Publica el servicio: `npx wrangler deploy`.
7. Usa el endpoint inicial **solo una vez** para registrar el primer administrador con una contraseña fuerte:

```sh
curl -X POST 'https://TU_WORKER.workers.dev/bootstrap' \
  -H 'Content-Type: application/json' \
  -H 'X-Bootstrap-Secret: TU_SECRETO_PRIVADO' \
  --data '{"username":"administrador","password":"UNA_CONTRASENA_DE_AL_MENOS_12_CARACTERES"}'
```

8. Configura la dirección HTTPS del Worker en cada APK desde **Vincular mi celular → Sincronización por internet**. Los tres se autentican con sus credenciales (el administrador crea las de mamá y hermano).

**No se ha desplegado este servicio**: falta que el propietario active y configure su cuenta Cloudflare. No hay servidor público disponible hasta entonces. La app mantiene datos locales cuando no hay red y reintenta sincronizar al abrirla.

Seguridad: TLS, contraseñas derivadas con PBKDF2 + sal individual, sesiones opacas de duración limitada, permisos comprobados en el servidor y límites de tamaño. Añade monitorización, copias de seguridad, rotación de credenciales y pruebas de concurrencia antes de utilizarlo con datos sensibles.

Cloudflare D1 se convierte en la **base central** de esta modalidad: el teléfono del administrador controla usuarios y asignaciones, pero **no es un servidor 24/7**. Si quieres que el teléfono almacene la única base central sin un servidor intermedio, necesitas una conexión entrante permanente o una VPN.
