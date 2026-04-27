let calendarInstance = null;
let draggableInstance = null;

let originalFavorites = [];
let currentLang = 'en';

function showLoading() {
    const spinner = document.getElementById('loading-spinner');
    if (spinner) {
        spinner.classList.remove('hidden');
    }
}

function hideLoading() {
    const spinner = document.getElementById('loading-spinner');
    if (spinner) {
        spinner.classList.add('hidden');
    }
}

document.addEventListener('DOMContentLoaded', async function () {
    if (!Auth || !Auth.isLoggedIn()) {
        Auth.goToLogin();
        return;
    }

    const urlParams = new URLSearchParams(window.location.search);
    const lang = urlParams.get('lang') || document.documentElement.lang || 'en';
    currentLang = lang;

    // Show loading spinner
    showLoading();

    initCalendar(lang);
    await loadFavoriteCardsForCalendar(lang);
    await loadSavedCalendars(lang);
    await loadMemos();

    // Hide loading spinner
    hideLoading();
});

function getCategoryIcon(category) {
    if (!category) return '';

    const categoryKey = String(category).trim().toLowerCase();

    const iconMap = {
        destination: '⛰️',
        park: '🛝',
        stay: '🏠',
        dining: '🍔'
    };

    return iconMap[categoryKey] || '';
}

function buildCalendarEventTitle(title, category) {
    const safeTitle = title || 'No Title';
    const icon = getCategoryIcon(category);

    return icon ? `${icon} ${safeTitle}` : safeTitle;
}

function initCalendar(lang) {
    const calendarEl = document.getElementById('calendar');

    calendarInstance = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        initialDate: new Date(),
        locale: 'en',
        height: 'auto',
        expandRows: true,

        headerToolbar: {
            left: 'prev',
            center: 'title',
            right: 'next'
        },

        droppable: true,
        editable: true,
        dayMaxEvents: 2,
        fixedWeekCount: false,

        eventReceive: async function (info) {
            const placeId = Number(info.event.extendedProps.placeId);
            const startDate = formatDate(info.event.start);

            try {
                const response = await Auth.authFetch('/me/calendar/add', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        placeId: placeId,
                        startDate: startDate
                    })
                });

                if (!response.ok) {
                    info.event.remove();
                    alert('Failed to save calendar event.');
                    return;
                }

                await loadSavedCalendars(lang);
            } catch (error) {
                console.error('Failed to save calendar event:', error);
                info.event.remove();
                alert('Failed to save calendar event.');
            }
        },

        eventClick: async function (info) {
            const memoId = info.event.extendedProps.memoId;
            const calendarId = info.event.extendedProps.calendarId;

            if (memoId) {
                await loadEditMemoModal(memoId);
                return;
            }

            const shouldDelete = confirm('Remove "' + info.event.title + '" from the calendar?');

            if (!shouldDelete) return;

            if (!calendarId) {
                info.event.remove();
                return;
            }

            try {
                const response = await Auth.authFetch(`/me/calendar/delete/${calendarId}`, {
                    method: 'DELETE'
                });

                if (!response.ok) {
                    alert('Failed to delete calendar event.');
                    return;
                }

                info.event.remove();
            } catch (error) {
                console.error('Failed to delete calendar event:', error);
                alert('Failed to delete calendar event.');
            }
        }
    });

    calendarInstance.render();
}

async function loadFavoriteCardsForCalendar(lang) {
    try {
        const listResponse = await Auth.authFetch(`/me/favorite/list?lang=${lang}`);

        if (!listResponse.ok) {
            showCalendarEmpty('No favorite destinations yet.');
            return;
        }

        const places = await listResponse.json();

        if (!places || places.length === 0) {
            originalFavorites = [];
            initFavoriteFilterOptions([]);
            bindFavoriteFilterEvents();
            showCalendarEmpty('No favorite destinations yet.');
            return;
        }

        originalFavorites = places;

        initFavoriteFilterOptions(places);
        bindFavoriteFilterEvents();

        renderCalendarFavoriteCards(places, lang);
        enableExternalDragging();
    } catch (error) {
        console.error('Failed to load calendar favorites:', error);
        showCalendarEmpty('No favorite destinations yet.');
    }
}

