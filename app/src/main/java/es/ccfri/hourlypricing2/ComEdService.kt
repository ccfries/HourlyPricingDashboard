package es.ccfri.hourlypricing2

import retrofit2.http.GET
import retrofit2.http.Query

interface ComEdService {
    @GET("api")
    suspend fun getCurrentPrices(
        @Query("type") type: String = "currenthouraverage"
    ): List<PricePoint>
}
