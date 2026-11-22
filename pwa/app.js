// PVS Cuotas PWA — Cámara Web (BarcodeDetector + ZXing fallback) + Gemini API + Cuotas Quincenales

const STORAGE_KEY = 'pvs_cuotas_pwa_v1';
const API_KEY_STORAGE = 'pvs_cuotas_gemini_api_key';

const CATALOG = [
  {
    code: 'TV-50',
    barcode: '7501001005011',
    name: 'Smart TV 50" 4K UHD',
    category: 'Tecnología',
    storePrice: 389.0,
    averagePrice: 460.0,
    quotasCount: 6,
    verdict: 'GREEN',
    verdictReason: 'Está un 15% por debajo del promedio del mercado. Excelente oportunidad para financiar en cuotas cómodas.'
  },
  {
    code: 'CEL-128',
    barcode: '7501001012804',
    name: 'Smartphone 128GB 5G',
    category: 'Telefonía',
    storePrice: 320.0,
    averagePrice: 295.0,
    quotasCount: 6,
    verdict: 'YELLOW',
    verdictReason: 'Cuesta un 8% más que el promedio habitual. Conviene comparar en otra tienda o esperar promoción.'
  },
  {
    code: 'ZAP-01',
    barcode: '7501001000109',
    name: 'Zapatillas Running Pro',
    category: 'Calzado',
    storePrice: 145.0,
    averagePrice: 105.0,
    quotasCount: 4,
    verdict: 'RED',
    verdictReason: 'Supera en un 38% el precio promedio real. El sobrecosto encarece innecesariamente tu quincena.'
  },
  {
    code: 'AUD-05',
    barcode: '7501001000505',
    name: 'Audífonos ANC Studio',
    category: 'Audio',
    storePrice: 118.0,
    averagePrice: 149.0,
    quotasCount: 4,
    verdict: 'GREEN',
    verdictReason: 'Ahorro real del 21% frente al mercado y un impacto ligero en tu presupuesto quincenal.'
  },
  {
    code: 'LAP-16',
    barcode: '7501001001601',
    name: 'Laptop Ultrabook 16GB',
    category: 'Computación',
    storePrice: 790.0,
    averagePrice: 735.0,
    quotasCount: 8,
    verdict: 'YELLOW',
    verdictReason: 'Precio ligeramente por encima del promedio (+7%) y cuota quincenal moderadamente alta.'
  },
  {
    code: 'CAF-02',
    barcode: '7501001000208',
    name: 'Cafetera Espresso Barista',
    category: 'Hogar',
    storePrice: 215.0,
    averagePrice: 160.0,
    quotasCount: 4,
    verdict: 'RED',
    verdictReason: 'Sobreprecio del 34% frente a distribuidores oficiales. Te conviene buscar otra oferta.'
  }
];

const CATEGORY_PRESETS = [
  'Tecnología', 'Hogar', 'Salud', 'Telefonía', 'Audio',
  'Calzado', 'Computación', 'Educación', 'Transporte', 'General'
];

const CATEGORY_COLORS = {
  'tecnología': '#38B261',
  'hogar': '#F59E0B',
  'salud': '#F43F5E',
  'telefonía': '#0EA5E9',
  'audio': '#8B5CF6',
  'calzado': '#14B8A6',
  'computación': '#10B981',
  'educación': '#3B82F6',
  'transporte': '#F97316',
  'general': '#64748B'
};

const CUSTOM_PALETTE = ['#EC4899', '#06B6D4', '#84CC16', '#A855F7', '#F97316', '#3B82F6', '#14B8A6'];

const DEFAULT_INSTALLMENTS = [
  { id: 'inst-1', title: 'Smart TV 50" 4K UHD', code: 'TV-50', category: 'Tecnología', initialPaid: 97.25, totalAmount: 389.0, biweeklyQuota: 48.63, totalQuotas: 6, paidQuotas: 4, daysUntilDue: 2 },
  { id: 'inst-2', title: 'Smartphone 128GB 5G', code: 'CEL-128', category: 'Telefonía', initialPaid: 80.0, totalAmount: 320.0, biweeklyQuota: 40.0, totalQuotas: 6, paidQuotas: 2, daysUntilDue: 3 },
  { id: 'inst-3', title: 'Audífonos ANC Studio', code: 'AUD-05', category: 'Audio', initialPaid: 29.5, totalAmount: 118.0, biweeklyQuota: 22.13, totalQuotas: 4, paidQuotas: 3, daysUntilDue: 11 },
  { id: 'inst-4', title: 'Zapatillas Running Pro', code: 'ZAP-01', category: 'Calzado', initialPaid: 36.25, totalAmount: 145.0, biweeklyQuota: 27.19, totalQuotas: 4, paidQuotas: 1, daysUntilDue: 16 }
];

const TUTORIAL_STEPS = [
  {
    title: '1. Escanea con la cámara web',
    body: 'Pulsa "Escanear código" en el Verificador (o sube una foto de la etiqueta) para leer códigos EAN-13, UPC o QR directamente desde el navegador de tu teléfono.'
  },
  {
    title: '2. Veredicto con Gemini AI',
    body: 'Conecta tu Gemini API Key en el ícono ✨ superior para analizar cualquier producto del mercado y saber si "¡Vale la pena!", "Piénsalo dos veces" o "No vale la pena".'
  },
  {
    title: '3. Calcula y controla tus cuotas',
    body: 'Ajusta el precio total y número de quincenas tras escanear y agrégalo con un toque a "Mis Cuotas", con gráfico de pastel por categoría y modo nocturno.'
  }
];

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatUsd(amount) {
  const num = Number(amount) || 0;
  return '$' + num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function categoryColor(category) {
  const key = String(category || 'General').trim().toLowerCase();
  if (CATEGORY_COLORS[key]) return CATEGORY_COLORS[key];
  let hash = 0;
  for (let i = 0; i < key.length; i++) {
    hash = (hash * 31 + key.charCodeAt(i)) >>> 0;
  }
  return CUSTOM_PALETTE[hash % CUSTOM_PALETTE.length];
}

function loadState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      return {
        budget: Number(parsed.budget) > 0 ? Number(parsed.budget) : 420,
        darkMode: Boolean(parsed.darkMode),
        installments: Array.isArray(parsed.installments) ? parsed.installments : DEFAULT_INSTALLMENTS,
        recentSearches: Array.isArray(parsed.recentSearches) ? parsed.recentSearches : [],
        scannedHistory: parsed.scannedHistory && typeof parsed.scannedHistory === 'object' ? parsed.scannedHistory : {},
        chatMessages: Array.isArray(parsed.chatMessages) ? parsed.chatMessages : [],
        geminiModel: parsed.geminiModel || 'gemini-2.5-flash',
        tutorialSeen: Boolean(parsed.tutorialSeen)
      };
    }
  } catch (err) {
    console.warn('Error cargando estado local:', err);
  }
  return {
    budget: 420,
    darkMode: false,
    installments: [...DEFAULT_INSTALLMENTS],
    recentSearches: [],
    scannedHistory: {},
    chatMessages: [],
    geminiModel: 'gemini-2.5-flash',
    tutorialSeen: false
  };
}

const appState = loadState();
let activeTab = 'verificador';
let chartMode = 'biweekly';
let categoryFilter = 'Todas';
let editingCategoryInstallmentId = null;
let tutorialStepIdx = 0;
let deferredInstallPrompt = null;

let currentVerification = {
  query: '',
  rawBarcode: null,
  product: null,
  aiStatus: 'idle', // idle | loading | success | error | local_only
  aiData: null,
  aiError: null,
  calcTotal: 0,
  calcQuotas: 6,
  calcIncludeInitial: true
};

// Camera Scanner State
let cameraStream = null;
let scannerRafId = null;
let zxingControls = null;
let scannerActive = false;
let useFrontCamera = false;
let torchEnabled = false;

function saveState() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(appState));
  } catch (err) {
    console.warn('No se pudo guardar estado:', err);
  }
}

function getGeminiKey() {
  return (localStorage.getItem(API_KEY_STORAGE) || '').trim();
}

function setGeminiKey(key) {
  const clean = String(key || '').trim();
  if (clean) {
    localStorage.setItem(API_KEY_STORAGE, clean);
  } else {
    localStorage.removeItem(API_KEY_STORAGE);
  }
  updateApiStatusUi();
}

