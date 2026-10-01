/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        "azul-oscuro": "#16325c",
        azul: "#1e4d8c",
        "azul-claro": "#eaf1fb",
        texto: "#1f2933",
        "texto-suave": "#5b6b79",
        borde: "#e2e8f0",
        fondo: "#f5f7fa",
        verde: "#1f9d55",
        "verde-claro": "#e3f7e9",
        rojo: "#d64545",
        "rojo-claro": "#fbe9e9",
        ambar: "#b7791f",
        "ambar-claro": "#fdf3df",
      },
      fontFamily: {
        sans: ['"Segoe UI"', "system-ui", "-apple-system", "sans-serif"],
      },
      boxShadow: {
        card: "0 1px 2px rgba(0, 0, 0, 0.05)",
        modal: "0 20px 50px rgba(0, 0, 0, 0.3)",
        login: "0 20px 40px rgba(0, 0, 0, 0.25)",
      },
    },
  },
  plugins: [],
};
