package com.formatura.financeiro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {
    
    // Método auxiliar para pegar os últimos 50 lançamentos, ordenados do mais recente para o mais antigo.
    List<Lancamento> findTop50ByOrderByDataLancamentoDescHoraLancamentoDesc();

    // Query JPQL para calcular o saldo total somando todos os lançamentos.
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l")
    BigDecimal calcularSaldoTotal();
    
    // Query JPQL para somar apenas os lançamentos do tipo 'ACAI'.
    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l WHERE l.tipo = 'ACAI'")
    BigDecimal calcularLucroAcai();
}