function initFavoriteFilterOptions(places) {
    const categorySelect = document.getElementById('filter-category');
    const regionSelect = document.getElementById('filter-region');

    if (!categorySelect || !regionSelect) return;

    categorySelect.innerHTML = '<option value="">All Categories</option>';
    regionSelect.innerHTML = '<option value="">All Regions</option>';

    const categories = [...new Set(
        places
            .map(place => place.categoryName)
            .filter(Boolean)
    )].sort((a, b) => a.localeCompare(b));

    const regions = [...new Set(
        places
            .map(place => place.regionName)
            .filter(Boolean)
    )].sort((a, b) => a.localeCompare(b));

    categories.forEach(category => {
        const option = document.createElement('option');
        option.value = category;
        option.textContent = category;
        categorySelect.appendChild(option);
    });

    regions.forEach(region => {
        const option = document.createElement('option');
        option.value = region;
        option.textContent = region;
        regionSelect.appendChild(option);
    });
}

function bindFavoriteFilterEvents() {
    const form = document.getElementById('calendar-filter-form');
    const queryInput = document.getElementById('filter-query');
    const categorySelect = document.getElementById('filter-category');
    const regionSelect = document.getElementById('filter-region');

    if (!form || !queryInput || !categorySelect || !regionSelect) return;
    if (form.dataset.bound === 'true') return;

    form.dataset.bound = 'true';

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        applyFavoriteFilter();
    });

    queryInput.addEventListener('input', applyFavoriteFilter);
    categorySelect.addEventListener('change', applyFavoriteFilter);
    regionSelect.addEventListener('change', applyFavoriteFilter);
}

function applyFavoriteFilter() {
    const queryInput = document.getElementById('filter-query');
    const categorySelect = document.getElementById('filter-category');
    const regionSelect = document.getElementById('filter-region');

    if (!queryInput || !categorySelect || !regionSelect) return;

    const query = queryInput.value.trim().toLowerCase();
    const category = categorySelect.value;
    const region = regionSelect.value;

    const filtered = originalFavorites.filter(place => {
        const title = (place.title || '').toLowerCase();
        const content = (place.content || '').toLowerCase();
        const categoryName = place.categoryName || '';
        const regionName = place.regionName || '';

        const matchesQuery =
            !query ||
            title.includes(query) ||
            content.includes(query) ||
            categoryName.toLowerCase().includes(query) ||
            regionName.toLowerCase().includes(query);

        const matchesCategory = !category || categoryName === category;
        const matchesRegion = !region || regionName === region;

        return matchesQuery && matchesCategory && matchesRegion;
    });

    renderCalendarFavoriteCards(filtered, currentLang);
    enableExternalDragging();
}

async function loadSavedCalendars(lang) {
    try {
        const response = await Auth.authFetch(`/me/calendar/list?lang=${lang}`);

        if (!response.ok) {
            console.error('Failed to load saved calendars');
            return;
        }

        const calendars = await response.json();

        if (!calendarInstance) return;

        calendarInstance.getEvents().forEach(event => {
            if (!event.extendedProps || !event.extendedProps.memoId) {
                event.remove();
            }
        });

        calendars.forEach(item => {
            calendarInstance.addEvent({
                title: buildCalendarEventTitle(item.title, item.categoryName),
                start: item.startDate,
                allDay: true,
                extendedProps: {
                    calendarId: item.calendarId,
                    placeId: item.placeId,
                    region: item.regionName || '',
                    content: item.content || '',
                    imageUrl: item.imageUrl || '',
                    categoryName: item.categoryName || '',
                    originalTitle: item.title || 'No Title'
                }
            });
        });
    } catch (error) {
        console.error('Failed to load saved calendars:', error);
    }
}

