import Foundation
import AVFoundation

@available(iOS 15.0, *)
class Audio {
    func playStartTransmit() {
        let resgridCap = Bundle(for: ResgridPlugin.self)
        let url = resgridCap.url(forResource: "StartTransmit", withExtension: "mp3")
        
        guard let url else {
            return
        }

        do {
            //let session = AVAudioSession.sharedInstance();
            //try session.setCategory(AVAudioSession.Category.ambient, mode: .default, options: [.mixWithOthers])
            //try session.setActive(true)
            //let player = try! AVAudioPlayer(contentsOf: url, fileTypeHint: AVFileType.mp3.rawValue)
            //player.volume = 1
            //player.numberOfLoops = 1
            //player.play()

            var soundID:SystemSoundID = 0
            AudioServicesCreateSystemSoundID(url as CFURL, &soundID)
            AudioServicesPlaySystemSound(soundID)

            //try session.setActive(false)
        } catch {
            print(error.localizedDescription)
        }
    }

    func playStopTransmit() {
        let resgridCap = Bundle(for: Self.self)
        let url = resgridCap.url(forResource: "StopTransmit", withExtension: "mp3")

        guard let url else {
            return
        }

        do {
            //let session = AVAudioSession.sharedInstance();
            //try session.setCategory(AVAudioSession.Category.ambient, mode: .default, options: [.mixWithOthers])
            //try session.setActive(true)
            //var player = try! AVAudioPlayer(contentsOf: url, fileTypeHint: AVFileType.mp3.rawValue)
            //player.numberOfLoops = 1
            //player.play()

            var soundID:SystemSoundID = 1
            AudioServicesCreateSystemSoundID(url as CFURL, &soundID)
            AudioServicesPlaySystemSound(soundID)

            //try session.setActive(false)
        } catch {
            print(error.localizedDescription)
        }
    }
}
