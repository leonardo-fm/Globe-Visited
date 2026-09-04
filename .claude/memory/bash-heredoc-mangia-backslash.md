---
name: bash-heredoc-mangia-backslash
description: "In questa sessione gli heredoc del tool Bash corrompono i backslash, quindi vanno evitati per scrivere codice"
metadata: 
  node_type: memory
  type: project
  originSessionId: 41cb3d5f-b93c-4509-833e-f6ccfe5d5cb4
  modified: 2026-09-04T19:08:36.394Z
---

Il tool Bash su questa macchina rompe gli heredoc: un `cat > file <<'EOF'` con
dentro un file HTML e' fallito con "unexpected EOF while looking for matching
quote", e piu' volte un `python - <<'PY'` contenente `\\u0300` o `[^"\\]` ha
ricevuto backslash gia' mangiati, producendo regex corrotte o errori di parsing.

**Perche' conta:** ci ho perso tre tentativi su una singola riga di regex, e il
file scritto sembrava a posto finche' non lo si rileggeva con `grep`.

**Come applicarlo:** per creare o modificare file usare lo strumento Write o
Edit, non gli heredoc. Se serve uno script Python, scriverlo prima su file (in
scratchpad) con Write e poi lanciarlo con `python percorso.py`. Se un backslash
deve finire dentro un file, verificarlo dopo con `grep`. In alternativa
costruire i caratteri senza escape, es. `String.fromCharCode(768)` al posto di
`̀`.
