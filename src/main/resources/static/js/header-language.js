(function () {
  function syncExpanded(dropdown) {
    const summary = dropdown.querySelector(".language-trigger");
    if (!summary) return;
    summary.setAttribute("aria-expanded", dropdown.open ? "true" : "false");
  }

  function updateBackParam(url, lang) {
    const back = url.searchParams.get("back");
    if (!back || !back.startsWith("/")) return;

    try {
      const backUrl = new URL(back, window.location.origin);
      backUrl.searchParams.set("lang", lang);
      url.searchParams.set("back", backUrl.pathname + backUrl.search + backUrl.hash);
    } catch (error) {
      console.warn("Failed to normalize back URL for language switch.", error);
    }
  }

  function buildLanguageUrl(lang) {
    const url = new URL(window.location.href);
    url.searchParams.set("lang", lang);
    updateBackParam(url, lang);
    return url.toString();
  }

  function submitLanguageOption(option) {
    const selectedLang = option.value;
    if (!selectedLang) return;

    window.location.assign(buildLanguageUrl(selectedLang));
  }

  document.addEventListener("DOMContentLoaded", function () {
    const dropdowns = Array.from(document.querySelectorAll("[data-language-dropdown]"));
    if (!dropdowns.length) return;

    dropdowns.forEach(syncExpanded);

    dropdowns.forEach(function (dropdown) {
      dropdown.addEventListener("toggle", function () {
        if (dropdown.open) {
          dropdowns.forEach(function (otherDropdown) {
            if (otherDropdown !== dropdown && otherDropdown.open) {
              otherDropdown.open = false;
              syncExpanded(otherDropdown);
            }
          });
        }

        syncExpanded(dropdown);
      });
    });

    const options = Array.from(document.querySelectorAll(".language-option"));
    options.forEach(function (option) {
      option.addEventListener("click", function (event) {
        event.preventDefault();
        event.stopPropagation();

        const dropdown = option.closest("[data-language-dropdown]");
        if (dropdown) {
          dropdown.open = false;
          syncExpanded(dropdown);
        }

        submitLanguageOption(option);
      });
    });

    document.addEventListener("click", function (event) {
      dropdowns.forEach(function (dropdown) {
        if (!dropdown.contains(event.target)) {
          dropdown.open = false;
          syncExpanded(dropdown);
        }
      });
    });

    document.addEventListener("keydown", function (event) {
      if (event.key !== "Escape") return;

      dropdowns.forEach(function (dropdown) {
        if (dropdown.open) {
          dropdown.open = false;
          syncExpanded(dropdown);
        }
      });
    });
  });
})();
