# TZ-003: Голосовой список покупок

- Версия документа: 1.0
- Статус: done
- Дата: 2026-09-28
- Реализовано: build ≤28 (Mic + parser + confirm на вкладке «Покупки»)
- Требования: [F-008b](../features/F-008-voice-input.md) (BR-V-02, 03, 04, 05, 06, 07, 08, 09, 12)
- База инфраструктуры: [TZ-001](TZ-001-voice-input.md) (STT, disclaimer, permission, ошибки)
- Платформа: Android (minSdk 26+), Jetpack Compose
- Оценка: M (реализовано в текущем коде; ТЗ — канон поведения)

## 1. Цель

Добавить в список покупок **один или несколько пунктов голосом** за одну фразу, с назначением категории (из речи / истории / вручную на confirm), без аккаунта и без автосохранения до подтверждения.

## 2. Scope

### In

- Mic (secondary FAB) на экране «Покупки»
- Системный `SpeechRecognizer`, `ru-RU`, `RECORD_AUDIO`
- First-run дисклеймер (общий с задачами, текст TZ-001 §4.1)
- Парсер фразы → список черновиков `(title, categoryId?)`
- Confirm sheet: правка title, выбор/сброс категории на строку, удаление строк
- Пакетное сохранение через `ShoppingViewModel.addItem(title, categoryId)`
- Snackbar после успешного добавления
- Unit-тесты `ShoppingVoiceParser`

### Out

- Автосоздание категорий из речи
- Облачный словарь «товар → категория»
- Голосовое редактирование / удаление уже существующих пунктов
- Голосовая отметка «куплено»
- Wake word, виджет, свой STT SDK
- Сохранение без confirm

## 3. Сценарии

### Happy path — несколько пунктов с общей категорией

1. Пользователь на вкладке «Покупки» → Mic.
2. Disclaimer (если первый раз) → permission → listening.
3. Фраза: «в Продукты: хлеб, молоко и яйца» (категория «Продукты» есть).
4. Parser → 3 строки, у всех `categoryId` = Продукты; titles без префикса.
5. Confirm «Новые покупки»: 3 строки с picker категории.
6. «Добавить (3)» → три пункта в списке → Snackbar (например «Добавлено: 3») → бейдж активных обновлён.

### Happy path — один пункт с категорией в хвосте

1. «молоко в Молочные».
2. Parser → title `молоко`, category «Молочные» (если есть).
3. Confirm → сохранить.

### Happy path — без категории в речи

1. «зубная паста».
2. Если в истории такой title уже был с категорией → подставить её; иначе `categoryId = null`.
3. Confirm → пользователь может выбрать категорию вручную → сохранить.

### Альтернативы / ошибки

| Ситуация | Поведение |
|----------|-----------|
| Отмена listening / processing | Нет записи в БД |
| Отмена confirm | Нет записи в БД |
| STT empty / NO_MATCH | Сообщение «Не удалось распознать речь»; БД не трогать |
| Нет сети (если STT требует) | «Для распознавания речи нужен интернет» |
| Permission deny | Rationale / settings; ручной QuickAdd доступен |
| Parser вернул 0 пунктов | Ошибка «Нет пунктов для добавления» (или empty confirm не открывать) |
| На confirm все titles пустые | «Добавить» disabled |
| Категория из речи не совпала ни с одной | Токен категории игнорируется; дальше история → null |
| У пользователя нет категорий | Только история title или null |

### Edge cases (таблица парсера)

| Вход | Результат |
|------|-----------|
| зубная паста | 1 пункт; cat = история/null |
| хлеб, молоко и яйца | 3 пункта; cat на каждый по правилам |
| молоко в Молочные | title=молоко; cat=Молочные (если есть) |
| в Продукты: хлеб, молоко | оба в Продукты |
| хлеб,, молоко | 2 пункта (пустые фрагменты drop) |
| в Несуществующая: хлеб | shared cat не матчится → title-разбор residual/raw по правилам; без автосоздания |
| очень длинное название… (>200) | truncate 200 + `truncated=true` на confirm |

Прочее: смена вкладки отменяет listening; поворот сохраняет confirm state.

## 4. UI / навигация

| Состояние | Описание |
|-----------|----------|
| Idle (Покупки) | Secondary FAB Mic рядом с Add (паттерн `VoiceCaptureFabColumn`) |
| Disclaimer | Как TZ-001 §4.1, один раз на приложение |
| Listening | «Говорите…», Отмена, Готово |
| Processing | «Распознаём…» |
| Error | Текст из маппинга STT (TZ-001 §6) |
| Confirm | Sheet «Новые покупки»: scrollable список строк — `OutlinedTextField` title + category picker + delete; primary «Добавить (N)» / dismiss «Отмена» |

