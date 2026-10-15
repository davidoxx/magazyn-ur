import { CssBaseline, ThemeProvider } from "@mui/material";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { HashRouter } from "react-router";
import { App } from "./App";
import { motyw } from "./motyw";
import { SesjaProvider } from "./sesja";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ThemeProvider theme={motyw}>
      <CssBaseline />
      <HashRouter>
        <SesjaProvider>
          <App />
        </SesjaProvider>
      </HashRouter>
    </ThemeProvider>
  </StrictMode>,
);
