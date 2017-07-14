(function() {
  document.addEventListener("DOMContentLoaded", onLoaded);

  function onLoaded(event) {
    // Page reloading
    setInterval(() => {
      fetch(location.href)
        .then((rsp) => rsp.text())
        .then((txt) => {
          const el = document.createElement("div");
          el.innerHTML = txt;

          document.querySelector("#main-content")
            .replaceWith(el.querySelector("#main-content"));
        });
    }, 2000);
  }
})();
