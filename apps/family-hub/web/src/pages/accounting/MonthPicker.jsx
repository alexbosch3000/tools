// Previous / next month selector.
import { addMonths } from '../../lib/dates.js'
import { formatMonth } from '../../lib/format.js'
import { Button } from '../../components/ui.jsx'

export default function MonthPicker({ month, onChange }) {
  const shift = (n) => onChange(addMonths(`${month}-01`, n).slice(0, 7))
  return (
    <div className="flex items-center gap-2">
      <Button variant="ghost" onClick={() => shift(-1)} aria-label="Mois précédent">‹</Button>
      <span className="min-w-36 text-center font-medium capitalize text-slate-800">{formatMonth(month)}</span>
      <Button variant="ghost" onClick={() => shift(1)} aria-label="Mois suivant">›</Button>
    </div>
  )
}
