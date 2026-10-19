# Документы бизнес-анализа

Артефакты ведутся агентом **business-analyst** (`.cursor/skills/business-analyst/`).

## Структура

| Путь | Назначение |
|------|------------|
| `market-YYYY-MM-DD.md` | Рыночный / конкурентный анализ |
| `requirements.md` | Актуальный свод требований |
| `features/` | Детализация user stories |
| `tz/` | Технические задания для разработки |

## Продукт

- **Название:** Task Tracker (android-task-tracker)
- **Платформа:** Android
- **Кратко:** локальный органайзер: задачи (приоритеты, сроки, повторы, подзадачи) + заметки + покупки + здоровье (draft)

## Актуальные документы

| Документ | Статус | Описание |
|----------|--------|----------|
| [market-2026-07-27.md](market-2026-07-27.md) | draft | Сравнительный анализ органайзеров и планировщиков |
| [requirements.md](requirements.md) | draft v0.4 | Свод требований: baseline + Must/Should |
| [features/F-001-reminders.md](features/F-001-reminders.md) | draft | Напоминания |
| [features/F-002-task-lists.md](features/F-002-task-lists.md) | draft | Списки задач |
| [features/F-003-backup-export.md](features/F-003-backup-export.md) | draft | Бэкап / экспорт |
| [features/F-008-voice-input.md](features/F-008-voice-input.md) | draft | Голос: задачи (+дата/время), покупки (+категория) |
| [features/F-009-health.md](features/F-009-health.md) | draft | Здоровье: ≤3 человека, анализы, лекарства/визиты → задача в списке |
| [tz/TZ-001-voice-input.md](tz/TZ-001-voice-input.md) | ready v1.2 | ТЗ: голос + парсинг атрибутов + текст дисклеймера |
| [tz/TZ-004-health.md](tz/TZ-004-health.md) | draft | ТЗ: вкладка «Здоровье» (лимит 3, типы назначений) |
