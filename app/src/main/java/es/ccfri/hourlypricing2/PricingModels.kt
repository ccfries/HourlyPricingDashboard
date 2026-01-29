package es.ccfri.hourlypricing2

import kotlinx.serialization.Serializable

@Serializable
data class PricePoint(
    val millisUTC: String,
    val price: String
)

enum class DeliveryType {
    NONE,
    FIXED,
    TIME_OF_DAY
}
