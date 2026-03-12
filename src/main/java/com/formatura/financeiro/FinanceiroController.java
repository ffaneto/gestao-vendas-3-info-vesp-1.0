package com.formatura.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class FinanceiroController {

    @Autowired
    private LancamentoRepository repository;
    
    @GetMapping("/vendas")
    public List<Lancamento> listarTodasVendas() {
        return repository.findAll();
    }

    @PostMapping("/vendas")
    public Lancamento salvar(@RequestBody Map<String, Object> payload) {
        
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

        return repository.save(novo);
    }
    
    @PostMapping("/login")
    public boolean login(@RequestBody Map<String, String> credenciais) {
        String user = credenciais.get("user");
        String pass = credenciais.get("pass");
        return "admin".equals(user) && "comissao".equals(pass);
    }
    
    @DeleteMapping("/vendas") 
    public ResponseEntity<Void> apagarTudo() {
        repository.deleteAll(); 
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/vendas/{id}")
    public ResponseEntity<Void> apagarPorId(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/vendas/{id}/data")
    public ResponseEntity<Lancamento> atualizarData(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
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
    public ResponseEntity<Lancamento> atualizarObservacao(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        return repository.findById(id).map(lancamento -> {
            String obs = body.get("observacao");
            lancamento.setObservacao(obs != null ? obs.trim() : null);
            return ResponseEntity.ok(repository.save(lancamento));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/backup")
    public ResponseEntity<List<Lancamento>> baixarBackupJson() {

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
            @RequestParam(defaultValue = "true") boolean limparAntes
    ) {
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

            // Cria uma nova instancia para ignorar id recebido no backup e evitar conflito.
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
}
