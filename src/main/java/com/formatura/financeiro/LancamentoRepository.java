package com.formatura.financeiro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {
    
    // Pega os últimos 50 registros (do mais novo para o mais velho)
    List<Lancamento> findTop50ByOrderByDataLancamentoDescHoraLancamentoDesc();

    // Soma tudo pra o Saldo Geral
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l")
    BigDecimal calcularSaldoTotal();
    
    // Soma só o lucro do Açaí
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l WHERE l.tipo = 'ACAI'")
    BigDecimal calcularLucroAcai();
}