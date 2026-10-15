import { Box, Button, CircularProgress, Container, Stack, TextField, Typography } from "@mui/material";
import { useState, type FormEvent } from "react";
import { useSesja } from "../sesja";
import { PoleHasla } from "../ui/wspolne";

export function Logowanie() {
  const { zaloguj, bladLogowania } = useSesja();
  const [login, ustawLogin] = useState("");
  const [haslo, ustawHaslo] = useState("");
  const [trwa, ustawTrwa] = useState(false);

  async function wyslij(e: FormEvent) {
    e.preventDefault();
    if (trwa) return;
    ustawTrwa(true);
    try {
      await zaloguj(login, haslo);
    } finally {
      ustawTrwa(false);
      ustawHaslo("");
    }
  }

  return (
    <Container maxWidth="xs">
      <Box component="form" onSubmit={wyslij} sx={{ pt: 6 }}>
        <Stack spacing={2}>
          <Typography variant="h4">Magazyn UR</Typography>
          <TextField
            label="Login"
            value={login}
            onChange={(e) => ustawLogin(e.target.value)}
            disabled={trwa}
            autoComplete="username"
            slotProps={{ htmlInput: { autoCapitalize: "none", autoCorrect: "off", spellCheck: false } }}
            fullWidth
          />
          <PoleHasla
            label="Hasło"
            value={haslo}
            onChange={(e) => ustawHaslo(e.target.value)}
            disabled={trwa}
            autoComplete="current-password"
            fullWidth
          />
          {bladLogowania && <Typography color="error">{bladLogowania}</Typography>}
          <Button type="submit" variant="contained" size="large" disabled={trwa} fullWidth>
            {trwa ? <CircularProgress size={22} color="inherit" /> : "Zaloguj"}
          </Button>
        </Stack>
      </Box>
    </Container>
  );
}
