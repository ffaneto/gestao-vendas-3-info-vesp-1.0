package com.formatura.financeiro;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class FinanceiroController {

    private static final String ADMIN_SESSION_KEY = "ADMIN_AUTH";
    private static final String CONTA_ANA = "ANA";
    private static final String CONTA_BOLSINHA = "BOLSINHA";
    private static final String CONTA_IZABELLY = "IZABELLY";
    private static final String CONTA_BOLSINHA_IZABELLY = "BOLSINHA_IZABELLY";
    private static final String CONTA_PEDRO = "PEDRO";
    private static final String CONTA_ERIVANIA = "ERIVANIA";
    private static final String CONTA_BOLSINHA_SALGADOS = "BOLSINHA_SALGADOS";
    private static final String CONTA_FRANCISCO = "FRANCISCO";
    private static final String CONTA_DIGITAL = "CONTA_DIGITAL";
    private static final String CONTA_CAIXA_FISICO = "CAIXA_FISICO";
    private static final Set<String> CONTAS_ESPECIE = new HashSet<>(Set.of(
            CONTA_CAIXA_FISICO,
            CONTA_BOLSINHA,
            CONTA_BOLSINHA_IZABELLY,
            CONTA_BOLSINHA_SALGADOS
    ));
    private static final Set<String> CONTAS_PIX_BANCO = new HashSet<>(Set.of(
            CONTA_DIGITAL,
            CONTA_ANA,
            CONTA_IZABELLY,
            CONTA_FRANCISCO,
            CONTA_ERIVANIA
    ));

    @Autowired
    private LancamentoRepository repository;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.password-hash:}")
    private String adminPasswordHash;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/vendas")
    public List<Lancamento> listarTodasVendas() {
        return repository.findAll();
    }

    @PostMapping("/vendas")
    public ResponseEntity<?> salvar(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        if (!isAdminAutenticado(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Acesso negado."));
        }

        String descricao = payload.get("descricao") == null ? "" : payload.get("descricao").toString().trim();
        String tipo = payload.get("tipo") == null ? "" : payload.get("tipo").toString().trim().toUpperCase();

        if (descricao.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Descricao obrigatoria."));
        }
        if (tipo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Tipo obrigatorio."));
        }
        if (payload.get("valor") == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Valor obrigatorio."));
        }

        BigDecimal valor;
        try {
            valor = new BigDecimal(payload.get("valor").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Valor invalido."));
        }

        LocalDate dataFinal = LocalDate.now();
        if (payload.get("dataLancamento") != null && !payload.get("dataLancamento").toString().isEmpty()) {
            dataFinal = LocalDate.parse(payload.get("dataLancamento").toString());
        } else if (payload.get("data") != null && !payload.get("data").toString().isEmpty()) {
            dataFinal = LocalDate.parse(payload.get("data").toString());
        }

        String contaInformada = payload.get("contaDestino") == null ? null : payload.get("contaDestino").toString();
        String contaDestino;
        try {
            contaDestino = resolverContaDestino(tipo, descricao, contaInformada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }

        Lancamento novo = new Lancamento();
        novo.setDescricao(descricao);
        novo.setValor(valor);
        novo.setTipo(tipo);
        novo.setContaDestino(contaDestino);
        novo.setDataLancamento(dataFinal);
        novo.setHoraLancamento(LocalTime.now());

        return ResponseEntity.ok(repository.save(novo));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credenciais, HttpServletRequest request) {
        String user = credenciais.getOrDefault("user", "");
        String pass = credenciais.getOrDefault("pass", "");
        boolean ok = autenticarAdmin(user, pass);

        if (!ok) {
            request.getSession(true).removeAttribute(ADMIN_SESSION_KEY);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "erro", "Credenciais inválidas."));
        }

        request.getSession(true).setAttribute(ADMIN_SESSION_KEY, true);
        return ResponseEntity.ok(Map.of("ok", true, "role", "ADMIN"));
    }

    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> sessao(HttpServletRequest request) {
        return ResponseEntity.ok(Map.of("admin", isAdminAutenticado(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/vendas")
    public ResponseEntity<?> apagarTudo(HttpServletRequest request) {
        if (!isAdminAutenticado(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Acesso negado."));
        }
        repository.deleteAll();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/vendas/{id}")
    public ResponseEntity<?> apagarPorId(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdminAutenticado(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Acesso negado."));
        }
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/vendas/{id}/data")
    public ResponseEntity<?> atualizarData(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!isAdminAutenticado(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Acesso negado."));
        }

        return repository.findById(id).map(lancamento -> {
            String novaData = body.get("dataLancamento");
            if (novaData == null || novaData.isBlank()) {
                return ResponseEntity.badRequest().<Lancamento>build();
            }
            lancamento.setDataLancamento(LocalDate.parse(novaData));
            return ResponseEntity.ok(repository.save(lancamento));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/vendas/{id}/observacao")
    public ResponseEntity<?> atualizarObservacao(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!isAdminAutenticado(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Acesso negado."));
        }

        return repository.findById(id).map(lancamento -> {
            String obs = body.get("observacao");
            lancamento.setObservacao(obs != null ? obs.trim() : null);
            return ResponseEntity.ok(repository.save(lancamento));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/backup")
    public ResponseEntity<?> baixarBackupJson(HttpServletRequest request) {
        if (!isAdminAutenticado(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Acesso negado."));
        }

        List<Lancamento> todosOsLancamentos = repository.findAll();

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=backup_formatura.json");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(todosOsLancamentos);
    }

    @PostMapping("/restore")
    @Transactional
    public ResponseEntity<Map<String, Object>> restaurarBackup(
            @RequestBody List<Lancamento> lancamentos,
            @RequestParam(defaultValue = "true") boolean limparAntes,
            HttpServletRequest request
    ) {
        if (!isAdminAutenticado(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Acesso negado."));
        }

        if (lancamentos == null || lancamentos.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Arquivo de backup vazio."));
        }

        List<String> erros = new ArrayList<>();
        List<Lancamento> paraSalvar = new ArrayList<>();

        for (int i = 0; i < lancamentos.size(); i++) {
            Lancamento item = lancamentos.get(i);
            String prefixo = "Item " + (i + 1) + ": ";

            if (item == null) {
                erros.add(prefixo + "registro nulo.");
                continue;
            }
            if (item.getDescricao() == null || item.getDescricao().isBlank()) {
                erros.add(prefixo + "descricao obrigatoria.");
                continue;
            }
            if (item.getTipo() == null || item.getTipo().isBlank()) {
                erros.add(prefixo + "tipo obrigatorio.");
                continue;
            }
            if (item.getValor() == null) {
                erros.add(prefixo + "valor obrigatorio.");
                continue;
            }

            String tipoNormalizado = item.getTipo().trim().toUpperCase();
            String descricaoNormalizada = item.getDescricao().trim();
            String contaDestino;
            try {
                contaDestino = resolverContaDestino(tipoNormalizado, descricaoNormalizada, item.getContaDestino());
            } catch (IllegalArgumentException e) {
                erros.add(prefixo + e.getMessage());
                continue;
            }

            Lancamento novo = new Lancamento();
            novo.setDescricao(descricaoNormalizada);
            novo.setTipo(tipoNormalizado);
            novo.setContaDestino(contaDestino);
            novo.setValor(item.getValor());
            novo.setDataLancamento(item.getDataLancamento() != null ? item.getDataLancamento() : LocalDate.now());
            novo.setHoraLancamento(item.getHoraLancamento() != null ? item.getHoraLancamento() : LocalTime.now());
            novo.setObservacao(item.getObservacao());
            paraSalvar.add(novo);
        }

        if (!erros.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Backup invalido.", "detalhes", erros));
        }

        if (limparAntes) {
            repository.deleteAllInBatch();
        }

        List<Lancamento> salvos = repository.saveAll(paraSalvar);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("mensagem", "Backup restaurado com sucesso.");
        resposta.put("importados", salvos.size());
        resposta.put("limparAntes", limparAntes);

        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/fechamento-cego")
    public ResponseEntity<?> auditarFechamentoCego(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        if (!isAdminAutenticado(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Acesso negado."));
        }

        LocalDate hoje = LocalDate.now();
        LocalDate dataInicio = parseData(payload.get("dataInicio"), hoje);
        LocalDate dataFim = parseData(payload.get("dataFim"), dataInicio);
        if (dataFim.isBefore(dataInicio)) {
            return ResponseEntity.badRequest().body(Map.of("erro", "dataFim nao pode ser menor que dataInicio."));
        }

        Map<String, Object> bolsinhas = castMap(payload.get("bolsinhas"));
        BigDecimal bolsinhaDoces = parseDecimalSafe(bolsinhas.get("doces"));
        BigDecimal bolsinhaSalgados = parseDecimalSafe(bolsinhas.get("salgados"));
        BigDecimal bolsinhaAcai = parseDecimalSafe(bolsinhas.get("acai"));
        BigDecimal caixaFisicoContado = bolsinhaDoces.add(bolsinhaSalgados).add(bolsinhaAcai);
        BigDecimal pixConferido = parseDecimalSafe(
                payload.get("pixConferido") != null ? payload.get("pixConferido") : castMap(payload.get("pix")).get("conferido")
        );

        List<Map<String, Object>> itens = castListMap(payload.get("itens"));
        if (itens.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Informe ao menos um item de estoque para auditoria."));
        }

        Map<String, ProdutoRegra> regras = regrasProdutosFechamento();
        Map<String, BigDecimal> resultadoPorSetor = new LinkedHashMap<>();
        List<Map<String, Object>> porProduto = new ArrayList<>();

        List<Lancamento> lancamentos = repository.findAll();
        BigDecimal saldoEspecieRegistrado = BigDecimal.ZERO;
        BigDecimal vendasEspecieRegistradas = BigDecimal.ZERO;
        BigDecimal saldoPixRegistrado = BigDecimal.ZERO;
        BigDecimal vendasPixRegistradas = BigDecimal.ZERO;

        for (Lancamento l : lancamentos) {
            if (l == null || l.getDataLancamento() == null) continue;
            if (l.getDataLancamento().isBefore(dataInicio) || l.getDataLancamento().isAfter(dataFim)) continue;

            String conta = normalizarConta(l.getContaDestino());
            if (isContaEspecie(conta)) {
                BigDecimal v = l.getValor() == null ? BigDecimal.ZERO : l.getValor();
                saldoEspecieRegistrado = saldoEspecieRegistrado.add(v);
                if (isVendaPositiva(l)) {
                    vendasEspecieRegistradas = vendasEspecieRegistradas.add(v);
                }
            }

            if (isContaPixBanco(conta)) {
                BigDecimal v = l.getValor() == null ? BigDecimal.ZERO : l.getValor();
                saldoPixRegistrado = saldoPixRegistrado.add(v);
                if (isVendaPositiva(l)) {
                    vendasPixRegistradas = vendasPixRegistradas.add(v);
                }
            }
        }

        for (Map<String, Object> item : itens) {
            String produto = normalizarConta(item.get("produto"));
            ProdutoRegra regra = regras.get(produto);
            if (regra == null) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Produto invalido no fechamento: " + produto));
            }

            long comprado = parseLongSafe(
                    item.get("comprado"),
                    item.get("qtdComprada"),
                    item.get("quantidadeComprada")
            );
            long sobrou = parseLongSafe(
                    item.get("sobrou"),
                    item.get("qtdSobrando"),
                    item.get("quantidadeSobrando"),
                    item.get("sobra")
            );

            if (comprado < 0 || sobrou < 0) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Quantidades nao podem ser negativas."));
            }
            if (sobrou > comprado) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Sobra maior que compra para " + produto + "."));
            }

            long vendido = comprado - sobrou;
            BigDecimal qtdVendida = BigDecimal.valueOf(vendido);
            BigDecimal receita = regra.precoVenda.multiply(qtdVendida);
            BigDecimal custo = regra.precoCusto.multiply(qtdVendida);
            BigDecimal lucro = receita.subtract(custo);

            BigDecimal valorVendaRegistrada = somarVendasDoProduto(lancamentos, dataInicio, dataFim, produto);
            BigDecimal qtdRegistrada = regra.precoVenda.compareTo(BigDecimal.ZERO) > 0
                    ? valorVendaRegistrada.divide(regra.precoVenda, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal diferencaQtd = qtdRegistrada.subtract(qtdVendida);

            resultadoPorSetor.merge(regra.setor, lucro, BigDecimal::add);

            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("produto", produto);
            linha.put("setor", regra.setor);
            linha.put("comprado", comprado);
            linha.put("sobrou", sobrou);
            linha.put("vendido", vendido);
            linha.put("precoVenda", regra.precoVenda);
            linha.put("precoCusto", regra.precoCusto);
            linha.put("receita", receita);
            linha.put("custo", custo);
            linha.put("lucro", lucro);
            linha.put("qtdRegistrada", qtdRegistrada);
            linha.put("diferencaQtd", diferencaQtd);
            porProduto.add(linha);
        }

        BigDecimal diferencaCaixaVsSaldo = caixaFisicoContado.subtract(saldoEspecieRegistrado);
        BigDecimal diferencaCaixaVsVendas = caixaFisicoContado.subtract(vendasEspecieRegistradas);
        BigDecimal diferencaPixVsSaldo = pixConferido.subtract(saldoPixRegistrado);
        BigDecimal diferencaPixVsVendas = pixConferido.subtract(vendasPixRegistradas);

        String statusFisico = diferencaCaixaVsSaldo.compareTo(BigDecimal.ZERO) == 0 ? "OK" : "DIVERGENTE";
        String statusPix = diferencaPixVsSaldo.compareTo(BigDecimal.ZERO) == 0 ? "OK" : "DIVERGENTE";
        String statusFinal = ("OK".equals(statusFisico) && "OK".equals(statusPix)) ? "OK" : "DIVERGENTE";

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("periodo", Map.of("inicio", dataInicio, "fim", dataFim));
        resposta.put("bolsinhas", Map.of(
                "doces", bolsinhaDoces,
                "salgados", bolsinhaSalgados,
                "acai", bolsinhaAcai,
                "totalFisico", caixaFisicoContado
        ));
        resposta.put("saldoEspecieRegistrado", saldoEspecieRegistrado);
        resposta.put("vendasEspecieRegistradas", vendasEspecieRegistradas);
        resposta.put("diferencaCaixaVsSaldo", diferencaCaixaVsSaldo);
        resposta.put("diferencaCaixaVsVendas", diferencaCaixaVsVendas);
        resposta.put("pixConferido", pixConferido);
        resposta.put("saldoPixRegistrado", saldoPixRegistrado);
        resposta.put("vendasPixRegistradas", vendasPixRegistradas);
        resposta.put("diferencaPixVsSaldo", diferencaPixVsSaldo);
        resposta.put("diferencaPixVsVendas", diferencaPixVsVendas);
        resposta.put("statusFisico", statusFisico);
        resposta.put("statusPix", statusPix);
        resposta.put("statusFinal", statusFinal);
        resposta.put("status", statusFinal);
        resposta.put("resultadoPorSetor", resultadoPorSetor);
        resposta.put("resultadoPorProduto", porProduto);
        resposta.put("mensagem", "Fechamento cego calculado com auditoria fisica e conciliacao PIX.");

        return ResponseEntity.ok(resposta);
    }

    private Map<String, ProdutoRegra> regrasProdutosFechamento() {
        Map<String, ProdutoRegra> regras = new LinkedHashMap<>();
        regras.put("TRUFA", new ProdutoRegra("DOCES", bd("2.50"), bd("1.50")));
        regras.put("BOLO", new ProdutoRegra("DOCES", bd("3.50"), bd("2.50")));
        regras.put("EMPADA", new ProdutoRegra("SALGADOS", bd("4.50"), bd("3.00")));
        regras.put("ESFIHA", new ProdutoRegra("SALGADOS", bd("5.00"), bd("3.00")));
        return regras;
    }

    private BigDecimal somarVendasDoProduto(List<Lancamento> lancamentos, LocalDate inicio, LocalDate fim, String tipoProduto) {
        BigDecimal total = BigDecimal.ZERO;
        for (Lancamento l : lancamentos) {
            if (l == null || l.getDataLancamento() == null || l.getValor() == null) continue;
            if (l.getDataLancamento().isBefore(inicio) || l.getDataLancamento().isAfter(fim)) continue;
            if (!tipoProduto.equals(normalizarConta(l.getTipo()))) continue;
            if (l.getValor().compareTo(BigDecimal.ZERO) <= 0) continue;
            total = total.add(l.getValor());
        }
        return total;
    }

    private boolean isVendaPositiva(Lancamento l) {
        if (l == null || l.getValor() == null || l.getValor().compareTo(BigDecimal.ZERO) <= 0) return false;
        String tipo = normalizarConta(l.getTipo());
        return "TRUFA".equals(tipo)
                || "BOLO".equals(tipo)
                || "EMPADA".equals(tipo)
                || "ESFIHA".equals(tipo)
                || "ACAI".equals(tipo);
    }

    private boolean isContaEspecie(String contaNormalizada) {
        return CONTAS_ESPECIE.contains(contaNormalizada);
    }

    private boolean isContaPixBanco(String contaNormalizada) {
        return CONTAS_PIX_BANCO.contains(contaNormalizada);
    }

    private String normalizarConta(Object valor) {
        return valor == null ? "" : valor.toString().trim().toUpperCase();
    }

    private LocalDate parseData(Object valor, LocalDate fallback) {
        if (valor == null) return fallback;
        String texto = valor.toString().trim();
        if (texto.isBlank()) return fallback;
        return LocalDate.parse(texto);
    }

    private BigDecimal parseDecimalSafe(Object valor) {
        if (valor == null) return BigDecimal.ZERO;
        try {
            String texto = valor.toString().replace(",", ".").trim();
            if (texto.isBlank()) return BigDecimal.ZERO;
            return new BigDecimal(texto);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private long parseLongSafe(Object... valores) {
        for (Object v : valores) {
            if (v == null) continue;
            try {
                String texto = v.toString().replace(",", ".").trim();
                if (texto.isBlank()) continue;
                return new BigDecimal(texto).longValue();
            } catch (Exception ignored) {
                // tenta o proximo campo candidato
            }
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object valor) {
        if (valor instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castListMap(Object valor) {
        if (!(valor instanceof List<?> lista)) return new ArrayList<>();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : lista) {
            if (item instanceof Map<?, ?> map) {
                out.add((Map<String, Object>) map);
            }
        }
        return out;
    }

    private BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    private static class ProdutoRegra {
        private final String setor;
        private final BigDecimal precoVenda;
        private final BigDecimal precoCusto;

        private ProdutoRegra(String setor, BigDecimal precoVenda, BigDecimal precoCusto) {
            this.setor = setor;
            this.precoVenda = precoVenda;
            this.precoCusto = precoCusto;
        }
    }

    private boolean isAdminAutenticado(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(ADMIN_SESSION_KEY));
    }

    private boolean autenticarAdmin(String user, String pass) {
        if (!adminUsername.equals(user) || pass == null || pass.isBlank()) {
            return false;
        }

        if (adminPasswordHash != null && !adminPasswordHash.isBlank()) {
            return passwordEncoder.matches(pass, adminPasswordHash);
        }

        return adminPassword != null && !adminPassword.isBlank() && adminPassword.equals(pass);
    }

    private String resolverContaDestino(String tipo, String descricao, String contaInformada) {
        String tipoNormalizado = tipo == null ? "" : tipo.trim().toUpperCase();
        String contaNormalizada = canonicalizarConta(contaInformada);

        if (CONTA_DIGITAL.equals(contaNormalizada)
                || CONTA_CAIXA_FISICO.equals(contaNormalizada)
                || CONTA_PEDRO.equals(contaNormalizada)) {
            return contaNormalizada;
        }

        String contaForcadaPorTipo = contaPadraoPorTipo(tipoNormalizado);
        if (contaForcadaPorTipo != null) {
            if (!contaNormalizada.isBlank() && !contaForcadaPorTipo.equals(contaNormalizada)
                    && !isContaValida(contaNormalizada)) {
                throw new IllegalArgumentException("contaDestino inconsistente com o tipo " + tipoNormalizado + ".");
            }
            return contaForcadaPorTipo;
        }

        if (CONTA_DIGITAL.equals(contaNormalizada)
                || CONTA_CAIXA_FISICO.equals(contaNormalizada)
                || CONTA_PEDRO.equals(contaNormalizada)) {
            return contaNormalizada;
        }

        String descricaoNormalizada = normalizarTexto(descricao);
        if (descricaoNormalizada.contains("erivania")) return CONTA_DIGITAL;
        if (descricaoNormalizada.contains("bolsinha salgados")) return CONTA_CAIXA_FISICO;
        if (descricaoNormalizada.contains("francisco")) return CONTA_DIGITAL;
        if (descricaoNormalizada.contains("bolsinha izabelly")) return CONTA_CAIXA_FISICO;
        if (descricaoNormalizada.contains("izabelly") || descricaoNormalizada.contains("acai") || descricaoNormalizada.contains("complemento")) {
            return CONTA_DIGITAL;
        }
        if (descricaoNormalizada.contains("carne") || descricaoNormalizada.contains("carnes")) {
            return CONTA_PEDRO;
        }
        if (descricaoNormalizada.contains("bolsinha") || descricaoNormalizada.contains("especie")
                || descricaoNormalizada.contains("moeda") || descricaoNormalizada.contains("nota")) {
            return CONTA_CAIXA_FISICO;
        }

        return CONTA_DIGITAL;
    }

    private String contaPadraoPorTipo(String tipoNormalizado) {
        if ("ERIVANIA".equals(tipoNormalizado)) return CONTA_DIGITAL;
        if ("ACAI".equals(tipoNormalizado)) return CONTA_DIGITAL;
        if (tipoNormalizado.startsWith("CARNE")) return CONTA_PEDRO;
        if ("TRUFA".equals(tipoNormalizado) || "BOLO".equals(tipoNormalizado)
                || "TRUFA_COMPRA".equals(tipoNormalizado) || "BOLO_COMPRA".equals(tipoNormalizado)
                || "EMPADA".equals(tipoNormalizado) || "ESFIHA".equals(tipoNormalizado)
                || "EMPADA_COMPRA".equals(tipoNormalizado) || "ESFIHA_COMPRA".equals(tipoNormalizado)) {
            return CONTA_DIGITAL;
        }
        return null;
    }

    private String canonicalizarConta(String conta) {
        String contaNormalizada = conta == null ? "" : conta.trim().toUpperCase();
        if (contaNormalizada.isBlank()) return "";
        if (CONTA_PEDRO.equals(contaNormalizada)) return CONTA_PEDRO;
        if (CONTA_DIGITAL.equals(contaNormalizada)
                || CONTA_ANA.equals(contaNormalizada)
                || CONTA_IZABELLY.equals(contaNormalizada)
                || CONTA_FRANCISCO.equals(contaNormalizada)
                || CONTA_ERIVANIA.equals(contaNormalizada)) {
            return CONTA_DIGITAL;
        }
        if (CONTA_CAIXA_FISICO.equals(contaNormalizada)
                || CONTA_BOLSINHA.equals(contaNormalizada)
                || CONTA_BOLSINHA_IZABELLY.equals(contaNormalizada)
                || CONTA_BOLSINHA_SALGADOS.equals(contaNormalizada)) {
            return CONTA_CAIXA_FISICO;
        }
        return contaNormalizada;
    }

    private boolean isContaValida(String conta) {
        return CONTA_DIGITAL.equals(conta)
                || CONTA_CAIXA_FISICO.equals(conta)
                || CONTA_ANA.equals(conta)
                || CONTA_BOLSINHA.equals(conta)
                || CONTA_IZABELLY.equals(conta)
                || CONTA_BOLSINHA_IZABELLY.equals(conta)
                || CONTA_PEDRO.equals(conta)
                || CONTA_ERIVANIA.equals(conta)
                || CONTA_BOLSINHA_SALGADOS.equals(conta)
                || CONTA_FRANCISCO.equals(conta);
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }
}
