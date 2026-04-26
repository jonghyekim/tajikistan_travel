// detail.js - Handle rating display, favorite functionality, and place detail info

document.addEventListener('DOMContentLoaded', function () {
    renderPlaceDetailInfo();

    const placeIdMeta = document.querySelector('meta[name="place-id"]');
    const placeId = placeIdMeta ? placeIdMeta.getAttribute('content') : null;

    if (!placeId) {
        console.warn('Place ID not found in meta tag');
        return;
    }

    loadRatingAndFavorite(placeId);
    setupFavoriteButton(placeId);
});

function renderPlaceDetailInfo() {
    const infoBox = document.querySelector('.detail-info-box');
    if (!infoBox) return;

    const openTime = normalizeValue(infoBox.dataset.openTime);
    const closeTime = normalizeValue(infoBox.dataset.closeTime);
    const isFree = normalizeValue(infoBox.dataset.isFree);
    const currencyCode = normalizeValue(infoBox.dataset.currencyCode);
    const admissionFee = normalizeValue(infoBox.dataset.admissionFee);

    const openingHoursEl = document.getElementById('detail-opening-hours');
    const admissionFeeEl = document.getElementById('detail-admission-fee');

    if (openingHoursEl) {
        openingHoursEl.textContent = formatOpeningHours(openTime, closeTime);
    }

    if (admissionFeeEl) {
        admissionFeeEl.textContent = formatAdmissionFee(isFree, currencyCode, admissionFee);
    }
}

function normalizeValue(value) {
    if (value === undefined || value === null) return '';

    const normalized = String(value).trim();

    if (
        normalized === '' ||
        normalized.toLowerCase() === 'null' ||
        normalized.toLowerCase() === 'undefined'
    ) {
        return '';
    }

    return normalized;
}

function formatOpeningHours(openTime, closeTime) {
    if (!openTime && !closeTime) return '-';

    const formattedOpenTime = openTime ? formatTime(openTime) : '-';
    const formattedCloseTime = closeTime ? formatTime(closeTime) : '-';

    return `${formattedOpenTime} - ${formattedCloseTime}`;
}

function formatTime(time) {
    if (!time) return '-';

    const value = String(time).trim();

    // 08:00:00 -> 08:00
    if (/^\d{2}:\d{2}:\d{2}$/.test(value)) {
        return value.substring(0, 5);
    }

    return value;
}

function formatAdmissionFee(isFree, currencyCode, admissionFee) {
    const freeValue = String(isFree).toLowerCase();

    if (freeValue === 'true') {
        return 'Free';
    }

    if (!admissionFee) {
        return '-';
    }

    return currencyCode ? `${admissionFee} ${currencyCode}` : admissionFee;
}

function setFavoriteButtonState(btnFavorite, isFavorite) {
    if (!btnFavorite) return;

    btnFavorite.classList.toggle('active', isFavorite);
    btnFavorite.setAttribute('aria-label', isFavorite ? '찜 취소' : '찜하기');

    const icon = btnFavorite.querySelector('.favorite-icon');
    if (icon) icon.textContent = 'favorite';
}

/**
 * Load rating from reviews API
 */
async function loadRatingAndFavorite(placeId) {
    try {
        const response = await fetch(`/api/reviews/${placeId}`);

        if (!response.ok) {
            console.warn('Failed to load reviews');
            return;
        }

        const data = await response.json();
        const { averageRating, reviewCount } = data;

        displayRating(averageRating, reviewCount);
    } catch (error) {
        console.error('Error loading reviews:', error);
    }
}

/**
 * Display rating in header
 */
function displayRating(averageRating, reviewCount) {
    const detailRating = document.getElementById('detail-rating');
    if (!detailRating) return;

    const ratingValue = detailRating.querySelector('.rating-value');
    const ratingCount = detailRating.querySelector('.rating-count');

    if (ratingValue) {
        const safeAverageRating = Number(averageRating || 0);
        ratingValue.textContent = safeAverageRating.toFixed(1);
    }

    if (ratingCount) {
        ratingCount.textContent = `(${reviewCount || 0})`;
    }

    detailRating.style.display = 'flex';
}

/**
 * Setup favorite button functionality
 */
function setupFavoriteButton(placeId) {
    const btnFavorite = document.getElementById('btn-favorite');
    if (!btnFavorite) return;

    if (window.Auth && Auth.isLoggedIn()) {
        checkFavoriteStatus(placeId, btnFavorite);
    } else {
        setFavoriteButtonState(btnFavorite, false);
    }

    btnFavorite.addEventListener('click', function (e) {
        e.preventDefault();

        if (!window.Auth || !Auth.isLoggedIn()) {
            if (window.Auth) {
                Auth.goToLogin();
            }
            return;
        }

        toggleFavorite(placeId, btnFavorite);
    });

    window.addEventListener('auth:changed', function (event) {
        if (event.detail && event.detail.loggedIn) {
            checkFavoriteStatus(placeId, btnFavorite);
            return;
        }

        setFavoriteButtonState(btnFavorite, false);
    });
}

/**
 * Check if place is already favorited
 */
async function checkFavoriteStatus(placeId, btnFavorite) {
    try {
        const response = await Auth.authFetch('/me/favorite/place-ids');
        if (!response.ok) return;

        const favoriteIds = await response.json();
        const isFavorite = favoriteIds.includes(parseInt(placeId, 10));

        setFavoriteButtonState(btnFavorite, isFavorite);
    } catch (error) {
        console.error('Error checking favorite status:', error);
    }
}

/**
 * Toggle favorite status (add or remove)
 */
async function toggleFavorite(placeId, btnFavorite) {
    const isFavorited = btnFavorite.classList.contains('active');
    const endpoint = isFavorited
        ? `/me/favorite/delete/${placeId}`
        : `/me/favorite/add/${placeId}`;
    const method = isFavorited ? 'DELETE' : 'POST';

    try {
        const response = await Auth.authFetch(endpoint, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            if (response.status === 401) {
                Auth.goToLogin();
                return;
            }

            console.error('Failed to toggle favorite');
            return;
        }

        setFavoriteButtonState(btnFavorite, !isFavorited);
    } catch (error) {
        console.error('Error toggling favorite:', error);
    }
}