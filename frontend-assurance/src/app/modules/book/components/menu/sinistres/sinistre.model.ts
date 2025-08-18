// ✅ MODÈLE SINISTRE CORRIGÉ ET COMPLET
// Synchronisé avec le modèle Java Sinistre.java

export interface Sinistre {
  // ✅ Champs principaux
  numSinistre: string;
  anneeExercice?: number | null;
  numContrat?: string | null;
  previsionDeRecoursDomVeh?: string | null;               // Ajouté (correspond à PREVISION_DE_RECOURS_Dom_veh)
  provisionDeRecoursDefenseEtRecours?: string | null;     // Ajouté (correspond à PROVISION_DE_RECOURS_DEFENSE_ET_RECOURS)
  typeUsage?: string | null;
  // ✅ Dates - formatées en ISO 8601 par le backend
  effetContrat?: string | null;
  dateExpiration?: string | null;
  prochainTerme?: string | null;
  dateDeclaration?: string | null;
  dateOuverture?: string | null;
  dateSurvenance?: string | null;

  // ✅ Informations générales
  usage?: string | null;
  codeIntermediaire?: number | null;
  natureSinistre?: string | null;
  lieuAccident?: string | null;
  gouvernorat?: string | null;
  typeSinistre?: string | null;
  compagnieAdverse?: string | null;
  codeResponsabilite?: number | null;

  // ✅ États et statuts
  libEtatSinistre?: string | null;
  etatSinAnnee?: string | null;

  // ✅ Montants principaux
  montantEvaluation?: number | null;
  totalReglement?: number | null;

  // ✅ Règlements détaillés (ajoutés depuis Java)
  reglementRc?: number | null;
  reglementDefenseEtRecours?: number | null;
  reglementIncendie?: number | null;
  reglementVol?: number | null;
  reglementDommageVehicule?: number | null;
  reglementDc?: number | null;
  reglementBg?: number | null;
  reglementPta?: number | null;
  reglementCarGlass?: number | null;
  reglementAssistance?: number | null;
  reglementIndividuelleAccident?: number | null;
  reglementCatastropheNaturelle?: number | null;
  reglementVolRadioCassette?: number | null;
  reglementEmeuteMouvementPopulaire?: number | null;
  reglementAuxiliaire?: number | null;
  reglementFraisExecution?: number | null;
  recettesActesJudiciairesRaj?: number | null;

  // ✅ SAP (Sinistres À Payer) - corrigés en number
  totalSapFinal?: number | null;
  sapRc?: number | null;
  sapDefenseEtRecours?: number | null;
  sapIncendie?: number | null;
  sapVol?: number | null;
  sapDommageVehicule?: number | null;
  sapDc?: number | null;
  sapBg?: number | null;
  sapPta?: number | null;
  sapCarglass?: number | null;
  sapAssistance?: number | null;
  sapIndividuelleAccident?: number | null;
  sapCatastropheNaturelle?: number | null;
  sapVolRadioCassette?: number | null;
  sapEmeuteMouvementPopulaire?: number | null;

  // ✅ Cumuls et provisions - corrigés en number
  cumulReglement?: number | null;
  provisionDeRecours?: number | null;
  previsionDeRecours?: number | null;
  cumulPrevisionDeRecours?: number | null;
  montantRecupere?: number | null;
  cumulMontantRecupere?: number | null;
  montantRecupereAnnule?: number | null;
  cumulMontantRecupereAnnule?: number | null;

  // ✅ Informations supplémentaires
  nombreBlesses?: number | null;
  nombreDeces?: number | null;

  // ✅ Champs calculés (ajoutés pour l'affichage frontend)
  // Ces champs seront calculés côté frontend
  montantEvaluationFormate?: string;
  totalReglementFormate?: string;
  etatAvecCouleur?: string;
  natureAvecIcone?: string;
  typeAvecIcone?: string;
  ageSinistreEnJours?: number;
  priorite?: string;
}

// ✅ CRITÈRES DE RECHERCHE AMÉLIORÉS
export interface SinistreSearchCriteria {
  // ✅ Critères simplifiés pour l'interface utilisateur
  searchText?: string | null;
  etat?: string | null;
  nature?: string | null;
  annee?: string | null;
  gouvernorat?: string | null;
  typeSinistre?: string | null;
  usage?: string | null;

  // ✅ Critères détaillés
  numContrat?: string | null;
  anneeExercice?: number | null;
  natureSinistre?: string | null;
  libEtatSinistre?: string | null;
  codeIntermediaire?: number | null;
  lieuAccident?: string | null;
  compagnieAdverse?: string | null;
  codeResponsabilite?: number | null;

  // ✅ Plages de dates
  dateDeclarationDebut?: string | null;
  dateDeclarationFin?: string | null;
  dateSurvenanceDebut?: string | null;
  dateSurvenanceFin?: string | null;
  dateOuvertureDebut?: string | null;
  dateOuvertureFin?: string | null;

  // ✅ Plages de montants
  montantEvaluationMin?: number | null;
  montantEvaluationMax?: number | null;
  totalReglementMin?: number | null;
  totalReglementMax?: number | null;

  // ✅ Critères numériques
  nombreBlessesMin?: number | null;
  nombreBlessesMax?: number | null;
  nombreDecesMin?: number | null;
  nombreDecesMax?: number | null;

  // ✅ Pagination et tri
  page?: number | null;
  size?: number | null;
  sortBy?: string | null;
  sortDirection?: string | null;
}

// ✅ RÉPONSE PAGINÉE COMPLÈTE
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;

  // ✅ Propriétés personnalisées pour compatibilité
  currentPage: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// ✅ STATISTIQUES DÉTAILLÉES
