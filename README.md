# Sistema de Gestão Financeira Formatura 3º Info

<div align="center">
  <img src="src/main/resources/static/logoifpb.png" alt="IFPB Logo" height="80"/>
  <br><br>
  <p><strong>Sistema de Gestão Financeira Para a Formatura do Terceiro Ano do Curso de Informática Integrado ao Ensino Médio</strong></p>
  <p>IFPB Campus Itaporanga</p>
</div>

---

##  Sobre o Projeto

Sistema web para controle financeiro da comissão de formatura, permitindo registrar vendas, gastos, aportes e acompanhar a evolução do saldo em tempo real com gráfico

O projeto foi desenvolvido para melhoria da qualidade da gestão da comissão da formatura

Além de expandir meus conhecimentos na programação

## Funcionalidades

-  **Dashboard em tempo real** — saldo, lucro do açaí e gráfico de projeção
-  **Gráfico dinâmico** — muda de verde (lucro) para vermelho (prejuízo) automaticamente
-  **Registro de vendas** — Trufas, Bolos e Açaí
-  **Controle de gastos** — reposição de estoque e despesas diversas
-  **Histórico completo** — com busca por texto, filtro por data, filtro por ID e paginação
-  **Edição rápida no histórico** — alterar a data de um lançamento direto na tabela
-  **Desfazer lançamento** — exclusão individual por ID no histórico
-  **Dois perfis de acesso** — Estudante da turma e Comissão
-  **Backup e Restore** — exportar e importar dados em JSON pela interface
-  **Reset de banco** — limpar todos os dados com confirmação

## 🛠️ Tech Stack

<div align="left">
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/java/java-original.svg" height="40" alt="java" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg" height="40" alt="spring boot" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mysql/mysql-original.svg" height="40" alt="mysql" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/html5/html5-original.svg" height="40" alt="html5" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/css3/css3-original.svg" height="40" alt="css3" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/javascript/javascript-original.svg" height="40" alt="javascript" />
</div>

| Camada | Tecnologia |
|--------|-----------|
| **Backend** | Java 21 + Spring Boot 3.1.5 |
| **Banco de Dados** | MySQL 8 |
| **Frontend** | HTML5 + CSS3 + JavaScript |
| **Build** | Maven |

## Estrutura do Projeto

```
financeiro/
├── src/main/java/com/formatura/financeiro/
│   ├── FinanceiroApplication.java    # Classe principal Spring Boot
│   ├── FinanceiroController.java     # REST Controller (API)
│   ├── Lancamento.java               # Entidade JPA
│   └── LancamentoRepository.java     # Repository Spring Data
├── src/main/resources/
│   ├── application.yml               # Configurações (porta, banco)
│   └── static/
│       └── index.html                # Interface completa (SPA)
└── pom.xml                           # Dependências Maven
```

## Endpoints da API

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/vendas` | Listar todos os lançamentos |
| `POST` | `/api/vendas` | Registrar novo lançamento |
| `DELETE` | `/api/vendas` | Apagar todos os dados |
| `DELETE` | `/api/vendas/{id}` | Apagar um lançamento específico |
| `PATCH` | `/api/vendas/{id}/data` | Atualizar apenas a data de um lançamento |
| `GET` | `/api/backup` | Baixar backup em JSON |
| `POST` | `/api/restore` | Restaurar backup JSON (`limparAntes=true|false`) |
| `POST` | `/api/login` | Autenticação local |

## Como Rodar

### Pré-requisitos

- **Java 21** 
- **MySQL 8** porta `3306`
- **Maven** 

### Criar o banco de dados

```sql
CREATE DATABASE db_formatura;
```

### Configurar credenciais

Edite `src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_formatura?useTimezone=true&serverTimezone=UTC
    username: root
    password: 'SUA_SENHA'
```

### Executar o projeto

```bash
# Windows (PowerShell)
.\mvnw.cmd spring-boot:run

# Git Bash / Linux / macOS
./mvnw spring-boot:run
```

### Acessar no navegador

```
http://localhost:8080
```

Usuário: admin , Senha: comissao

## Autor

**Francisco Figueiredo** [![GitHub](https://img.shields.io/badge/-ffaneto-181717?logo=github&logoColor=white&style=flat-square)](https://github.com/ffaneto)

- 3º Ano Informática Integrado ao Ensino Médio
- IFPB Campus Itaporanga

