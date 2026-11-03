// French labels and colours of the codes used by the API.

export const ENTRY_TYPES = {
  EVENT: 'Événement',
  SHOPPING: 'Courses',
  MEAL: 'Repas',
  ACCOUNTING: 'Budget',
}

export const EVENT_CATEGORIES = {
  VACATION: 'Vacances',
  DAY_CAMP: 'Centre aéré',
  TAXES: 'Impôts',
  SPORT: 'Sport',
  HEALTH: 'Santé',
  SCHOOL: 'École',
  LEISURE: 'Loisirs',
  BIRTHDAY: 'Anniversaire',
  HOUSEHOLD: 'Maison',
  UTILITIES: 'Factures',
  OTHER: 'Autre',
}

export const ACCOUNTING_ACTIONS = {
  REPORT: 'Faire le bilan du mois',
  APPROVAL: 'Approuver les opérations',
}

export const FREQUENCIES = {
  DAILY: 'Tous les jours',
  WEEKLY: 'Toutes les semaines',
  MONTHLY: 'Tous les mois',
  YEARLY: 'Tous les ans',
}

export const WEEKDAYS = [
  ['MONDAY', 'Lun'], ['TUESDAY', 'Mar'], ['WEDNESDAY', 'Mer'], ['THURSDAY', 'Jeu'],
  ['FRIDAY', 'Ven'], ['SATURDAY', 'Sam'], ['SUNDAY', 'Dim'],
]

export const STORE_TYPES = {
  SUPERMARKET: 'Supermarché',
  MARKET: 'Marché',
  BAKERY: 'Boulangerie',
  BUTCHER: 'Boucherie',
  PHARMACY: 'Pharmacie',
  DIY: 'Bricolage',
  GARDEN: 'Jardinerie',
  ONLINE: 'En ligne',
  OTHER: 'Autre',
}

export const AISLES = {
  FRUITS_VEGETABLES: 'Fruits et légumes',
  BAKERY: 'Boulangerie',
  MEAT_FISH: 'Viandes et poissons',
  DAIRY: 'Crèmerie',
  GROCERY: 'Épicerie',
  FROZEN: 'Surgelés',
  DRINKS: 'Boissons',
  HYGIENE: 'Hygiène',
  HOUSEHOLD: 'Entretien',
  BABY: 'Bébé',
  PETS: 'Animaux',
  HEALTH: 'Santé',
  TOOLS: 'Outillage',
  HARDWARE: 'Quincaillerie',
  PAINT: 'Peinture',
  ELECTRICITY: 'Électricité',
  PLUMBING: 'Plomberie',
  PLANTS: 'Plantes et graines',
  GARDEN_SUPPLIES: 'Terreau et engrais',
  OTHER: 'Autre',
}

export const ITEM_KINDS = {
  GOODS: 'Article',
  INGREDIENT: 'Ingrédient',
}

export const UNIT_LABELS = {
  G: { singular: 'g', plural: 'g' },
  KG: { singular: 'kg', plural: 'kg' },
  ML: { singular: 'ml', plural: 'ml' },
  CL: { singular: 'cl', plural: 'cl' },
  L: { singular: 'l', plural: 'l' },
  TSP: { singular: 'c. à café', plural: 'c. à café' },
  TBSP: { singular: 'c. à soupe', plural: 'c. à soupe' },
  PIECE: { singular: 'pièce', plural: 'pièces' },
  PACK: { singular: 'paquet', plural: 'paquets' },
  BUNCH: { singular: 'botte', plural: 'bottes' },
  PINCH: { singular: 'pincée', plural: 'pincées' },
}

export const UNITS = Object.fromEntries(Object.entries(UNIT_LABELS).map(([code, l]) => [code, l.singular]))

export const PRIORITIES = { LOW: 'Basse', NORMAL: 'Normale', HIGH: 'Haute' }

export const REPEATS = { DAILY: 'Chaque jour', WEEKLY: 'Chaque semaine', MONTHLY: 'Chaque mois' }

export const TRANSACTION_STATUSES = { PLANNED: 'Prévue', PENDING: 'À approuver', APPROVED: 'Approuvée' }

export const TRANSACTION_STATUS_STYLES = {
  PLANNED: 'bg-sky-50 text-sky-800',
  PENDING: 'bg-amber-50 text-amber-800',
  APPROVED: 'bg-emerald-50 text-emerald-800',
}

export const SOURCE_TYPES = { MANUAL: 'Saisie', PLANNING: 'Planning', SHOPPING: 'Courses', RECURRING: 'Échéance' }

export const BUDGET_GROUPS = { NEEDS: 'Besoins', WANTS: 'Envies', SAVINGS: 'Épargne' }

export const PERIODS = {
  MONTHLY: 'Mensuel',
  BIMONTHLY: 'Bimestriel',
  QUARTERLY: 'Trimestriel',
  SEMIANNUAL: 'Semestriel',
  YEARLY: 'Annuel',
}

export const ACCOUNT_KINDS = { CHECKING: 'Compte courant', SAVINGS: 'Épargne', CASH: 'Espèces', CARD: 'Carte' }

export const ROLES = { PARENT: 'Parent', CHILD: 'Enfant' }

/** Chip colours of planning occurrences: by category for events, by type otherwise. */
const CATEGORY_COLORS = {
  VACATION: 'bg-teal-100 text-teal-900 border-teal-400',
  DAY_CAMP: 'bg-orange-100 text-orange-900 border-orange-400',
  TAXES: 'bg-rose-100 text-rose-900 border-rose-400',
  SPORT: 'bg-lime-100 text-lime-900 border-lime-500',
  HEALTH: 'bg-red-100 text-red-900 border-red-400',
  SCHOOL: 'bg-violet-100 text-violet-900 border-violet-400',
  LEISURE: 'bg-pink-100 text-pink-900 border-pink-400',
  BIRTHDAY: 'bg-fuchsia-100 text-fuchsia-900 border-fuchsia-400',
  HOUSEHOLD: 'bg-stone-100 text-stone-900 border-stone-400',
  UTILITIES: 'bg-cyan-100 text-cyan-900 border-cyan-400',
  OTHER: 'bg-indigo-100 text-indigo-900 border-indigo-400',
}

const TYPE_COLORS = {
  SHOPPING: 'bg-emerald-100 text-emerald-900 border-emerald-500',
  MEAL: 'bg-amber-100 text-amber-900 border-amber-500',
  ACCOUNTING: 'bg-sky-100 text-sky-900 border-sky-500',
}

export function entryColor(entry) {
  if (entry.type === 'EVENT') return CATEGORY_COLORS[entry.category] ?? CATEGORY_COLORS.OTHER
  return TYPE_COLORS[entry.type]
}

export const MEMBER_COLORS = ['#2563eb', '#db2777', '#16a34a', '#ea580c', '#7c3aed', '#0891b2', '#ca8a04', '#475569']
