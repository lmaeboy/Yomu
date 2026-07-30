package com.example.yomu

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Library : NavKey
@Serializable data object Browse : NavKey
@Serializable data class Detail(val mangaUrl: String) : NavKey
@Serializable data class Viewer(val mangaUrl: String, val chapterUrl: String) : NavKey
