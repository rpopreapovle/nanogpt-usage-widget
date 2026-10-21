# NanoGPT Balance Widget

Суперлёгкий Android-виджет (≈15 КБ, без зависимостей): показывает **баланс** аккаунта NanoGPT и **траты за сегодня** прямо на рабочем столе.

## Что показывает
- `$баланс` — `POST /api/check-balance`
- `сегодня: $X` — `GET /api/v1/usage?from=сегодня&to=сегодня&group_by=day` (netCostUsd, UTC)
- число запросов за сегодня
- время последнего обновления

## Как работает
- Обновление каждые 15 минут через `AlarmManager` + стандартный `updatePeriodMillis` (30 мин), после загрузки телефона и по тапу на виджет.
- Ответы кэшируются в `SharedPreferences`, так что без сети виджет показывает последние данные.
- API-ключ хранится только локально (SharedPreferences), бэкап приложения отключён.
- Никаких библиотек, только платформа + `org.json`.

## Сборка
```bash
gradle assembleRelease
```
APK: `app/build/outputs/apk/release/app-release-unsigned.apk`, подписывать своим ключом.

## Установка
1. Поставить APK.
2. Открыть приложение, вставить API-ключ NanoGPT (`nano-gpt.com` → API keys), «Сохранить ключ».
3. «Добавить виджет на рабочий стол» (или долгое нажатие на рабочем столе → Виджеты → NanoGPT Balance).

## API
Документация: https://docs.nano-gpt.com/llms.txt
