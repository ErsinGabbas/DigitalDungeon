# DigitalDungeon

Android-приложение — журнал кампаний настольных ролевых игр. Полностью офлайновое:
все данные лежат в локальной базе на устройстве, сети приложение не использует.

## Что умеет

- **Кампании** — список кампаний с названием, описанием и датой создания.
- **Участники кампании** — имя, уровень и максимальный запас HP персонажа; хранятся
  в самой кампании.
- **Сессии** — внутри кампании ведётся список игровых сессий (заголовок, заметки,
  дата создания).
- **Таймлайн событий сессии** — упорядоченная лента событий с типом:
  `SCENE`, `COMBAT`, `DIALOGUE`, `DISCOVERY`, `NOTE`. У события есть необязательный
  заголовок, текст, позиция в таймлайне (`orderIndex`) и отметка времени.
- **Снимки участников в событии** — в момент создания события к нему прикрепляется
  неизменяемый снимок участников (`EventParticipant`: имя, уровень, maxHp).
  Это именно снимок: позже изменённый состав кампании старые события не переписывает.
- **Быстрая заметка** — поле для создания события типа `NOTE` без лишних шагов.

## Стек

| Что | Версия |
| --- | --- |
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.7.3 |
| Gradle | 8.9 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 24 |
| Java (source/target, jvmTarget) | 17 |
| Jetpack Compose | BOM 2024.11.00 (ui 1.7.5, material3 1.3.1) |
| Navigation Compose | 2.8.9 |
| Room (KSP) | 2.6.1 |
| Hilt (KSP) | 2.52 |
| kotlinx.serialization | 1.8.0 |

Кодогенерация Room и Hilt идёт через KSP (не kapt). Версии зависимостей собраны
в каталоге версий `gradle/libs.versions.toml`.

## Архитектура

Три слоя внутри пакета `com.example.digitaldungeon`.

### `data` — доступ к данным

```
data/
  local/
    database/   AppDatabase (Room, version = 7), Migrations.kt
    dao/        CampaignDao, SessionDao, SessionEventDao — suspend-функции и Flow
    entity/     CampaignEntity, SessionEntity, SessionEventEntity
  mapper/       CampaignMapper, SessionMapper, SessionEventMapper
  repository/   интерфейсы *Repository + реализации *RepositoryImpl
```

- DAO возвращают `Flow<List<...Entity>>` для списков и `suspend` для записи.
- Мапперы переводят entity ↔ domain-модель. Списки участников хранятся в колонке
  `participants` как JSON-строка (kotlinx.serialization); битый или несовместимый
  JSON не пробрасывается наружу, а деградирует до пустого списка
  (`decodeParticipants` в `CampaignMapper.kt`).
- Репозитории — единственная точка, через которую UI видит данные; интерфейс отдельно
  от реализации, чтобы его можно было подменить в тестах.

### `domain` — модели

`Campaign`, `Session`, `SessionEvent` + `SessionEventType`, `EventParticipant`.
Это обычные `data class` без зависимостей от Android и Room. `EventParticipant`
помечен `@Serializable` — именно он сериализуется в колонку `participants`.

### `ui` — Compose + ViewModel

```
ui/
  navigation/   AppNavigation.kt — NavHost, маршруты campaigns / details/{campaignId} / session/{sessionId}
  campaign/     screen/ + components/ + state/ + viewmodel/
  session/      screen/ + components/ + state/ + viewmodel/
  components/   общие диалоги (ConfirmDeleteDialog)
  theme/        Color.kt, Theme.kt, Type.kt (Material3)
```

- Каждый экран получает ViewModel через `hiltViewModel()`.
- ViewModel держит один `StateFlow<...UiState>`; экран читает его через
  `collectAsStateWithLifecycle()`. Классы состояния лежат в `state/`.
- `MainActivity` помечена `@AndroidEntryPoint`, включает edge-to-edge и рисует
  `AppNavigation()` внутри `DigitalDungeonTheme`.

### `di` — Hilt

- `DigitalDungeonApplication` — `@HiltAndroidApp`.
- `di/AppModule.kt` — `AppDatabase` (singleton, имя файла базы `digital_dungeon_db`) и DAO.
- `di/RepositoryModule.kt` — `@Binds` интерфейсов репозиториев на их реализации.

## Сборка и запуск

Нужен JDK 17 или новее (`gradle/gradle-daemon-jvm.properties` просит toolchain 21;
путь к локальному JDK, если он задан, лежит в `local.properties` и в репозиторий не
коммитится) и Android SDK с platform 35.

