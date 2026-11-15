# Тест-план

Цель — проверить бизнес-правила, ответ API, сохранение истории, работу PostgreSQL и корректность статистики.

| Требование | Проверки | Уровень |
|---|---|---|
| Начальный статус NEW, приоритет | TicketRulesTest | Unit |
| Переходы NEW → IN_PROGRESS → RESOLVED и отмена | TicketRulesTest | Unit |
| Нельзя пропускать шаги/возобновлять закрытые заявки | TicketRulesTest, TicketServiceTest, ApiIntegrationTest | Unit / E2E |
| Нельзя менять RESOLVED | TicketRulesTest | Unit |
| Просрочка строго после срока | TicketRulesTest | Unit |
| HTTP 404/400 | TicketServiceTest, ApiIntegrationTest | Unit / Integration |
| Миграции и справочник категорий | ApiIntegrationTest.migrationsLoadCategories | Integration / Testcontainers |
| Создание, чтение, фильтрация | ApiIntegrationTest | Integration / Testcontainers |
| Смена статуса и история в базе | ApiIntegrationTest.historyPersistsWithStatus | Integration / Testcontainers |
| Kotlin / Java, пустая статистика, отсутствие закрытых | ReportServiceTest | Unit |
| Неверный период | ReportServiceTest | Unit |
| Полный цикл и конечный отчёт | ApiIntegrationTest.lifecycleEndsWithResolvedAndKotlinReport | E2E |
| Запрет и отсутствие побочных эффектов | ApiIntegrationTest.forbiddenOperationKeepsStateAndHistory | E2E |
| Фильтрация заявок без срока выполнения | ApiIntegrationTest.ticketWithoutDeadlineIsNotOverdue | Integration / Testcontainers |

Все unit-тесты используют фиксированный `Clock`, интеграционные тесты проверяют реальные HTTP-запросы через MockMvc с PostgreSQL Testcontainers. Unit-тесты не зависят от порядка запуска. В CI публикуются JaCoCo HTML/XML и JUnit-отчёт.