export interface SinistreStatistiques {
  // ✅ Totaux généraux
  totalSinistres: number;
  pageActuelle: number;
  elementsParPage: number;
  pourcentageVisible: number;

  // ✅ Répartition par nature
  corporel: number;
  materiel: number;
  mixte: number;

  // ✅ Répartition par état
  ouverture: number;
  miseAJour: number;
  reprise: number;
  reouverture: number;

  // ✅ Montants totaux
  montantTotalEvaluation: number;
  montantTotalReglement: number;

  // ✅ Moyennes
  montantMoyenEvaluation?: number;
  montantMoyenReglement?: number;

  // ✅ Répartition par gouvernorat
  repartitionGouvernorat?: { [key: string]: number };

  // ✅ Répartition par année
  repartitionAnnee?: { [key: string]: number };
}

// ✅ OPTIONS DE FILTRES DYNAMIQUES
export interface FilterOptions {
  natures: string[];
  types: string[];
  etats: string[];
  gouvernorats: string[];
  annees: number[];
  usages: string[];
  compagniesAdverses: string[];
  codesIntermediaires: number[];
}

// ✅ CONFIGURATION DES COLONNES
export interface ColonnesVisibles {
  numSinistre: boolean;
  anneeExercice: boolean;
  numContrat: boolean;
  dateDeclaration: boolean;
  natureSinistre: boolean;
  typeSinistre: boolean;
  libEtatSinistre: boolean;
  montantEvaluation: boolean;
  totalReglement: boolean;
  lieuAccident: boolean;
  gouvernorat: boolean;
  compagnieAdverse: boolean;
  usage: boolean;
  priorite: boolean;
  nombreBlesses: boolean;
  nombreDeces: boolean;
}

// ✅ OPTIONS DE TRI
export interface SortOption {
  value: string;
  label: string;
}

// ✅ RÉPONSES API STANDARDISÉES
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  status: number;
}

export interface ErrorResponse {
  error: boolean;
  message: string;
  details?: string;
  timestamp: string;
  status: number;
}

// ✅ TYPES UTILITAIRES
export type SortDirection = 'asc' | 'desc';
export type SinistreStatus = 'OUVERTURE' | 'MISE A JOUR' | 'REPRISE' | 'REOUVERTURE' | 'CLOTURE';
export type SinistreNature = 'CORPOREL' | 'MATERIEL' | 'MIXTE';
export type SinistreType = 'DEFENSE JUDICIAIRE' | 'RECOURS HORS IDA' | 'DEFENSE HORS IDA' | 'COLLISION' | 'VOL' | 'INCENDIE' | 'BRIS DE GLACE';
export type PrioriteLevel = 'HAUTE' | 'MOYENNE' | 'NORMALE';

// ✅ CONSTANTES POUR L'INTERFACE
export const NATURES_SINISTRE: SinistreNature[] = ['CORPOREL', 'MATERIEL', 'MIXTE'];
export const ETATS_SINISTRE: SinistreStatus[] = ['OUVERTURE', 'MISE A JOUR', 'REPRISE', 'REOUVERTURE', 'CLOTURE'];
export const TYPES_SINISTRE: SinistreType[] = ['DEFENSE JUDICIAIRE', 'RECOURS HORS IDA', 'DEFENSE HORS IDA', 'COLLISION', 'VOL', 'INCENDIE', 'BRIS DE GLACE'];
export const GOUVERNORATS_TUNISIE: string[] = ['TUNIS', 'ARIANA', 'SFAX', 'SOUSSE', 'MONASTIR', 'NABEUL', 'BIZERTE', 'KAIROUAN', 'KASSERINE', 'SIDI BOUZID', 'GABES', 'MEDENINE', 'TATAOUINE', 'GAFSA', 'TOZEUR', 'KEBILI', 'JENDOUBA', 'KEF', 'SILIANA', 'BEJA', 'ZAGHOUAN', 'MANOUBA', 'BEN AROUS', 'MAHDIA'];

// ✅ CONFIGURATION PAR DÉFAUT
export const DEFAULT_PAGE_SIZE = 20;
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
export const DEFAULT_SORT_BY = 'dateDeclaration';
export const DEFAULT_SORT_DIRECTION: SortDirection = 'desc';

// ✅ COLONNES VISIBLES PAR DÉFAUT
export const DEFAULT_COLONNES_VISIBLES: ColonnesVisibles = {
  numSinistre: true,
  anneeExercice: true,
  numContrat: true,
  dateDeclaration: true,
  natureSinistre: true,
  typeSinistre: true,
  libEtatSinistre: true,
  montantEvaluation: true,
  totalReglement: true,
  lieuAccident: false,
  gouvernorat: true,
  compagnieAdverse: false,
  usage: false,
  priorite: true,
  nombreBlesses: false,
  nombreDeces: false
};

// ✅ OPTIONS DE TRI DISPONIBLES
export const SORT_OPTIONS: SortOption[] = [
  { value: 'numSinistre', label: 'N° Sinistre' },
  { value: 'dateDeclaration', label: 'Date Déclaration' },
  { value: 'dateSurvenance', label: 'Date Survenance' },
  { value: 'anneeExercice', label: 'Année Exercice' },
  { value: 'montantEvaluation', label: 'Montant Évaluation' },
  { value: 'totalReglement', label: 'Total Règlement' },
  { value: 'natureSinistre', label: 'Nature Sinistre' },
  { value: 'libEtatSinistre', label: 'État Sinistre' },
  { value: 'gouvernorat', label: 'Gouvernorat' }
];
