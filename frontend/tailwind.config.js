/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        dark: {
          950: '#030712', // deep background
          900: '#080d1a', // card/secondary background
          850: '#0f172a', // alternative panel
          800: '#1e293b', // element hover
          700: '#334155', // borders
          600: '#475569', // secondary text
        },
        primary: {
          500: '#6366f1', // electric indigo accent
          600: '#4f46e5',
          700: '#4338ca',
        },
        cyber: {
          green: '#10b981',
          red: '#ef4444',
          orange: '#f59e0b',
          blue: '#3b82f6',
        }
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        mono: ['Fira Code', 'JetBrains Mono', 'monospace'],
      }
    },
  },
  plugins: [],
}
