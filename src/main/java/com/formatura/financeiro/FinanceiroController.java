package com.formatura.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
}

