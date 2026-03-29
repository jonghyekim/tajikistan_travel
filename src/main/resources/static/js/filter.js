/**
 * Filter page functionality
 * - Load ratings and favorites for each place card
 * - Handle favorite button interactions
 */

document.addEventListener('DOMContentLoaded', async () => {
  // Initialize ratings and favorites when page loads
  await loadRatings();
  await loadFavorites();
  attachFavoriteButtonListeners();
});

/**
 * Load average ratings for all visible place cards
 */
async function loadRatings() {
  const ratingElements = document.querySelectorAll('.rating-value[data-place-id]');
  
  // Create array of promises for parallel loading
  const ratingPromises = Array.from(ratingElements).map(async (element) => {
    const placeId = element.getAttribute('data-place-id');
    try {
      const rating = await fetchRating(placeId);
      element.textContent = rating.toFixed(1);
    } catch (error) {
      console.error(`Failed to load rating for place ${placeId}:`, error);
      element.textContent = '0.0';
    }
  });
  
  await Promise.all(ratingPromises);
}

/**
 * Fetch average rating for a specific place
 * @param {number} placeId - The place ID
 * @returns {Promise<number>} Average rating (0-5)
 */
async function fetchRating(placeId) {
  const response = await Auth.authFetch(`/api/reviews/${placeId}`);
  
  if (!response.ok) {
    return 0;
  }
  
  const data = await response.json();
  return data.averageRating || 0;
}

/**
 * Load favorite status for current user
 * Marks favorite buttons for places in user's favorites list
 */
async function loadFavorites() {
  try {
    const response = await Auth.authFetch('/me/favorite/place-ids');
    
    // 401 means user is not logged in - this is okay
    if (response.status === 401) {
      return;
    }
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const favoriteIds = await response.json();
    
    // Mark buttons for favorite places
    favoriteIds.forEach(placeId => {
      const btn = document.querySelector(`.favorite-btn[data-place-id="${placeId}"]`);
      if (btn) {
        btn.classList.add('is-favorite');
        updateFavoriteButtonIcon(btn);
      }
    });
  } catch (error) {
    // Silently fail for non-auth errors to avoid disrupting page load
    if (error.message && !error.message.includes('401')) {
      console.warn('Could not load favorites:', error);
    }
  }
}

/**
 * Update the icon for a favorite button based on its state
 * @param {HTMLElement} button - The favorite button element
 */
function updateFavoriteButtonIcon(button) {
  const icon = button.querySelector('.material-icons');
  if (button.classList.contains('is-favorite')) {
    icon.textContent = 'favorite';
  } else {
    icon.textContent = 'favorite_border';
  }
}

/**
 * Attach click listeners to all favorite buttons
 */
function attachFavoriteButtonListeners() {
  const buttons = document.querySelectorAll('.favorite-btn');
  
  buttons.forEach(btn => {
    btn.addEventListener('click', handleFavoriteClick);
  });
}

/**
 * Handle click on favorite button
 * @param {Event} event - Click event
 */
async function handleFavoriteClick(event) {
  // Prevent default link behavior
  event.preventDefault();
  event.stopPropagation();
  
  const placeId = this.getAttribute('data-place-id');
  const isFavorite = this.classList.contains('is-favorite');
  
  try {
    if (isFavorite) {
      // Remove from favorites
      const response = await Auth.authFetch(`/me/favorite/delete/${placeId}`, {
        method: 'DELETE'
      });
      
      if (response.status === 401) {
        handleUnauthorized();
        return;
      }
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      this.classList.remove('is-favorite');
      updateFavoriteButtonIcon(this);
    } else {
      // Add to favorites
      const response = await Auth.authFetch(`/me/favorite/add/${placeId}`, {
        method: 'POST'
      });
      
      if (response.status === 401) {
        handleUnauthorized();
        return;
      }
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      this.classList.add('is-favorite');
      updateFavoriteButtonIcon(this);
    }
  } catch (error) {
    console.error('Failed to update favorite status:', error);
    alert('찜 정보를 업데이트하는데 실패했습니다.');
  }
}

/**
 * Handle unauthorized access (not logged in)
 */
function handleUnauthorized() {
  const message = '로그인이 필요합니다.';
  alert(message);
  window.location.href = '/login';
}
