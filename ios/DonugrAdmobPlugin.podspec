Pod::Spec.new do |s|
  s.name = 'DonugrAdmobPlugin'
  s.version = '0.1.0'
  s.summary = 'Standalone Capacitor AdMob plugin with consent, standard ads, native host ads, and inline banner host placements.'
  s.license = 'MIT'
  s.source = { :git => 'https://github.com/donugr/admob.git', :tag => s.version.to_s }
  s.source_files = 'Plugin/**/*.{swift}'
  s.ios.deployment_target = '13.0'
  s.dependency 'Capacitor'
end
