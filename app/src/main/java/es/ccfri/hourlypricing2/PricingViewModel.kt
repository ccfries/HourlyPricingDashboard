/*
 * Copyright (C) 2026 Chris Fries
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package es.ccfri.hourlypricing2

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.util.*

class PricingViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val _currentPrice = MutableStateFlow<Double?>(null)
    val currentPrice: StateFlow<Double?> = _currentPrice

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    // Robust converter that handles potential text prefixes in the response
    private val sanitizingConverterFactory = object : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *>? {
            val delegate = json.asConverterFactory("application/json".toMediaType())
                .responseBodyConverter(type, annotations, retrofit) as? Converter<ResponseBody, *>
            
            return Converter<ResponseBody, Any> { value ->
                val bodyString = value.string()
                val jsonStart = bodyString.indexOf("[")
                if (jsonStart != -1) {
                    val sanitizedJson = bodyString.substring(jsonStart)
                    val sanitizedBody = sanitizedJson.toResponseBody("application/json".toMediaType())
                    delegate?.convert(sanitizedBody)
                } else {
                    Log.e("PricingViewModel", "Invalid JSON response: $bodyString")
                    null
                }
            }
        }
    }

    private val service: ComEdService = Retrofit.Builder()
        .baseUrl("https://hourlypricing.comed.com/")
        .client(client)
        .addConverterFactory(sanitizingConverterFactory)
        .build()
        .create(ComEdService::class.java)

    val deliveryPrice = combine(
        repository.includeDelivery,
        repository.deliveryType
    ) { include, type ->
        if (!include) 0.0
        else when (type) {
            DeliveryType.NONE -> 0.0
            DeliveryType.FIXED -> 6.0
            DeliveryType.TIME_OF_DAY -> calculateTimeOfDayDelivery()
        }
    }

    val displayPrice = combine(
        _currentPrice,
        deliveryPrice
    ) { price, delivery ->
        if (price == null) null
        else price + delivery
    }

    init {
        refreshPrice()
    }

    private fun refreshPrice() {
        viewModelScope.launch {
            while (true) {
                try {
                    // Using currenthouraverage to show the price for this hour
                    val prices = service.getCurrentPrices(type = "currenthouraverage")
                    if (prices.isNotEmpty()) {
                        val priceStr = prices.first().price
                        _currentPrice.value = priceStr.toDoubleOrNull()
                    }
                } catch (e: Exception) {
                    Log.e("PricingViewModel", "Error fetching price", e)
                }
                delay(30000) // Refresh twice every minute
            }
        }
    }

    private fun calculateTimeOfDayDelivery(): Double {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        // Overnight: 12 AM - 6 AM
        // Morning: 6AM - 1 PM
        // Peak: 1 PM - 7 PM
        // Evening: 7 PM - 9 PM
        // Overnight: 9 PM - 12 AM
        val price = when { hour in 6..12 -> 4.0
            hour in 13..18 -> 10.0
            hour in 19..20 -> 4.0
            else -> 3.0
        };
        return price
    }
}
