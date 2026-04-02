/* ========================================
   ShopFlow - Interactive JavaScript
   Modern E-commerce UI Interactions
   ======================================== */

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', function () {
  initSidebar();
  initAlerts();
  initTableSearch();
  initDropdowns();
  initImagePreview();
});

/* ========== Sidebar Toggle for Mobile ========== */
function initSidebar() {
  const toggleBtn = document.getElementById('sidebarToggle');
  const sidebar = document.querySelector('.sf-sidebar');
  
  if (toggleBtn && sidebar) {
    toggleBtn.addEventListener('click', function() {
      sidebar.classList.toggle('open');
    });
    
    // Close sidebar when clicking outside on mobile
    document.addEventListener('click', function(event) {
      if (window.innerWidth <= 992) {
        if (!sidebar.contains(event.target) && !toggleBtn.contains(event.target)) {
          sidebar.classList.remove('open');
        }
      }
    });
  }
}

/* ========== Alert Auto-dismiss ========== */
function initAlerts() {
  setTimeout(() => {
    const alerts = document.querySelectorAll('.alert-autohide, .sf-alert');
    alerts.forEach(alert => {
      if (alert.classList.contains('alert')) {
        // Bootstrap 5 alert
        const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
        bsAlert.close();
      } else {
        // Custom alert
        alert.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        alert.style.opacity = '0';
        alert.style.transform = 'translateY(-10px)';
        setTimeout(() => alert.remove(), 300);
      }
    });
  }, 4000);
}

/* ========== Table Search Filter ========== */
function initTableSearch() {
  const searchInput = document.getElementById('tableSearch');
  if (searchInput) {
    searchInput.addEventListener('input', function () {
      const query = this.value.toLowerCase();
      const table = document.querySelector('.sf-table, .table');
      if (!table) return;
      
      const rows = table.querySelectorAll('tbody tr');
      rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        row.style.display = text.includes(query) ? '' : 'none';
      });
    });
  }
}

/* ========== Dropdown Menus ========== */
function initDropdowns() {
  const dropdownToggles = document.querySelectorAll('[data-dropdown-toggle]');
  
  dropdownToggles.forEach(toggle => {
    toggle.addEventListener('click', function(e) {
      e.stopPropagation();
      const dropdown = this.closest('.sf-dropdown');
      
      // Close other dropdowns
      document.querySelectorAll('.sf-dropdown.active').forEach(other => {
        if (other !== dropdown) {
          other.classList.remove('active');
        }
      });
      
      // Toggle current dropdown
      dropdown.classList.toggle('active');
    });
  });
  
  // Close dropdowns when clicking outside
  document.addEventListener('click', function() {
    document.querySelectorAll('.sf-dropdown.active').forEach(dropdown => {
      dropdown.classList.remove('active');
    });
  });
}

/* ========== Image Preview on URL Input ========== */
function initImagePreview() {
  const imageUrlInputs = document.querySelectorAll('input[name="imageUrl"], input[id*="imageUrl"]');
  
  imageUrlInputs.forEach(input => {
    // Create preview container if it doesn't exist
    let preview = input.parentElement.querySelector('.image-preview');
    if (!preview) {
      preview = document.createElement('div');
      preview.className = 'image-preview mt-2';
      preview.style.cssText = 'display: none; max-width: 200px; border-radius: 8px; overflow: hidden; box-shadow: var(--sf-card-shadow);';
      input.parentElement.appendChild(preview);
    }
    
    // Update preview on input
    input.addEventListener('input', function() {
      const url = this.value.trim();
      if (url) {
        const img = document.createElement('img');
        img.src = url;
        img.style.cssText = 'width: 100%; height: auto; display: block;';
        img.onerror = function() {
          preview.style.display = 'none';
        };
        img.onload = function() {
          preview.innerHTML = '';
          preview.appendChild(img);
          preview.style.display = 'block';
        };
      } else {
        preview.style.display = 'none';
      }
    });
    
    // Trigger on page load if value exists
    if (input.value) {
      input.dispatchEvent(new Event('input'));
    }
  });
}