async function loadMemos() {
    try {
        const response = await Auth.authFetch('/me/calendar-memo/list');

        if (!response.ok) {
            console.error('Failed to load memos');
            return;
        }

        const memos = await response.json();

        if (!calendarInstance) return;

        calendarInstance.getEvents().forEach(event => {
            if (event.extendedProps && event.extendedProps.memoId) {
                event.remove();
            }
        });

        memos.forEach(memo => {
            const displayMemo = memo.memo.length > 25
                ? '📝 ' + memo.memo.substring(0, 25) + '...'
                : '📝 ' + memo.memo;

            calendarInstance.addEvent({
                title: displayMemo,
                start: memo.startDate,
                allDay: true,
                backgroundColor: '#3b82f6',
                borderColor: '#1e40af',
                textColor: '#ffffff',
                extendedProps: {
                    memoId: memo.memoId,
                    memo: memo.memo,
                    isNote: true
                }
            });
        });
    } catch (error) {
        console.error('Failed to load memos:', error);
    }
}

function renderCalendarFavoriteCards(places, lang) {
    const container = document.getElementById('external-events');
    const countEl = document.getElementById('favorites-side-count');
    const emptyEl = document.getElementById('calendar-empty');

    if (!container || !countEl || !emptyEl) return;

    container.innerHTML = '';

    const count = places ? places.length : 0;
    countEl.textContent = `${count} Favorites`;

    if (!places || places.length === 0) {
        emptyEl.style.display = 'block';
        emptyEl.textContent = originalFavorites.length === 0
            ? 'No favorite destinations yet.'
            : 'No matching favorite places.';
        return;
    }

    emptyEl.style.display = 'none';

    places.forEach(place => {
        const card = createSidebarPlaceCard(place, lang);
        if (card) {
            container.appendChild(card);
        }
    });
}

function createSidebarPlaceCard(place, lang) {
    if (!place || !place.placeId) return null;

    const imageUrl = place.imageUrl || '/assets/images/home_reg_2.png';
    const title = place.title || 'No Title';
    const category = place.categoryName || 'Category';
    const region = place.regionName || 'Region';
    const content = place.content || '';
    const placeId = place.placeId;

    const wrapper = document.createElement('div');
    wrapper.className = 'place-card-wrapper sidebar-place-card fc-draggable-event';
    wrapper.setAttribute('data-title', title);
    wrapper.setAttribute('data-category', category);
    wrapper.setAttribute('data-region', region);
    wrapper.setAttribute('data-place-id', placeId);

    const thumbDiv = document.createElement('div');
    wrapper.appendChild(thumbDiv);
    thumbDiv.className = 'place-thumb sidebar-place-thumb';

    const img = document.createElement('img');
    img.src = imageUrl;
    img.alt = title;
    thumbDiv.appendChild(img);

    const categoryBadge = document.createElement('span');
    categoryBadge.className = 'badge badge-top';
    categoryBadge.textContent = category;
    thumbDiv.appendChild(categoryBadge);

    const bodyDiv = document.createElement('div');
    bodyDiv.className = 'place-body sidebar-place-body';

    const headerDiv = document.createElement('div');
    headerDiv.className = 'place-header sidebar-place-header';

    const titleElem = document.createElement('h3');
    titleElem.className = 'place-title sidebar-place-title';
    titleElem.textContent = title;
    headerDiv.appendChild(titleElem);

    bodyDiv.appendChild(headerDiv);

    if (content) {
        const descElem = document.createElement('p');
        descElem.className = 'place-desc sidebar-place-desc';
        descElem.textContent = content;
        bodyDiv.appendChild(descElem);
    }

    const tagsDiv = document.createElement('div');
    tagsDiv.className = 'place-tags';

    const regionBadge = document.createElement('span');
    regionBadge.className = 'badge badge-bottom';

    const locationIcon = document.createElement('span');
    locationIcon.className = 'material-icons';
    locationIcon.setAttribute('aria-hidden', 'true');
    locationIcon.textContent = 'location_on';
    regionBadge.appendChild(locationIcon);

    const regionText = document.createElement('span');
    regionText.textContent = region;
    regionBadge.appendChild(regionText);

    tagsDiv.appendChild(regionBadge);
    bodyDiv.appendChild(tagsDiv);

    wrapper.appendChild(bodyDiv);

    wrapper.addEventListener('dblclick', function () {
        window.location.href = `/detail/${placeId}?lang=${lang}&back=/me/calendar?lang=${lang}`;
    });

    return wrapper;
}

