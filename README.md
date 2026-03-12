# Sistema de Gestão Financeira - Formatura 3o Info

<div align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="src/main/resources/static/logoifpb-branca.png">
    <source media="(prefers-color-scheme: light)" srcset="src/main/resources/static/logoifpb-preta.png">
    <img alt="IFPB Logo" src="src/main/resources/static/logoifpb-preta.png" height="120">
  </picture>
  <br><br>
  <p><strong>Sistema de Gestao Financeira para a formatura do 3o ano do Curso Tecnico em Informatica Integrado ao Ensino Medio</strong></p>
  <p>IFPB Campus Itaporanga</p>
</div>

---

## Sobre o Projeto

Este projeto consiste em uma aplicacao web para gestao financeira da comissao de formatura, com foco em organizacao, rastreabilidade e transparencia das movimentacoes.

A plataforma permite registrar entradas e saidas, acompanhar o historico completo de lancamentos e visualizar a evolucao do caixa por meio de grafico dinamico.

O sistema foi concebido inicialmente em 26/02/2026 e evoluiu para uma versao em producao. Atualmente, o projeto esta em uso como tema de Trabalho de Conclusao de Curso (TCC) no IFPB, no Curso Tecnico em Informatica.

## Ambiente em Producao

- **Hospedagem:** DigitalOcean
- **Banco em producao:** PostgreSQL
- **URL publica:** `https://gestao-vendas-formatura-ke5o4.ondigitalocean.app/`

## Funcionalidades

- **Dashboard em tempo real** - acompanhamento de saldo e indicadores principais
- **Grafico de projecao** - variacao visual entre lucro (verde) e prejuizo (vermelho)
- **Registro de vendas** - Trufas, Bolos e Acai
- **Controle de gastos** - reposicao de estoque e despesas diversas
- **Historico completo** - busca por texto, filtro por data, filtro por ID e paginacao
- **Edicao rapida no historico** - alteracao de data diretamente na tabela
- **Desfazer lancamento** - exclusao individual por ID
- **Dois perfis de acesso** - Estudante e Comissao
- **Backup e restore** - exportacao e importacao de JSON pela interface
- **Reset de banco** - limpeza total dos dados com confirmacao

## Tech Stack

<div align="left">
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/java/java-original.svg" height="40" alt="java" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg" height="40" alt="spring boot" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/postgresql/postgresql-original.svg" height="40" alt="postgresql" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mysql/mysql-original.svg" height="40" alt="mysql" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/html5/html5-original.svg" height="40" alt="html5" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/css3/css3-original.svg" height="40" alt="css3" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/javascript/javascript-original.svg" height="40" alt="javascript" />
</div>

| Camada | Tecnologia |
|--------|------------|
| **Backend** | Java 21 + Spring Boot 3.1.5 |
| **Banco de Dados (producao)** | PostgreSQL |
| **Banco de Dados (local/dev)** | MySQL 8 |
| **Frontend** | HTML5 + CSS3 + JavaScript |
| **Build** | Maven |
| **Infraestrutura** | DigitalOcean |

## Estrutura do Projeto

```text
financeiro/
├── src/main/java/com/formatura/financeiro/
│   ├── FinanceiroApplication.java
│   ├── FinanceiroController.java
│   ├── Lancamento.java
│   └── LancamentoRepository.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── static/
│       ├── index.html
│       ├── app.css
│       └── app.js
└── pom.xml
```

## Endpoints da API

| Metodo | Rota | Descricao |
|--------|------|-----------|
| `GET` | `/api/vendas` | Lista todos os lancamentos |
| `POST` | `/api/vendas` | Registra novo lancamento |
| `DELETE` | `/api/vendas` | Remove todos os dados |
| `DELETE` | `/api/vendas/{id}` | Remove lancamento especifico |
| `PATCH` | `/api/vendas/{id}/data` | Atualiza a data de um lancamento |
| `PATCH` | `/api/vendas/{id}/observacao` | Atualiza observacao de um lancamento |
| `GET` | `/api/backup` | Baixa backup JSON |
| `POST` | `/api/restore` | Restaura backup JSON |
| `POST` | `/api/login` | Autenticacao da comissao |
| `GET` | `/api/session` | Verifica sessao admin ativa |
| `POST` | `/api/logout` | Encerra sessao admin |

## Como Rodar Localmente

### Pre-requisitos

- **Java 21**
- **MySQL 8** (porta `3306`)
- **Maven**

### 1) Criar o banco local

```sql
CREATE DATABASE financeiro;
```

### 2) Configurar ambiente de desenvolvimento

Para executar localmente com MySQL, utilize o profile `dev` em `src/main/resources/application-dev.yml`.

Se quiser configurar via variaveis de ambiente, exemplo:

```bash
SPRING_PROFILES_ACTIVE=dev
ADMIN_USERNAME=admin
ADMIN_PASSWORD=comissao
```

### 3) Executar o projeto

```bash
# Windows (PowerShell)
.\mvnw.cmd spring-boot:run

# Git Bash / Linux / macOS
./mvnw spring-boot:run
```

### 4) Acessar no navegador

```text
http://localhost:8080
```

## Deploy (DigitalOcean)

Em producao, o projeto utiliza PostgreSQL e variaveis de ambiente no painel da DigitalOcean.

Variaveis principais:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<porta>/<database>
SPRING_DATASOURCE_USERNAME=<usuario>
SPRING_DATASOURCE_PASSWORD=<senha>

ADMIN_USERNAME=admin
ADMIN_PASSWORD=<senha_forte>
CORS_ALLOWED_ORIGIN=https://gestao-vendas-formatura-ke5o4.ondigitalocean.app
SESSION_COOKIE_SECURE=true
```

> Opcional (mais seguro): use `ADMIN_PASSWORD_HASH` (BCrypt) e deixe `ADMIN_PASSWORD` vazio.

## Seguranca

A autenticacao da comissao e validada no backend por sessao HTTP. Dessa forma, credenciais administrativas nao ficam hardcoded no frontend.

## Autor

**Francisco Figueiredo** [![GitHub](https://img.shields.io/badge/-ffaneto-181717?logo=github&logoColor=white&style=flat-square)](https://github.com/ffaneto)

- 3o ano - Informatica Integrado ao Ensino Medio
- IFPB Campus Itaporanga
- Projeto atualmente utilizado como tema de TCC
