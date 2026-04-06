// detail.js - Handle rating display and favorite functionality

document.addEventListener('DOMContentLoaded', function() {
    const placeIdMeta = document.querySelector('meta[name="place-id"]');
    const placeId = placeIdMeta ? placeIdMeta.getAttribute('content') : null;

    if (!placeId) {
        console.warn('Place ID not found in meta tag');
        return;
    }

    // Load rating and favorite status
    loadRatingAndFavorite(placeId);

    // Setup favorite button
    setupFavoriteButton(placeId);
});

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
        ratingValue.textContent = averageRating.toFixed(1);
    }

    if (ratingCount) {
        ratingCount.textContent = `(${reviewCount})`;
    }

    detailRating.style.display = 'flex';
}

/**
 * Setup favorite button functionality
 */
function setupFavoriteButton(placeId) {
    const btnFavorite = document.getElementById('btn-favorite');
    if (!btnFavorite) return;

    // Check if user is logged in and get favorite status
    if (Auth && Auth.isLoggedIn()) {
        checkFavoriteStatus(placeId, btnFavorite);
    } else {
        setFavoriteButtonState(btnFavorite, false);
    }

    // Add click handler
    btnFavorite.addEventListener('click', function(e) {
        e.preventDefault();

        if (!Auth || !Auth.isLoggedIn()) {
            Auth.goToLogin();
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
    const endpoint = isFavorited ? 
        `/me/favorite/delete/${placeId}` : 
        `/me/favorite/add/${placeId}`;
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
                // Token expired, redirect to login
                Auth.goToLogin();
                return;
            }
            console.error('Failed to toggle favorite');
            return;
        }

        // Update button state
        if (isFavorited) {
            setFavoriteButtonState(btnFavorite, false);
        } else {
            setFavoriteButtonState(btnFavorite, true);
        }
    } catch (error) {
        console.error('Error toggling favorite:', error);
    }
}
