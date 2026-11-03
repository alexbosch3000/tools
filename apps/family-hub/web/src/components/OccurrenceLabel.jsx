// One planning occurrence as a line of an agenda: time, title, type and attendees.
import { formatTime } from '../lib/format.js'
import { ENTRY_TYPES, EVENT_CATEGORIES, entryColor } from '../lib/labels.js'
import { useSession } from '../lib/session.js'
import { Badge, MemberDot } from './ui.jsx'

export default function OccurrenceLabel({ occurrence }) {
  const { members } = useSession()
  const { entry } = occurrence
  const attendees = members.filter((m) => entry.memberIds.includes(m.id))
  return (
    <div className="flex items-center gap-3 py-1.5 text-sm">
      <span className="w-12 shrink-0 tabular-nums text-slate-500">
        {entry.allDay ? 'Journée' : formatTime(occurrence.start)}
      </span>
      <span className={`h-2 w-2 shrink-0 rounded-full border-2 ${entryColor(entry)}`} />
      <span className={`flex-1 truncate ${entry.done ? 'text-slate-400 line-through' : 'text-slate-800'}`}>
        {entry.title}
      </span>
      <Badge>{entry.type === 'EVENT' ? EVENT_CATEGORIES[entry.category] : ENTRY_TYPES[entry.type]}</Badge>
      <span className="flex -space-x-1">{attendees.map((m) => <MemberDot key={m.id} member={m} size="h-5 w-5 text-[10px]" />)}</span>
    </div>
  )
}
