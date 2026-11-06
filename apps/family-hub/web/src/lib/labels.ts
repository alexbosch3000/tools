// French labels and colours of the codes used by the API, typed against the OpenAPI contract.
import type { Schemas } from '../api/client.ts'

type Labels<K extends string> = Record<K, string>

export const ENTRY_TYPES: Labels<Schemas['EntryType']> = {
  EVENT: 'Événement', SHOPPING: 'Courses', MEAL: 'Repas', ACCOUNTING: 'Budget',
}

export const EVENT_CATEGORIES: Labels<Schemas['EventCategory']> = {
  VACATION: 'Vacances', DAY_CAMP: 'Centre aéré', TAXES: 'Impôts', SPORT: 'Sport', HEALTH: 'Santé',
  SCHOOL: 'École', LEISURE: 'Loisirs', BIRTHDAY: 'Anniversaire', HOUSEHOLD: 'Maison', UTILITIES: 'Factures',
  OTHER: 'Autre',
}

export const ACCOUNTING_ACTIONS: Labels<Schemas['AccountingAction']> = {
  REPORT: 'Faire le bilan du mois', APPROVAL: 'Approuver les opérations',
}

export const FREQUENCIES: Labels<Schemas['Frequency']> = {
  DAILY: 'Tous les jours', WEEKLY: 'Toutes les semaines', MONTHLY: 'Tous les mois', YEARLY: 'Tous les ans',
}

export const WEEKDAYS: [Schemas['DayOfWeek'], string][] = [
  ['MONDAY', 'Lun'], ['TUESDAY', 'Mar'], ['WEDNESDAY', 'Mer'], ['THURSDAY', 'Jeu'],
  ['FRIDAY', 'Ven'], ['SATURDAY', 'Sam'], ['SUNDAY', 'Dim'],
]

export const STORE_TYPES: Labels<Schemas['StoreType']> = {
  SUPERMARKET: 'Supermarché', MARKET: 'Marché', BAKERY: 'Boulangerie', BUTCHER: 'Boucherie',
  PHARMACY: 'Pharmacie', CLOTHING: 'Vêtements', DIY: 'Bricolage', GARDEN: 'Jardinerie', ONLINE: 'En ligne',
  OTHER: 'Autre',
}

export const AISLES: Labels<Schemas['Aisle']> = {
  FRUITS_VEGETABLES: 'Fruits et légumes', BAKERY: 'Boulangerie', MEAT_FISH: 'Viandes et poissons',
  DAIRY: 'Crèmerie', GROCERY: 'Épicerie', FROZEN: 'Surgelés', DRINKS: 'Boissons', HYGIENE: 'Hygiène',
  HOUSEHOLD: 'Entretien', BABY: 'Bébé', PETS: 'Animaux', HEALTH: 'Santé', CLOTHES_ADULTS: 'Vêtements adultes',
  CLOTHES_CHILDREN: 'Vêtements enfants', CLOTHES_BABY: 'Vêtements bébé', UNDERWEAR: 'Sous-vêtements et chaussettes',
  SHOES: 'Chaussures', SPORTSWEAR: 'Sport et bain', ACCESSORIES: 'Accessoires', TOOLS: 'Outillage',
  HARDWARE: 'Quincaillerie', PAINT: 'Peinture', ELECTRICITY: 'Électricité', PLUMBING: 'Plomberie',
  PLANTS: 'Plantes et graines', GARDEN_SUPPLIES: 'Terreau et engrais', OTHER: 'Autre',
}

export const ITEM_KINDS: Labels<Schemas['ItemKind']> = { GOODS: 'Article', INGREDIENT: 'Ingrédient' }

