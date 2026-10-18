package com.example.model

enum class IpoSeries(val displayName: String) {
    MAINBOARD("Mainboard"),
    SME("SME")
}

enum class IpoStatus(val displayName: String) {
    UPCOMING("Upcoming"),
    OPEN("Open Now"),
    CLOSED("Closed / Awaiting Allotment"),
    ALLOTTED("Allotment Finalized"),
    LISTED("Listed")
}

enum class IpoCategory(val displayName: String, val maxLimitInr: Double) {
    RETAIL("Retail Individual (RII)", 200000.0),
    SHNI("Small HNI (sHNI / bHNI-1)", 500000.0),
    BHNI("Big HNI (bHNI)", 10000000.0),
    SHAREHOLDER("Shareholder Quota", 200000.0),
    EMPLOYEE("Employee Quota", 500000.0)
}

enum class MandateStatus(val displayName: String) {
    PENDING("Mandate Pending"),
    APPROVED("Mandate Approved"),
    EXPIRED("Mandate Expired"),
    REJECTED("Mandate Failed")
}

enum class AllotmentStatus(val displayName: String) {
    AWAITING("Awaiting Allotment"),
    ALLOTTED("Allotted"),
    NOT_ALLOTTED("Not Allotted"),
    REFUNDED("Refund Initiated")
}

enum class Depository(val displayName: String) {
    CDSL("CDSL"),
    NSDL("NSDL")
}
