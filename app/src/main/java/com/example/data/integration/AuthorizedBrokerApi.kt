package com.example.data.integration

/**
 * Stage 2 Integration Extension Point: Authorized Broker API.
 *
 * NOTE: Production broker submission (e.g. Zerodha Kite Connect, Angel One SmartAPI,
 * Upstox Developer API, ICICI Direct Breeze) requires formal corporate developer
 * agreements, OAuth2 client credentials, and SEBI algorithmic/bid authorization.
 *
 * In Stage 1 (Personal Tracker), all bids are submitted by the user on their broker's app
 * and logged here. This interface defines the contract for Stage 2 authorized integrations.
 */
interface AuthorizedBrokerApi {
    val brokerIdentifier: String // "ZERODHA", "ANGEL_ONE", "GROWW", "UPSTOX"
    val isAuthorized: Boolean // True only when production partnership key is configured

    suspend fun placeIpoBid(
        applicationReference: String,
        symbol: String,
        dematAccountId: String,
        upiId: String,
        lots: Int,
        cutoffPrice: Boolean,
        bidPrice: Double
    ): BrokerBidResult

    suspend fun cancelIpoBid(
        brokerApplicationNumber: String
    ): BrokerCancelResult
}

sealed class BrokerBidResult {
    data class Success(val exchangeApplicationNumber: String, val message: String) : BrokerBidResult()
    data class Error(val code: String, val errorMessage: String) : BrokerBidResult()
    object Unauthorized : BrokerBidResult()
}

sealed class BrokerCancelResult {
    data class Success(val message: String) : BrokerCancelResult()
    data class Error(val code: String, val errorMessage: String) : BrokerCancelResult()
    object Unauthorized : BrokerCancelResult()
}