Ручной QuickAddBar и FAB «+» без регрессии.

## 5. Данные и правила

### Сущность при save

| Поле | Значение |
|------|----------|
| title | с confirm, не blank, max 200 |
| categoryId | с confirm; только существующий id или null |
| isChecked | false |

### `ShoppingVoiceParser` (контракт)

```text
data class ShoppingVoiceLine(
  title: String,
  categoryId: Long?,
  truncated: Boolean
)

fun parseShoppingVoice(
  raw: String,
  categories: List<ShoppingCategoryEntity>,  // id + name
  categoryIdByTitleLower: Map<String, Long?> // как addSuggestedItem
): List<ShoppingVoiceLine>
```

### Алгоритм

1. **Общий префикс категории**  
   `^(?:в\s+категорию|категория|в)\s+(.+?)\s*:\s*(.+)$`  
   group1 equalsIgnoreCase имени категории пользователя → `sharedCategoryId`, residual = group2.  
   Иначе residual = raw, shared = null.

2. **Split** residual по `,` | `;` | `\s+и\s+` | newlines.  
   Не дробить по одиночным пробелам («зубная паста» = 1 пункт).

3. **На фрагмент**  
   - Суффикс категории:  
     `^(.+?)\s+(?:в\s+категорию|в|категория)\s+(.+)$`  
     если group2 = имя категории → localCategoryId, titlePart = group1.  
   - title = normalize(titlePart): trim → collapse spaces → truncate 200.  
   - categoryId = local ?: shared ?: categoryIdByTitleLower[title.lowercase] ?: null.  
   - Пустой title → drop.

4. Пустой список после разбора → ошибка UI, без confirm save.

**Не создавать** категории. Сопоставление имён: trim + equalsIgnoreCase, locale `ru`.

### Confirm → save

- Пользователь может править title, сменить/сбросить category, удалить строку.
- «Добавить» enabled ⇔ есть ≥1 строка с non-blank title.
- Save: для каждой валидной строки `addItem(title.trim(), categoryId)`.
- Порядок: как в confirm (сверху вниз).

## 6. Интеграции

- STT: системный Android SpeechRecognizer (см. TZ-001 §6).
- Категории: только из `observeCategories` / текущего UI state.
- История категорий: `categoryIdByTitleLower` из уже сохранённых shopping items.

## 7. NFR

| ID | Критерий |
|----|----------|
| N-S-01 | Нет записи в БД до confirm |
| N-S-02 | Нет аудио на диске; нет аккаунта |
| N-S-03 | Cancel recognizer при уходе с экрана / onStop |
| N-S-04 | Unit-тесты парсера по таблице §3 |
| N-S-05 | Ручной ввод покупок без регрессии |
| N-S-06 | UI-строки в `strings.xml`, русский |

## 8. Критерии приёмки (DoD)

- [x] Mic на «Покупки» запускает голосовой сценарий
- [x] Disclaimer / permission / ошибки STT — по TZ-001
- [x] «в Продукты: хлеб, молоко и яйца» → 3 пункта с категорией Продукты (если есть)
- [x] «молоко в Молочные» → title без хвоста, категория Молочные
- [x] Без категории в речи → история или null; на confirm можно назначить
- [x] Перечисление через запятую / «и» / перевод строки работает
- [x] Отмена до save → 0 новых записей
- [x] После save пункты видны в нужных группах; бейдж активных корректен
- [x] Unit-тесты `ShoppingVoiceParser` зелёные
- [x] Нет регрессии QuickAdd / категорий / clear checked

## 9. Связь с кодом

| Компонент | Назначение |
|-----------|------------|
| `VoiceCaptureController` / `VoiceCaptureFabColumn` | STT + Mic UI |
| `ShoppingVoiceParser` | Разбор фразы |
| `ShoppingVoiceConfirmSheet` | Confirm |
| `ShoppingListScreen` | Wiring + snackbar |
| `ShoppingViewModel.addItem` | Persist |

## 10. Открытые вопросы

| # | Вопрос | Блокер? | Решение |
|---|--------|---------|---------|
| 1 | Подсказка категории без точного совпадения title? | нет | Out v1; см. TZ-002 V-AI-06 |
| 2 | Лимит числа пунктов за одну фразу? | нет | Без лимита |
| 3 | Дубликаты title в одном confirm | нет | Разрешены |

---

**Статус `done`.** Реализовано в приложении (build ≤28+).
