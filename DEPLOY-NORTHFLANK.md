# Guia de Deploy no Northflank (Plano Gratuito)

Este documento detalha o passo a passo para hospedar o backend **ThicketHub (Java 21 + Spring Boot 3)** no plano gratuito (Developer Free Tier) do **Northflank**.

---

## 1. O que foi preparado no projeto

- **`Dockerfile` multi-stage com OpenJDK 21 Alpine:** Imagem final ultra-leve (~150MB com JRE 21), sem ferramentas de compilação ou arquivos desnecessários.
- **Ajuste fino de memória para instâncias Micro (512MB RAM):**
  - `-XX:MaxRAMPercentage=75.0`: Reserva 25% da RAM para metaspace e thread stacks, prevenindo terminação por OOMKilled.
  - `-XX:+UseSerialGC`: Reduz significativamente a sobrecarga de memória e CPU em instâncias pequenas (0.2–0.5 vCPU).
  - `-XX:+ExitOnOutOfMemoryError`: Garante reinício rápido e limpo caso a memória seja excedida.
- **Pool de Conexões HikariCP Otimizado:** Máximo de 5 conexões simultâneas para não estourar os limites de conexões gratuitas e economizar RAM.
- **Porta Dinâmica:** Mapeada para `${PORT:${SERVER_PORT:8080}}`, compatível com a injeção automática de porta do Northflank.
- **Tratamento Automático de Banco de Dados (`DATABASE_URL` / `POSTGRES_URI`):** Converte automaticamente URLs em formato `postgres://` ou `postgresql://` (padrão de nuvem) para `jdbc:postgresql://` e extrai usuário e senha com fallback transparente.
- **Health Checks & Actuator:** Endpoint `/api/actuator/health` configurado com verificações de liveness e readiness isoladas de dependências opcionais (como Redis e Mail).
- **CORS flexível:** Suporte nativo para domínios do Northflank (`https://*.northflank.app`), Vercel (`https://*.vercel.app`) e variável de ambiente `CORS_ALLOWED_ORIGINS`.

---

## 2. Passo a Passo no Northflank

### Passo 2.1: Criar o Projeto
1. Acesse o painel do [Northflank](https://app.northflank.com/) e faça login.
2. Clique em **Create Project**.
3. Escolha um nome para o projeto (ex: `thickethub`).
4. Selecione a região mais próxima (ex: `eu-west` ou `us-east`).

---

### Passo 2.2: Criar o Banco de Dados PostgreSQL (Addon Gratuito)
1. No menu lateral do projeto, clique em **Addons** > **Create Addon**.
2. Escolha **PostgreSQL**.
3. Selecione a versão recomendada (ex: `16` ou `15`).
4. Em **Plan**, escolha o plano gratuito disponível (ex: **Micro** ou **Free/Sandbox**).
5. Defina um nome para o addon (ex: `thickethub-db`) e clique em **Create Addon**.
6. Aguarde alguns segundos até o banco inicializar.

> **Nota:** Se você preferir usar outro PostgreSQL gratuito (como [Neon.tech](https://neon.tech) ou [Supabase](https://supabase.com)), basta copiar a URL de conexão fornecida por eles e configurá-la como a variável de ambiente `DATABASE_URL`.

---

### Passo 2.3: Criar o Serviço do Backend (Combined Service ou Deployment)
1. No painel do projeto, clique em **Services** > **Create Service**.
2. Selecione **Combined Service** (ele compila o código direto do GitHub e o executa).
3. Conecte sua conta do GitHub e selecione o repositório `ThicketHubJava` e branch `master` (ou `main`).
4. Em **Build configuration**:
   - **Build type:** Selecione **Dockerfile**.
   - **Dockerfile path:** `./Dockerfile`
   - **Context path:** `/`
5. Em **Deployment configuration**:
   - **Plan:** Selecione o plano **Micro** (0.2 vCPU / 512MB RAM) compatível com o free tier.
   - **Instances:** `1`.

---

### Passo 2.4: Configurar Networking & Portas
1. Na seção **Networking / Ports**:
   - Clique em **Add Port**.
   - **Port name:** `http`
   - **Port number:** `8080`
   - **Protocol:** `HTTP`
   - Marque a opção **Publicly readable via Northflank domain** (gera a URL pública HTTPS).

---

### Passo 2.5: Configurar Health Check (Probes)
1. Na seção **Health checks**:
   - **Health check path:** `/api/actuator/health`
   - **Initial delay:** `40s` (tempo para a JVM inicializar no plano free)
   - **Timeout:** `5s`

---

### Passo 2.6: Variáveis de Ambiente (Environment Variables)
Na seção **Environment**:

Se você criou o **PostgreSQL Addon** no Northflank:
- Você pode usar **Link Addon** e vincular o `thickethub-db`.
- O Northflank criará variáveis de conexão automaticamente (nosso backend detecta `POSTGRES_URI` ou `DATABASE_URL` sozinho).

Adicione manualmente as variáveis abaixo:

| Variável | Valor | Descrição |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` | Ativa o perfil de produção |
| `JWT_SECRET` | `9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f` | Chave secreta de no mínimo 256 bits |
| `DB_POOL_MAX_SIZE` | `5` | Limite de conexões para economizar RAM |
| `DB_POOL_MIN_IDLE` | `1` | Mínimo de conexões ociosas |
| `CORS_ALLOWED_ORIGINS` | `https://thicket-hub-front.vercel.app` | URL do seu frontend |

*(Caso esteja usando banco externo, como Neon ou Supabase):*
- `DATABASE_URL`: `postgresql://usuario:senha@ep-exemplo.neon.tech/neondb?sslmode=require`

---

### Passo 2.7: Deploy
1. Clique em **Create Service**.
2. O Northflank iniciará o build do Dockerfile e o deploy.
3. Você pode acompanhar os logs em tempo real na aba **Logs**.
4. Quando finalizar, você verá o status **Running** e a URL pública gerada (ex: `https://thickethub-backend-xxxxx.code.northflank.app`).

---

## 3. Conectar o Frontend (`ThicketHubFront`)
No seu projeto frontend (`ThicketHubFront/.env.production` ou nas variáveis de ambiente da Vercel):
```env
VITE_API_URL=https://thickethub-backend-xxxxx.code.northflank.app
```
*(Substitua pela URL real fornecida pelo Northflank)*.
