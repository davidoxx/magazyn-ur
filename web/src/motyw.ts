import { createTheme } from "@mui/material";

// Kolory jak w aplikacji (Material 3, bez kolorów z tapety), jasne i ciemne wg ustawień urządzenia
export const motyw = createTheme({
  cssVariables: { colorSchemeSelector: "media" },
  colorSchemes: {
    light: {
      palette: {
        primary: { main: "#6650a4" },
        secondary: { main: "#625b71" },
        error: { main: "#b3261e" },
        background: { default: "#fffbfe", paper: "#fffbfe" },
      },
    },
    dark: {
      palette: {
        primary: { main: "#d0bcff" },
        secondary: { main: "#ccc2dc" },
        error: { main: "#f2b8b5" },
        background: { default: "#1c1b1f", paper: "#1c1b1f" },
      },
    },
  },
  shape: { borderRadius: 12 },
  typography: { button: { textTransform: "none" } },
});
