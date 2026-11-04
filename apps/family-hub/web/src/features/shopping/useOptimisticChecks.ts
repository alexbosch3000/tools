// Ticks shown as soon as they are clicked, while the server records them: in a shop, the network can be slow.
import { useState } from 'react'

/**
 * Keeps the value of each box being saved in React state, which re-renders before the browser event ends:
 * a value put in the query cache would arrive a moment later and the box would flicker back meanwhile.
 * Once the save settles, the box shows the server's value again (unchanged when the save failed).
 */
export function useOptimisticChecks() {
  const [saving, setSaving] = useState<Record<string, boolean>>({})

  const isChecked = (key: string, serverValue: boolean): boolean => saving[key] ?? serverValue

  const toggle = (key: string, serverValue: boolean, save: (checked: boolean) => Promise<unknown>): void => {
    const checked = !isChecked(key, serverValue)
    setSaving((current) => ({ ...current, [key]: checked }))
    // A later click on the same box owns the value from then on.
    const settled = () => setSaving((current) => {
      if (current[key] !== checked) return current
      const { [key]: _saved, ...others } = current
      return others
    })
    save(checked).then(settled, settled)
  }

  return { isChecked, toggle }
}
