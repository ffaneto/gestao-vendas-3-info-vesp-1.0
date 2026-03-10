const API_URL = "/api/vendas";
const BACKUP_URL = "/api/backup";
const BACKUP_URL_FALLBACK = `${API_URL}/backup`;
const RESTORE_URL = "/api/restore";
let chartInstance = null;
let historicoOriginal = [];
let historicoFiltrado = [];
let historicoPaginaAtual = 1;
const historicoItensPorPagina = 15;
let isAdminLogado = false;

window.onload = () => {
    configurarEventos();
    const hoje = new Date().toISOString().split('T')[0];
    document.querySelectorAll('input[type="date"]').forEach(el => el.value = hoje);
    document.getElementById('data-global').value = hoje;
    const usuarioLogado = localStorage.getItem("usuario_logado");
    if(usuarioLogado === "ADMIN") iniciarSistema(true);
    else if(usuarioLogado === "ESTUDANTE") iniciarSistema(false);
};

function configurarEventos() {
    vincularCliqueComTeclado(document.getElementById('login-estudante'), () => entrarComo('ESTUDANTE'));
    vincularCliqueComTeclado(document.getElementById('login-comissao'), mostrarFormLogin);

    document.getElementById('btn-login')?.addEventListener('click', fazerLogin);
    document.getElementById('btn-cancelar-login')?.addEventListener('click', cancelarLogin);
    document.getElementById('login-pass')?.addEventListener('keydown', verificarEnter);
    document.getElementById('btn-sair')?.addEventListener('click', sair);

    document.getElementById('btn-aporte')?.addEventListener('click', () => lancarGenerico('SALDO_INICIAL', 'valor-aporte', 'Aporte', 'data-aporte'));
    document.getElementById('btn-erivania')?.addEventListener('click', () => lancarGenerico('ERIVANIA', 'valor-erivania', 'Aporte Erivânia', 'data-erivania'));
    document.getElementById('btn-vender-trufa')?.addEventListener('click', () => venderQtd('TRUFA', 2.50, 'qtd-trufa', 'data-trufa'));
    document.getElementById('btn-vender-bolo')?.addEventListener('click', () => venderQtd('BOLO', 3.50, 'qtd-bolo', 'data-bolo'));
    document.getElementById('btn-vender-acai')?.addEventListener('click', () => lancarGenerico('ACAI', 'valor-acai', 'Venda Açaí', 'data-acai'));
    document.getElementById('btn-pagar-trufa')?.addEventListener('click', () => lancarGastoQtd('TRUFA_COMPRA', 1.50, 'qtd-trufa-compra', 'data-trufa-compra'));
    document.getElementById('btn-pagar-bolo')?.addEventListener('click', () => lancarGastoQtd('BOLO_COMPRA', 2.50, 'qtd-bolo-compra', 'data-bolo-compra'));
    document.getElementById('btn-lancar-gasto')?.addEventListener('click', lancarGastoGenerico);
    document.getElementById('btn-resetar')?.addEventListener('click', resetarBanco);

    document.getElementById('btn-importar-backup')?.addEventListener('click', () => document.getElementById('arquivo-backup')?.click());
    document.getElementById('btn-baixar-backup')?.addEventListener('click', baixarBackup);
    document.getElementById('btn-data-global')?.addEventListener('click', () => {
        try { document.getElementById('data-global')?.showPicker(); } catch (_) { document.getElementById('data-global')?.focus(); }
    });
    document.getElementById('data-global')?.addEventListener('change', (event) => sincronizarDatas(event.target.value));
    document.getElementById('arquivo-backup')?.addEventListener('change', restaurarBackupJson);

    document.getElementById('filtro-historico')?.addEventListener('input', () => aplicarFiltrosHistorico());
    document.getElementById('data-inicio-historico')?.addEventListener('change', () => aplicarFiltrosHistorico());
    document.getElementById('data-fim-historico')?.addEventListener('change', () => aplicarFiltrosHistorico());
    document.getElementById('filtro-id-historico')?.addEventListener('input', () => aplicarFiltrosHistorico());
    document.getElementById('btn-limpar-filtros')?.addEventListener('click', limparFiltrosHistorico);
    document.getElementById('btn-pagina-anterior')?.addEventListener('click', paginaAnteriorHistorico);
    document.getElementById('btn-pagina-proxima')?.addEventListener('click', paginaProximaHistorico);
}