function showToast(msg) {
  const toast = document.getElementById('toast');
  if (!toast) return;
  toast.textContent = msg;
  toast.hidden = false;
  clearTimeout(showToast._timer);
  showToast._timer = setTimeout(() => {
    toast.hidden = true;
  }, 2800);
}

// --- Barcode & Catalog Resolution (mirrors ProductVerifierCatalog.kt) ---

function lookupCatalog(rawInput) {
  const trimmed = String(rawInput || '').trim();
  if (!trimmed) return null;

  const digitsOnly = trimmed.replace(/\D/g, '');
  if (digitsOnly.length >= 8) {
    const byBarcode = CATALOG.find(
      (item) =>
        item.barcode === digitsOnly ||
        item.barcode.replace(/^0+/, '') === digitsOnly.replace(/^0+/, '')
    );
    if (byBarcode) return byBarcode;
  }

  const upper = trimmed.toUpperCase();
  const compact = upper.replace(/[-\s]/g, '');
  const byCode = CATALOG.find((item) => {
    const itemUpper = item.code.toUpperCase();
    const itemCompact = itemUpper.replace(/-/g, '');
    return itemUpper === upper || itemCompact === compact || upper.includes(itemUpper);
  });
  if (byCode) return byCode;

  return CATALOG.find((item) => item.name.toLowerCase().includes(trimmed.toLowerCase())) || null;
}

function resolveBarcodeValue(rawBarcode) {
  const trimmed = String(rawBarcode || '').trim();
  if (!trimmed) return '';
  const match = lookupCatalog(trimmed);
  if (match) return match.code;
  const digits = trimmed.replace(/\D/g, '');
  if (digits.length >= 8 && /^[0-9\-\s]+$/.test(trimmed)) return digits;
  return trimmed;
}

function computeVerdict(storePrice, averagePrice) {
  if (!averagePrice || averagePrice <= 0) {
    return { code: 'YELLOW', headline: 'Piénsalo dos veces', badge: 'Semáforo Amarillo' };
  }
  const diffRatio = (storePrice - averagePrice) / averagePrice;
  if (diffRatio <= 0.0) {
    return { code: 'GREEN', headline: '¡Vale la pena!', badge: 'Semáforo Verde' };
  }
  if (diffRatio <= 0.15) {
    return { code: 'YELLOW', headline: 'Piénsalo dos veces', badge: 'Semáforo Amarillo' };
  }
  return { code: 'RED', headline: 'No vale la pena', badge: 'Semáforo Rojo' };
}

function verdictMeta(verdictCode) {
  switch (verdictCode) {
    case 'GREEN':
      return { css: 'green', headline: '¡Vale la pena!', badge: 'Semáforo Verde' };
    case 'RED':
      return { css: 'red', headline: 'No vale la pena', badge: 'Semáforo Rojo' };
    default:
      return { css: 'yellow', headline: 'Piénsalo dos veces', badge: 'Semáforo Amarillo' };
  }
}

function buildAutocomplete(query) {
  const q = String(query || '').trim().toLowerCase();
  if (!q) {
    return CATALOG.slice(0, 5);
  }
  return CATALOG.filter(
    (item) =>
      item.code.toLowerCase().includes(q) ||
      item.name.toLowerCase().includes(q) ||
      item.category.toLowerCase().includes(q) ||
      item.barcode.includes(q)
  ).slice(0, 5);
}

// --- Gemini API Direct Browser Client ---

async function callGeminiJson(promptText, schema) {
  const apiKey = getGeminiKey();
  if (!apiKey) {
    throw new Error('NO_API_KEY');
  }
  const model = appState.geminiModel || 'gemini-2.5-flash';
  const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(apiKey)}`;

  const response = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      contents: [{ role: 'user', parts: [{ text: promptText }] }],
      generationConfig: {
        temperature: 0.35,
        maxOutputTokens: 700,
        responseMimeType: 'application/json',
        responseSchema: schema
      }
    })
  });

  if (!response.ok) {
    const errBody = await response.text().catch(() => '');
    if (response.status === 400 || response.status === 401 || response.status === 403) {
      throw new Error('API Key de Gemini inválida o sin permisos. Verifica tu clave en el botón ✨.');
    }
    throw new Error(`Error Gemini (${response.status}): ${errBody.slice(0, 140)}`);
  }

  const data = await response.json();
  const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) {
    throw new Error('Gemini devolvió una respuesta vacía.');
  }
  return JSON.parse(text);
}

async function callGeminiChat(userMessage) {
  const apiKey = getGeminiKey();
  if (!apiKey) {
    return buildOfflineAssistantReply(userMessage);
  }

  const active = appState.installments.filter((i) => i.paidQuotas < i.totalQuotas);
  const committed = active.reduce((acc, i) => acc + i.biweeklyQuota, 0);
  const free = Math.max(0, appState.budget - committed);

  const systemContext =
    `Eres el asistente financiero de PVS Cuotas. Responde en español claro, directo y práctico (máximo 140 palabras).\n` +
    `Contexto del usuario:\n` +
    `- Presupuesto quincenal: $${appState.budget.toFixed(2)}\n` +
    `- Cuotas comprometidas por quincena: $${committed.toFixed(2)}\n` +
    `- Disponible libre por quincena: $${free.toFixed(2)}\n` +
    `- Cuotas activas: ${active.map((i) => `${i.title} ($${i.biweeklyQuota.toFixed(2)}/quincena, ${i.paidQuotas}/${i.totalQuotas})`).join('; ') || 'ninguna'}.`;

  const model = appState.geminiModel || 'gemini-2.5-flash';
  const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(apiKey)}`;

  const response = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      systemInstruction: { parts: [{ text: systemContext }] },
      contents: [{ role: 'user', parts: [{ text: userMessage }] }],
      generationConfig: { temperature: 0.5, maxOutputTokens: 450 }
    })
  });

  if (!response.ok) {
    throw new Error(`No se pudo consultar Gemini (${response.status}). Revisa tu API Key en ✨.`);
  }
  const data = await response.json();
  return data?.candidates?.[0]?.content?.parts?.[0]?.text || 'No obtuve respuesta en este momento.';
}

function buildOfflineAssistantReply(userMessage) {
  const active = appState.installments.filter((i) => i.paidQuotas < i.totalQuotas);
  const committed = active.reduce((acc, i) => acc + i.biweeklyQuota, 0);
  const free = Math.max(0, appState.budget - committed);
  return (
    `Con tu presupuesto actual de ${formatUsd(appState.budget)} por quincena, ya tienes comprometidos ${formatUsd(committed)} en ${active.length} cuotas activas y te quedan ${formatUsd(free)} libres.\n\n` +
    `Consejo PVS: procura que la nueva cuota no supere el 40% de tu saldo libre (${formatUsd(free * 0.4)}/quincena).\n\n` +
    `💡 Configura tu Gemini API Key en el ícono ✨ arriba para respuestas personalizadas con IA.`
  );
}

const GEMINI_PRODUCT_SCHEMA = {
  type: 'OBJECT',
  properties: {
    name: { type: 'STRING' },
    category: { type: 'STRING' },
    storePrice: { type: 'NUMBER' },
    averagePrice: { type: 'NUMBER' },
    suggestedQuotas: { type: 'INTEGER' },
    verdict: { type: 'STRING', enum: ['GREEN', 'YELLOW', 'RED'] },
    verdictHeadline: { type: 'STRING' },
    verdictReason: { type: 'STRING' },
    powerAndSpecs: { type: 'STRING' },
    durability: { type: 'STRING' },
    summary: { type: 'STRING' }
  },
  required: ['name', 'category', 'storePrice', 'averagePrice', 'suggestedQuotas', 'verdict', 'verdictHeadline', 'verdictReason', 'powerAndSpecs', 'durability', 'summary']
};