```bash
# debug APK
./gradlew :app:assembleDebug

# установить на подключённое устройство или запущенный эмулятор
./gradlew :app:installDebug

# release APK (с минификацией и шринкингом ресурсов, без подписи — см. ниже)
./gradlew :app:assembleRelease
```

Из Android Studio: открыть корень проекта, дождаться синка Gradle, запустить
конфигурацию `app`.

## Тесты

```bash
# юнит-тесты (JVM, устройство не нужно)
./gradlew :app:testDebugUnitTest

# инструментальные тесты — ТРЕБУЮТ подключённого устройства или запущенного эмулятора
./gradlew :app:connectedDebugAndroidTest
```

- Юнит-тесты: `app/src/test/java/...`. Для корутин подключён
  `kotlinx-coroutines-test` (`runTest`, тестовые диспетчеры).
- Инструментальные тесты: `app/src/androidTest/java/...` — там же лежат тесты
  миграций Room.

## Миграции Room

Текущая версия схемы — **7** (`AppDatabase`, `exportSchema = true`).

- Миграции описаны в
  `app/src/main/java/com/example/digitaldungeon/data/local/database/Migrations.kt`.
  Все они собраны в массив `ALL_MIGRATIONS` — и Hilt-провайдер базы, и тесты миграций
  используют именно его, поэтому забыть зарегистрировать миграцию не получится:
  тесты упадут.
- Экспортированные схемы — в `app/schemas/com.example.digitaldungeon.data.local.database.AppDatabase/`
  (файлы `4.json` … `7.json`). Папка подключена к `androidTest` как assets
  (`sourceSets` в `app/build.gradle.kts`), чтобы `MigrationTestHelper` мог их читать.
  **Экспортированные схемы коммитятся в репозиторий** — без них миграции не протестировать.
- Схемы версий **1–3 никогда не экспортировались**. Поэтому:
  - для версий 1 и 2 включён деструктивный откат
    (`fallbackToDestructiveMigrationFrom(*DESTRUCTIVELY_MIGRATED_VERSIONS)`) — такие базы
    пересоздаются с нуля вместо падения при открытии;
  - `MIGRATION_3_4` — пустая заглушка: воспроизвести изменения версии 3 не из чего,
    но без неё путь 3 → 7 разорвался бы.
- Тесты: `MigrationTest` (пошаговые и сквозные пути начиная с версии 4) и
  `DatabaseFallbackTest` (поведение на базе без пути миграции) —
  в `app/src/androidTest/java/com/example/digitaldungeon/data/local/database/`.

### Как добавить миграцию

1. Изменить entity и поднять `version` в `@Database`.
2. Собрать проект — KSP положит новый `<version>.json` в `app/schemas/`.
3. Написать `MIGRATION_<n>_<n+1>` в `Migrations.kt` и добавить её в `ALL_MIGRATIONS`.
4. Дописать тест в `MigrationTest` и прогнать `:app:connectedDebugAndroidTest`.
5. Закоммитить новый файл схемы вместе с кодом.

## Перед релизом

1. **Настроить подпись.** Keystore в репозитории нет и быть не должно. Нужно создать
   его локально, положить пароли в `local.properties` (файл в `.gitignore`) или в
   переменные окружения CI, и добавить в `app/build.gradle.kts` блок
   `signingConfigs { create("release") { ... } }`, подставив его в `buildTypes.release`
   через `signingConfig`. Сейчас `assembleRelease` собирает неподписанный артефакт.
2. **Поднять версию.** В `app/build.gradle.kts` сейчас `versionCode = 1` и
   `versionName = "1.0"` — значения по умолчанию, их нужно обновлять на каждый выпуск.
3. **Проверить минифицированную сборку на устройстве.** В release включены
   `isMinifyEnabled` и `isShrinkResources`, поэтому keep-правила из
   `app/proguard-rules.pro` реально применяются. Критичный сценарий — события и
   кампании с участниками: если R8 выбросит сгенерированный сериализатор
   `EventParticipant`, списки участников молча станут пустыми (ошибка декодирования
   подавляется в `decodeParticipants`), и в debug-сборке это не видно.
4. **Решение по бэкапу.** Бэкап включён осознанно: приложение офлайновое, журнал ведут
   месяцами, потеря данных при смене устройства хуже, чем факт их попадания в облачный
   бэкап аккаунта. Что именно бэкапится — явно перечислено в
   `app/src/main/res/xml/backup_rules.xml` (Android 6–11) и
   `app/src/main/res/xml/data_extraction_rules.xml` (Android 12+): файл базы
   `digital_dungeon_db` вместе с `-wal` и `-shm`. Если решение будет пересмотрено,
   менять нужно оба файла и `android:allowBackup` в манифесте.