function enableExternalDragging() {
    const externalEventsEl = document.getElementById('external-events');

    if (!externalEventsEl) return;

    if (draggableInstance && typeof draggableInstance.destroy === 'function') {
        draggableInstance.destroy();
    }

    draggableInstance = new FullCalendar.Draggable(externalEventsEl, {
        itemSelector: '.fc-draggable-event',
        eventData: function (eventEl) {
            const title = eventEl.dataset.title || 'No Title';
            const category = eventEl.dataset.category || '';

            return {
                title: buildCalendarEventTitle(title, category),
                allDay: true,
                extendedProps: {
                    region: eventEl.dataset.region || '',
                    placeId: eventEl.dataset.placeId || '',
                    categoryName: category,
                    originalTitle: title
                }
            };
        }
    });
}

function showCalendarEmpty(message) {
    const container = document.getElementById('external-events');
    const countEl = document.getElementById('favorites-side-count');
    const emptyEl = document.getElementById('calendar-empty');

    if (!container || !countEl || !emptyEl) return;

    container.innerHTML = '';
    countEl.textContent = '0 Favorites';
    emptyEl.textContent = message || 'No favorite destinations yet.';
    emptyEl.style.display = 'block';
}

function formatDate(date) {
    if (!date) return '';

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}

document.getElementById('download-image-btn').addEventListener('click', downloadCalendarAsImage);
document.getElementById('copy-image-btn').addEventListener('click', copyCalendarToClipboard);

async function downloadCalendarAsImage() {
    const calendarBoard = document.querySelector('.calendar-board');
    const timestamp = new Date().toISOString().slice(0, 10);

    try {
        const canvas = await html2canvas(calendarBoard, {
            scale: 2,
            useCORS: true,
            logging: false,
            backgroundColor: '#ffffff'
        });

        const link = document.createElement('a');
        link.href = canvas.toDataURL('image/png');
        link.download = `travel-calendar-${timestamp}.png`;
        link.click();
    } catch (error) {
        console.error('Failed to download calendar as image:', error);
        alert('Failed to download calendar as image. Please try again.');
    }
}

async function copyCalendarToClipboard() {
    const calendarBoard = document.querySelector('.calendar-board');
    const copyBtn = document.getElementById('copy-image-btn');
    const originalText = copyBtn.querySelector('span:last-child').textContent;

    try {
        const canvas = await html2canvas(calendarBoard, {
            scale: 2,
            useCORS: true,
            logging: false,
            backgroundColor: '#ffffff'
        });

        canvas.toBlob(async function (blob) {
            try {
                await navigator.clipboard.write([
                    new ClipboardItem({ 'image/png': blob })
                ]);

                copyBtn.querySelector('span:last-child').textContent = 'Copied!';
                setTimeout(() => {
                    copyBtn.querySelector('span:last-child').textContent = originalText;
                }, 2000);
            } catch (error) {
                console.error('Failed to copy to clipboard:', error);
                alert('Failed to copy to clipboard. Please try again.');
            }
        });
    } catch (error) {
        console.error('Failed to copy calendar to clipboard:', error);
        alert('Failed to copy calendar to clipboard. Please try again.');
    }
}

let calendarMonth = new Date().getMonth();
let calendarYear = new Date().getFullYear();
let selectedDate = new Date();
let currentMemoId = null;

const noteModal = document.getElementById('note-modal');
const addNoteBtn = document.getElementById('add-note-btn');
const noteModalClose = document.getElementById('note-modal-close');
const noteCancelBtn = document.getElementById('note-cancel-btn');
const noteSaveBtn = document.getElementById('note-save-btn');
const noteDeleteBtn = document.getElementById('note-delete-btn');
const noteModalOverlay = document.querySelector('.note-modal-overlay');
const noteTextarea = document.getElementById('note-textarea');
const noteCharUsed = document.getElementById('note-char-used');
const noteDateInput = document.getElementById('note-date');

