package com.example.data.integration

/**
 * Stage 2 Integration Extension Point: Authorized Registrar Allotment API.
 *
 * NOTE: Registrars (Link Intime, KFin Technologies, Bigshare Services, Cameo)
 * do NOT provide open public REST APIs for individual developers. Automated scraping
 * or bypassing CAPTCHAs violates their terms of service and SEBI data policies.
 *
 * This interface represents the authorized partnership webhook / B2B feed contract
 * once official registrar licensing is established.
 */
interface AuthorizedRegistrarApi {
    val registrarName: String // "LINK_INTIME", "KFINTECH", "BIGSHARE"
    val hasCommercialAgreement: Boolean

    suspend fun queryAllotmentStatus(
        companySymbol: String,
        pan: String,
        applicationNumber: String? = null,
        dpIdClientId: String? = null
    ): RegistrarAllotmentResult
}

sealed class RegistrarAllotmentResult {
    data class Found(
        val isAllotted: Boolean,
        val sharesApplied: Int,
        val sharesAllotted: Int,
        val refundAmount: Double,
        val bankReference: String?
    ) : RegistrarAllotmentResult()

    object NotFinalized : RegistrarAllotmentResult()
    data class Error(val message: String) : RegistrarAllotmentResult()
    object PartnershipRequired : RegistrarAllotmentResult()
}