export const UNIT_LABELS: Record<Schemas['Unit'], { singular: string, plural: string }> = {
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

export const UNITS = Object.fromEntries(
  Object.entries(UNIT_LABELS).map(([code, label]) => [code, label.singular])) as Labels<Schemas['Unit']>

export const PRIORITIES: Labels<Schemas['Priority']> = { LOW: 'Basse', NORMAL: 'Normale', HIGH: 'Haute' }

export const REPEATS: Labels<Schemas['Repeat']> = {
  DAILY: 'Chaque jour', WEEKLY: 'Chaque semaine', MONTHLY: 'Chaque mois',
}

export const TRANSACTION_STATUSES: Labels<Schemas['TransactionStatus']> = {
  PLANNED: 'Prévue', PENDING: 'À approuver', APPROVED: 'Approuvée',
}

export const SOURCE_TYPES: Labels<Schemas['SourceType']> = {
  MANUAL: 'Saisie', PLANNING: 'Planning', SHOPPING: 'Courses', RECURRING: 'Échéance',
}

export const BUDGET_GROUPS: Labels<Schemas['BudgetGroup']> = { NEEDS: 'Besoins', WANTS: 'Envies', SAVINGS: 'Épargne' }

export const PERIODS: Labels<Schemas['Period']> = {
  MONTHLY: 'Mensuel', BIMONTHLY: 'Bimestriel', QUARTERLY: 'Trimestriel', SEMIANNUAL: 'Semestriel', YEARLY: 'Annuel',
}

export const ACCOUNT_KINDS: Labels<Schemas['AccountKind']> = {
  CHECKING: 'Compte courant', SAVINGS: 'Épargne', CASH: 'Espèces', CARD: 'Carte',
}

export const ROLES: Labels<Schemas['Role']> = { PARENT: 'Parent', CHILD: 'Enfant' }

export const TRANSACTION_STATUS_STYLES: Labels<Schemas['TransactionStatus']> = {
  PLANNED: 'bg-sky-100 text-sky-900 dark:bg-sky-950 dark:text-sky-200',
  PENDING: 'bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-200',
  APPROVED: 'bg-emerald-100 text-emerald-900 dark:bg-emerald-950 dark:text-emerald-200',
}

/** Chip colours of planning occurrences: by category for events, by type otherwise. */
const CATEGORY_COLORS: Labels<Schemas['EventCategory']> = {
  VACATION: 'bg-teal-100 text-teal-950 border-teal-500 dark:bg-teal-950 dark:text-teal-100',
  DAY_CAMP: 'bg-orange-100 text-orange-950 border-orange-500 dark:bg-orange-950 dark:text-orange-100',
  TAXES: 'bg-rose-100 text-rose-950 border-rose-500 dark:bg-rose-950 dark:text-rose-100',
  SPORT: 'bg-lime-100 text-lime-950 border-lime-600 dark:bg-lime-950 dark:text-lime-100',
  HEALTH: 'bg-red-100 text-red-950 border-red-500 dark:bg-red-950 dark:text-red-100',
  SCHOOL: 'bg-violet-100 text-violet-950 border-violet-500 dark:bg-violet-950 dark:text-violet-100',
  LEISURE: 'bg-pink-100 text-pink-950 border-pink-500 dark:bg-pink-950 dark:text-pink-100',
  BIRTHDAY: 'bg-fuchsia-100 text-fuchsia-950 border-fuchsia-500 dark:bg-fuchsia-950 dark:text-fuchsia-100',
  HOUSEHOLD: 'bg-stone-200 text-stone-950 border-stone-500 dark:bg-stone-800 dark:text-stone-100',
  UTILITIES: 'bg-cyan-100 text-cyan-950 border-cyan-500 dark:bg-cyan-950 dark:text-cyan-100',
  OTHER: 'bg-indigo-100 text-indigo-950 border-indigo-500 dark:bg-indigo-950 dark:text-indigo-100',
}

const TYPE_COLORS: Record<Exclude<Schemas['EntryType'], 'EVENT'>, string> = {
  SHOPPING: 'bg-emerald-100 text-emerald-950 border-emerald-600 dark:bg-emerald-950 dark:text-emerald-100',
  MEAL: 'bg-amber-100 text-amber-950 border-amber-500 dark:bg-amber-950 dark:text-amber-100',
  ACCOUNTING: 'bg-sky-100 text-sky-950 border-sky-500 dark:bg-sky-950 dark:text-sky-100',
}

export function entryColor(entry: Pick<Schemas['PlanningEntry'], 'type' | 'category'>): string {
  if (entry.type === 'EVENT') return CATEGORY_COLORS[entry.category ?? 'OTHER']
  return TYPE_COLORS[entry.type]
}

/**
 * Chips of to-dos in the planning: a surface background with a coloured left border, so they differ from the
 * filled chips of planning entries; the priority shows in the border and the weight.
 */
const TASK_COLORS: Labels<Schemas['Priority']> = {
  HIGH: 'bg-surface text-ink border border-line border-l-4 border-l-red-600 font-bold',
  NORMAL: 'bg-surface text-ink border border-line border-l-4 border-l-slate-500',
  LOW: 'bg-surface text-ink-3 border border-line border-l-4 border-l-slate-300',
}

export function taskColor(priority: Schemas['Priority']): string {
  return TASK_COLORS[priority]
}

export const MEMBER_COLORS = ['#2563eb', '#db2777', '#16a34a', '#ea580c', '#7c3aed', '#0891b2', '#ca8a04', '#475569']