function vincularCliqueComTeclado(elemento, handler) {
    if (!elemento) return;
    elemento.addEventListener('click', handler);
    elemento.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            handler();
        }
    });
}

function sincronizarDatas(valor) {
    const idsLancamento = [
        'data-aporte', 'data-erivania', 'data-trufa', 'data-bolo', 'data-acai',
        'data-trufa-compra', 'data-bolo-compra', 'data-outros'
    ];
    idsLancamento.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = valor;
    });
}

async function carregarDados() {
    try {
        const response = await fetch(API_URL);
        if (!response.ok) throw new Error('Servidor offline');
        const data = await response.json();

        const saldo = data.filter(i => i.tipo !== 'ERIVANIA').reduce((acc, i) => acc + i.valor, 0);
        const lucroAcai = data.filter(i => i.tipo === 'ACAI').reduce((acc, i) => acc + i.valor, 0);
        const saldoErivania = data.filter(i => i.tipo === 'ERIVANIA').reduce((acc, i) => acc + i.valor, 0);
        const caixaTotal = saldo + saldoErivania;

        document.getElementById('saldo').innerText = fmtMoeda(saldo);
        document.getElementById('lucro-acai').innerText = fmtMoeda(lucroAcai);
        document.getElementById('saldo-erivania').innerText = fmtMoeda(saldoErivania);

        document.getElementById('saldo-total').innerText = fmtMoeda(caixaTotal);
        document.getElementById('est-saldo-ana').innerText = fmtMoeda(saldo);
        document.getElementById('est-lucro-acai').innerText = fmtMoeda(lucroAcai);
        document.getElementById('est-saldo-erivania').innerText = fmtMoeda(saldoErivania);

        renderizarHistorico(data);
        renderizarGrafico(data);
    } catch (error) {
        console.error(error);
        Swal.fire({icon:'error', title:'Erro', text:'O Java está rodando?', background:'#222', color:'#fff'});
    }
}

async function enviarLancamento(payload) {
    try {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (response.ok) {
            carregarDados();
            Swal.fire({toast: true, position: 'top-end', icon: 'success', title: 'Salvo no MySQL!', showConfirmButton: false, timer: 1500});
        }
    } catch (error) {
        erro("Erro ao conectar com o Java.");
    }
}

async function resetarBanco() {
    Swal.fire({
        title: 'Apagar?',
        text: "Vai apagar TODOS os dados do BD",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#8A00C4',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sim',
        background: '#222',
        color: '#fff'
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                const response = await fetch(API_URL, { method: 'DELETE' });

                if (response.ok) {
                    Swal.fire({title: 'Apagado!', text: 'Banco de dados reiniciado.', icon: 'success', background:'#222', color:'#fff'});
                    carregarDados();
                } else {
                    erro("Erro ao tentar resetar o banco.");
                }
            } catch (e) {
                erro("Erro de conexão.");
            }
        }
    })
}

async function baixarBackup() {
    try {
        let response = await fetch(BACKUP_URL);
        if (!response.ok && response.status === 404) {
            response = await fetch(BACKUP_URL_FALLBACK);
        }
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const blob = await response.blob();
        const disposition = response.headers.get('content-disposition') || '';
        const match = disposition.match(/filename="?([^\"]+)"?/i);
        const fileName = match && match[1] ? match[1] : 'backup_formatura.json';

        const blobUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = blobUrl;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(blobUrl);
    } catch (error) {
        Swal.fire({
            icon:'error',
            title:'Erro no backup',
            text:`Nao foi possivel baixar o arquivo agora (${error.message}).`,
            background:'#222',
            color:'#fff'
        });
    }
}

