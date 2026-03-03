package com.formatura.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/dados")
    public Map<String, Object> getDashboard() {
        return Map.of(
            "saldoTotal", repository.calcularSaldoTotal(),
            "lucroAcai", repository.calcularLucroAcai(),
            "historico", repository.findTop50ByOrderByDataLancamentoDescHoraLancamentoDesc()
        );
    }

    @PostMapping("/lancamento")
    public Lancamento salvar(@RequestBody Lancamento lancamento) {
        lancamento.setDataLancamento(LocalDate.now());
        lancamento.setHoraLancamento(LocalTime.now());
        return repository.save(lancamento);
    }
    
    @org.springframework.web.bind.annotation.DeleteMapping("/limpar")
    public void limparBanco() {
        repository.deleteAll(); // Isso apaga tudo do banco
    }
}