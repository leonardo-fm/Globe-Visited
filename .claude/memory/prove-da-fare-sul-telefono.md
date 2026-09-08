---
name: prove-da-fare-sul-telefono
description: "Lista delle prove che l'utente fa sull'S10e la notte fra l'8 e il 9 settembre 2026, con il sintomo da guardare per ognuna"
metadata:
  node_type: memory
  type: project
  modified: 2026-09-08T00:30:00.000Z
---

Deciso il 2026-09-08 alle ~00:30: l'utente collauda tutto sull'S10e **la notte
fra l'8 e il 9 settembre 2026**. Fino ad allora niente e' confermato, e il
working tree resta da committare.

Sette cose, con quello che va guardato - non basta "funziona":

1. **La ricerca si azzera a ogni tocco.** Scrivi una citta', tocca il globo,
   gira, torna sulla barra: deve ripartire vuota. E' il caso che prima non
   funzionava, perche' il fuoco non arriva mai da una WebView.
2. **Toccare il campo a meta' parola.** Effetto collaterale accettato: azzera
   anche mentre stai scrivendo. Se da' fastidio si restringe al primo tocco
   dopo essere stati altrove.
3. **Lo spazio fra nome della citta' e paese** nella lista dei risultati.
4. **I pin sono sfere**, non cilindri, e il raggio segue lo zoom **senza
   scatti** (prima era quantizzato a passi di 1,6).
5. **I confini sul bordo del globo.** Lo stacco a filo dell'orizzonte deve
   essersi ridotto di circa la meta'. **Il sintomo di essere andati troppo giu'
   con `SHELL_GAP` e' il ritorno dello sfarfallio a globo PICCOLO**: va guardato
   li', non al centro. Se torna, si risale.
6. **Il backup, giro completo:** esporta -> disinstalla -> reinstalla ->
   importa. E' l'unica prova che dice se serve a qualcosa. Guardare anche che la
   conferma dica i numeri giusti ("N paesi e M luoghi").
7. **Roba rimasta indietro da prima**, mai provata sul telefono: il tempo di
   avvio reale col dataset a risoluzione alta, se il **wireframe della
   schermata di caricamento continua a girare** mentre il thread e' bloccato, e
   la fase 3 dei luoghi (pressione lunga sul globo, creazione da coordinate
   incollate), scritta su sua richiesta senza provarla.

I flussi 1, 3, 6 e la fase 3 si possono provare prima nel browser con
[[banco-di-prova-web]], ma **1 e 3 nascono da Compose e li' non compaiono**:
il browser li' non dimostra niente.

Vedi [[been-there-stato-lavoro]] e [[ambiente-senza-toolchain-android]].