async function restaurarBackupJson() {
    const arquivoInput = document.getElementById('arquivo-backup');
    const arquivo = arquivoInput?.files?.[0];

    if (!arquivo) {
        return erro('Selecione um arquivo JSON para restaurar.');
    }

    const confirmacao = await Swal.fire({
        title: 'Restaurar Backup?',
        html: `<span style="color:#ccc">Arquivo: <b style="color:#fff">${arquivo.name}</b></span>`,
        icon: 'question',
        showCancelButton: true,
        showDenyButton: true,
        confirmButtonText: 'Limpar e restaurar',
        denyButtonText: 'Apenas adicionar',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#00bcd4',
        denyButtonColor: '#ff9800',
        background: '#222',
        color: '#fff'
    });

    if (confirmacao.isDismissed) { arquivoInput.value = ''; return; }
    const limparAntes = confirmacao.isConfirmed;

    try {
        const texto = await arquivo.text();
        const parsed = JSON.parse(texto);
        const lancamentos = Array.isArray(parsed) ? parsed : parsed?.lancamentos;

        if (!Array.isArray(lancamentos) || lancamentos.length === 0) {
            return erro('Arquivo invalido. Esperado uma lista de lancamentos.');
        }

        const response = await fetch(`${RESTORE_URL}?limparAntes=${limparAntes}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(lancamentos)
        });

        let payload = {};
        try { payload = await response.json(); } catch (_) { payload = {}; }

        if (!response.ok) {
            const detalhes = Array.isArray(payload?.detalhes) ? payload.detalhes.slice(0, 3).join(' | ') : '';
            throw new Error(`${payload?.erro || `HTTP ${response.status}`}${detalhes ? ` - ${detalhes}` : ''}`);
        }

        Swal.fire({
            icon: 'success',
            title: 'Restore concluido',
            text: `${payload?.importados || lancamentos.length} registros importados.`,
            background: '#222',
            color: '#fff'
        });
        arquivoInput.value = '';
        carregarDados();
    } catch (error) {
        Swal.fire({
            icon:'error',
            title:'Erro na restauracao',
            text: `Nao foi possivel restaurar (${error.message}).`,
            background:'#222',
            color:'#fff'
        });
    }
}

function mostrarFormLogin() { document.getElementById('login-overlay').style.display = 'none'; document.getElementById('form-login-container').style.display = 'block'; document.getElementById('login-user').focus(); }
function cancelarLogin() { document.getElementById('form-login-container').style.display = 'none'; document.getElementById('login-overlay').style.display = 'flex'; }
function verificarEnter(event) { if (event.key === 'Enter') fazerLogin(); }
function entrarComo(tipo) {
    const tipoNormalizado = tipo === 'ADMIN' ? 'ADMIN' : 'ESTUDANTE';
    localStorage.setItem("usuario_logado", tipoNormalizado);
    iniciarSistema(tipoNormalizado === 'ADMIN');
}

function fazerLogin() {
    const u = document.getElementById('login-user').value;
    const p = document.getElementById('login-pass').value;
    if(u === 'admin' && p === 'comissao') {
        localStorage.setItem("usuario_logado", "ADMIN");
        iniciarSistema(true);
    } else {
        Swal.fire({
            icon:'error',
            title:'Login Errado',
            text:'Tente Novamente',
            background:'#222',
            zIndex: 20001
        });
    }
}

function iniciarSistema(isAdmin) {
    isAdminLogado = isAdmin;
    document.getElementById('login-overlay').style.display = 'none';
    document.getElementById('form-login-container').style.display = 'none';
    document.getElementById('app').style.display = 'block';
    document.querySelectorAll('.admin-only').forEach(el => {
        if (!isAdmin) { el.style.display = 'none'; return; }
        if (el.classList.contains('main-grid')) el.style.display = 'grid';
        else if (el.id === 'data-global-wrapper') el.style.display = 'flex';
        else el.style.display = 'block';
    });
    document.querySelectorAll('.estudante-only').forEach(el => {
        el.style.display = isAdmin ? 'none' : 'block';
    });
    carregarDados();
}

function sair() { localStorage.removeItem("usuario_logado"); location.reload(); }

function venderQtd(tipo, preco, idQtd, idData) {
    const qtd = document.getElementById(idQtd).value;
    const data = document.getElementById(idData).value;
    if(!qtd || qtd <= 0) return erro("Qtd inválida");
    enviarLancamento({ tipo, descricao: `Venda ${qtd}x ${tipo}`, valor: (qtd * preco), dataLancamento: data });
    document.getElementById(idQtd).value = '';
}

function lancarGastoQtd(tipo, custo, idQtd, idData) {
    const qtd = document.getElementById(idQtd).value;
    const data = document.getElementById(idData).value;
    if(!qtd || qtd <= 0) return erro("Qtd inválida");
    enviarLancamento({ tipo, descricao: `Compra Estoque ${qtd}x`, valor: -(qtd * custo), dataLancamento: data });
    document.getElementById(idQtd).value = '';
}

function lancarGenerico(tipo, idValor, desc, idData) {
    const val = document.getElementById(idValor).value;
    const data = document.getElementById(idData).value;
    if(!val || val <= 0) return erro("Valor inválido");
    enviarLancamento({ tipo, descricao: desc, valor: parseFloat(val), dataLancamento: data });
    document.getElementById(idValor).value = '';
}

function lancarGastoGenerico() {
    const val = document.getElementById('valor-outros').value;
    const desc = document.getElementById('desc-outros').value || 'Despesa';
    const data = document.getElementById('data-outros').value;
    if(!val || val <= 0) return erro("Valor inválido");
    enviarLancamento({ tipo: 'OUTROS', descricao: desc, valor: -Math.abs(val), dataLancamento: data });
    document.getElementById('valor-outros').value = '';
    document.getElementById('desc-outros').value = '';
}

function renderizarHistorico(lista) {
    historicoOriginal = [...lista].sort((a, b) => a.dataLancamento > b.dataLancamento ? -1 : 1);
    historicoPaginaAtual = 1;
    aplicarFiltrosHistorico(false);
}

function aplicarFiltrosHistorico(resetPagina = true) {
    const filtroTexto = (document.getElementById('filtro-historico')?.value || '').trim().toLowerCase();
    const dataInicio = document.getElementById('data-inicio-historico')?.value || '';
    const dataFim = document.getElementById('data-fim-historico')?.value || '';
    const filtroId = (document.getElementById('filtro-id-historico')?.value || '').trim();

    if (resetPagina) historicoPaginaAtual = 1;

    historicoFiltrado = historicoOriginal.filter(i => {
        const descricao = (i.descricao || '').toLowerCase();
        const tipo = (i.tipo || '').toLowerCase();
        const textoOk = !filtroTexto || descricao.includes(filtroTexto) || tipo.includes(filtroTexto);
        const data = i.dataLancamento || '';
        const inicioOk = !dataInicio || data >= dataInicio;
        const fimOk = !dataFim || data <= dataFim;
        const idAtual = String(i.id ?? '');
        const idOk = !filtroId || idAtual === filtroId;
        return textoOk && inicioOk && fimOk && idOk;
    });

    renderizarHistoricoPaginado();
}

function renderizarHistoricoPaginado() {
    const tb = document.getElementById('tabela-historico');
    tb.innerHTML = '';

    const totalPaginas = Math.max(1, Math.ceil(historicoFiltrado.length / historicoItensPorPagina));
    if (historicoPaginaAtual > totalPaginas) historicoPaginaAtual = totalPaginas;

    const inicio = (historicoPaginaAtual - 1) * historicoItensPorPagina;
    const pagina = historicoFiltrado.slice(inicio, inicio + historicoItensPorPagina);

    if (pagina.length === 0) {
        tb.innerHTML = '<tr><td colspan="4" style="text-align:center; opacity:0.6;">Nenhum lançamento encontrado</td></tr>';
    } else {
        pagina.forEach(i => {
            const isPos = i.valor >= 0;
            const dataFmt = i.dataLancamento ? i.dataLancamento.split('-').reverse().join('/') : '--';
            const dataEditavel = isAdminLogado
                ? `<td style="text-align:right;opacity:0.7;cursor:pointer;position:relative;" title="Clique para alterar a data" onclick="editarData(this, ${i.id}, '${i.dataLancamento || ''}')"><span style="border-bottom:1px dashed #555;">${dataFmt}</span></td>`
                : `<td style="text-align:right;opacity:0.5">${dataFmt}</td>`;
            const btnDesfazer = isAdminLogado
                ? `<td style="width:40px;text-align:center"><button onclick="desfazerLancamento(${i.id}, '${i.descricao.replace(/'/g, "\\'")}')" style="background:rgba(255,23,68,0.15); border:1px solid var(--red); color:var(--red); border-radius:8px; padding:6px 10px; cursor:pointer; font-size:0.75rem;" title="Desfazer"><i class="fas fa-undo"></i></button></td>`
                : '';
            tb.innerHTML += `<tr>
                <td style="width:40px;text-align:center"><i class="fas ${isPos ? 'fa-arrow-up txt-verde' : 'fa-arrow-down txt-vermelho'}"></i></td>
                <td style="font-weight:600">${i.descricao}</td>
                ${dataEditavel}
                <td style="text-align:right;font-weight:800" class="${isPos ? 'txt-verde' : 'txt-vermelho'}">${fmtMoeda(i.valor)}</td>
                ${btnDesfazer}
            </tr>`;
        });
    }

    document.getElementById('historico-page-info').innerText = `Página ${historicoPaginaAtual} de ${totalPaginas} (${historicoFiltrado.length} registros)`;
    document.getElementById('btn-pagina-anterior').disabled = historicoPaginaAtual <= 1;
    document.getElementById('btn-pagina-proxima').disabled = historicoPaginaAtual >= totalPaginas;
}

