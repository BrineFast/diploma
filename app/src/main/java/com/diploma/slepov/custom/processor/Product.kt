package com.diploma.slepov.custom.processor

/** Класс, хранящий информацию о полученных с сервера объектов **/
data class Product internal constructor(val imageUrl: String, val title: String, val subtitle: String)
