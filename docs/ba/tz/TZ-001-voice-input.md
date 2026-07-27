# TZ-001: Голосовое добавление задач и покупок

- Версия документа: 1.2
- Статус: ready
- Дата: 2026-07-27
- Обновлено: 2026-07-27 — v1.2: зафиксирован текст first-run дисклеймера
- Требования: [F-008](../features/F-008-voice-input.md) (F-008a/b), BR-V-01…BR-V-12
- Платформа: Android (minSdk 26, targetSdk 35), Jetpack Compose
- Оценка: M→L (парсеры + confirm UI)

## 1. Цель

Быстрый голосовой захват **задач** (с разбором даты/времени исполнения) и **покупок** (с назначением категории), с обязательным подтверждением черновика. Без аккаунта приложения; системный STT с прозрачностью про интернет.

## 2. Scope

### In

- Кнопка голоса на экранах Задачи и Покупки
- `RECORD_AUDIO`, rationale, отказ, settings deep-link
- `SpeechRecognizer`, `ru-RU`, first-run дисклеймер
- Состояния: idle → listening → processing → confirm / error
- Confirm до записи в БД
- **TaskVoiceParser:** дата и/или время из русской фразы + очистка title
- Confirm задачи: редактирование title, даты, времени (точки или интервала), сброс срока
- **ShoppingVoiceParser:** split перечисления + категория на пункт (явная / история / null)
- Confirm покупок: правка title, выбор категории на строку, удаление строк
- Нормализация, max 200 символов
- Snackbar после save
- Unit-тесты обоих парсеров

### Out

- Приоритет и повторы из речи
- «Через час» / относительное время без явной даты (Could)
- Автосоздание категорий и словарь «товар → категория» без категорий пользователя
- Wake word, Assistant, виджет (F-007)
- On-device STT как must
- Свои cloud STT API
- Автосейв без confirm

## 3. Сценарии

### Happy path — задача

1. Тап Mic на «Задачи» → permission / disclaimer → listening.
2. Фраза, например: «Позвонить маме завтра в 18:00».
3. `TaskVoiceParser` → title «Позвонить маме», date=завтра, time=18:00.
4. Confirm sheet: поля названия, даты, времени предзаполнены; пользователь может править.
5. «Добавить» → `TaskViewModel.addTask(...)` с полями с confirm → Snackbar «Задача добавлена».

### Happy path — покупки

1. Mic на «Покупки» → listening.
2. Фраза: «в Продукты: хлеб, молоко и яйца» (категория «Продукты» существует).
3. Parser → 3 пункта, у всех categoryId категории «Продукты», titles без префикса.
4. Confirm: список строк с chip/picker категории; правка возможна.
5. «Добавить (3)» → три `addItem(title, categoryId)` → Snackbar «Добавлено: 3».

### Альтернативные / ошибки

| Ситуация | Поведение |
|----------|-----------|
| Отказ микрофона / permanent deny | Как в v1 ТЗ: rationale / settings + ручной ввод |
| STT unavailable / сеть / пустой результат | Сообщения §6; без записи в БД |
| Отмена до «Добавить» | Нет записи в БД |
| Парсер не нашёл срок | Confirm без даты/времени; title = normalize(raw) |
| После очистки title пуст | Fail-soft: title = normalize(raw), срок не применять |
| Категория из речи не найдена среди категорий пользователя | Игнор явного токена; дальше история → null |
| Нет категорий у пользователя | Только история (обычно null) или null |
| Пустое название на confirm | «Добавить» disabled |

### Edge cases

**Задачи**

| Вход | title | date | time |
|------|-------|------|------|
| Позвонить маме | Позвонить маме | — | — |
| Позвонить маме завтра в 18:00 | Позвонить маме | завтра | 18:00 |
| Встреча в пятницу с 14 до 15 | Встреча | ближайшая пт | 14:00–15:00 |
| Созвон сегодня в 9 утра | Созвон | сегодня | 09:00 |
| Отчёт через 3 дня | Отчёт | сегодня+3 | — |

**Покупки**

| Вход | Результат |
|------|-----------|
| зубная паста | 1 пункт; категория история/null |
| хлеб, молоко и яйца | 3 пункта; категория на каждый по правилам |
| молоко в Молочные | title молоко; cat Молочные (если есть) |
| в Продукты: хлеб, молоко | оба в Продукты |
| хлеб,, молоко | 2 пункта |

Прочие: truncate 200; смена вкладки отменяет сессию; поворот сохраняет confirm state.

