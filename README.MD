# Exécution

Dans Eclipse: 

Exemple de run configuration:
- `<path-to-result-file> <print-mode>`
  with print-mode in `SCREEN, TSV, XLS, XSLX`
  
Arguments:
- `src/main/data/r12944.csv XLSX`


# Impression

1. Vérifier les résultats
  - Les adapter manuellemant si nécessaire
1. Pour chaque onglet, les zones d'impression sont déjà définies
  - Vérifier les sauts de pages et les adapter manuellement (il n'est pas possible de calculer correctement la hauteur des lignes. Il s'agit donc d'une estimation.
  
Ne pas oublier de tester avec Excel !
(OpenOffice se comporte différemment dans certains cas, notamment pour les cellules fusionnées)