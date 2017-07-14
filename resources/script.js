(function() {
  document.addEventListener("DOMContentLoaded", onLoaded);

  function onLoaded(event) {
    // Page reloading
    setInterval(async () => {
      const response = await fetch(location.href);
      const text = await response.text();


      const el = document.createElement("div")
      el.innerHTML = text;

      document.querySelector("#main-content")
        .replaceWith(el.querySelector("#main-content"));
    }, 5000);
  }
})();
