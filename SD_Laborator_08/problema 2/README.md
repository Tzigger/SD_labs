# SD Laborator 08 - Problema 2 (Tema 1)

Implementarea extinde aplicația exemplu astfel încât:
- și studentul poate pune întrebări;
- întrebările pot merge către profesor, către toți studenții sau către un student anume;
- răspunsurile sunt `PRIVATE` pentru comunicare `one-to-one`;
- răspunsurile sunt `PUBLIC` pentru comunicare `one-to-many`.

## Start rapid cu Docker

Din folderul `problema 2`:

```bash
docker compose up --build -d
docker compose ps
```

Sau, pentru prezentare, rulează scriptul:

```bash
./start_lab.sh
```

Opțional:

```bash
./start_lab.sh --no-build --client
./start_lab.sh --logs
```

`--client` pornește clientul web.

Servicii expuse:
- `message_manager`: `localhost:1500`
- `teacher_microservice`: `localhost:1600`
- `student_microservice_1`: `localhost:1701`
- `student_microservice_2`: `localhost:1702`
- `student_microservice_3`: `localhost:1703`

Stop:

```bash
docker compose down
```

## Interfață GUI

Varianta recomandata (web, Node):

```bash
./start_web_client.sh
```

Pornire web + laborator intr-un singur pas:

```bash
./start_web_client.sh --with-lab
```

Cu rebuild Docker pentru demo complet:

```bash
./start_web_client.sh --with-lab --build-lab
```

Clientul web este disponibil pe:
- `http://127.0.0.1:3000` (implicit)

Clientul Python a ramas ca fallback:

```bash
python3 TeacherStudentGUI/src/TeacherStudentGUI.py
```

Notă: dacă runtime-ul `tkinter` de pe macOS dă crash, scriptul trece automat în mod CLI interactiv.

Din GUI:
- selectezi cine întreabă (`teacher`, `student1`, `student2`, `student3`);
- selectezi destinația (`Teacher`, `Toti studentii`, `Student 1/2/3`);
- scrii întrebarea și apeși `Intreaba`.

## Protocol client -> microserviciu

Format extins:

```text
ASK|<TARGET_TYPE>|<TARGET_VALUE>|<QUESTION_TEXT>
```

Unde:
- `TARGET_TYPE`: `TEACHER`, `ALL_STUDENTS`, `STUDENT`
- `TARGET_VALUE`: `teacher` / `-` / `studentX`

Compatibilitate:
- dacă trimiți doar text simplu (fără prefix `ASK|`), este tratat ca întrebare către `ALL_STUDENTS`.
