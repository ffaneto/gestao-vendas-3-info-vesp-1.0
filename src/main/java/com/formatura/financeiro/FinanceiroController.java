package com.formatura.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class FinanceiroController {

    @Autowired
    private LancamentoRepository repository;
    
    @PostMapping("/login")
    public boolean login(@RequestBody Map<String, String> credenciais) {
        String user = credenciais.get("user");
        String pass = credenciais.get("pass");
        return "admin".equals(user) && "comissao".equals(pass);
    }

    @GetMapping("/dados")
    public Map<String, Object> getDashboard() {
        List<Lancamento> lista = repository.findAll();

        // Ordenar por data Antigo - Novo
        lista.sort((a, b) -> {
            if (a.getDataLancamento() == null) return -1;
            if (b.getDataLancamento() == null) return 1;
            return a.getDataLancamento().compareTo(b.getDataLancamento());
        });

        // Calcular Saldo Total
        BigDecimal saldoTotal = BigDecimal.ZERO;
        for (Lancamento l : lista) {
            if (l.getValor() != null) {
                saldoTotal = saldoTotal.add(l.getValor());
            }
        }

        // Calcular Lucro Açaí
        BigDecimal lucroAcai = BigDecimal.ZERO;
        for (Lancamento l : lista) {
            if ("ACAI".equalsIgnoreCase(l.getTipo()) && l.getValor() != null) {
                lucroAcai = lucroAcai.add(l.getValor());
            }
        }

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("saldoTotal", saldoTotal);
        resposta.put("lucroAcai", lucroAcai);
        resposta.put("historico", lista);
        
        return resposta;
    }

    @PostMapping("/lancamento")
    public Lancamento salvar(@RequestBody Map<String, Object> payload) {
        
        String descricao = (String) payload.get("descricao");
        String tipo = (String) payload.get("tipo");
        
        BigDecimal valor = new BigDecimal(payload.get("valor").toString());

        LocalDate dataFinal = LocalDate.now();
        
        if (payload.get("data") != null && !payload.get("data").toString().isEmpty()) {
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
    
    @org.springframework.web.bind.annotation.DeleteMapping("/limpar")
    public void limparBanco() {
        repository.deleteAll(); // Isso apaga tudo do banco
    }
}