async function verifyProductQuery(rawInput, { rawBarcode = null, storePriceOverride = null } = {}) {
  const resolved = resolveBarcodeValue(rawInput);
  if (!resolved) return;

  const catalogMatch = lookupCatalog(resolved) || lookupCatalog(rawInput);
  const committed = appState.installments
    .filter((i) => i.paidQuotas < i.totalQuotas)
    .reduce((sum, i) => sum + i.biweeklyQuota, 0);
  const freeBudget = Math.max(0, appState.budget - committed);

  let baseProduct = null;
  if (catalogMatch) {
    const effectiveStorePrice =
      storePriceOverride && storePriceOverride > 0 ? storePriceOverride : catalogMatch.storePrice;
    const recalculated = computeVerdict(effectiveStorePrice, catalogMatch.averagePrice);
    baseProduct = {
      ...catalogMatch,
      storePrice: effectiveStorePrice,
      verdict: storePriceOverride ? recalculated.code : catalogMatch.verdict,
      verdictReason: storePriceOverride
        ? `Precio en tienda (${formatUsd(effectiveStorePrice)}) comparado contra promedio de ${formatUsd(catalogMatch.averagePrice)}.`
        : catalogMatch.verdictReason
    };
  }

  currentVerification = {
    query: resolved,
    rawBarcode,
    product: baseProduct,
    aiStatus: getGeminiKey() ? 'loading' : 'local_only',
    aiData: null,
    aiError: null,
    calcTotal: baseProduct ? baseProduct.storePrice : (storePriceOverride || 120),
    calcQuotas: baseProduct ? baseProduct.quotasCount : 6,
    calcIncludeInitial: true
  };

  if (baseProduct) {
    recordRecentAndHistory(baseProduct, rawBarcode);
  }
  renderVerifierResult();

  if (!getGeminiKey()) {
    if (!baseProduct) {
      const estimatedAvg = storePriceOverride ? storePriceOverride * 0.96 : 115.0;
      const effectiveStore = storePriceOverride || 125.0;
      const heuristic = computeVerdict(effectiveStore, estimatedAvg);
      currentVerification.product = {
        code: resolved.toUpperCase().slice(0, 14),
        barcode: rawBarcode || resolved,
        name: `Producto escaneado (${resolved.slice(0, 16)})`,
        category: 'General',
        storePrice: effectiveStore,
        averagePrice: estimatedAvg,
        quotasCount: 6,
        verdict: heuristic.code,
        verdictReason:
          'Código externo detectado. Conecta tu Gemini API Key (botón ✨) para identificar marca, precio real de mercado y veredicto inteligente.'
      };
      currentVerification.calcTotal = effectiveStore;
    }
    renderVerifierResult();
    return;
  }

  try {
    const priceContext = storePriceOverride
      ? `El usuario reporta un precio visto en tienda de $${storePriceOverride.toFixed(2)} USD.`
      : catalogMatch
      ? `Precio visto en tienda: $${catalogMatch.storePrice.toFixed(2)} USD (promedio referencia $${catalogMatch.averagePrice.toFixed(2)} USD).`
      : 'El usuario no ingresó precio manualmente; estima un precio típico en tienda y precio promedio de mercado realista en USD.';

    const prompt =
      `Eres el verificador de compras inteligente de PVS Cuotas.\n` +
      `El usuario escaneó o buscó este código/producto: "${rawInput}" (código resuelto: "${resolved}").\n` +
      (catalogMatch ? `Producto reconocido en catálogo PVS: ${catalogMatch.name} (${catalogMatch.category}).\n` : '') +
      `${priceContext}\n` +
      `Presupuesto libre quincenal del usuario tras cuotas activas: $${freeBudget.toFixed(2)} USD.\n\n` +
      `Evalúa si la compra conviene en cuotas quincenales y devuelve JSON estricto en ESPAÑOL:\n` +
      `- verdict: "GREEN" si es buena compra / buen precio, "YELLOW" si conviene pensarlo dos veces o comparar otra tienda, "RED" si tiene sobreprecio o afecta demasiado su quincena.\n` +
      `- verdictHeadline: exactamente una de: "¡Vale la pena!", "Piénsalo dos veces", "No vale la pena".\n` +
      `- verdictReason: 1-2 frases explicando claramente el porqué del semáforo considerando precio vs mercado y su quincena libre.\n` +
      `- suggestedQuotas: entero entre 2 y 12.\n` +
      `- powerAndSpecs, durability, summary: ficha breve y útil para el comprador.`;

    const aiResult = await callGeminiJson(prompt, GEMINI_PRODUCT_SCHEMA);

    const resolvedStore = storePriceOverride && storePriceOverride > 0
      ? storePriceOverride
      : Math.max(1, Number(aiResult.storePrice) || baseProduct?.storePrice || 100);
    const resolvedAvg = Math.max(1, Number(aiResult.averagePrice) || baseProduct?.averagePrice || resolvedStore);

    const productObj = {
      code: baseProduct?.code || resolved.toUpperCase().replace(/[^A-Z0-9-]/g, '').slice(0, 14) || 'SCAN-IA',
      barcode: rawBarcode || baseProduct?.barcode || resolved,
      name: baseProduct?.name || aiResult.name || `Producto ${resolved}`,
      category: baseProduct?.category || aiResult.category || 'General',
      storePrice: resolvedStore,
      averagePrice: resolvedAvg,
      quotasCount: Math.min(12, Math.max(2, Number(aiResult.suggestedQuotas) || 6)),
      verdict: aiResult.verdict || computeVerdict(resolvedStore, resolvedAvg).code,
      verdictReason: aiResult.verdictReason || ''
    };

    currentVerification.product = productObj;
    currentVerification.aiStatus = 'success';
    currentVerification.aiData = aiResult;
    currentVerification.calcTotal = productObj.storePrice;
    currentVerification.calcQuotas = productObj.quotasCount;
    recordRecentAndHistory(productObj, rawBarcode);
  } catch (err) {
    currentVerification.aiStatus = 'error';
    currentVerification.aiError = err.message || 'No se pudo consultar Gemini.';
    if (!currentVerification.product) {
      const fallbackPrice = storePriceOverride || 120;
      currentVerification.product = {
        code: resolved.toUpperCase().slice(0, 14),
        barcode: rawBarcode || resolved,
        name: `Código ${resolved}`,
        category: 'General',
        storePrice: fallbackPrice,
        averagePrice: fallbackPrice,
        quotasCount: 6,
        verdict: 'YELLOW',
        verdictReason: 'No se pudo contactar a Gemini. Verifica tu clave API o conexión.'
      };
    }
  }

  renderVerifierResult();
}

function recordRecentAndHistory(product, rawBarcode) {
  appState.recentSearches = [
    { code: product.code, name: product.name },
    ...appState.recentSearches.filter((r) => r.code !== product.code)
  ].slice(0, 6);

  const list = appState.scannedHistory[product.code] || [];
  list.push({
    price: product.storePrice,
    barcode: rawBarcode || product.barcode || product.code,
    at: Date.now()
  });
  appState.scannedHistory[product.code] = list.slice(-5);
  saveState();
}

// --- Rendering Functions ---

