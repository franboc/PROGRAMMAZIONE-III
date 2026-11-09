# Mail Client / Mail Server

Progetto di laboratorio di Programmazione III: un mail server e un mail client
in JavaFX (FXML, pattern MVC) che comunicano tramite socket Java con messaggi testuali.

## Struttura

| Cartella       | Contenuto                                              |
|----------------|--------------------------------------------------------|
| `mail-server/` | Server: gestisce le caselle di posta e mostra il log   |
| `mail-client/` | Client: inbox, invio, reply, reply-all, forward        |

Ogni cartella è un progetto Maven indipendente, da aprire separatamente in IntelliJ IDEA.

## Requisiti

- JDK 21
- Maven (incluso in IntelliJ IDEA)

## Avvio

Avviare prima il server, poi uno o più client:

```bash
cd mail-server && mvn javafx:run
cd mail-client && mvn javafx:run
```
