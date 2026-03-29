document.addEventListener("DOMContentLoaded", async function () {
  if (!window.Auth) return;

  const placeId = document.querySelector('meta[name="place-id"]')?.content;
  if (!placeId) return;

  const formContainer = document.getElementById("reviews-form-container");
  const listContainer = document.getElementById("reviews-list-container");
  const reviewsList = document.getElementById("reviews-list");
  const reviewsEmpty = document.getElementById("reviews-empty");
  const starRating = document.getElementById("star-rating");
  const submitBtn = document.getElementById("submit-review-btn");
  const contentTextarea = document.getElementById("review-content");

  let selectedRating = 0;
  let currentUserId = null;

  // Initialize UI based on login status
  function updateAuthUI() {
    if (window.Auth.isLoggedIn()) {
      if (formContainer) formContainer.style.display = "block";
    } else {
      if (formContainer) formContainer.style.display = "none";
    }
  }

  // Star rating selection
  if (starRating) {
    const starBtns = starRating.querySelectorAll(".star-btn");
    starBtns.forEach((btn) => {
      btn.addEventListener("click", function (e) {
        e.preventDefault();
        selectedRating = Number(this.dataset.rating);
        updateStarDisplay();
      });

      btn.addEventListener("mouseover", function () {
        const hoverRating = Number(this.dataset.rating);
        starBtns.forEach((b, idx) => {
          b.dataset.selected = idx < hoverRating ? "true" : "false";
        });
      });
    });

    starRating.addEventListener("mouseout", updateStarDisplay);
  }

  function updateStarDisplay() {
    const starBtns = starRating.querySelectorAll(".star-btn");
    const displaySpan = starRating.querySelector(".star-display");

    starBtns.forEach((btn, idx) => {
      btn.dataset.selected = idx < selectedRating ? "true" : "false";
    });

    if (displaySpan) {
      displaySpan.textContent = selectedRating > 0 ? selectedRating.toFixed(1) : "";
    }
  }

  // Format relative time (e.g., "2 weeks ago")
  function formatTimeAgo(isoString) {
    const createdDate = new Date(isoString);
    const now = new Date();
    const diffMs = now - createdDate;
    const diffSecs = Math.floor(diffMs / 1000);
    const diffMins = Math.floor(diffSecs / 60);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);
    const diffWeeks = Math.floor(diffDays / 7);

    if (diffSecs < 60) return "just now";
    if (diffMins < 60) return diffMins + (diffMins === 1 ? " minute ago" : " minutes ago");
    if (diffHours < 24) return diffHours + (diffHours === 1 ? " hour ago" : " hours ago");
    if (diffDays < 7) return diffDays + (diffDays === 1 ? " day ago" : " days ago");
    if (diffWeeks < 4) return diffWeeks + (diffWeeks === 1 ? " week ago" : " weeks ago");

    return createdDate.toLocaleDateString();
  }

  // Render star rating (filled stars)
  function renderStars(rating) {
    let html = "";
    for (let i = 1; i <= 5; i++) {
      html += `<span class="star">${i <= rating ? "★" : "☆"}</span>`;
    }
    return html;
  }

  // Build review item HTML
  function buildReviewHTML(review) {
    const isMine = review.mine;
    const deleteBtn = isMine ? `<button type="button" class="review-delete-btn delete-review" data-review-id="${review.reviewId}">Delete</button>` : "";

    return `
      <div class="review-item" data-review-id="${review.reviewId}">
        <div class="review-header">
          <div class="review-meta">
            <span class="review-author">${escapeHtml(review.nickname)}</span>
            <div class="review-rating">${renderStars(review.rating)}</div>
          </div>
          <div class="review-actions">
            <span class="review-time">${formatTimeAgo(review.createdAt)}</span>
            ${deleteBtn}
          </div>
        </div>
        <div class="review-content">${review.content ? escapeHtml(review.content) : ""}</div>
      </div>
    `;
  }

  function escapeHtml(text) {
    return String(text || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/\"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  // Load reviews list
  async function loadReviews() {
    if (!reviewsList || !reviewsEmpty) return;

    try {
      const res = await (window.Auth.isLoggedIn() ? window.Auth.authFetch(`/api/reviews/${placeId}`) : fetch(`/api/reviews/${placeId}`));

      if (!res.ok) {
        reviewsEmpty.style.display = "block";
        return;
      }

      const data = await res.json();
      const reviews = data.reviews || [];

      if (reviews.length === 0) {
        reviewsList.innerHTML = "";
        reviewsEmpty.style.display = "block";
      } else {
        const html = reviews.map(buildReviewHTML).join("");
        reviewsList.innerHTML = html;
        reviewsEmpty.style.display = "none";

        // Attach delete listeners
        reviewsList.querySelectorAll(".delete-review").forEach((btn) => {
          btn.addEventListener("click", handleDeleteReview);
        });
      }
    } catch (error) {
      console.error("Failed to load reviews:", error);
      reviewsEmpty.style.display = "block";
    }
  }

  // Submit review
  async function handleSubmitReview(e) {
    e.preventDefault();

    if (!window.Auth.isLoggedIn()) {
      window.Auth.goToLogin();
      return;
    }

    if (selectedRating === 0) {
      alert("Please select a rating");
      return;
    }

    const content = contentTextarea.value.trim();

    const payload = {
      placeId: Number(placeId),
      rating: selectedRating,
      content: content || null,
    };

    try {
      submitBtn.disabled = true;
      const res = await window.Auth.authFetch("/api/reviews", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        alert("Failed to submit review");
        return;
      }

      // Reset form
      selectedRating = 0;
      contentTextarea.value = "";
      updateStarDisplay();

      // Reload list
      await loadReviews();
    } catch (error) {
      console.error("Error submitting review:", error);
      alert("Error submitting review");
    } finally {
      submitBtn.disabled = false;
    }
  }

  // Delete review
  async function handleDeleteReview(e) {
    e.preventDefault();

    if (!confirm("Are you sure you want to delete this review?")) return;

    const reviewId = Number(this.dataset.reviewId);
    if (!reviewId) return;

    try {
      const res = await window.Auth.authFetch(`/api/reviews/${reviewId}`, {
        method: "DELETE",
      });

      if (!res.ok) {
        alert("Failed to delete review");
        return;
      }

      // Remove item from DOM
      const reviewItem = document.querySelector(`[data-review-id="${reviewId}"]`);
      if (reviewItem) reviewItem.remove();

      // Check empty state
      if (reviewsList.children.length === 0) {
        reviewsEmpty.style.display = "block";
      }
    } catch (error) {
      console.error("Error deleting review:", error);
      alert("Error deleting review");
    }
  }

  // Event listeners
  if (submitBtn) {
    submitBtn.addEventListener("click", handleSubmitReview);
  }

  // Initial load
  updateAuthUI();
  await loadReviews();
});
