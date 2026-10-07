# NexWallet Backend

![Java](https://img.shields.io/badge/Java-21-blue?style=flat&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.3-success?style=flat&logo=springboot)
![License](https://img.shields.io/github/license/FelipeRodrigues05/febank-backend)
![MySQL](https://img.shields.io/badge/MySQL-8.0-informational?style=flat&logo=mysql)
![OAuth2](https://img.shields.io/badge/Auth-OAuth2.0-yellow?style=flat&logo=jsonwebtokens)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-orange?style=flat&logo=rabbitmq)
![Flyway](https://img.shields.io/badge/Migrations-Flyway-red?style=flat)

Backend do **NexWallet**, um sistema bancário digital completo desenvolvido com **Java 21**, **Spring Boot 3.5**, arquitetura modular por domínio, e integrações simuladas com BACEN/Receita Federal.

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Runtime | Java 21 + Spring Boot 3.5.3 |
| Segurança | Spring Security + JWT HMAC256 + OAuth 2.0 Client Credentials |
| Banco de Dados | MySQL 8 + Spring Data JPA + Flyway |
| Mensageria | RabbitMQ (transações e transferências assíncronas) |
| Observabilidade | Spring Actuator + Micrometer + Prometheus + Grafana |
| E-mail | Spring Mail + Mailpit (dev) |
| Documentação | Springdoc OpenAPI (Swagger UI) |
| Build | Maven 3 + Docker Compose |

---

## Módulos e Funcionalidades

### Autenticação e Segurança
- OAuth 2.0 Client Credentials com JWT próprio (sem Keycloak)
- Autenticação de usuários via e-mail/senha
- 2FA via OTP de 6 dígitos enviado por e-mail (validade 5 min)
- Dispositivos confiáveis com validade de 30 dias
- Limites operacionais por usuário (PIX diário, TED, compra, saque)

### Contas Bancárias
- Abertura de conta corrente e poupança
- Depósito e saque
- Extrato com filtros avançados (tipo, categoria, período, status) com paginação
- Categorização automática de transações (alimentação, transporte, investimento, etc.)

### Transferências
- Transferências internas entre contas (processamento assíncrono via RabbitMQ)
- Transferências agendadas (TRANSFER, PIX, BOLETO) com cron configurável

### PIX
- Registro e gestão de chaves PIX (CPF, CNPJ, e-mail, telefone, aleatória)
- Transferência PIX com consulta ao DICT simulado
- QR Code estático e dinâmico (payload EMV simulado)
- PIX agendado com processamento automático
- Devolução/estorno de PIX
- PIX Saque e PIX Troco
- Limites diários, por transação e noturnos (20h–6h)
- Contatos favoritos com contagem de frequência

### Cartões
- Cartão de débito e crédito
- Número e CVV armazenados criptografados (AES-256-ECB)
- Fatura mensal com fechamento automático
- Pagamento de fatura
- Compra parcelada com Tabela Price
- Cartão virtual com validade de 1 ano
- Bloqueio e desbloqueio de cartão
- Chargeback (contestação) com aprovação/rejeição
- Cashback de 0,5% em compras no crédito
- Solicitação de aumento de limite (auto-aprovação até 3× o limite atual)

### Boleto
- Emissão com código de barras simulado (padrão FEBRABAN)
- Pagamento com validação de vencimento
- Expiração automática de boletos vencidos via cron

### Investimentos
- Transferência entre conta corrente e conta investimento
- Catálogo de produtos: CDB 120% CDI, CDB 100% CDI, LCI 95% CDI, LCA 90% CDI, Tesouro Selic 2027, Tesouro Prefixado 12,75% 2029
- Aplicação e resgate em produtos do catálogo
- Rendimento diário automático (taxa anual / 252 dias úteis)

### Cofrinhos (Savings Box)
- Criação com depósito inicial opcional
- Depósito e resgate vinculados à conta corrente

### Crédito Pessoal
- Simulação de empréstimo com Tabela Price
- Taxa de juros por score de crédito: 1,5% (≥800), 2,5% (≥600), 4,0% (≥400), 6,0% (<400)
- Aprovação com disbursamento automático
- Pagamento de parcelas

### Compliance (BACEN/Receita Federal simulado)
- KYC com validação matemática de CPF e CNPJ (dígitos verificadores reais)
- AML: flagging automático de transações acima do limiar COAF (R$10.000)
- Score de crédito interno baseado em histórico e saldo
- Open Finance: modelo de consentimento (criar, autorizar, revogar)

### Auditoria e Observabilidade
- Log de auditoria persistido em banco para todas as ações críticas
- Spring Actuator com endpoints: `health`, `info`, `metrics`, `prometheus`
- Prometheus configurado para scraping do `/actuator/prometheus`
- Grafana com datasource Prometheus provisionado automaticamente
- Health probes de liveness e readiness habilitados
- Tags de métricas: `application` e `environment`

---

## Arquitetura

O projeto segue arquitetura modular por domínio. Cada módulo é independente e tem sua própria estrutura interna:

```
com.spring.bank
├── account/        (contas, abertura, movimentação)
├── auth/           (OAuth2, JWT, clientes)
├── audit/          (log de auditoria)
├── boleto/         (emissão, pagamento, expiração)
├── card/           (cartões, fatura, parcelamento, cashback, chargeback)
├── compliance/     (KYC, AML, score de crédito)
├── investment/     (aplicação, resgate, catálogo de produtos, rendimento)
├── limit/          (limites operacionais por usuário)
├── loan/           (simulação, aprovação, parcelas)
├── notification/   (e-mail, SMS, push)
├── openfinance/    (consentimentos Open Finance)
├── pix/            (chaves, transferência, QR Code, saque, devolução)
├── savings/        (cofrinhos)
├── scheduling/     (pagamentos agendados)
├── shared/         (utilitários compartilhados)
├── transaction/    (criação, consulta, categorização, filtros)
├── transfer/       (TED entre contas)
├── twofa/          (OTP, dispositivos confiáveis)
└── user/           (cadastro, recuperação de senha)
```

---

## Banco de Dados

As migrações são gerenciadas pelo **Flyway**:

| Versão | Arquivo | Conteúdo |
|---|---|---|
| V1 | `V1__initial_schema.sql` | Tabelas base: users, clients, accounts, cards, card_bills, transactions, transfers, pix_keys, pix_contacts, savings_boxes |
| V2 | `V2__new_features.sql` | Novos módulos: KYC, AML, auditoria, OTP, dispositivos confiáveis, limites, cartão virtual, parcelamento, cashback, chargeback, PIX limits/QR/devolution/saque/agendado, boletos, empréstimos, produtos de investimento, Open Finance, pagamentos agendados |

Em um banco já existente, `baseline-on-migrate=true` garante que o Flyway execute apenas as migrações novas.

---

## Configuração e Execução

### Com Docker Compose

```bash
docker compose up --build
```

Sobe: API (8000), workers de transação e transferência, MySQL (3306), RabbitMQ
(5672 / UI 15672), Mailpit (1025 / UI 8025), Prometheus (9090) e Grafana (3001).
Os papéis de execução e o plano de separação dos demais módulos estão em
[`docs/microservices-migration.md`](docs/microservices-migration.md).

| Serviço | URL |
|---|---|
| API | http://localhost:8000 |
| Swagger UI | http://localhost:8000/swagger-ui |
| Actuator Health | http://localhost:8000/actuator/health |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (admin/admin) |
| RabbitMQ | http://localhost:15672 (guest/guest) |
| Mailpit | http://localhost:8025 |

### Local (sem Docker)

```bash
export DB_URL=jdbc:mysql://localhost:3306/nexwallet
export DB_USERNAME=root
export DB_PASSWORD=senha
export RABBITMQ_HOST=localhost
export JWT_SECRET=seu-secret-de-32-chars-ou-mais
export DEFAULT_CLIENT_SECRET=seu-client-secret
./mvnw spring-boot:run
```

---

## Autenticação

Implementação própria de OAuth 2.0 Client Credentials. Na inicialização, um client padrão é criado pelo `ClientSeeder`.

**1. Obter token:**
```http
POST /oauth/token
Authorization: Basic base64(client_id:client_secret)
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials
```

**2. Usar token nas requisições:**
```http
Authorization: Bearer <token>
```

**3. Criar novos clients (rota pública):**
```http
POST /clients
Content-Type: application/json

{ "clientId": "meu-app", "name": "Meu App" }
```

O secret é gerado pelo servidor e retornado apenas na criação.

---

## Testes

```bash
./mvnw test
```

Cobertura atual: **~82 testes unitários** cobrindo controllers (`@WebMvcTest`) e services (`@ExtendWith(MockitoExtension.class)`) de todos os módulos principais.

---

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SERVER_PORT` | `8000` | Porta da API |
| `DB_URL` | `jdbc:mysql://localhost:3306/nexwallet` | URL do MySQL |
| `DB_USERNAME` | `docker` | Usuário do MySQL |
| `DB_PASSWORD` | `password` | Senha do MySQL |
| `DDL_AUTO` | `validate` | Hibernate DDL mode |
| `JWT_SECRET` | — | Secret HMAC256 (mínimo 32 chars) |
| `JWT_EXPIRATION_SECONDS` | `3600` | Validade do token JWT |
| `DEFAULT_CLIENT_ID` | `nexwallet-app` | ID do client padrão |
| `DEFAULT_CLIENT_SECRET` | — | Secret do client padrão |
| `ENCRYPTION_KEY` | — | Chave AES-256 para criptografia de cartões (32 chars) |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `MAIL_HOST` | `localhost` | Host do servidor de e-mail |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Origins permitidas |
| `INVESTMENT_CRON` | `0 0 0 * * *` | Cron do rendimento diário |
| `CARD_BILL_CLOSE_CRON` | `0 0 0 1 * *` | Cron de fechamento de fatura |
| `SCHEDULING_CRON` | `0 0 8 * * *` | Cron de pagamentos agendados |
| `APP_ENVIRONMENT` | `local` | Tag de ambiente nas métricas |
| `NEXWALLET_RUNTIME_ROLE` | `api` | Processo: `api`, `transaction-worker` ou `transfer-worker` |

---

## Contribuindo

1. Fork o repositório
2. Crie uma branch: `git checkout -b feature/nova-feature`
3. Commit: `git commit -m 'feat: nova feature'`
4. Push: `git push origin feature/nova-feature`
5. Abra um Pull Request

---

## Licença

Distribuído sob a licença MIT.

> Feito com <3 por @FelipeRodrigues05
