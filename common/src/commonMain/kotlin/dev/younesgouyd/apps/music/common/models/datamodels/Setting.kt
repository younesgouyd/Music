package dev.younesgouyd.apps.music.common.models.datamodels

import kotlinx.serialization.Serializable

@Serializable
sealed class Setting {
    abstract val name: String
    abstract val valueSerialized: String
    abstract val defaultValueSerialized: String

    @Serializable
    data class DarkMode(val value: Values) : Setting() {
        companion object {
            const val NAME = "dark_mode"
            val defaultValue = Values.Enabled

            fun default() = DarkMode(defaultValue)
        }

        override val name: String = NAME
        override val valueSerialized: String = value.name
        override val defaultValueSerialized: String = defaultValue.name

        enum class Values(val label: String) {
            SystemDefault("System Default"),
            Disabled("Disabled"),
            Enabled("Enabled")
        }
    }

    @Serializable
    data class ThemeFromPlaying(val value: Boolean) : Setting() {
        companion object {
            const val NAME = "THEME_COLOR_SOURCE"
            private const val DEFAULT_VALUE = true

            fun default() = ThemeFromPlaying(DEFAULT_VALUE)
        }

        override val name: String = NAME
        override val valueSerialized: String = value.toString()
        override val defaultValueSerialized: String = DEFAULT_VALUE.toString()
    }

    @Serializable
    data class PaletteStyle(val value: Values) : Setting() {
        companion object {
            const val NAME = "THEME_PALETTE_STYLE"
            val defaultValue = Values.Fidelity

            fun default() = PaletteStyle(defaultValue)
        }

        override val name: String = NAME
        override val valueSerialized: String = value.name
        override val defaultValueSerialized: String = defaultValue.name

        enum class Values {
            Cmf,
            Content,
            Expressive,
            Fidelity,
            FruitSalad,
            Monochrome,
            Neutral,
            Rainbow,
            TonalSpot,
            Vibrant
        }
    }

    @Serializable
    data class ColorSpec(val value: Values) : Setting() {
        companion object {
            const val NAME = "THEME_COLOR_SPEC"
            val defaultValue = Values.SPEC_2025

            fun default() = ColorSpec(defaultValue)
        }

        override val name: String = NAME
        override val valueSerialized: String = value.name
        override val defaultValueSerialized: String = defaultValue.name

        enum class Values {
            SPEC_2021,
            SPEC_2025,
            SPEC_2026
        }
    }

    @Serializable
    data class IsAmoled(val value: Boolean) : Setting() {
        companion object {
            const val NAME = "THEME_IS_AMOLED"
            private const val DEFAULT_VALUE = false

            fun default() = IsAmoled(DEFAULT_VALUE)
        }

        override val name: String = NAME
        override val valueSerialized: String = value.toString()
        override val defaultValueSerialized: String = DEFAULT_VALUE.toString()
    }
}