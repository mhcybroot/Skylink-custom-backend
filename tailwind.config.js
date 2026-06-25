/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/main/resources/templates/**/*.html",
    "./src/main/resources/static/js/**/*.js"
  ],
  corePlugins: {
    // Disable preflight to prevent Tailwind from overwriting Bootstrap base styles
    preflight: false,
  },
  theme: {
    extend: {},
  },
  plugins: [],
}
