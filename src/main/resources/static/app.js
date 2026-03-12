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

// ── SweetAlert2 dark theme global ──
const DarkSwal = Swal.mixin({
    background: '#1a1a1e',
    color: '#fff',
    confirmButtonColor: '#2979ff',
    cancelButtonColor: '#555',
    customClass: {
        popup: 'swal-dark-popup'
    }
});
const DarkToast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 1800,
    timerProgressBar: true,
    background: '#1a1a1e',
    color: '#fff',
    customClass: {
        popup: 'swal-dark-popup'
    }
});

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

    document.getElementById('btn-expand-chart')?.addEventListener('click', abrirGraficoFullscreen);
    document.getElementById('btn-close-chart-fullscreen')?.addEventListener('click', fecharGraficoFullscreen);
    document.getElementById('chart-fullscreen-overlay')?.addEventListener('click', (e) => {
        if (e.target === e.currentTarget) fecharGraficoFullscreen();
    });
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') fecharGraficoFullscreen();
    });
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

        const saldoAna = data
            .filter(i => i.tipo !== 'ERIVANIA')
            .reduce((acc, i) => acc + i.valor, 0);
        const lucroAcai = data
            .filter(i => i.tipo === 'ACAI')
            .reduce((acc, i) => acc + i.valor, 0);
        const saldoErivania = data
            .filter(i => i.tipo === 'ERIVANIA')
            .reduce((acc, i) => acc + i.valor, 0);
        const caixaTotal = saldoAna + saldoErivania;

        document.getElementById('admin-caixa-total').innerText = fmtMoeda(caixaTotal);
        document.getElementById('saldo').innerText = fmtMoeda(saldoAna);
        document.getElementById('lucro-acai').innerText = fmtMoeda(lucroAcai);
        document.getElementById('saldo-erivania').innerText = fmtMoeda(saldoErivania);

        document.getElementById('saldo-total').innerText = fmtMoeda(caixaTotal);
        document.getElementById('est-saldo-ana').innerText = fmtMoeda(saldoAna);
        document.getElementById('est-lucro-acai').innerText = fmtMoeda(lucroAcai);
        document.getElementById('est-saldo-erivania').innerText = fmtMoeda(saldoErivania);

        renderizarHistorico(data);
        renderizarGrafico(data);
    } catch (error) {
        console.error(error);
        DarkSwal.fire({icon:'error', title:'Erro', text:'O Java está rodando?'});
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
            DarkToast.fire({icon: 'success', title: 'Salvo no PostgreSQL!'});
        }
    } catch (error) {
        erro("Erro ao conectar com o Java.");
    }
}

