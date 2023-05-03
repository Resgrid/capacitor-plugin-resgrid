package com.resgrid.plugins.resgrid.models

data class ConfigData (
        var url: String? = null,
        var type: Int = 0,
        var defaultMic: String? = null,
        var defaultSpeaker: String? = null,
        var rooms: ArrayList<RoomData> = ArrayList()
)