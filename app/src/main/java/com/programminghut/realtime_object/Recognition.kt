package com.programminghut.realtime_object

import android.graphics.RectF

data class Recognition(
    var labelId: Int,
    var labelName: String,
    var labelScore: Float,
    var confidence: Float,
    var location: RectF
) {
    init {
        require(labelId != null) { "Label ID must not be null" }
        require(labelScore != null) { "Label Score must not be null" }
        require(confidence != null) { "Confidence must not be null" }
    }

    override fun toString(): String {
        var resultString = ""

        labelId?.let { resultString += "$it " }
        labelName?.let { resultString += "$it " }
        confidence?.let { resultString += "(%.1f%%) ".format(it * 100.0f) }
        location?.let { resultString += "$it " }

        return resultString.trim()
    }
}