function renderVerifierResult() {
  const container = document.getElementById('verifier-result-area');
  if (!container) return;

  const { product, aiStatus, aiData, aiError, rawBarcode } = currentVerification;

  if (!product && aiStatus === 'idle') {
    const recentHtml = appState.recentSearches.length
      ? `<div class="card">
          <div class="card-header">
            <h2>Búsquedas recientes</h2>
          </div>
          <div class="chips-wrap">
            ${appState.recentSearches
              .map(
                (r) =>
                  `<button type="button" class="chip-btn" data-recent-code="${escapeHtml(r.code)}">${escapeHtml(r.code)} · ${escapeHtml(r.name)}</button>`
              )
              .join('')}
          </div>
        </div>`
      : '';

    container.innerHTML = `
      <div class="card">
        <span class="eyebrow">Listo para escanear</span>
        <h2 style="margin:6px 0 6px;font-size:1.1rem;">Apunta tu cámara o elige un código de ejemplo</h2>
        <p class="card-subtitle">Al escanear, verás el semáforo de compra (<strong>¡Vale la pena!</strong>, <strong>Piénsalo dos veces</strong> o <strong>No vale la pena</strong>), la opinión de Gemini y el cálculo automático de cuotas quincenales.</p>
      </div>
      ${recentHtml}
    `;

    container.querySelectorAll('[data-recent-code]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const code = btn.getAttribute('data-recent-code');
        document.getElementById('search-code-input').value = code;
        verifyProductQuery(code);
      });
    });
    return;
  }

  if (!product && aiStatus === 'loading') {
    container.innerHTML = `
      <div class="card">
        <div style="display:flex;align-items:center;gap:12px;">
          <div class="spinner"></div>
          <div>
            <strong>Analizando con Gemini AI...</strong>
            <div class="card-subtitle">Evaluando precio de mercado y cuotas para ${escapeHtml(currentVerification.query)}</div>
          </div>
        </div>
      </div>
    `;
    return;
  }

  const meta = verdictMeta(product.verdict);
  const diffPct = Math.round(((product.storePrice - product.averagePrice) / Math.max(1, product.averagePrice)) * 100);
  const diffLabel = diffPct === 0 ? 'Igual al promedio' : diffPct < 0 ? `${diffPct}% vs mercado` : `+${diffPct}% vs mercado`;
  const history = appState.scannedHistory[product.code] || [];

  const total = Math.max(1, Number(currentVerification.calcTotal) || product.storePrice);
  const quotas = Math.max(2, Number(currentVerification.calcQuotas) || 6);
  const initialAmt = currentVerification.calcIncludeInitial ? total * 0.25 : 0;
  const biweeklyAmt = (total - initialAmt) / quotas;

  let aiSectionHtml = '';
  if (aiStatus === 'loading') {
    aiSectionHtml = `
      <div class="gemini-box">
        <div style="display:flex;align-items:center;gap:10px;">
          <div class="spinner"></div>
          <span>Consultando veredicto detallado con Gemini AI...</span>
        </div>
      </div>`;
  } else if (aiStatus === 'success' && aiData) {
    aiSectionHtml = `
      <div class="gemini-box">
        <div class="gemini-box-head">
          <span class="gemini-badge">✨ Análisis Gemini AI</span>
          <span class="card-subtitle">${escapeHtml(appState.geminiModel)}</span>
        </div>
        <div class="gemini-spec-grid">
          <div class="gemini-spec-item">
            <strong>Rendimiento y especificaciones</strong>
            <span>${escapeHtml(aiData.powerAndSpecs)}</span>
          </div>
          <div class="gemini-spec-item">
            <strong>Durabilidad estimada</strong>
            <span>${escapeHtml(aiData.durability)}</span>
          </div>
          <div class="gemini-spec-item">
            <strong>Resumen para tu bolsillo</strong>
            <span>${escapeHtml(aiData.summary)}</span>
          </div>
        </div>
      </div>`;
  } else if (aiStatus === 'error') {
    aiSectionHtml = `
      <div class="gemini-box">
        <div class="gemini-box-head">
          <span class="gemini-badge">⚠️ Gemini API</span>
          <button type="button" class="btn btn-sm btn-outline" id="btn-open-api-inline">Configurar clave</button>
        </div>
        <p class="card-subtitle">${escapeHtml(aiError)}</p>
      </div>`;
  } else {
    aiSectionHtml = `
      <div class="gemini-box">
        <div class="gemini-box-head">
          <span class="gemini-badge">Modo Catálogo Local</span>
          <button type="button" class="btn btn-sm btn-primary" id="btn-open-api-inline">Activar Gemini AI</button>
        </div>
        <p class="card-subtitle">Agrega tu Gemini API Key gratuita para analizar cualquier producto desconocido en tiempo real directamente desde tu navegador.</p>
      </div>`;
  }

  container.innerHTML = `
    <div class="verdict-card ${meta.css}">
      <div class="semaphore-header">
        <div class="semaphore-housing" aria-hidden="true">
          <span class="sem-bulb red ${product.verdict === 'RED' ? 'lit' : ''}"></span>
          <span class="sem-bulb yellow ${product.verdict === 'YELLOW' ? 'lit' : ''}"></span>
          <span class="sem-bulb green ${product.verdict === 'GREEN' ? 'lit' : ''}"></span>
        </div>
        <div>
          <span class="verdict-badge">${escapeHtml(meta.badge)}</span>
          <h2 class="verdict-headline">${escapeHtml(aiData?.verdictHeadline || meta.headline)}</h2>
          <div class="card-subtitle">
            <strong>${escapeHtml(product.name)}</strong> · <span class="mono">${escapeHtml(product.code)}</span>
            ${rawBarcode ? ` · Escaneado: <span class="mono">${escapeHtml(rawBarcode)}</span>` : ''}
          </div>
        </div>
      </div>

      <p class="verdict-reason">${escapeHtml(product.verdictReason)}</p>

      <div class="product-compare-grid">
        <div class="compare-cell">
          <span>En tienda</span>
          <strong class="mono">${formatUsd(product.storePrice)}</strong>
        </div>
        <div class="compare-cell">
          <span>Promedio</span>
          <strong class="mono">${formatUsd(product.averagePrice)}</strong>
        </div>
        <div class="compare-cell">
          <span>Diferencia</span>
          <strong>${escapeHtml(diffLabel)}</strong>
        </div>
      </div>
    </div>

    ${aiSectionHtml}

    <div class="card calculator-card">
      <div class="card-header">
        <div>
          <h2>Calculadora de cuotas quincenales</h2>
          <p class="card-subtitle">Ajusta el precio y plazo antes de agregar a tus cuotas</p>
        </div>
      </div>

      <div class="two-col">
        <div>
          <label for="calc-total-input" class="input-label">Precio total (USD)</label>
          <input type="number" id="calc-total-input" class="text-input mono" min="1" step="0.01" value="${total.toFixed(2)}" />
        </div>
        <div>
          <label for="calc-quotas-select" class="input-label">Número de pagos</label>
          <select id="calc-quotas-select" class="text-input">
            ${[2, 4, 6, 8, 12]
              .map((q) => `<option value="${q}" ${q === quotas ? 'selected' : ''}>${q} quincenas</option>`)
              .join('')}
          </select>
        </div>
      </div>

      <label style="display:flex;align-items:center;gap:8px;margin-top:10px;font-size:0.84rem;cursor:pointer;">
        <input type="checkbox" id="calc-initial-check" ${currentVerification.calcIncludeInitial ? 'checked' : ''} />
        <span>Incluir pago inicial sugerido del 25% (${formatUsd(initialAmt)})</span>
      </label>

      <div class="quota-summary-banner">
        <div>
          <span class="card-subtitle" style="display:block;">Cuota cada 15 días</span>
          <strong class="mono" id="calc-biweekly-display">${formatUsd(biweeklyAmt)}</strong>
        </div>
        <button type="button" class="btn btn-primary" id="btn-add-verified-quota">+ Agregar a cuotas</button>
      </div>
    </div>

    ${
      history.length
        ? `<div class="card">
            <div class="card-header">
              <h2>Últimos precios escaneados</h2>
              <span class="pill-count">${history.length}</span>
            </div>
            <div class="stack-sm">
              ${history
                .slice()
                .reverse()
                .map(
                  (h) => `
                  <div class="alert-row">
                    <span class="mono">${escapeHtml(h.barcode)}</span>
                    <strong class="mono">${formatUsd(h.price)}</strong>
                  </div>`
                )
                .join('')}
            </div>
          </div>`
        : ''
    }
  `;

  const inlineApiBtn = document.getElementById('btn-open-api-inline');
  if (inlineApiBtn) {
    inlineApiBtn.addEventListener('click', () => openModal('api-modal'));
  }

  const calcTotalInput = document.getElementById('calc-total-input');
  const calcQuotasSelect = document.getElementById('calc-quotas-select');
  const calcInitialCheck = document.getElementById('calc-initial-check');
  const calcDisplay = document.getElementById('calc-biweekly-display');

  const updateCalcPreview = () => {
    const t = Math.max(1, Number(calcTotalInput.value) || 0);
    const q = Math.max(2, Number(calcQuotasSelect.value) || 6);
    const initCheck = calcInitialCheck.checked;
    currentVerification.calcTotal = t;
    currentVerification.calcQuotas = q;
    currentVerification.calcIncludeInitial = initCheck;
    const initVal = initCheck ? t * 0.25 : 0;
    const perQuota = (t - initVal) / q;
    calcDisplay.textContent = formatUsd(perQuota);
  };

  calcTotalInput?.addEventListener('input', updateCalcPreview);
  calcQuotasSelect?.addEventListener('change', updateCalcPreview);
  calcInitialCheck?.addEventListener('change', updateCalcPreview);

  document.getElementById('btn-add-verified-quota')?.addEventListener('click', () => {
    const t = Math.max(1, Number(calcTotalInput.value) || product.storePrice);
    const q = Math.max(2, Number(calcQuotasSelect.value) || 6);
    const initVal = calcInitialCheck.checked ? t * 0.25 : 0;
    const perQuota = (t - initVal) / q;

    appState.installments.unshift({
      id: 'inst-' + Date.now(),
      title: product.name,
      code: product.code,
      category: product.category || 'General',
      initialPaid: Number(initVal.toFixed(2)),
      totalAmount: Number(t.toFixed(2)),
      biweeklyQuota: Number(perQuota.toFixed(2)),
      totalQuotas: q,
      paidQuotas: 0,
      daysUntilDue: 15
    });
    saveState();
    renderAll();
    showToast(`Agregado "${product.name}" en ${q} cuotas de ${formatUsd(perQuota)}`);
    switchTab('cuotas');
  });
}

