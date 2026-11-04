// One planning occurrence as a line of an agenda: time, title, type and attendees.
import type { Schemas } from '../api/client.ts'
import { useSession } from '../app/useSession.ts'
import { formatTime } from '../lib/format.ts'
import { ENTRY_TYPES, EVENT_CATEGORIES, entryColor } from '../lib/labels.ts'
import { Badge, MemberDot } from './ui.tsx'

export function OccurrenceLabel({ occurrence }: { occurrence: Schemas['Occurrence'] }) {
  const { members } = useSession()
  const { entry } = occurrence
  const attendees = members.filter((m) => entry.memberIds.includes(m.id))
  return (
    <div className="flex items-center gap-3 py-1.5 text-sm">
      <span className="w-14 shrink-0 tabular-nums text-ink-3">{entry.allDay ? 'Journée' : formatTime(occurrence.start)}</span>
      <span className={`h-2.5 w-2.5 shrink-0 rounded-full border-2 ${entryColor(entry)}`} aria-hidden="true" />
      <span className={`flex-1 truncate ${entry.done ? 'text-ink-3 line-through' : ''}`}>{entry.title}</span>
      <Badge>{entry.type === 'EVENT' ? EVENT_CATEGORIES[entry.category ?? 'OTHER'] : ENTRY_TYPES[entry.type]}</Badge>
      <span className="flex -space-x-1">{attendees.map((m) => <MemberDot key={m.id} member={m} size="h-5 w-5 text-[10px]" />)}</span>
    </div>
  )
}