function paginaAnteriorHistorico() {
    if (historicoPaginaAtual <= 1) return;
    historicoPaginaAtual -= 1;
    renderizarHistoricoPaginado();
}

function paginaProximaHistorico() {
    const totalPaginas = Math.max(1, Math.ceil(historicoFiltrado.length / historicoItensPorPagina));
    if (historicoPaginaAtual >= totalPaginas) return;
    historicoPaginaAtual += 1;
    renderizarHistoricoPaginado();
}

function limparFiltrosHistorico() {
    document.getElementById('filtro-historico').value = '';
    document.getElementById('data-inicio-historico').value = '';
    document.getElementById('data-fim-historico').value = '';
    document.getElementById('filtro-id-historico').value = '';
    aplicarFiltrosHistorico();
}

function renderizarGrafico(historico) {
    const ctx = document.getElementById('graficoEvolucao').getContext('2d');
    const semErivania = historico.filter(i => i.tipo !== 'ERIVANIA');
    const ordenado = [...semErivania].sort((a,b) => a.dataLancamento > b.dataLancamento ? 1 : -1);
    let soma = 0;
    const labels = ordenado.map(i => i.dataLancamento.split('-').reverse().join('/'));
    const dados = ordenado.map(i => { soma += i.valor; return soma; });

    function corSegmento(ctx, verde, vermelho) {
        const v1 = dados[ctx.p1DataIndex];
        return v1 >= 0 ? verde : vermelho;
    }

    if(chartInstance) chartInstance.destroy();
    chartInstance = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                data: dados,
                segment: {
                    borderColor: ctx => corSegmento(ctx, '#00e676', '#ff1744'),
                    backgroundColor: ctx => corSegmento(ctx, 'rgba(0, 230, 118, 0.1)', 'rgba(255, 23, 68, 0.1)')
                },
                borderColor: '#00e676',
                backgroundColor: 'rgba(0, 230, 118, 0.1)',
                fill: true,
                tension: 0.4,
                borderWidth: 3,
                pointRadius: 3,
                pointBackgroundColor: dados.map(v => v >= 0 ? '#00e676' : '#ff1744')
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: context => `Saldo: ${fmtMoeda(context.parsed.y)}`
                    }
                }
            },
            scales: {
                y: {
                    grid: { color: 'rgba(255,255,255,0.05)' },
                    ticks: { color: '#888' }
                },
                x: {
                    grid: { color: 'rgba(255,255,255,0.05)' },
                    ticks: { color: '#888' }
                }
            }
        }
    });
}

