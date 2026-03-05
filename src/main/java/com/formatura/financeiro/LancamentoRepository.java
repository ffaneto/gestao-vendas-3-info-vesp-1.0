package com.formatura.financeiro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {
    
    List<Lancamento> findTop50ByOrderByDataLancamentoDescHoraLancamentoDesc();

    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l")
    BigDecimal calcularSaldoTotal();
    
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l WHERE l.tipo = 'ACAI'")
    BigDecimal calcularLucroAcai();
}