function renderDashboard() {
  const active = appState.installments.filter((i) => i.paidQuotas < i.totalQuotas);
  const biweeklyTotal = active.reduce((sum, i) => sum + i.biweeklyQuota, 0);
  const availableLeft = Math.max(0, appState.budget - biweeklyTotal);
  const pct = appState.budget > 0 ? Math.min(100, Math.round((biweeklyTotal / appState.budget) * 100)) : 0;

  document.getElementById('dash-budget-amount').textContent = formatUsd(appState.budget);
  document.getElementById('dash-biweekly-total').textContent = formatUsd(biweeklyTotal);
  document.getElementById('dash-available-left').textContent = formatUsd(availableLeft);
  document.getElementById('dash-budget-bar').style.width = `${pct}%`;
  document.getElementById('dash-budget-caption').textContent = `${pct}% de tu quincena comprometida (${active.length} planes activos)`;

  // Alerts
  const upcoming = [...active].sort((a, b) => a.daysUntilDue - b.daysUntilDue).slice(0, 4);
  document.getElementById('alerts-count-badge').textContent = String(upcoming.length);
  const alertsList = document.getElementById('upcoming-alerts-list');
  if (!upcoming.length) {
    alertsList.innerHTML = '<p class="card-subtitle">No tienes cuotas pendientes por vencer.</p>';
  } else {
    alertsList.innerHTML = upcoming
      .map((item) => {
        const dueText = item.daysUntilDue <= 0 ? 'Vence hoy' : item.daysUntilDue === 1 ? 'Vence mañana' : `En ${item.daysUntilDue} días`;
        return `
          <div class="alert-row">
            <div>
              <strong>${escapeHtml(item.title)}</strong>
              <div class="card-subtitle">${escapeHtml(dueText)} · ${escapeHtml(item.category)}</div>
            </div>
            <strong class="mono">${formatUsd(item.biweeklyQuota)}</strong>
          </div>`;
      })
      .join('');
  }

  renderCategoryPieChart();
}

function renderCategoryPieChart() {
  const relevant = chartMode === 'biweekly'
    ? appState.installments.filter((i) => i.paidQuotas < i.totalQuotas && i.biweeklyQuota > 0)
    : appState.installments.filter((i) => i.totalAmount > 0);

  const svgWrap = document.getElementById('category-pie-wrap');
  const legendWrap = document.getElementById('category-pie-legend');

  if (!relevant.length) {
    svgWrap.innerHTML = '';
    legendWrap.innerHTML = '<p class="card-subtitle">Agrega cuotas para ver la distribución por categoría.</p>';
    return;
  }

  const map = new Map();
  relevant.forEach((item) => {
    const cat = (item.category || 'General').trim();
    const val = chartMode === 'biweekly' ? item.biweeklyQuota : item.totalAmount;
    map.set(cat, (map.get(cat) || 0) + val);
  });

  const slices = [...map.entries()]
    .map(([category, amount]) => ({ category, amount, color: categoryColor(category) }))
    .sort((a, b) => b.amount - a.amount);

  const total = slices.reduce((s, x) => s + x.amount, 0);
  const radius = 54;
  const circumference = 2 * Math.PI * radius;
  let offset = 0;

  const circles = slices
    .map((slice) => {
      const fraction = slice.amount / total;
      const dash = fraction * circumference;
      const circle = `<circle cx="75" cy="75" r="${radius}" fill="transparent" stroke="${slice.color}" stroke-width="24" stroke-dasharray="${dash.toFixed(2)} ${circumference.toFixed(2)}" stroke-dashoffset="${(-offset).toFixed(2)}" transform="rotate(-90 75 75)" />`;
      offset += dash;
      return circle;
    })
    .join('');

  svgWrap.innerHTML = `
    <svg viewBox="0 0 150 150" width="150" height="150" role="img" aria-label="Gráfico de pastel por categoría">
      ${circles}
      <text x="75" y="71" text-anchor="middle" fill="currentColor" font-size="11" font-weight="700" opacity="0.68">${chartMode === 'biweekly' ? 'Quincena' : 'Total'}</text>
      <text x="75" y="88" text-anchor="middle" fill="currentColor" font-size="14" font-weight="800">${formatUsd(total)}</text>
    </svg>
  `;

  legendWrap.innerHTML = slices
    .map((slice) => {
      const pct = Math.round((slice.amount / total) * 100);
      return `
        <div class="legend-row">
          <div class="legend-left">
            <span class="legend-swatch" style="background:${slice.color}"></span>
            <span>${escapeHtml(slice.category)}</span>
          </div>
          <strong class="mono">${pct}% · ${formatUsd(slice.amount)}</strong>
        </div>`;
    })
    .join('');
}

function renderInstallments() {
  const filtersContainer = document.getElementById('cuotas-category-filters');
  const listContainer = document.getElementById('installments-list');

  const categories = ['Todas', ...new Set(appState.installments.map((i) => i.category || 'General'))];
  filtersContainer.innerHTML = categories
    .map(
      (cat) =>
        `<button type="button" class="chip-btn ${cat === categoryFilter ? 'active' : ''}" data-filter-cat="${escapeHtml(cat)}">${escapeHtml(cat)}</button>`
    )
    .join('');

  filtersContainer.querySelectorAll('[data-filter-cat]').forEach((btn) => {
    btn.addEventListener('click', () => {
      categoryFilter = btn.getAttribute('data-filter-cat');
      renderInstallments();
    });
  });

  const filtered = categoryFilter === 'Todas'
    ? appState.installments
    : appState.installments.filter((i) => (i.category || 'General') === categoryFilter);

  if (!filtered.length) {
    listContainer.innerHTML = '<div class="card"><p class="card-subtitle">No hay cuotas en esta categoría.</p></div>';
    return;
  }

  listContainer.innerHTML = filtered
    .map((item) => {
      const pct = item.totalQuotas > 0 ? Math.min(100, Math.round((item.paidQuotas / item.totalQuotas) * 100)) : 100;
      const done = item.paidQuotas >= item.totalQuotas;
      const color = categoryColor(item.category);
      return `
        <article class="installment-card">
          <div class="installment-top">
            <div>
              <strong>${escapeHtml(item.title)}</strong>
              <div class="card-subtitle mono">${escapeHtml(item.code)} · Total ${formatUsd(item.totalAmount)}</div>
            </div>
            <button type="button" class="category-tag-btn" data-edit-cat="${escapeHtml(item.id)}" style="border-left:4px solid ${color};">
              🏷️ ${escapeHtml(item.category || 'General')}
            </button>
          </div>

          <div class="progress-track">
            <div class="progress-fill" style="width:${pct}%"></div>
          </div>

          <div class="installment-meta">
            <span>${item.paidQuotas}/${item.totalQuotas} cuotas pagadas</span>
            <strong class="mono">${done ? '¡Liquidado!' : `${formatUsd(item.biweeklyQuota)} / quincena`}</strong>
          </div>

          <div class="installment-actions">
            ${
              done
                ? ''
                : `<button type="button" class="btn btn-sm btn-primary flex-1" data-pay-id="${escapeHtml(item.id)}">✓ Registrar pago quincenal</button>`
            }
            <button type="button" class="btn btn-sm btn-outline" data-delete-id="${escapeHtml(item.id)}">Eliminar</button>
          </div>
        </article>
      `;
    })
    .join('');

  listContainer.querySelectorAll('[data-pay-id]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const id = btn.getAttribute('data-pay-id');
      const target = appState.installments.find((i) => i.id === id);
      if (target && target.paidQuotas < target.totalQuotas) {
        target.paidQuotas += 1;
        target.daysUntilDue = 15;
        saveState();
        renderAll();
        showToast(`Pago registrado para ${target.title}`);
      }
    });
  });

  listContainer.querySelectorAll('[data-delete-id]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const id = btn.getAttribute('data-delete-id');
      appState.installments = appState.installments.filter((i) => i.id !== id);
      saveState();
      renderAll();
      showToast('Plan eliminado');
    });
  });

  listContainer.querySelectorAll('[data-edit-cat]').forEach((btn) => {
    btn.addEventListener('click', () => {
      editingCategoryInstallmentId = btn.getAttribute('data-edit-cat');
      openCategoryModal();
    });
  });
}

