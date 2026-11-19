package com.amdg.jajanyuks.domain

// A `data class` gets equals/hashCode/toString/copy for free — ideal for plain models.
data class Vendor(
    val id: String,
    val name: String?, // null until the seller sets a name (sprint 2)
    val lat: Double,
    val lng: Double,
)
