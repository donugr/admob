Pod::Spec.new do |s|
  s.name = 'DonugrAdmobPlugin'
  s.version = '0.2.0'
  s.summary = 'Capacitor AdMob plugin with Android-first support for consent, standard ads, rewarded interstitial, native host placements, and inline banner host placements.'
  s.description = <<-DESC
    Public Capacitor AdMob plugin with an Android-first API surface for consent flows,
    banner, interstitial, rewarded, rewarded interstitial, app open, native host
    placements, and inline banner host placements. iOS source layout is included
    for future parity work.
  DESC
  s.homepage = 'https://github.com/donugr/admob'
  s.authors = { 'Donugr Teknomedia Nusantara' => 'dev@donugr.id' }
  s.license = 'MIT'
  s.source = { :git => 'https://github.com/donugr/admob.git', :tag => s.version.to_s }
  s.source_files = 'Plugin/**/*.swift'
  s.ios.deployment_target = '13.0'
  s.swift_version = '5.9'
  s.requires_arc = true
  s.module_name = 'DonugrAdmobPlugin'
  s.dependency 'Capacitor'
end