const dateDisplayBtn = document.getElementById('date-display-btn');
const dateDisplayText = document.getElementById('date-display-text');
const calendarPickerDiv = document.getElementById('calendar-picker');
const calendarBackdropDiv = document.getElementById('calendar-picker-backdrop');
const calPrevBtn = document.getElementById('cal-prev-btn');
const calNextBtn = document.getElementById('cal-next-btn');

addNoteBtn.addEventListener('click', openNoteModal);
noteModalClose.addEventListener('click', closeNoteModal);
noteCancelBtn.addEventListener('click', closeNoteModal);
noteDeleteBtn.addEventListener('click', deleteMemoConfirm);
noteModalOverlay.addEventListener('click', closeNoteModal);
dateDisplayBtn.addEventListener('click', toggleCalendar);
calendarBackdropDiv.addEventListener('click', closeCalendar);
calPrevBtn.addEventListener('click', () => changeMonth(-1));
calNextBtn.addEventListener('click', () => changeMonth(1));
noteTextarea.addEventListener('input', () => {
    noteCharUsed.textContent = noteTextarea.value.length;
});
noteSaveBtn.addEventListener('click', saveNote);

function positionCalendar() {
    const dateBtn = document.getElementById('date-display-btn');
    const modal = document.querySelector('.note-modal-content');

    if (!dateBtn || !modal) return;

    const dateRect = dateBtn.getBoundingClientRect();
    const modalRect = modal.getBoundingClientRect();

    const top = dateRect.bottom - modalRect.top + 8;
    const left = dateRect.left - modalRect.left;

    calendarPickerDiv.style.position = 'absolute';
    calendarPickerDiv.style.top = top + 'px';
    calendarPickerDiv.style.left = left + 'px';
    calendarPickerDiv.style.minWidth = dateRect.width + 'px';
}

function toggleCalendar() {
    if (calendarPickerDiv.style.display === 'none') {
        positionCalendar();
        calendarPickerDiv.style.display = 'block';
        calendarBackdropDiv.style.display = 'block';
        renderCalendar();
    } else {
        closeCalendar();
    }
}

function closeCalendar() {
    calendarPickerDiv.style.display = 'none';
    calendarBackdropDiv.style.display = 'none';
}

function changeMonth(offset) {
    calendarMonth += offset;
    if (calendarMonth > 11) {
        calendarMonth = 0;
        calendarYear++;
    } else if (calendarMonth < 0) {
        calendarMonth = 11;
        calendarYear--;
    }
    renderCalendar();
}

function updateDateDisplay() {
    const options = { year: 'numeric', month: 'long', day: 'numeric' };
    const dateStr = selectedDate.toLocaleDateString('en-US', options);
    dateDisplayText.textContent = dateStr;
    noteDateInput.value = formatDate(selectedDate);
    closeCalendar();
}

function renderCalendar() {
    const monthNames = ['January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'];
    document.getElementById('cal-month-year').textContent = `${monthNames[calendarMonth]} ${calendarYear}`;

    const firstDay = new Date(calendarYear, calendarMonth, 1).getDay();
    const daysInMonth = new Date(calendarYear, calendarMonth + 1, 0).getDate();
    const daysInPrevMonth = new Date(calendarYear, calendarMonth, 0).getDate();

    const calDays = document.getElementById('cal-days');
    calDays.innerHTML = '';

    for (let i = firstDay - 1; i >= 0; i--) {
        const day = document.createElement('button');
        day.className = 'cal-day other-month';
        day.textContent = daysInPrevMonth - i;
        day.type = 'button';
        calDays.appendChild(day);
    }

    for (let day = 1; day <= daysInMonth; day++) {
        const dayBtn = document.createElement('button');
        dayBtn.className = 'cal-day';
        dayBtn.textContent = day;
        dayBtn.type = 'button';

        const today = new Date();
        if (day === today.getDate() &&
            calendarMonth === today.getMonth() &&
            calendarYear === today.getFullYear()) {
            dayBtn.classList.add('today');
        }

        if (day === selectedDate.getDate() &&
            calendarMonth === selectedDate.getMonth() &&
            calendarYear === selectedDate.getFullYear()) {
            dayBtn.classList.add('selected');
        }

        dayBtn.addEventListener('click', () => {
            selectedDate = new Date(calendarYear, calendarMonth, day);
            updateDateDisplay();
        });

        calDays.appendChild(dayBtn);
    }

    const totalCells = calDays.children.length;
    const remainingCells = 42 - totalCells;
    for (let day = 1; day <= remainingCells; day++) {
        const dayBtn = document.createElement('button');
        dayBtn.className = 'cal-day other-month';
        dayBtn.textContent = day;
        dayBtn.type = 'button';
        calDays.appendChild(dayBtn);
    }
}