/* ========== Confirmation Dialogs ========== */
function confirmAction(message) {
  return confirm(message || 'Are you sure you want to perform this action?');
}

// Add confirmation to delete buttons
document.addEventListener('DOMContentLoaded', function() {
  const deleteForms = document.querySelectorAll('form[action*="delete"], form[action*="remove"]');
  deleteForms.forEach(form => {
    form.addEventListener('submit', function(e) {
      if (!confirmAction('Are you sure you want to delete this item? This action cannot be undone.')) {
        e.preventDefault();
      }
    });
  });
});

/* ========== Quantity Input Controls ========== */
document.addEventListener('DOMContentLoaded', function() {
  const quantityInputs = document.querySelectorAll('input[type="number"][name*="quantity"]');
  quantityInputs.forEach(input => {
    input.addEventListener('change', function() {
      if (this.value < 1) this.value = 1;
      if (this.max && this.value > parseInt(this.max)) this.value = this.max;
    });
  });
});

/* ========== Form Validation Helpers ========== */
function validateForm(formId) {
  const form = document.getElementById(formId);
  if (form) {
    const inputs = form.querySelectorAll('input[required], select[required], textarea[required]');
    let isValid = true;
    
    inputs.forEach(input => {
      if (!input.value.trim()) {
        input.classList.add('is-invalid');
        isValid = false;
      } else {
        input.classList.remove('is-invalid');
      }
    });
    
    return isValid;
  }
  return true;
}

/* ========== Smooth Scroll ========== */
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
  anchor.addEventListener('click', function (e) {
    const target = document.querySelector(this.getAttribute('href'));
    if (target) {
      e.preventDefault();
      target.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      });
    }
  });
});

/* ========== Status Filter for Orders ========== */
function initStatusFilter() {
  const filterSelect = document.getElementById('statusFilter');
  if (filterSelect) {
    filterSelect.addEventListener('change', function() {
      const status = this.value.toLowerCase();
      const rows = document.querySelectorAll('tbody tr');
      
      rows.forEach(row => {
        if (!status || status === 'all') {
          row.style.display = '';
        } else {
          const rowStatus = row.querySelector('.badge')?.textContent.toLowerCase().trim();
          row.style.display = rowStatus && rowStatus.includes(status) ? '' : 'none';
        }
      });
    });
  }
}

// Initialize status filter
document.addEventListener('DOMContentLoaded', initStatusFilter);

/* ========== Loading State Helper ========== */
function showLoading(buttonElement) {
  if (buttonElement) {
    buttonElement.disabled = true;
    const originalText = buttonElement.innerHTML;
    buttonElement.setAttribute('data-original-text', originalText);
    buttonElement.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Loading...';
  }
}

function hideLoading(buttonElement) {
  if (buttonElement) {
    buttonElement.disabled = false;
    const originalText = buttonElement.getAttribute('data-original-text');
    if (originalText) {
      buttonElement.innerHTML = originalText;
    }
  }
}

/* ========== Copy to Clipboard ========== */
function copyToClipboard(text) {
  navigator.clipboard.writeText(text).then(() => {
    showNotification('Copied to clipboard!', 'success');
  }).catch(() => {
    showNotification('Failed to copy', 'danger');
  });
}

/* ========== Toast Notifications ========== */
function showNotification(message, type = 'info') {
  const toast = document.createElement('div');
  toast.className = `sf-alert sf-alert-${type}`;
  toast.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
  
  const icon = {
    success: 'fa-check-circle',
    danger: 'fa-exclamation-circle',
    warning: 'fa-exclamation-triangle',
    info: 'fa-info-circle'
  }[type] || 'fa-info-circle';
  
  toast.innerHTML = `<i class="fas ${icon}"></i> <span>${message}</span>`;
  document.body.appendChild(toast);
  
  setTimeout(() => {
    toast.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}

/* ========== Export Functions for Global Use ========== */
window.ShopFlow = {
  confirmAction,
  validateForm,
  showLoading,
  hideLoading,
  copyToClipboard,
  showNotification
};
