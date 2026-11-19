package com.amdg.jajanyuks.domain

data class MenuItem(
    val id: String,
    val name: String,
    val price: Long, // in Rupiah, no decimals
    val stock: Int,
)
