// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "KMPFirebase",
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: "KMPFirebase",
            targets: ["KMPFirebase"])
    ],
    targets: [
        .binaryTarget(
            name: "KMPFirebase",
            url: "https://repo1.maven.org/maven2/com/kweku/armah/kmpfirebase/0.0.1/kmpfirebase-0.0.1-xcframework.zip",
            checksum: "REPLACE_WITH_SHA256_CHECKSUM"
        )
    ]
)

// To generate checksum, run:
// swift package compute-checksum kmpfirebase-0.0.1-xcframework.zip
//
// Then replace "REPLACE_WITH_SHA256_CHECKSUM" with the generated checksum
