// Theme switcher functionality
function setupThemeToggle() {
  const toggleSwitch = document.querySelector('#checkbox');
  const currentTheme = localStorage.getItem('theme');

  if (currentTheme) {
    document.documentElement.setAttribute('data-theme', currentTheme);
    if (currentTheme === 'dark') {
      toggleSwitch.checked = true;
    }
  }

  toggleSwitch.addEventListener('change', switchTheme);

  function switchTheme(e) {
    if (e.target.checked) {
      document.documentElement.setAttribute('data-theme', 'dark');
      localStorage.setItem('theme', 'dark');
    } else {
      document.documentElement.setAttribute('data-theme', 'light');
      localStorage.setItem('theme', 'light');
    }
  }
}

// XML syntax highlighting
function highlightXML(text) {
  // Replace tags
  text = text.replace(/(&lt;\/?)(\w+)(\s|&gt;)/g, '$1<span class="xml-tag">$2</span>$3');
  
  // Replace attributes
  text = text.replace(/(\s)(\w+)(\s*=\s*)/g, '$1<span class="xml-attribute">$2</span>$3');
  
  // Replace values
  text = text.replace(/(".*?")/g, '<span class="xml-value">$1</span>');
  
  // Replace comments
  text = text.replace(/(&lt;!--.*?--&gt;)/g, '<span class="xml-comment">$1</span>');
  
  return text;
}

// Apply syntax highlighting to the XML output
function updateOutput() {
  // ... existing code ...
  
  // Apply syntax highlighting before displaying
  if (outputElement) {
    let formattedXML = formatXML(xmlString);
    formattedXML = escapeHTML(formattedXML);
    formattedXML = highlightXML(formattedXML);
    outputElement.innerHTML = formattedXML;
  }
}

// Helper function to escape HTML
function escapeHTML(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// Initialize everything when the DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
  // ... existing initialization code ...
  
  setupThemeToggle();
}); 