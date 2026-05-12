package com.kakdela.p2p.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.system.exitProcess

/**
 * Экран пользовательского соглашения.
 * Заменяет OnboardingScreen для прямого юридического подтверждения.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Цветовая палитра из вашей неоновой темы
    val colorBg = Color(0原生_09, 0原生_09, 0原生_0B) // #09090B
    val colorCyan = Color(0xFF00FFFF)
    val colorPink = Color(0xFFFF00FF)
    val colorSurface = Color(0xFF18181B)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding() // Отступы для статус-бара и навигации
        ) {
            // Заголовок
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "СОГЛАШЕНИЕ",
                    color = colorCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
            }

            // Контейнер с текстом соглашения
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, colorSurface, RoundedCornerShape(12.dp))
                    .background(colorSurface.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    Text(
                        text = AGREEMENT_TEXT,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }

            // Панель кнопок
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Кнопка принятия (Основная - Циан)
                Button(
                    onClick = onFinished,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorCyan),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = "ПРИНЯТЬ И ПРОДОЛЖИТЬ",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Кнопка отказа (Второстепенная - Розовая обводка)
                OutlinedButton(
                    onClick = {
                        // Закрытие приложения при отказе
                        exitProcess(0)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "ОТКЛОНИТЬ И ВЫЙТИ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = "Нажимая «Принять», вы подтверждаете совершеннолетие (18+)",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

private const val AGREEMENT_TEXT = """
ПОЛЬЗОВАТЕЛЬСКОЕ СОГЛАШЕНИЕ
для мессенджера «Как дела?» (далее — Приложение)

Последняя редакция: «05 мая 2026 г.»
Версия документа: 1.0

Важно! Внимательно прочитайте этот документ перед использованием Приложения. Устанавливая, запуская, копируя или любым иным способом используя Приложение, вы подтверждаете, что полностью ознакомились с условиями настоящего Соглашения, поняли их и согласны с ними в полном объёме. Если вы не согласны с каким-либо пунктом, вы обязаны немедленно прекратить использование Приложения и удалить его со всех устройств.

1. Общие положения. Статус Приложения
1.1. Приложение представляет собой децентрализованное программное обеспечение, предоставляемое на условиях «как есть» (AS IS). Оно объединяет технологии peer-to-peer сети Tyr и протоколы сквозного шифрования E2EE.
1.2. Разработчик (Администрация) не владеет, не управляет и не контролирует содержимое сообщений, файлов или метаданных пользователей.
1.3. Все коммуникации в Приложении по умолчанию шифруются. Разработчик не имеет технической возможности расшифровать или хранить вашу переписку.
1.4. Приложение не является организатором распространения информации (ОРИ). Любые иные выводы могут быть установлены только вступившим в законную силу решением суда.
1.5. Приложение предназначено исключительно для личного, семейного и домашнего использования.

2. Возрастные ограничения
2.1. Приложением могут пользоваться лица, достигшие возраста 18 лет.
2.2. Если вы не достигли указанного возраста, вы не имеете права использовать Приложение.

3. Ответственность Пользователя
3.1. Вы самостоятельно несёте ответственность за соблюдение законодательства РФ (включая ФЗ №149, №152, №114), содержание сообщений и сохранность ваших ключей шифрования.
3.2. Пользователь обязуется не использовать Приложение для распространения запрещённых материалов (экстремизм, призывы к насилию, вредоносное ПО и др.).
3.3. Любые претензии третьих лиц урегулируются Пользователем за свой счёт.

4. Отказ от гарантий (Disclaimer)
4.1. Приложение предоставляется «КАК ЕСТЬ». Разработчик не гарантирует бесперебойную работу или отсутствие ошибок.
4.2. Использование осуществляется на ваш собственный страх и риск.

5. Ограничение ответственности Разработчика
5.1. Разработчик не несёт ответственности за косвенные убытки или потерю данных.
5.2. Максимальная ответственность Разработчика ограничена суммой в 0 рублей, так как Приложение бесплатно.

6. Отсутствие контроля и модерации
6.1. Разработчик не осуществляет модерацию переписки в силу технической невозможности доступа к E2EE-данным.
6.2. Все жалобы на нарушения прав должны направляться непосредственно конечному нарушителю.

7. Интеллектуальная собственность
7.1. Исходный код Приложения распространяется на условиях открытой лицензии.
7.2. Логотипы и дизайн являются собственностью Разработчика.

8. Персональные данные
8.1. Приложение не собирает персональные данные на серверах. Обработка происходит локально на вашем устройстве.

9. Применимое право
9.1. Настоящее Соглашение регулируется законодательством Российской Федерации.
9.2. Все споры рассматриваются в суде по месту регистрации Разработчика.

10. Изменение Соглашения
10.1. Разработчик вправе менять Соглашение в любое время. Продолжение использования означает ваше согласие с новой редакцией.
"""