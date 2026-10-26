/* ==========================================================================
   Pantalla "Registrar Venta" - FarmaControl
   - Búsqueda en vivo: pide al servidor el fragmento HTML de tarjetas (Thymeleaf)
     cada vez que se escribe una letra o se cambia de categoría.
   - Canasta: subtotal, IGV y total se actualizan al instante.
   - Procesar: el servidor valida, guarda la venta y descuenta el stock.
   El HTML lo arma Thymeleaf (fragmentos); aquí solo se llenan textos y datos.
   ========================================================================== */
(() => {
    'use strict';

    const BASE = document.querySelector('meta[name="base-url"]').content; // "/" normalmente
    const FACTOR_IGV = 1.18; // los precios ya incluyen IGV
    const ESPERA_BUSQUEDA_MS = 250;

    const $ = (selector) => document.querySelector(selector);
    const el = {
        buscador: $('#buscador'),
        btnLimpiarBusqueda: $('#btnLimpiarBusqueda'),
        categorias: document.querySelectorAll('input[name="categoria"]'),
        grid: $('#gridProductos'),
        estado: $('#estadoBusqueda'),
        contador: $('#contadorResultados'),
        lista: $('#listaCanasta'),
        vacia: $('#canastaVacia'),
        plantillaLinea: $('#tplLinea'),
        badge: $('#badgeItems'),
        pagos: document.querySelectorAll('input[name="metodoPago"]'),
        errorPago: $('#errorPago'),
        qrPanel: $('#panelQr'),
        qrImagen: $('#qrImagen'),
        qrFalta: $('#qrFalta'),
        qrMetodo: $('#qrMetodo'),
        qrMonto: $('#qrMonto'),
        subtotal: $('#lblSubtotal'),
        igv: $('#lblIgv'),
        total: $('#lblTotal'),
        alerta: $('#alertaVenta'),
        btnProcesar: $('#btnProcesar'),
        btnCancelar: $('#btnCancelar'),
        modal: $('#modalVentaOk'),
    };

    const state = {
        canasta: new Map(),   // idProducto -> { id, nombre, precio, stock, cantidad }
        categoriaId: 0,
        metodoPago: null,
        procesando: false,
        peticion: null,       // AbortController de la búsqueda en curso
    };

    /* ---------------------------- utilidades ---------------------------- */

    const dinero = (centimos) => 'S/ ' + (centimos / 100).toFixed(2);
    const aCentimos = (soles) => Math.round(Number(soles) * 100);

    let temporizadorAlerta;
    function mostrarAlerta(tipo, mensaje, autoOcultar = false) {
        clearTimeout(temporizadorAlerta);
        el.alerta.className = `alert alert-${tipo} py-2 small`;
        el.alerta.textContent = mensaje;
        if (autoOcultar) temporizadorAlerta = setTimeout(ocultarAlerta, 3500);
    }
    function ocultarAlerta() {
        clearTimeout(temporizadorAlerta);
        el.alerta.className = 'alert d-none py-2 small';
        el.alerta.textContent = '';
    }

    /* ------------------------- búsqueda de productos ------------------------- */

    let temporizadorBusqueda;
    function programarBusqueda() {
        clearTimeout(temporizadorBusqueda);
        temporizadorBusqueda = setTimeout(cargarProductos, ESPERA_BUSQUEDA_MS);
    }

    function quitarFotosRotas() {
        // Si el producto no tiene foto en /static/img/productos/, se queda el ícono de pastilla.
        el.grid.querySelectorAll('img').forEach((img) => {
            img.addEventListener('error', () => img.remove(), { once: true });
            if (img.complete && img.naturalWidth === 0) img.remove();
        });
    }

    function actualizarContador() {
        const n = el.grid.querySelectorAll('[data-card]').length;
        el.contador.textContent = n ? n + (n === 1 ? ' producto' : ' productos') : '';
    }

    async function cargarProductos() {
        if (state.peticion) state.peticion.abort(); // descarta la búsqueda anterior si aún no terminó
        state.peticion = new AbortController();

        const params = new URLSearchParams({
            q: el.buscador.value.trim(),
            categoriaId: String(state.categoriaId),
        });

        try {
            const respuesta = await fetch(`${BASE}ventas/buscar?${params}`, {
                signal: state.peticion.signal,
                headers: { Accept: 'text/html' },
            });
            if (!respuesta.ok) throw new Error('HTTP ' + respuesta.status);
            const html = await respuesta.text();

            el.estado.classList.add('d-none');
            el.grid.innerHTML = html;        // fragmento generado por Thymeleaf
            quitarFotosRotas();
            actualizarContador();
            sincronizarCanasta();
        } catch (error) {
            if (error.name === 'AbortError') return;
            el.estado.innerHTML = '<i class="bi bi-wifi-off fs-1 d-block mb-2"></i>No se pudieron cargar los productos. Revisa la conexión e inténtalo de nuevo.';
            el.estado.classList.remove('d-none');
        }
    }

    /* ------------------------------ canasta ------------------------------ */

    function agregarDesdeBoton(boton) {
        if (!boton || boton.disabled) return;
        const id = Number(boton.dataset.id);
        const stock = Number(boton.dataset.stock);
        const linea = state.canasta.get(id);

        if (linea) {
            linea.stock = stock;
            cambiarCantidad(id, linea.cantidad + 1);
            return;
        }
        state.canasta.set(id, {
            id,
            nombre: boton.dataset.nombre,
            precio: Number(boton.dataset.precio),
            stock,
            cantidad: 1,
        });
        ocultarAlerta();
        pintarCanasta();
    }

    function cambiarCantidad(id, nueva) {
        const linea = state.canasta.get(id);
        if (!linea) return;
        if (!Number.isFinite(nueva)) nueva = linea.cantidad;
        nueva = Math.trunc(nueva);

        if (nueva <= 0) {
            state.canasta.delete(id);
        } else if (nueva > linea.stock) {
            linea.cantidad = linea.stock;
            mostrarAlerta('warning', `Solo hay ${linea.stock} unidades de ${linea.nombre}.`, true);
        } else {
            linea.cantidad = nueva;
            ocultarAlerta();
        }
        pintarCanasta();
    }

    /** Tras recargar las tarjetas, actualiza el stock de las líneas y recorta cantidades que ya no alcanzan. */
    function sincronizarCanasta() {
        let cambio = false;
        state.canasta.forEach((linea) => {
            const boton = el.grid.querySelector(`.js-agregar[data-id="${linea.id}"]`);
            if (!boton) return; // no está en esta búsqueda: se deja como estaba
            linea.stock = Number(boton.dataset.stock);
            if (linea.stock <= 0) {
                state.canasta.delete(linea.id);
                cambio = true;
            } else if (linea.cantidad > linea.stock) {
                linea.cantidad = linea.stock;
                cambio = true;
            }
        });
        if (cambio) pintarCanasta();
    }

    function totales() {
        let totalCentimos = 0;
        state.canasta.forEach((l) => { totalCentimos += aCentimos(l.precio) * l.cantidad; });
        const subtotal = Math.round(totalCentimos / FACTOR_IGV);
        return { total: totalCentimos, subtotal, igv: totalCentimos - subtotal };
    }

    /** Clona la plantilla <template id="tplLinea"> (definida en Thymeleaf) y rellena sus textos. */
    function crearLinea(l) {
        const li = el.plantillaLinea.content.firstElementChild.cloneNode(true);
        li.dataset.id = l.id;
        li.querySelector('.js-nombre').textContent = l.nombre;
        li.querySelector('.js-precio-unitario').textContent = 'Precio unitario: ' + dinero(aCentimos(l.precio));
        li.querySelector('.js-importe').textContent = dinero(aCentimos(l.precio) * l.cantidad);
        li.querySelector('.js-quitar').setAttribute('aria-label', 'Quitar ' + l.nombre);
        const cantidad = li.querySelector('.js-cantidad');
        cantidad.value = l.cantidad;
        cantidad.max = l.stock;
        cantidad.setAttribute('aria-label', 'Cantidad de ' + l.nombre);
        return li;
    }

    function pintarCanasta() {
        const lineas = [...state.canasta.values()];
        el.vacia.classList.toggle('d-none', lineas.length > 0);
        el.lista.replaceChildren(...lineas.map(crearLinea));

        el.badge.textContent = lineas.length + (lineas.length === 1 ? ' producto' : ' productos');

        const t = totales();
        el.subtotal.textContent = dinero(t.subtotal);
        el.igv.textContent = dinero(t.igv);
        el.total.textContent = dinero(t.total);
        actualizarQr();
        actualizarBotones();
    }

    /** Yape y Plin muestran su código QR con el total a cobrar; los demás métodos lo ocultan. */
    function actualizarQr() {
        const usaQr = state.metodoPago === 'YAPE' || state.metodoPago === 'PLIN';
        el.qrPanel.classList.toggle('d-none', !usaQr);
        if (!usaQr) return;
        el.qrMetodo.textContent = state.metodoPago === 'YAPE' ? 'Yape' : 'Plin';
        el.qrMonto.textContent = 'Total: ' + dinero(totales().total);
    }

    function cargarImagenQr() {
        el.qrFalta.classList.add('d-none');
        el.qrImagen.classList.remove('d-none');
        el.qrImagen.src = `${BASE}img/qr/${state.metodoPago.toLowerCase()}.png`;
    }

    function actualizarBotones() {
        el.btnProcesar.disabled = state.procesando || state.canasta.size === 0;
        el.btnProcesar.querySelector('.js-texto').classList.toggle('d-none', state.procesando);
        el.btnProcesar.querySelector('.js-cargando').classList.toggle('d-none', !state.procesando);
        el.btnCancelar.disabled = state.procesando;
    }

    function vaciarCanasta() {
        state.canasta.clear();
        state.metodoPago = null;
        el.pagos.forEach((radio) => { radio.checked = false; });
        el.errorPago.classList.add('d-none');
        ocultarAlerta();
        pintarCanasta();
    }

    /* ------------------------------ procesar ------------------------------ */

    async function procesarVenta() {
        if (state.procesando) return;
        ocultarAlerta();

        if (state.canasta.size === 0) {
            mostrarAlerta('warning', 'Agrega al menos un producto a la canasta.');
            return;
        }
        if (!state.metodoPago) {
            el.errorPago.classList.remove('d-none');
            el.errorPago.scrollIntoView({ behavior: 'smooth', block: 'center' });
            return;
        }

        state.procesando = true;
        actualizarBotones();

        const cuerpo = {
            metodoPago: state.metodoPago,
            items: [...state.canasta.values()].map((l) => ({ idProducto: l.id, cantidad: l.cantidad })),
        };

        try {
            const respuesta = await fetch(`${BASE}ventas/api/procesar`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
                body: JSON.stringify(cuerpo),
            });
            const datos = await respuesta.json().catch(() => ({}));

            if (!respuesta.ok) {
                mostrarAlerta('danger', datos.mensaje || 'No se pudo registrar la venta. Inténtalo de nuevo.');
                if (respuesta.status === 409) cargarProductos(); // el stock cambió: refresca las cantidades
                return;
            }
            mostrarExito(datos);
            vaciarCanasta();
            cargarProductos(); // refresca el stock mostrado
        } catch (error) {
            mostrarAlerta('danger', 'No hay conexión con el servidor. La venta NO se registró; inténtalo de nuevo.');
        } finally {
            state.procesando = false;
            actualizarBotones();
        }
    }

    function mostrarExito(venta) {
        const etiquetas = { EFECTIVO: 'Efectivo', TARJETA: 'Tarjeta', YAPE: 'Yape', PLIN: 'Plin', TRANSFERENCIA: 'Transferencia' };
        $('#okNumero').textContent = venta.idVenta;
        $('#okPago').textContent = etiquetas[venta.metodoPago] || venta.metodoPago;
        $('#okTotal').textContent = dinero(aCentimos(venta.total));
        $('#okSubtotal').textContent = dinero(aCentimos(venta.subtotal));
        $('#okIgv').textContent = dinero(aCentimos(venta.igv));
        bootstrap.Modal.getOrCreateInstance(el.modal).show();
    }

    /* ------------------------------- eventos ------------------------------- */

    // Búsqueda en vivo
    el.buscador.addEventListener('input', () => {
        el.btnLimpiarBusqueda.classList.toggle('d-none', el.buscador.value === '');
        programarBusqueda();
    });
    el.buscador.addEventListener('keydown', (evento) => {
        if (evento.key === 'Enter') {
            evento.preventDefault();
            // Enter agrega el primer producto con stock del resultado actual
            agregarDesdeBoton(el.grid.querySelector('.js-agregar:not(:disabled)'));
        } else if (evento.key === 'Escape') {
            limpiarBusqueda();
        }
    });
    el.btnLimpiarBusqueda.addEventListener('click', limpiarBusqueda);

    function limpiarBusqueda() {
        el.buscador.value = '';
        el.btnLimpiarBusqueda.classList.add('d-none');
        el.buscador.focus();
        programarBusqueda();
    }

    // Categorías (botones tipo radio de Bootstrap: btn-check)
    el.categorias.forEach((radio) => radio.addEventListener('change', () => {
        state.categoriaId = Number(radio.value);
        cargarProductos();
    }));

    // Agregar desde las tarjetas (delegación: sirve aunque el fragmento se recargue)
    el.grid.addEventListener('click', (evento) => {
        agregarDesdeBoton(evento.target.closest('.js-agregar'));
    });

    // Botones + / - / quitar dentro de la canasta
    el.lista.addEventListener('click', (evento) => {
        const boton = evento.target.closest('[data-accion]');
        if (!boton) return;
        const id = Number(boton.closest('li').dataset.id);
        const linea = state.canasta.get(id);
        if (!linea) return;
        if (boton.dataset.accion === 'mas') cambiarCantidad(id, linea.cantidad + 1);
        if (boton.dataset.accion === 'menos') cambiarCantidad(id, linea.cantidad - 1);
        if (boton.dataset.accion === 'quitar') cambiarCantidad(id, 0);
    });
    // Cantidad escrita a mano
    el.lista.addEventListener('change', (evento) => {
        if (!evento.target.classList.contains('js-cantidad')) return;
        const id = Number(evento.target.closest('li').dataset.id);
        cambiarCantidad(id, Number(evento.target.value));
    });

    // Método de pago
    el.pagos.forEach((radio) => radio.addEventListener('change', () => {
        state.metodoPago = radio.value;
        el.errorPago.classList.add('d-none');
        if (state.metodoPago === 'YAPE' || state.metodoPago === 'PLIN') cargarImagenQr();
        actualizarQr();
    }));
    // Si todavía no está la imagen del QR, se avisa en vez de mostrar una imagen rota
    el.qrImagen.addEventListener('error', () => {
        el.qrImagen.classList.add('d-none');
        el.qrFalta.classList.remove('d-none');
    });

    el.btnProcesar.addEventListener('click', procesarVenta);

    el.btnCancelar.addEventListener('click', () => {
        if (state.canasta.size === 0) {
            limpiarBusqueda();
            return;
        }
        if (confirm('¿Vaciar la canasta? Se perderán los productos agregados.')) {
            vaciarCanasta();
            el.buscador.focus();
        }
    });

    // Al cerrar el aviso de venta registrada, listo para la siguiente
    el.modal.addEventListener('hidden.bs.modal', () => el.buscador.focus());

    // Evita perder una venta a medias por cerrar o recargar la pestaña
    window.addEventListener('beforeunload', (evento) => {
        if (state.canasta.size > 0 && !state.procesando) evento.preventDefault();
    });

    /* ------------------------------- arranque ------------------------------- */
    quitarFotosRotas(); // las tarjetas iniciales ya vienen dibujadas por Thymeleaf
    pintarCanasta();
})();