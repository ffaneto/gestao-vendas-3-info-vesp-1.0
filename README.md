# Sistema de Gestão Financeira - Formatura 3º Info

<div align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="src/main/resources/static/logoifpb-branca.png">
    <source media="(prefers-color-scheme: light)" srcset="src/main/resources/static/logoifpb-preta.png">
    <img alt="IFPB Logo" src="src/main/resources/static/logoifpb-preta.png" height="120">
  </picture>
  <br><br>
  <p><strong>Sistema de Gestão Financeira para a formatura do 3º ano do Curso Técnico em Informática Integrado ao Ensino Médio</strong></p>
  <p>IFPB Campus Itaporanga</p>
</div>

---

## Sobre o Projeto

Este projeto consiste em uma aplicação web para gestão financeira da comissão de formatura, com foco em organização, rastreabilidade e transparência das movimentações.

A plataforma permite registrar entradas e saídas, acompanhar o histórico completo de lançamentos e visualizar a evolução do caixa por meio de gráfico dinâmico.

O sistema foi concebido inicialmente em 26/02/2026 e evoluiu para uma versão em produção. Atualmente, o projeto está em uso como tema de Trabalho de Conclusão de Curso (TCC) no IFPB, no Curso Técnico em Informática.


https://github.com/user-attachments/assets/138125f8-b6b7-40b6-9410-a80b49664d06

## Ambiente em Produção

- **Hospedagem:** DigitalOcean
- **Banco em produção:** PostgreSQL
- **URL pública:** `https://gestao-vendas-formatura-ke5o4.ondigitalocean.app/`

## Funcionalidades

- **Dashboard em tempo real** - acompanhamento de saldo e indicadores principais
- **Gráfico de projeção** - variação visual entre lucro (verde) e prejuízo (vermelho)
- **Registro de vendas** - Trufas, Bolos e Açaí
- **Controle de gastos** - reposição de estoque e despesas diversas
- **Histórico completo** - busca por texto, filtro por data, filtro por ID e paginação
- **Edição rápida no histórico** - alteração de data diretamente na tabela
- **Desfazer lançamento** - exclusão individual por ID
- **Dois perfis de acesso** - Estudante e Comissão
- **Backup e restore** - exportação e importação de JSON pela interface
- **Reset de banco** - limpeza total dos dados com confirmação

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
| **Banco de Dados (produção)** | PostgreSQL |
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

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/vendas` | Lista todos os lançamentos |
| `POST` | `/api/vendas` | Registra novo lançamento |
| `DELETE` | `/api/vendas` | Remove todos os dados |
| `DELETE` | `/api/vendas/{id}` | Remove lançamento específico |
| `PATCH` | `/api/vendas/{id}/data` | Atualiza a data de um lançamento |
| `PATCH` | `/api/vendas/{id}/observacao` | Atualiza observação de um lançamento |
| `GET` | `/api/backup` | Baixa backup JSON |
| `POST` | `/api/restore` | Restaura backup JSON |
| `POST` | `/api/login` | Autenticação da comissão |
| `GET` | `/api/session` | Verifica sessão admin ativa |
| `POST` | `/api/logout` | Encerra sessão admin |

## Como Rodar Localmente

### Pré-requisitos

- **Java 21**
- **MySQL 8** (porta `3306`)
- **Maven**

### 1) Criar o banco local

```sql
CREATE DATABASE financeiro;
```

### 2) Configurar ambiente de desenvolvimento

Para executar localmente com MySQL, utilize o profile `dev` em `src/main/resources/application-dev.yml`.

Se quiser configurar via variáveis de ambiente, exemplo:

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

Na parte da produção, o projeto utiliza PostgreSQL e variáveis de ambiente no painel da DigitalOcean.

## Segurança

A autenticação da comissão é validada no backend por sessão HTTP. Dessa forma, credenciais administrativas não ficam hardcoded no frontend.

## Autor

**Francisco Figueiredo** [![GitHub](https://img.shields.io/badge/-ffaneto-181717?logo=github&logoColor=white&style=flat-square)](https://github.com/ffaneto)

- 3º ano - Informática Integrado ao Ensino Médio
- IFPB Campus Itaporanga
- Projeto atualmente utilizado como tema de TCC
