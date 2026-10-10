(() => {
  const API_BASE = ["localhost", "127.0.0.1"].includes(window.location.hostname)
    ? "https://delicias-peruanas-api.onrender.com"
    : window.location.origin;

  let token = "";
  let session = { correo: "", rol: "" };

  const $ = (id) => document.getElementById(id);

  const loginForm = $("loginForm");
  const reservationForm = $("reservationForm");
  const loginSection = $("loginSection");
  const dashboardSection = $("dashboardSection");
  const reservationSection = $("reservationSection");

  const setMessage = (element, text, type = "") => {
    element.textContent = text;
    element.className = "message" + (type ? " " + type : "");
  };

  const safeJson = async (response) => {
    const text = await response.text();
    if (!text) return null;
    try {
      return JSON.parse(text);
    } catch {
      return { message: text };
    }
  };

  const authHeaders = () => ({
    "Content-Type": "application/json",
    "Authorization": "Bearer " + token
  });

  const renderCategories = (items) => {
    const box = $("categoriesList");
    if (!Array.isArray(items) || items.length === 0) {
      box.className = "list-box empty";
      box.textContent = "No hay categorías registradas.";
      return;
    }

    box.className = "list-box";
    box.innerHTML = items.map((item) => `
      <div class="item">
        <strong>${item.nombre ?? "Sin nombre"}</strong>
        <small>${item.tipo ?? "Sin tipo"} · ID ${item.id ?? "—"}</small>
      </div>
    `).join("");
  };

  const renderProducts = (items) => {
    const box = $("productsList");
    if (!Array.isArray(items) || items.length === 0) {
      box.className = "list-box empty";
      box.textContent = "No hay productos registrados.";
      return;
    }

    box.className = "list-box";
    box.innerHTML = items.map((item) => `
      <div class="item">
        <strong>${item.nombre ?? "Sin nombre"}</strong>
        <small>S/ ${Number(item.precio ?? 0).toFixed(2)} · ${item.disponible ? "Disponible" : "No disponible"}</small>
      </div>
    `).join("");
  };

  loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const message = $("loginMessage");
    setMessage(message, "Autenticando…");

    try {
      const response = await fetch(API_BASE + "/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          correo: $("correo").value.trim(),
          password: $("password").value
        })
      });

      const data = await safeJson(response);

      if (!response.ok || !data?.token) {
        throw new Error(data?.message || "Credenciales inválidas o servicio no disponible.");
      }

      token = data.token;
      session = { correo: data.correo, rol: data.rol };

      $("authStatus").textContent = "Autenticado";
      $("authStatus").className = "status status--ok";
      $("sessionEmail").textContent = session.correo;
      $("sessionRole").textContent = session.rol;

      dashboardSection.classList.remove("hidden");
      reservationSection.classList.remove("hidden");
      setMessage(message, "Inicio de sesión correcto. JWT recibido desde el backend.", "ok");
      dashboardSection.scrollIntoView({ behavior: "smooth", block: "start" });
    } catch (error) {
      setMessage(message, error.message, "error");
    }
  });

  $("loadDataButton").addEventListener("click", async () => {
    const message = $("dataMessage");
    setMessage(message, "Consultando endpoints protegidos…");

    try {
      const [categoriesResponse, productsResponse] = await Promise.all([
        fetch(API_BASE + "/api/categorias", { headers: authHeaders() }),
        fetch(API_BASE + "/api/productos", { headers: authHeaders() })
      ]);

      const categories = await safeJson(categoriesResponse);
      const products = await safeJson(productsResponse);

      if (!categoriesResponse.ok) {
        throw new Error(categories?.message || "No se pudieron consultar las categorías.");
      }
      if (!productsResponse.ok) {
        throw new Error(products?.message || "No se pudieron consultar los productos.");
      }

      renderCategories(categories);
      renderProducts(products);
      setMessage(message, "Consulta protegida correcta: HTTP 200 con JWT válido.", "ok");
    } catch (error) {
      setMessage(message, error.message, "error");
    }
  });

  reservationForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const message = $("reservationMessage");
    const responseBox = $("reservationResponse");
    responseBox.classList.add("hidden");
    setMessage(message, "Enviando reserva al backend…");

    const payload = {
      cliente: $("cliente").value.trim(),
      fecha: $("fecha").value,
      horaInicio: $("horaInicio").value,
      horaFin: $("horaFin").value,
      cantidadPersonas: Number($("cantidadPersonas").value),
      mesa: { id: Number($("mesaId").value) }
    };

    try {
      const response = await fetch(API_BASE + "/api/reservas", {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify(payload)
      });

      const data = await safeJson(response);

      responseBox.textContent = JSON.stringify({
        http: response.status,
        respuesta: data
      }, null, 2);
      responseBox.classList.remove("hidden");

      if (response.status === 201) {
        setMessage(message, "Reserva creada correctamente: HTTP 201 Created.", "ok");
        return;
      }

      if (response.status === 409) {
        setMessage(message, "Regla de negocio validada: HTTP 409 Conflict por superposición.", "error");
        return;
      }

      throw new Error(data?.message || "La operación terminó con HTTP " + response.status + ".");
    } catch (error) {
      setMessage(message, error.message, "error");
    }
  });

  $("logoutButton").addEventListener("click", () => {
    token = "";
    session = { correo: "", rol: "" };
    dashboardSection.classList.add("hidden");
    reservationSection.classList.add("hidden");
    $("authStatus").textContent = "Sin autenticar";
    $("authStatus").className = "status status--idle";
    loginForm.reset();
    setMessage($("loginMessage"), "Sesión cerrada.");
    loginSection.scrollIntoView({ behavior: "smooth", block: "start" });
  });

  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  $("fecha").value = tomorrow.toISOString().slice(0, 10);
})();
