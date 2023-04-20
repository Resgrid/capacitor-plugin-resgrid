import Foundation
import SwiftUI
import LiveKitClient
import SFSafeSymbols
import WebRTC
import AVKit

struct StatsView: View {

    @ObservedObject private var viewModel: DelegateObserver
    private let track: Track

    init(track: Track) {
        self.track = track
        viewModel = DelegateObserver(track: track)
    }

    var body: some View {
        HStack(alignment: .top, spacing: 5) {
            VStack(alignment: .leading, spacing: 5) {
                if track is VideoTrack {
                    HStack(spacing: 3) {
                        Image(systemSymbol: .videoFill)
                        Text("Video").fontWeight(.bold)
                        if let dimensions = viewModel.dimensions {
                            Text("\(dimensions.width)×\(dimensions.height)")
                        }
                    }
                } else if track is AudioTrack {
                    HStack(spacing: 3) {
                        Image(systemSymbol: .micFill)
                        Text("Audio").fontWeight(.bold)
                    }
                } else {
                    Text("Unknown").fontWeight(.bold)
                }

                if let trackStats = viewModel.stats {

                    if trackStats.bpsSent != 0 {

                        HStack(spacing: 3) {
                            if let codecName = trackStats.codecName {
                                Text(codecName.uppercased()).fontWeight(.bold)
                            }
                            Image(systemSymbol: .arrowUpCircle)
                            Text(trackStats.formattedBpsSent())
                        }
                    }

                    if trackStats.bpsReceived != 0 {
                        HStack(spacing: 3) {
                            if let codecName = trackStats.codecName {
                                Text(codecName.uppercased()).fontWeight(.bold)
                            }
                            Image(systemSymbol: .arrowDownCircle)
                            Text(trackStats.formattedBpsReceived())
                        }
                    }
                }
            }
                    //.font(.system(size: 10))
                    //.foregroundColor(Color.white)
                    //.padding(5)
                    //.background(Color.black.opacity(0.5))
                    //.cornerRadius(8)
        }
    }
}

extension StatsView {

    class DelegateObserver: ObservableObject, TrackDelegate {
        private let track: Track
        @Published var dimensions: Dimensions?
        @Published var stats: TrackStats?

        init(track: Track) {
            self.track = track

            dimensions = track.dimensions
            stats = track.stats

            track.add(delegate: self)
        }

        func track(_ track: VideoTrack, didUpdate dimensions: Dimensions?) {
            Task.detached { @MainActor in
                self.dimensions = dimensions
            }
        }

        func track(_ track: Track, didUpdate stats: TrackStats) {
            Task.detached { @MainActor in
                self.stats = stats
            }
        }
    }
}

