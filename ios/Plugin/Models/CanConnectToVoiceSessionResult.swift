//
//  CanConnectToVoiceSessionResult.swift
//  Plugin
//
//  Created by Shawn Jackson on 6/15/23.
//  Copyright © 2023 Resgrid, LLC. All rights reserved.
//

import Foundation

struct CanConnectToVoiceSessionResult: Codable{
    let PageSize: Int?
    let Page: Int?
    let Timestamp: String?
    let Version: String?
    let Node: String?
    let Environment: String?
    let RequestId: String?
    let Status: String?
    let PreviousPageUrl: String?
    let NextPageUrl: String?
    let Data: CanConnectToVoiceSessionResultData?
}

struct CanConnectToVoiceSessionResultData: Codable{
    let CurrentSessions: Int?
    let MaxSessions: Int?
    let CanConnect: Bool?
}
