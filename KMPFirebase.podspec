Pod::Spec.new do |spec|
  spec.name                     = 'KMPFirebase'
  spec.version                  = '0.0.1'
  spec.homepage                 = 'https://github.com/kwekuarmah/kmpfirebase'
  spec.source                   = {
    :http => 'https://repo1.maven.org/maven2/com/kweku/armah/kmpfirebase/0.0.1/kmpfirebase-0.0.1-xcframework.zip'
  }
  spec.authors                  = 'Kweku Armah'
  spec.license                  = { :type => 'Apache-2.0', :file => 'LICENSE' }
  spec.summary                  = 'Firebase Crashlytics KMP wrapper with embedded Firebase frameworks'
  spec.description              = <<-DESC
    A Kotlin Multiplatform library that wraps Firebase Crashlytics with the Firebase frameworks embedded.
    This allows iOS projects to use Firebase Crashlytics through a single XCFramework without manual setup.
  DESC

  spec.vendored_frameworks      = 'KMPFirebase.xcframework'
  spec.libraries                = 'c++'
  spec.ios.deployment_target    = '13.0'

  # Framework is already fat/universal, contains all architectures
  spec.pod_target_xcconfig = {
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386'
  }

  # Firebase is already embedded, no additional dependencies needed
  spec.static_framework = true
end
