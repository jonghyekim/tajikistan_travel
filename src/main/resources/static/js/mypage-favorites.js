// mypage-favorites.js - Handle My Favorites page (using filter.html design)

document.addEventListener('DOMContentLoaded', async function() {
    console.log('mypage-favorites.js loaded');

    // Check if user is logged in
    if (!Auth || !Auth.isLoggedIn()) {
        console.log('User not logged in, redirecting...');
        Auth.goToLogin();
        return;
    }

    // Get language from URL or default to 'en'
    const urlParams = new URLSearchParams(window.location.search);
    const lang = urlParams.get('lang') || 'en';
    console.log('Current language:', lang);

    // Load favorites
    await loadFavorites(lang);
});

/**
 * Load favorites from API
 */
async function loadFavorites(lang) {
    try {
        console.log('Loading favorites for lang:', lang);

        // First get the list of favorite place IDs
        const idsResponse = await Auth.authFetch(`/me/favorite/place-ids?lang=${lang}`);
        console.log('IDs response status:', idsResponse.status);

        if (!idsResponse.ok) {
            console.warn('Failed to load favorite place IDs:', idsResponse.status);
            showEmpty();
            return;
        }

        const favoriteIds = await idsResponse.json();
        console.log('Favorite IDs:', favoriteIds);

        if (!favoriteIds || favoriteIds.length === 0) {
            console.log('No favorites found');
            showEmpty();
            return;
        }

        // Then get the full list with details
        const listResponse = await Auth.authFetch(`/me/favorite/list?lang=${lang}`);
        console.log('List response status:', listResponse.status);

        if (!listResponse.ok) {
            console.warn('Failed to load favorite list:', listResponse.status);
            showEmpty();
            return;
        }

        const places = await listResponse.json();
        console.log('Places data:', places);

        if (places && places.length > 0) {
            displayFavorites(places, lang);
        } else {
            console.log('No places data returned');
            showEmpty();
        }
    } catch (error) {
        console.error('Error loading favorites:', error);
        showEmpty();
    }
}

/**
 * Display favorites in grid
 */
function displayFavorites(places, lang) {
    console.log('Displaying', places.length, 'places');

    const container = document.getElementById('favorites-grid');
    if (!container) {
        console.error('Card grid container not found');
        return;
    }

    // Clear previous content
    container.innerHTML = '';

    // Hide empty message
    const emptyDiv = document.getElementById('favorites-empty');
    if (emptyDiv) emptyDiv.style.display = 'none';

    // Update results count
    const resultsDiv = document.getElementById('favorites-count');
    if (resultsDiv) {
        const translations = {
            'en': `${places.length} Results`,
            'ko': `결과 ${places.length}건`,
            'ru': `${places.length} результатов`,
            'tg': `${places.length} натиҷа`
        };
        const text = translations[lang] || translations['en'];
        resultsDiv.textContent = text;
        console.log('Updated results count:', text);
    }

    // Create cards for each place
    places.forEach((place, index) => {
        try {
            const card = createPlaceCard(place, lang);
            if (card) {
                container.appendChild(card);
                console.log('Added card', index + 1, ':', place.title);
            }
        } catch (err) {
            console.error('Error creating card for place:', place, err);
        }
    });

    // Hide skeleton loading
    const skeleton = document.getElementById('loading-skeleton');
    if (skeleton) {
        skeleton.style.display = 'none';
        console.log('Skeleton hidden');
    }

    // Show main content
    const mainContent = document.getElementById('main-content');
    if (mainContent) {
        mainContent.style.visibility = 'visible';
        mainContent.style.opacity = '1';
        console.log('Main content shown');
    }
}

/**
 * Create a place card element (matching filter.html design)
 */
function createPlaceCard(place, lang) {
    if (!place || !place.placeId) {
        console.warn('Invalid place object:', place);
        return null;
    }

    const imageUrl = place.imageUrl || '../assets/images/home_reg_2.png';
    const title = place.title || 'No Title';
    const category = place.categoryName || 'Category';
    const region = place.regionName || 'Region';
    const content = place.content || '';

    // Create the link element
    const link = document.createElement('a');
    link.className = 'place-card-link';
    link.href = `/detail/${place.placeId}?lang=${lang}&back=/me/favorites?lang=${lang}`;
    link.style.textDecoration = 'none';
    link.style.color = 'inherit';

    // Create the article element
    const article = document.createElement('article');
    article.className = 'place-card';

    // Create the thumbnail div
    const thumbDiv = document.createElement('div');
    thumbDiv.className = 'place-thumb';
    
    const img = document.createElement('img');
    img.src = escapeHtml(imageUrl);
    img.alt = 'Place image';
    thumbDiv.appendChild(img);

    const categoryBadge = document.createElement('span');
    categoryBadge.className = 'badge badge-top';
    categoryBadge.textContent = escapeHtml(category);
    thumbDiv.appendChild(categoryBadge);

    article.appendChild(thumbDiv);

    // Create the body div
    const bodyDiv = document.createElement('div');
    bodyDiv.className = 'place-body';

    const titleElem = document.createElement('h3');
    titleElem.className = 'place-title';
    titleElem.textContent = escapeHtml(title);
    bodyDiv.appendChild(titleElem);

    if (content) {
        const descElem = document.createElement('p');
        descElem.className = 'place-desc';
        descElem.textContent = escapeHtml(content);
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
    regionText.textContent = escapeHtml(region);
    regionBadge.appendChild(regionText);

    tagsDiv.appendChild(regionBadge);
    bodyDiv.appendChild(tagsDiv);

    article.appendChild(bodyDiv);
    link.appendChild(article);

    return link;
}

/**
 * Show empty state message
 */
function showEmpty() {
    console.log('Showing empty state');

    const emptyDiv = document.getElementById('favorites-empty');
    if (emptyDiv) {
        emptyDiv.style.display = 'block';
    }

    const container = document.getElementById('favorites-grid');
    if (container) {
        container.innerHTML = '';
    }

    // Update results count to 0
    const resultsDiv = document.getElementById('favorites-count');
    if (resultsDiv) {
        resultsDiv.textContent = '0 Results';
    }

    // Hide skeleton loading
    const skeleton = document.getElementById('loading-skeleton');
    if (skeleton) skeleton.style.display = 'none';

    // Show main content
    const mainContent = document.getElementById('main-content');
    if (mainContent) {
        mainContent.style.visibility = 'visible';
        mainContent.style.opacity = '1';
    }
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}
