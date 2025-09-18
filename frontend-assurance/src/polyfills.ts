import 'zone.js';

// Polyfill pour libs qui attendent la variable Node `global` dans le navigateur
(window as any).global = window;
(window as any).process = (window as any).process || { env: {} as any };

// Si un jour tu as une erreur similaire pour `process`, tu peux ajouter :
// (window as any).process = { env: {} as any };