## 4. UI / навигация

| Экран / состояние | Описание | Переходы |
|-------------------|----------|----------|
| List idle | Secondary FAB `Mic`, одинаково на обоих экранах | → permission / disclaimer / listening |
| First-run disclaimer | Диалог/sheet **до** первого запуска STT (если `voiceDisclaimerAccepted != true`). Текст — §4.1. Кнопки: «Отмена» → idle; «Продолжить» → выставить флаг и дальше permission (если нужно) / listening | |
| Permission rationale | «Микрофон нужен, чтобы диктовать задачи и покупки» | → system permission / dismiss |
| Listening / processing / error | Listening: «Говорите…», «Отмена», «Готово»; processing: «Распознаём…» | → confirm / error / idle |
| Confirm task | Sheet «Новая задача»: title; блок срока — дата (datepicker / chips Сегодня\|Завтра\|Сбросить); время опционально (time picker; для интервала — начало и конец как в TaskEditor); «Отмена» / «Добавить» | → save |
| Confirm shopping | Sheet «Новые покупки»: строки title + `ShoppingCategoryPickerRow` (или компактный dropdown) на строку; delete; «Добавить (N)» | → save batch |

FAB «+» и QuickAddBar сохранить.

### 4.1. Текст first-run дисклеймера (утверждён)

Показывать **один раз** до первого использования голоса. Строки — в `strings.xml`.

| Элемент | Текст (verbatim) |
|---------|------------------|
| Заголовок | Распознавание речи |
| Тело | Чтобы диктовать задачи и покупки, приложению нужен доступ к микрофону. Распознавание выполняется средствами Android и может потребовать подключение к интернету. Запись голоса в приложении не сохраняется. |
| Primary | Продолжить |
| Dismiss | Отмена |

Рекомендуемые id ресурсов: `voice_disclaimer_title`, `voice_disclaimer_body`, `voice_disclaimer_continue`, `voice_disclaimer_cancel` (cancel можно переиспользовать общей строкой «Отмена»).

Поведение:

1. «Продолжить» → `voiceDisclaimerAccepted = true` → не показывать снова → запрос `RECORD_AUDIO` (если ещё не выдан) → listening.
2. «Отмена» → закрыть без изменения флага (при следующем тапе Mic дисклеймер снова).
3. Компактный вариант **не использовать** в UI v1; полный текст выше — единственный канонический.

## 5. Данные и правила

### Сущности

| Сущность | Поля с голоса | Ограничения |
|----------|---------------|-------------|
| Task | title; notes=""; priority=MEDIUM; dueDateEpochDay?; dueTimeMinutes?; dueTimeEndMinutes?; recurrence=NONE | title не blank; max 200; если date=null → times null (как Repository) |
| ShoppingItem | title; categoryId?; isChecked=false | title не blank; max 200; categoryId только существующий id или null |
| Settings | voiceDisclaimerAccepted | |

### Нормализация title

`trim → whitespace collapse → truncate 200`.

### TaskVoiceParser (контракт)

```text
data class TaskVoiceParseResult(
  title: String,
  dueDateEpochDay: Long?,
  dueTimeMinutes: Int?,
  dueTimeEndMinutes: Int?,
  truncated: Boolean
)

fun parseTaskVoice(raw: String, now: LocalDateTime, zone: ZoneId): TaskVoiceParseResult
```

Правила паттернов — по F-008 § «Парсинг даты и времени». Часовой пояс: `ZoneId.systemDefault()`. Использовать/расширять идеи `DateUtils`, не дублировать противоречиво.

После извлечения — удалить matched spans из raw → normalize. Пустой title → fail-soft (title=normalize(raw), все due* = null).

Интервал: end > start, иначе сохранить только start.

### ShoppingVoiceParser (контракт)

```text
data class ShoppingVoiceLine(
  title: String,
  categoryId: Long?,
  truncated: Boolean
)

fun parseShoppingVoice(
  raw: String,
  categories: List<ShoppingCategoryEntity>,  // id + name
  categoryIdByTitleLower: Map<String, Long?> // как история addSuggestedItem
): List<ShoppingVoiceLine>
```

Алгоритм:

1. Детект общего префикса категории:  
   `^(?:в\s+категорию|категория|в)\s+(.+?)\s*:\s*(.+)$`  
   где group1 trim equalsIgnoreCase имени категории → `sharedCategoryId`; residual = group2.  
   Если не матч — residual = raw, sharedCategoryId = null.