function openNoteModal() {
    const today = new Date();
    calendarMonth = today.getMonth();
    calendarYear = today.getFullYear();
    selectedDate = new Date(today);
    currentMemoId = null;

    updateDateDisplay();
    closeCalendar();
    noteTextarea.value = '';
    noteCharUsed.textContent = '0';

    const noteSaveBtn = document.getElementById('note-save-btn');
    noteSaveBtn.disabled = false;
    noteSaveBtn.textContent = 'Save Note';
    noteDeleteBtn.style.display = 'none';

    noteModal.style.display = 'flex';
    setTimeout(() => noteTextarea.focus(), 100);
}

function closeNoteModal() {
    noteModal.style.display = 'none';
    closeCalendar();
}

async function loadEditMemoModal(memoId) {
    try {
        const response = await Auth.authFetch(`/me/calendar-memo/${memoId}`);

        if (!response.ok) {
            alert('Failed to load memo');
            return;
        }

        const memo = await response.json();

        currentMemoId = memo.memoId;
        selectedDate = new Date(memo.startDate + 'T00:00:00');
        calendarMonth = selectedDate.getMonth();
        calendarYear = selectedDate.getFullYear();

        updateDateDisplay();
        closeCalendar();
        noteTextarea.value = memo.memo;
        noteCharUsed.textContent = memo.memo.length;

        const noteSaveBtn = document.getElementById('note-save-btn');
        noteSaveBtn.disabled = false;
        noteSaveBtn.textContent = 'Update Note';
        noteDeleteBtn.style.display = 'block';

        noteModal.style.display = 'flex';
        setTimeout(() => noteTextarea.focus(), 100);
    } catch (error) {
        console.error('Failed to load memo:', error);
        alert('Failed to load memo. Please try again.');
    }
}

async function saveNote() {
    const date = noteDateInput.value;
    const content = noteTextarea.value.trim();

    if (!date) {
        alert('Please select a date');
        return;
    }

    if (!content) {
        alert('Please write a note');
        return;
    }

    const noteSaveBtn = document.getElementById('note-save-btn');
    const originalText = noteSaveBtn.textContent;
    noteSaveBtn.disabled = true;
    noteSaveBtn.textContent = 'Saving...';

    try {
        let url, method;

        if (currentMemoId) {
            url = `/me/calendar-memo/${currentMemoId}`;
            method = 'PUT';
        } else {
            url = '/me/calendar-memo/save';
            method = 'POST';
        }

        const response = await Auth.authFetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                startDate: date,
                memo: content
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        console.log('Note saved successfully:', result);

        noteSaveBtn.disabled = false;
        noteSaveBtn.textContent = originalText;
        closeNoteModal();

        await loadMemos();
    } catch (error) {
        console.error('Failed to save note:', error);
        noteSaveBtn.disabled = false;
        noteSaveBtn.textContent = originalText;
        alert('Failed to save note. Please try again.');
    }
}

function deleteMemoConfirm() {
    if (!currentMemoId) return;

    const shouldDelete = confirm('Are you sure you want to delete this memo?');
    if (!shouldDelete) return;

    deleteMemo();
}

async function deleteMemo() {
    if (!currentMemoId) return;

    try {
        const response = await Auth.authFetch(`/me/calendar-memo/${currentMemoId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        closeNoteModal();
        await loadMemos();
    } catch (error) {
        console.error('Failed to delete memo:', error);
        alert('Failed to delete memo. Please try again.');
    }
}