function renderChat() {
  const box = document.getElementById('chat-messages-box');
  if (!appState.chatMessages.length) {
    box.innerHTML = `
      <div class="chat-bubble assistant">
        ¡Hola! Soy tu asistente financiero PVS. Pregúntame si te conviene comprar un producto a cuotas o cómo optimizar tu presupuesto quincenal de ${formatUsd(appState.budget)}.
      </div>`;
    return;
  }
  box.innerHTML = appState.chatMessages
    .map(
      (msg) => `<div class="chat-bubble ${msg.role === 'user' ? 'user' : 'assistant'}">${escapeHtml(msg.text)}</div>`
    )
    .join('');
}

function renderThemeAndApiStatus() {
  document.documentElement.setAttribute('data-theme', appState.darkMode ? 'dark' : 'light');
  document.getElementById('theme-icon').textContent = appState.darkMode ? '☀️' : '🌙';
  const metaTheme = document.querySelector('meta[name="theme-color"]');
  if (metaTheme) metaTheme.setAttribute('content', appState.darkMode ? '#0A1710' : '#1B5E3A');
  updateApiStatusUi();
}

function updateApiStatusUi() {
  const hasKey = Boolean(getGeminiKey());
  const dot = document.getElementById('api-status-dot');
  const subtitle = document.getElementById('header-subtitle');
  const assistantLabel = document.getElementById('assistant-mode-label');

  if (dot) dot.classList.toggle('connected', hasKey);
  if (subtitle) subtitle.textContent = hasKey ? 'Gemini AI conectado' : 'Verificador web y cuotas';
  if (assistantLabel) {
    assistantLabel.textContent = hasKey
      ? `Conectado a ${appState.geminiModel} con tu presupuesto actual`
      : 'Modo local activo (toca ✨ arriba para conectar Gemini API)';
  }
}

function renderAll() {
  renderThemeAndApiStatus();
  renderDashboard();
  renderVerifierResult();
  renderInstallments();
  renderChat();
}

function switchTab(tabName) {
  activeTab = tabName;
  document.querySelectorAll('.tab-panel').forEach((panel) => {
    const isTarget = panel.getAttribute('data-tab') === tabName;
    panel.hidden = !isTarget;
    panel.classList.toggle('active', isTarget);
  });
  document.querySelectorAll('.nav-item').forEach((nav) => {
    nav.classList.toggle('active', nav.getAttribute('data-nav') === tabName);
  });
}

// --- Modal Helpers ---

function openModal(id) {
  const el = document.getElementById(id);
  if (el) el.hidden = false;
}

function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.hidden = true;
}

function openCategoryModal() {
  const wrap = document.getElementById('category-preset-chips');
  wrap.innerHTML = CATEGORY_PRESETS.map(
    (cat) => `<button type="button" class="chip-btn" data-pick-cat="${escapeHtml(cat)}">${escapeHtml(cat)}</button>`
  ).join('');
  wrap.querySelectorAll('[data-pick-cat]').forEach((btn) => {
    btn.addEventListener('click', () => applyCategoryTag(btn.getAttribute('data-pick-cat')));
  });
  document.getElementById('custom-category-input').value = '';
  openModal('category-modal');
}

function applyCategoryTag(categoryName) {
  const clean = String(categoryName || '').trim().slice(0, 32) || 'General';
  const item = appState.installments.find((i) => i.id === editingCategoryInstallmentId);
  if (item) {
    item.category = clean;
    saveState();
    renderAll();
    showToast(`Categoría actualizada a "${clean}"`);
  }
  closeModal('category-modal');
}

// --- Web Camera Barcode Scanner (Native BarcodeDetector + ZXing Fallback) ---

async function openWebScanner() {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    showToast('Tu navegador no permite cámara en vivo aquí; usa "Foto" para escanear una imagen.');
    return;
  }

  openModal('scanner-modal');
  scannerActive = true;
  torchEnabled = false;
  await startCameraStream();
}

function stopWebScanner() {
  scannerActive = false;
  if (scannerRafId) {
    cancelAnimationFrame(scannerRafId);
    scannerRafId = null;
  }
  if (zxingControls && typeof zxingControls.stop === 'function') {
    try { zxingControls.stop(); } catch (_) {}
    zxingControls = null;
  }
  if (cameraStream) {
    cameraStream.getTracks().forEach((track) => track.stop());
    cameraStream = null;
  }
  const canvas = document.getElementById('scanner-overlay-canvas');
  if (canvas) {
    const ctx = canvas.getContext('2d');
    ctx?.clearRect(0, 0, canvas.width, canvas.height);
  }
  closeModal('scanner-modal');
}

async function startCameraStream() {
  if (cameraStream) {
    cameraStream.getTracks().forEach((t) => t.stop());
    cameraStream = null;
  }
  if (zxingControls && typeof zxingControls.stop === 'function') {
    try { zxingControls.stop(); } catch (_) {}
    zxingControls = null;
  }

  const video = document.getElementById('scanner-video');
  const engineLabel = document.getElementById('scanner-engine-label');
  const hintPill = document.getElementById('scanner-hint-pill');

  try {
    cameraStream = await navigator.mediaDevices.getUserMedia({
      video: {
        facingMode: { ideal: useFrontCamera ? 'user' : 'environment' },
        width: { ideal: 1280 },
        height: { ideal: 720 }
      },
      audio: false
    });
    video.srcObject = cameraStream;
    await video.play();
  } catch (err) {
    hintPill.textContent = 'No se pudo abrir la cámara. Permite el acceso o usa el botón 🖼️ de imagen.';
    engineLabel.textContent = 'Permiso de cámara requerido';
    return;
  }

  if ('BarcodeDetector' in window) {
    try {
      const supported = await window.BarcodeDetector.getSupportedFormats();
      const desired = ['ean_13', 'ean_8', 'upc_a', 'upc_e', 'code_128', 'code_39', 'qr_code', 'itf'].filter((f) =>
        supported.includes(f)
      );
      const detector = new window.BarcodeDetector({ formats: desired.length ? desired : supported });
      engineLabel.textContent = 'Motor nativo BarcodeDetector activo';
      runNativeBarcodeLoop(video, detector);
      return;
    } catch (e) {
      console.warn('BarcodeDetector no disponible, cargando ZXing...', e);
    }
  }

  // Fallback for iOS Safari / Firefox via ZXing ESM
  try {
    engineLabel.textContent = 'Cargando motor ZXing para navegador...';
    const { BrowserMultiFormatReader } = await import('https://cdn.jsdelivr.net/npm/@zxing/browser@0.1.5/+esm');
    if (!scannerActive) return;
    const reader = new BrowserMultiFormatReader();
    engineLabel.textContent = 'Escáner ZXing activo';
    zxingControls = await reader.decodeFromVideoElement(video, (result) => {
      if (result && scannerActive) {
        const value = result.getText();
        handleDetectedBarcode(value, result.getBarcodeFormat?.()?.toString() || 'Código');
      }
    });
  } catch (err) {
    engineLabel.textContent = 'Usa el ícono 🖼️ para subir foto del código';
    hintPill.textContent = 'Tu navegador requiere subir foto del código de barras.';
  }
}

