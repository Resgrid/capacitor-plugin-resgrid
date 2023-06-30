//
// Created by Shawn Jackson on 6/15/23.
// Copyright (c) 2023 Resgrid, LLC. All rights reserved.
//

import Foundation


class ApiVoiceRequest : ObservableObject{
    @Published var canConnect: Bool = false

    var resourceURL: URL
    let urlString = "api/v4/Voice/CanConnectToVoiceSession?token="

    init(config: ConfigModel) {
        resourceURL = URL(string: config.apiUrl + urlString + config.canConnectToVoiceApiToken)!
    }

    func canUserConnectToSession() async -> CanConnectToVoiceSessionResult {
        do {
            let (data, response) = try await URLSession.shared.data(from: resourceURL)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else {
                return CanConnectToVoiceSessionResult(PageSize: 0, Page: 0, Timestamp: "", Version: "", Node: "",
                        Environment: "", RequestId: "", Status: "", PreviousPageUrl: "", NextPageUrl: "",
                        Data: CanConnectToVoiceSessionResultData(CurrentSessions: 0, MaxSessions: 0, CanConnect: false))
            }

            let decoder = JSONDecoder()
            decoder.keyDecodingStrategy = .convertFromSnakeCase
            let decodedResponse = try decoder.decode(CanConnectToVoiceSessionResult.self, from: data)

            return decodedResponse
        } catch {
            print(error)
            return CanConnectToVoiceSessionResult(PageSize: 0, Page: 0, Timestamp: "", Version: "", Node: "",
                    Environment: "", RequestId: "", Status: "", PreviousPageUrl: "", NextPageUrl: "",
                    Data: CanConnectToVoiceSessionResultData(CurrentSessions: 0, MaxSessions: 0, CanConnect: false))
        }
    }
    /*
    func canUserConnectToSession(completion:@escaping (CanConnectToVoiceSessionResult) -> ()) {
        URLSession.shared.dataTask(with: resourceURL) { data, response, error in
                    if (data != nil) {
                        do {
                            let canConnect = try JSONDecoder().decode(CanConnectToVoiceSessionResult.self, from: data!)

                            DispatchQueue.main.async {
                                completion(canConnect)
                            }
                        } catch {
                            completion(CanConnectToVoiceSessionResult(PageSize: 0, Page: 0, Timestamp: "", Version: "", Node: "",
                                    Environment: "", RequestId: "", Status: "", PreviousPageUrl: "", NextPageUrl: "",
                                    Data: CanConnectToVoiceSessionResultData(CurrentSessions: 0, MaxSessions: 0, CanConnect: false)))
                        }


                    } else {
                        completion(CanConnectToVoiceSessionResult(PageSize: 0, Page: 0, Timestamp: "", Version: "", Node: "",
                                Environment: "", RequestId: "", Status: "", PreviousPageUrl: "", NextPageUrl: "",
                                Data: CanConnectToVoiceSessionResultData(CurrentSessions: 0, MaxSessions: 0, CanConnect: false)))
                    }
                }.resume()

    }*/
}

struct Api {

    var resourceURL: URL
    let urlString = "api/v4/Voice/CanConnectToVoiceSession"

    init(config: ConfigModel) {
        resourceURL = URL(string: config.apiUrl + urlString)!
    }

    func canUserConnectToSession(completion: @escaping(Result<CanConnectToVoiceSessionResult, Error>) -> Void) {

        let dataTask = URLSession.shared.dataTask(with: resourceURL) { (data, response, error) in

            guard error == nil else {
                print (error!.localizedDescription)
                print ("stuck in data task")
                return
            }

            let decoder = JSONDecoder()

            do {
                let jsonData = try decoder.decode(CanConnectToVoiceSessionResult.self, from: data!)
                completion(.success(jsonData))
            }
            catch {
                print ("an error in catch")
                print (error)
            }



        }
        dataTask.resume()
    }
}
