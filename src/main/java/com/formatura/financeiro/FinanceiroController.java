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
    
    // Endpoint de listagem. Mapeado para /vendas para bater com o frontend.
    // Retorna apenas a lista pura para o JavaScript calcular os totais.
    @GetMapping("/vendas")
    public List<Lancamento> listarTodasVendas() {
        return repository.findAll();
    }

    // Endpoint de criação. Mapeado para /vendas (POST).
    @PostMapping("/vendas")
    public Lancamento salvar(@RequestBody Map<String, Object> payload) {
        
        String descricao = (String) payload.get("descricao");
        String tipo = (String) payload.get("tipo");
        
        // Conversão do valor para BigDecimal para evitar erros de ponto flutuante.
        BigDecimal valor = new BigDecimal(payload.get("valor").toString());

        LocalDate dataFinal = LocalDate.now();
        
        // Prioriza a data enviada pelo frontend. Caso contrário, registra como data de hoje.
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
    
    // Endpoint de login básico (hardcoded para teste).
    @PostMapping("/login")
    public boolean login(@RequestBody Map<String, String> credenciais) {
        String user = credenciais.get("user");
        String pass = credenciais.get("pass");
        return "admin".equals(user) && "comissao".equals(pass);
    }
    
    // Endpoint para limpar todo o banco de dados (apenas para testes).
    @DeleteMapping("/vendas") 
    public ResponseEntity<Void> apagarTudo() {
        repository.deleteAll(); // Remove todos os registros
        return ResponseEntity.noContent().build();
    }
}