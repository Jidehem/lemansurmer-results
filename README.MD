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
1. Pour chaque onglet, définir les zones d'impression
  1. Définir les premières lignes comme "à répéter sur chaque page" dans la mise en page
  1. Vérifier le format de page
  1. Vérifier les sauts de pages et les adapter manuellement
  
Ne pas oublier de tester avec Excel !
(OpenOffice se comporte différemment dans certains cas, notamment pour les cellules fusionnées)