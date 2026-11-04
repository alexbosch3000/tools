// Tabs of the budget page, also used as route parameters (/budget/$tab).
export const ACCOUNTING_TABS = {
  bilan: 'Bilan',
  operations: 'Opérations',
  approbations: 'À approuver',
  enveloppes: 'Enveloppes',
  echeances: 'Échéances',
  comptes: 'Comptes',
} as const

export type AccountingTab = keyof typeof ACCOUNTING_TABS

/** Cache areas a budget change may affect. */
export const ACCOUNTING_AREAS = ['accounting', 'dashboard'] as const
