# Mail Client / Mail Server

Progetto di laboratorio di Programmazione III: un mail server e un mail client
in JavaFX (FXML, pattern MVC) che comunicano tramite socket Java con messaggi testuali.

## Struttura

| Cartella       | Contenuto                                              |
|----------------|--------------------------------------------------------|
| `mail-server/` | Server: gestisce le caselle di posta e mostra il log   |
| `mail-client/` | Client: inbox, invio, reply, reply-all, forward        |

Ogni cartella è un progetto Maven indipendente, da aprire separatamente in IntelliJ IDEA.

## Protocollo di comunicazione

Ogni operazione apre una nuova connessione TCP (come HTTP): il client invia
**una riga** di testo JSON con la richiesta, il server risponde con **una riga**
JSON e chiude la connessione.

| Comando  | Campi della richiesta | Risposta OK               |
|----------|-----------------------|---------------------------|
| `LOGIN`  | `user`                | conferma che l'account esiste |
| `FETCH`  | `user`, `lastId`      | `emails`: solo i messaggi con ID > `lastId` |
| `SEND`   | `user`, `email`       | conferma dell'invio       |
| `DELETE` | `user`, `emailId`     | conferma della cancellazione |

In caso di errore la risposta è `{"status":"ERROR","message":"..."}`.

Esempio:

```
→ {"command":"FETCH","user":"alice@mail.it","lastId":3}
← {"status":"OK","emails":[{"id":4,"sender":"bob@mail.it", ...}]}
```

## Requisiti

- JDK 21
- Maven (incluso in IntelliJ IDEA)

## Avvio

Avviare prima il server, poi uno o più client:

```bash
cd mail-server && mvn javafx:run
cd mail-client && mvn javafx:run
```
