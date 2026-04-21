# Sistem Chelneri-Bucatari

Simularea modeleaza interactiunea dintre doua echipe:
- chelneri (preiau si servesc comenzi)
- bucatari (pregatesc comenzi)

## Caracteristici
- fiecare lucrator are identificator unic de forma `Chelner<nr><token>` / `Bucatar<nr><token>`
- meniuri de la 1 la 5
- coada comuna pentru bucatarie
- cozi de livrare separate pentru fiecare chelner
- timp de preparare diferit pe meniu + factor de viteza individual pe bucatar
- servire cu intarziere mica, in functie de meniu

## Rulare

```bash
python3 restaurant_simulation.py
```

Scriptul afiseaza in consola fluxul comenzilor si un sumar final pe fiecare chelner/bucatar.