function runNativeBarcodeLoop(video, detector) {
  const canvas = document.getElementById('scanner-overlay-canvas');
  const ctx = canvas?.getContext('2d');

  const scanFrame = async () => {
    if (!scannerActive) return;
    if (video.readyState >= 2) {
      try {
        const results = await detector.detect(video);
        if (canvas && ctx) {
          canvas.width = video.clientWidth;
          canvas.height = video.clientHeight;
          ctx.clearRect(0, 0, canvas.width, canvas.height);
        }
        if (results && results.length > 0) {
          const hit = results[0];
          if (hit.rawValue) {
            if (ctx && hit.boundingBox && video.videoWidth && video.videoHeight) {
              const scaleX = canvas.width / video.videoWidth;
              const scaleY = canvas.height / video.videoHeight;
              ctx.strokeStyle = '#4CD478';
              ctx.lineWidth = 4;
              ctx.strokeRect(
                hit.boundingBox.x * scaleX,
                hit.boundingBox.y * scaleY,
                hit.boundingBox.width * scaleX,
                hit.boundingBox.height * scaleY
              );
            }
            handleDetectedBarcode(hit.rawValue, hit.format || 'Código');
            return;
          }
        }
      } catch (_) {}
    }
    scannerRafId = requestAnimationFrame(scanFrame);
  };
  scannerRafId = requestAnimationFrame(scanFrame);
}

function handleDetectedBarcode(rawValue, formatLabel) {
  const clean = String(rawValue || '').trim();
  if (!clean || !scannerActive) return;

  if (navigator.vibrate) {
    navigator.vibrate(85);
  }
  const hintPill = document.getElementById('scanner-hint-pill');
  if (hintPill) {
    hintPill.textContent = `✓ ${String(formatLabel).toUpperCase()} · ${clean}`;
  }

  setTimeout(() => {
    stopWebScanner();
    switchTab('verificador');
    const resolved = resolveBarcodeValue(clean);
    document.getElementById('search-code-input').value = resolved;
    const priceInputVal = Number(document.getElementById('store-price-input').value);
    const storePriceOverride = priceInputVal > 0 ? priceInputVal : null;
    verifyProductQuery(clean, { rawBarcode: clean, storePriceOverride });
  }, 240);
}

async function decodeImageFile(file) {
  if (!file) return;
  showToast('Leyendo código de la imagen...');
  const imgUrl = URL.createObjectURL(file);
  try {
    const img = new Image();
    img.src = imgUrl;
    await new Promise((res, rej) => {
      img.onload = res;
      img.onerror = rej;
    });

    if ('BarcodeDetector' in window) {
      const detector = new window.BarcodeDetector();
      const hits = await detector.detect(img);
      if (hits && hits.length && hits[0].rawValue) {
        if (scannerActive) stopWebScanner();
        switchTab('verificador');
        document.getElementById('search-code-input').value = resolveBarcodeValue(hits[0].rawValue);
        verifyProductQuery(hits[0].rawValue, { rawBarcode: hits[0].rawValue });
        return;
      }
    }

    const { BrowserMultiFormatReader } = await import('https://cdn.jsdelivr.net/npm/@zxing/browser@0.1.5/+esm');
    const reader = new BrowserMultiFormatReader();
    const res = await reader.decodeFromImageElement(img);
    if (res && res.getText()) {
      if (scannerActive) stopWebScanner();
      switchTab('verificador');
      const code = res.getText();
      document.getElementById('search-code-input').value = resolveBarcodeValue(code);
      verifyProductQuery(code, { rawBarcode: code });
      return;
    }
    showToast('No se detectó un código claro en esa foto.');
  } catch (err) {
    showToast('No pude leer el código en la imagen. Intenta acercar más la cámara.');
  } finally {
    URL.revokeObjectURL(imgUrl);
  }
}

// --- Event Wiring & Init ---

