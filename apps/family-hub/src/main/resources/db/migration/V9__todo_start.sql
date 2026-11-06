-- Optional start of the work on a to-do item, in the same text format as due_at; items with a start show
-- as a span in the planning.
ALTER TABLE todo_item ADD COLUMN start_at TEXT;
