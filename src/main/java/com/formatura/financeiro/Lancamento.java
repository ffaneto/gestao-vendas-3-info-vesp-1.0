package com.formatura.financeiro;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "tb_lancamento")
public class Lancamento {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String descricao;
    private BigDecimal valor;
    private String tipo;
    private LocalDate dataLancamento;
    private LocalTime horaLancamento;
    private String observacao;
    private String contaDestino;

    public Lancamento() {} 
    
    public Lancamento(String descricao, BigDecimal valor, String tipo, LocalDate data) {
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.dataLancamento = (data != null) ? data : LocalDate.now();
        this.horaLancamento = LocalTime.now();
    }

    public Long getId() { 
    	return id; 
    }
    
    public void setId(Long id){ 
    	this.id = id; 
    }
    
    public String getDescricao() {
    	return descricao;
    }
    
    public void setDescricao(String descricao) { 
    	this.descricao = descricao;
    }
    
    public BigDecimal getValor() { 
    	return valor; 
    }
    
    public void setValor(BigDecimal valor) { 
    	this.valor = valor; 
    }
    
    public String getTipo() { 
    	return tipo; 
    }
    
    public void setTipo(String tipo) { 
    	this.tipo = tipo; 
    }
    
    public LocalDate getDataLancamento() { 
    	return dataLancamento; 
    }
    
    public void setDataLancamento(LocalDate dataLancamento) { 
    	this.dataLancamento = dataLancamento; 
    }
    
    public LocalTime getHoraLancamento() {
    	return horaLancamento; 
    }
    
    public void setHoraLancamento(LocalTime horaLancamento) { 
    	this.horaLancamento = horaLancamento; 
    }

    public String getObservacao() {
    	return observacao;
    }

    public void setObservacao(String observacao) {
    	this.observacao = observacao;
    }

    public String getContaDestino() {
        return contaDestino;
    }

    public void setContaDestino(String contaDestino) {
        this.contaDestino = contaDestino == null ? null : contaDestino.trim().toUpperCase();
    }
}