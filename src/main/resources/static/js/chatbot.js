(function () {
  'use strict';

  /* ── i18n strings ── */
  var STRINGS = {
    en: {
      greeting:    "Hello! I'm your Dushanbe Assistant. How can I help you today?",
      quick_label: "Quick actions",
      emergency:   "🚨 Emergency",
      places:      "📍 Recommend Places",
      hours:       "🕐 Operating Hours",
      placeholder: "Ask me anything...",
      nudge:       "Need help with your trip?",
      verified:    "Verified Source",
      no_data:     "I couldn't find that in our records. Would you like to try one of these?",
      err_conn:    "Connection lost. Please try again.",
      source:      "Source",
      online:      "Online"
    },
    ru: {
      greeting:    "Привет! Я ваш ассистент в Душанбе. Чем могу помочь?",
      quick_label: "Быстрые действия",
      emergency:   "🚨 Экстренные службы",
      places:      "📍 Места для посещения",
      hours:       "🕐 Часы работы",
      placeholder: "Задайте вопрос...",
      nudge:       "Нужна помощь в путешествии?",
      verified:    "Проверенный источник",
      no_data:     "Не нашёл в базе. Попробуйте:",
      err_conn:    "Нет соединения. Попробуйте снова.",
      source:      "Источник",
      online:      "Онлайн"
    },
    tg: {
      greeting:    "Салом! Ман ёрдамчии шумо дар Душанбе ҳастам. Чӣ гуна кӯмак карда метавонам?",
      quick_label: "Амалҳои зуд",
      emergency:   "🚨 Ёрии таъҷилӣ",
      places:      "📍 Ҷойҳои дидниро тавсия диҳ",
      hours:       "🕐 Соатҳои кор",
      placeholder: "Савол нависед...",
      nudge:       "Кӯмак лозим аст?",
      verified:    "Манбаи тасдиқшуда",
      no_data:     "Дар базаи мо ёфт нашуд. Кӯшиш кунед:",
      err_conn:    "Пайвастшавӣ қатъ шуд. Дубора кӯшиш кунед.",
      source:      "Манба",
      online:      "Онлайн"
    },
    ko: {
      greeting:    "안녕하세요! 두샨베 여행 도우미입니다. 무엇을 도와드릴까요?",
      quick_label: "빠른 질문",
      emergency:   "🚨 긴급 연락처",
      places:      "📍 장소 추천",
      hours:       "🕐 운영 시간",
      placeholder: "질문을 입력하세요...",
      nudge:       "여행에 도움이 필요하신가요?",
      verified:    "검증된 출처",
      no_data:     "데이터베이스에서 찾을 수 없습니다. 아래를 시도해보세요:",
      err_conn:    "연결이 끊겼습니다. 다시 시도해주세요.",
      source:      "출처",
      online:      "온라인"
    }
  };

  /* Quick-action preset messages per language */
  var QUICK_MSG = {
    emergency: {
      en: "emergency",
      ru: "экстренная",
      tg: "изтирорӣ",
      ko: "긴급"
    },
    places: {
      en: "parks",
      ru: "парки",
      tg: "боғ",
      ko: "공원"
    },
    hours: {
      en: "Rudaki Park opening hours",
      ru: "Рудаки часы работы",
      tg: "Боғи Рӯдакӣ соатҳои кор",
      ko: "루다키 공원 운영 시간"
    }
  };


  /* Icon per intent */
  var INTENT_ICON = {
    EMERGENCY_CONTACT: "🚨",
    PHONE_NUMBER:       "📞",
    OPERATING_HOURS:    "🕐",
    TOUR_PLACE_SEARCH:  "📍",
    GENERAL_TOURISM:    "🌍",
    UNKNOWN:            "💬"
  };

  /* Inline SVG strings */
  var SVG = {
	sparkle: '<img src="/assets/logo/favicon.png" alt="" aria-hidden="true" style="width:28px;height:28px;object-fit:contain;">',
    send:    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>',
    check:   '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>',
    close:   '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>',
    warn:    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>'
  };

  /* ── Helpers ── */
  function t(widget, key) {
    var lang = widget._cbLang || 'en';
    return (STRINGS[lang] || STRINGS.en)[key] || STRINGS.en[key] || key;
  }

  function makeEl(tag, className, innerHTML) {
    var node = document.createElement(tag);
    if (className)   node.className = className;
    if (innerHTML != null) node.innerHTML = innerHTML;
    return node;
  }

  function appendAll(parent) {
    for (var i = 1; i < arguments.length; i++) {
      if (arguments[i]) parent.appendChild(arguments[i]);
    }
    return parent;
  }

  function scrollBottom(el) {
    el.scrollTop = el.scrollHeight;
  }

  /* ── Build widget HTML ── */
  function buildHTML(container) {
    var lang = container._cbLang;
    var str  = STRINGS[lang] || STRINGS.en;

    var langBtns = ['en', 'ru', 'tg', 'ko'].map(function (l) {
      var label = l === 'tg' ? 'TJ' : l.toUpperCase();
      var active = l === lang ? ' active' : '';
      return '<button class="cb-lang-btn' + active + '" data-cb-lang="' + l + '">' + label + '</button>';
    }).join('');

    container.innerHTML =
      /* FAB */
      '<div class="cb-fab-wrap">' +
        '<div class="cb-notif-dot" data-cb-notif></div>' +
        '<button class="cb-fab" data-cb-toggle aria-label="Open assistant" aria-expanded="false">' +
          SVG.sparkle +
        '</button>' +
        '<div class="cb-nudge" data-cb-nudge hidden>' + str.nudge + '</div>' +
      '</div>' +

      /* Panel */
      '<div class="cb-panel" data-cb-panel hidden>' +

        /* Header */
        '<div class="cb-header">' +
          '<div class="cb-header-left">' +
		  '<div class="cb-bot-avatar"><img src="/assets/logo/favicon.png" alt="Assistant" style="width:22px;height:22px;object-fit:contain;"></div>' +
            '<div>' +
              '<div class="cb-header-title">Dushanbe Assistant</div>' +
              '<div class="cb-header-sub">' + str.online + '</div>' +
            '</div>' +
          '</div>' +
          '<div class="cb-header-right">' +
            '<div class="cb-lang-pills">' + langBtns + '</div>' +
            '<button class="cb-close-btn" data-cb-close aria-label="Close">' + SVG.close + '</button>' +
          '</div>' +
        '</div>' +

        /* Messages */
        '<div class="cb-messages" data-cb-messages></div>' +

        /* Input */
        '<div class="cb-input-area">' +
          '<input class="cb-input" data-cb-input type="text"' +
            ' placeholder="' + str.placeholder + '"' +
            ' autocomplete="off" maxlength="1000">' +
          '<button class="cb-send-btn" data-cb-send aria-label="Send">' + SVG.send + '</button>' +
        '</div>' +

      '</div>';
  }

  /* ── Quick pill ── */
  function makeQuickPill(widget, key, label) {
    var btn = makeEl('button', 'cb-quick-pill', label);
    btn.addEventListener('click', function () {
      var text = (QUICK_MSG[key] || {})[widget._cbLang] || (QUICK_MSG[key] || {}).en;
      if (text) sendMessage(widget, text);
    });
    return btn;
  }

  /* ── Welcome message ── */
  function renderWelcome(widget) {
    var msgs = widget.querySelector('[data-cb-messages]');

    var pills = makeEl('div', 'cb-quick-pills');
    appendAll(pills,
      makeQuickPill(widget, 'emergency', t(widget, 'emergency')),
      makeQuickPill(widget, 'places',    t(widget, 'places')),
      makeQuickPill(widget, 'hours',     t(widget, 'hours'))
    );

    var welcome = makeEl('div', 'cb-welcome');
    var greeting = makeEl('p');
    greeting.textContent = t(widget, 'greeting');
    var qlabel = makeEl('div', 'cb-quick-label');
    qlabel.textContent = t(widget, 'quick_label');
    appendAll(welcome, greeting, qlabel, pills);

    var bubble = makeEl('div', 'cb-bubble');
    bubble.appendChild(welcome);

    var body = makeEl('div', 'cb-msg-body');
    body.appendChild(bubble);

    var row = makeEl('div', 'cb-msg bot');
    appendAll(row, makeEl('div', 'cb-msg-avatar', '<img src="/assets/logo/favicon.png" alt="Assistant" style="width:22px;height:22px;object-fit:contain;">')
, body);

    msgs.appendChild(row);
  }

  /* ── User message ── */
  function renderUserMsg(widget, text) {
    var msgs   = widget.querySelector('[data-cb-messages]');
    var bubble = makeEl('div', 'cb-bubble');
    bubble.textContent = text;
    var body   = makeEl('div', 'cb-msg-body');
    body.appendChild(bubble);
    var row = makeEl('div', 'cb-msg user');
	appendAll(row, body);
    msgs.appendChild(row);
    scrollBottom(msgs);
    return row;
  }

  /* ── Typing indicator ── */
  function renderTyping(widget) {
    var msgs   = widget.querySelector('[data-cb-messages]');
    var dots   = makeEl('div', 'cb-dots', '<span></span><span></span><span></span>');
    var bubble = makeEl('div', 'cb-bubble');
    bubble.appendChild(dots);
    var body   = makeEl('div', 'cb-msg-body');
    body.appendChild(bubble);
    var row    = makeEl('div', 'cb-msg bot cb-typing');
    appendAll(row, makeEl('div', 'cb-msg-avatar', '<img src="/assets/logo/favicon.png" alt="Assistant" style="width:22px;height:22px;object-fit:contain;">')
, body);
    msgs.appendChild(row);
    scrollBottom(msgs);
    return row;
  }

  /* ── Emergency contact icon by title keyword ── */
  function ecIcon(title) {
    var t = title.toLowerCase();
    if (t.includes('police') || t.includes('полиц') || t.includes('пулис') || t.includes('경찰')) return '🚔';
    if (t.includes('ambulance') || t.includes('скор') || t.includes('тез') || t.includes('구급') || t.includes('응급')) return '🚑';
    if (t.includes('fire') || t.includes('пожар') || t.includes('оташнш') || t.includes('소방')) return '🚒';
    if (t.includes('hospital') || t.includes('clinic') || t.includes('больниц') || t.includes('клиник') || t.includes('шифохо') || t.includes('병원')) return '🏥';
    if (t.includes('embassy') || t.includes('посольств') || t.includes('сафорат') || t.includes('대사관')) return '🏛️';
    return '🆘';
  }

  /* ── Emergency contact card list ── */
  function renderEmergencyCards(answer) {
    var list = makeEl('div', 'cb-emergency-list');
    (answer || '').split('\n').forEach(function (line) {
      line = line.trim();
      if (!line) return;
      var m = line.match(/^(.+?):\s+(.+?)\s+\((.+)\)$/);
      var title = m ? m[1] : line;
      var phone = m ? m[2] : '';
      var desc  = m ? m[3] : '';

      var titleEl = makeEl('div', 'cb-ec-title');
      titleEl.textContent = title;

      var infoEl = makeEl('div', 'cb-ec-info');
      infoEl.appendChild(titleEl);
      if (desc) {
        var descEl = makeEl('div', 'cb-ec-desc');
        descEl.textContent = desc;
        infoEl.appendChild(descEl);
      }
      if (phone) {
        var phoneEl = document.createElement('a');
        phoneEl.className = 'cb-ec-phone';
        phoneEl.href = 'tel:' + phone.replace(/\s/g, '');
        phoneEl.textContent = phone;
        infoEl.appendChild(phoneEl);
      }

      var iconEl = makeEl('div', 'cb-ec-icon');
      iconEl.textContent = ecIcon(title);

      var card = makeEl('div', 'cb-ec-card');
      appendAll(card, iconEl, infoEl);

      list.appendChild(card);
    });
    return list;
  }

  /* ── Bot message ── */
  function renderBotMsg(widget, data) {
    var msgs = widget.querySelector('[data-cb-messages]');
    var body = makeEl('div', 'cb-msg-body');

    /* Answer bubble or no-data fallback */
    if (data.noData) {
      var pills = makeEl('div', 'cb-quick-pills');
      appendAll(pills,
        makeQuickPill(widget, 'emergency', t(widget, 'emergency')),
        makeQuickPill(widget, 'places',    t(widget, 'places')),
        makeQuickPill(widget, 'hours',     t(widget, 'hours'))
      );
      var ndText = makeEl('p');
      ndText.textContent = t(widget, 'no_data');
      var ndWrap = makeEl('div', 'cb-welcome');
      appendAll(ndWrap, ndText, pills);
      var ndBubble = makeEl('div', 'cb-bubble');
      ndBubble.appendChild(ndWrap);
      body.appendChild(ndBubble);
    } else {
      var bubble = makeEl('div', 'cb-bubble');
      var isEmergency = data.intent === 'EMERGENCY_CONTACT' || data.intent === 'PHONE_NUMBER';
      if (isEmergency) {
        bubble.appendChild(renderEmergencyCards(data.answer));
      } else {
        bubble.textContent = data.answer || '';
      }
      body.appendChild(bubble);
    }

    /* Verified badge */
    if (data.grounded && !data.noData) {
      var badge = makeEl('div', 'cb-verified', SVG.check + ' ' + t(widget, 'verified'));
      body.appendChild(badge);
    }
	
	/* More info links for tour places */
	if (!data.noData && data.citations && data.citations.length) {
	  data.citations.forEach(function (c) {
	    if (c.sourceType === 'TOUR_PLACE') {
	      var id = c.sourceId.replace('tour_place:', '');
	      var link = document.createElement('a');
	      link.className = 'cb-more-info-btn';
	      link.textContent = '📍 ' + c.title;
	      link.href = '/detail/' + id;
	      body.appendChild(link);
	    }
	  });
	}

    /* Intent label */
    if (data.intent && data.intent !== 'UNKNOWN' && INTENT_ICON[data.intent]) {
      var intentEl = makeEl('div', 'cb-intent-badge');
      intentEl.textContent = INTENT_ICON[data.intent] + ' ' + data.intent.replace(/_/g, ' ').toLowerCase();
      body.appendChild(intentEl);
    }

    /* Citation cards */
    /* if (data.citations && data.citations.length) {
      var citWrap = makeEl('div', 'cb-citations');
      data.citations.forEach(function (c) {
        var icon = INTENT_ICON[c.sourceType] || '📄';
        var title = makeEl('div', 'cb-citation-title');
        title.textContent = c.title || c.sourceId;
        var type = makeEl('div', 'cb-citation-type');
        type.textContent = t(widget, 'source') + ' · ' + (c.sourceType || '');
        var info = makeEl('div', 'cb-citation-info');
        appendAll(info, title, type);
        var card = makeEl('div', 'cb-citation-card');
        appendAll(card, makeEl('span', 'cb-citation-icon', icon), info);
        citWrap.appendChild(card);
      });
      body.appendChild(citWrap);
    } */

    var row = makeEl('div', 'cb-msg bot');
    appendAll(row, makeEl('div', 'cb-msg-avatar', '<img src="/assets/logo/favicon.png" alt="Assistant" style="width:22px;height:22px;object-fit:contain;">')
, body);
    msgs.appendChild(row);
    scrollBottom(msgs);
  }

  /* ── Error toast ── */
  function showToast(text) {
    var toast = document.createElement('div');
    toast.className = 'cb-toast-popup';
    toast.innerHTML = SVG.warn + '<span>' + text + '</span>';
    document.body.appendChild(toast);
    setTimeout(function () { if (toast.parentNode) toast.remove(); }, 4500);
  }

  /* ── API call ── */
  function sendMessage(widget, text) {
    if (widget._cbSending) return;
    widget._cbSending = true;

    var input   = widget.querySelector('[data-cb-input]');
    var sendBtn = widget.querySelector('[data-cb-send]');
    if (input)   { input.value = ''; input.disabled = true; }
    if (sendBtn) sendBtn.disabled = true;

    /* hide notification dot once user sends first message */
    var notif = widget.querySelector('[data-cb-notif]');
    if (notif) notif.hidden = true;

    renderUserMsg(widget, text);
    var typingRow = renderTyping(widget);

    fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text, locale: widget._cbLang })
    })
    .then(function (res) {
      return res.json().then(function (body) {
        return { ok: res.ok, status: res.status, body: body };
      });
    })
    .then(function (result) {
      typingRow.remove();
      if (!result.ok) {
        showToast(t(widget, 'err_conn') + ' (' + result.status + ')');
        var errBubble = makeEl('div', 'cb-bubble cb-bubble-error');
        errBubble.textContent = (result.body && result.body.message) || JSON.stringify(result.body);
        var errBody = makeEl('div', 'cb-msg-body');
        errBody.appendChild(errBubble);
        var errRow  = makeEl('div', 'cb-msg bot');
        appendAll(errRow, makeEl('div', 'cb-msg-avatar', '<img src="/assets/logo/favicon.png" alt="Assistant" style="width:22px;height:22px;object-fit:contain;">')
, errBody);
        var msgs = widget.querySelector('[data-cb-messages]');
        msgs.appendChild(errRow);
        scrollBottom(msgs);
      } else {
        renderBotMsg(widget, result.body);
      }
    })
    .catch(function () {
      typingRow.remove();
      showToast(t(widget, 'err_conn'));
    })
    .finally(function () {
      widget._cbSending = false;
      if (input)   input.disabled = false;
      if (sendBtn) sendBtn.disabled = false;
      if (input)   input.focus();
    });
  }

  /* ── Init ── */
  function init(container) {
    if (container.dataset.cbReady === 'true') return;
    container.dataset.cbReady = 'true';

    var rawLang = container.getAttribute('data-lang') ||
                  new URLSearchParams(window.location.search).get('lang') || 'en';
    container._cbLang = STRINGS[rawLang] ? rawLang : 'en';

    buildHTML(container);

    var toggle   = container.querySelector('[data-cb-toggle]');
    var closeBtn = container.querySelector('[data-cb-close]');
    var panel    = container.querySelector('[data-cb-panel]');
    var sendBtn  = container.querySelector('[data-cb-send]');
    var input    = container.querySelector('[data-cb-input]');
    var nudge    = container.querySelector('[data-cb-nudge]');
    var langBtns = container.querySelectorAll('[data-cb-lang]');

    var composing    = false;
    var welcomeShown = false;

    /* Show nudge after 3 s, auto-hide after 5 s */
    setTimeout(function () {
      if (panel.hidden) nudge.hidden = false;
      setTimeout(function () { nudge.hidden = true; }, 5000);
    }, 3000);

    /* Open / close */
    toggle.addEventListener('click', function () {
      var opening = panel.hidden;
      panel.hidden = !opening;
      toggle.setAttribute('aria-expanded', String(opening));
      nudge.hidden = true;
      if (opening) {
        if (!welcomeShown) { welcomeShown = true; renderWelcome(container); }
        input.focus();
      }
    });

    closeBtn.addEventListener('click', function () {
      panel.hidden = true;
      toggle.setAttribute('aria-expanded', 'false');
      toggle.focus();
    });

    /* Language switcher */
	langBtns.forEach(function (btn) {
	      btn.addEventListener('click', function () {
	        container._cbLang = btn.getAttribute('data-cb-lang');
	        langBtns.forEach(function (b) { b.classList.toggle('active', b === btn); });
	        input.placeholder = t(container, 'placeholder');

	        var msgs = container.querySelector('[data-cb-messages]');
	        msgs.innerHTML = '';
	        renderWelcome(container);
	      });
	    });

    /* Submit */
    function submit() {
      if (composing || container._cbSending) return;
      var text = input.value.trim();
      if (!text) return;
      input.value = '';
      sendMessage(container, text);
    }

    sendBtn.addEventListener('click', submit);

    input.addEventListener('compositionstart', function () { composing = true; });
    input.addEventListener('compositionend',   function () {
      setTimeout(function () { composing = false; }, 0);
    });
    input.addEventListener('keydown', function (e) {
      if (e.key !== 'Enter' || e.isComposing || e.keyCode === 229 || composing) return;
      e.preventDefault();
      submit();
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    var widget = document.querySelector('[data-chatbot]');
    if (widget) init(widget);
  });

})();
