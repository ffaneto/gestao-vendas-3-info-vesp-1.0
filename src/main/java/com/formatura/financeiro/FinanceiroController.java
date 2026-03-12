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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FinanceiroController {

    private static final String ADMIN_SESSION_KEY = "ADMIN_AUTH";

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

        String descricao = (String) payload.get("descricao");
        String tipo = (String) payload.get("tipo");
        BigDecimal valor = new BigDecimal(payload.get("valor").toString());

        LocalDate dataFinal = LocalDate.now();
        if (payload.get("dataLancamento") != null && !payload.get("dataLancamento").toString().isEmpty()) {
            dataFinal = LocalDate.parse(payload.get("dataLancamento").toString());
        } else if (payload.get("data") != null && !payload.get("data").toString().isEmpty()) {
            dataFinal = LocalDate.parse(payload.get("data").toString());
        }

        Lancamento novo = new Lancamento();
        novo.setDescricao(descricao);
        novo.setValor(valor);
        novo.setTipo(tipo);
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

            Lancamento novo = new Lancamento();
            novo.setDescricao(item.getDescricao().trim());
            novo.setTipo(item.getTipo().trim());
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
}
