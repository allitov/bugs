package io.allitov.bugs.model

import androidx.annotation.DrawableRes

/**
 * Игровая сущность «Жук».
 *
 * Позиция [x], [y], скорость [speed] и размер [size] хранятся в логической системе
 * координат игрового поля и не зависят от пикселей конкретного устройства.
 *
 * @property id       уникальный идентификатор жука
 * @property type     тип жука
 * @property x        координата центра жука по горизонтали в логических единицах
 * @property y        координата центра жука по вертикали в логических единицах
 * @property angle    направление движения в градусах (0 — вверх, отсчёт по часовой стрелке)
 * @property speed    скорость в логических единицах в секунду
 * @property size     радиус жука в логических единицах
 * @property points   стоимость жука в очках
 * @property imageRes визуальный ресурс жука
 */
data class Bug(
    val id: Long,
    val type: BugType,
    var x: Float,
    var y: Float,
    var angle: Float,
    val speed: Float,
    val size: Float,
    val points: Int,
    @DrawableRes val imageRes: Int
)