function initEvents() {
  // Bottom Nav
  document.querySelectorAll('.nav-item').forEach((btn) => {
    btn.addEventListener('click', () => switchTab(btn.getAttribute('data-nav')));
  });

  // Home CTA
  document.getElementById('btn-home-scan').addEventListener('click', () => {
    switchTab('verificador');
    openWebScanner();
  });

  // Verifier Search + Autocomplete
  const searchInput = document.getElementById('search-code-input');
  const dropdown = document.getElementById('autocomplete-dropdown');

  const renderDropdown = () => {
    const matches = buildAutocomplete(searchInput.value);
    if (!matches.length) {
      dropdown.hidden = true;
      return;
    }
    dropdown.innerHTML = matches
      .map(
        (m) => `
        <button type="button" class="autocomplete-item" data-ac-code="${escapeHtml(m.code)}">
          <div>
            <strong>${escapeHtml(m.name)}</strong>
            <div class="card-subtitle mono">EAN: ${escapeHtml(m.barcode)}</div>
          </div>
          <span class="autocomplete-code mono">${escapeHtml(m.code)}</span>
        </button>`
      )
      .join('');
    dropdown.hidden = false;

    dropdown.querySelectorAll('[data-ac-code]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const code = btn.getAttribute('data-ac-code');
        searchInput.value = code;
        dropdown.hidden = true;
        const priceVal = Number(document.getElementById('store-price-input').value);
        verifyProductQuery(code, { storePriceOverride: priceVal > 0 ? priceVal : null });
      });
    });
  };

  searchInput.addEventListener('focus', renderDropdown);
  searchInput.addEventListener('input', renderDropdown);
  searchInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      dropdown.hidden = true;
      const priceVal = Number(document.getElementById('store-price-input').value);
      verifyProductQuery(searchInput.value, { storePriceOverride: priceVal > 0 ? priceVal : null });
    }
  });

  document.addEventListener('click', (e) => {
    if (!e.target.closest('.search-field-wrap')) {
      dropdown.hidden = true;
    }
  });

  document.getElementById('btn-submit-search').addEventListener('click', () => {
    dropdown.hidden = true;
    const priceVal = Number(document.getElementById('store-price-input').value);
    verifyProductQuery(searchInput.value, { storePriceOverride: priceVal > 0 ? priceVal : null });
  });

  document.querySelectorAll('#example-chips-row .chip-btn').forEach((chip) => {
    chip.addEventListener('click', () => {
      const code = chip.getAttribute('data-code');
      searchInput.value = code;
      dropdown.hidden = true;
      const priceVal = Number(document.getElementById('store-price-input').value);
      verifyProductQuery(code, { storePriceOverride: priceVal > 0 ? priceVal : null });
    });
  });

  // Scanner Controls
  document.getElementById('btn-open-scanner').addEventListener('click', openWebScanner);
  document.getElementById('btn-close-scanner').addEventListener('click', stopWebScanner);
  document.getElementById('btn-scanner-flip').addEventListener('click', async () => {
    useFrontCamera = !useFrontCamera;
    await startCameraStream();
  });
  document.getElementById('btn-scanner-torch').addEventListener('click', async () => {
    const track = cameraStream?.getVideoTracks?.()[0];
    if (!track) return;
    try {
      const caps = track.getCapabilities?.();
      if (!caps || !caps.torch) {
        showToast('Este teléfono/navegador no expone control de linterna web.');
        return;
      }
      torchEnabled = !torchEnabled;
      await track.applyConstraints({ advanced: [{ torch: torchEnabled }] });
      showToast(torchEnabled ? 'Linterna encendida' : 'Linterna apagada');
    } catch (_) {
      showToast('No se pudo activar la linterna.');
    }
  });

  document.getElementById('photo-barcode-input').addEventListener('change', (e) => {
    decodeImageFile(e.target.files?.[0]);
    e.target.value = '';
  });
  document.getElementById('photo-barcode-input-modal').addEventListener('change', (e) => {
    decodeImageFile(e.target.files?.[0]);
    e.target.value = '';
  });

  // Pie Chart Mode
  document.querySelectorAll('.seg-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      chartMode = btn.getAttribute('data-chart-mode');
      document.querySelectorAll('.seg-btn').forEach((b) => b.classList.toggle('active', b === btn));
      renderCategoryPieChart();
    });
  });

  // Theme Toggle
  document.getElementById('btn-theme-toggle').addEventListener('click', () => {
    appState.darkMode = !appState.darkMode;
    saveState();
    renderThemeAndApiStatus();
  });

  // API Key Modal
  document.getElementById('btn-api-settings').addEventListener('click', () => {
    document.getElementById('gemini-api-key-input').value = getGeminiKey();
    document.getElementById('gemini-model-select').value = appState.geminiModel || 'gemini-2.5-flash';
    openModal('api-modal');
  });
  document.getElementById('btn-save-api-key').addEventListener('click', () => {
    const keyVal = document.getElementById('gemini-api-key-input').value;
    appState.geminiModel = document.getElementById('gemini-model-select').value;
    setGeminiKey(keyVal);
    saveState();
    closeModal('api-modal');
    renderAll();
    showToast(keyVal.trim() ? 'Gemini API Key guardada en este dispositivo' : 'Modo local activado');
  });
  document.getElementById('btn-clear-api-key').addEventListener('click', () => {
    document.getElementById('gemini-api-key-input').value = '';
    setGeminiKey('');
    closeModal('api-modal');
    renderAll();
    showToast('API Key eliminada');
  });

  // Budget Modal
  document.getElementById('btn-edit-budget').addEventListener('click', () => {
    document.getElementById('budget-amount-input').value = appState.budget;
    openModal('budget-modal');
  });
  document.getElementById('budget-form').addEventListener('submit', (e) => {
    e.preventDefault();
    const val = Number(document.getElementById('budget-amount-input').value);
    if (val >= 20) {
      appState.budget = val;
      saveState();
      renderAll();
      closeModal('budget-modal');
      showToast('Presupuesto actualizado');
    }
  });

  // Add Installment Modal
  const addQuotaTotal = document.getElementById('quota-total-input');
  const addQuotaCount = document.getElementById('quota-count-input');
  const updateAddPreview = () => {
    const total = Number(addQuotaTotal.value) || 0;
    const count = Number(addQuotaCount.value) || 4;
    const init = total * 0.25;
    const biweekly = total > 0 ? (total - init) / count : 0;
    document.getElementById('add-quota-preview').textContent =
      `Inicial 25%: ${formatUsd(init)} · ${count} pagos de ${formatUsd(biweekly)}/quincena`;
  };
  addQuotaTotal.addEventListener('input', updateAddPreview);
  addQuotaCount.addEventListener('change', updateAddPreview);

  document.getElementById('btn-open-add-quota').addEventListener('click', () => openModal('add-quota-modal'));
  document.getElementById('add-quota-form').addEventListener('submit', (e) => {
    e.preventDefault();
    const title = document.getElementById('quota-title-input').value.trim();
    const code = document.getElementById('quota-code-input').value.trim().toUpperCase() || `PVS-${appState.installments.length + 10}`;
    const category = document.getElementById('quota-category-select').value;
    const total = Math.max(1, Number(addQuotaTotal.value) || 0);
    const count = Math.max(2, Number(addQuotaCount.value) || 4);
    const init = Number((total * 0.25).toFixed(2));
    const biweekly = Number(((total - init) / count).toFixed(2));

    appState.installments.unshift({
      id: 'inst-' + Date.now(),
      title,
      code,
      category,
      initialPaid: init,
      totalAmount: Number(total.toFixed(2)),
      biweeklyQuota: biweekly,
      totalQuotas: count,
      paidQuotas: 0,
      daysUntilDue: 15
    });
    saveState();
    renderAll();
    closeModal('add-quota-modal');
    e.target.reset();
    showToast(`Cuota "${title}" agregada`);
  });

  // Custom Category Form
  document.getElementById('custom-category-form').addEventListener('submit', (e) => {
    e.preventDefault();
    applyCategoryTag(document.getElementById('custom-category-input').value);
  });

  // Close Modal Buttons
  document.querySelectorAll('[data-close-modal]').forEach((btn) => {
    btn.addEventListener('click', () => closeModal(btn.getAttribute('data-close-modal')));
  });

  // Tutorial
  const showTutorialStep = (idx) => {
    tutorialStepIdx = idx;
    const step = TUTORIAL_STEPS[idx];
    document.getElementById('tutorial-step-counter').textContent = `Paso ${idx + 1} de ${TUTORIAL_STEPS.length}`;
    document.getElementById('tutorial-title').textContent = step.title;
    document.getElementById('tutorial-body').textContent = step.body;
    document.getElementById('btn-tutorial-next').textContent =
      idx === TUTORIAL_STEPS.length - 1 ? '¡Entendido!' : 'Siguiente →';
    openModal('tutorial-modal');
  };

  document.getElementById('btn-tutorial').addEventListener('click', () => showTutorialStep(0));
  document.getElementById('btn-tutorial-next').addEventListener('click', () => {
    if (tutorialStepIdx >= TUTORIAL_STEPS.length - 1) {
      appState.tutorialSeen = true;
      saveState();
      closeModal('tutorial-modal');
    } else {
      showTutorialStep(tutorialStepIdx + 1);
    }
  });
  document.getElementById('btn-tutorial-skip').addEventListener('click', () => {
    appState.tutorialSeen = true;
    saveState();
    closeModal('tutorial-modal');
  });

  // Chat
  const sendChat = async (text) => {
    const clean = String(text || '').trim();
    if (!clean) return;
    appState.chatMessages.push({ role: 'user', text: clean });
    appState.chatMessages.push({ role: 'assistant', text: 'Pensando...' });
    renderChat();

    try {
      const reply = await callGeminiChat(clean);
      appState.chatMessages[appState.chatMessages.length - 1] = { role: 'assistant', text: reply };
    } catch (err) {
      appState.chatMessages[appState.chatMessages.length - 1] = {
        role: 'assistant',
        text: `Error al consultar Gemini: ${err.message}`
      };
    }
    saveState();
    renderChat();
  };

  document.getElementById('chat-form').addEventListener('submit', (e) => {
    e.preventDefault();
    const input = document.getElementById('chat-input');
    const val = input.value;
    input.value = '';
    sendChat(val);
  });

  document.querySelectorAll('.chat-prompt-chip').forEach((chip) => {
    chip.addEventListener('click', () => sendChat(chip.getAttribute('data-prompt')));
  });

  document.getElementById('btn-clear-chat').addEventListener('click', () => {
    appState.chatMessages = [];
    saveState();
    renderChat();
  });

  // PWA Install Prompt
  const installBtn = document.getElementById('btn-install-pwa');
  const installBanner = document.getElementById('install-banner');

  const isIosSafari =
    /iphone|ipad|ipod/i.test(navigator.userAgent) &&
    !window.matchMedia('(display-mode: standalone)').matches &&
    navigator.standalone !== true;
  if (isIosSafari) {
    installBtn.hidden = false;
  }

  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredInstallPrompt = e;
    installBtn.hidden = false;
    installBanner.hidden = false;
  });

  const triggerInstall = async () => {
    if (deferredInstallPrompt) {
      deferredInstallPrompt.prompt();
      const choice = await deferredInstallPrompt.userChoice;
      if (choice.outcome === 'accepted') {
        installBanner.hidden = true;
        installBtn.hidden = true;
      }
      deferredInstallPrompt = null;
    } else {
      showToast('En Safari iOS: toca Compartir → "Agregar a pantalla de inicio".');
    }
  };

  installBtn.addEventListener('click', triggerInstall);
  document.getElementById('btn-banner-install').addEventListener('click', triggerInstall);
  document.getElementById('btn-banner-close').addEventListener('click', () => {
    installBanner.hidden = true;
  });

  // URL shortcut parameters
  const params = new URLSearchParams(window.location.search);
  const tabParam = params.get('tab');
  if (tabParam && ['inicio', 'verificador', 'cuotas', 'asistente'].includes(tabParam)) {
    switchTab(tabParam);
  }
  if (params.get('scan') === '1') {
    openWebScanner();
  }
}

function registerServiceWorker() {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch((err) => {
      console.warn('Service Worker no registrado:', err);
    });
  }
}

document.addEventListener('DOMContentLoaded', () => {
  initEvents();
  renderAll();
  registerServiceWorker();
});