function fmtMoeda(val) { return new Intl.NumberFormat('pt-BR', {style:'currency', currency:'BRL'}).format(val); }
function erro(msg) { Swal.fire({icon:'warning', title:'Ops', text:msg, background:'#222', color:'#fff', toast:true, position:'top-end', showConfirmButton:false, timer:2000}); }

function editarData(td, id, dataAtual) {
    if (td.querySelector('input')) return;
    td.onclick = null;
    const input = document.createElement('input');
    input.type = 'date';
    input.value = dataAtual;
    Object.assign(input.style, {
        background: 'rgba(255,255,255,0.08)',
        border: '1px solid var(--blue)',
        color: '#fff',
        borderRadius: '6px',
        padding: '4px 6px',
        fontSize: '0.85rem',
        width: '100%',
        cursor: 'pointer'
    });
    td.innerHTML = '';
    td.appendChild(input);
    input.focus();
    try { input.showPicker(); } catch(_) {}

    input.addEventListener('change', () => salvarData(id, input.value));
    input.addEventListener('blur', () => {
        setTimeout(() => {
            if (td.querySelector('input')) {
                const fmt = dataAtual ? dataAtual.split('-').reverse().join('/') : '--';
                td.innerHTML = `<span style="border-bottom:1px dashed #555;">${fmt}</span>`;
                td.onclick = () => editarData(td, id, dataAtual);
            }
        }, 200);
    });
}