async function resetarBanco() {
    DarkSwal.fire({
        title: 'Apagar?',
        text: "Vai apagar TODOS os dados do BD",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#8A00C4',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sim',
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                const response = await fetch(API_URL, { method: 'DELETE' });

                if (response.ok) {
                    DarkSwal.fire({title: 'Apagado!', text: 'Banco de dados reiniciado.', icon: 'success'});
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
        DarkSwal.fire({
            icon:'error',
            title:'Erro no backup',
            text:`Nao foi possivel baixar o arquivo agora (${error.message}).`,
        });
    }
}

async function restaurarBackupJson() {
    const arquivoInput = document.getElementById('arquivo-backup');
    const arquivo = arquivoInput?.files?.[0];

    if (!arquivo) {
        return erro('Selecione um arquivo JSON para restaurar.');
    }

    const confirmacao = await DarkSwal.fire({
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

        DarkSwal.fire({
            icon: 'success',
            title: 'Restore concluido',
            text: `${payload?.importados || lancamentos.length} registros importados.`,
        });
        arquivoInput.value = '';
        carregarDados();
    } catch (error) {
        DarkSwal.fire({
            icon:'error',
            title:'Erro na restauracao',
            text: `Nao foi possivel restaurar (${error.message}).`,
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
        DarkSwal.fire({
            icon:'error',
            title:'Login Errado',
            text:'Tente Novamente',
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
    const qtd = parseInt(document.getElementById(idQtd).value);
    const data = document.getElementById(idData).value;
    if(!qtd || qtd <= 0) return erro("Qtd inválida");
    enviarLancamento({ tipo, descricao: `Venda ${qtd}x ${tipo}`, valor: (qtd * preco), dataLancamento: data });
    document.getElementById(idQtd).value = '';
}

function lancarGastoQtd(tipo, custo, idQtd, idData) {
    const qtd = parseInt(document.getElementById(idQtd).value);
    const data = document.getElementById(idData).value;
    if(!qtd || qtd <= 0) return erro("Qtd inválida");
    enviarLancamento({ tipo, descricao: `Compra Estoque ${qtd}x`, valor: -(qtd * custo), dataLancamento: data });
    document.getElementById(idQtd).value = '';
}

function lancarGenerico(tipo, idValor, desc, idData) {
    const val = parseFloat(document.getElementById(idValor).value);
    const data = document.getElementById(idData).value;
    if(!val || val <= 0) return erro("Valor inválido");
    enviarLancamento({ tipo, descricao: desc, valor: val, dataLancamento: data });
    document.getElementById(idValor).value = '';
}

function lancarGastoGenerico() {
    const val = parseFloat(document.getElementById('valor-outros').value);
    const desc = document.getElementById('desc-outros').value || 'Despesa';
    const data = document.getElementById('data-outros').value;
    if(!val || val <= 0) return erro("Valor inválido");
    enviarLancamento({ tipo: 'OUTROS', descricao: desc, valor: -Math.abs(val), dataLancamento: data });
    document.getElementById('valor-outros').value = '';
    document.getElementById('desc-outros').value = '';
}

function renderizarHistorico(lista) {
    historicoOriginal = [...lista].sort((a, b) => {
        if (a.dataLancamento !== b.dataLancamento) return a.dataLancamento > b.dataLancamento ? -1 : 1;
        const horaA = a.horaLancamento || '';
        const horaB = b.horaLancamento || '';
        if (horaA !== horaB) return horaA > horaB ? -1 : 1;
        return (b.id || 0) - (a.id || 0);
    });
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
            const obsTexto = i.observacao ? i.observacao : '';
            const obsHtml = obsTexto
                ? `<div class="obs-display">${obsTexto}</div>`
                : '';
            const btnObs = isAdminLogado
                ? `<span class="obs-btn ${obsTexto ? 'obs-btn-filled' : ''}" onclick="editarObservacao(${i.id}, '${(obsTexto).replace(/'/g, "\\'").replace(/\n/g, '\\n')}')" title="${obsTexto ? 'Editar observação' : 'Adicionar observação'}"><i class="fas ${obsTexto ? 'fa-comment-dots' : 'fa-plus-circle'}"></i></span>`
                : '';
            tb.innerHTML += `<tr>
                <td style="width:40px;text-align:center"><i class="fas ${isPos ? 'fa-arrow-up txt-verde' : 'fa-arrow-down txt-vermelho'}"></i></td>
                <td style="font-weight:600"><div class="desc-cell">${i.descricao}${btnObs}</div>${obsHtml}</td>
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

let chartFullscreenInstance = null;
let ultimoHistoricoGrafico = [];

function renderizarGrafico(historico) {
    ultimoHistoricoGrafico = historico;
    const ctx = document.getElementById('graficoEvolucao').getContext('2d');
    const config = criarConfigGrafico(historico, ctx, false);
    if(chartInstance) chartInstance.destroy();
    chartInstance = new Chart(ctx, config);
}

function abrirGraficoFullscreen() {
    const overlay = document.getElementById('chart-fullscreen-overlay');
    overlay.classList.add('active');
    document.body.style.overflow = 'hidden';

    // Render stats bar
    renderizarStatsBar(ultimoHistoricoGrafico);

    setTimeout(() => {
        const ctx = document.getElementById('graficoFullscreen').getContext('2d');
        const config = criarConfigGrafico(ultimoHistoricoGrafico, ctx, true);
        if(chartFullscreenInstance) chartFullscreenInstance.destroy();
        chartFullscreenInstance = new Chart(ctx, config);
    }, 80);
}

function renderizarStatsBar(historico) {
    const statsBar = document.getElementById('chart-stats-bar');
    if (!statsBar) return;

    const semErivania = historico.filter(i => i.tipo !== 'ERIVANIA');
    const saldoAtual = semErivania.reduce((acc, i) => acc + i.valor, 0);
    const entradas = semErivania.filter(i => i.valor > 0).reduce((acc, i) => acc + i.valor, 0);
    const saidas = semErivania.filter(i => i.valor < 0).reduce((acc, i) => acc + i.valor, 0);
    const totalLancamentos = semErivania.length;

    const saldoClass = saldoAtual >= 0 ? 'positive' : 'negative';

    statsBar.innerHTML = `
        <div class="chart-stat-item">
            <span class="chart-stat-label">Saldo Atual</span>
            <span class="chart-stat-value ${saldoClass}">${fmtMoeda(saldoAtual)}</span>
        </div>
        <div class="chart-stat-item">
            <span class="chart-stat-label">Total Entradas</span>
            <span class="chart-stat-value positive">${fmtMoeda(entradas)}</span>
        </div>
        <div class="chart-stat-item">
            <span class="chart-stat-label">Total Saídas</span>
            <span class="chart-stat-value negative">${fmtMoeda(saidas)}</span>
        </div>
        <div class="chart-stat-item">
            <span class="chart-stat-label">Lançamentos</span>
            <span class="chart-stat-value neutral">${totalLancamentos}</span>
        </div>
    `;
}

function fecharGraficoFullscreen() {
    const overlay = document.getElementById('chart-fullscreen-overlay');
    overlay.classList.remove('active');
    document.body.style.overflow = '';
    if(chartFullscreenInstance) { chartFullscreenInstance.destroy(); chartFullscreenInstance = null; }
}

function criarConfigGrafico(historico, ctx, isFullscreen) {
    const semErivania = historico.filter(i => i.tipo !== 'ERIVANIA');
    const ordenado = [...semErivania].sort((a,b) => {
        if (a.dataLancamento !== b.dataLancamento) return a.dataLancamento > b.dataLancamento ? 1 : -1;
        const horaA = a.horaLancamento || '';
        const horaB = b.horaLancamento || '';
        if (horaA !== horaB) return horaA > horaB ? 1 : -1;
        return (a.id || 0) - (b.id || 0);
    });
    let soma = 0;
    const labels = ordenado.map(i => i.dataLancamento.split('-').reverse().join('/'));
    const dados = ordenado.map(i => { soma += i.valor; return soma; });

    function corSegmento(segCtx, verde, vermelho) {
        const v1 = dados[segCtx.p1DataIndex];
        return v1 >= 0 ? verde : vermelho;
    }

    const chartHeight = isFullscreen ? 600 : 220;

    const gradVerde = ctx.createLinearGradient(0, 0, 0, chartHeight);
    gradVerde.addColorStop(0, 'rgba(0, 230, 118, 0.30)');
    gradVerde.addColorStop(0.4, 'rgba(0, 230, 118, 0.10)');
    gradVerde.addColorStop(0.8, 'rgba(0, 230, 118, 0.02)');
    gradVerde.addColorStop(1, 'rgba(0, 230, 118, 0.0)');

    const gradVermelho = ctx.createLinearGradient(0, 0, 0, chartHeight);
    gradVermelho.addColorStop(0, 'rgba(255, 23, 68, 0.0)');
    gradVermelho.addColorStop(0.2, 'rgba(255, 23, 68, 0.02)');
    gradVermelho.addColorStop(0.6, 'rgba(255, 23, 68, 0.10)');
    gradVermelho.addColorStop(1, 'rgba(255, 23, 68, 0.28)');

    const pointSize = isFullscreen ? 5 : 3;
    const lineWidth = isFullscreen ? 3.5 : 2.5;
    const hoverRadius = isFullscreen ? 12 : 7;

    // Crosshair plugin for fullscreen
    const crosshairPlugin = isFullscreen ? {
        id: 'crosshair',
        afterDraw(chart) {
            if (chart.tooltip?._active?.length) {
                const x = chart.tooltip._active[0].element.x;
                const yAxis = chart.scales.y;
                const ctx2 = chart.ctx;
                ctx2.save();
                ctx2.beginPath();
                ctx2.setLineDash([4, 4]);
                ctx2.moveTo(x, yAxis.top);
                ctx2.lineTo(x, yAxis.bottom);
                ctx2.lineWidth = 1;
                ctx2.strokeStyle = 'rgba(255, 255, 255, 0.12)';
                ctx2.stroke();
                ctx2.restore();
            }
        }
    } : null;

    // Zero line plugin
    const zeroLinePlugin = {
        id: 'zeroLine',
        afterDraw(chart) {
            const yScale = chart.scales.y;
            const yPos = yScale.getPixelForValue(0);
            if (yPos >= yScale.top && yPos <= yScale.bottom) {
                const ctx2 = chart.ctx;
                ctx2.save();
                ctx2.beginPath();
                ctx2.setLineDash([6, 4]);
                ctx2.moveTo(chart.chartArea.left, yPos);
                ctx2.lineTo(chart.chartArea.right, yPos);
                ctx2.lineWidth = 1;
                ctx2.strokeStyle = 'rgba(255, 255, 255, 0.15)';
                ctx2.stroke();
                ctx2.restore();
            }
        }
    };

    const plugins = [zeroLinePlugin];
    if (crosshairPlugin) plugins.push(crosshairPlugin);

    return {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                data: dados,
                segment: {
                    borderColor: segCtx => corSegmento(segCtx, '#00e676', '#ff1744'),
                    backgroundColor: segCtx => corSegmento(segCtx, gradVerde, gradVermelho)
                },
                borderColor: '#00e676',
                backgroundColor: gradVerde,
                fill: true,
                tension: 0.4,
                borderWidth: lineWidth,
                pointRadius: pointSize,
                pointHoverRadius: hoverRadius,
                pointBackgroundColor: dados.map(v => v >= 0 ? '#00e676' : '#ff1744'),
                pointBorderColor: dados.map(v => v >= 0 ? 'rgba(0,230,118,0.3)' : 'rgba(255,23,68,0.3)'),
                pointBorderWidth: isFullscreen ? 3 : 2,
                pointHoverBackgroundColor: '#fff',
                pointHoverBorderWidth: isFullscreen ? 4 : 3,
                pointHoverBorderColor: dados.map(v => v >= 0 ? '#00e676' : '#ff1744'),
            }]
        },
        plugins: plugins,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: {
                duration: isFullscreen ? 1000 : 600,
                easing: 'easeOutQuart',
                delay: (context) => {
                    if (context.type === 'data' && context.mode === 'default') {
                        return context.dataIndex * (isFullscreen ? 15 : 8);
                    }
                    return 0;
                }
            },
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(10, 10, 15, 0.95)',
                    titleColor: '#999',
                    titleFont: { size: isFullscreen ? 13 : 11, weight: '600', family: 'Inter' },
                    bodyColor: '#fff',
                    bodyFont: { size: isFullscreen ? 17 : 13, weight: '800', family: 'Inter' },
                    borderColor: 'rgba(255,255,255,0.06)',
                    borderWidth: 1,
                    cornerRadius: 14,
                    padding: isFullscreen ? 18 : 10,
                    displayColors: false,
                    caretSize: isFullscreen ? 8 : 6,
                    callbacks: {
                        title: items => items[0] ? `📅 ${items[0].label}` : '',
                        label: context => {
                            const val = context.parsed.y;
                            const emoji = val >= 0 ? '🟢' : '🔴';
                            return `${emoji}  ${fmtMoeda(val)}`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    grid: {
                        color: isFullscreen ? 'rgba(255,255,255,0.05)' : 'rgba(255,255,255,0.04)',
                        drawBorder: false,
                        lineWidth: 0.5
                    },
                    border: { display: false },
                    ticks: {
                        color: '#555',
                        font: { size: isFullscreen ? 13 : 10, weight: '600', family: 'Inter' },
                        padding: isFullscreen ? 14 : 6,
                        callback: val => fmtMoeda(val)
                    }
                },
                x: {
                    grid: {
                        color: isFullscreen ? 'rgba(255,255,255,0.03)' : 'rgba(255,255,255,0.02)',
                        drawBorder: false,
                        lineWidth: 0.5
                    },
                    border: { display: false },
                    ticks: {
                        color: '#555',
                        font: { size: isFullscreen ? 12 : 9, weight: '600', family: 'Inter' },
                        padding: isFullscreen ? 12 : 4,
                        maxRotation: isFullscreen ? 45 : 60,
                        autoSkip: true,
                        maxTicksLimit: isFullscreen ? 35 : 12
                    }
                }
            }
        }
    };
}

function fmtMoeda(val) { return new Intl.NumberFormat('pt-BR', {style:'currency', currency:'BRL'}).format(val); }
function erro(msg) { DarkToast.fire({icon:'warning', title:'Ops', text:msg}); }

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
            DarkToast.fire({icon: 'success', title: 'Data atualizada!'});
            carregarDados();
        } else {
            erro('Erro ao atualizar a data.');
        }
    } catch (e) {
        erro('Erro de conexão com o servidor.');
    }
}

async function desfazerLancamento(id, descricao) {
    const resultado = await DarkSwal.fire({
        title: 'Desfazer lançamento?',
        html: `<span style="color:#ccc">Tem certeza que deseja remover:<br><b style="color:#fff">${descricao}</b></span>`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#ff1744',
        cancelButtonColor: '#555',
        confirmButtonText: 'Sim, desfazer',
        cancelButtonText: 'Cancelar',
    });

    if (!resultado.isConfirmed) return;

    try {
        const response = await fetch(`${API_URL}/${id}`, { method: 'DELETE' });
        if (response.ok) {
            DarkToast.fire({icon: 'success', title: 'Lançamento desfeito!'});
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

async function editarObservacao(id, obsAtual) {
    const resultado = await DarkSwal.fire({
        title: 'Observação',
        input: 'textarea',
        inputLabel: 'Adicione um detalhe ao lançamento',
        inputPlaceholder: 'Ex: 100 Pix + 100 Espécie',
        inputValue: obsAtual || '',
        showCancelButton: true,
        confirmButtonText: 'Salvar',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#2979ff',
        inputAttributes: {
            style: 'background:rgba(255,255,255,0.08); color:#fff; border:1px solid #444; border-radius:8px; font-size:0.95rem;'
        },
        showDenyButton: obsAtual ? true : false,
        denyButtonText: 'Remover',
        denyButtonColor: '#ff1744'
    });

    if (resultado.isDismissed) return;
    if (resultado.isDenied) {
        await salvarObservacao(id, '');
        return;
    }
    if (resultado.isConfirmed) {
        await salvarObservacao(id, resultado.value);
    }
}

async function salvarObservacao(id, obs) {
    try {
        const response = await fetch(`${API_URL}/${id}/observacao`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ observacao: obs || null })
        });
        if (response.ok) {
            DarkToast.fire({icon: 'success', title: 'Observação salva!'});
            carregarDados();
        } else {
            erro('Erro ao salvar observação.');
        }
    } catch (e) {
        erro('Erro de conexão com o servidor.');
    }
}

