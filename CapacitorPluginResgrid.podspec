require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name = 'CapacitorPluginResgrid'
  s.version = package['version']
  s.summary = package['description']
  s.license = package['license']
  s.homepage = package['repository']['url']
  s.author = package['author']
  s.source = { :git => package['repository']['url'], :tag => s.version.to_s }
  s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
  s.resources = ['ios/Plugin/Resources/*.mp3']
  s.ios.deployment_target  = '15.0'
  s.dependency 'Capacitor'
  s.dependency 'LiveKitClient', '= 1.0.8'
  s.dependency 'KeychainAccess'
  s.dependency 'SFSafeSymbols', '~> 4.1.1'
  s.dependency 'SwiftProtobuf', '~> 1.0'
  s.swift_version = '5.1'
  s.static_framework = true
end