2. Split residual по `,` | `;` | `\s+и\s+` | newlines.
3. Для каждого фрагмента:
   - Попытка суффикса/вставки явной категории:  
     `^(.+?)\s+(?:в\s+категорию|в|категория)\s+(.+)$`  
     если group2 equalsIgnoreCase имени категории → localCategoryId, titlePart=group1; иначе titlePart=фрагмент, localCategoryId=null.
   - title = normalize(titlePart)
   - categoryId = localCategoryId ?: sharedCategoryId ?: categoryIdByTitleLower[title.lowercase()] ?: null
   - drop empty titles
4. Пустой список → ошибка «Нет пунктов для добавления».

Не создавать категории. Сопоставление **только** по `category.name` пользователя (equalsIgnoreCase, trim).

### Валидации UI / save

- Task: enabled ⇔ title not blank; при save передать due* с confirm; recurrence NONE.
- Shopping: enabled ⇔ ≥1 valid line; save каждую через `addItem(title, categoryId)`.

## 6. Интеграции

### SpeechRecognizer

Без изменений: system API, `ru-RU`, `RECORD_AUDIO`, без сторонних STT SDK.

### Маппинг ошибок STT

| Код | Текст |
|-----|-------|
| ERROR_AUDIO / CLIENT | «Ошибка микрофона. Попробуйте ещё раз» |
| ERROR_NETWORK / TIMEOUT | «Для распознавания речи нужен интернет» |
| ERROR_NO_MATCH / empty | «Не удалось распознать речь» |
| ERROR_SPEECH_TIMEOUT | «Речь не распознана. Попробуйте ещё раз» |
| ERROR_RECOGNIZER_BUSY | «Распознавание занято. Подождите» |
| not available | «Распознавание речи недоступно на устройстве» |

## 7. NFR

| ID | Критерий |
|----|----------|
| N-V-01 | Listening UI ≤ 1 с при выданном permission (цель) |
| N-V-02 | Ручной ввод без регрессии |
| N-V-03 | Строки UI в `strings.xml`, русский |
| N-V-04 | Нет аудио на диске; нет аккаунта |
| N-V-05 | Cancel recognizer onStop / смена вкладки |
| N-V-06 | Unit-тесты `TaskVoiceParser` + `ShoppingVoiceParser` по таблицам §3 |

## 8. Критерии приёмки (DoD)

- [ ] Голос на Задачах и Покупках (единый паттерн кнопки)
- [ ] First-run дисклеймер с текстом §4.1 (verbatim); повторно не показывается после «Продолжить»
- [ ] Permission, ошибки STT
- [ ] «Позвонить маме завтра в 18:00» → confirm с title без срока + дата завтра + 18:00; после save поля в БД верны
- [ ] Пользователь может сменить/сбросить срок на confirm — в БД уходит его выбор
- [ ] Фраза без срока → задача без due
- [ ] «в Продукты: хлеб, молоко и яйца» → 3 пункта с category Продукты (если категория есть)
- [ ] «молоко в Молочные» → категория Молочные; title без хвоста
- [ ] Без категории в речи → история title или null
- [ ] Confirm позволяет сменить категорию
- [ ] Отмена до save → нет записей
- [ ] Unit-тесты парсеров зелёные
- [ ] Edge cases §3; нет регрессии FAB/QuickAdd

## 9. Декомпозиция

1. `VoiceCaptureController` (SpeechRecognizer)
2. `TaskVoiceParser` + `ShoppingVoiceParser` — pure Kotlin, тесты
3. Confirm sheets (task с датой/временем; shopping с category picker)
4. Wiring TaskTrackerScreen / ShoppingListScreen
5. Settings disclaimer flag + Manifest permission

## 10. Открытые вопросы

| # | Вопрос | Блокер? | Решение |
|---|--------|---------|---------|
| 1 | Релизный Must/Should | нет | Should |
| 2 | «Через час» | нет | **Out** v1 |
| 3 | System STT | нет | Зафиксировано |
| 4 | Стоп | нет | End-of-speech + «Готово»; всегда confirm |
| 5 | Виджет | нет | Out |
| 6 | Место Mic | нет | Secondary FAB |
| 7 | >200 символов | нет | Truncate + индикатор |
| 8 | Дата/время и категория из речи | нет | **In** (v1.1) |
| 9 | Текст дисклеймера | нет | **Зафиксирован** в §4.1 (v1.2) |

---

**Статус `ready`.**  
История: 2026-07-27 v1.1 — парсинг срока и категорий; v1.2 — текст first-run дисклеймера.
