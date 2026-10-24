package com.wardclick.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wardclick.app.data.model.CreateBookingRequest
import com.wardclick.app.data.remote.MockApiService
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Обход экранов приложения со скриншотами — для проверки вёрстки на эмуляторе
 * (workflow «UI screenshots»). Снимки и журнал шагов — в files/screens приложения.
 * Шаг, который не удался, не останавливает обход: он помечается в log.txt и снимке *_FAIL.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTour {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val prefix = InstrumentationRegistry.getArguments().getString("prefix", "")
    private val dir = File(context.filesDir, "screens").apply { mkdirs() }
    private val log = StringBuilder()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Test
    fun tour() {
        prepareData()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            this.scenario = scenario
            step("01_cities") { waitText("Где хотите отдохнуть?") }
            step("02_list") { click("Москва"); waitText("Особняки в городе") }
            step("03_details") { click("Особняк на Рублёвке"); waitText("Удобства") }
            step("04_details_vip") { scrollTo("Расположение") }
            step("05_details_reviews") { scrollTo("Отзывы (") }
            step("05b_reviewer") { scrollTo("Профиль ›"); click("Профиль ›"); waitText("Профиль гостя") }
            step("06_list_again") { back(); waitText("Удобства"); back(); waitText("Особняки в городе") }
            step("07_map") { click("Карта"); Thread.sleep(4000) }
            step("08_filters") { click("Список"); clickDesc("Фильтры"); waitText("Сортировка") }
            step("09_vip") { click("Показать"); Thread.sleep(800); nav("VIP"); Thread.sleep(1200) }
            step("10_favorites_guest") { nav("Избранное") }
            step("11_login") { nav("Профиль"); click("Войти"); waitText("Тестовые аккаунты", substring = true) }
            step("12_profile_admin") {
                click("Администратор"); click("Войти"); waitText("Выйти из аккаунта")
            }
            step("13_admin_overview") {
                repeat(10) { click("Версия приложения", substring = true) }
                waitText("Панель администратора")
            }
            step("14_admin_bookings") { click("Аренды (", substring = true); waitText("Ожидает подтверждения") }
            step("15_admin_listings") { click("Особняки"); waitText("Скрыть") }
            step("16_bookings_admin") { back(); waitText("Выйти из аккаунта"); nav("Аренды"); waitText("Мои аренды") }
            step("17_search_from_bookings") {
                nav("Поиск")
                waitAny("Особняки в городе", "Где хотите отдохнуть?")
            }
            step("18_booking_form") {
                click("Особняк на Рублёвке"); waitText("Удобства"); click("Арендовать")
                waitText("Аренда особняка")
            }
            step("19_booking_form_bottom") { scrollTo("Оплата при заселении") }
            step("20_user_bookings") {
                back(); back(); Thread.sleep(500)
                nav("Профиль"); click("Выйти из аккаунта"); Thread.sleep(800)
                nav("Профиль"); click("Войти"); click("Пользователь"); click("Войти")
                waitText("Выйти из аккаунта")
                nav("Аренды"); waitText("Мои аренды")
            }
            step("21_user_completed") { click("Завершённые"); Thread.sleep(800) }
            step("22_favorites_user") { nav("Избранное"); Thread.sleep(800) }
        }
        File(dir, "${prefix}log.txt").writeText(log.toString())
    }

    /** Чистое демо-состояние + брони, чтобы были видны карточки с кнопками. */
    private fun prepareData() {
        listOf("wardclick_mock", "wardclick_session", "favorites").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
        val today = LocalDate.now()
        fun book(token: String, listing: String, from: Long, nights: Long, guests: Int, vip: List<String>) = runBlocking {
            MockApiService(context.applicationContext) { token }.createBooking(
                CreateBookingRequest().apply {
                    listingId = listing
                    checkInDate = today.plusDays(from).toString()
                    checkOutDate = today.plusDays(from + nights).toString()
                    guestsCount = guests
                    vipServiceIds = vip
                }
            )
        }
        // как на скриншоте пользователя: бронь администратора со всеми VIP-услугами
        book("mock-admin-token", "l1", 4, 8, 2, listOf("v1", "v2", "v3", "v4", "v5", "v6"))
        book("mock-user-token", "l3", 10, 4, 4, listOf("v2", "v4"))
    }

    private fun step(name: String, block: () -> Unit) {
        val ok = runCatching(block)
        shot(if (ok.isSuccess) name else "${name}_FAIL")
        log.append(name).append(": ").append(ok.exceptionOrNull()?.let { "FAIL ${it.javaClass.simpleName}: ${it.message?.take(300)}" } ?: "ok").append('\n')
    }

    private fun shot(name: String) {
        Thread.sleep(700)
        rule.waitForIdle()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        File(dir, "$prefix$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun waitText(text: String, substring: Boolean = false, timeout: Long = 10_000) =
        rule.waitUntil(timeout) {
            rule.onAllNodesWithText(text, substring = substring, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

    private fun waitAny(vararg texts: String, timeout: Long = 6_000) =
        rule.waitUntil(timeout) {
            texts.any { rule.onAllNodesWithText(it, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        }

    private fun click(text: String, substring: Boolean = false) {
        waitText(text, substring)
        rule.onAllNodesWithText(text, substring = substring, useUnmergedTree = true).onFirst().performClick()
        rule.waitForIdle()
    }

    private fun clickDesc(description: String) {
        rule.onAllNodesWithContentDescription(description, useUnmergedTree = true).onFirst().performClick()
        rule.waitForIdle()
    }

    /** Вкладка нижней панели (у иконки вкладки contentDescription = подпись). */
    private fun nav(label: String) {
        clickDesc(label)
        Thread.sleep(600)
    }

    /** «Назад» через диспетчер активити: не требует фокуса окна (в отличие от Espresso.pressBack). */
    private fun back() {
        scenario?.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        Thread.sleep(500)
    }

    private fun scrollTo(text: String) {
        rule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(text, substring = true))
        rule.waitForIdle()
    }
}
