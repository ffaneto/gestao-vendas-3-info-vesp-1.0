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
    
    // --- CORREÇÃO 1: O endpoint agora chama "/vendas" igual ao HTML ---
    // --- CORREÇÃO 2: Retorna apenas a LISTA pura, para o JavaScript calcular os totais ---
    @GetMapping("/vendas")
    public List<Lancamento> listarTodasVendas() {
        return repository.findAll();
    }

    // --- CORREÇÃO 3: O endpoint de salvar também chama "/vendas" (Método POST) ---
    @PostMapping("/vendas")
    public Lancamento salvar(@RequestBody Map<String, Object> payload) {
        
        String descricao = (String) payload.get("descricao");
        String tipo = (String) payload.get("tipo");
        
        // Tratamento de segurança para converter o valor
        BigDecimal valor = new BigDecimal(payload.get("valor").toString());

        LocalDate dataFinal = LocalDate.now();
        
        // Se vier data do front, usa ela. Se não, usa hoje.
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
    
    // Login simples (opcional, já que o HTML está fazendo validação local por enquanto)
    @PostMapping("/login")
    public boolean login(@RequestBody Map<String, String> credenciais) {
        String user = credenciais.get("user");
        String pass = credenciais.get("pass");
        return "admin".equals(user) && "comissao".equals(pass);
    }
    
 // No final do seu FinanceiroController.java

    @DeleteMapping("/vendas") // <--- ADICIONE O ("/vendas") AQUI
    public ResponseEntity<Void> apagarTudo() {
        repository.deleteAll(); // Apaga tudo do banco
        return ResponseEntity.noContent().build();
    }
}
