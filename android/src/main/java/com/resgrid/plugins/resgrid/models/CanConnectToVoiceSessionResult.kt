package com.resgrid.plugins.resgrid.models

class CanConnectToVoiceSessionResult(
    var PageSize: Int,
    var Page: Int,
    var Timestamp: String,
    var Version: String,
    var Node: String,
    var Environment: String,
    var RequestId: String,
    var Status: String,
    var PreviousPageUrl: String,
    var NextPageUrl: String,
    var Data: CanConnectToVoiceSessionResultData
)

class CanConnectToVoiceSessionResultData (
    var CanConnect: Boolean,
    var CurrentSessions: Int,
    var MaxSessions: Int
)