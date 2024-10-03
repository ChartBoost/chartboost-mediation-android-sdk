package com.chartboost.sdk.internal.AdUnitManager.data

internal typealias SizeDp = Double

data class InfoIcon(
    val imageUrl: String = "",
    val clickthroughUrl: String = "",
    val position: Position = Position.TOP_LEFT,
    val margin: DoubleSize = DoubleSize(),
    val padding: DoubleSize = DoubleSize(),
    val size: DoubleSize = DoubleSize(),
) {
    enum class Position(val intValue: Int) {
        TOP_LEFT(0),
        TOP_RIGHT(1),
        BOTTOM_LEFT(2),
        BOTTOM_RIGHT(3),
        ;

        companion object {
            fun parse(infoIconPosition: Int): Position {
                return entries.find {
                    it.intValue == infoIconPosition
                } ?: TOP_LEFT
            }
        }
    }

    data class DoubleSize(
        val width: SizeDp = 0.0,
        val height: SizeDp = 0.0,
    )
}
