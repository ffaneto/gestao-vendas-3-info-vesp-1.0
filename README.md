
Sistema de Gestão Financeira Para a Formatura do Terceiro Ano do Curso de Infórmatica Integrado ao Ensino Médio IFPB - Campus Itaporanga

# 💰 Sistema de Gestão Financeira — Formatura 3º Info

<div align="center">
  <img src="src/main/resources/static/logoifpb.png" alt="IFPB Logo" height="80"/>
  <br><br>
  <p><strong>Sistema de Gestão Financeira Para a Formatura do Terceiro Ano do Curso de Informática Integrado ao Ensino Médio</strong></p>
  <p>IFPB — Campus Itaporanga</p>
</div>

---

## 📋 Sobre o Projeto

Sistema web para controle financeiro da comissão de formatura, permitindo registrar vendas (trufas, bolos, açaí), gastos, aportes e acompanhar a evolução do saldo em tempo real com gráficos dinâmicos.

O projeto foi desenvolvido como ferramenta interna da turma do 3º ano de Informática do IFPB Campus Itaporanga para gerenciar de forma transparente os recursos arrecadados para a formatura.

## ✨ Funcionalidades

- 📊 **Dashboard em tempo real** — saldo, lucro do açaí e gráfico de projeção
- 📈 **Gráfico dinâmico** — muda de verde (lucro) para vermelho (prejuízo) automaticamente
- 🛒 **Registro de vendas** — Trufas (R$ 2,50), Bolos (R$ 3,50) e Açaí (valor livre)
- 💸 **Controle de gastos** — reposição de estoque e despesas diversas
- 📜 **Histórico completo** — com busca por texto, filtro por data e paginação
- 👥 **Dois perfis de acesso** — Estudante (visualização) e Comissão (gestão completa)
- 💾 **Backup e Restore** — exportar e importar dados em JSON pela interface
- 🗑️ **Reset de banco** — limpar todos os dados com confirmação

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
| **Banco de Dados** | MySQL 8 (`db_formatura`) |
| **Frontend** | HTML5 + CSS3 + JavaScript (vanilla) |
| **Gráficos** | Chart.js |
| **Alertas** | SweetAlert2 |
| **Ícones** | Font Awesome 6 |
| **Build** | Maven |

## 📁 Estrutura do Projeto

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

## 🔌 Endpoints da API

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/vendas` | Listar todos os lançamentos |
| `POST` | `/api/vendas` | Registrar novo lançamento |
| `DELETE` | `/api/vendas` | Apagar todos os dados |
| `GET` | `/api/backup` | Baixar backup em JSON |
| `POST` | `/api/restore` | Restaurar backup JSON |
| `POST` | `/api/login` | Autenticação local |

## 🚀 Como Rodar

### Pré-requisitos

- **Java 21** instalado
- **MySQL 8** rodando na porta `3306`
- **Maven** (ou usar o wrapper `mvnw` incluso)

### 1. Criar o banco de dados

```sql
CREATE DATABASE db_formatura;
```

### 2. Configurar credenciais

Edite `src/main/resources/application.yml` com seu usuário e senha do MySQL:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_formatura?useTimezone=true&serverTimezone=UTC
    username: root
    password: 'SUA_SENHA'
```

### 3. Executar o projeto

```bash
./mvnw spring-boot:run
```

### 4. Acessar no navegador

```
http://localhost:8080
```

- **Estudante**: clique em "Estudante" na tela inicial
- **Comissão**: clique em "Comissão" → usuário: `admin` / senha: `comissao`

## 📸 Preview

| Tela de Login | Dashboard Admin |
|:---:|:---:|
| Dois perfis: Estudante e Comissão | Vendas, gastos, gráfico e histórico |

## 👨‍💻 Autor

**Francisco Figueiredo**
- 🎓 3º Ano — Informática Integrado ao Ensino Médio
- 🏫 IFPB Campus Itaporanga

---

<div align="center">
  <sub>Feito com ☕ e Java para a formatura do 3º Info — IFPB Itaporanga</sub>
</div>

>>>>>>> accd74f (Melhoria do README)