async function salvarData(id, novaData) {
    if (!novaData) return;
    try {
        const response = await fetch(`${API_URL}/${id}/data`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ dataLancamento: novaData })
        });
        if (response.ok) {
            Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Data atualizada!', showConfirmButton: false, timer: 1500, background: '#222', color: '#fff' });
            carregarDados();
        } else {
            erro('Erro ao atualizar a data.');
        }
    } catch (e) {
        erro('Erro de conexão com o servidor.');
    }
}

async function desfazerLancamento(id, descricao) {
    const resultado = await Swal.fire({
        title: 'Desfazer lançamento?',
        html: `<span style="color:#ccc">Tem certeza que deseja remover:<br><b style="color:#fff">${descricao}</b></span>`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#ff1744',
        cancelButtonColor: '#555',
        confirmButtonText: 'Sim, desfazer',
        cancelButtonText: 'Cancelar',
        background: '#222',
        color: '#fff'
    });

    if (!resultado.isConfirmed) return;

    try {
        const response = await fetch(`${API_URL}/${id}`, { method: 'DELETE' });
        if (response.ok) {
            Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Lançamento desfeito!', showConfirmButton: false, timer: 1500, background: '#222', color: '#fff' });
            carregarDados();
        } else if (response.status === 404) {
            erro('Lançamento não encontrado (já removido?).');
            carregarDados();
        } else {
            erro('Erro ao desfazer o lançamento.');
        }
    } catch (e) {
        erro('Erro de conexão com o servidor.');
    }
}
